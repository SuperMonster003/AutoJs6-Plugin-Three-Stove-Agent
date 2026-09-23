package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptConfirmation

internal object ScriptConfirmationView {
    fun create(context: Context, pending: JsonObject): View {
        val padding = (16 * context.resources.displayMetrics.density).toInt()
        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(padding, padding, padding, padding) }
        content.addView(TextView(context).apply { text = pending["description"].asString; setTextIsSelectable(true) })
        val table = TableLayout(context).apply { isShrinkAllColumns = true; isStretchAllColumns = true }
        fun row(name: String, value: String, header: Boolean = false) {
            table.addView(TableRow(context).apply {
                for (label in listOf(name, value)) addView(TextView(context).apply {
                    text = label; setPadding(padding / 2, padding / 2, padding / 2, padding / 2)
                    if (header) setTypeface(typeface, Typeface.BOLD) else setTextIsSelectable(true)
                })
            })
        }
        row(context.getString(R.string.confirmation_parameter), context.getString(R.string.confirmation_value), true)
        for ((name, value) in ScriptConfirmation.rows(pending.getAsJsonObject("arguments"))) row(name, value)
        content.addView(table)
        return ScrollView(context).apply { addView(content) }
    }
}
