package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** The complete source must fit both the confirmation event and each retained private step. */
object DynamicScriptSource {
    const val MAX_BYTES = 8192
    fun validate(source: String): String {
        AgentJson.checkUnicode(source)
        require(source.isNotBlank() && '\u0000' !in source)
        require(source.toByteArray(Charsets.UTF_8).size <= MAX_BYTES && StepJournal.bytes(source.json()) <= MAX_BYTES)
        return source
    }
    fun available(optional: Set<String>, methods: Set<String>, permissions: Set<String>) =
        "engines.execScript" in optional && methods.containsAll(setOf("engines.execScript", "engines.stop")) &&
            permissions.containsAll(setOf("engines", "engines.exec", "agent", "agent.exec"))
}

/** Inline scripts have the host runtime's permissions, not a JavaScript sandbox. */
class DynamicScriptTools(private val delegate: RunTools, private val source: ScriptCatalogSource,
                         private val allowed: Boolean, private val runId: () -> String, private val preset: String,
                         private val maximumTimeoutMs: Long = RunLimits.TOOL_TIMEOUT_MS) : RunTools {
    private class Inspected(val plan: ToolPlan.DynamicScript)
    override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
        if (invocation.name != ToolNames.SCRIPT_RUN_SOURCE) return delegate.prepare(invocation, timeoutMs, callback)
        if (!allowed) callback(PortResult.Failure(RunError.CAPABILITY_DENIED))
        else {
            val plan = invocation.plan as? ToolPlan.DynamicScript
            if (plan == null || runCatching { DynamicScriptSource.validate(plan.source) }.isFailure)
                callback(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID))
            else callback(PortResult.Success(PreparedTool(invocation, ToolMetadata(forceConfirmation = true,
                scriptTimeoutMs = minOf(plan.timeoutMs, maximumTimeoutMs)), Inspected(plan))))
        }
        return Cancellation.NONE
    }
    override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
        if (prepared.invocation.name != ToolNames.SCRIPT_RUN_SOURCE) return delegate.execute(prepared, timeoutMs, callback)
        val inspected = prepared.opaqueContext as? Inspected
        if (!allowed || inspected == null || inspected.plan != prepared.invocation.plan) {
            callback(PortResult.Failure(RunError.INVALID_REQUEST)); return Cancellation.NONE
        }
        val id = UUID.randomUUID().toString()
        val terminal = AtomicBoolean()
        val stopped = AtomicBoolean()
        val handle = AtomicReference(Cancellation.NONE)
        fun stopOwned() {
            if (stopped.compareAndSet(false, true)) runCatching {
                source.load(ToolHandlers.bridge("engines.stop", jsonArray(jsonObject("agentInvocationId" to id.json())), 5000)) {}
            }
        }
        val timeout = minOf(timeoutMs, maximumTimeoutMs, inspected.plan.timeoutMs)
        val options = jsonObject("agentInvocationId" to id.json(), "agentRunId" to runId().json(), "presetName" to preset.json(),
            "timeoutMs" to timeout.json(), "captureConsole" to true.json())
        try {
            val pending = source.load(ToolHandlers.bridge("engines.execScript",
                jsonArray("agent-generated.js".json(), inspected.plan.source.json(), options), timeout)) { result ->
                if (terminal.compareAndSet(false, true)) when (result) {
                    is PortResult.Failure -> { stopOwned(); callback(result) }
                    is PortResult.Success -> {
                        val outcome = runCatching { ScriptOutcome.parse(null, result.value) }.getOrNull()
                        if (outcome == null) { stopOwned(); callback(PortResult.Failure(RunError.SCRIPT_FAILED)) }
                        else {
                            if (outcome.error != null || !outcome.finished) stopOwned()
                            callback(PortResult.Success(ToolReply(outcome.observation, outcome)))
                        }
                    }
                }
            }
            handle.set(pending)
            if (stopped.get()) pending.cancel()
        } catch (_: Exception) {
            if (terminal.compareAndSet(false, true)) { stopOwned(); callback(PortResult.Failure(RunError.HOST_UNAVAILABLE)) }
        }
        return Cancellation { if (terminal.compareAndSet(false, true)) { stopOwned(); handle.get().cancel() } }
    }
}
