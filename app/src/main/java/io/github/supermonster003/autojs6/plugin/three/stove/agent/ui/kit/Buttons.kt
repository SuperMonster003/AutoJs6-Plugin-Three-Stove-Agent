package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit

import android.content.res.ColorStateList
import android.widget.ImageButton
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.google.android.material.button.MaterialButton

/**
 * Button factories. Every variant applies the runtime palette explicitly so the host or custom
 * theme color always wins over static theme attributes. Touch targets never drop below 48dp.
 */
private fun Kit.baseButton(label: CharSequence, tag: String?, style: Int = 0): MaterialButton =
    (if (style == 0) MaterialButton(context) else MaterialButton(context, null, style)).apply {
        text = label
        this.tag = tag
        isAllCaps = false
        textSize = 14.5f
        typeface = Ui.medium
        minimumHeight = dp(Ui.TOUCH_TARGET); minHeight = dp(Ui.TOUCH_TARGET)
        minimumWidth = dp(Ui.TOUCH_TARGET); minWidth = dp(Ui.TOUCH_TARGET)
        insetTop = 0; insetBottom = 0
        cornerRadius = dp(Ui.RADIUS_PILL)
        setPaddingRelative(dp(20), 0, dp(20), 0)
        stateListAnimator = null
    }

private fun Kit.states(disabled: Int, enabled: Int) =
    ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(disabled, enabled))

/** High-emphasis filled button: the single primary action of a region. */
internal fun Kit.filledButton(label: CharSequence, tag: String? = null, onClick: () -> Unit): MaterialButton = baseButton(label, tag).apply {
    backgroundTintList = states(palette.surfaceVariant, palette.primary)
    setTextColor(states(AgentColorPolicy.withAlpha(palette.muted, 0x99), palette.onPrimary))
    iconTint = states(AgentColorPolicy.withAlpha(palette.muted, 0x99), palette.onPrimary)
    rippleColor = ColorStateList.valueOf(AgentColorPolicy.withAlpha(palette.onPrimary, 0x33))
    setOnClickListener { onClick() }
}

/** Medium-emphasis tonal button. */
internal fun Kit.tonalButton(label: CharSequence, tag: String? = null, onClick: () -> Unit): MaterialButton = baseButton(label, tag).apply {
    backgroundTintList = states(AgentColorPolicy.withAlpha(palette.muted, 0x14), palette.accentTone)
    setTextColor(states(AgentColorPolicy.withAlpha(palette.muted, 0x99), palette.accent))
    iconTint = states(AgentColorPolicy.withAlpha(palette.muted, 0x99), palette.accent)
    rippleColor = ColorStateList.valueOf(palette.accentRipple)
    setOnClickListener { onClick() }
}

/** Outlined button for secondary choices; [danger] recolors it for destructive decisions. */
internal fun Kit.outlinedButton(label: CharSequence, tag: String? = null, danger: Boolean = false, onClick: () -> Unit): MaterialButton =
    baseButton(label, tag).apply {
        val color = if (danger) palette.danger else palette.text
        backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        strokeColor = states(AgentColorPolicy.withAlpha(palette.muted, 0x33), if (danger) AgentColorPolicy.withAlpha(palette.danger, 0x99) else palette.outline)
        strokeWidth = dp(1)
        setTextColor(states(AgentColorPolicy.withAlpha(palette.muted, 0x99), color))
        iconTint = states(AgentColorPolicy.withAlpha(palette.muted, 0x99), color)
        rippleColor = ColorStateList.valueOf(AgentColorPolicy.withAlpha(color, 0x22))
        setOnClickListener { onClick() }
    }

/** Low-emphasis text button. */
internal fun Kit.textButton(label: CharSequence, tag: String? = null, danger: Boolean = false, onClick: () -> Unit): MaterialButton =
    baseButton(label, tag, androidx.appcompat.R.attr.borderlessButtonStyle).apply {
        val color = if (danger) palette.danger else palette.accent
        backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        setPaddingRelative(dp(Ui.SPACE_MD), 0, dp(Ui.SPACE_MD), 0)
        setTextColor(states(AgentColorPolicy.withAlpha(palette.muted, 0x99), color))
        iconTint = states(AgentColorPolicy.withAlpha(palette.muted, 0x99), color)
        rippleColor = ColorStateList.valueOf(AgentColorPolicy.withAlpha(color, 0x2E))
        setOnClickListener { onClick() }
    }

/** 48dp icon button with a borderless ripple. */
internal fun Kit.iconButton(@DrawableRes icon: Int, description: CharSequence, tag: String? = null,
                            tint: Int = palette.text, onClick: () -> Unit): ImageButton = ImageButton(context).apply {
    this.tag = tag
    layoutParams = android.view.ViewGroup.LayoutParams(dp(Ui.TOUCH_TARGET), dp(Ui.TOUCH_TARGET))
    minimumWidth = dp(Ui.TOUCH_TARGET); minimumHeight = dp(Ui.TOUCH_TARGET)
    background = null
    selectableBackground(this, borderless = true)
    scaleType = ImageView.ScaleType.CENTER
    setImageDrawable(tintedDrawable(icon, tint))
    contentDescription = description
    setOnClickListener { onClick() }
}
