package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.view.View
import android.widget.LinearLayout
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*

/**
 * Readable parameters for a confirmation: each name above its full, selectable value. Stacking keeps
 * long names and values legible at large font sizes and in narrow windows such as the floating card.
 * Literal values (numbers, booleans, null) use a monospace face so "false" and false stay distinct.
 */
internal fun Kit.parameterTable(rows: List<ArgumentRow>): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    background = roundedFill(palette.surfaceVariant, Ui.RADIUS_CONTROL)
    setPaddingRelative(dp(Ui.SPACE_MD), dp(Ui.SPACE_SM), dp(Ui.SPACE_MD), dp(Ui.SPACE_SM))
    tag = "parameter-table"
    addView(text(string(R.string.interaction_parameters), Ui.TEXT_SECTION, palette.muted, medium = true).apply {
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, dp(Ui.SPACE_XS), 0, dp(Ui.SPACE_XS))
    })
    if (rows.isEmpty()) addView(text(string(R.string.interaction_no_parameters), Ui.TEXT_BODY, palette.muted).apply {
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, dp(Ui.SPACE_XS), 0, dp(Ui.SPACE_SM))
    })
    rows.forEachIndexed { index, row ->
        if (index > 0) addView(View(context).apply { setBackgroundColor(palette.divider) }, LinearLayout.LayoutParams(-1, dp(1)))
        addView(text(row.name, Ui.TEXT_CAPTION, palette.muted, medium = true).apply {
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; textDirection = View.TEXT_DIRECTION_LTR
            setPaddingRelative(0, dp(Ui.SPACE_SM), 0, 0); setTextIsSelectable(true)
        })
        addView(text(row.value, Ui.TEXT_BODY, if (row.literal) palette.accent else palette.text).apply {
            if (row.literal) { typeface = Ui.monospace; textDirection = View.TEXT_DIRECTION_LTR }
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setPaddingRelative(0, dp(2), 0, dp(Ui.SPACE_SM)); setTextIsSelectable(true)
        })
    }
}
