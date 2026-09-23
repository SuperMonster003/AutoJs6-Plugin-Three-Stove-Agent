package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*

/** Immutable inspected registration and effective arguments, shared by confirmation and P3.3 execution. */
class PreparedScript internal constructor(val registration: RegisteredScript, parameters: JsonObject) {
    private val data = parameters.deepCopy()
    val parameters: JsonObject get() = data.deepCopy()
    fun arguments() = jsonObject("id" to registration.id.json(), "parameters" to parameters)
    override fun toString() = "PreparedScript(parameterCount=${data.size()})"
}

/** Resolves model IDs only inside the task's catalog, then reads fresh registration by canonical path. */
class RegisteredScriptTools(private val client: ScriptCatalogClient, roots: Set<String>, private val source: ScriptCatalogSource,
                            private val delegate: RunTools, private val validator: DecisionValidator,
                            private val allowed: Boolean, private val nowMs: () -> Long) : RunTools {
    private val roots = roots.toSet()
    override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
        if (invocation.name != "script_run") return delegate.prepare(invocation, timeoutMs, callback)
        if (!allowed) { callback(PortResult.Failure(RunError.CAPABILITY_DENIED)); return Cancellation.NONE }
        val operation = Admission(callback)
        val deadline = nowMs() + timeoutMs
        val id = invocation.arguments.string("id")
        val plan = invocation.plan as? ToolPlan.RegisteredScript
        if (id == null || plan == null) { operation.finish(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID)); return operation }
        operation.launch({ reply -> client.load(roots, false, timeoutMs, source, reply) }) { listed ->
            when (listed) {
                is PortResult.Failure -> operation.finish(listed)
                is PortResult.Success -> {
                    val entry = listed.value.entries.singleOrNull { it.id == id }
                    if (entry == null) operation.finish(PortResult.Failure(RunError.SCRIPT_NOT_REGISTERED))
                    else if (nowMs() >= deadline) operation.finish(PortResult.Failure(RunError.BUDGET_EXCEEDED))
                    else operation.launch<JsonElement>({ reply -> source.load(plan.manifest.copy(args = jsonArray(entry.path.json()),
                        timeoutMs = minOf(plan.manifest.timeoutMs, deadline - nowMs())), reply) }) { loaded ->
                        when (loaded) {
                            is PortResult.Failure -> operation.finish(loaded)
                            is PortResult.Success -> {
                                val fresh = runCatching { RegisteredScript.parse(loaded.value) }.getOrNull()
                                if (fresh == null || fresh.id != entry.id || fresh.path != entry.path) operation.finish(PortResult.Failure(RunError.SCRIPT_NOT_REGISTERED))
                                else try {
                                    when (val checked = validator.validateScriptParameters(fresh, invocation.arguments.getAsJsonObject("parameters"))) {
                                        is ScriptParameterCheck.Invalid -> operation.finish(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID, scriptParameters = checked.problem))
                                        is ScriptParameterCheck.Valid -> {
                                            val script = PreparedScript(fresh, checked.parameters)
                                            val risk = when (fresh.risk) { "readonly" -> RiskLevel.READ_ONLY; "sensitive" -> RiskLevel.SENSITIVE; else -> RiskLevel.NORMAL }
                                            operation.finish(PortResult.Success(PreparedTool(invocation, ToolMetadata(
                                                context = RiskContext(registeredScriptRisk = risk),
                                                forceConfirmation = fresh.confirm == "before-run" || risk == RiskLevel.SENSITIVE,
                                                scriptTimeoutMs = fresh.snapshot().number("timeoutMs"), script = script), script)))
                                        }
                                    }
                                } catch (_: Exception) { operation.finish(PortResult.Failure(RunError.SCRIPT_NOT_REGISTERED)) }
                            }
                        }
                    }
                }
            }
        }
        return operation
    }

    // P3.3 supplies ScriptInvoker. Until then the production bridge returns TOOL_DISABLED for this plan.
    override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit) = delegate.execute(prepared, timeoutMs, callback)

    private class Admission(private val callback: (PortResult<PreparedTool>) -> Unit) : Cancellation {
        private val lock = Any()
        private var stopped = false
        private var phase = 0
        private var current = Cancellation.NONE
        fun finish(value: PortResult<PreparedTool>) {
            val deliver = synchronized(lock) { if (stopped) false else { stopped = true; true } }
            if (deliver) callback(value)
        }
        fun <T> launch(start: ((PortResult<T>) -> Unit) -> Cancellation, receive: (PortResult<T>) -> Unit) {
            val token = synchronized(lock) { if (stopped) return else ++phase }
            val handle = try { start { value ->
                val deliver = synchronized(lock) { if (stopped || phase != token) false else { phase++; true } }
                if (deliver) receive(value)
            } }
                catch (_: Exception) { finish(PortResult.Failure(RunError.HOST_UNAVAILABLE)); Cancellation.NONE }
            val cancel = synchronized(lock) { if (stopped || phase != token) true else { current = handle; false } }
            if (cancel) handle.cancel()
        }
        override fun cancel() {
            val handle = synchronized(lock) { if (stopped) return else { stopped = true; current } }
            handle.cancel()
        }
    }
}
