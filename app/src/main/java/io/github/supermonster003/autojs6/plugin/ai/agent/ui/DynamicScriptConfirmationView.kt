package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

/** Generated JavaScript is plain, selectable text, never rendered as markup. */
internal object DynamicScriptConfirmationView {
    fun create(context: Context, arguments: JsonObject, expanded: Boolean = false,
        confirmation: Boolean = true, onExpanded: (Boolean) -> Unit = {}): View {
        val source = arguments.string("source").orEmpty()
        val preview = AgentJson.truncate(source.lineSequence().take(8).joinToString("\n"), 1024)
            .let { if (it == source) it else "$it\n..." }
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        fun label(value: String) = TextView(context).apply {
            text = value; setTextIsSelectable(true); column.addView(this)
        }
        if (confirmation) label(context.getString(R.string.script_dynamic_warning))
        label(context.getString(R.string.script_dynamic_source_summary, source.toByteArray(Charsets.UTF_8).size,
            source.count { it == '\n' } + 1))
        arguments.number("timeoutMs")?.let { label(context.getString(R.string.script_dynamic_timeout, it)) }
        label(context.getString(R.string.script_dynamic_source)).labelFor = R.id.script_dynamic_source
        var showingFull = expanded
        val code = label(if (showingFull) source else preview).apply {
            id = R.id.script_dynamic_source
            typeface = Typeface.MONOSPACE
            textDirection = View.TEXT_DIRECTION_LTR
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        column.addView(Button(context).apply {
            id = R.id.script_dynamic_expand
            isAllCaps = false
            minHeight = (48 * resources.displayMetrics.density).toInt()
            setText(if (showingFull) R.string.script_dynamic_collapse else R.string.script_dynamic_expand)
            setOnClickListener {
                showingFull = !showingFull
                code.text = if (showingFull) source else preview
                setText(if (showingFull) R.string.script_dynamic_collapse else R.string.script_dynamic_expand)
                onExpanded(showingFull)
            }
        })
        return column
    }
}
