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
        column = HistoryViews.column(this).apply { layoutDirection = resources.configuration.layoutDirection }
        message = HistoryViews.label(column, getString(R.string.interaction_loading))
        setContentView(ScrollView(this).apply { fitsSystemWindows = true; addView(column) })
        connection = SettingsConnection(this, ::refresh)
        updates = AppUpdateCoordinator(this, aiAgentPluginRuntimeInfo().versionName)
    }
    override fun onStart() { super.onStart(); saving = false; connection.start() }
    override fun onStop() { if (rendered) draft = readDraft(); connection.stop(); updates.cancel(); prompt?.dismiss(); prompt = null; super.onStop() }
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
        val form = draft ?: value.getAsJsonObject("settings")
        column.removeAllViews(); groups.clear(); budgets.clear()
        HistoryViews.label(column, getString(R.string.settings_title), true)
        button(R.string.workbench_back, "back") { finish() }
        message = HistoryViews.label(column, "").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE }
        HistoryViews.label(column, getString(R.string.settings_policy_note))
        HistoryViews.label(column, getString(R.string.presets_tools), true)
        val enabled = form.getAsJsonArray("toolGroups").map { it.asString }.toSet()
        ToolGroup.entries.forEach { group ->
            groups[group.id] = checkbox(groupLabels.getValue(group), "group-${group.id}", group.id in enabled)
        }
        HistoryViews.label(column, getString(R.string.settings_ocr_note))
        HistoryViews.label(column, getString(R.string.settings_budget_note))
        for ((key, label) in budgetLabels) {
            val caption = HistoryViews.label(column, getString(label))
            budgets[key] = EditText(this).apply {
                id = View.generateViewId(); tag = key; caption.labelFor = id; inputType = InputType.TYPE_CLASS_NUMBER
                setText(form.getAsJsonObject("budget")[key]?.asString.orEmpty())
                filters = arrayOf(android.text.InputFilter.LengthFilter(16)); column.addView(this)
            }
        }
        cautious = checkbox(R.string.presets_cautious, "cautious", form.flag("cautious") == true)
        voice = checkbox(R.string.settings_voice, "voice", form.flag("voice") == true)
        floating = checkbox(R.string.settings_floating, "floating", form.flag("floating") == true && Settings.canDrawOverlays(this))
        HistoryViews.label(column, getString(R.string.floating_setting_note))
        floating.setOnCheckedChangeListener { _, checked ->
            if (checked && !Settings.canDrawOverlays(this)) {
                floating.isChecked = false
                draft = readDraft(); awaitingOverlayPermission = true
                runCatching { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
                    .onFailure { awaitingOverlayPermission = false; error() }
            }
        }
        button(R.string.settings_save, "save") {
            if (saving) return@button
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
        HistoryViews.label(column, getString(R.string.settings_default, value.string("defaultName")), true)
        button(R.string.presets_set_default, "default") {
            draft = readDraft()
            val names = value.getAsJsonArray("presets").map { it.asString }
            prompt = AlertDialog.Builder(this).setTitle(R.string.presets_set_default).setItems(names.toTypedArray()) { _, which ->
                request("default", jsonObject("name" to names[which].json())) { refresh() }
            }.show()
        }
        button(R.string.presets_title, "presets") { open(PresetsActivity::class.java) }
        button(R.string.script_roots_title, "roots") { open(ScriptRootsActivity::class.java) }
        HistoryViews.label(column, getString(R.string.settings_data), true)
        for ((kind, key, label) in listOf(Triple("history", "historyData", R.string.history_title), Triple("presets", "presetData", R.string.presets_title), Triple("memory", "memoryData", R.string.memory_title))) {
            val statistics = value.getAsJsonObject(key)
            HistoryViews.label(column, getString(R.string.settings_usage, getString(label), statistics.number("count"), statistics.number("bytes")))
                .tag = "data-$kind"
            button(R.string.settings_clear, "clear-$kind") {
                draft = readDraft()
                prompt = AlertDialog.Builder(this).setTitle(label).setMessage(if (kind == "presets") R.string.settings_clear_presets else R.string.settings_clear_confirm)
                    .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.settings_clear) { _, _ ->
                        request("clear", jsonObject("store" to kind.json())) { refresh() }
                    }.show()
            }.apply {
                isEnabled = value.flag("busy") != true
                contentDescription = getString(R.string.settings_clear) + " " + getString(label)
            }
        }
        HistoryViews.label(column, getString(R.string.settings_about), true)
        val info = aiAgentPluginRuntimeInfo()
        HistoryViews.label(column, getString(R.string.settings_version, info.versionName, info.versionCode,
            getString(R.string.plugin_version_date), getString(R.string.plugin_author)))
        button(R.string.settings_license, "license") { startActivity(Intent(this, ReleaseHistoryActivity::class.java).putExtra("document", "license")) }
        button(R.string.settings_notices, "notices") { startActivity(Intent(this, ReleaseHistoryActivity::class.java).putExtra("document", "notices")) }
        button(R.string.settings_source, "source") { AppUpdateCoordinator.openPage(this, ReleaseInfoCodec.SOURCE) }
        button(R.string.release_history_title, "history") { open(ReleaseHistoryActivity::class.java) }
        HistoryViews.label(column, getString(R.string.update_policy))
        button(R.string.update_check, "update") { updates.check() }
        rendered = true; tint(column)
    }
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
            ToolGroup.FILES to R.string.presets_group_files, ToolGroup.SHELL to R.string.presets_group_shell, ToolGroup.MEMORY to R.string.presets_group_memory,
            ToolGroup.USER to R.string.presets_group_user)
    }
}
