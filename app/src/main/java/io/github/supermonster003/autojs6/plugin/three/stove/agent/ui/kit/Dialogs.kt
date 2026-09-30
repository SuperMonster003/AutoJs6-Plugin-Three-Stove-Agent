package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/** Base builder used by every dialog in the app. */
internal fun Kit.materialDialog(): MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(context)
    .setBackground(roundedFill(palette.surface, 24))

/** Plain message with a single acknowledgement. */
internal fun Kit.messageDialog(title: CharSequence?, message: CharSequence?): AlertDialog = materialDialog()
    .setTitle(title).setMessage(message).setPositiveButton(android.R.string.ok, null).show().also { tintDialogButtons(it) }

/** Confirmation; destructive actions color the positive button with the danger tone. */
internal fun Kit.confirmDialog(title: CharSequence?, message: CharSequence?, positive: CharSequence,
                               destructive: Boolean = false, onPositive: () -> Unit): AlertDialog = materialDialog()
    .setTitle(title).setMessage(message)
    .setNegativeButton(android.R.string.cancel, null)
    .setPositiveButton(positive) { _, _ -> onPositive() }
    .show().also { tintDialogButtons(it, destructive) }

/** Outlined text field with inline validation, used by dialogs and forms. */
internal fun Kit.textField(value: CharSequence?, hint: CharSequence? = null, inputType: Int = InputType.TYPE_CLASS_TEXT,
                           maxLength: Int? = null, singleLine: Boolean = true, tag: String? = null): Pair<TextInputLayout, TextInputEditText> {
    val edit = TextInputEditText(context).apply {
        setText(value)
        this.inputType = inputType
        isSingleLine = singleLine
        this.tag = tag
        textSize = Ui.TEXT_ITEM
        minimumHeight = dp(56)
        setPaddingRelative(dp(Ui.SPACE_LG), dp(Ui.SPACE_MD), dp(Ui.SPACE_LG), dp(Ui.SPACE_MD))
        maxLength?.let { filters = arrayOf(InputFilter.LengthFilter(it)) }
        setTextColor(palette.text)
        setHintTextColor(palette.muted)
        tintEditText(this)
        background = null
        backgroundTintList = null
    }
    val layout = TextInputLayout(context).apply {
        boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        boxBackgroundColor = Color.TRANSPARENT
        boxStrokeWidth = dp(1); boxStrokeWidthFocused = dp(2)
        val radius = dpF(Ui.RADIUS_CONTROL.toFloat())
        setBoxCornerRadii(radius, radius, radius, radius)
        setBoxStrokeColorStateList(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()), intArrayOf(palette.accent, palette.outline)))
        boxStrokeErrorColor = ColorStateList.valueOf(palette.danger)
        setErrorTextColor(ColorStateList.valueOf(palette.danger))
        defaultHintTextColor = ColorStateList.valueOf(palette.muted)
        hintTextColor = ColorStateList.valueOf(palette.accent)
        this.hint = hint
        addView(edit)
    }
    edit.backgroundTintList = null
    return layout to edit
}

/**
 * Single field dialog. [validate] returns an error message or null; [neutral] adds an optional
 * third action (for example "Automatic" to clear a limit).
 */
internal fun Kit.inputDialog(
    title: CharSequence,
    value: CharSequence?,
    message: CharSequence? = null,
    hint: CharSequence? = null,
    inputType: Int = InputType.TYPE_CLASS_TEXT,
    maxLength: Int? = null,
    positive: CharSequence = string(android.R.string.ok),
    neutral: Pair<CharSequence, () -> Unit>? = null,
    validate: (String) -> CharSequence? = { null },
    onSubmit: (String) -> Unit,
): AlertDialog {
    val (layout, edit) = textField(value, hint, inputType, maxLength)
    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPaddingRelative(dp(Ui.SPACE_XXL), dp(Ui.SPACE_SM), dp(Ui.SPACE_XXL), 0)
        addView(layout, LinearLayout.LayoutParams(-1, -2))
    }
    val builder = materialDialog().setTitle(title).setMessage(message).setView(container)
        .setNegativeButton(android.R.string.cancel, null).setPositiveButton(positive, null)
    neutral?.let { (label, action) -> builder.setNeutralButton(label) { _, _ -> action() } }
    val dialog = builder.create()
    dialog.setOnShowListener {
        tintDialogButtons(dialog)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val text = edit.text?.toString().orEmpty()
            val problem = validate(text)
            if (problem != null) layout.error = problem else { dialog.dismiss(); onSubmit(text) }
        }
        edit.requestFocus()
    }
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    dialog.show()
    return dialog
}

/** Single-choice adapter whose rows wrap long labels and keep a 52dp minimum height. */
private class PaletteChoiceItem(context: android.content.Context) : LinearLayout(context), android.widget.Checkable {
    val indicator = com.google.android.material.radiobutton.MaterialRadioButton(context).apply {
        isClickable = false; isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        minimumWidth = 0; minimumHeight = 0
    }
    val label = TextView(context).apply {
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        textSize = 16f
        isSingleLine = false; maxLines = Int.MAX_VALUE; ellipsize = null
        setLineSpacing(0f, 1.08f)
    }
    init {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        addView(indicator)
        addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }
    override fun isChecked() = indicator.isChecked
    override fun setChecked(value: Boolean) { indicator.isChecked = value }
    override fun toggle() { isChecked = !isChecked }
    override fun onInitializeAccessibilityNodeInfo(info: android.view.accessibility.AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = android.widget.RadioButton::class.java.name
        info.isCheckable = true; info.isChecked = isChecked
    }
}

internal class PaletteChoiceAdapter(private val kit: Kit, labels: List<CharSequence>) :
    ArrayAdapter<CharSequence>(kit.context, android.R.layout.simple_list_item_single_choice, labels) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = (convertView as? PaletteChoiceItem) ?: PaletteChoiceItem(kit.context)
        item.layoutParams = android.widget.AbsListView.LayoutParams(-1, -2)
        item.minimumHeight = kit.dp(if (getItem(position)?.contains('\n') == true) 72 else 56)
        item.setPaddingRelative(kit.dp(24), kit.dp(12), kit.dp(24), kit.dp(12))
        item.indicator.layoutParams = LinearLayout.LayoutParams(kit.dp(32), kit.dp(32)).apply { marginEnd = kit.dp(8) }
        item.indicator.buttonTintList = kit.controlTintList()
        item.label.text = getItem(position); item.label.setTextColor(kit.palette.text)
        item.contentDescription = item.label.text
        item.isChecked = (parent as? android.widget.ListView)?.isItemChecked(position) == true
        kit.selectableBackground(item)
        return item
    }
}

/** Single-choice dialog; picking an item applies it immediately. */
internal fun Kit.singleChoiceDialog(title: CharSequence, labels: List<CharSequence>, checked: Int, onSelect: (Int) -> Unit): AlertDialog =
    materialDialog().setTitle(title)
        .setSingleChoiceItems(PaletteChoiceAdapter(this, labels), checked) { dialog, index -> dialog.dismiss(); onSelect(index) }
        .setNegativeButton(android.R.string.cancel, null)
        .show().also { tintDialogButtons(it) }

/** Multi-choice dialog; [onConfirm] receives the checked positions. */
internal fun Kit.multiChoiceDialog(title: CharSequence, labels: List<CharSequence>, checked: BooleanArray, positive: CharSequence,
                                   onConfirm: (BooleanArray) -> Unit): AlertDialog {
    val state = checked.copyOf()
    return materialDialog().setTitle(title)
        .setMultiChoiceItems(labels.toTypedArray(), state) { _, index, value -> state[index] = value }
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(positive) { _, _ -> onConfirm(state) }
        .show().also { tintDialogButtons(it) }
}

internal class SheetHandle(val dialog: BottomSheetDialog, val content: LinearLayout)

/**
 * Fully expanded bottom sheet with a pinned title (and optional pinned [header]) and a scrolling body.
 * Used for the model switcher and multi-toggle lists; full-page editors carry their own Save button.
 */
internal fun Kit.bottomSheet(
    title: CharSequence,
    header: View? = null,
    minHeightFraction: Float = 0.5f,
    onDismiss: (() -> Unit)? = null,
): SheetHandle {
    val dialog = BottomSheetDialog(context)
    val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = roundedFill(palette.surface, Ui.RADIUS_SHEET)
        setPaddingRelative(0, dp(Ui.SPACE_SM), 0, 0)
    }
    column.addView(View(context).apply { background = roundedFill(palette.outline, Ui.RADIUS_PILL) },
        LinearLayout.LayoutParams(dp(32), dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(Ui.SPACE_XS); bottomMargin = dp(Ui.SPACE_SM) })
    column.addView(TextView(context).apply {
        text = title; textSize = Ui.TEXT_PAGE_TITLE; typeface = Ui.medium; setTextColor(palette.text)
        setPaddingRelative(dp(Ui.SPACE_XXL), dp(Ui.SPACE_SM), dp(Ui.SPACE_XXL), dp(Ui.SPACE_MD))
        if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
    })
    header?.let { column.addView(it, LinearLayout.LayoutParams(-1, -2)) }
    val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPaddingRelative(0, 0, 0, dp(Ui.SPACE_LG)) }
    column.addView(NestedScrollView(context).apply { addView(body, ViewGroup.LayoutParams(-1, -2)) }, LinearLayout.LayoutParams(-1, 0, 1f))
    dialog.setContentView(column)
    dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    dialog.behavior.skipCollapsed = true
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    (column.parent as? View)?.background = null
    onDismiss?.let { callback -> dialog.setOnDismissListener { callback() } }
    dialog.show()
    column.minimumHeight = (context.resources.displayMetrics.heightPixels * minHeightFraction).toInt()
    return SheetHandle(dialog, body)
}

/** Appearance choice draft. Only the positive action calls [onConfirm]; all dismissal paths discard it. */
internal fun Kit.confirmedChoiceDialog(title: CharSequence, labels: List<CharSequence>, checked: Int,
                                          onConfirm: (Int) -> Unit): AlertDialog {
    var draft = checked
    return materialDialog().setTitle(title)
        .setSingleChoiceItems(PaletteChoiceAdapter(this, labels), checked) { _, index -> draft = index }
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm(draft) }
        .show().also {
            tintDialogButtons(it)
            val width = minOf(dp(560), context.resources.displayMetrics.widthPixels - dp(48))
            it.window?.setBackgroundDrawable(roundedFill(palette.surface, 24))
            it.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.window?.decorView?.post {
                val maximum = (context.resources.displayMetrics.heightPixels * 0.85f).toInt()
                if ((it.window?.decorView?.height ?: 0) > maximum) it.window?.setLayout(width, maximum)
            }
            it.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.apply {
                textSize = 20f
                setTextColor(palette.text)
            }
            it.listView.post { it.listView.setSelection(0) }
        }
}
