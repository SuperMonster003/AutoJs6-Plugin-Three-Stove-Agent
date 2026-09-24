package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Context
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

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
    fun summary(row: JsonObject): String = row.getAsJsonObject("result")?.string("summary").orEmpty()
    fun budget(context: Context, row: JsonObject): String {
        val limits = row.getAsJsonObject("budget") ?: return ""
        return context.getString(R.string.workbench_budget, row.number("step") ?: 0, limits.number("maxSteps") ?: 0,
            limits.number("maxModelCalls") ?: 0, (limits.number("maxDurationMs") ?: 0) / 60000, limits.number("maxTotalTokens") ?: 0)
    }
}
