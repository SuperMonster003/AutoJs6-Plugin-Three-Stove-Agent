package io.github.supermonster003.autojs6.plugin.three.stove.agent.store

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.scripts.ScriptRoots

/** Names are stable public identifiers, also used by history and private memory scopes. */
internal data class Preset(
    val name: String, val targetId: String? = null, val toolGroups: Set<String>? = null,
    val budget: Map<String, Long> = emptyMap(), val confirmPolicy: String = "default",
    val context: String = "", val scriptRoots: Set<String>? = null, val memoryScope: String = "global_and_preset",
    val planMode: Boolean = false,
) {
    fun groups(allowed: Set<String>) = toolGroups?.intersect(allowed) ?: allowed
}

internal class PresetSnapshot(val defaultName: String, presets: List<Preset>) {
    val presets = presets.toList()
    fun resolve(name: String? = null): Preset = requireNotNull(presets.find { it.name == (name ?: defaultName) })
    companion object { val INITIAL = PresetSnapshot("default", listOf(Preset("default"))) }
}

/** Closed, bounded, versioned private format. Unknown versions are never replaced with defaults. */
internal object PresetCodec {
    const val MAX_COUNT = 32
    const val MAX_ROW_BYTES = 96 * 1024
    const val MAX_FILE_BYTES = 1024 * 1024
    /** A portable file is reviewed row by row and travels through saved state, so it stays well below the private file limit. */
    const val MAX_EXPORT_BYTES = 256 * 1024
    val scopes = listOf("global_and_preset", "global", "preset", "none")
    val ceilings = SettingsCodec.ceilings
    fun name(text: String) = text.also {
        AgentJson.checkUnicode(it)
        require(it.isNotBlank() && it == it.trim() && it.utf8Size() <= 128 && it != "global" && it.none(Character::isISOControl))
    }
    fun target(text: String) = text.also { require(it.length <= 256 && it.matches(Regex("[a-z0-9][a-z0-9._-]{0,127}:[a-z0-9][a-z0-9._-]{0,127}"))) }
    private fun strings(value: JsonObject, key: String): Set<String>? {
        if (!value.has(key)) return null
        val array = requireNotNull(value[key]?.takeIf { it.isJsonArray }?.asJsonArray)
        require(array.size() <= 32)
        return array.map { require(it.isJsonPrimitive && it.asJsonPrimitive.isString); it.asString }.toSet().also { require(it.size == array.size()) }
    }
    fun decodePreset(value: JsonObject): Preset {
        require(value.keySet().all { it in setOf("name", "targetId", "toolGroups", "budget", "confirmPolicy", "context", "scriptRoots", "memoryScope", "planMode") })
        val groups = strings(value, "toolGroups")?.also { require(it.all { id -> ToolGroup.entries.any { group -> group.id == id } }) }
        val rawBudget = if (value.has("budget")) requireNotNull(value["budget"].takeIf { it.isJsonObject }?.asJsonObject) else JsonObject()
        val budget = rawBudget.keySet().associateWith { key ->
            val ceiling = requireNotNull(ceilings[key]); requireNotNull(rawBudget.number(key)).also { require(it in 1..ceiling) }
        }
        fun text(key: String, default: String) = if (value.has(key)) requireNotNull(value.string(key)) else default
        val context = text("context", "").also { AgentJson.checkUnicode(it); require(it.utf8Size() <= 8192) }
        val roots = strings(value, "scriptRoots")?.let(ScriptRoots::validate)
        return Preset(name(requireNotNull(value.string("name"))), if (value.has("targetId")) target(requireNotNull(value.string("targetId"))) else null,
            groups, budget, text("confirmPolicy", "default").also { require(it in setOf("default", "cautious")) }, context, roots,
            text("memoryScope", "global_and_preset").also { require(it in scopes) }, if (value.has("planMode")) requireNotNull(value.flag("planMode")) else false)
    }
    fun encodePreset(preset: Preset): JsonObject = jsonObject("name" to preset.name.json(), "confirmPolicy" to preset.confirmPolicy.json(),
        "context" to preset.context.json(), "memoryScope" to preset.memoryScope.json(), "budget" to JsonObject().apply {
            preset.budget.forEach { (key, value) -> addProperty(key, value) }
        }).apply {
        preset.targetId?.let { addProperty("targetId", it) }
        if (preset.planMode) addProperty("planMode", true)
        preset.toolGroups?.let { add("toolGroups", JsonArray().apply { it.sorted().forEach(::add) }) }
        preset.scriptRoots?.let { add("scriptRoots", JsonArray().apply { it.sorted().forEach(::add) }) }
    }
    /** Portable presets (roadmap I.3): no default selection and no model target, which is a device choice (D46). */
    fun decodeExport(text: String): List<Preset> {
        val root = AgentJson.objectOf(text, MAX_EXPORT_BYTES)
        require(root.keySet() == setOf("version", "presets") && root.number("version") == 1L)
        val raw = requireNotNull(root["presets"]?.takeIf { it.isJsonArray }?.asJsonArray)
        require(raw.size() in 1..MAX_COUNT)
        val rows = raw.map { value -> decodePreset(AgentJson.objectOf(value.toString(), MAX_ROW_BYTES)).copy(targetId = null) }
        require(rows.map { it.name }.toSet().size == rows.size)
        return rows
    }
    fun encodeExport(presets: List<Preset>): String {
        val text = jsonObject("version" to 1.json(), "presets" to JsonArray().apply { presets.forEach { add(encodePreset(it.copy(targetId = null))) } }).toString()
        decodeExport(text) // A written file must always import under exactly the same limits.
        return text
    }
    fun decode(text: String): PresetSnapshot {
        val root = AgentJson.objectOf(text, MAX_FILE_BYTES)
        require(root.keySet() == setOf("version", "defaultName", "presets") && root.number("version") == 1L)
        val raw = requireNotNull(root["presets"]?.takeIf { it.isJsonArray }?.asJsonArray)
        require(raw.size() in 1..MAX_COUNT)
        val rows = raw.map { value ->
            val row = AgentJson.objectOf(value.toString(), MAX_ROW_BYTES)
            decodePreset(row)
        }
        require(rows.map { it.name }.toSet().size == rows.size && rows.any { it.name == "default" })
        val default = name(requireNotNull(root.string("defaultName")))
        require(rows.any { it.name == default })
        return PresetSnapshot(default, rows)
    }
    fun encode(snapshot: PresetSnapshot): String {
        val text = jsonObject("version" to 1.json(), "defaultName" to snapshot.defaultName.json(),
            "presets" to JsonArray().apply { snapshot.presets.forEach { add(encodePreset(it)) } }).toString()
        decode(text) // A write must always be readable under exactly the same limits.
        return text
    }
}
