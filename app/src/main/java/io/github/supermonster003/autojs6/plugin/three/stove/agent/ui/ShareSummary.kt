package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Context
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*

/**
 * The text handed to the system share sheet for a finished task (roadmap P13): the goal, the terminal
 * state with the summary, evidence and unfinished work. Observations, arguments, script results and
 * error details never leave the private history this way; summary and evidence were already redacted
 * by the journal.
 */
internal object ShareSummary {
    fun text(context: Context, run: JsonObject): String? {
        val result = run.getAsJsonObject("result") ?: return null
        if (WorkbenchText.active(run)) return null
        val lines = mutableListOf<String>()
        run.string("goal")?.takeIf { it.isNotBlank() }?.let { lines += it; lines += "" }
        lines += WorkbenchText.state(context, run) + ": " + result.string("summary").orEmpty()
        for ((field, title) in listOf("evidence" to R.string.history_evidence, "unfinished" to R.string.history_unfinished)) {
            val items = result.getAsJsonArray(field)?.takeIf { it.size() > 0 } ?: continue
            lines += ""; lines += context.getString(title) + ":"
            items.forEach { item -> if (item.isJsonPrimitive) lines += "- " + item.asString }
        }
        lines += ""; lines += context.getString(R.string.history_share_footer, context.getString(R.string.app_name))
        return lines.joinToString("\n")
    }
}
