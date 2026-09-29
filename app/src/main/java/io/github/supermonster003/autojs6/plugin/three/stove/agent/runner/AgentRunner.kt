package io.github.supermonster003.autojs6.plugin.three.stove.agent.runner

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.RejectedExecutionException
import java.util.Locale

/** One run, confined to a link's serial scheduler. No Binder, Android or model-provider dependency.
 * Observers must return promptly; the P2.5 adapter forwards events through the oneway callback. */
class AgentRunner internal constructor(
    val id: String, private val options: RunOptions, private val scheduler: RunScheduler,
    private var catalog: ToolCatalog, private var policy: ToolPolicy, private var compiler: RunContextCompiler,
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
    private var gate = ConfirmationGate(policy, options.confirmationMode)
    private var handlers = ToolHandlers(catalog)
    private var validator = DecisionValidator(catalog)
    private var cleanup = Cancellation.NONE
    private var acceptedComponents: RunComponents? = null
    private val loopRules = LoopRules()
    private val doneRules = DoneRules(text)
    private var budget: Budget? = null
    private var durationTimer = Cancellation.NONE
    private var operation: Operation? = null
    private var interaction: Interaction? = null
    /** Plan mode: the first accepted decision must be a plan; the approved (possibly edited) steps then ride along in the guidance. */
    private var planRequired = false
    private var approvedPlan: List<String>? = null
    private var eventSequence = 0L
    private var requestSequence = 0
    private var decision: AgentDecision? = null
    private var parseMode: ParseMode? = null
    private var repairSession: DecisionRepairSession? = null
    private var responseLimitExceeded = false
    private var internalFailure: String? = null // Exception class only; never its message.
    private var observation: String? = null
    private var observationImages = emptyList<ModelImage>()
    private var confirmation: String? = null
    private var stepStartedMs = 0L
    private var stepUsageStart = JsonObject()
    private var stepEstimated = false
    private var recorded = true
    private var userRequestedStep = false
    private var successfulTools = 0
    private var scriptCalls = 0
    private var otherActions = 0
    private var scriptResult: JsonObject? = null
    private var format = options.format
    private var formatFallbacks = 0
    private var nativeTurn: NativeToolTurn? = null
    private var nativeDeadlineMs: Long? = null
    private var nativeContextRebuilt = false
    private val nativeQueue = ArrayDeque<AgentDecision.Tool>()
    private val nativeResults = mutableListOf<NativeToolResult>()
    private var activeNativeCall: NativeToolCall? = null

    private class Operation(val onCancelled: (Cancellation) -> Unit) {
        var active = true
        var timer = Cancellation.NONE
        var cancellation = Cancellation.NONE
    }
    private class Interaction(val id: String, val deadlineMs: Long, val ask: AgentDecision.Ask? = null,
                              val tool: PreparedTool? = null, val assessment: ConfirmationAssessment? = null, val plan: AgentDecision.Plan? = null) {
        var timer = Cancellation.NONE
    }

    internal fun start() = scheduler.execute {
        guarded {
            if (state != RunState.QUEUED) return@guarded
            budget = Budget(options.limits, scheduler.nowMs(), scheduler::nowMs)
            durationTimer = scheduler.schedule(options.limits.maxDurationMs) { guarded { throw BudgetExceeded("duration") } }
            transition(RunState.RUNNING)
            if (preparation == null) { format = startingFormat(); nextStep() }
            else beginOperation(minOf(RunLimits.PREPARATION_MS, checkNotNull(budget).remainingMs), RunError.TARGET_UNAVAILABLE, RunError.HOST_UNAVAILABLE,
                { callback -> preparation.prepare(callback) }, onDiscard = { outcome ->
                    if (outcome is PortResult.Success && outcome.value !== acceptedComponents) safely(outcome.value.cleanup::cancel)
                }) { outcome ->
                when (outcome) {
                    is PortResult.Failure -> finishError(outcome.error, mcpReason = outcome.mcpReason)
                    is PortResult.Success -> {
                        acceptedComponents = outcome.value
                        cleanup = outcome.value.cleanup
                        outcome.value.catalog?.let { catalog = it; handlers = ToolHandlers(it); validator = DecisionValidator(it) }
                        compiler = outcome.value.compiler; model = outcome.value.model; tools = outcome.value.tools
                        outcome.value.policy?.let { policy = it; gate = ConfirmationGate(it, options.confirmationMode) }
                        outcome.value.maximumTokens?.let { checkNotNull(budget).narrowTokens(it) }
                        format = startingFormat(); nextStep()
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

    fun respond(requestId: String, value: JsonElement, rememberScope: String? = null, callback: (ReplyStatus) -> Unit = {}) {
        if (state.terminal) { safely { callback(ReplyStatus.NOT_WAITING) }; return }
        val copy = try { AgentJson.parse(value.toString(), 4096) } catch (_: Exception) { null }
        dispatchReply(callback) {
            val waiting = currentInteraction(requestId)
            val ask = waiting?.ask
            val plan = waiting?.plan
            val status = when {
                plan != null -> if (copy == null || rememberScope != null || !validPlan(copy)) ReplyStatus.INVALID else { acceptPlan(plan, copy.asJsonArray); ReplyStatus.ACCEPTED }
                ask == null -> ReplyStatus.NOT_WAITING
                copy == null || !validAnswer(ask, copy) -> ReplyStatus.INVALID
                rememberScope != null && (ask.memoryKey == null || !policy.isEnabled(checkNotNull(catalog[ToolNames.MEMORY_PROPOSE]))) -> ReplyStatus.INVALID
                else -> {
                    clearInteraction()
                    transition(RunState.RUNNING)
                    val answer = jsonObject("answer" to copy)
                    ask.memoryKey?.let { answer.addProperty("memoryKey", it); answer.addProperty("memoryProposalOnly", true) }
                    observation = ToolObservation.success(answer)
                    observationImages = emptyList()
                    record(observation)
                    guarded {
                        if (rememberScope == null) nextStep()
                        else nextStep(AgentDecision.Tool(ToolNames.MEMORY_PROPOSE, jsonObject("key" to checkNotNull(ask.memoryKey).json(),
                            "value" to copy.asString.json(), "scope" to rememberScope.json()), null))
                    }
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

    private fun nextStep(userProposal: AgentDecision.Tool? = null) {
        if (!canContinue()) return
        val b = checkNotNull(budget)
        b.beginStep()
        decision = null; parseMode = null; confirmation = null; recorded = false
        responseLimitExceeded = false
        stepStartedMs = scheduler.nowMs(); stepUsageStart = b.usageJson(); stepEstimated = false
        userRequestedStep = userProposal != null
        if (userProposal != null) {
            // Explicit UI intent is charged as a tool step and still passes preparation and ConfirmationGate.
            repairSession = null; decision = userProposal; prepareTool(userProposal); return
        }
        repairSession = DecisionRepairSession(validator, policy, format) { proposed ->
            doneRules.validate(proposed)
            if (planRequired && proposed is AgentDecision.Tool) throw DecisionFailure("DECISION_UNPARSABLE", "Plan mode: respond with kind plan (1 to 8 short steps in execution order) before the first tool decision.")
        }
        if (nativeQueue.isNotEmpty()) {
            val accepted = nativeQueue.removeFirst()
            activeNativeCall = checkNotNull(nativeTurn).calls[nativeResults.size]
            decision = accepted; parseMode = ParseMode.NATIVE_TOOL
            prepareTool(accepted); return
        }
        requestModel(null)
    }

    private fun requestModel(repair: JsonObject?) {
        if (!canContinue()) return
        val b = checkNotNull(budget)
        if (policy.isOrderGoal(options.goal)) doneRules.requireOrderStatus()
        val guidance = verificationGuidance()
        var continuation = nativeTurn?.continuation
        var results = if (continuation == null) emptyList() else nativeResults.toList()
        val continuedBytes = try { continuation?.inputBytes(results) } catch (_: NativeContextLimitExceeded) {
            // Every call in the paused batch already has a journaled outcome. End only this
            // append-only conversation, then repack that history with the latest observation.
            // Keep the native format, repair allowance and run budget; the rebuilt request is a new round.
            check(nativeQueue.isEmpty() && activeNativeCall == null)
            val progress = closeNative()
            if (progress != null && progress.error != RunError.CANCELLED) {
                finishError(progress.error); return
            }
            nativeContextRebuilt = true
            continuation = null; results = emptyList()
            null
        }
        b.check()
        val input = if (continuation != null) null else compiler.compile(RunContext(options.goal, journal.history(), observation, repair?.deepCopy(), b.remainingJson(), format, options.locale, guidance, observationImages))
        val inputBytes = continuedBytes ?: checkNotNull(input).inputBytes
        if (!canContinue()) return
        // Every native round (the first request and each continuation after tool results) gets the full model
        // timeout; the run budget bounds the whole turn. Maintainer decision of 2026-09-29 (roadmap P13).
        if (format.nativeTools) nativeDeadlineMs = scheduler.nowMs() + options.modelTimeoutMs
        val timeoutMs = minOf(options.modelTimeoutMs, b.remainingMs,
            nativeDeadlineMs?.let { it - scheduler.nowMs() } ?: Long.MAX_VALUE)
        if (timeoutMs <= 0 || (format.nativeTools && timeoutMs < 1000)) { finishError(RunError.MODEL_TIMEOUT); return }
        val imageTokens = continuation?.imageTokens(results) ?: checkNotNull(input).imageTokens
        val reservation = b.reserveModel(inputBytes, minOf(options.maximumOutputTokens, input?.maximumOutputTokens ?: options.maximumOutputTokens), imageTokens)
        var settled = false
        fun settle(usage: ModelUsage?, outputBytes: Int) {
            if (settled) return
            b.settleModel(reservation, usage, outputBytes); settled = true
            stepEstimated = stepEstimated || usage?.inputTokens == null || usage.outputTokens == null
        }
        val admittedContinuation = continuation
        beginOperation(timeoutMs, RunError.MODEL_TIMEOUT, RunError.MODEL_FAILED,
            { callback -> if (admittedContinuation == null) model.generate(checkNotNull(input), reservation.maximumOutputTokens, timeoutMs, callback)
                else admittedContinuation.resume(results, reservation.maximumOutputTokens, timeoutMs, callback) },
            onCancelled = { handle -> (handle as? ModelCallCancellation)?.progress()?.let { settle(it.usage, it.outputBytes) } }) { outcome ->
            when (outcome) {
                is PortResult.Failure -> {
                    if (outcome.error == RunError.LIMIT_EXCEEDED) responseLimitExceeded = true
                    settle(outcome.usage, outcome.outputBytes)
                    if (outcome.error.hostLost) { finishError(outcome.error); return@beginOperation }
                    b.check()
                    val next = if (continuation == null && !nativeContextRebuilt && formatFallbacks < 2) model.fallbackFormat(format, outcome) else null
                    if (next == null) finishError(outcome.error, detail = if (outcome.error == RunError.LIMIT_EXCEEDED) "key:limit_response" else outcome.reason) else {
                        formatFallbacks++; format = next
                        nativeDeadlineMs = null
                        checkNotNull(repairSession).switchFormat(next)
                        requestModel(repair) // New admission and usage ticket, same step and repair allowance.
                    }
                }
                is PortResult.Success -> {
                    val reply = outcome.value
                    nativeTurn = reply.nativeTurn; nativeResults.clear(); activeNativeCall = null
                    if (nativeTurn == null) { nativeDeadlineMs = null; nativeContextRebuilt = false }
                    nativeTurn?.continuation?.claim()
                    val outputBytes = maxOf(reply.outputBytes, if (reply.text.length <= AgentJson.MAX_MODEL_BYTES) reply.text.utf8Size() else AgentJson.MAX_MODEL_BYTES + 1)
                    responseLimitExceeded = outputBytes > AgentJson.MAX_MODEL_BYTES
                    settle(reply.usage, outputBytes)
                    b.check()
                    if (responseLimitExceeded) { finishError(RunError.LIMIT_EXCEEDED, detail = "key:limit_response"); return@beginOperation }
                    val turn = reply.nativeTurn
                    if (turn != null && (turn.continuation.limits.batchBytes - 256) / turn.calls.size < 1024) {
                        finishError(RunError.LIMIT_EXCEEDED, detail = "key:limit_tool_results"); return@beginOperation
                    }
                    turn?.continuation?.onFailure { error -> enqueueCallback { guarded {
                        if (nativeTurn?.continuation === turn.continuation) finishError(error)
                    } } }
                    val attempt = if (turn == null) checkNotNull(repairSession).evaluate(reply.text) else checkNotNull(repairSession).evaluateNative(turn.calls)
                    when (attempt) {
                        is DecisionAttempt.Repair -> {
                            turn?.calls?.forEach { nativeResults += nativeResult(it, attempt.observation.toString(), true) }
                            requestModel(attempt.observation)
                        }
                        is DecisionAttempt.Exhausted -> finishError(RunError.DECISION_UNPARSABLE)
                        is DecisionAttempt.Accepted -> {
                            decision = attempt.decision; parseMode = attempt.parseMode
                            if (turn != null) {
                                activeNativeCall = turn.calls.first()
                                nativeQueue.addAll(attempt.remainingTools)
                            }
                            when (val accepted = attempt.decision) {
                                is AgentDecision.Tool -> prepareTool(accepted)
                                is AgentDecision.Ask -> waitForInput(accepted)
                                is AgentDecision.Plan -> waitForPlan(accepted)
                                is AgentDecision.Done -> {
                                    val checked = doneRules.normalize(accepted)
                                    val final = checked.decision
                                    decision = final
                                    record(checked.rules.takeIf { it.isNotEmpty() }?.let { rules ->
                                        jsonObject("rules" to JsonArray().apply { rules.forEach(::add) }, "proposedStatus" to accepted.status.json()).toString()
                                    })
                                    finish(RunState.valueOf(final.status.uppercase(Locale.ROOT)), final.summary, final.evidence, final.unfinished, final.orderStatus)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // The default policy is implied, which keeps the compact local-model prompt inside its budget.
    private fun verificationGuidance() = loopRules.guidance().apply {
        addProperty("orderStatusRequired", doneRules.orderStatusRequired)
        if (planRequired) addProperty("planRequired", true)
        approvedPlan?.let { steps -> add("plan", JsonArray().apply { steps.forEach(::add) }) }
        if (options.confirmationMode != ConfirmationMode.DEFAULT) addProperty("confirmationMode", options.confirmationMode.name.lowercase(Locale.ROOT))
    }

    private fun prepareTool(value: AgentDecision.Tool) {
        if (!canContinue()) return
        val b = checkNotNull(budget)
        val invocation = ToolInvocation(value.name, value.arguments, handlers.prepare(value.name, value.arguments, policy))
        beginOperation(b.toolTimeout(), RunError.BUDGET_EXCEEDED, RunError.HOST_UNAVAILABLE,
            { callback -> tools.prepare(invocation, b.toolTimeout(), callback) }) { outcome ->
            when (outcome) {
                is PortResult.Failure -> toolFailed(outcome.error, unknownPassword = true, scriptParameters = outcome.scriptParameters, mcpReason = outcome.mcpReason)
                is PortResult.Success -> {
                    val prepared = outcome.value
                    if (prepared.invocation !== invocation) { finishError(RunError.INVALID_REQUEST); return@beginOperation }
                    prepared.metadata.script?.let { decision = value.copy(arguments = it.arguments()) }
                    if (prepared.metadata.passwordField) protectText()
                    val spec = policy.requireEnabled(catalog, value.name)
                    if (!spec.readOnlyHint && (prepared.metadata.payment || policy.isPayment(prepared.metadata.context))) doneRules.requireOrderStatus()
                    if (!loopRules.admit(spec, prepared)) {
                        record(jsonObject("rule" to "REPEATED_ACTION".json(), "limit" to LoopRules.REPEAT_LIMIT.json()).toString())
                        val reason = text.rule("repeated_action")
                        finish(RunState.BLOCKED, reason, unfinished = listOf(reason))
                        return@beginOperation
                    }
                    val assessment = gate.assess(spec, prepared.metadata)
                    if (assessment.required) waitForConfirmation(prepared, spec, assessment)
                    else { confirmation = "auto"; executeTool(prepared) }
                }
            }
        }
    }

    private fun executeTool(prepared: PreparedTool) {
        if (!canContinue()) return
        if (!checkNotNull(catalog[prepared.invocation.name]).readOnlyHint) observationImages = emptyList()
        if (prepared.invocation.name == ToolNames.SCREEN_CAPTURE && nativeResults.sumOf { it.images.size } >= 4) throw ContextLimitExceeded()
        val b = checkNotNull(budget)
        // An inspection cannot smuggle a longer non-script operation through script metadata.
        val scriptTimeout = if (prepared.invocation.name in setOf(ToolNames.SCRIPT_RUN, ToolNames.SCRIPT_RUN_SOURCE)) prepared.metadata.scriptTimeoutMs else null
        val timeout = b.toolTimeout(scriptTimeout)
        if (prepared.invocation.name == ToolNames.REPORT_PROGRESS) {
            emit("progress", jsonObject("step" to b.steps.json(), "message" to (prepared.invocation.arguments["message"] ?: "".json()), "budget" to b.remainingJson()))
        }
        if (!canContinue()) return
        loopRules.started(checkNotNull(catalog[prepared.invocation.name]))
        b.beginTool()
        when (prepared.invocation.name) {
            ToolNames.SCRIPT_RUN, ToolNames.SCRIPT_RUN_SOURCE -> { scriptCalls++; scriptResult = null }
            ToolNames.SCRIPT_CATALOG, ToolNames.REPORT_PROGRESS -> Unit
            else -> otherActions++
        }
        beginOperation(timeout, if (prepared.invocation.name in setOf(ToolNames.SCRIPT_RUN, ToolNames.SCRIPT_RUN_SOURCE)) RunError.SCRIPT_TIMEOUT else RunError.BUDGET_EXCEEDED,
            RunError.HOST_UNAVAILABLE, { callback -> tools.execute(prepared, timeout, callback) }) { outcome ->
            when (outcome) {
                is PortResult.Failure -> toolFailed(outcome.error, mcpReason = outcome.mcpReason)
                is PortResult.Success -> {
                    require(outcome.value.images.isEmpty() || (prepared.invocation.name == ToolNames.SCREEN_CAPTURE && policy.visionAvailable))
                    require(prepared.invocation.name != ToolNames.SCREEN_CAPTURE || outcome.value.images.size == 1)
                    observationImages = outcome.value.images
                    val toolError = outcome.value.error ?: outcome.value.script?.error
                    if (toolError == null) successfulTools++
                    if (prepared.invocation.name == ToolNames.SCRIPT_RUN) scriptResult = outcome.value.script?.scriptResult
                    if (toolError == null) loopRules.succeeded(checkNotNull(catalog[prepared.invocation.name]), outcome.value.result)
                    else loopRules.failed(checkNotNull(catalog[prepared.invocation.name]))
                    val redacted = journal.redact(outcome.value.result)
                    observation = if (toolError == null) compiler.observe(prepared.invocation.name, redacted)
                        else compiler.observeFailure(prepared.invocation.name, redacted, toolError)
                    record(observation, toolError, outcome.value.images)
                    nextStep()
                }
            }
        }
    }

    private fun toolFailed(error: RunError, unknownPassword: Boolean = false,
                           scriptParameters: io.github.supermonster003.autojs6.plugin.three.stove.agent.scripts.ScriptParameterProblem? = null, mcpReason: String? = null) {
        if (unknownPassword) protectText()
        (decision as? AgentDecision.Tool)?.let { loopRules.failed(checkNotNull(catalog[it.name])) }
        if (error.hostLost || error == RunError.BUDGET_EXCEEDED) {
            finishError(error, budgetDimension = "toolTimeout".takeIf { error == RunError.BUDGET_EXCEEDED },
                detail = if (error == RunError.BUDGET_EXCEEDED) budgetDetail("toolTimeout") else null); return
        }
        observation = scriptParameters?.observation() ?: jsonObject("error" to error.name.json()).apply {
            if (error == RunError.A11Y_SERVICE_NOT_RUNNING) addProperty("hint",
                "AutoJs6 accessibility is not running and automatic startup failed or is not configured. Ask the user to enable it, then observe again before acting.")
            mcpReason?.let { addProperty("reason", it) }
            if (mcpReason == "MCP_CATALOG_CHANGED") addProperty("hint",
                "The MCP server changed a selected tool definition, so its tools stay unavailable for the rest of this task. Ask the user to refresh the server's tool selection in MCP settings and start a new task; do not retry the call.")
        }.toString()
        observationImages = emptyList()
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
    /** Plan mode asks for the plan without native tools; once a plan is accepted the model's own format (native tools when offered) takes over. */
    private fun startingFormat(): DecisionFormat {
        planRequired = options.format.planMode
        return if (planRequired) model.planningFormat(options.format) else model.initialFormat(options.format)
    }
    private fun waitForPlan(plan: AgentDecision.Plan) {
        if (!canContinue()) return
        val timeout = minOf(options.limits.askTimeoutMs, checkNotNull(budget).remainingMs)
        val waiting = installInteraction(timeout, plan = plan)
        transition(RunState.WAITING_INPUT)
        emit("input", jsonObject("requestId" to waiting.id.json(), "kind" to "plan".json(), "question" to text.rule("plan_question").json(),
            "steps" to JsonArray().apply { plan.steps.forEach(::add) }, "timeoutMs" to timeout.json()))
    }
    /** The reviewed plan, possibly edited by the user, replaces the proposal; nothing was executed meanwhile. */
    private fun acceptPlan(proposed: AgentDecision.Plan, value: JsonArray) {
        clearInteraction()
        transition(RunState.RUNNING)
        val steps = value.map { it.asString.trim() }
        approvedPlan = steps; planRequired = false
        format = model.initialFormat(options.format)
        observation = ToolObservation.success(jsonObject("plan" to JsonArray().apply { steps.forEach(::add) }, "approved" to true.json(),
            "edited" to (steps != proposed.steps).json()))
        observationImages = emptyList()
        record(observation)
        guarded { nextStep() }
    }
    private fun validPlan(value: JsonElement): Boolean = value.isJsonArray && value.asJsonArray.size() in 1..AgentDecision.Plan.MAX_STEPS &&
        value.asJsonArray.all { item -> item.isJsonPrimitive && item.asJsonPrimitive.isString && item.asString.isNotBlank() &&
            item.asString.trim().let { it.codePointCount(0, it.length) <= AgentDecision.Plan.MAX_STEP_CHARACTERS && runCatching { AgentJson.checkUnicode(it) }.isSuccess } }
    private fun waitForConfirmation(prepared: PreparedTool, spec: ToolSpec, assessment: ConfirmationAssessment) {
        if (!canContinue()) return
        val proposed = gate.arguments(prepared.invocation.arguments, prepared.metadata)
        val arguments = journal.redact(proposed)
        // Never ask users to approve a redacted preview while executing different source bytes.
        if (spec.name == ToolNames.SCRIPT_RUN_SOURCE && proposed != arguments) {
            toolFailed(RunError.TOOL_ARGUMENTS_INVALID); return
        }
        val timeout = minOf(options.limits.confirmationTimeoutMs, checkNotNull(budget).remainingMs)
        val waiting = installInteraction(timeout, tool = prepared, assessment = assessment)
        transition(RunState.WAITING_CONFIRMATION)
        // Script parameters and memory values are bounded. Approval must display the full proposed change.
        val summary = if (prepared.metadata.script != null || prepared.metadata.memoryScope != null || spec.name == ToolNames.SCRIPT_RUN_SOURCE) arguments else StepJournal.clipped(arguments, 4096)
        emit("confirmation", jsonObject("requestId" to waiting.id.json(), "tool" to spec.name.json(),
            "description" to gate.description(spec, prepared.metadata, options.locale).json(), "risk" to assessment.risk.name.lowercase(Locale.ROOT).json(),
            "arguments" to summary, "allowRunScope" to assessment.allowRunScope.json(), "timeoutMs" to timeout.json()))
    }
    private fun installInteraction(timeout: Long, ask: AgentDecision.Ask? = null, tool: PreparedTool? = null,
                                   assessment: ConfirmationAssessment? = null, plan: AgentDecision.Plan? = null): Interaction {
        val waiting = Interaction("$id:${++requestSequence}", scheduler.nowMs() + timeout, ask, tool, assessment, plan)
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
        observationImages = emptyList()
        record(observation, RunError.USER_TIMEOUT)
        nextStep()
    }
    private fun rejected(error: RunError) {
        confirmation = "denied"
        observation = errorObservation(error)
        observationImages = emptyList()
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
                                   onDiscard: (PortResult<T>) -> Unit = {},
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
                if (state.terminal) safely { onDiscard(outcome) }
                else try {
                    scheduler.execute {
                        var consumed = false
                        try {
                            guarded {
                                if (operation === pending && pending.active) {
                                    clearOperation(cancel = false)
                                    consumed = true
                                    accept(outcome)
                                }
                            }
                        } finally {
                            if (!consumed) safely { onDiscard(outcome) }
                        }
                    }
                } catch (error: RejectedExecutionException) { safely { onDiscard(outcome) }; if (!state.terminal) throw error }
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
        catch (error: BudgetExceeded) { finishError(RunError.BUDGET_EXCEEDED, error.dimension, detail = budgetDetail(error.dimension)) }
        catch (_: ContextLimitExceeded) { finishError(RunError.LIMIT_EXCEEDED, detail = "key:limit_context") }
        catch (error: Exception) {
            // A programming error is indistinguishable from a bad request on the public surface; keep the class for diagnosis.
            internalFailure = error.javaClass.simpleName.ifBlank { "Exception" }
            finishError(RunError.INVALID_REQUEST, detail = internalFailure)
        }
    }
    private fun record(value: String?, error: RunError? = null, images: List<ModelImage> = emptyList()) {
        if (recorded) return
        val current = decision
        val rejections = repairSession?.rejections.orEmpty() +
            if (responseLimitExceeded) listOf(DecisionRejection.LIMIT_EXCEEDED) else emptyList()
        if (current == null && rejections.isEmpty() && internalFailure == null) return
        val b = checkNotNull(budget)
        if (b.steps < 1) return // Failed before the first step: the result carries the error code.
        recorded = true
        val usage = b.usageJson().apply {
            for (key in listOf("modelCalls", "inputTokens", "outputTokens", "totalTokens")) addProperty(key, number(key)!! - (stepUsageStart.number(key) ?: 0))
            addProperty("estimated", stepEstimated)
        }
        // An error record contains validator metadata, never a fabricated or rejected model decision.
        val data = (current?.let(StepJournal::decision) ?: jsonObject("kind" to "error".json(),
            "source" to (if (internalFailure != null && rejections.isEmpty()) "runtime" else "validator").json())).apply {
            if (userRequestedStep) addProperty("source", "user")
            parseMode?.let { addProperty("parseMode", it.name) }
            addProperty("repairs", repairSession?.repairsUsed ?: 0)
            addProperty("degraded", format.degraded)
            internalFailure?.let { addProperty("failure", it) }
        }
        val entry = journal.append(StepRecord(b.steps, data.string("kind")!!, data,
            (current as? AgentDecision.Tool)?.name, (current as? AgentDecision.Tool)?.arguments,
            confirmation, value, usage, (scheduler.nowMs() - stepStartedMs).coerceAtLeast(0), error?.name, rejections))
        activeNativeCall?.let { call ->
            nativeResults += nativeResult(call, value ?: errorObservation(RunError.INVALID_REQUEST), error != null, images)
            activeNativeCall = null
        }
        emit("step", entry)
    }
    private fun nativeResult(call: NativeToolCall, value: String, isError: Boolean, images: List<ModelImage> = emptyList()): NativeToolResult {
        val turn = checkNotNull(nativeTurn)
        val limits = turn.continuation.limits
        // Include verification and remaining allowances in the actual continuation. Bound the JSON
        // string after escaping so even a 32-call batch stays inside the negotiated envelope limit.
        val result = AgentJson.objectOf(value, ToolObservation.DEFAULT_MAX_BYTES).apply {
            add("remaining_budget", checkNotNull(budget).remainingJson())
            add("verification", verificationGuidance())
        }
        val allowance = (limits.batchBytes - 256) / turn.calls.size
        var maximum = minOf(ToolObservation.DEFAULT_MAX_BYTES, limits.resultBytes, allowance - 256)
        while (maximum >= 128) {
            val compacted = ObservationCompactor.compact(result, maximum, false).toString()
            val output = NativeToolResult(call.id, compacted, isError, images)
            if (StepJournal.bytes(output.wire()) <= allowance) return output
            maximum /= 2
        }
        throw ContextLimitExceeded()
    }
    private fun closeNative(): PortResult.Failure? {
        val current = nativeTurn?.continuation
        nativeTurn = null; nativeQueue.clear(); nativeResults.clear(); activeNativeCall = null
        return current?.let {
            safely(it::cancel)
            val progress = it.takeProgress()
            budget?.settleProgress(progress.usage, progress.outputBytes)
            progress
        }
    }
    private fun protectText() {
        (decision as? AgentDecision.Tool)?.takeIf { it.name == ToolNames.UI_SET_TEXT }?.arguments?.string("text")?.let(journal::protectText)
    }
    private fun finishStop(error: RunError) {
        if (state.terminal) return
        if (error == RunError.CANCELLED && state != RunState.QUEUED) transition(RunState.CANCELLING)
        finishError(error)
    }
    /** "used/limit" for a budget dimension, so the summary says which limit and how far it went. */
    private fun budgetDetail(dimension: String): String? {
        val b = budget ?: return null
        return when (dimension) {
            "steps" -> "${b.steps}/${b.limits.maxSteps}"
            "modelCalls" -> "${b.modelCalls}/${b.limits.maxModelCalls}"
            "duration" -> "${b.durationMs / 1000} s/${b.limits.maxDurationMs / 1000} s"
            "tokens" -> "${b.totalTokens}/${b.tokenLimit}"
            "toolTimeout" -> "${b.limits.stepToolTimeoutMs / 1000} s"
            else -> null
        }
    }
    private fun finishError(error: RunError, budgetDimension: String? = null, mcpReason: String? = null, detail: String? = null) {
        if (state.terminal) return
        clearOperation(cancel = true); clearInteraction()
        closeNative()
        observationImages = emptyList()
        budget?.abandonModel()
        protectText() // Also protects an unresolved/password-unknown text operation on cancellation.
        record(errorObservation(error), error)
        val terminal = when {
            error == RunError.CANCELLED -> RunState.CANCELLED
            error.hostLost -> RunState.BLOCKED
            error == RunError.BUDGET_EXCEEDED && successfulTools > 0 -> RunState.PARTIAL
            else -> RunState.FAILED
        }
        val cause = mcpReason?.takeIf { error == RunError.TOOL_FAILED && it in PortResult.Failure.MCP_REASONS } ?: detail
        finish(terminal, text.terminal(error, budgetDimension, cause), error = error)
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
        closeNative()
        val resources = cleanup; cleanup = Cancellation.NONE; safely(resources::cancel)
        observationImages = emptyList()
        val b = budget
        val value = jsonObject("id" to id.json(), "status" to terminal.wire.json(), "summary" to summary.json(),
            "steps" to (b?.steps ?: 0).json(), "toolCalls" to (b?.toolCalls ?: 0).json(),
            "usage" to (b?.usageJson() ?: jsonObject("modelCalls" to 0.json(), "estimated" to false.json())),
            "durationMs" to (b?.durationMs ?: 0).json(), "evidence" to JsonArray().apply { evidence.forEach(::add) },
            "unfinished" to JsonArray().apply {
                (if (terminal == RunState.PARTIAL && unfinished.isEmpty()) listOf(text.rule("unfinished_missing")) else unfinished).forEach(::add)
            })
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
        val event = RunEvent(id, ++eventSequence, type, payload,
            if (type in setOf("input", "confirmation")) interaction?.deadlineMs else null)
        safely { listener(event) } // A detached observer cannot abort an owned task.
    }
    private inline fun safely(action: () -> Unit) { try { action() } catch (_: Exception) { /* No private exception text in ordinary logs. */ } }
}
