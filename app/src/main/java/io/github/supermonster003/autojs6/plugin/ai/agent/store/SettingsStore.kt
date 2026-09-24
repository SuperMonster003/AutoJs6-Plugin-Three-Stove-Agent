package io.github.supermonster003.autojs6.plugin.ai.agent.store

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunLimits
import java.io.File
import java.io.FileOutputStream

internal data class AgentSettings(
    val toolGroups: Set<String> = ToolGroup.entries.filter { it.defaultEnabled }.map { it.id }.toSet(),
    val budget: Map<String, Long> = emptyMap(), val cautious: Boolean = false, val voice: Boolean = true,
)

/** Private format; a corrupt or future version never silently restores a more permissive policy. */
internal object SettingsCodec {
    const val MAX_BYTES = 4096
    val ceilings = mapOf("maxSteps" to RunLimits.STEPS.toLong(), "maxModelCalls" to RunLimits.MODEL_CALLS.toLong(),
        "maxDurationMs" to RunLimits.DETACHED_DURATION_MS, "maxTotalTokens" to RunLimits.TOKENS)
    fun decode(text: String): AgentSettings {
        val root = AgentJson.objectOf(text, MAX_BYTES)
        require(root.keySet() == setOf("version", "toolGroups", "budget", "cautious", "voice") && root.number("version") == 1L)
        val groups = requireNotNull(root["toolGroups"]?.takeIf { it.isJsonArray }?.asJsonArray).map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString); it.asString
        }
        require(groups.toSet().size == groups.size && groups.all { id -> ToolGroup.entries.any { it.id == id } })
        val budget = requireNotNull(root["budget"]?.takeIf { it.isJsonObject }?.asJsonObject)
        val limits = budget.keySet().associateWith { key ->
            val ceiling = requireNotNull(ceilings[key]); requireNotNull(runCatching { budget.number(key) }.getOrNull()).also { require(it in 1..ceiling) }
        }
        return AgentSettings(groups.toSet(), limits, requireNotNull(root.flag("cautious")), requireNotNull(root.flag("voice")))
    }
    fun json(value: AgentSettings) = jsonObject("version" to 1.json(), "toolGroups" to JsonArray().apply {
        value.toolGroups.sorted().forEach(::add)
    }, "budget" to JsonObject().apply { value.budget.forEach { (key, number) -> addProperty(key, number) } },
        "cautious" to value.cautious.json(), "voice" to value.voice.json())
    fun encode(value: AgentSettings): String = json(value).toString().also { decode(it) }
}

/** Owned by the settings worker in :agent, published only after a durable replacement. */
internal class SettingsStore(private val file: File) {
    fun open(): AgentSettings {
        val backup = File(file.path + ".bak")
        val source = if (backup.exists()) backup else file
        val value = if (source.exists()) SettingsCodec.decode(MemoryStore.read(source, SettingsCodec.MAX_BYTES)) else AgentSettings()
        if (backup.exists()) { check(!file.exists() || file.delete()); check(backup.renameTo(file)) }
        return value
    }
    fun save(value: AgentSettings) {
        val text = SettingsCodec.encode(value)
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        val temp = File(file.path + ".new"); val backup = File(file.path + ".bak")
        try {
            FileOutputStream(temp).use { it.write(text.toByteArray(Charsets.UTF_8)); it.fd.sync() }
            check(!backup.exists())
            if (file.exists()) check(file.renameTo(backup))
            if (!temp.renameTo(file)) { if (backup.exists()) check(backup.renameTo(file)); error("Settings write failed") }
            if (backup.exists() && !backup.delete()) { check(file.delete()); check(backup.renameTo(file)); error("Settings write failed") }
        } finally { temp.delete() }
    }
}
