package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.*

/**
 * Private preset management: cards with a row menu, and a full-page editor whose draft is kept until
 * the agent process acknowledges an atomic save. Presets carry tools, limits, confirmation policy,
 * context, script directories and memory scope; the model is chosen on the home screen (roadmap D46).
 */
class PresetsActivity : HostAppearanceActivity() {
    private lateinit var connection: PresetConnection
    private lateinit var scaffold: Scaffold
    private lateinit var page: LinearLayout
    private lateinit var bar: LinearLayout
    private lateinit var message: TextView
    private var configuration = JsonObject()
    private var draft: JsonObject? = null
    /** The editor's starting point, to detect unsaved changes. */
    private var initial: JsonObject? = null
    private var editing: String? = null
    /** A legacy preset model is kept for scripts only; the plugin UI uses the shared model choice. */
    private var targetId: String? = null
    private lateinit var name: TextInputEditText
    private lateinit var fixedContext: TextInputEditText
    private lateinit var inheritGroups: MaterialCheckBox
    private lateinit var inheritRoots: MaterialCheckBox
    internal lateinit var confirmation: ChoiceRow; private set
    internal lateinit var scope: ChoiceRow; private set
    private val groups = linkedMapOf<String, MaterialCheckBox>()
    private val roots = linkedMapOf<String, MaterialCheckBox>()
    private val budgets = linkedMapOf<String, TextInputEditText>()
    /** The last opened dialog or menu, exposed for instrumentation. */
    internal var prompt: AlertDialog? = null; private set
    internal var menu: PopupMenu? = null; private set
    private var editorVisible = false
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editing = savedInstanceState?.getString("editing")
        draft = savedInstanceState?.getString("draft")?.let { runCatching { AgentJson.objectOf(it, PresetCodec.MAX_ROW_BYTES) }.getOrNull() }
        initial = savedInstanceState?.getString("initial")?.let { runCatching { AgentJson.objectOf(it, PresetCodec.MAX_ROW_BYTES) }.getOrNull() }
        scaffold = buildScaffold(getString(R.string.presets_title), contentPadding = ContentPadding.SCREEN)
        page = scaffold.content
        bar = kit.actionBar().apply { visibility = View.GONE }
        scaffold.root.addView(bar, LinearLayout.LayoutParams(-1, -2))
        bar.addView(kit.filledButton(getString(R.string.presets_save), "preset-save") { save() })
        setContentView(scaffold.root)
        message = kit.text("", Ui.TEXT_BODY, palette.danger)
        connection = PresetConnection(this) { refresh() }
    }
    override fun onStart() { super.onStart(); busy = false; connection.start() }
    override fun onStop() { if (editorVisible) draft = readDraft(); connection.stop(); prompt?.dismiss(); prompt = null; menu?.dismiss(); menu = null; super.onStop() }
    override fun onDestroy() { connection.close(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("editing", editing)
        outState.putString("draft", (if (editorVisible) readDraft() else draft)?.toString())
        outState.putString("initial", initial?.toString())
        super.onSaveInstanceState(outState)
    }
    override fun navigateBack() {
        if (busy) return
        if (!editorVisible) { finish(); return }
        val leave = { draft = null; initial = null; editing = null; editorVisible = false; refresh() }
        if (readDraft() == initial) leave() else prompt = kit.unsavedChanges(leave)
    }

    private fun request(operation: String, extra: JsonObject = JsonObject(), complete: (JsonObject) -> Unit) {
        extra.addProperty("operation", operation)
        connection.query(extra) { result -> result.onSuccess(complete).onFailure { showError() } }
    }
    private fun refresh() {
        request("list") { value ->
            configuration = value
            val saved = draft
            if (saved == null) showList() else showEditor(saved, initial ?: saved)
        }
    }
    private fun reset(title: Int) {
        page.removeAllViews(); groups.clear(); roots.clear(); budgets.clear()
        supportActionBar?.title = getString(title)
        message = kit.text("", Ui.TEXT_BODY, palette.danger).apply {
            visibility = View.GONE; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
        page.addView(message, LinearLayout.LayoutParams(-1, -2))
    }
    private fun presetLabel(name: String) = if (name == "default") getString(R.string.workbench_default_preset) else name

    private fun showList() {
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
    private fun icon(icon: Int) = android.widget.ImageView(this).apply {
        setImageDrawable(kit.tintedDrawable(icon, palette.accent)); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private fun edit(key: String, copy: Boolean) = request("get", jsonObject("name" to key.json())) { row ->
        editing = key.takeUnless { copy }
        if (copy) row.addProperty("name", "")
        showEditor(row, row.deepCopy())
    }
    private fun actions(key: String, anchor: View) {
        menu = PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, R.string.presets_edit); menu.add(0, 2, 1, R.string.presets_copy)
            if (configuration.string("defaultName") != key) menu.add(0, 3, 2, R.string.presets_set_default)
            if (TaskEntries.canPin(this@PresetsActivity)) menu.add(0, 4, 3, R.string.shortcut_pin)
            if (key != "default") menu.add(0, 5, 4, R.string.presets_delete)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> edit(key, copy = false)
                    2 -> edit(key, copy = true)
                    3 -> request("default", jsonObject("name" to key.json())) { refresh() }
                    4 -> pin(key)
                    5 -> prompt = kit.confirmDialog(getString(R.string.presets_delete), getString(R.string.presets_delete_confirm, key),
                        getString(R.string.presets_delete), destructive = true) { request("delete", jsonObject("name" to key.json())) { refresh() } }
                }
                true
            }
            show()
        }
    }
    private fun pin(key: String) {
        prompt = kit.inputDialog(getString(R.string.shortcut_pin), null, getString(R.string.shortcut_review), getString(R.string.shortcut_goal),
            maxLength = 4096, positive = getString(R.string.shortcut_pin),
            validate = { goal -> if (runCatching { TaskEntry(goal.trim(), key) }.isSuccess) null else getString(R.string.entry_invalid) },
        ) { goal ->
            if (!runCatching { TaskEntries.pin(this, TaskEntry(goal.trim(), key)) }.getOrDefault(false)) kit.snackbar(scaffold.root, getString(R.string.entry_invalid))
        }
    }

    private fun showEditor(row: JsonObject, start: JsonObject) {
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
        for ((key, label) in BUDGET_LABELS) {
            val value = saved?.get(key)?.asString?.let { text -> if (key == DURATION) text.toLongOrNull()?.let { ((it + 59_999) / 60_000).toString() } ?: text else text }
            budgets[key] = kit.formField(page, getString(label), value.orEmpty(), if (key == DURATION) "preset-maxDuration" else "preset-$key",
                InputType.TYPE_CLASS_NUMBER, 12, helper = getString(R.string.history_auto))
        }

        kit.formSection(page, getString(R.string.presets_confirmation))
        confirmation = kit.choiceRow(getString(R.string.presets_confirmation), listOf(getString(R.string.presets_standard), getString(R.string.presets_cautious)),
            if (row.string("confirmPolicy") == "cautious") 1 else 0, R.drawable.ic_shield, "preset-confirm") {}
        page.addView(confirmation.view.apply { setPaddingRelative(0, paddingTop, 0, paddingBottom) }, LinearLayout.LayoutParams(-1, -2))
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
        scope = kit.choiceRow(getString(R.string.presets_memory), SCOPE_LABELS.map(::getString), PresetCodec.scopes.indexOf(row.string("memoryScope")).coerceAtLeast(0),
            R.drawable.ic_lightbulb, "preset-memory") {}
        page.addView(scope.view.apply { setPaddingRelative(0, paddingTop, 0, paddingBottom) }, LinearLayout.LayoutParams(-1, -2))
    }
    private fun readDraft(): JsonObject = jsonObject("name" to name.text.toString().json(), "context" to fixedContext.text.toString().json(),
        "confirmPolicy" to (if (confirmation.selectedIndex == 1) "cautious" else "default").json(),
        "memoryScope" to PresetCodec.scopes[scope.selectedIndex.coerceAtLeast(0)].json(),
        "budget" to JsonObject().apply { budgets.forEach { (key, input) ->
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) addProperty(key, if (key == DURATION) text.toLongOrNull()?.let { (it * 60_000).toString() } ?: text else text)
        } }).apply {
        targetId?.let { addProperty("targetId", it) }
        if (!inheritGroups.isChecked) add("toolGroups", JsonArray().apply { groups.filterValues { it.isChecked }.keys.forEach(::add) })
        if (!inheritRoots.isChecked) add("scriptRoots", JsonArray().apply { roots.filterValues { it.isChecked }.keys.forEach(::add) })
    }
    private fun save() {
        if (busy || !editorVisible) return
        val row = runCatching {
            val value = readDraft()
            val budget = value.getAsJsonObject("budget")
            budget.keySet().toList().forEach { key -> budget.addProperty(key, budget[key].asString.toLong()) }
            val parsed = PresetCodec.decodePreset(value)
            require(parsed.toolGroups?.all { it in configuration.getAsJsonArray("toolGroups").map { value -> value.asString } } != false)
            require(parsed.scriptRoots?.all { it in configuration.getAsJsonArray("scriptRoots").map { value -> value.asString } } != false)
            require(PresetCodec.encodePreset(parsed).toString().toByteArray(Charsets.UTF_8).size <= PresetCodec.MAX_ROW_BYTES)
            value
        }.getOrElse { showError(); return }
        busy = true
        // Keep the acknowledged draft stable, including Back, while its write is in flight.
        val enabled = mutableListOf<Pair<View, Boolean>>()
        fun freeze(view: View) {
            enabled += view to view.isEnabled; view.isEnabled = false
            if (view is ViewGroup) for (index in 0 until view.childCount) freeze(view.getChildAt(index))
        }
        freeze(page); freeze(bar)
        connection.query(jsonObject("operation" to "save".json(), "preset" to row, "create" to (editing == null).json())) { result ->
            busy = false; enabled.forEach { (view, wasEnabled) -> view.isEnabled = wasEnabled }
            result.onSuccess { draft = null; initial = null; editing = null; editorVisible = false; refresh() }.onFailure { showError() }
        }
    }
    private fun showError() {
        message.setText(R.string.presets_error); message.visibility = View.VISIBLE
        scaffold.scroll?.smoothScrollTo(0, 0)
    }
    private companion object {
        const val DURATION = "maxDurationMs"
        val BUDGET_LABELS = linkedMapOf("maxSteps" to R.string.presets_steps, "maxModelCalls" to R.string.presets_calls,
            DURATION to R.string.settings_duration_minutes, "maxTotalTokens" to R.string.presets_tokens)
        val SCOPE_LABELS = listOf(R.string.presets_memory_both, R.string.presets_memory_global, R.string.presets_memory_preset, R.string.presets_memory_none)
    }
}
