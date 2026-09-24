package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Context
import android.widget.*
import com.google.gson.*
import java.text.DateFormat
import java.util.Date

internal object HistoryViews {
    fun column(context: Context) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val padding = (16 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding / 2, padding, padding / 2)
    }
    fun label(parent: LinearLayout, value: String, title: Boolean = false) = TextView(parent.context).apply {
        text = value; setTextIsSelectable(true); setPadding(0, 8, 0, 8)
        if (title) setTextAppearance(android.R.style.TextAppearance_Medium)
        parent.addView(this)
    }
    fun button(parent: LinearLayout, label: Int, tag: String, action: () -> Unit) = Button(parent.context).apply {
        setText(label); this.tag = tag; setOnClickListener { action() }; parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    fun date(context: Context, time: Long) = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
        context.resources.configuration.locales[0]).format(Date(time))
    fun pretty(value: JsonElement?) = value?.let { GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(it) }.orEmpty()
}
