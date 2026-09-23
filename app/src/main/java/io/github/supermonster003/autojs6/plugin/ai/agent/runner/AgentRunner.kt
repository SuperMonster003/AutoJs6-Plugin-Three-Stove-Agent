package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.RejectedExecutionException
import java.util.Locale

/** One run, confined to a link's serial scheduler. No Binder, Android or model-provider dependency.
 * Observers must return promptly; the P2.5 adapter forwards events through the oneway callback. */
class AgentRunner internal constructor(
    val id: String, private val options: RunOptions, private val scheduler: RunScheduler,
    private val catalog: ToolCatalog, private val policy: ToolPolicy, private var compiler: RunContextCompiler,
    private var model: RunModel, private var tools: RunTools, private val text: RunnerText,
    private val listener: (RunEvent) -> Unit, private val onTerminal: (AgentRunner) -> Unit,
    private val preparation: RunPreparation? = null,
) {
    @Volatile var state = RunState.QUEUED; private set
    @Volatile private var resultData: JsonObject? = null
    val result: JsonObject? get() = resultData?.deepCopy()
    private val stopping = AtomicReference<RunError?>(null)
    private var terminalClaimed = false // Protected by stopping's monitor, shared with requestStop.
    private val journal = StepJournal()
    private val gate = ConfirmationGate(policy, options.confirmationMode)
    private val handlers = ToolHandlers(catalog)
    private val validator = DecisionValidator(catalog)
    private var budget: Budget? = null
    private var durationTimer = Cancellation.NONE
    private var operation: Operation? = null
    private var interaction: Interaction? = null
    private var eventSequence = 0L
    private var requestSequence = 0
    private var decision: AgentDecision? = null
    private var parseMode: ParseMode? = null
    private var repairSession: DecisionRepairSession? = null
    private var observation: String? = null
    private var confirmation: String? = null
    private var stepStartedMs = 0L
    private var stepUsageStart = JsonObject()
    private var stepEstimated = false
    private var recorded = true
    private var successfulTools = 0
    private var scriptCalls = 0
    private var otherActions = 0
    private var scriptResult: JsonObject? = null
    private var format = options.format
    private var formatFallbacks = 0

    private class Operation(val onCancelled: (Cancellation) -> Unit) {
        var active = true
        var timer = Cancellation.NONE
        var cancellation = Cancellation.NONE
    }
    private class Interaction(val id: String, val deadlineMs: Long, val ask: AgentDecision.Ask? = null,
                              val tool: PreparedTool? = null, val assessment: ConfirmationAssessment? = null) {
        var timer = Cancellation.NONE
    }

    internal fun start() = scheduler.execute {
        guarded {
            if (state != RunState.QUEUED) return@guarded
            budget = Budget(options.limits, scheduler.nowMs(), scheduler::nowMs)
            durationTimer = scheduler.schedule(options.limits.maxDurationMs) { guarded { throw BudgetExceeded("duration") } }
            transition(RunState.RUNNING)
            if (preparation == null) { format = model.initialFormat(options.format); nextStep() }
            else beginOperation(minOf(15_000, checkNotNull(budget).remainingMs), RunError.TARGET_UNAVAILABLE, RunError.HOST_UNAVAILABLE,
                { callback -> preparation.prepare(callback) }) { outcome ->
                when (outcome) {
                    is PortResult.Failure -> finishError(outcome.error)
                    is PortResult.Success -> {
                        compiler = outcome.value.compiler; model = outcome.value.model; tools = outcome.value.tools
                        outcome.value.maximumTokens?.let { checkNotNull(budget).narrowTokens(it) }
                        format = model.initialFormat(options.format); nextStep()
                    }
                }
            }
        }
    }

    fun cancel(): Boolean = requestStop(RunError.CANCELLED)
    internal fun hostUnavailable(error: RunError): Boolean { require(error.hostLost); return requestStop(error) }
    private fun requestStop(error: RunError): Boolean {
        synchronized(stopping) {
            if (state.terminal || terminalClaimed || !stopping.compareAndSet(null, error)) return false
        }
        enqueueCallback { if (!state.terminal) finishStop(error) }
        return true
    }

    fun respond(requestId: String, value: JsonElement, callback: (ReplyStatus) -> Unit = {}) {
        if (state.terminal) { safely { callback(ReplyStatus.NOT_WAITING) }; return }
        val copy = try { AgentJson.parse(value.toString(), 4096) } catch (_: Exception) { null }
        dispatchReply(callback) {
            val waiting = currentInteraction(requestId)
            val ask = waiting?.ask
            val status = when {
                ask == null -> ReplyStatus.NOT_WAITING
                copy == null || !validAnswer(ask, copy) -> ReplyStatus.INVALID
                else -> {
                    clearInteraction()
                    transition(RunState.RUNNING)
                    val answer = jsonObject("answer" to copy)
                    ask.memoryKey?.let { answer.addProperty("memoryKey", it); answer.addProperty("memoryProposalOnly", true) }
                    observation = ToolObservation.success(answer)
                    record(observation)
                    guarded { nextStep() }
                    ReplyStatus.ACCEPTED
                }
            }
            safely { callback(status) }
        }
    }

    fun confirm(requestId: String, allowed: Boolean, scope: ConfirmationScope = ConfirmationScope.ONCE,
                callback: (ReplyStatus) -> Unit = {}) = dispatchReply(callback) {
        val waiting = currentInteraction(requestId)
        val assessment = waiting?.assessment
        val status = when {
            assessment == null -> ReplyStatus.NOT_WAITING
            allowed && !gate.allow(assessment, scope) -> ReplyStatus.INVALID
            else -> {
                clearInteraction()
                transition(RunState.RUNNING)
                if (allowed) { confirmation = "allowed"; guarded { executeTool(checkNotNull(waiting.tool)) } }
                else rejected(RunError.USER_DENIED)
                ReplyStatus.ACCEPTED
            }
        }
        safely { callback(status) }
    }

    fun readJournal(callback: (JsonObject) -> Unit) {
        if (state.terminal) { safely { callback(journal.snapshot()) }; return }
        try { scheduler.execute { safely { callback(journal.snapshot()) } } }
        catch (error: RejectedExecutionException) {
            if (state.terminal) safely { callback(journal.snapshot()) } else throw error
        }
    }
    private fun dispatchReply(callback: (ReplyStatus) -> Unit, action: () -> Unit) {
        if (state.terminal) { safely { callback(ReplyStatus.NOT_WAITING) }; return }
        try { scheduler.execute(action) }
        catch (error: RejectedExecutionException) {
            if (state.terminal) safely { callback(ReplyStatus.NOT_WAITING) } else throw error
        }
    }

    private fun nextStep() {
        if (!canContinue()) return
        val b = checkNotNull(budget)
        b.beginStep()
        decision = null; parseMode = null; confirmation = null; recorded = false
        stepStartedMs = scheduler.nowMs(); stepUsageStart = b.usageJson(); stepEstimated = false
        repairSession = DecisionRepairSession(validator, policy, format)
        requestModel(null)
    }

    private fun requestModel(repair: JsonObject?) {
        if (!canContinue()) return
        val b = checkNotNull(budget)
        val input = compiler.compile(RunContext(options.goal, journal.history(), observation, repair?.deepCopy(), b.remainingJson(), format, options.locale))
        if (!canContinue()) return
        val reservation = b.reserveModel(input.inputBytes, minOf(options.maximumOutputTokens, input.maximumOutputTokens ?: options.maximumOutputTokens))
        var settled = false
        fun settle(usage: ModelUsage?, outputBytes: Int) {
            if (settled) return
            b.settleModel(reservation, usage, outputBytes); settled = true
            stepEstimated = stepEstimated || usage?.inputTokens == null || usage.outputTokens == null
        }
        beginOperation(minOf(options.modelTimeoutMs, b.remainingMs), RunError.MODEL_TIMEOUT, RunError.MODEL_FAILED,
            { callback -> model.generate(input, reservation.maximumOutputTokens, minOf(options.modelTimeoutMs, b.remainingMs), callback) },
            onCancelled = { handle -> (handle as? ModelCallCancellation)?.progress()?.let { settle(it.usage, it.outputBytes) } }) { outcome ->
            when (outcome) {
                is PortResult.Failure -> {
                    settle(outcome.usage, outcome.outputBytes)
                    if (outcome.error.hostLost) { finishError(outcome.error); return@beginOperation }
                    b.check()
                    val next = if (formatFallbacks < 2) model.fallbackFormat(format, outcome) else null
                    if (next == null) finishError(outcome.error) else {
                        formatFallbacks++; format = next
                        checkNotNull(repairSession).switchFormat(next)
                        requestModel(repair) // New admission and usage ticket, same step and repair allowance.
                    }
                }
                is PortResult.Success -> {
                    val reply = outcome.value
                    val outputBytes = if (reply.text.length <= AgentJson.MAX_MODEL_BYTES) reply.text.toByteArray(Charsets.UTF_8).size else AgentJson.MAX_MODEL_BYTES + 1
                    settle(reply.usage, outputBytes)
                    b.check()
                    if (outputBytes > AgentJson.MAX_MODEL_BYTES) { finishError(RunError.LIMIT_EXCEEDED); return@beginOperation }
                    when (val attempt = checkNotNull(repairSession).evaluate(reply.text)) {
                        is DecisionAttempt.Repair -> requestModel(attempt.observation)
                        is DecisionAttempt.Exhausted -> finishError(RunError.DECISION_UNPARSABLE)
                        is DecisionAttempt.Accepted -> {
                            decision = attempt.decision; parseMode = attempt.parseMode
                            when (val accepted = attempt.decision) {
                                is AgentDecision.Tool -> prepareTool(accepted)
                                is AgentDecision.Ask -> waitForInput(accepted)
                                is AgentDecision.Done -> {
                                    record(null)
                                    finish(RunState.valueOf(accepted.status.uppercase(Locale.ROOT)), accepted.summary, accepted.evidence, accepted.unfinished, accepted.orderStatus)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun prepareTool(value: AgentDecision.Tool) {
        if (!canContinue()) return
        val b = checkNotNull(budget)
        val invocation = ToolInvocation(value.name, value.arguments, handlers.prepare(value.name, value.arguments, policy))
        beginOperation(b.toolTimeout(), RunError.BUDGET_EXCEEDED, RunError.HOST_UNAVAILABLE,
            { callback -> tools.prepare(invocation, b.toolTimeout(), callback) }) { outcome ->
            when (outcome) {
                is PortResult.Failure -> toolFailed(outcome.error, unknownPassword = true, scriptParameters = outcome.scriptParameters)
                is PortResult.Success -> {
                    val prepared = outcome.value
                    if (prepared.invocation !== invocation) { finishError(RunError.INVALID_REQUEST); return@beginOperation }
                    prepared.metadata.script?.let { decision = value.copy(arguments = it.arguments()) }
                    if (prepared.metadata.passwordField) protectText()
                    val spec = policy.requireEnabled(catalog, value.name)
                    val assessment = gate.assess(spec, prepared.metadata)
                    if (assessment.required) waitForConfirmation(prepared, spec, assessment)
                    else { confirmation = "auto"; executeTool(prepared) }
                }
            }
        }
    }

    private fun executeTool(prepared: PreparedTool) {
        if (!canContinue()) return
        val b = checkNotNull(budget)
        // An inspection cannot smuggle a longer non-script operation through script metadata.
        val scriptTimeout = if (prepared.invocation.name == "script_run") prepared.metadata.scriptTimeoutMs else null
        val timeout = b.toolTimeout(scriptTimeout)
        if (prepared.invocation.name == "report_progress") {
            emit("progress", jsonObject("step" to b.steps.json(), "message" to (prepared.invocation.arguments["message"] ?: "".json()), "budget" to b.remainingJson()))
        }
        if (!canContinue()) return
        b.beginTool()
        when (prepared.invocation.name) {
            "script_run" -> { scriptCalls++; scriptResult = null }
            "script_catalog", "report_progress" -> Unit
            else -> otherActions++
        }
        beginOperation(timeout, if (prepared.invocation.name == "script_run") RunError.SCRIPT_TIMEOUT else RunError.BUDGET_EXCEEDED,
            RunError.HOST_UNAVAILABLE, { callback -> tools.execute(prepared, timeout, callback) }) { outcome ->
            when (outcome) {
                is PortResult.Failure -> toolFailed(outcome.error)
                is PortResult.Success -> {
                    if (outcome.value.script?.error == null) successfulTools++
                    if (prepared.invocation.name == "script_run") scriptResult = outcome.value.script?.scriptResult
                    observation = compiler.observe(prepared.invocation.name, journal.redact(outcome.value.result))
                    record(observation, outcome.value.script?.error)
                    nextStep()
                }
            }
        }
    }

    private fun toolFailed(error: RunError, unknownPassword: Boolean = false,
                           scriptParameters: io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptParameterProblem? = null) {
        if (unknownPassword) protectText()
        if (error.hostLost || error == RunError.BUDGET_EXCEEDED) { finishError(error); return }
        observation = scriptParameters?.observation() ?: errorObservation(error)
        record(observation, error)
        nextStep()
    }

    private fun waitForInput(ask: AgentDecision.Ask) {
        if (!canContinue()) return
        val timeout = minOf(options.limits.askTimeoutMs, checkNotNull(budget).remainingMs)
        val waiting = installInteraction(timeout, ask = ask)
        transition(RunState.WAITING_INPUT)
        emit("input", jsonObject("requestId" to waiting.id.json(), "kind" to ask.kind.json(), "question" to ask.question.json(),
            "choices" to JsonArray().apply { ask.choices.forEach(::add) }, "timeoutMs" to timeout.json())
            .apply { ask.memoryKey?.let { addProperty("memoryKey", it) } })
    }
    private fun waitForConfirmation(prepared: PreparedTool, spec: ToolSpec, assessment: ConfirmationAssessment) {
        if (!canContinue()) return
        val timeout = minOf(options.limits.confirmationTimeoutMs, checkNotNull(budget).remainingMs)
        val waiting = installInteraction(timeout, tool = prepared, assessment = assessment)
        transition(RunState.WAITING_CONFIRMATION)
        val arguments = journal.redact(gate.arguments(prepared.invocation.arguments, prepared.metadata))
        // Script parameters are at most 16 KiB. Approval must display every effective parameter.
        val summary = if (prepared.metadata.script != null) arguments else StepJournal.clipped(arguments, 4096)
        emit("confirmation", jsonObject("requestId" to waiting.id.json(), "tool" to spec.name.json(),
            "description" to gate.description(spec, prepared.metadata, options.locale).json(), "risk" to assessment.risk.name.lowercase(Locale.ROOT).json(),
            "arguments" to summary, "allowRunScope" to assessment.allowRunScope.json(), "timeoutMs" to timeout.json()))
    }
    private fun installInteraction(timeout: Long, ask: AgentDecision.Ask? = null, tool: PreparedTool? = null,
                                   assessment: ConfirmationAssessment? = null): Interaction {
        val waiting = Interaction("$id:${++requestSequence}", scheduler.nowMs() + timeout, ask, tool, assessment)
        interaction = waiting
        waiting.timer = scheduler.schedule(timeout) { guarded { if (interaction === waiting) interactionTimedOut() } }
        return waiting
    }
    private fun currentInteraction(requestId: String): Interaction? {
        if (state.terminal || stopping.get() != null) return null
        val waiting = interaction?.takeIf { it.id == requestId } ?: return null
        if (scheduler.nowMs() >= waiting.deadlineMs) { guarded { interactionTimedOut() }; return null }
        return waiting
    }
    private fun interactionTimedOut() {
        val wasConfirmation = interaction?.assessment != null
        clearInteraction(); transition(RunState.RUNNING)
        if (wasConfirmation) confirmation = "denied"
        observation = errorObservation(RunError.USER_TIMEOUT)
        record(observation, RunError.USER_TIMEOUT)
        nextStep()
    }
    private fun rejected(error: RunError) {
        confirmation = "denied"
        observation = errorObservation(error)
        record(observation, error)
        guarded { nextStep() }
    }
    private fun clearInteraction() { interaction?.timer?.let { safely(it::cancel) }; interaction = null }
    private fun validAnswer(ask: AgentDecision.Ask, value: JsonElement): Boolean = when (ask.kind) {
        "confirm" -> value.isJsonPrimitive && value.asJsonPrimitive.isBoolean
        "choice" -> value.isJsonPrimitive && value.asJsonPrimitive.isString && value.asString in ask.choices
        else -> value.isJsonPrimitive && value.asJsonPrimitive.isString && value.asString.isNotBlank()
    }

    private fun <T> beginOperation(timeout: Long, timeoutError: RunError, exceptionError: RunError,
                                   invoke: ((PortResult<T>) -> Unit) -> Cancellation, onCancelled: (Cancellation) -> Unit = {},
                                   accept: (PortResult<T>) -> Unit) {
        if (!canContinue()) return
        check(operation == null && interaction == null)
        val pending = Operation(onCancelled); operation = pending
        pending.timer = scheduler.schedule(timeout) {
            guarded {
                if (operation === pending && pending.active) {
                    clearOperation(cancel = true)
                    if (timeoutError == RunError.BUDGET_EXCEEDED) throw BudgetExceeded("toolTimeout")
                    accept(PortResult.Failure(timeoutError))
                }
            }
        }
        try {
            val cancellation = invoke { outcome ->
                enqueueCallback {
                    guarded {
                        if (operation === pending && pending.active) {
                            clearOperation(cancel = false)
                            accept(outcome)
                        }
                    }
                }
            }
            pending.cancellation = cancellation
            if (!pending.active || stopping.get() != null) {
                safely(cancellation::cancel)
                if (!state.terminal) onCancelled(cancellation)
                pending.cancellation = Cancellation.NONE
            }
        } catch (_: Exception) {
            scheduler.execute { guarded {
                if (operation === pending && pending.active) { clearOperation(cancel = true); accept(PortResult.Failure(exceptionError)) }
            } }
        }
    }
    private fun enqueueCallback(action: () -> Unit) {
        if (state.terminal) return
        try { scheduler.execute(action) }
        catch (error: RejectedExecutionException) { if (!state.terminal) throw error }
    }
    private fun clearOperation(cancel: Boolean) {
        val pending = operation ?: return
        operation = null; pending.active = false
        safely(pending.timer::cancel)
        if (cancel) { safely(pending.cancellation::cancel); pending.onCancelled(pending.cancellation) }
        pending.cancellation = Cancellation.NONE
    }

    private fun canContinue(): Boolean {
        if (state.terminal) return false
        stopping.get()?.let { finishStop(it); return false }
        budget?.check()
        return true
    }
    private inline fun guarded(action: () -> Unit) {
        if (state.terminal) return
        try { if (canContinue()) action() }
        catch (error: BudgetExceeded) { finishError(RunError.BUDGET_EXCEEDED, error.dimension) }
        catch (_: ContextLimitExceeded) { finishError(RunError.LIMIT_EXCEEDED) }
        catch (_: Exception) { finishError(RunError.INVALID_REQUEST) }
    }
    private fun record(value: String?, error: RunError? = null) {
        if (recorded) return
        val current = decision ?: return // Do not invent a model decision when generation/repair failed.
        val b = checkNotNull(budget)
        recorded = true
        val usage = b.usageJson().apply {
            for (key in listOf("modelCalls", "inputTokens", "outputTokens", "totalTokens")) addProperty(key, number(key)!! - (stepUsageStart.number(key) ?: 0))
            addProperty("estimated", stepEstimated)
        }
        val data = StepJournal.decision(current).apply {
            parseMode?.let { addProperty("parseMode", it.name) }
            addProperty("repairs", repairSession?.repairsUsed ?: 0)
            addProperty("degraded", format.degraded)
        }
        val entry = journal.append(StepRecord(b.steps, data.string("kind")!!, data,
            (current as? AgentDecision.Tool)?.name, (current as? AgentDecision.Tool)?.arguments,
            confirmation, value, usage, (scheduler.nowMs() - stepStartedMs).coerceAtLeast(0), error?.name))
        emit("step", entry)
    }
    private fun protectText() {
        (decision as? AgentDecision.Tool)?.takeIf { it.name == "ui_set_text" }?.arguments?.string("text")?.let(journal::protectText)
    }
    private fun finishStop(error: RunError) {
        if (state.terminal) return
        if (error == RunError.CANCELLED && state != RunState.QUEUED) transition(RunState.CANCELLING)
        finishError(error)
    }
    private fun finishError(error: RunError, budgetDimension: String? = null) {
        if (state.terminal) return
        clearOperation(cancel = true); clearInteraction()
        budget?.abandonModel()
        protectText() // Also protects an unresolved/password-unknown text operation on cancellation.
        record(errorObservation(error), error)
        val terminal = when {
            error == RunError.CANCELLED -> RunState.CANCELLED
            error.hostLost -> RunState.BLOCKED
            error == RunError.BUDGET_EXCEEDED && successfulTools > 0 -> RunState.PARTIAL
            else -> RunState.FAILED
        }
        finish(terminal, text.terminal(error, budgetDimension), error = error)
    }
    private fun finish(terminal: RunState, summary: String, evidence: List<String> = emptyList(), unfinished: List<String> = emptyList(),
                       orderStatus: String? = null, error: RunError? = null) {
        val stop = synchronized(stopping) {
            if (state.terminal || terminalClaimed) return
            val requested = stopping.get()
            if (requested != null && requested != error) requested
            else { terminalClaimed = true; null }
        }
        if (stop != null) { finishStop(stop); return }
        check(terminal.terminal)
        clearOperation(cancel = true); clearInteraction(); safely(durationTimer::cancel)
        val b = budget
        val value = jsonObject("id" to id.json(), "status" to terminal.wire.json(), "summary" to summary.json(),
            "steps" to (b?.steps ?: 0).json(), "toolCalls" to (b?.toolCalls ?: 0).json(),
            "usage" to (b?.usageJson() ?: jsonObject("modelCalls" to 0.json(), "estimated" to false.json())),
            "durationMs" to (b?.durationMs ?: 0).json(), "evidence" to JsonArray().apply { evidence.forEach(::add) },
            "unfinished" to JsonArray().apply { unfinished.forEach(::add) })
        orderStatus?.let { value.addProperty("orderStatus", it) }
        error?.let { value.add("error", jsonObject("code" to it.name.json(), "message" to summary.json())) }
        if (error == null && decision is AgentDecision.Done && scriptCalls == 1 && otherActions == 0) {
            scriptResult?.let { value.add("script", it.deepCopy()) }
        }
        resultData = journal.finish(value)
        transition(terminal)
        error?.let { emit("error", checkNotNull(resultData).getAsJsonObject("error") ?: jsonObject("code" to it.name.json())) }
        emit("done", checkNotNull(resultData))
        safely { onTerminal(this) }
    }
    private fun errorObservation(error: RunError) = jsonObject("error" to error.name.json()).toString()
    private fun transition(next: RunState) {
        if (state == next) return
        val previous = state; state = next
        emit("state", jsonObject("from" to previous.wire.json(), "to" to next.wire.json()))
    }
    private fun emit(type: String, payload: JsonObject) {
        val event = RunEvent(id, ++eventSequence, type, payload)
        safely { listener(event) } // A detached observer cannot abort an owned task.
    }
    private inline fun safely(action: () -> Unit) { try { action() } catch (_: Exception) { /* No private exception text in ordinary logs. */ } }
}
