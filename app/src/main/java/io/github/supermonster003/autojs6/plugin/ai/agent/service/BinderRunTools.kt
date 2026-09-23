package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.os.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptCatalogSnapshot
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.autojs.plugin.host.capability.api.IHostCapabilityBroker
import org.autojs.plugin.host.capability.api.IHostCapabilityCallback
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Small bridge execution adapter for P2.5. P3/P4 add registered scripts, append/readback and UI recovery.
 * Until P4 supplies identity-bound risk inspection, every device mutation requires per-action confirmation. */
internal class BinderRunTools(private val broker: IHostCapabilityBroker, private val ownerUid: Int,
                              private val workers: LinkWorkers, private val scheduler: RunScheduler, private val catalog: ToolCatalog,
                              private val alive: () -> Boolean, private val methods: Set<String>, private val permissions: Set<String>,
                              private val maximumRequestBytes: Int, private val maximumTimeoutMs: Long) : RunTools {
    override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
        if (!alive()) callback(PortResult.Failure(RunError.HOST_UNAVAILABLE))
        else if (invocation.name !in IMPLEMENTED || invocation.plan is ToolPlan.AppendText) callback(PortResult.Failure(RunError.TOOL_DISABLED))
        else callback(PortResult.Success(PreparedTool(invocation, ToolMetadata(
            passwordField = invocation.name == "ui_set_text", forceConfirmation = catalog[invocation.name]?.readOnlyHint == false))))
        return Cancellation.NONE
    }
    override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
        val cancelled = AtomicBoolean()
        val current = AtomicReference<Cancellation>(Cancellation.NONE)
        val end = scheduler.nowMs() + timeoutMs
        fun finish(value: PortResult<ToolReply>) { if (cancelled.compareAndSet(false, true)) callback(value) }
        fun call(request: BridgeCall, receive: (PortResult<JsonElement>) -> Unit) {
            if (cancelled.get()) return
            if (!alive()) { finish(PortResult.Failure(RunError.HOST_UNAVAILABLE)); return }
            val remaining = (end - scheduler.nowMs()).coerceAtLeast(1)
            val next = dispatch(request.copy(timeoutMs = minOf(request.timeoutMs, maximumTimeoutMs, remaining))) { value ->
                runCatching { scheduler.execute { if (!cancelled.get()) receive(value) } }
            }
            current.set(next)
            if (cancelled.get()) next.cancel()
        }
        fun success(value: JsonElement) {
            try { finish(PortResult.Success(ToolReply(value))) } catch (_: Exception) { finish(PortResult.Failure(RunError.LIMIT_EXCEEDED)) }
        }
        when (val plan = prepared.invocation.plan) {
            is ToolPlan.Call -> call(plan.request) { value -> when (value) {
                is PortResult.Failure -> finish(value)
                is PortResult.Success -> success(if (plan.resultLimit != null && value.value.isJsonArray)
                    JsonArray().apply { value.value.asJsonArray.take(plan.resultLimit).forEach(::add) } else value.value)
            } }
            is ToolPlan.Poll -> {
                val deadline = minOf(end, scheduler.nowMs() + plan.deadlineMs)
                fun poll() {
                    call(plan.request) { value -> when (value) {
                        is PortResult.Failure -> finish(value)
                        is PortResult.Success -> {
                            val present = !value.value.isJsonNull && value.value != false.json()
                            if (present == (plan.state == "appear")) success(jsonObject("matched" to true.json(), "state" to plan.state.json(), "node" to value.value))
                            else if (scheduler.nowMs() >= deadline) finish(PortResult.Failure(RunError.NODE_NOT_FOUND))
                            else {
                                val timer = scheduler.schedule(minOf(plan.intervalMs, deadline - scheduler.nowMs()).coerceAtLeast(1)) { if (!cancelled.get()) poll() }
                                current.set(timer); if (cancelled.get()) timer.cancel()
                            }
                        }
                    } }
                }
                poll()
            }
            is ToolPlan.Repeat -> {
                var count = 0
                fun repeatCall() { call(plan.request) { value -> when (value) {
                    is PortResult.Failure -> finish(value)
                    is PortResult.Success -> if (value.value == false.json() || ++count >= plan.times) success(value.value) else repeatCall()
                } } }
                repeatCall()
            }
            is ToolPlan.Local -> when (plan.name) {
                "report_progress" -> success(jsonObject("reported" to true.json()))
                else -> finish(PortResult.Failure(RunError.TOOL_DISABLED))
            }
            else -> finish(PortResult.Failure(RunError.TOOL_DISABLED))
        }
        return Cancellation { cancelled.set(true); current.get().cancel() }
    }

    internal fun dispatch(call: BridgeCall, callback: (PortResult<JsonElement>) -> Unit): Cancellation {
        val id = "tool-${UUID.randomUUID()}"
        val isScriptCatalog = call.module == "agent" && call.method == "listScripts"
        val maximumPayloadBytes = if (isScriptCatalog) ScriptCatalogSnapshot.MAX_BYTES else 512 * 1024
        val maximumNodes = if (isScriptCatalog) ScriptCatalogSnapshot.MAX_NODES else 16_384
        val closed = AtomicBoolean()
        val claimed = AtomicBoolean()
        val payload = AtomicReference<OwnedJson?>()
        fun result(value: PortResult<JsonElement>) { if (closed.compareAndSet(false, true)) callback(value) }
        if ("${call.module}.${call.method}" !in methods || !permissions.containsAll(call.permissions)) {
            result(PortResult.Failure(RunError.CAPABILITY_DENIED)); return Cancellation.NONE
        }
        val json = call.envelope(id).toString()
        if (json.toByteArray(Charsets.UTF_8).size > maximumRequestBytes) { result(PortResult.Failure(RunError.LIMIT_EXCEEDED)); return Cancellation.NONE }
        val remote = object : IHostCapabilityCallback.Stub() {
            @Suppress("DEPRECATION")
            override fun onResponse(response: Bundle?) {
                if (Binder.getCallingUid() != ownerUid || closed.get() || !claimed.compareAndSet(false, true)) { AgentWire.closeDescriptors(response); return }
                var owned: OwnedJson? = null
                try {
                    // Shared V1 responses retain the released MCP/Node KEY_BRIDGE_* envelope.
                    // The brokerInfo handshake negotiates the version; legacy replies omit it.
                    require(response != null && (!response.containsKey(H.KEY_CONTRACT_VERSION) || response.get(H.KEY_CONTRACT_VERSION) == H.CONTRACT_VERSION))
                    val text = response.get(H.KEY_BRIDGE_RESPONSE_JSON) as? String ?: error("Missing response")
                    require(text.toByteArray(Charsets.UTF_8).size <= H.MAX_BRIDGE_INLINE_JSON_BYTES)
                    val ok = response.get(H.KEY_BRIDGE_RESPONSE_OK) as? Boolean ?: error("Missing result flag")
                    val fd = response.get(H.KEY_BRIDGE_PAYLOAD_FD) as? ParcelFileDescriptor
                    val count = if (fd != null) response.get(H.KEY_BRIDGE_PAYLOAD_BYTES) as? Long else null
                    val mime = response.getString(H.KEY_BRIDGE_PAYLOAD_MIME)
                    if (fd != null) {
                        require(count != null && count in 0..maximumPayloadBytes.toLong() && mime == "application/json")
                        owned = OwnedJson(null, fd, maximumPayloadBytes); payload.set(owned)
                    }
                    AgentWire.closeDescriptors(response, fd)
                    val data = owned
                    workers.reads.execute {
                        try {
                            if (closed.get()) return@execute
                            val envelope = AgentJson.objectOf(text, H.MAX_BRIDGE_INLINE_JSON_BYTES, maximumNodes)
                            require(envelope.string("id") == id && envelope.flag("ok") == ok)
                            if (!ok) result(PortResult.Failure(bridgeError(envelope.getAsJsonObject("error")).let {
                                if (call.module == "agent" && call.method == "execRegistered" && it == RunError.NODE_NOT_FOUND) RunError.SCRIPT_TIMEOUT else it
                            }))
                            else if (data == null) result(PortResult.Success(envelope["result"] ?: JsonNull.INSTANCE))
                            else {
                                val marker = envelope.getAsJsonObject("result")?.getAsJsonObject("payload")
                                require(marker?.string("kind") == "descriptor" && marker.string("mime") == mime && marker.number("bytes") == count)
                                val decoded = data.use { it.read(call.timeoutMs) }
                                require(decoded.toByteArray(Charsets.UTF_8).size.toLong() == count)
                                result(PortResult.Success(AgentJson.parse(decoded, maximumPayloadBytes, maximumNodes)))
                            }
                        } catch (_: Exception) { result(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID)) }
                        finally { data?.close(); payload.compareAndSet(data, null) }
                    }
                } catch (_: Exception) { owned?.close(); AgentWire.closeDescriptors(response); result(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID)) }
            }
        }
        try { workers.io.execute {
            if (closed.get()) return@execute
            if (!alive()) { result(PortResult.Failure(RunError.HOST_UNAVAILABLE)); return@execute }
            try { broker.dispatch(AgentWire.envelope(H.KEY_BRIDGE_REQUEST_JSON, json), remote) }
            catch (_: Exception) { result(PortResult.Failure(RunError.HOST_UNAVAILABLE)) }
        } } catch (_: Exception) { result(PortResult.Failure(RunError.HOST_UNAVAILABLE)) }
        // The public capability broker has no per-call cancel. Fence future work and close owned reads.
        return Cancellation { closed.set(true); payload.getAndSet(null)?.close() }
    }
    companion object {
        val IMPLEMENTED = setOf("ui_dump", "ui_find", "ui_wait_for", "app_current", "screen_state", "device_info", "console_tail", "ocr_screen",
            "ui_click", "ui_long_click", "ui_set_text", "ui_scroll", "ui_press_key", "app_launch", "clipboard_get", "clipboard_set",
            "ui_click_xy", "ui_swipe", "ui_gesture", "script_catalog", "script_stop", "files_list", "files_stat", "files_read", "files_write", "shell_exec", "report_progress")
        fun bridgeError(error: JsonObject?): RunError {
            val stable = error?.string("message")?.substringBefore(':')?.trim()
            val recognized = setOf("A11Y_SERVICE_NOT_RUNNING", "NODE_REF_STALE", "NODE_NOT_FOUND", "SCREEN_LOCKED", "OCR_PLUGIN_REQUIRED",
                "SCRIPT_NOT_REGISTERED", "SCRIPT_TIMEOUT", "SCRIPT_FAILED", "CANCELLED", "CAPABILITY_DENIED", "QUOTA_EXCEEDED", "LIMIT_EXCEEDED", "RATE_LIMITED")
            if (stable in recognized) return RunError.valueOf(stable!!)
            return when (error?.string("category")) {
                H.ERROR_PROCESS_DEAD -> RunError.HOST_UNAVAILABLE
                H.ERROR_PERMISSION_DENIED, H.ERROR_CAPABILITY_DENIED -> RunError.CAPABILITY_DENIED
                H.ERROR_RATE_LIMITED -> RunError.RATE_LIMITED
                H.ERROR_RESOURCE_LIMIT -> RunError.LIMIT_EXCEEDED
                H.ERROR_TIMEOUT -> RunError.NODE_NOT_FOUND
                else -> RunError.TOOL_ARGUMENTS_INVALID
            }
        }
    }
}
