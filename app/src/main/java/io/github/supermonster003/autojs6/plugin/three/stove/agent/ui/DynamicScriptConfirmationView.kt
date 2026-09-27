package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*

/** Generated JavaScript is plain, selectable, left-to-right text, never rendered as markup. */
internal object DynamicScriptConfirmationView {
    fun create(context: Context, arguments: JsonObject, expanded: Boolean = false,
        confirmation: Boolean = true, kit: Kit = Kit.of(context), onExpanded: (Boolean) -> Unit = {}): View {
        val source = arguments.string("source").orEmpty()
        val preview = AgentJson.truncate(source.lineSequence().take(8).joinToString("\n"), 1024)
            .let { if (it == source) it else "$it\n..." }
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        fun add(view: View, top: Int = Ui.SPACE_XS) = column.addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(top) })
        fun label(value: String, color: Int = kit.palette.muted, size: Float = Ui.TEXT_SECONDARY): TextView = kit.text(value, size, color).apply {
            setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
        if (confirmation) add(label(context.getString(R.string.script_dynamic_warning), kit.toneColors(Tone.WARNING).second, Ui.TEXT_BODY).apply {
            background = kit.roundedFill(kit.palette.warningSurface, Ui.RADIUS_CONTROL)
            setPaddingRelative(kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_SM), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_SM))
        }, top = Ui.SPACE_SM)
        add(label(context.getString(R.string.script_dynamic_source_summary, source.utf8Size(), source.count { it == '\n' } + 1)))
        arguments.number("timeoutMs")?.let { add(label(context.getString(R.string.script_dynamic_timeout, it))) }
        add(kit.text(context.getString(R.string.script_dynamic_source), Ui.TEXT_SECTION, kit.palette.muted, medium = true).apply {
            labelFor = R.id.script_dynamic_source; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }, top = Ui.SPACE_MD)
        var showingFull = expanded
        val code = kit.text(if (showingFull) source else preview, Ui.TEXT_SECONDARY).apply {
            id = R.id.script_dynamic_source
            typeface = Ui.monospace
            setTextIsSelectable(true)
            textDirection = View.TEXT_DIRECTION_LTR
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            background = kit.roundedFill(kit.palette.surfaceVariant, Ui.RADIUS_CONTROL)
            setPaddingRelative(kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_SM), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_SM))
        }
        add(code)
        lateinit var toggle: com.google.android.material.button.MaterialButton
        toggle = kit.textButton(context.getString(if (showingFull) R.string.script_dynamic_collapse else R.string.script_dynamic_expand)) {
            showingFull = !showingFull
            code.text = if (showingFull) source else preview
            toggle.setText(if (showingFull) R.string.script_dynamic_collapse else R.string.script_dynamic_expand)
            onExpanded(showingFull)
        }.apply { id = R.id.script_dynamic_expand }
        column.addView(toggle, LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
        return column
    }
}
