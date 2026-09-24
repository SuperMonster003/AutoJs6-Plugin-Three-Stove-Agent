package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.os.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** Same-app asynchronous IO; large snapshots are immutable, unlinked, read-only descriptors. */
internal class HistoryEndpoint(private val archive: RunArchive, private val cache: File, private val toolNames: Set<String>) : IRunHistory.Stub() {
    private val queued = AtomicInteger()
    override fun query(request: Bundle?, callback: IRunHistoryCallback?) {
        if (Binder.getCallingUid() != Process.myUid()) { AgentWire.closeDescriptors(request); throw SecurityException("Private history") }
        val value = runCatching {
            AgentJson.objectOf(AgentWire.inline(request, C.KEY_RUN_REQUEST_JSON, 1024))
        }.getOrElse { callback?.onResult(AgentWire.error(C.ERROR_INVALID_REQUEST)); return }
        if (callback == null) return
        if (queued.incrementAndGet() > 8) {
            queued.decrementAndGet(); callback.onResult(AgentWire.error(C.ERROR_LIMIT_EXCEEDED)); return
        }
        archive.history({
            val op = value.string("operation")
            require(value.keySet().all { it in setOf("operation", "runId", "touch") })
            require(!value.has("touch") || value.flag("touch") != null)
            if (op in setOf("list", "clear")) require(!value.has("runId"))
            if (op == "list") archive.list(200, 0) else if (op == "clear") {
                require(!value.has("runId")); archive.remove(null); JsonObject()
            } else {
                val id = RunHistoryCodec.id(requireNotNull(value.string("runId")))
                when (op) {
                    "get", "export" -> {
                        val row = requireNotNull(archive.full(id))
                        if (value.flag("touch") == true) archive.touch(id)
                        if (op == "export") RunHistoryExport.redact(row, toolNames) else row
                    }
                    "delete" -> { archive.remove(id); JsonObject() }
                    else -> error("Unknown operation")
                }
            }
        }) { result ->
            try {
                if (result.isFailure) callback.onResult(AgentWire.error(C.ERROR_INVALID_REQUEST))
                else send(callback, result.getOrThrow().toString())
            } catch (_: Exception) { /* A closed screen must not stop the journal worker. */ }
            finally { queued.decrementAndGet() }
        }
    }
    private fun send(callback: IRunHistoryCallback, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size <= RunHistoryCodec.MAX_BYTES)
        if (bytes.size <= 32 * 1024) { callback.onResult(AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, text)); return }
        val file = File.createTempFile("history-", ".json", cache)
        try {
            file.writeBytes(bytes)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                check(file.delete())
                callback.onResult(Bundle().apply { putInt(C.KEY_CONTRACT_VERSION, C.CONTRACT_VERSION); putParcelable(C.KEY_PAYLOAD_FD, descriptor) })
            }
        } finally { file.delete() }
    }
    companion object { const val ACTION = "io.github.supermonster003.autojs6.plugin.ai.agent.HISTORY" }
}
