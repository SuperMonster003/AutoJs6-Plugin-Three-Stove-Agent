package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.ModelSelectionState
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*

/** Compact pill naming the model for new tasks. Tapping it opens the shared model switcher. */
internal class ModelCapsule(private val kit: Kit, onClick: () -> Unit) {
    private val icon = ImageView(kit.context).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
    /** Model names are unbounded; the label may ellipsize because the capsule's description states the full name. */
    private val label: TextView = kit.text("", Ui.TEXT_SECONDARY, kit.palette.text, medium = true).apply {
        Ui.truncatable(this); maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        maxWidth = kit.dp(220); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    val view: LinearLayout = LinearLayout(kit.context).apply {
        id = R.id.workbench_model
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = kit.dp(Ui.TOUCH_TARGET)
        setPaddingRelative(kit.dp(Ui.SPACE_MD), 0, kit.dp(Ui.SPACE_SM), 0)
        isClickable = true; isFocusable = true
        addView(icon, LinearLayout.LayoutParams(kit.dp(18), kit.dp(18)).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
        // Weighted so that, when the bar is narrow, the label shrinks instead of pushing the arrow out.
        addView(label, LinearLayout.LayoutParams(-2, -2, 1f))
        addView(ImageView(kit.context).apply {
            setImageDrawable(kit.tintedDrawable(R.drawable.ic_expand, kit.palette.muted))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(kit.dp(20), kit.dp(20)).apply { marginStart = kit.dp(Ui.SPACE_XS) })
        accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info); info.className = android.widget.Button::class.java.name
            }
        }
        setOnClickListener { onClick() }
    }

    /** Renders the stored choice. [automaticPick] names the model Automatic would use, when known. */
    fun render(selection: ModelSelectionState, automaticPick: String?, available: Boolean, connected: Boolean = true) {
        val current = selection.current
        val name = current?.name ?: automaticPick?.let { kit.string(R.string.model_automatic_with, it) } ?: kit.string(R.string.model_automatic)
        label.text = name
        label.setTextColor(if (available) kit.palette.text else kit.palette.danger)
        // The spark takes the accent color only while AutoJs6 is attached.
        icon.setImageDrawable(kit.tintedDrawable(if (available) R.drawable.ic_spark else R.drawable.ic_warning,
            if (!available) kit.palette.danger else if (connected) kit.palette.accent else kit.palette.muted))
        val fill = if (available) kit.palette.accentTone else kit.palette.dangerSurface
        view.background = kit.roundedRippleFill(fill, Ui.RADIUS_PILL, if (available) null else kit.palette.danger)
        view.contentDescription = kit.string(R.string.model_capsule_description,
            if (available) name else kit.string(R.string.presets_unavailable, name))
    }

}
