package io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit

import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import io.github.supermonster003.autojs6.plugin.ai.agent.R

/** Outlined field with a floating label, added to [parent]. The tag goes on the EditText itself. */
internal fun Kit.formField(parent: LinearLayout, label: CharSequence, value: CharSequence?, tag: String,
                           inputType: Int = InputType.TYPE_CLASS_TEXT, maxLength: Int, multiline: Boolean = false,
                           helper: CharSequence? = null): TextInputEditText {
    val (layout, edit) = textField(value, label, inputType or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0,
        maxLength, singleLine = !multiline, tag = tag)
    edit.filters = arrayOf(InputFilter.LengthFilter(maxLength))
    if (multiline) { edit.minLines = 3; edit.maxLines = 12; edit.gravity = Gravity.TOP or Gravity.START }
    if (android.os.Build.VERSION.SDK_INT >= 26) edit.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    helper?.let { layout.helperText = it }
    parent.addView(layout, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(Ui.SPACE_MD) })
    return edit
}

/** Full-width checkbox row for multi-select lists; 48dp tall and wrapping long labels. */
internal fun Kit.checkRow(parent: LinearLayout, label: CharSequence, tag: String, checked: Boolean): MaterialCheckBox =
    MaterialCheckBox(context).apply {
        text = label; this.tag = tag; isChecked = checked
        textSize = Ui.TEXT_BODY; setTextColor(palette.text); buttonTintList = controlTintList()
        minHeight = dp(Ui.TOUCH_TARGET); textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }

/** Selectable body paragraph aligned to the start edge, used by cards and detail pages. */
internal fun Kit.paragraph(value: CharSequence): TextView = text(value, Ui.TEXT_BODY).apply {
    textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setTextIsSelectable(true); setPaddingRelative(0, dp(Ui.SPACE_XS), 0, 0)
}

/** Muted secondary line below a paragraph or title. */
internal fun Kit.note(value: CharSequence): TextView = text(value, Ui.TEXT_SECONDARY, palette.muted).apply {
    textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, dp(Ui.SPACE_XS), 0, 0)
}

/** Secondary explanatory text below a form control. */
internal fun Kit.caption(parent: LinearLayout, value: CharSequence): TextView = text(value, Ui.TEXT_SECONDARY, palette.muted).apply {
    textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, dp(Ui.SPACE_XS), 0, 0)
    parent.addView(this, LinearLayout.LayoutParams(-1, -2))
}

/** Section title inside a padded form. */
internal fun Kit.formSection(parent: LinearLayout, title: CharSequence): TextView = sectionHeader(title).apply {
    setPaddingRelative(0, dp(Ui.SECTION_GAP), 0, dp(Ui.SPACE_XS))
    parent.addView(this, LinearLayout.LayoutParams(-1, -2))
}

/** A bar pinned below the scrolling content, for the screen's primary action. */
internal fun Kit.actionBar(): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL or Gravity.END
    setBackgroundColor(palette.surface); elevation = dp(4).toFloat()
    setPaddingRelative(dp(Ui.SPACE_LG), dp(Ui.SPACE_SM), dp(Ui.SPACE_LG), dp(Ui.SPACE_SM))
}

/** Asks before discarding edits; [discard] runs only when the user confirms. */
internal fun Kit.unsavedChanges(discard: () -> Unit): AlertDialog =
    confirmDialog(string(R.string.ui_unsaved_title), string(R.string.ui_unsaved_note), string(R.string.ui_discard), destructive = true, onPositive = discard)
