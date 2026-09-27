package io.github.supermonster003.autojs6.plugin.three.stove.agent.store

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import java.util.Locale

/**
 * Pure history filter. [state] is a wire state or [ACTIVE] for any unfinished task; [query] matches
 * the goal, preset or model name ignoring case; [from] is inclusive and [until] exclusive.
 */
internal data class RunHistoryFilter(val state: String? = null, val preset: String? = null, val from: Long? = null, val until: Long? = null,
                                     val query: String? = null) {
    fun matches(row: JsonObject): Boolean {
        val started = row.number("startedAt") ?: return false
        val stateMatches = when (state) {
            null -> true
            ACTIVE -> row.string("state").let { it != null && it !in RunHistoryCodec.terminal }
            else -> row.string("state") == state
        }
        return stateMatches && (preset == null || row.string("preset") == preset) &&
            (from == null || started >= from) && (until == null || started < until) && matchesQuery(row)
    }

    private fun matchesQuery(row: JsonObject): Boolean {
        val needle = query?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() } ?: return true
        return listOfNotNull(row.string("goal"), row.string("preset"), row.getAsJsonObject("model")?.string("name"))
            .any { it.lowercase(Locale.ROOT).contains(needle) }
    }

    companion object {
        const val ACTIVE = "active"
    }
}
