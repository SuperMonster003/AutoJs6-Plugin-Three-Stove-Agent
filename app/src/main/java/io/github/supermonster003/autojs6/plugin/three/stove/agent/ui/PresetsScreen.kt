package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*

/** Presets screen construction (roadmap P11): the list, the editor form and the import review; PresetsActivity keeps state and events. */

internal fun PresetsActivity.showList() {
    editorVisible = false; bar.visibility = View.GONE; reset(R.string.presets_title)
    kit.caption(page, getString(R.string.ui_preset_optional))
    page.addView(LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        // Equal shares, as on the memory screen: at large text sizes the labels wrap instead of overflowing.
        addView(kit.tonalButton(getString(R.string.presets_new), "preset-new") {
            editing = null; val row = PresetCodec.encodePreset(Preset("")); showEditor(row, row)
        }.apply { isEnabled = configuration.getAsJsonArray("presets").size() < PresetCodec.MAX_COUNT },
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
        addView(kit.textButton(getString(R.string.presets_import), "preset-import") {
            runCatching { importDocument.launch(arrayOf("application/json")) }.onFailure { showError() }
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
        addView(kit.textButton(getString(R.string.presets_export), "preset-export") {
            prompt = kit.confirmDialog(getString(R.string.presets_export), getString(R.string.presets_export_notice), getString(R.string.presets_export)) {
                runCatching { exportDocument.launch("three-stove-agent-presets.json") }.onFailure { showError() }
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD); bottomMargin = kit.dp(Ui.SPACE_MD) })
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
    bar.removeAllViews()
    bar.addView(kit.filledButton(getString(R.string.presets_save), "preset-save") { save() })
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

/**
 * One imported row at a time, shown exactly as it will be stored: the model target never travels in a file,
 * and tool groups or script directories this device does not offer are named and dropped (roadmap I.3).
 */
internal fun PresetsActivity.showImport() {
    editorVisible = false; bar.visibility = View.VISIBLE; reset(R.string.presets_title)
    bar.removeAllViews()
    val row = imports[importIndex]
    val existing = configuration.getAsJsonArray("presets").map { it.asString }
    val (kept, dropped) = importable(row)
    val replacing = row.name in existing
    val full = !replacing && existing.size >= PresetCodec.MAX_COUNT
    val allowedGroups = configuration.getAsJsonArray("toolGroups").map { it.asString }.toSet()
    val allowedRoots = configuration.getAsJsonArray("scriptRoots").map { it.asString }.toSet()
    page.addView(kit.text(getString(R.string.memory_import_progress, importIndex + 1, imports.size), Ui.TEXT_SECTION, palette.accent, medium = true))
    page.addView(kit.text(presetLabel(row.name), Ui.TEXT_TITLE, medium = true).apply {
        tag = "preset-import-name"; setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
    fun banner(text: String, tone: Tone) = page.addView(Banner(kit).apply { show(text, tone, R.drawable.ic_warning) }.view,
        LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
    if (replacing) banner(getString(R.string.presets_replace), Tone.WARNING)
    if (dropped.isNotEmpty()) banner(getString(R.string.presets_import_dropped, dropped.joinToString(", ")), Tone.WARNING)
    if (full) banner(getString(R.string.presets_import_full, PresetCodec.MAX_COUNT), Tone.DANGER)
    fun section(title: Int, value: String, ltr: Boolean = false) {
        kit.formSection(page, getString(title))
        page.addView(kit.text(value, Ui.TEXT_BODY).apply {
            setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            if (ltr) textDirection = View.TEXT_DIRECTION_LTR
        })
    }
    fun mark(label: String, available: Boolean) = if (available) label else getString(R.string.presets_unavailable, label)
    section(R.string.presets_tools, row.toolGroups?.let { ids -> ids.sorted().joinToString(", ") { mark(groupLabel(it), it in allowedGroups) }.ifEmpty { "-" } }
        ?: getString(R.string.presets_inherit))
    section(R.string.ui_budget, if (row.budget.isEmpty()) getString(R.string.history_auto) else PresetsActivity.BUDGET_LABELS.mapNotNull { (key, label) ->
        row.budget[key]?.let { value -> getString(label) + ": " + (if (key == PresetsActivity.DURATION) (value + 59_999) / 60_000 else value) }
    }.joinToString("\n"))
    section(R.string.presets_confirmation, getString(if (row.confirmPolicy == "cautious") R.string.presets_cautious else R.string.presets_standard))
    if (row.context.isNotBlank()) section(R.string.presets_context_section, row.context)
    section(R.string.script_roots_title, row.scriptRoots?.let { paths -> paths.sorted().joinToString("\n") { mark(it, it in allowedRoots) }.ifEmpty { "-" } }
        ?: getString(R.string.presets_inherit), ltr = true)
    section(R.string.presets_memory, getString(PresetsActivity.SCOPE_LABELS[PresetCodec.scopes.indexOf(row.memoryScope).coerceAtLeast(0)]))
    page.addView(kit.textButton(getString(R.string.memory_cancel_import), "preset-cancel-import") { cancelImport() },
        LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_LG) })
    // Equal shares: at large text sizes both labels wrap instead of overflowing the bar.
    bar.addView(kit.tonalButton(getString(R.string.presets_skip), "preset-skip") { nextImport() },
        LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
    bar.addView(kit.filledButton(getString(R.string.presets_accept), "preset-accept") { importPreset(kept, create = !replacing) }.apply { isEnabled = !full },
        LinearLayout.LayoutParams(0, -2, 1f))
}
