package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit

import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.chip.Chip

/** The single card shape: surface fill, rounded corners, hairline outline. Selected cards gain an accent tone. */
internal fun Kit.card(interactive: Boolean = false, selected: Boolean = false, fill: Int? = null, stroke: Int? = null): LinearLayout =
    LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val surface = fill ?: if (selected) palette.accentTone else palette.surface
        val border = stroke ?: if (selected) palette.accent else palette.outline
        background = if (interactive) roundedRippleFill(surface, Ui.RADIUS_CARD, border) else roundedFill(surface, Ui.RADIUS_CARD, border)
        if (interactive) { isClickable = true; isFocusable = true }
        setPaddingRelative(dp(Ui.SPACE_LG), dp(Ui.SPACE_LG - 2), dp(Ui.SPACE_LG), dp(Ui.SPACE_LG - 2))
    }

internal fun Kit.cardParams(topDp: Int = 0, bottomDp: Int = Ui.SPACE_MD): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(topDp); bottomMargin = dp(bottomDp) }

internal enum class Tone { NEUTRAL, ACCENT, SUCCESS, WARNING, DANGER }

internal fun Kit.toneColors(tone: Tone): Pair<Int, Int> = when (tone) {
    Tone.NEUTRAL -> palette.surfaceVariant to palette.muted
    Tone.ACCENT -> palette.accentTone to palette.accent
    Tone.SUCCESS -> palette.successSurface to palette.success
    Tone.WARNING -> palette.warningSurface to palette.warning
    Tone.DANGER -> palette.dangerSurface to palette.danger
}

/** Non-interactive status marker. A TextView, not a chip, so it is never mistaken for a control. */
internal fun Kit.badge(label: CharSequence, tone: Tone = Tone.NEUTRAL): TextView = TextView(context).apply {
    val (fill, foreground) = toneColors(tone)
    text = label
    textSize = Ui.TEXT_CAPTION
    typeface = Ui.medium
    setTextColor(foreground)
    gravity = Gravity.CENTER_VERTICAL
    background = roundedFill(fill, Ui.RADIUS_PILL)
    setPaddingRelative(dp(10), dp(3), dp(10), dp(3))
}

/** Interactive assist or filter chip, tinted from the runtime palette with a 48dp touch target. */
internal fun Kit.chip(label: CharSequence, tag: String? = null, checkable: Boolean = false, checked: Boolean = false,
                      icon: Int? = null, onClick: (() -> Unit)? = null): Chip = Chip(context).apply {
    text = label
    this.tag = tag
    isCheckable = checkable
    isChecked = checked
    isCheckedIconVisible = false
    setEnsureMinTouchTargetSize(true)
    textSize = Ui.TEXT_SECONDARY
    val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
    chipBackgroundColor = android.content.res.ColorStateList(states, intArrayOf(palette.accentTone, palette.surface))
    chipStrokeColor = android.content.res.ColorStateList(states, intArrayOf(palette.accent, palette.outline))
    chipStrokeWidth = dpF(1f)
    setTextColor(android.content.res.ColorStateList(states, intArrayOf(palette.accent, palette.text)))
    rippleColor = android.content.res.ColorStateList.valueOf(palette.accentRipple)
    icon?.let {
        chipIcon = tintedDrawable(it, palette.muted)
        isChipIconVisible = true
        chipIconSize = dpF(18f)
    }
    onClick?.let { action -> setOnClickListener { action() } }
}
