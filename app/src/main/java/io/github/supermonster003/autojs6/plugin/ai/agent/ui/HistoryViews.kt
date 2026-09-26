package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Context
import android.widget.*
import com.google.gson.*
import java.text.DateFormat
import java.util.Date

internal object HistoryViews {
    fun column(context: Context) = AgentUi.column(context)
    fun label(parent: LinearLayout, value: String, title: Boolean = false) = TextView(parent.context).apply {
        text = value; setTextIsSelectable(true)
        setTextColor(AgentUi.palette(context).let { if (title) it.text else it.muted })
        textSize = if (title) 20f else 14f
        if (title) typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        setLineSpacing(AgentUi.dp(context, 2).toFloat(), 1.1f)
        setPaddingRelative(0, AgentUi.dp(context, if (title) 16 else 8), 0, AgentUi.dp(context, 8))
        if (title && android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    fun button(parent: LinearLayout, label: Int, tag: String, action: () -> Unit) = AgentUi.action(parent, label, tag,
        tag in setOf("save", "preset-save", "preset-new", "mcp-save", "memory-save", "rerun"), action)
    fun date(context: Context, time: Long) = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
        context.resources.configuration.locales[0]).format(Date(time))
    fun pretty(value: JsonElement?) = value?.let { GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(it) }.orEmpty()
}
