package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.StepJournal

/** Read side of D29/D39. Only previously confirmed private entries may feed this snapshot.
 * P6 owns proposals, writes and management UI; task arguments and model output never seed it. */
class MemoryContext private constructor(entries: JsonArray, val truncated: Boolean, val unavailable: Boolean = false) {
    private val data = entries.deepCopy()
    val entries: JsonArray get() = data.deepCopy()
    override fun toString() = "MemoryContext(entries=${data.size()}, truncated=$truncated, unavailable=$unavailable)"

    companion object {
        const val MAX_FILE_BYTES = 256 * 1024
        const val MAX_ENTRIES = 500
        const val MAX_INJECTION_BYTES = 4096
        val EMPTY = MemoryContext(JsonArray(), false)
        val UNAVAILABLE = MemoryContext(JsonArray(), false, true)

        fun decode(json: String, preset: String, includeGlobal: Boolean = true, includePreset: Boolean = true): MemoryContext {
            require(preset.isNotBlank() && preset.codePointCount(0, preset.length) <= 128)
            val root = AgentJson.objectOf(json, MAX_FILE_BYTES)
            require(root.keySet() == setOf("version", "entries") && root.number("version") == 1L)
            val raw = root["entries"]?.takeIf { it.isJsonArray }?.asJsonArray ?: error("Invalid memory entries")
            require(raw.size() <= MAX_ENTRIES)
            val seen = mutableSetOf<Pair<String, String>>()
            val rows = raw.map { item ->
                require(item.isJsonObject)
                val row = item.asJsonObject
                require(row.keySet() == setOf("key", "value", "scope", "sourceRunId", "createdAt", "updatedAt"))
                fun text(key: String, max: Int, blank: Boolean = false) = requireNotNull(row.string(key)).also {
                    require((blank || it.isNotBlank()) && it.codePointCount(0, it.length) <= max)
                }
                val key = text("key", 64); text("value", 4096, true); val scope = text("scope", 128)
                require(key.none(Character::isISOControl) && scope.none(Character::isISOControl))
                require(text("sourceRunId", 36).matches(Regex("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")))
                val created = requireNotNull(row.number("createdAt")); val updated = requireNotNull(row.number("updatedAt"))
                require(created >= 0 && updated >= created && seen.add(scope to key))
                row
            }.filter { includeGlobal && it.string("scope") == "global" || includePreset && it.string("scope") == preset }
            // A preset-specific preference overrides a global value with the same exact key.
            val selected = rows.groupBy { it.string("key")!! }.values.map { group ->
                group.firstOrNull { it.string("scope") == preset } ?: group.single()
            }.sortedWith(compareByDescending<JsonObject> { it.number("updatedAt") }.thenBy { it.string("key") })
            val packed = JsonArray()
            for (row in selected) {
                val projected = jsonObject("key" to row["key"], "value" to row["value"], "scope" to row["scope"])
                packed.add(projected)
                if (StepJournal.bytes(packed) > MAX_INJECTION_BYTES) { packed.remove(packed.size() - 1); break }
            }
            return MemoryContext(packed, packed.size() < selected.size)
        }
    }
}
