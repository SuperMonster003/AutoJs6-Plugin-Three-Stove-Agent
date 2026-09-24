package io.github.supermonster003.autojs6.plugin.ai.agent.store

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

internal data class RunHistoryFilter(val state: String? = null, val preset: String? = null, val from: Long? = null, val until: Long? = null) {
    fun matches(row: JsonObject): Boolean {
        val started = row.number("startedAt") ?: return false
        return (state == null || row.string("state") == state) && (preset == null || row.string("preset") == preset) &&
            (from == null || started >= from) && (until == null || started < until)
    }
}
