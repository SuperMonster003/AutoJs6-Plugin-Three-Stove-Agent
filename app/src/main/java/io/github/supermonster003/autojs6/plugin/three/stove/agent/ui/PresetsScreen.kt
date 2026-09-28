package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*

/** Presets screen construction (roadmap P11): the list and the editor form; PresetsActivity keeps state and events. */

internal fun PresetsActivity.showList() {
    editorVisible = false; bar.visibility = View.GONE; reset(R.string.presets_title)
    kit.caption(page, getString(R.string.ui_preset_optional))
    page.addView(kit.tonalButton(getString(R.string.presets_new), "preset-new") {
        editing = null; val row = PresetCodec.encodePreset(Preset("")); showEditor(row, row)
    }.apply { isEnabled = configuration.getAsJsonArray("presets").size() < PresetCodec.MAX_COUNT },
        LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_MD); bottomMargin = kit.dp(Ui.SPACE_MD) })
    val defaultName = configuration.string("defaultName")
    configuration.getAsJsonArray("presets").forEach { item ->
        val key = item.asString
        page.addView(kit.card(interactive = true).apply {
            tag = "preset-$key"
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPaddingRelative(kit.dp(Ui.SPACE_LG), kit.dp(Ui.SPACE_SM), kit.dp(Ui.SPACE_XS), kit.dp(Ui.SPACE_SM))
            addView(icon(R.drawable.ic_layers), LinearLayout.LayoutParams(kit.dp(Ui.ICON_SIZE), kit.dp(Ui.ICON_SIZE)).apply { marginEnd = kit.dp(Ui.SPACE_LG) })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(kit.text(presetLabel(key), Ui.TEXT_ITEM, medium = true).apply { textAlignment = View.TEXT_ALIGNMENT_VIEW_START })
                if (key == defaultName) addView(kit.badge(getString(R.string.presets_default_badge), Tone.ACCENT),
                    LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            val more = kit.iconButton(R.drawable.ic_more, getString(R.string.presets_actions, presetLabel(key)), "preset-menu-$key", palette.muted) {}
            more.setOnClickListener { actions(key, more) }
            addView(more)
            contentDescription = presetLabel(key) + if (key == defaultName) ", " + getString(R.string.presets_default_badge) else ""
            setOnClickListener { edit(key, copy = false) }
        }, kit.cardParams(bottomDp = Ui.SPACE_SM))
    }
}

internal fun PresetsActivity.icon(icon: Int) = android.widget.ImageView(this).apply {
    setImageDrawable(kit.tintedDrawable(icon, palette.accent)); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
}

internal fun PresetsActivity.showEditor(row: JsonObject, start: JsonObject) {
    draft = row; initial = start; editorVisible = true; bar.visibility = View.VISIBLE
    reset(if (editing == null) R.string.presets_new else R.string.presets_edit)
    name = kit.formField(page, getString(R.string.presets_name), row.string("name").orEmpty(), "preset-name", maxLength = 128,
        helper = getString(R.string.presets_name_note)).apply { isEnabled = editing == null }
    targetId = row.string("targetId")

    kit.formSection(page, getString(R.string.presets_tools))
    val allowed = configuration.getAsJsonArray("toolGroups").map { it.asString }.toSet()
    val selectedGroups = row.getAsJsonArray("toolGroups")?.map { it.asString }?.toSet() ?: allowed
    inheritGroups = kit.checkRow(page, getString(R.string.presets_inherit), "preset-inherit-groups", !row.has("toolGroups"))
    ToolGroup.entries.forEach { group ->
        val label = ToolPresentation.groupLabel(this, group)
        groups[group.id] = kit.checkRow(page, if (group.id in allowed) label else getString(R.string.presets_unavailable, label),
            "preset-group-${group.id}", group.id in selectedGroups).apply { (layoutParams as ViewGroup.MarginLayoutParams).marginStart = kit.dp(Ui.SPACE_LG) }
    }
    fun enableGroups() { groups.forEach { (id, box) -> box.isEnabled = !inheritGroups.isChecked && (id in allowed || box.isChecked) } }
    inheritGroups.setOnCheckedChangeListener { _, _ -> enableGroups() }; enableGroups()

    kit.formSection(page, getString(R.string.ui_budget))
    kit.caption(page, getString(R.string.presets_budget_note))
    val saved = row.getAsJsonObject("budget")
    for ((key, label) in PresetsActivity.BUDGET_LABELS) {
        val value = saved?.get(key)?.asString?.let { text -> if (key == PresetsActivity.DURATION) text.toLongOrNull()?.let { ((it + 59_999) / 60_000).toString() } ?: text else text }
        budgets[key] = kit.formField(page, getString(label), value.orEmpty(), if (key == PresetsActivity.DURATION) "preset-maxDuration" else "preset-$key",
            InputType.TYPE_CLASS_NUMBER, 12, helper = getString(R.string.history_auto))
    }

    kit.formSection(page, getString(R.string.presets_confirmation))
    confirmation = kit.choiceRow(getString(R.string.presets_confirmation), listOf(getString(R.string.presets_standard), getString(R.string.presets_cautious)),
        if (row.string("confirmPolicy") == "cautious") 1 else 0, R.drawable.ic_shield, "preset-confirm") {}
    page.addView(confirmation.view.apply { setPaddingRelative(0, paddingTop, 0, paddingBottom) }, LinearLayout.LayoutParams(-1, -2))
    kit.caption(page, getString(R.string.presets_confirmation_note))

    kit.formSection(page, getString(R.string.presets_context_section))
    fixedContext = kit.formField(page, getString(R.string.presets_context), row.string("context").orEmpty(), "preset-context", maxLength = 8192, multiline = true)

    kit.formSection(page, getString(R.string.script_roots_title))
    inheritRoots = kit.checkRow(page, getString(R.string.presets_inherit), "preset-inherit-roots", !row.has("scriptRoots"))
    val allowedRoots = configuration.getAsJsonArray("scriptRoots").map { it.asString }.toSet()
    val selectedRoots = row.getAsJsonArray("scriptRoots")?.map { it.asString }?.toSet() ?: allowedRoots
    (allowedRoots + selectedRoots).sorted().forEach { path ->
        roots[path] = kit.checkRow(page, if (path in allowedRoots) path else getString(R.string.presets_unavailable, path), "preset-root-$path", path in selectedRoots)
            .apply { (layoutParams as ViewGroup.MarginLayoutParams).marginStart = kit.dp(Ui.SPACE_LG); textDirection = View.TEXT_DIRECTION_LTR }
    }
    fun enableRoots() { roots.forEach { (path, box) -> box.isEnabled = !inheritRoots.isChecked && (path in allowedRoots || box.isChecked) } }
    inheritRoots.setOnCheckedChangeListener { _, _ -> enableRoots() }; enableRoots()

    kit.formSection(page, getString(R.string.presets_memory))
    scope = kit.choiceRow(getString(R.string.presets_memory), PresetsActivity.SCOPE_LABELS.map(::getString), PresetCodec.scopes.indexOf(row.string("memoryScope")).coerceAtLeast(0),
        R.drawable.ic_lightbulb, "preset-memory") {}
    page.addView(scope.view.apply { setPaddingRelative(0, paddingTop, 0, paddingBottom) }, LinearLayout.LayoutParams(-1, -2))
}
