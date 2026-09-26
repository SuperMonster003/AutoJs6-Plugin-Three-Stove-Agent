package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import io.github.supermonster003.autojs6.plugin.ai.agent.update.*

class SettingsActivity : HostAppearanceActivity() {
    private lateinit var connection: SettingsConnection
    private lateinit var page: LinearLayout
    internal lateinit var appearanceSettings: AppearanceSettings; private set
    private var baseline: JsonObject? = null
    private lateinit var column: LinearLayout
    private lateinit var message: TextView
    internal lateinit var updates: AppUpdateCoordinator; private set
    internal var prompt: AlertDialog? = null; private set
    private var draft: JsonObject? = null
    private var rendered = false
    private var saving = false
    private val groups = linkedMapOf<String, CheckBox>()
    private val budgets = linkedMapOf<String, EditText>()
    private lateinit var cautious: CheckBox
    private lateinit var voice: CheckBox
    private lateinit var floating: CheckBox
    private var awaitingOverlayPermission = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setTitle(R.string.settings_title)
        draft = savedInstanceState?.getString("draft")?.let { runCatching { AgentJson.objectOf(it) }.getOrNull() }
        awaitingOverlayPermission = savedInstanceState?.getBoolean("overlayPermission") == true
        page = HistoryViews.column(this)
        appearanceSettings = AppearanceSettings(this)
        updates = AppUpdateCoordinator(this, aiAgentPluginRuntimeInfo().versionName)
        appearanceSettings.build(page)
        column = page
        message = HistoryViews.label(page, getString(R.string.interaction_loading))
        localSections()
        setContentView(AgentUi.screen(this, getString(R.string.settings_title), page, onBack = ::requestExit))
        connection = SettingsConnection(this, ::refresh)
        tint(page)
    }
    override fun onStart() { super.onStart(); saving = false; connection.start() }
    override fun onStop() { if (rendered) draft = readDraft(); connection.stop(); updates.cancel(); appearanceSettings.close(); prompt?.dismiss(); prompt = null; super.onStop() }
    override fun onDestroy() { updates.close(); super.onDestroy() }
    override fun onResume() {
        super.onResume()
        if (awaitingOverlayPermission) {
            awaitingOverlayPermission = false
            val granted = Settings.canDrawOverlays(this)
            draft?.addProperty("floating", granted)
            if (rendered) floating.isChecked = granted
            if (!granted) Toast.makeText(this, R.string.floating_permission_required, Toast.LENGTH_LONG).show()
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("overlayPermission", awaitingOverlayPermission)
        outState.putString("draft", (if (rendered) readDraft() else draft)?.toString()); super.onSaveInstanceState(outState)
    }
    private fun request(operation: String, fields: JsonObject = JsonObject(), complete: (JsonObject) -> Unit) {
        fields.addProperty("operation", operation)
        connection.query(fields) { result -> result.onSuccess(complete).onFailure { error() } }
    }
    private fun refresh() { request("get") { render(it) } }
    private fun error() { saving = false; message.setText(R.string.settings_error); message.visibility = View.VISIBLE }
    private fun button(resource: Int, tag: String, action: () -> Unit) = HistoryViews.button(column, resource, tag, action).apply { isAllCaps = false }
    private fun open(type: Class<*>) { startActivity(Intent(this, type)) }
    private fun checkbox(resource: Int, tag: String, checked: Boolean) = CheckBox(this).apply {
        setText(resource); this.tag = tag; isChecked = checked; minHeight = (48 * resources.displayMetrics.density).toInt(); column.addView(this)
    }
    private fun render(value: JsonObject) {
        baseline = value.getAsJsonObject("settings").deepCopy()
        val form = draft ?: baseline!!
        page.removeAllViews(); groups.clear(); budgets.clear()
        appearanceSettings.build(page)
        message = HistoryViews.label(page, "").apply { visibility = View.GONE; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE }
        AgentUi.section(page, R.string.ui_task_section)
        column = AgentUi.disclosure(page, R.string.ui_task_options, R.string.settings_policy_note)
        val enabled = form.getAsJsonArray("toolGroups").map { it.asString }.toSet()
        val options = column
        column = AgentUi.disclosure(options, R.string.presets_tools)
        ToolGroup.entries.forEach { group -> groups[group.id] = checkbox(groupLabels.getValue(group), "group-${group.id}", group.id in enabled) }
        HistoryViews.label(column, getString(R.string.settings_ocr_note))
        column = AgentUi.disclosure(options, R.string.ui_budget, R.string.settings_budget_note)
        for ((key, label) in budgetLabels) {
            val caption = HistoryViews.label(column, getString(label))
            budgets[key] = EditText(this).apply {
                id = View.generateViewId(); tag = key; caption.labelFor = id; inputType = InputType.TYPE_CLASS_NUMBER
                setText(form.getAsJsonObject("budget")[key]?.asString.orEmpty()); setHint(R.string.history_auto)
                filters = arrayOf(android.text.InputFilter.LengthFilter(16)); column.addView(this, LinearLayout.LayoutParams(-1, -2))
            }
        }
        column = options
        cautious = checkbox(R.string.presets_cautious, "cautious", form.flag("cautious") == true)
        voice = checkbox(R.string.settings_voice, "voice", form.flag("voice") == true)
        floating = checkbox(R.string.settings_floating, "floating", form.flag("floating") == true && Settings.canDrawOverlays(this))
        HistoryViews.label(column, getString(R.string.floating_setting_note))
        floating.setOnCheckedChangeListener { _, checked ->
            if (checked && !Settings.canDrawOverlays(this)) {
                floating.isChecked = false; draft = readDraft(); awaitingOverlayPermission = true
                runCatching { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
                    .onFailure { awaitingOverlayPermission = false; error() }
            }
        }
        button(R.string.settings_save, "save") {
            if (saving) return@button
            var validFields = true
            budgets.forEach { (key, field) ->
                val text = field.text.toString().trim()
                if (text.isNotEmpty() && text.toLongOrNull()?.let { it in 1..SettingsCodec.ceilings.getValue(key) } != true) {
                    field.error = getString(R.string.ui_budget_invalid, SettingsCodec.ceilings.getValue(key)); validFields = false
                    (field.parent as? View)?.visibility = View.VISIBLE
                }
            }
            if (!validFields) return@button
            val next = readDraft()
            val valid = runCatching { SettingsCodec.decode(next.toString()) }.getOrNull()
            if (valid == null) error() else {
                saving = true
                request("save", jsonObject("settings" to SettingsCodec.json(valid))) {
                    saving = false; draft = null; rendered = false; refresh()
                    Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
                }
            }
        }
        column = AgentUi.card(page)
        AgentUi.row(column, getString(R.string.presets_title), getString(R.string.ui_preset_optional), "presets") { open(PresetsActivity::class.java) }
        AgentUi.row(column, getString(R.string.presets_set_default), value.string("defaultName"), "default") {
            draft = readDraft()
            val names = value.getAsJsonArray("presets").map { it.asString }
            prompt = AlertDialog.Builder(this).setTitle(R.string.presets_set_default).setItems(names.toTypedArray()) { _, which ->
                request("default", jsonObject("name" to names[which].json())) { refresh() }
            }.showStyled()
        }
        AgentUi.row(column, getString(R.string.memory_title), null, "memory") { open(MemoryActivity::class.java) }
        AgentUi.row(column, getString(R.string.script_roots_title), null, "roots") { open(ScriptRootsActivity::class.java) }
        AgentUi.row(column, getString(R.string.mcp_servers), null, "mcp-servers") { open(McpServersActivity::class.java) }
        column = AgentUi.disclosure(page, R.string.settings_data)
        for ((kind, key, label) in listOf(Triple("history", "historyData", R.string.history_title), Triple("presets", "presetData", R.string.presets_title), Triple("memory", "memoryData", R.string.memory_title))) {
            val statistics = value.getAsJsonObject(key)
            HistoryViews.label(column, getString(R.string.settings_usage, getString(label), statistics.number("count"), statistics.number("bytes"))).tag = "data-$kind"
            button(R.string.settings_clear, "clear-$kind") {
                draft = readDraft()
                prompt = AlertDialog.Builder(this).setTitle(label).setMessage(if (kind == "presets") R.string.settings_clear_presets else R.string.settings_clear_confirm)
                    .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.settings_clear) { _, _ ->
                        request("clear", jsonObject("store" to kind.json())) { refresh() }
                    }.showStyled()
            }.apply {
                AgentUi.role(this, "danger"); isEnabled = value.flag("busy") != true
                contentDescription = getString(R.string.settings_clear) + " " + getString(label)
            }
        }
        localSections()
        rendered = true; tint(page)
    }
    private fun localSections() {
        val info = aiAgentPluginRuntimeInfo()
        val preferences = AppUpdateSettings(this)
        AgentUi.section(page, R.string.app_settings_updates)
        column = AgentUi.card(page)
        AgentUi.row(column, getString(R.string.update_check), getString(R.string.app_update_current_version, info.versionName), "update") { updates.check() }
        val automatic = Switch(this).apply {
            setText(R.string.app_update_automatic); tag = "update-automatic"; isChecked = preferences.automatic
            column.addView(this, LinearLayout.LayoutParams(-1, -2))
            setOnCheckedChangeListener { _, checked -> preferences.automatic = checked }
        }
        HistoryViews.label(column, getString(R.string.app_update_automatic_summary)).labelFor = automatic.id
        AgentUi.row(column, getString(R.string.app_update_manage_ignored), null, "update-ignored") { ignoredUpdates() }
        AgentUi.row(column, getString(R.string.release_history_title), getString(R.string.app_update_release_history_summary), "history") { open(ReleaseHistoryActivity::class.java) }
        column = AgentUi.disclosure(page, R.string.ui_about)
        HistoryViews.label(column, getString(R.string.app_name), true)
        HistoryViews.label(column, getString(R.string.plugin_description))
        HistoryViews.label(column, getString(R.string.settings_version, info.versionName, info.versionCode,
            getString(R.string.plugin_version_date), getString(R.string.plugin_author)))
        AgentUi.row(column, getString(R.string.settings_source), null, "source") { AppUpdateCoordinator.openPage(this, ReleaseInfoCodec.SOURCE) }
        AgentUi.row(column, getString(R.string.settings_license), null, "license") { startActivity(Intent(this, ReleaseHistoryActivity::class.java).putExtra("document", "license")) }
        AgentUi.row(column, getString(R.string.settings_notices), null, "notices") { startActivity(Intent(this, ReleaseHistoryActivity::class.java).putExtra("document", "notices")) }
    }
    private fun ignoredUpdates() {
        val settings = AppUpdateSettings(this)
        val ignored = settings.ignored.sorted()
        val builder = AlertDialog.Builder(this).setTitle(R.string.app_update_manage_ignored)
        if (ignored.isEmpty()) builder.setMessage(R.string.app_update_no_ignored).setPositiveButton(android.R.string.ok, null)
        else {
            val selected = BooleanArray(ignored.size)
            builder.setMultiChoiceItems(ignored.toTypedArray(), selected) { _, index, checked -> selected[index] = checked }
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.app_update_stop_ignoring) { _, _ ->
                    settings.unignore(ignored.filterIndexed { index, _ -> selected[index] })
                }
        }
        prompt = builder.showStyled()
    }
    private fun requestExit() {
        if (saving) return
        if (!rendered || readDraft() == baseline) { finish(); return }
        prompt = AlertDialog.Builder(this).setTitle(R.string.ui_unsaved_title).setMessage(R.string.ui_unsaved_note)
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.ui_discard) { _, _ -> finish() }.showStyled()
    }
    override fun navigateBack() { requestExit() }
    private fun readDraft() = jsonObject("version" to 2.json(), "toolGroups" to JsonArray().apply { groups.filterValues { it.isChecked }.keys.forEach(::add) },
        "budget" to JsonObject().apply { budgets.forEach { (key, field) ->
            val text = field.text.toString().trim(); if (text.isNotEmpty()) {
                val number = text.toLongOrNull(); if (number == null) addProperty(key, text) else addProperty(key, number)
            }
        } }, "cautious" to cautious.isChecked.json(), "voice" to voice.isChecked.json(), "floating" to floating.isChecked.json())
    companion object {
        internal val budgetLabels = linkedMapOf("maxSteps" to R.string.presets_steps, "maxModelCalls" to R.string.presets_calls,
            "maxDurationMs" to R.string.presets_duration, "maxTotalTokens" to R.string.presets_tokens)
        private val groupLabels = mapOf(ToolGroup.OBSERVE to R.string.presets_group_observe, ToolGroup.ACT to R.string.presets_group_act,
            ToolGroup.GESTURE to R.string.presets_group_gesture, ToolGroup.OCR to R.string.presets_group_ocr, ToolGroup.SCRIPT to R.string.presets_group_script,
            ToolGroup.SCRIPT_DYNAMIC to R.string.presets_group_script_dynamic, ToolGroup.MCP to R.string.presets_group_mcp,
            ToolGroup.FILES to R.string.presets_group_files, ToolGroup.SHELL to R.string.presets_group_shell, ToolGroup.MEMORY to R.string.presets_group_memory,
            ToolGroup.USER to R.string.presets_group_user)
    }
}
