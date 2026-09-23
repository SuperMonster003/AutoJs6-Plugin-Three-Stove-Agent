package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.content.Context
import android.os.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import org.autojs.plugin.ai.agent.api.IAiAgentModelBroker
import org.autojs.plugin.ai.agent.api.IAiAgentModelCallback
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal class SelectedModel(val target: ModelTarget, val maximumInputBytes: Int, val maximumTokens: Long, val maximumSchemaBytes: Int)

/** Worker/descriptor adapter for the host's model broker. No Provider binding or HTTP implementation. */
internal class BinderModelBroker(private val context: Context, private val broker: IAiAgentModelBroker,
                                private val ownerUid: Int, private val workers: LinkWorkers) : ModelBrokerTransport, AutoCloseable {
    private val streams = ConcurrentHashMap<String, OrderedModelEvents>()
    private val closed = AtomicBoolean()
    override fun generate(requestJson: String, onEvent: (String) -> Unit) = generate(requestJson, onEvent) {}
    override fun generate(requestJson: String, onEvent: (String) -> Unit, onFailure: (RunError) -> Unit) {
        val request = AgentJson.objectOf(requestJson, C.MAX_MODEL_REQUEST_PAYLOAD_BYTES)
        val id = requireNotNull(request.string("requestId"))
        send(id, requestJson, false, onEvent) { code -> onFailure(RunError.entries.firstOrNull { it.name == code } ?: RunError.MODEL_FAILED) }
    }
    fun select(targetId: String?, callback: (PortResult<SelectedModel>) -> Unit): Cancellation {
        val id = "catalog-${UUID.randomUUID()}"
        val cancelled = AtomicBoolean()
        try { workers.io.execute {
            if (closed.get() || cancelled.get()) return@execute
            try {
                val bundle = broker.brokerInfo
                val info = AgentJson.objectOf(AgentWire.inline(bundle, C.KEY_MODEL_BROKER_INFO_JSON))
                if (info.flag("available") != true) throw WireFailure(C.ERROR_TARGET_UNAVAILABLE)
                val provider = requireNotNull(info.string("providerId"))
                val inputLimit = requireNotNull(info.number("maximumInputBytes")).coerceAtMost(C.MAX_MODEL_REQUEST_PAYLOAD_BYTES.toLong()).toInt()
                val outputLimit = requireNotNull(info.number("maximumOutputBytes")).coerceAtMost(C.MAX_MODEL_OUTPUT_BYTES.toLong()).toInt()
                val grant = bundle.getString(H.KEY_GRANT_JSON)?.let { AgentJson.objectOf(it) } ?: throw WireFailure(C.ERROR_INVALID_REQUEST)
                val grantedInput = requireNotNull(grant.number("maxInputBytesPerRequest")).also { require(it in 1..C.MAX_MODEL_REQUEST_PAYLOAD_BYTES) }
                val maxInput = minOf(inputLimit, grantedInput.toInt())
                val tokenLimit = requireNotNull(grant.number("maxTotalTokens")).also { require(it > 0) }
                val consumed = (grant.number("consumedTokens") ?: 0).also { require(it >= 0) }
                val maxTokens = (tokenLimit - minOf(tokenLimit, consumed)).coerceAtLeast(0)
                val schemaLimit = requireNotNull(info.number("maximumResponseSchemaBytes")).also { require(it in 0..C.MAX_RESPONSE_SCHEMA_BYTES) }.toInt()
                require(maxInput > 0 && outputLimit > 0)
                var sequence = 0L
                val request = jsonObject("requestId" to id.json(), "timeoutMs" to 10_000.json()).toString()
                if (cancelled.get()) return@execute
                send(id, request, true, { json ->
                    if (cancelled.get()) return@send
                    try {
                        val event = AgentJson.objectOf(json, C.MAX_MODEL_REQUEST_PAYLOAD_BYTES)
                        require(event.string("requestId") == id && event.number("sequence") == ++sequence)
                        val type = event.string("type")
                        require(if (sequence == 1L) type == C.MODEL_EVENT_STARTED else type != C.MODEL_EVENT_STARTED)
                        when (type) {
                            C.MODEL_EVENT_STARTED -> Unit
                            C.MODEL_EVENT_COMPLETED -> {
                                val entries = requireNotNull(event.getAsJsonArray("targets")); require(entries.size() <= 256)
                                val targets = entries.mapNotNull { entry -> runCatching { ModelTarget.fromCatalog(provider, entry.asJsonObject, outputLimit) }.getOrNull() }
                                val selected = if (targetId == null) targets.firstOrNull { it.locality == ModelLocality.ON_DEVICE } ?: targets.firstOrNull()
                                    else targets.firstOrNull { it.targetId == targetId }
                                if (selected == null) throw WireFailure(C.ERROR_TARGET_UNAVAILABLE)
                                callback(PortResult.Success(SelectedModel(selected, maxInput, maxTokens, schemaLimit)))
                            }
                            C.MODEL_EVENT_FAILED, C.MODEL_EVENT_CANCELLED -> callback(PortResult.Failure(
                                RunError.entries.firstOrNull { it.name == event.string("code") } ?: RunError.MODEL_FAILED))
                            else -> throw WireFailure(C.ERROR_INVALID_REQUEST)
                        }
                    } catch (failure: Exception) {
                        callback(PortResult.Failure(RunError.entries.firstOrNull { it.name == (failure as? WireFailure)?.code } ?: RunError.INVALID_REQUEST)); cancel(id)
                    }
                }, { code -> if (!cancelled.get()) callback(PortResult.Failure(RunError.entries.firstOrNull { it.name == code } ?: RunError.MODEL_FAILED)) })
                if (cancelled.get()) cancel(id)
            } catch (failure: Exception) {
                if (!cancelled.get()) callback(PortResult.Failure(if (failure is RemoteException) RunError.HOST_UNAVAILABLE
                    else RunError.entries.firstOrNull { it.name == (failure as? WireFailure)?.code } ?: RunError.INVALID_REQUEST))
            }
        } } catch (_: Exception) { callback(PortResult.Failure(RunError.HOST_UNAVAILABLE)) }
        return Cancellation { cancelled.set(true); cancel(id) }
    }
    private fun send(id: String, json: String, catalog: Boolean, event: (String) -> Unit, failure: (String) -> Unit) {
        if (closed.get()) { failure(C.ERROR_LINK_DETACHED); return }
        val ordered = OrderedModelEvents(workers, { value ->
            event(value)
            val type = runCatching { AgentJson.objectOf(value, C.MAX_MODEL_REQUEST_PAYLOAD_BYTES).string("type") }.getOrNull()
            if (type in C.MODEL_TERMINAL_EVENTS) streams.remove(id)?.close()
        }, { code -> streams.remove(id)?.close(); failure(code) })
        require(streams.putIfAbsent(id, ordered) == null)
        val callback = object : IAiAgentModelCallback.Stub() {
            override fun onEvent(bundle: Bundle?) {
                if (Binder.getCallingUid() != ownerUid || closed.get() || streams[id] !== ordered) { AgentWire.closeDescriptors(bundle); return }
                try { ordered.accept(AgentWire.take(bundle, C.KEY_MODEL_EVENT_JSON, C.KEY_PAYLOAD_FD, C.MAX_EVENT_JSON_BYTES, C.MAX_MODEL_REQUEST_PAYLOAD_BYTES)) }
                catch (_: Exception) { ordered.close(); streams.remove(id); failure(C.ERROR_INVALID_REQUEST) }
            }
        }
        try { workers.io.execute {
            if (closed.get() || streams[id] !== ordered) return@execute
            try {
                if (catalog) broker.listTargets(AgentWire.envelope(C.KEY_MODEL_REQUEST_JSON, json), callback)
                else if (json.toByteArray(Charsets.UTF_8).size <= C.MAX_MODEL_REQUEST_INLINE_BYTES) broker.generate(AgentWire.envelope(C.KEY_MODEL_REQUEST_JSON, json), callback)
                else {
                    val file = File.createTempFile("agent-model-", ".json", context.cacheDir)
                    try {
                        file.writeText(json, Charsets.UTF_8)
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                            val bundle = AgentWire.envelope(C.KEY_MODEL_REF_JSON, jsonObject("requestId" to id.json()).toString()).apply { putParcelable(C.KEY_PAYLOAD_FD, fd) }
                            if (streams[id] === ordered) broker.generate(bundle, callback)
                        }
                    } finally { file.delete() }
                }
            } catch (_: Exception) { streams.remove(id)?.close(); failure(C.ERROR_HOST_UNAVAILABLE) }
        } } catch (_: Exception) { streams.remove(id)?.close(); failure(C.ERROR_HOST_UNAVAILABLE) }
    }
    override fun cancel(requestId: String) {
        streams.remove(requestId)?.close()
        runCatching { workers.io.execute { runCatching { broker.cancel(AgentWire.envelope(C.KEY_MODEL_REF_JSON, jsonObject("requestId" to requestId.json()).toString())) } } }
    }
    override fun close() { closed.set(true); streams.keys.toList().forEach(::cancel) }
}
