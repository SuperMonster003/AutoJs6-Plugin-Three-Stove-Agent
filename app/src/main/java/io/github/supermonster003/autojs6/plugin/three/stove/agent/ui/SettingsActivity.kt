package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.format.Formatter
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.threeStoveAgentPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.BudgetLimits
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.SettingsCodec
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.update.*

/**
 * Settings modelled on 3-Stone AI: every change applies at once (no Save button). Appearance is a
 * UI-process preference; task settings are whole-object saves through [SettingsUpdater].
 */
class SettingsActivity : HostAppearanceActivity() {
    private lateinit var connection: SettingsConnection
    internal lateinit var scaffold: Scaffold; private set
    internal lateinit var updates: AppUpdateCoordinator; private set
    /** The last opened dialog or bottom sheet, exposed for instrumentation. */
    internal var prompt: AlertDialog? = null; private set
    internal var sheet: SheetHandle? = null; private set
    internal lateinit var access: ChoiceRow
    internal lateinit var updater: SettingsUpdater
    private var snapshot: JsonObject? = null
    internal var awaitingOverlayPermission = false
    internal val rows = linkedMapOf<String, SettingRow>()
    private val groupRows = linkedMapOf<String, SettingRow>()
    private val limitRows = linkedMapOf<String, SettingRow>()
    internal val clearButtons = linkedMapOf<String, View>()
    internal lateinit var fullAccessNote: Banner
    internal lateinit var appearancePreferences: AppearancePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        awaitingOverlayPermission = savedInstanceState?.getBoolean("overlayPermission") == true
        appearancePreferences = AppearancePreferences.read(this)
        updates = AppUpdateCoordinator(this, threeStoveAgentPluginRuntimeInfo().versionName)
        updater = SettingsUpdater(::save, ::renderSettings) {
            kit.snackbar(scaffold.root, getString(R.string.settings_error)); refresh()
        }
        scaffold = buildScaffold(getString(R.string.settings_title))
        buildPage(scaffold.content)
        setContentView(scaffold.root)
        connection = SettingsConnection(this, ::refresh)
    }
    override fun onStart() { super.onStart(); connection.start() }
    override fun onStop() {
        connection.stop(); updates.cancel(); prompt?.dismiss(); prompt = null
        sheet?.dialog?.dismiss(); sheet = null
        super.onStop()
    }
    override fun onDestroy() { updates.close(); super.onDestroy() }
    override fun onResume() {
        super.onResume()
        if (awaitingOverlayPermission) {
            awaitingOverlayPermission = false
            if (Settings.canDrawOverlays(this)) updater.apply { it.withFloating(true) }
            else kit.snackbar(scaffold.root, getString(R.string.floating_permission_required))
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("overlayPermission", awaitingOverlayPermission); super.onSaveInstanceState(outState)
    }

    internal fun choose(title: Int, labels: List<Int>, selection: Int, selected: (Int) -> Unit) {
        prompt = kit.singleChoiceDialog(getString(title), labels.map(::getString), selection, selected)
    }
    internal fun open(type: Class<*>) { startActivity(Intent(this, type)) }

    internal fun saveAppearance(value: AppearancePreferences) {
        runCatching { value.save(this) }.onSuccess { appearancePreferences = value; recreate() }
            .onFailure { kit.snackbar(scaffold.root, getString(R.string.settings_error)) }
    }
    internal fun themeColors() {
        val seeds = listOf<Int?>(null) + AppearancePreferences.CURATED_COLORS
        val labels = listOf(R.string.app_settings_follow_autojs6, R.string.app_settings_theme_blue, R.string.app_settings_theme_teal,
            R.string.app_settings_theme_green, R.string.app_settings_theme_purple, R.string.ui_theme_amber, R.string.app_settings_theme_custom)
        choose(R.string.app_settings_theme_color, labels, seeds.indexOf(appearancePreferences.color).let { if (it < 0) seeds.size else it }) {
            if (it < seeds.size) saveAppearance(appearancePreferences.copy(color = seeds[it]))
            else prompt = kit.inputDialog(getString(R.string.app_settings_custom_color_title),
                AppearancePreferences.colorHex(appearancePreferences.color ?: appearance!!.primary),
                hint = getString(R.string.app_settings_custom_color_hint), maxLength = 7,
                validate = { value -> if (AppearancePreferences.parseColor(value) == null) getString(R.string.app_settings_custom_color_error) else null },
            ) { value -> saveAppearance(appearancePreferences.copy(color = AppearancePreferences.parseColor(value))) }
        }
    }

    private fun request(operation: String, fields: JsonObject = JsonObject(), complete: (JsonObject) -> Unit) {
        fields.addProperty("operation", operation)
        connection.query(fields) { result -> result.onSuccess(complete).onFailure { kit.snackbar(scaffold.root, getString(R.string.settings_error)) } }
    }
    private fun refresh() { request("get") { value -> snapshot = value; renderSnapshot(value); updater.load(SettingsCodec.decode(value.getAsJsonObject("settings").toString())) } }
    private fun save(settings: io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings, done: (Boolean) -> Unit) {
        connection.query(jsonObject("operation" to "save".json(), "settings" to SettingsCodec.json(settings))) { done(it.isSuccess) }
    }

    private fun renderSnapshot(value: JsonObject) {
        rows.getValue("default").setSummary(presetLabel(value.string("defaultName").orEmpty()))
        val busy = value.flag("busy") == true
        for ((kind, key) in listOf("history" to "historyData", "presets" to "presetData", "memory" to "memoryData")) {
            val statistics = value.getAsJsonObject(key)
            rows.getValue("data-$kind").setSummary(getString(R.string.settings_data_summary, (statistics.number("count") ?: 0).toInt(),
                Formatter.formatShortFileSize(this, statistics.number("bytes") ?: 0)))
            clearButtons.getValue(kind).isEnabled = !busy
        }
        for (key in SETTING_KEYS) rows.getValue(key).setEnabled(true)
    }
    private fun renderSettings(draft: SettingsDraft) {
        val settings = draft.settings
        if (access.selectedIndex != draft.accessMode.ordinal) access.select(draft.accessMode.ordinal)
        if (draft.accessMode == AccessMode.FULL) fullAccessNote.view.visibility = View.VISIBLE else fullAccessNote.hide()
        rows.getValue("tool-groups").setSummary(getString(R.string.settings_tool_groups_summary, settings.toolGroups.size, ToolGroup.entries.size))
        groupRows.forEach { (id, row) -> row.switch!!.isChecked = id in settings.toolGroups }
        val defaults = BudgetLimits.defaults(false)
        fun count(key: String, fallback: Long) = (draft.limit(key) ?: fallback).toString()
        rows.getValue("limits").setSummary(getString(R.string.settings_limits_summary, count("maxSteps", defaults.maxSteps.toLong()),
            count("maxModelCalls", defaults.maxModelCalls.toLong()), getString(R.string.settings_minutes, draft.durationMinutes() ?: defaults.maxDurationMs / 60_000),
            count("maxTotalTokens", defaults.maxTotalTokens)))
        limitRows.forEach { (key, row) -> row.setSummary(limitSummary(draft, key)) }
        rows.getValue("risk").setSummary(getString(R.string.settings_risk_summary, settings.riskPackages.size, settings.riskKeywords.size))
        rows.getValue("voice").switch!!.isChecked = settings.voice
        for (channel in AgentSettings.ALERT_CHANNELS) rows.getValue("alert-$channel").switch!!.isChecked = channel in settings.failureAlerts
        rows.getValue("floating").switch!!.isChecked = settings.floating && Settings.canDrawOverlays(this)
        rows.values.forEach(SettingRow::refreshDescription)
    }
    private fun presetLabel(name: String) = if (name == "default") getString(R.string.workbench_default_preset) else name

    internal fun defaultPreset() {
        val names = snapshot?.getAsJsonArray("presets")?.map { it.asString } ?: return
        prompt = kit.singleChoiceDialog(getString(R.string.settings_default_preset), names.map(::presetLabel), names.indexOf(snapshot?.string("defaultName"))) {
            request("default", jsonObject("name" to names[it].json())) { refresh() }
        }
    }
    internal fun toolGroups() {
        val draft = updater.current ?: return
        groupRows.clear()
        val handle = kit.bottomSheet(getString(R.string.presets_tools), onDismiss = { groupRows.clear() })
        ToolGroup.entries.forEach { group ->
            val row = kit.switchRow(ToolPresentation.groupLabel(this@SettingsActivity, group), null, null, group.id in draft.settings.toolGroups, "group-${group.id}") { enabled ->
                if (!updater.apply { it.withGroup(group.id, enabled) }) groupRows[group.id]?.switch?.isChecked = !enabled
            }
            groupRows[group.id] = row; handle.content.addView(row.view)
        }
        handle.content.addView(kit.pageCaption(getString(R.string.settings_ocr_note)))
        sheet = handle
    }
    internal fun limits() {
        val draft = updater.current ?: return
        limitRows.clear()
        val handle = kit.bottomSheet(getString(R.string.ui_budget), minHeightFraction = 0.4f, onDismiss = { limitRows.clear() })
        for (key in listOf("maxSteps", "maxModelCalls", SettingsDraft.DURATION, "maxTotalTokens")) {
            val tag = if (key == SettingsDraft.DURATION) "limit-maxDuration" else "limit-$key"
            val row = kit.settingRow(getString(limitLabels.getValue(key)), limitSummary(draft, key), null, tag) { editLimit(key) }
            limitRows[key] = row; handle.content.addView(row.view)
        }
        handle.content.addView(kit.pageCaption(getString(R.string.settings_limits_note)))
        sheet = handle
    }
    private fun limitSummary(draft: SettingsDraft, key: String): String {
        val defaults = BudgetLimits.defaults(false)
        return if (key == SettingsDraft.DURATION) draft.durationMinutes()?.let { getString(R.string.settings_minutes, it) }
            ?: getString(R.string.settings_limit_automatic, getString(R.string.settings_minutes, defaults.maxDurationMs / 60_000))
        else draft.limit(key)?.toString() ?: getString(R.string.settings_limit_automatic, when (key) {
            "maxSteps" -> defaults.maxSteps.toString(); "maxModelCalls" -> defaults.maxModelCalls.toString(); else -> defaults.maxTotalTokens.toString()
        })
    }
    internal fun editLimit(key: String) {
        val draft = updater.current ?: return
        val duration = key == SettingsDraft.DURATION
        val ceiling = if (duration) SettingsDraft.MAX_DURATION_MINUTES else SettingsCodec.ceilings.getValue(key)
        val current = if (duration) draft.durationMinutes() else draft.limit(key)
        val label = getString(limitLabels.getValue(key))
        prompt = kit.inputDialog(label, current?.toString(), hint = label, inputType = InputType.TYPE_CLASS_NUMBER, maxLength = 9,
            neutral = getString(R.string.history_auto) to { applyLimit(key, null) },
            validate = { value -> if (value.trim().toLongOrNull()?.let { it in 1..ceiling } == true) null else getString(R.string.ui_budget_invalid, ceiling) },
        ) { value -> applyLimit(key, value.trim().toLong()) }
    }
    private fun applyLimit(key: String, value: Long?) {
        updater.apply { if (key == SettingsDraft.DURATION) it.withDurationMinutes(value) else it.withLimit(key, value) }
    }
    internal fun clearData(kind: String, title: Int) {
        prompt = kit.confirmDialog(getString(title), getString(if (kind == "presets") R.string.settings_clear_presets else R.string.settings_clear_confirm),
            getString(R.string.settings_clear), destructive = true) { request("clear", jsonObject("store" to kind.json())) { refresh() } }
    }
    internal fun ignoredUpdates() {
        val settings = AppUpdateSettings(this)
        val ignored = settings.ignored.sorted()
        prompt = if (ignored.isEmpty()) kit.messageDialog(getString(R.string.app_update_manage_ignored), getString(R.string.app_update_no_ignored))
        else kit.multiChoiceDialog(getString(R.string.app_update_manage_ignored), ignored, BooleanArray(ignored.size), getString(R.string.app_update_stop_ignoring)) { selected ->
            settings.unignore(ignored.filterIndexed { index, _ -> selected[index] })
            rows.getValue("update-ignored").setSummary(getString(R.string.app_update_ignored_count, AppUpdateSettings(this).ignored.size))
        }
    }

    companion object {
        internal val languageLabels = listOf(R.string.app_settings_follow_autojs6, R.string.app_settings_follow_system,
            R.string.app_language_zh_hans, R.string.app_language_zh_hant_hk, R.string.app_language_zh_hant_tw, R.string.app_language_en,
            R.string.app_language_fr, R.string.app_language_es, R.string.app_language_ja, R.string.app_language_ko, R.string.app_language_ru, R.string.app_language_ar)
        internal val modeLabels = listOf(R.string.app_settings_follow_autojs6, R.string.app_settings_follow_system,
            R.string.app_settings_always_light, R.string.app_settings_always_dark)
        private val limitLabels = mapOf("maxSteps" to R.string.presets_steps, "maxModelCalls" to R.string.presets_calls,
            SettingsDraft.DURATION to R.string.settings_duration_minutes, "maxTotalTokens" to R.string.presets_tokens)
    }
}

/** Rows that follow the private settings snapshot; disabled until it loads. */
internal val SETTING_KEYS = listOf("default", "confirmation-mode", "tool-groups", "limits", "risk", "voice", "floating",
    "alert-" + AgentSettings.ALERT_NOTIFICATION, "alert-" + AgentSettings.ALERT_TOAST, "alert-" + AgentSettings.ALERT_DIALOG)
