package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.google.android.material.snackbar.Snackbar

/** Centered empty state: tonal icon disc and a title. */
internal fun Kit.emptyState(title: CharSequence, @DrawableRes icon: Int? = null): LinearLayout =
    LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPaddingRelative(dp(Ui.SPACE_XXL), dp(Ui.SPACE_XXL), dp(Ui.SPACE_XXL), dp(Ui.SPACE_XXL))
        icon?.let {
            addView(FrameLayout(context).apply {
                background = roundedFill(palette.accentTone, Ui.RADIUS_PILL)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                addView(ImageView(context).apply { setImageDrawable(tintedDrawable(it, palette.accent)) }, FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER))
            }, LinearLayout.LayoutParams(dp(64), dp(64)))
        }
        addView(text(title, Ui.TEXT_TITLE, medium = true).apply {
            gravity = Gravity.CENTER; setPaddingRelative(0, dp(Ui.SPACE_LG), 0, dp(Ui.SPACE_SM))
        }, LinearLayout.LayoutParams(-1, -2))
    }

/**
 * End-aligned action buttons that sit side by side while they fit and stack vertically otherwise, so a
 * large text size on a narrow screen never squeezes a button into one character per line.
 */
internal class ActionRow(context: android.content.Context) : LinearLayout(context) {
    private val gap = (Ui.SPACE_XS * context.resources.displayMetrics.density + 0.5f).toInt()
    init { orientation = HORIZONTAL; gravity = Gravity.END }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var needed = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == View.GONE) continue
            val params = child.layoutParams as MarginLayoutParams
            child.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            needed += child.measuredWidth + params.leftMargin + params.rightMargin
        }
        val stacked = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED && needed > available
        val wanted = if (stacked) VERTICAL else HORIZONTAL
        if (orientation != wanted) {
            orientation = wanted
            for (index in 0 until childCount) (getChildAt(index).layoutParams as MarginLayoutParams).topMargin = if (stacked && index > 0) gap else 0
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

/** Inline notice with a tone, an optional icon and optional actions; announced politely. */
internal class Banner(private val kit: Kit) {
    val message: TextView = kit.text("", Ui.TEXT_BODY)
    private val icon = ImageView(kit.context).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
    val actions: LinearLayout = ActionRow(kit.context)
    val view: LinearLayout = LinearLayout(kit.context).apply {
        orientation = LinearLayout.VERTICAL
        setPaddingRelative(kit.dp(Ui.SPACE_LG), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_SM), kit.dp(Ui.SPACE_SM))
        addView(LinearLayout(kit.context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(icon, LinearLayout.LayoutParams(kit.dp(20), kit.dp(20)).apply { marginEnd = kit.dp(Ui.SPACE_MD); topMargin = kit.dp(2) })
            addView(message, LinearLayout.LayoutParams(0, -2, 1f))
        })
        addView(actions, LinearLayout.LayoutParams(-1, -2))
        message.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
    fun show(text: CharSequence, tone: Tone = Tone.NEUTRAL, @DrawableRes iconResource: Int? = null) {
        val (fill, foreground) = kit.toneColors(tone)
        view.background = kit.roundedFill(fill, Ui.RADIUS_CARD)
        message.text = text
        message.setTextColor(if (tone == Tone.NEUTRAL) kit.palette.text else foreground)
        icon.visibility = if (iconResource == null) View.GONE else View.VISIBLE
        iconResource?.let { icon.setImageDrawable(kit.tintedDrawable(it, foreground)) }
        view.visibility = View.VISIBLE
    }
    fun hide() { view.visibility = View.GONE }
}

/** Inverse-surface snackbar legible in both modes and any theme color. */
internal fun Kit.snackbar(anchor: View, message: CharSequence, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(anchor, message, duration).setBackgroundTint(palette.text).setTextColor(palette.background)
        .setActionTextColor(AgentColorPolicy.readableAccent(palette.primary, palette.text)).show()
}
