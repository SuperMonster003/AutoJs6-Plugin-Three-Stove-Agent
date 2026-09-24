package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.StepJournal
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*

/** Read side of D29/D39. Only previously confirmed private entries may feed this snapshot. */
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
            return select(MemoryCodec.decode(json), preset, includeGlobal, includePreset)
        }
        internal fun select(entries: List<MemoryEntry>, preset: String, includeGlobal: Boolean, includePreset: Boolean,
                            keys: Set<String>? = null, maximumBytes: Int = MAX_INJECTION_BYTES): MemoryContext {
            val rows = entries.filter { (includeGlobal && it.scope == "global" || includePreset && it.scope == preset) && (keys == null || it.key in keys) }
            // A preset-specific preference overrides a global value with the same exact key.
            val selected = rows.groupBy { it.key }.values.map { group ->
                group.firstOrNull { it.scope == preset } ?: group.single()
            }.sortedWith(compareByDescending<MemoryEntry> { it.updatedAt }.thenBy { it.key })
            val packed = JsonArray()
            for (row in selected) {
                val projected = jsonObject("key" to row.key.json(), "value" to row.value.json(), "scope" to row.scope.json())
                packed.add(projected)
                if (StepJournal.bytes(packed) > maximumBytes) { packed.remove(packed.size() - 1); break }
            }
            return MemoryContext(packed, packed.size() < selected.size)
        }
    }
}
