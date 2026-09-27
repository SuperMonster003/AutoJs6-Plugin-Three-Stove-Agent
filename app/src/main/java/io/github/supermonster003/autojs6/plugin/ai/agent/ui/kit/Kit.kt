package io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit

import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.BaseProgressIndicator
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.HostAppearanceActivity

/** Design tokens shared by every standalone surface (3-Stone AI scale, AI Agent palette). */
internal object Ui {
    /** Role of a single-line label allowed to ellipsize; its full text must be in a content description. */
    const val TRUNCATABLE = "truncatable"
    fun truncatable(view: android.view.View) { view.setTag(io.github.supermonster003.autojs6.plugin.ai.agent.R.id.ui_role, TRUNCATABLE) }

    const val SPACE_XS = 4
    const val SPACE_SM = 8
    const val SPACE_MD = 12
    const val SPACE_LG = 16
    const val SPACE_XL = 20
    const val SPACE_XXL = 24
    const val SPACE_XXXL = 32
    const val SCREEN_MARGIN = 20
    const val SECTION_GAP = 24
    /** Content never grows wider than this on tablets and foldables. */
    const val MAX_CONTENT_WIDTH = 840

    const val RADIUS_CONTROL = 12
    const val RADIUS_CARD = 16
    const val RADIUS_BUBBLE = 20
    const val RADIUS_SHEET = 28
    const val RADIUS_PILL = 100

    const val TEXT_DISPLAY = 24f
    const val TEXT_PAGE_TITLE = 20f
    const val TEXT_TITLE = 17f
    const val TEXT_ITEM = 16f
    const val TEXT_BODY = 14.5f
    const val TEXT_SECONDARY = 13f
    const val TEXT_SECTION = 12.5f
    const val TEXT_CAPTION = 12f
    const val LINE_SPACING_BODY = 1.15f

    const val TOUCH_TARGET = 48
    const val ICON_SIZE = 24
    const val DISABLED_ALPHA = 0.42f

    val medium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val monospace: Typeface = Typeface.MONOSPACE
}

/**
 * The kit entry point: every builder hangs off a context plus its resolved palette, so the same
 * components work in activities, in dialogs and in the :agent floating overlay.
 */
internal class Kit(val context: Context, val palette: AgentPalette) {
    fun dp(value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()
    fun dpF(value: Float): Float = value * context.resources.displayMetrics.density
    fun string(resource: Int, vararg args: Any): String = context.getString(resource, *args)

    fun roundedFill(fill: Int, radiusDp: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dpF(radiusDp.toFloat())
        if (stroke != null) setStroke(dp(1), stroke)
    }

    fun roundedRippleFill(fill: Int, radiusDp: Int, stroke: Int? = null, ripple: Int = palette.accentRipple): RippleDrawable =
        RippleDrawable(ColorStateList.valueOf(ripple), roundedFill(fill, radiusDp, stroke), null)

    fun selectableBackground(view: View, borderless: Boolean = false) {
        val value = TypedValue()
        val attribute = if (borderless) android.R.attr.selectableItemBackgroundBorderless else android.R.attr.selectableItemBackground
        if (view.context.theme.resolveAttribute(attribute, value, true)) view.setBackgroundResource(value.resourceId)
    }

    fun tintedDrawable(@DrawableRes resource: Int, color: Int): Drawable? =
        AppCompatResources.getDrawable(context, resource)?.let { tinted(it, color) }

    fun tinted(drawable: Drawable, color: Int): Drawable =
        DrawableCompat.wrap(drawable.mutate()).also { DrawableCompat.setTint(it, color) }

    fun text(value: CharSequence?, size: Float = Ui.TEXT_BODY, color: Int = palette.text, medium: Boolean = false): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (medium) typeface = Ui.medium
            setLineSpacing(0f, Ui.LINE_SPACING_BODY)
        }

    fun controlTintList(): ColorStateList = ColorStateList(
        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_focused), intArrayOf()),
        intArrayOf(AgentColorPolicy.withAlpha(palette.muted, 0x66), palette.accent, palette.accent, palette.muted),
    )

    fun switchThumbTintList(): ColorStateList = ColorStateList(
        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(AgentColorPolicy.withAlpha(palette.muted, 0x55), palette.onPrimary, palette.muted),
    )

    fun switchTrackTintList(): ColorStateList = ColorStateList(
        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(AgentColorPolicy.withAlpha(palette.muted, 0x24), palette.primary, palette.surfaceVariant),
    )

    fun tintEditText(editText: EditText) {
        editText.highlightColor = AgentColorPolicy.withAlpha(palette.accent, 0x55)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            editText.textCursorDrawable?.let { editText.textCursorDrawable = tinted(it, palette.accent) }
            editText.textSelectHandle?.let { editText.setTextSelectHandle(tinted(it, palette.accent)) }
            editText.textSelectHandleLeft?.let { editText.setTextSelectHandleLeft(tinted(it, palette.accent)) }
            editText.textSelectHandleRight?.let { editText.setTextSelectHandleRight(tinted(it, palette.accent)) }
        }
    }

    /** Recursively applies the runtime palette to framework and Material controls not built by the kit. */
    fun applyThemeToControls(root: View) {
        when (root) {
            is SwitchCompat -> { root.thumbTintList = switchThumbTintList(); root.trackTintList = switchTrackTintList() }
            is CompoundButton -> root.buttonTintList = controlTintList()
            is CheckedTextView -> root.checkMarkTintList = controlTintList()
            is EditText -> tintEditText(root)
            is BaseProgressIndicator<*> -> { root.setIndicatorColor(palette.accent); root.trackColor = AgentColorPolicy.withAlpha(palette.accent, 0x33) }
            is ProgressBar -> {
                root.progressTintList = ColorStateList.valueOf(palette.accent)
                root.indeterminateTintList = ColorStateList.valueOf(palette.accent)
            }
            is MaterialButton -> Unit
        }
        if (root is ViewGroup) for (index in 0 until root.childCount) applyThemeToControls(root.getChildAt(index))
    }

    fun tintDialogButtons(dialog: AlertDialog, destructive: Boolean = false) {
        dialog.listView?.let(::applyThemeToControls)
        for (which in listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL)) {
            dialog.getButton(which)?.apply {
                isAllCaps = false
                minHeight = dp(Ui.TOUCH_TARGET); minWidth = dp(Ui.TOUCH_TARGET)
                setTextColor(if (destructive && which == AlertDialog.BUTTON_POSITIVE) palette.danger else palette.accent)
            }
        }
    }

    companion object {
        /** The activity's kit when [context] belongs to one; otherwise a kit from the stored appearance. */
        fun of(context: Context): Kit {
            var current: Context? = context
            while (current != null) {
                if (current is HostAppearanceActivity) return current.kit
                current = (current as? ContextWrapper)?.baseContext?.takeIf { it !== current }
            }
            val palette = AgentPalette.resolve(context, AppearancePreferences.resolve(context))
            return Kit(materialContext(context, palette.isDark), palette)
        }

        /**
         * Material widgets require a Material theme. Application, configuration and overlay contexts
         * may carry a platform theme, so they are wrapped in the app theme for the current night mode.
         */
        fun materialContext(context: Context, dark: Boolean): Context {
            val attributes = context.theme.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorPrimaryVariant))
            val material = attributes.hasValue(0); attributes.recycle()
            return if (material) context else androidx.appcompat.view.ContextThemeWrapper(context,
                if (dark) io.github.supermonster003.autojs6.plugin.ai.agent.R.style.Theme_AiAgent_Dark
                else io.github.supermonster003.autojs6.plugin.ai.agent.R.style.Theme_AiAgent_Light)
        }
    }
}
