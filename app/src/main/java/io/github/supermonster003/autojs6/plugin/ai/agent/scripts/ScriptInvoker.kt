package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ScriptExecutionTools(private val delegate: RunTools, private val invoker: ScriptInvoker) : RunTools {
    override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit) = delegate.prepare(invocation, timeoutMs, callback)
    override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit) =
        if (prepared.invocation.name == "script_run") invoker.execute(prepared, timeoutMs, callback) else delegate.execute(prepared, timeoutMs, callback)
}

/** Stop addresses this invocation even before the host has assigned an execution ID. */
class ScriptInvoker(private val source: ScriptCatalogSource, private val runId: () -> String, private val preset: String) {
    fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
        val script = prepared.opaqueContext as? PreparedScript
        if (script == null || script !== prepared.metadata.script) {
            callback(PortResult.Failure(RunError.INVALID_REQUEST)); return Cancellation.NONE
        }
        val id = UUID.randomUUID().toString()
        val stopped = AtomicBoolean()
        val terminal = AtomicBoolean()
        val handle = AtomicReference(Cancellation.NONE)
        fun stopOwned() {
            if (!stopped.compareAndSet(false, true)) return
            runCatching { source.load(ToolHandlers.bridge("engines.stop", jsonArray(jsonObject("agentInvocationId" to id.json())), 5000)) {} }
        }
        val options = jsonObject("captureConsole" to true.json(), "timeoutMs" to timeoutMs.json(), "agentInvocationId" to id.json(),
            "agentRunId" to runId().json(), "presetName" to preset.json(), "expectedManifest" to script.registration.snapshot())
        try {
            val pending = source.load(ToolHandlers.bridge("agent.execRegistered",
                jsonArray(script.registration.path.json(), script.parameters, options), timeoutMs)) { reply ->
                if (terminal.compareAndSet(false, true)) when (reply) {
                    is PortResult.Failure -> { stopOwned(); callback(reply) }
                    is PortResult.Success -> {
                        val parsed = runCatching { ScriptOutcome.parse(script, reply.value) }.getOrNull()
                        if (parsed == null) { stopOwned(); callback(PortResult.Failure(RunError.SCRIPT_FAILED)) }
                        else {
                            if (parsed.error != null || !parsed.finished) stopOwned()
                            callback(PortResult.Success(ToolReply(parsed.observation, parsed)))
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

class ScriptOutcome private constructor(observation: JsonObject, val error: RunError?, val finished: Boolean, summary: JsonObject?) {
    private val data = observation.deepCopy()
    private val summary = summary?.deepCopy()
    val observation: JsonObject get() = data.deepCopy()
    val scriptResult: JsonObject? get() = summary?.deepCopy()
    override fun toString() = "ScriptOutcome(error=$error, finished=$finished)"
    companion object {
        fun parse(script: PreparedScript, raw: JsonElement): ScriptOutcome {
            val value = AgentJson.objectOf(raw.toString(), 256 * 1024)
            val outcome = value.string("outcome")
            require(outcome in setOf("success", "exception", "stopped", "timeout"))
            val finished = requireNotNull(value.flag("finished"))
            val executionId = value["executionId"]?.takeUnless { it.isJsonNull }?.let { requireNotNull(value.number("executionId")) }
            require(executionId == null || executionId >= 0)
            val reported = requireNotNull(value.flag("resultReported"))
            val result = requireNotNull(value["result"])
            require(StepJournal.bytes(result) <= 64 * 1024 && (reported || result.isJsonNull))
            require(outcome != "success" || finished && executionId != null)
            val error = when (outcome) { "timeout" -> RunError.SCRIPT_TIMEOUT; "stopped" -> RunError.CANCELLED; "exception" -> RunError.SCRIPT_FAILED; else -> null }
            val tail = requireNotNull(value["consoleTail"]?.takeIf { it.isJsonArray }?.asJsonArray)
            require(tail.all { it.isJsonPrimitive && it.asJsonPrimitive.isString })
            val secrets = script.parameters.entrySet().mapNotNull { (_, v) -> v.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.takeIf { it.isNotEmpty() } }
            val console = JsonArray()
            val lines = ArrayDeque<String>()
            tail.forEach { entry -> entry.asString.lineSequence().forEach { line ->
                lines.add(line)
                if (lines.size > 40) lines.removeFirst()
            } }
            var remaining = 8192
            for (line in lines.toList().asReversed()) {
                val redacted = ScriptOutputRedactor.text(line, secrets)
                val bounded = AgentJson.truncate(redacted, minOf(1024, remaining / 6))
                if (bounded.isEmpty() && redacted.isNotEmpty()) break
                val cost = StepJournal.bytes(bounded.json()) + 1
                if (cost > remaining) break
                console.add(bounded); remaining -= cost
            }
            val observation = jsonObject("outcome" to outcome!!.json(), "finished" to finished.json(),
                "result" to ScriptOutputRedactor.redact(result), "resultReported" to reported.json(),
                "consoleTail" to JsonArray().apply { console.toList().asReversed().forEach(::add) },
                "error" to (error?.let { jsonObject("code" to it.name.json()) } ?: JsonNull.INSTANCE)).apply {
                add("executionId", executionId?.json() ?: JsonNull.INSTANCE)
                value.string("consoleCaptureMode")?.takeIf { it == "global-window" }?.let { addProperty("consoleCaptureMode", it) }
            }
            val summary = if (reported && executionId != null) jsonObject("id" to script.registration.id.json(), "path" to script.registration.path.json(),
                "executionId" to executionId.json(), "result" to ScriptOutputRedactor.redact(result)) else null
            return ScriptOutcome(observation, error, finished, summary)
        }
    }
}

internal object ScriptOutputRedactor {
    private val key = Regex("(?i)^(?:password|passwd|pwd|token|access[_-]?token|refresh[_-]?token|api[_-]?key|secret|authorization|cookie)$")
    private val assignment = Regex("""(?i)(password|passwd|pwd|(?:access[_-]?|refresh[_-]?)?token|api[_-]?key|secret|authorization|cookie)(["']?\s*[:=]\s*)(?:"[^"]*"|'[^']*'|[^\s,;]+)""")
    fun text(value: String, secrets: List<String> = emptyList()): String {
        var result = value
        for (secret in secrets.sortedByDescending { it.length }) result = result.replace(secret, "***").replace(secret.json().toString().drop(1).dropLast(1), "***")
        return assignment.replace(result) { it.groupValues[1] + it.groupValues[2] + "***" }
            .replace(Regex("(?i)Bearer\\s+[A-Za-z0-9._~+/-]+=*"), "Bearer ***")
    }
    fun redact(value: JsonElement): JsonElement = when {
        value.isJsonObject -> JsonObject().apply { value.asJsonObject.entrySet().forEach { (name, child) -> add(name, if (key.matches(name)) "***".json() else redact(child)) } }
        value.isJsonArray -> JsonArray().apply { value.asJsonArray.forEach { add(redact(it)) } }
        value.isJsonPrimitive && value.asJsonPrimitive.isString -> text(value.asString).json()
        else -> value.deepCopy()
    }
}
