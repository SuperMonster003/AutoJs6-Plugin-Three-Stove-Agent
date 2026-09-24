package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*

/** Private preset management. Editing is a draft until the agent process acknowledges an atomic save. */
class PresetsActivity : HostAppearanceActivity() {
    private lateinit var connection: PresetConnection
    private lateinit var body: LinearLayout
    private lateinit var error: TextView
    private var configuration = JsonObject()
    private var targets = emptyList<JsonObject>()
    private var catalogAvailable = false
    private var draft: JsonObject? = null
    private var editing: String? = null
    private var targetId: String? = null
    private var targetIds = emptyList<String?>()
    private lateinit var target: Spinner
    private lateinit var targetStatus: TextView
    private lateinit var name: EditText
    private lateinit var fixedContext: EditText
    private lateinit var inheritGroups: CheckBox
    private lateinit var inheritRoots: CheckBox
    private lateinit var confirmation: Spinner
    private lateinit var scope: Spinner
    private val groups = linkedMapOf<String, CheckBox>()
    private val roots = linkedMapOf<String, CheckBox>()
    private val budgets = linkedMapOf<String, EditText>()
    private var editorVisible = false
    private var busy = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editing = savedInstanceState?.getString("editing")
        draft = savedInstanceState?.getString("draft")?.let { runCatching { AgentJson.objectOf(it, PresetCodec.MAX_ROW_BYTES) }.getOrNull() }
        body = HistoryViews.column(this).apply { layoutDirection = resources.configuration.layoutDirection }
        setContentView(ScrollView(this).apply { fitsSystemWindows = true; addView(body) })
        connection = PresetConnection(this) { refresh() }
        error = HistoryViews.label(body, "")
    }
    override fun onStart() { super.onStart(); busy = false; connection.start() }
    override fun onStop() { if (editorVisible) draft = readDraft(); connection.stop(); super.onStop() }
    override fun onDestroy() { connection.close(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("editing", editing)
        outState.putString("draft", (if (editorVisible) readDraft() else draft)?.toString())
        super.onSaveInstanceState(outState)
    }
    private fun request(operation: String, extra: JsonObject = JsonObject(), complete: (JsonObject) -> Unit) {
        extra.addProperty("operation", operation)
        connection.query(extra) { result -> result.onSuccess(complete).onFailure { showError() } }
    }
    private fun refresh() {
        request("list") { value ->
            configuration = value
            val saved = draft
            if (saved == null) showList() else showEditor(saved)
            loadTargets()
        }
    }
    private fun loadTargets() {
        connection.query(jsonObject("operation" to "targets".json())) { result ->
            catalogAvailable = result.isSuccess
            targets = result.getOrNull()?.getAsJsonArray("targets")?.map { it.asJsonObject }.orEmpty()
            if (editorVisible) renderTargets()
        }
    }
    private fun header(title: Int) {
        body.removeAllViews()
        HistoryViews.label(body, getString(title), true)
        HistoryViews.button(body, R.string.workbench_back, "back") {
            if (editorVisible) { draft = null; editing = null; refresh() } else finish()
        }
        error = HistoryViews.label(body, "").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE }
    }
    private fun showList() {
        editorVisible = false; header(R.string.presets_title)
        HistoryViews.button(body, R.string.presets_new, "preset-new") {
            editing = null; showEditor(PresetCodec.encodePreset(Preset("")))
        }.isEnabled = configuration.getAsJsonArray("presets").size() < PresetCodec.MAX_COUNT
        configuration.getAsJsonArray("presets").forEach { item ->
            val key = item.asString
            body.addView(Button(this).apply {
                tag = "preset-$key"; isAllCaps = false
                text = if (configuration.string("defaultName") == key) getString(R.string.presets_default_item, key) else key
                setOnClickListener { actions(key) }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        tint(body)
    }
    private fun actions(key: String) {
        val labels = listOf(R.string.presets_edit, R.string.presets_copy, R.string.presets_set_default) +
            if (key == "default") emptyList() else listOf(R.string.presets_delete)
        AlertDialog.Builder(this).setTitle(key).setItems(labels.map(::getString).toTypedArray()) { _, index ->
            when (index) {
                0, 1 -> request("get", jsonObject("name" to key.json())) { row ->
                    editing = key.takeIf { index == 0 }
                    if (index == 1) row.addProperty("name", "")
                    showEditor(row)
                }
                2 -> request("default", jsonObject("name" to key.json())) { refresh() }
                3 -> AlertDialog.Builder(this).setMessage(getString(R.string.presets_delete_confirm, key))
                    .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.presets_delete) { _, _ ->
                        request("delete", jsonObject("name" to key.json())) { refresh() }
                    }.show()
            }
        }.show()
    }
    private fun field(label: Int, tag: String, value: String, multiline: Boolean = false): EditText {
        val caption = HistoryViews.label(body, getString(label))
        return EditText(this).apply {
            id = View.generateViewId(); caption.labelFor = id; this.tag = tag
            inputType = InputType.TYPE_CLASS_TEXT or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
            if (multiline) { minLines = 3; gravity = android.view.Gravity.TOP or android.view.Gravity.START }
            if (android.os.Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            setText(value); body.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
    }
    private fun spinner(label: Int, tag: String, labels: List<String>, position: Int): Spinner {
        HistoryViews.label(body, getString(label))
        return Spinner(this).apply {
            this.tag = tag; contentDescription = getString(label); minimumHeight = (48 * resources.displayMetrics.density).toInt()
            adapter = ArrayAdapter(this@PresetsActivity, android.R.layout.simple_spinner_dropdown_item, labels)
            setSelection(position.coerceAtLeast(0)); body.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
    }
    private fun check(label: String, tag: String, checked: Boolean) = CheckBox(this).apply {
        text = label; this.tag = tag; isChecked = checked; body.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    private fun showEditor(row: JsonObject) {
        draft = row; editorVisible = true; header(if (editing == null) R.string.presets_new else R.string.presets_edit)
        name = field(R.string.presets_name, "preset-name", row.string("name").orEmpty()).apply {
            isEnabled = editing == null; filters = arrayOf(InputFilter.LengthFilter(128))
        }
        HistoryViews.label(body, getString(R.string.presets_name_note))
        targetId = row.string("targetId")
        target = spinner(R.string.presets_model, "preset-target", emptyList(), 0)
        targetStatus = HistoryViews.label(body, "")
        HistoryViews.button(body, R.string.presets_refresh_models, "preset-refresh-models") { loadTargets() }
        renderTargets()
        target.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { targetId = targetIds.getOrNull(position) }
        }
        HistoryViews.label(body, getString(R.string.presets_tools), true)
        val allowed = configuration.getAsJsonArray("toolGroups").map { it.asString }.toSet()
        val selectedGroups = row.getAsJsonArray("toolGroups")?.map { it.asString }?.toSet() ?: allowed
        groups.clear()
        inheritGroups = check(getString(R.string.presets_inherit), "preset-inherit-groups", !row.has("toolGroups"))
        ToolGroup.entries.forEach { group ->
            val label = getString(GROUP_LABELS.getValue(group))
            groups[group.id] = check(if (group.id in allowed) label else getString(R.string.presets_unavailable, label), "preset-group-${group.id}", group.id in selectedGroups)
        }
        fun enableGroups() { groups.forEach { (id, box) -> box.isEnabled = !inheritGroups.isChecked && (id in allowed || box.isChecked) } }
        inheritGroups.setOnCheckedChangeListener { _, _ -> enableGroups() }; enableGroups()
        HistoryViews.label(body, getString(R.string.presets_budget_note))
        budgets.clear()
        for ((key, label) in BUDGET_LABELS) {
            budgets[key] = field(label, "preset-$key", row.getAsJsonObject("budget")?.get(key)?.asString.orEmpty()).apply {
                inputType = InputType.TYPE_CLASS_NUMBER; filters = arrayOf(InputFilter.LengthFilter(12))
                hint = getString(R.string.history_auto)
            }
        }
        confirmation = spinner(R.string.presets_confirmation, "preset-confirm", listOf(getString(R.string.presets_standard), getString(R.string.presets_cautious)),
            if (row.string("confirmPolicy") == "cautious") 1 else 0)
        fixedContext = field(R.string.presets_context, "preset-context", row.string("context").orEmpty(), true).apply { filters = arrayOf(InputFilter.LengthFilter(8192)) }
        HistoryViews.label(body, getString(R.string.script_roots_title), true)
        inheritRoots = check(getString(R.string.presets_inherit), "preset-inherit-roots", !row.has("scriptRoots"))
        roots.clear()
        val allowedRoots = configuration.getAsJsonArray("scriptRoots").map { it.asString }.toSet()
        val selectedRoots = row.getAsJsonArray("scriptRoots")?.map { it.asString }?.toSet() ?: allowedRoots
        (allowedRoots + selectedRoots).sorted().forEach { path ->
            roots[path] = check(if (path in allowedRoots) path else getString(R.string.presets_unavailable, path), "preset-root-$path", path in selectedRoots)
        }
        fun enableRoots() { roots.forEach { (path, box) -> box.isEnabled = !inheritRoots.isChecked && (path in allowedRoots || box.isChecked) } }
        inheritRoots.setOnCheckedChangeListener { _, _ -> enableRoots() }; enableRoots()
        scope = spinner(R.string.presets_memory, "preset-memory", SCOPE_LABELS.map(::getString), PresetCodec.scopes.indexOf(row.string("memoryScope")))
        HistoryViews.button(body, R.string.presets_save, "preset-save") { save() }
        tint(body)
    }
    private fun renderTargets() {
        val selected = targetId
        targetIds = (listOf<String?>(null) + targets.map { it.string("targetId")!! } + listOfNotNull(selected)).distinct()
        val labels = targetIds.map { id ->
            if (id == null) getString(R.string.workbench_auto_model) else {
                val model = targets.find { it.string("targetId") == id }
                if (model == null) getString(R.string.presets_unavailable, id) else {
                    val locality = when (model.string("locality")) {
                        "ON_DEVICE" -> R.string.presets_local
                        "REMOTE" -> R.string.presets_remote
                        else -> R.string.presets_hybrid
                    }
                    getString(R.string.presets_target_label, model.string("displayName"), getString(locality),
                        getString(if (model.flag("structuredJson") == true) R.string.presets_structured else R.string.presets_degraded))
                }
            }
        }
        target.adapter = ArrayAdapter(this, R.layout.item_preset_model, labels)
        target.dropDownWidth = ViewGroup.LayoutParams.MATCH_PARENT
        target.setSelection(targetIds.indexOf(selected).coerceAtLeast(0))
        targetStatus.text = if (catalogAvailable) "" else getString(R.string.presets_models_unavailable)
    }
    private fun readDraft(): JsonObject = jsonObject("name" to name.text.toString().json(), "context" to fixedContext.text.toString().json(),
        "confirmPolicy" to (if (confirmation.selectedItemPosition == 1) "cautious" else "default").json(),
        "memoryScope" to PresetCodec.scopes[scope.selectedItemPosition.coerceAtLeast(0)].json(),
        "budget" to JsonObject().apply { budgets.forEach { (key, input) -> if (input.text.isNotEmpty()) addProperty(key, input.text.toString()) } }).apply {
        targetId?.let { addProperty("targetId", it) }
        if (!inheritGroups.isChecked) add("toolGroups", JsonArray().apply { groups.filterValues { it.isChecked }.keys.forEach(::add) })
        if (!inheritRoots.isChecked) add("scriptRoots", JsonArray().apply { roots.filterValues { it.isChecked }.keys.forEach(::add) })
    }
    private fun save() {
        if (busy) return
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
        freeze(body)
        connection.query(jsonObject("operation" to "save".json(), "preset" to row, "create" to (editing == null).json())) { result ->
            busy = false; enabled.forEach { (view, wasEnabled) -> view.isEnabled = wasEnabled }
            result.onSuccess { draft = null; editing = null; editorVisible = false; refresh() }.onFailure { showError() }
        }
    }
    private fun showError() {
        error.setText(R.string.presets_error)
        (body.parent as? ScrollView)?.smoothScrollTo(0, 0)
    }
    private companion object {
        val BUDGET_LABELS = linkedMapOf("maxSteps" to R.string.presets_steps, "maxModelCalls" to R.string.presets_calls,
            "maxDurationMs" to R.string.presets_duration, "maxTotalTokens" to R.string.presets_tokens)
        val SCOPE_LABELS = listOf(R.string.presets_memory_both, R.string.presets_memory_global, R.string.presets_memory_preset, R.string.presets_memory_none)
        val GROUP_LABELS = mapOf(ToolGroup.OBSERVE to R.string.presets_group_observe, ToolGroup.OCR to R.string.presets_group_ocr,
            ToolGroup.ACT to R.string.presets_group_act, ToolGroup.GESTURE to R.string.presets_group_gesture, ToolGroup.SCRIPT to R.string.presets_group_script,
            ToolGroup.FILES to R.string.presets_group_files, ToolGroup.SHELL to R.string.presets_group_shell, ToolGroup.MEMORY to R.string.presets_group_memory, ToolGroup.USER to R.string.presets_group_user)
    }
}
