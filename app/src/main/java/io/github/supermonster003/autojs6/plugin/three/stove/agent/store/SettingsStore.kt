package io.github.supermonster003.autojs6.plugin.three.stove.agent.store

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.RiskRules
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.RunLimits
import java.io.File
import java.io.FileOutputStream

internal data class AgentSettings(
    val toolGroups: Set<String> = ToolGroup.entries.filter { it.defaultEnabled }.map { it.id }.toSet(),
    val budget: Map<String, Long> = emptyMap(), val cautious: Boolean = false, val voice: Boolean = true,
    val floating: Boolean = false, val fullAccess: Boolean = false,
    /** Channels that report a task stopped by an error, a budget limit or a lost host (P14.1). */
    val failureAlerts: Set<String> = ALERT_CHANNELS,
    /** User additions to the packaged payment application list (P13 risk recognition); built-ins stay. */
    val riskPackages: Set<String> = emptySet(),
    /** User additions to the packaged sensitive keyword table; matched case-insensitively as substrings. */
    val riskKeywords: Set<String> = emptySet(),
    /** Channels for a normal completion, independent of abnormal-stop alerts. */
    val completionAlerts: Set<String> = DEFAULT_COMPLETION_ALERTS,
) {
    init {
        require(!cautious || !fullAccess); require(failureAlerts.all { it in ALERT_CHANNELS })
        require(completionAlerts.all { it in ALERT_CHANNELS })
        require(riskPackages.size <= RiskRules.MAX_ENTRIES && riskPackages.all(RiskRules::isPackage))
        require(riskKeywords.size <= RiskRules.MAX_ENTRIES && riskKeywords.all(RiskRules::isKeyword))
    }
    /** Wire value of the global access mode shown on the workbench: standard, cautious or full. */
    val accessMode: String get() = when { fullAccess -> "full"; cautious -> "cautious"; else -> "standard" }
    companion object {
        const val ALERT_NOTIFICATION = "notification"
        const val ALERT_TOAST = "toast"
        const val ALERT_DIALOG = "dialog"
        val ALERT_CHANNELS = setOf(ALERT_NOTIFICATION, ALERT_TOAST, ALERT_DIALOG)
        val DEFAULT_COMPLETION_ALERTS = setOf(ALERT_NOTIFICATION, ALERT_TOAST)
    }
}

/** Private format; a corrupt or future version never silently restores a more permissive policy. */
internal object SettingsCodec {
    /** Version 5 adds two bounded lists (32 x 128 chars), which no longer fit the former 4 KiB cap. */
    const val MAX_BYTES = 16384
    val ceilings = mapOf("maxSteps" to RunLimits.STEPS.toLong(), "maxModelCalls" to RunLimits.MODEL_CALLS.toLong(),
        "maxDurationMs" to RunLimits.DETACHED_DURATION_MS, "maxTotalTokens" to RunLimits.TOKENS)
    fun decode(text: String): AgentSettings {
        val root = AgentJson.objectOf(text, MAX_BYTES)
        val version = requireNotNull(root.number("version"))
        require(version in 1L..6L)
        require(root.keySet() == setOf("version", "toolGroups", "budget", "cautious", "voice") +
            (if (version >= 2L) setOf("floating") else emptySet()) +
            (if (version >= 3L) setOf("fullAccess") else emptySet()) +
            (if (version >= 4L) setOf("failureAlerts") else emptySet()) +
            (if (version >= 5L) setOf("riskPackages", "riskKeywords") else emptySet()) +
            (if (version >= 6L) setOf("completionAlerts") else emptySet()))
        val groups = requireNotNull(root["toolGroups"]?.takeIf { it.isJsonArray }?.asJsonArray).map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString); it.asString
        }
        require(groups.toSet().size == groups.size && groups.all { id -> ToolGroup.entries.any { it.id == id } })
        val budget = requireNotNull(root["budget"]?.takeIf { it.isJsonObject }?.asJsonObject)
        val limits = budget.keySet().associateWith { key ->
            val ceiling = requireNotNull(ceilings[key]); requireNotNull(runCatching { budget.number(key) }.getOrNull()).also { require(it in 1..ceiling) }
        }
        fun channels(key: String) = requireNotNull(root[key]?.takeIf { it.isJsonArray }?.asJsonArray).map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString); it.asString
        }.also { require(it.toSet().size == it.size && it.all { channel -> channel in AgentSettings.ALERT_CHANNELS }) }.toSet()
        // Preserve every explicit choice, including an empty set. Only absent fields gain defaults.
        val alerts = if (version >= 4L) channels("failureAlerts") else AgentSettings.ALERT_CHANNELS
        val completionAlerts = if (version >= 6L) channels("completionAlerts") else AgentSettings.DEFAULT_COMPLETION_ALERTS
        fun strings(key: String): Set<String> = if (version < 5L) emptySet() else requireNotNull(root[key]?.takeIf { it.isJsonArray }?.asJsonArray).map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString); it.asString
        }.also { require(it.toSet().size == it.size) }.toSet()
        return AgentSettings(groups.toSet(), limits, requireNotNull(root.flag("cautious")), requireNotNull(root.flag("voice")),
            if (version >= 2L) requireNotNull(root.flag("floating")) else false,
            if (version >= 3L) requireNotNull(root.flag("fullAccess")) else false, alerts, strings("riskPackages"), strings("riskKeywords"), completionAlerts)
    }
    fun json(value: AgentSettings) = jsonObject("version" to 6.json(), "toolGroups" to JsonArray().apply {
        value.toolGroups.sorted().forEach(::add)
    }, "budget" to JsonObject().apply { value.budget.forEach { (key, number) -> addProperty(key, number) } },
        "cautious" to value.cautious.json(), "voice" to value.voice.json(), "floating" to value.floating.json(), "fullAccess" to value.fullAccess.json(),
        "failureAlerts" to JsonArray().apply { value.failureAlerts.sorted().forEach(::add) },
        "completionAlerts" to JsonArray().apply { value.completionAlerts.sorted().forEach(::add) },
        "riskPackages" to JsonArray().apply { value.riskPackages.sorted().forEach(::add) },
        "riskKeywords" to JsonArray().apply { value.riskKeywords.sorted().forEach(::add) })
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
