package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Context
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*

internal object WorkbenchText {
    private val terminal = setOf("completed", "partial", "failed", "cancelled", "blocked")
    fun active(row: JsonObject) = row.string("state") !in terminal
    fun state(context: Context, row: JsonObject): String = context.getString(when (row.string("state")) {
        "queued" -> R.string.run_queued
        "running", "preparing", "observing", "deciding", "executing" -> R.string.run_running
        "waiting_input" -> R.string.run_waiting_input
        "waiting_confirmation" -> R.string.run_waiting_confirmation
        "cancelling" -> R.string.run_cancelling
        "completed" -> R.string.run_completed
        "partial" -> R.string.run_partial
        "failed" -> R.string.run_failed
        "cancelled" -> R.string.run_cancelled
        "blocked" -> R.string.run_blocked
        else -> R.string.run_running
    })
    /** Badge tone and icon for a task state, shared by the feed, history and details. */
    fun tone(row: JsonObject): Pair<io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.Tone, Int> = when (row.string("state")) {
        "completed" -> io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.Tone.SUCCESS to R.drawable.ic_check
        "failed", "blocked" -> io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.Tone.DANGER to R.drawable.ic_warning
        "partial", "cancelled" -> io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.Tone.WARNING to R.drawable.ic_block
        else -> io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.Tone.ACCENT to R.drawable.ic_timer
    }
    /** "Model calls: n, tokens: m" from a usage object; estimated totals are marked. Null without counters. */
    fun usage(context: Context, usage: JsonObject?): String? {
        usage ?: return null
        val calls = usage.number("modelCalls"); val tokens = usage.number("totalTokens")
        if (calls == null && tokens == null) return null
        val text = context.getString(R.string.history_usage_summary, calls ?: 0, tokens ?: 0)
        return if (usage.flag("estimated") == true) context.getString(R.string.history_usage_estimated, text) else text
    }
    fun summary(row: JsonObject): String = row.getAsJsonObject("result")?.string("summary").orEmpty()
    /** The latest tool attempt could not reach host accessibility even after unattended startup. A later ask step keeps it visible. */
    fun accessibilityBlocked(row: JsonObject?): Boolean = row?.getAsJsonArray("steps")?.lastOrNull {
        it.isJsonObject && it.asJsonObject.has("tool")
    }?.asJsonObject?.string("error") == "A11Y_SERVICE_NOT_RUNNING"
    fun budget(context: Context, row: JsonObject): String {
        val limits = row.getAsJsonObject("budget") ?: return ""
        return context.getString(R.string.workbench_budget, row.number("step") ?: 0, limits.number("maxSteps") ?: 0,
            limits.number("maxModelCalls") ?: 0, (limits.number("maxDurationMs") ?: 0) / 60000, limits.number("maxTotalTokens") ?: 0)
    }
}
