package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.os.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import java.io.File
import java.util.concurrent.atomic.*

/** Same-UID management; mutations and snapshots are serialized in the agent process. */
internal class PresetEndpoint(private val runtime: AgentRuntime, private val cache: File) : IPresetStore.Stub() {
    private val queued = AtomicInteger()
    private val main = Handler(Looper.getMainLooper())
    override fun query(request: Bundle?, callback: IPresetStoreCallback?) {
        if (Binder.getCallingUid() != Process.myUid()) { AgentWire.closeDescriptors(request); throw SecurityException("Private presets") }
        val value = runCatching { AgentJson.objectOf(AgentWire.inline(request, C.KEY_RUN_REQUEST_JSON, PresetCodec.MAX_ROW_BYTES + 1024), PresetCodec.MAX_ROW_BYTES + 1024) }
            .getOrElse { callback?.onResult(AgentWire.error(C.ERROR_INVALID_REQUEST)); return }
        if (callback == null) return
        if (queued.incrementAndGet() > 8) { queued.decrementAndGet(); callback.onResult(AgentWire.error(C.ERROR_LIMIT_EXCEEDED)); return }
        val finished = AtomicBoolean()
        fun finish(result: Result<JsonObject>) {
            if (!finished.compareAndSet(false, true)) return
            try {
                if (result.isSuccess) send(callback, result.getOrThrow().toString())
                else callback.onResult(AgentWire.error((result.exceptionOrNull() as? WireFailure)?.code ?: C.ERROR_INVALID_REQUEST))
            } catch (_: Exception) { /* Closed UI. */ } finally { queued.decrementAndGet() }
        }
        if (value.string("operation") == "targets" && value.keySet() == setOf("operation")) {
            val link = runtime.current ?: run { finish(Result.failure(WireFailure(C.ERROR_LINK_DETACHED))); return }
            val call = AtomicReference(Cancellation.NONE)
            val timeout = Runnable { finish(Result.failure(WireFailure(C.ERROR_MODEL_TIMEOUT))); call.get().cancel() }
            main.postDelayed(timeout, 12_000)
            call.set(link.targets { result ->
                main.removeCallbacks(timeout)
                when (result) {
                    is PortResult.Failure -> finish(Result.failure(WireFailure(result.error.name)))
                    is PortResult.Success -> finish(runCatching {
                        check(runtime.current === link)
                        jsonObject("targets" to JsonArray().apply { result.value.forEach { model -> add(jsonObject(
                            "targetId" to model.target.targetId.json(), "displayName" to model.displayName.json(),
                            "locality" to model.target.locality.name.json(), "structuredJson" to model.target.structuredJson.json())) } })
                    })
                }
            })
            if (finished.get()) call.get().cancel()
            return
        }
        runtime.presets.query({ store, snapshot ->
            fun name() = PresetCodec.name(requireNotNull(value.string("name")))
            fun fields(vararg keys: String) { require(value.keySet() == setOf("operation", *keys)) }
            fun configuration() = runtime.current?.presetConfiguration() ?: LinkConfiguration.parse("{}")
            when (value.string("operation")) {
                "list" -> {
                    fields(); val config = configuration()
                    jsonObject("defaultName" to snapshot.defaultName.json(), "presets" to JsonArray().apply { snapshot.presets.forEach { add(it.name) } },
                        "toolGroups" to JsonArray().apply { config.groups.sorted().forEach(::add) },
                        "scriptRoots" to JsonArray().apply { config.roots.sorted().forEach(::add) })
                }
                "get" -> { fields("name"); PresetCodec.encodePreset(snapshot.resolve(name())) }
                "save" -> {
                    fields("preset", "create")
                    val config = configuration()
                    runtime.presets.publish(store.save(PresetCodec.decodePreset(requireNotNull(value.getAsJsonObject("preset"))),
                        requireNotNull(value.flag("create")), config.groups, config.roots))
                    JsonObject()
                }
                "delete" -> { fields("name"); runtime.presets.publish(store.delete(name())); JsonObject() }
                "default" -> { fields("name"); runtime.presets.publish(store.setDefault(name())); JsonObject() }
                else -> error("Invalid preset operation")
            }
        }, ::finish)
    }
    private fun send(callback: IPresetStoreCallback, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8); require(bytes.size <= MAX_RESPONSE_BYTES)
        if (bytes.size <= 32768) { callback.onResult(AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, text)); return }
        val file = File.createTempFile("presets-", ".json", cache)
        try {
            file.writeBytes(bytes)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                check(file.delete())
                callback.onResult(Bundle().apply { putInt(C.KEY_CONTRACT_VERSION, C.CONTRACT_VERSION); putParcelable(C.KEY_PAYLOAD_FD, descriptor) })
            }
        } finally { file.delete() }
    }
    companion object {
        const val ACTION = "io.github.supermonster003.autojs6.plugin.ai.agent.PRESETS"
        const val MAX_RESPONSE_BYTES = 256 * 1024
    }
}
