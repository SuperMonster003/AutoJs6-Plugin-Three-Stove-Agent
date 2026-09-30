package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit

import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R

/** Accent-colored group header; exposed to accessibility services as a heading. */
internal fun Kit.sectionHeader(title: CharSequence): TextView = TextView(context).apply {
    text = title
    textSize = 14f
    typeface = Ui.medium
    setTextColor(palette.muted)
    setPaddingRelative(dp(Ui.SPACE_XXL), dp(Ui.SECTION_GAP), dp(Ui.SPACE_XXL), dp(Ui.SPACE_SM))
    if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
}

internal fun Kit.hairline(insetStartDp: Int = Ui.SPACE_XXL): View = View(context).apply {
    setBackgroundColor(palette.divider)
    layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply { marginStart = dp(insetStartDp) }
}

internal class SettingRow(val view: LinearLayout, val title: TextView, val summary: TextView, val switch: MaterialSwitch?) {
    fun setSummary(value: CharSequence?) {
        summary.text = value
        summary.visibility = if (value.isNullOrEmpty()) View.GONE else View.VISIBLE
        refreshDescription()
    }
    fun setEnabled(enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else Ui.DISABLED_ALPHA
        switch?.isEnabled = enabled
    }
    internal fun refreshDescription() {
        view.contentDescription = listOf(title.text, summary.text.takeIf { summary.visibility == View.VISIBLE })
            .filter { !it.isNullOrEmpty() }.joinToString(", ")
    }
}

private fun Kit.rowShell(tag: String?): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = dp(72)
    setPaddingRelative(dp(Ui.SPACE_XXL), dp(Ui.SPACE_MD), dp(Ui.SPACE_XXL), dp(Ui.SPACE_MD))
    this.tag = tag
}

private fun Kit.rowIcon(@DrawableRes icon: Int, color: Int = palette.muted): ImageView = ImageView(context).apply {
    setImageDrawable(tintedDrawable(icon, color))
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    layoutParams = LinearLayout.LayoutParams(dp(Ui.ICON_SIZE), dp(Ui.ICON_SIZE)).apply { marginEnd = dp(Ui.SPACE_LG) }
}

private fun Kit.rowText(title: CharSequence, summary: CharSequence?, titleColor: Int): Triple<LinearLayout, TextView, TextView> {
    val titleView = TextView(context).apply {
        text = title; textSize = Ui.TEXT_ITEM; setTextColor(titleColor)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    val summaryView = TextView(context).apply {
        text = summary; textSize = 14f; setTextColor(palette.muted)
        setLineSpacing(0f, 1.1f); setPaddingRelative(0, dp(4), 0, 0)
        // Follow the layout direction, so a Latin value (a preset name) still aligns with Arabic titles.
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        visibility = if (summary.isNullOrEmpty()) View.GONE else View.VISIBLE
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; addView(titleView); addView(summaryView) }
    return Triple(column, titleView, summaryView)
}

/** Tappable settings row: optional leading icon, title and summary, optional chevron. */
internal fun Kit.settingRow(
    title: CharSequence,
    summary: CharSequence? = null,
    @DrawableRes icon: Int? = null,
    tag: String? = null,
    chevron: Boolean = true,
    titleColor: Int = palette.text,
    onClick: (() -> Unit)? = null,
): SettingRow {
    val shell = rowShell(tag).apply { minimumHeight = dp(if (summary.isNullOrEmpty()) 56 else 72) }
    icon?.let { shell.addView(rowIcon(it)) }
    val (column, titleView, summaryView) = rowText(title, summary, titleColor)
    shell.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
    if (chevron && onClick != null) shell.addView(ImageView(context).apply {
        setImageDrawable(tintedDrawable(R.drawable.ic_settings_chevron, palette.muted)); alpha = 0.7f
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams = LinearLayout.LayoutParams(dp(Ui.ICON_SIZE), dp(Ui.ICON_SIZE)).apply { marginStart = dp(Ui.SPACE_LG) }
    })
    if (onClick != null) {
        shell.isClickable = true; shell.isFocusable = true
        selectableBackground(shell)
        shell.setOnClickListener { onClick() }
    }
    return SettingRow(shell, titleView, summaryView, null).also { it.refreshDescription() }
}

/** Row with a trailing switch; the whole row toggles and carries the switch semantics. */
internal fun Kit.switchRow(
    title: CharSequence,
    summary: CharSequence? = null,
    @DrawableRes icon: Int? = null,
    checked: Boolean,
    tag: String? = null,
    onToggle: (Boolean) -> Unit,
): SettingRow {
    val shell = rowShell(tag).apply { minimumHeight = dp(if (summary.isNullOrEmpty()) 56 else 72) }
    icon?.let { shell.addView(rowIcon(it)) }
    val (column, titleView, summaryView) = rowText(title, summary, palette.text)
    shell.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
    val switch = MaterialSwitch(context).apply {
        isChecked = checked
        thumbTintList = switchThumbTintList(); trackTintList = switchTrackTintList()
        isClickable = false; isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    shell.addView(switch, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(Ui.SPACE_MD) })
    shell.isClickable = true; shell.isFocusable = true
    selectableBackground(shell)
    shell.accessibilityDelegate = object : View.AccessibilityDelegate() {
        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.className = android.widget.Switch::class.java.name
            info.isCheckable = true; info.isChecked = switch.isChecked
        }
    }
    shell.setOnClickListener {
        switch.isChecked = !switch.isChecked
        onToggle(switch.isChecked)
    }
    return SettingRow(shell, titleView, summaryView, switch).also { it.refreshDescription() }
}

/**
 * A setting that chooses one of [labels] through a single-choice dialog; the summary shows the
 * current label. [selectedIndex] is readable by tests and updated before [onSelect] runs.
 */
internal class ChoiceRow(
    private val kit: Kit,
    val row: SettingRow,
    private val title: CharSequence,
    private val labels: List<CharSequence>,
    selected: Int,
    private val onSelect: (Int) -> Unit,
) {
    var selectedIndex: Int = selected
        private set
    /** The open choice dialog, if any. */
    var dialog: androidx.appcompat.app.AlertDialog? = null
        private set
    val view: LinearLayout get() = row.view
    init {
        render()
        row.view.setOnClickListener { dialog = kit.singleChoiceDialog(title, labels, selectedIndex) { choose(it) } }
    }
    /** User choice: updates the summary, then reports it. */
    fun choose(index: Int) {
        select(index)
        onSelect(index)
    }
    /** Programmatic state update that does not report a choice. */
    fun select(index: Int) {
        require(index in labels.indices)
        selectedIndex = index
        render()
    }
    private fun render() { row.setSummary(labels.getOrNull(selectedIndex)) }
}

internal fun Kit.choiceRow(
    title: CharSequence,
    labels: List<CharSequence>,
    selected: Int,
    @DrawableRes icon: Int? = null,
    tag: String? = null,
    onSelect: (Int) -> Unit,
): ChoiceRow = ChoiceRow(this, settingRow(title, labels.getOrNull(selected), icon, tag) {}, title, labels, selected, onSelect)

/** Selectable label + value pair for read-only details (About, run metadata). */
internal fun Kit.infoBlock(label: CharSequence, value: CharSequence, tag: String? = null): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    this.tag = tag
    setPaddingRelative(dp(Ui.SPACE_XXL), dp(Ui.SPACE_MD), dp(Ui.SPACE_XXL), dp(Ui.SPACE_MD))
    addView(text(label, Ui.TEXT_SECTION, palette.accent, medium = true).apply { textAlignment = View.TEXT_ALIGNMENT_VIEW_START })
    addView(text(value, Ui.TEXT_BODY + 0.5f).apply {
        setTextIsSelectable(true); setPaddingRelative(0, dp(Ui.SPACE_XS), 0, 0); textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    })
}
