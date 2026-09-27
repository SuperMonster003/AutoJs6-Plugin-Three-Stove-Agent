package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.AgentColorPolicy
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.AgentPalette

/** Shared spacing, surfaces, typography and navigation for every standalone screen. */
internal object AgentUi {
    data class Palette(val background: Int, val surface: Int, val inset: Int, val text: Int, val muted: Int,
                       val border: Int, val accent: Int, val onAccent: Int, val soft: Int, val danger: Int)
    fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density + .5f).toInt()
    private fun appearanceOf(context: Context): HostAppearance? = when (context) {
        is HostAppearanceActivity -> context.appearance
        is android.content.ContextWrapper -> context.baseContext.takeIf { it !== context }?.let(::appearanceOf)
        else -> null
    }
    /** Legacy view of [AgentPalette] kept until the last screen moves to the kit (roadmap D45). */
    fun palette(context: Context, appearance: HostAppearance? = appearanceOf(context)): Palette {
        val p = AgentPalette.resolve(context, appearance)
        return Palette(p.background, p.surface, p.surfaceVariant, p.text, p.muted, p.outline, p.accent,
            AgentColorPolicy.onFilledColor(p.accent), AgentColorPolicy.blend(p.surface, p.accent, .09), p.danger)
    }
    fun shape(context: Context, fill: Int, radius: Int = 16, stroke: Int? = null) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(context, radius).toFloat()
        stroke?.let { setStroke(dp(context, 1), it) }
    }
    fun role(view: View, name: String): View { view.setTag(R.id.ui_role, name); return view }
    fun column(context: Context, padding: Int = 20) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPaddingRelative(dp(context, padding), dp(context, 12), dp(context, padding), dp(context, 24))
    }
    fun card(parent: LinearLayout): LinearLayout = column(parent.context, 18).apply {
        role(this, "card"); background = shape(context, palette(context).surface, 22)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 16) })
    }
    fun section(parent: LinearLayout, title: Int) = text(parent, parent.context.getString(title), 13, true).apply {
        setTextColor(palette(context).muted)
        setPaddingRelative(dp(context, 4), dp(context, 16), 0, dp(context, 10))
        if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
    }
    fun text(parent: LinearLayout, value: CharSequence, size: Int = 15, bold: Boolean = false) = TextView(parent.context).apply {
        text = value; textSize = size.toFloat(); setTextColor(palette(context).text)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setLineSpacing(dp(context, 2).toFloat(), 1.08f)
        setPaddingRelative(0, dp(context, 6), 0, dp(context, 6))
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    fun action(parent: LinearLayout, label: Int, tag: String, primary: Boolean = false, action: () -> Unit): Button = Button(parent.context).apply {
        setText(label); this.tag = tag; role(this, if (primary) "primary" else "secondary")
        setOnClickListener { action() }
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(context, 6); bottomMargin = dp(context, 6) })
        style(this)
    }
    fun row(parent: LinearLayout, title: CharSequence, summary: CharSequence? = null, tag: String, action: () -> Unit): Button = Button(parent.context).apply {
        this.tag = tag; role(this, "row")
        text = if (summary.isNullOrBlank()) title else android.text.SpannableStringBuilder(title).apply {
            append('\n'); val start = length; append(summary)
            setSpan(android.text.style.ForegroundColorSpan(palette(context).muted), start, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(android.text.style.RelativeSizeSpan(.86f), start, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(android.text.style.TypefaceSpan("sans-serif"), start, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        setOnClickListener { action() }
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 4) })
        style(this)
        val arrow = context.getDrawable(R.drawable.ic_chevron)!!.mutate().apply { setTint(palette(context).muted); setBounds(0, 0, dp(context, 20), dp(context, 20)) }
        compoundDrawablePadding = dp(context, 12); setCompoundDrawablesRelative(null, null, arrow, null)
    }
    fun disclosure(parent: LinearLayout, title: Int, summary: Int? = null, expanded: Boolean = false): LinearLayout {
        val nested = parent.getTag(R.id.ui_role) in setOf("card", "disclosure-content")
        val card = if (nested) LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(context, 8); bottomMargin = dp(context, 8) })
        } else card(parent)
        val toggle = row(card, parent.context.getString(title), summary?.let(parent.context::getString), "section-$title") {}
        val state = (parent.context as? HostAppearanceActivity)?.sectionState
        val content = column(parent.context, 0).apply {
            role(this, "disclosure-content")
            visibility = if (state?.getOrPut(title) { expanded } ?: expanded) View.VISIBLE else View.GONE
        }
        card.addView(content)
        fun update() {
            toggle.isActivated = content.visibility == View.VISIBLE
            state?.set(title, toggle.isActivated)
            toggle.contentDescription = parent.context.getString(title) + ", " + parent.context.getString(
                if (toggle.isActivated) R.string.ui_collapse else R.string.ui_expand)
            val arrow = parent.context.getDrawable(if (toggle.isActivated) R.drawable.ic_collapse else R.drawable.ic_expand)!!.mutate().apply {
                setTint(palette(parent.context).muted); setBounds(0, 0, dp(parent.context, 20), dp(parent.context, 20))
            }
            toggle.setCompoundDrawablesRelative(null, null, arrow, null)
        }
        toggle.setOnClickListener { content.visibility = if (content.visibility == View.VISIBLE) View.GONE else View.VISIBLE; update() }
        update()
        return content
    }
    fun toolbar(activity: Activity, title: CharSequence, onBack: (() -> Unit)? = { activity.finish() }): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(activity, 64); setPaddingRelative(dp(activity, 8), dp(activity, 6), dp(activity, 12), dp(activity, 6))
        if (onBack != null) addView(icon(activity, R.drawable.ic_back, R.string.workbench_back, "back", onBack))
        addView(TextView(activity).apply {
            text = title; textSize = 21f; setTextColor(palette(activity).text)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPaddingRelative(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8))
            if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }
    fun icon(context: Context, drawable: Int, label: Int, tag: String, action: () -> Unit) = ImageButton(context).apply {
        this.tag = tag; contentDescription = context.getString(label); setImageResource(drawable)
        imageTintList = ColorStateList.valueOf(palette(context).text)
        background = RippleDrawable(ColorStateList.valueOf(palette(context).soft), null, shape(context, Color.WHITE, 16))
        setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12))
        layoutParams = LinearLayout.LayoutParams(dp(context, 48), dp(context, 48))
        setOnClickListener { action() }
    }
    fun screen(activity: Activity, title: CharSequence, content: View, scroll: Boolean = true, onBack: (() -> Unit)? = { activity.finish() }): LinearLayout = column(activity, 0).apply {
        setPadding(0, 0, 0, 0); fitsSystemWindows = true; layoutDirection = activity.resources.configuration.layoutDirection
        background = shape(activity, palette(activity).background, 0)
        addView(toolbar(activity, title, onBack))
        val bounded = object : FrameLayout(activity) {
            override fun onMeasure(width: Int, height: Int) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(minOf(MeasureSpec.getSize(width), dp(context, 840)), MeasureSpec.getMode(width)), height)
            }
        }.apply { addView(content, FrameLayout.LayoutParams(-1, -2)) }
        val centered = FrameLayout(activity).apply { addView(bounded, FrameLayout.LayoutParams(-1, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL)) }
        val body = if (scroll) ScrollView(activity).apply { isFillViewport = true; clipToPadding = false; addView(centered) } else content
        if (!scroll) bounded.removeView(content)
        addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
    }
    fun style(view: View, appearance: HostAppearance? = (view.context as? HostAppearanceActivity)?.appearance) {
        val p = palette(view.context, appearance)
        val role = view.getTag(R.id.ui_role) as? String
        val minimum = dp(view.context, 48)
        when (view) {
            // Kit-built Material widgets carry their own palette styling.
            is MaterialButton, is MaterialSwitch, is Chip, is TextInputEditText, is com.google.android.material.checkbox.MaterialCheckBox -> Unit
            is CompoundButton -> {
                view.minHeight = minimum; view.minWidth = minimum; view.textSize = 15f; view.setTextColor(p.text)
                val colors = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(p.accent, p.muted))
                view.buttonTintList = colors
                if (view is Switch) { view.thumbTintList = colors; view.trackTintList = ColorStateList.valueOf(p.border) }
                view.setPaddingRelative(dp(view.context, 4), dp(view.context, 8), dp(view.context, 4), dp(view.context, 8))
            }
            is Button -> {
                view.isAllCaps = false; view.textSize = 15f; view.minimumHeight = minimum; view.minimumWidth = minimum
                view.stateListAnimator = null
                view.gravity = if (role == "row") Gravity.START or Gravity.CENTER_VERTICAL else Gravity.CENTER
                val color = when (role) { "primary" -> p.accent; "danger" -> p.danger; else -> p.text }
                val fill = when (role) { "primary" -> p.accent; "row" -> if (view.isSelected) p.soft else p.surface; else -> p.soft }
                val foreground = if (role == "primary") p.onAccent else if (role == "row" && view.isSelected) p.accent else color
                val radius = if (view.tag == "floating-toggle") 28 else 14
                // Separate drawables retain their fills when Android mutates a replacement background.
                val surface = if (role == "primary") android.graphics.drawable.StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_enabled), shape(view.context, fill, radius))
                    addState(intArrayOf(), shape(view.context, p.inset, radius))
                } else shape(view.context, fill, radius)
                view.setTextColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf()), intArrayOf(foreground, p.muted)))
                view.backgroundTintList = null
                // Mutate before View sets the current state; tint application otherwise clones
                // LayerDrawable children after that state was already dispatched.
                view.background = RippleDrawable(ColorStateList.valueOf(p.border), surface, null).mutate()
                view.setPaddingRelative(dp(view.context, 16), dp(view.context, 14), dp(view.context, 16), dp(view.context, 14))
            }
            is EditText -> {
                view.minimumHeight = minimum; view.minimumWidth = minimum; view.textSize = 16f
                view.setTextColor(p.text); view.setHintTextColor(p.muted)
                view.backgroundTintList = null; view.background = shape(view.context, p.inset, 14, p.border)
                view.setPaddingRelative(dp(view.context, 14), dp(view.context, 14), dp(view.context, 14), dp(view.context, 14))
                if (android.os.Build.VERSION.SDK_INT >= 26) view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            }
            is Spinner -> { view.minimumHeight = minimum; view.minimumWidth = minimum; view.setPopupBackgroundDrawable(shape(view.context, p.surface, 16)) }
            is ProgressBar -> { view.progressTintList = ColorStateList.valueOf(p.accent); view.indeterminateTintList = ColorStateList.valueOf(p.accent) }
        }
        if (role == "card") view.background = shape(view.context, p.surface, 22)
        if (view is ViewGroup) for (index in 0 until view.childCount) style(view.getChildAt(index), appearance)
    }
}
