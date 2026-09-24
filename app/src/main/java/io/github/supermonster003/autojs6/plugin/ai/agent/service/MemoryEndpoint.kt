package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.os.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** Same-UID management only. Import writes exactly one reviewed row per request. */
internal class MemoryEndpoint(private val runtime: AgentRuntime, private val cache: File) : IMemoryStore.Stub() {
    private val queued = AtomicInteger()
    override fun query(request: Bundle?, callback: IMemoryStoreCallback?) {
        if (Binder.getCallingUid() != Process.myUid()) { AgentWire.closeDescriptors(request); throw SecurityException("Private memory") }
        val body = runCatching { AgentJson.objectOf(AgentWire.inline(request, C.KEY_RUN_REQUEST_JSON, MAX_REQUEST_BYTES), MAX_REQUEST_BYTES) }
            .getOrElse { callback?.onResult(AgentWire.error(C.ERROR_INVALID_REQUEST)); return }
        if (callback == null) return
        if (queued.incrementAndGet() > 8) { queued.decrementAndGet(); callback.onResult(AgentWire.error(C.ERROR_LIMIT_EXCEEDED)); return }
        runtime.presets // Start the independent preset snapshot load without Binder IO.
        runtime.memories.query({ store ->
            fun fields(vararg keys: String) { require(body.keySet() == setOf("operation", *keys)) }
            when (body.string("operation")) {
                "list", "export" -> {
                    fields()
                    val root = AgentJson.objectOf(MemoryCodec.encode(store.snapshot()), MemoryCodec.MAX_BYTES)
                    if (body.string("operation") == "list") root.add("scopes", JsonArray().apply {
                        add("global"); runCatching { runtime.presets.snapshot().presets }.getOrDefault(emptyList()).forEach { add(it.name) }
                    })
                    root
                }
                "save" -> {
                    fields("entry", "before", "imported")
                    val proposed = MemoryCodec.decodeEntry(requireNotNull(body.getAsJsonObject("entry")))
                    val before = body["before"].takeUnless { it.isJsonNull }?.let { MemoryCodec.decodeEntry(it.asJsonObject) }
                    val imported = requireNotNull(body.flag("imported"))
                    require(before == null || before.identity == proposed.identity)
                    if (imported) require(proposed.scope == "global" || runtime.presets.snapshot().presets.any { it.name == proposed.scope })
                    else { requireNotNull(before); require(proposed.sourceRunId == before.sourceRunId && proposed.createdAt == before.createdAt) }
                    val now = maxOf(System.currentTimeMillis(), before?.updatedAt ?: 0)
                    store.put(proposed.copy(createdAt = before?.createdAt ?: minOf(proposed.createdAt, now), updatedAt = now), before)
                    JsonObject()
                }
                "delete" -> {
                    fields("entry")
                    val row = MemoryCodec.decodeEntry(requireNotNull(body.getAsJsonObject("entry")))
                    store.delete(row.scope, row.key, row); JsonObject()
                }
                else -> error("Unknown memory operation")
            }
        }) { result ->
            try {
                if (result.isFailure) callback.onResult(AgentWire.error(C.ERROR_INVALID_REQUEST))
                else send(callback, result.getOrThrow().toString())
            } catch (_: Exception) { /* A closed UI does not stop the memory worker. */ }
            finally { queued.decrementAndGet() }
        }
    }
    private fun send(callback: IMemoryStoreCallback, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8); require(bytes.size <= MAX_RESPONSE_BYTES)
        if (bytes.size <= 32768) { callback.onResult(AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, text)); return }
        val file = File.createTempFile("memory-", ".json", cache)
        try {
            file.writeBytes(bytes)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                check(file.delete())
                callback.onResult(Bundle().apply { putInt(C.KEY_CONTRACT_VERSION, C.CONTRACT_VERSION); putParcelable(C.KEY_PAYLOAD_FD, descriptor) })
            }
        } finally { file.delete() }
    }
    companion object {
        const val ACTION = "io.github.supermonster003.autojs6.plugin.ai.agent.MEMORY"
        const val MAX_REQUEST_BYTES = 2 * MemoryCodec.MAX_ROW_BYTES + 1024
        const val MAX_RESPONSE_BYTES = MemoryCodec.MAX_BYTES + 16 * 1024
    }
}
