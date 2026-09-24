package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.os.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import java.util.concurrent.atomic.*

/** Settings and data management without disk IO or waiting on a Binder thread. */
internal class SettingsEndpoint(private val runtime: AgentRuntime) : IAgentSettings.Stub() {
    private val queued = AtomicInteger()
    override fun query(request: Bundle?, callback: IPresetStoreCallback?) {
        if (Binder.getCallingUid() != Process.myUid()) { AgentWire.closeDescriptors(request); throw SecurityException("Private settings") }
        val body = runCatching { AgentJson.objectOf(AgentWire.inline(request, C.KEY_RUN_REQUEST_JSON, MAX_RESPONSE_BYTES), MAX_RESPONSE_BYTES) }
            .getOrElse { callback?.onResult(AgentWire.error(C.ERROR_INVALID_REQUEST)); return }
        if (callback == null) return
        if (queued.incrementAndGet() > 8) { queued.decrementAndGet(); callback.onResult(AgentWire.error(C.ERROR_LIMIT_EXCEEDED)); return }
        val completed = AtomicBoolean()
        var clearing = false
        fun finish(result: Result<JsonObject>) {
            if (!completed.compareAndSet(false, true)) return
            if (clearing) runtime.endMaintenance()
            try {
                callback.onResult(if (result.isSuccess) AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON,
                    result.getOrThrow().toString().also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_RESPONSE_BYTES) })
                    else AgentWire.error(C.ERROR_INVALID_REQUEST))
            } catch (_: Exception) { /* The caller may close the screen. */ } finally { queued.decrementAndGet() }
        }
        fun complete(result: Result<*>) = finish(result.map { JsonObject() })
        try {
            fun fields(vararg keys: String) { require(body.keySet() == setOf("operation", *keys)) }
            when (body.string("operation")) {
                "get" -> {
                    fields()
                    runtime.settings.query settingsReply@ { settings ->
                        if (settings.isFailure) { finish(Result.failure(settings.exceptionOrNull()!!)); return@settingsReply }
                        val value = jsonObject("settings" to SettingsCodec.json(settings.getOrThrow()))
                        runtime.presets.query({ store, snapshot ->
                            value.addProperty("defaultName", snapshot.defaultName)
                            value.add("presets", JsonArray().apply { snapshot.presets.forEach { add(it.name) } })
                            value.add("presetData", statistics(snapshot.presets.size, store.bytes()))
                        }) presetsReply@ { presets ->
                            if (presets.isFailure) { finish(Result.failure(presets.exceptionOrNull()!!)); return@presetsReply }
                            runtime.memories.query({ store -> statistics(store.snapshot().size, store.bytes()) }) memoriesReply@ { memories ->
                                if (memories.isFailure) { finish(Result.failure(memories.exceptionOrNull()!!)); return@memoriesReply }
                                value.add("memoryData", memories.getOrThrow())
                                runtime.archive.history({ runtime.archive.statistics() }) { history ->
                                    finish(history.map { value.apply { add("historyData", it); addProperty("busy", runtime.maintenance || runtime.current?.liveRuns()?.isNotEmpty() == true) } })
                                }
                            }
                        }
                    }
                }
                "save" -> { fields("settings"); runtime.settings.query(SettingsCodec.decode(requireNotNull(body["settings"]).toString()), ::complete) }
                "default" -> {
                    fields("name"); val name = PresetCodec.name(requireNotNull(body.string("name")))
                    runtime.presets.query({ store, _ -> runtime.presets.publish(store.setDefault(name)) }, ::complete)
                }
                "clear" -> {
                    fields("store"); val kind = body.string("store"); require(kind in setOf("history", "presets", "memory"))
                    check(runtime.beginMaintenance()); clearing = true
                    when (kind) {
                        "history" -> runtime.archive.history({ runtime.archive.remove(null); JsonObject() }, ::finish)
                        "presets" -> runtime.presets.query({ store, _ -> runtime.presets.publish(store.clear()) }, ::complete)
                        "memory" -> runtime.memories.query({ it.clear() }, ::complete)
                    }
                }
                else -> error("Invalid settings operation")
            }
        } catch (failure: Exception) { finish(Result.failure(failure)) }
    }
    private fun statistics(count: Int, bytes: Long) = jsonObject("count" to count.json(), "bytes" to bytes.json())
    companion object {
        const val ACTION = "io.github.supermonster003.autojs6.plugin.ai.agent.SETTINGS"
        const val MAX_RESPONSE_BYTES = 16 * 1024
    }
}
