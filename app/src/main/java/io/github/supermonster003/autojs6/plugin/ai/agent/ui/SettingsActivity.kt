package io.github.supermonster003.autojs6.plugin.ai.agent.ui

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
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.aiAgentPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.BudgetLimits
import io.github.supermonster003.autojs6.plugin.ai.agent.store.SettingsCodec
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.*
import io.github.supermonster003.autojs6.plugin.ai.agent.update.*

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
    internal lateinit var access: ChoiceRow; private set
    private lateinit var updater: SettingsUpdater
    private var snapshot: JsonObject? = null
    private var awaitingOverlayPermission = false
    private val rows = linkedMapOf<String, SettingRow>()
    private val groupRows = linkedMapOf<String, SettingRow>()
    private val limitRows = linkedMapOf<String, SettingRow>()
    private val clearButtons = linkedMapOf<String, View>()
    private lateinit var fullAccessNote: Banner
    private lateinit var appearancePreferences: AppearancePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        awaitingOverlayPermission = savedInstanceState?.getBoolean("overlayPermission") == true
        appearancePreferences = AppearancePreferences.read(this)
        updates = AppUpdateCoordinator(this, aiAgentPluginRuntimeInfo().versionName)
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

    private fun buildPage(page: LinearLayout) = with(kit) {
        // Appearance: UI-process preferences, applied immediately by recreating the screen.
        page.addView(sectionHeader(getString(R.string.app_settings_appearance)))
        val current = appearance!!
        add(page, "appearance-language", settingRow(getString(R.string.app_settings_language), followSummary(appearancePreferences.language,
            languageLabels[AppearancePreferences.languages.indexOf(appearancePreferences.language)],
            java.util.Locale.forLanguageTag(current.language).getDisplayName(resources.configuration.locales[0])), R.drawable.ic_language, "appearance-language") {
            choose(R.string.app_settings_language, languageLabels, AppearancePreferences.languages.indexOf(appearancePreferences.language)) {
                saveAppearance(appearancePreferences.copy(language = AppearancePreferences.languages[it]))
            }
        })
        add(page, "appearance-dark", settingRow(getString(R.string.app_settings_dark_mode), followSummary(appearancePreferences.darkMode,
            modeLabels[AppearancePreferences.modes.indexOf(appearancePreferences.darkMode)],
            getString(if (current.dark) R.string.app_settings_always_dark else R.string.app_settings_always_light)), R.drawable.ic_dark_mode, "appearance-dark") {
            choose(R.string.app_settings_dark_mode, modeLabels, AppearancePreferences.modes.indexOf(appearancePreferences.darkMode)) {
                saveAppearance(appearancePreferences.copy(darkMode = AppearancePreferences.modes[it]))
            }
        })
        val color = settingRow(getString(R.string.app_settings_theme_color),
            appearancePreferences.color?.let(AppearancePreferences::colorHex)
                ?: getString(R.string.app_settings_follow_autojs6_summary, AppearancePreferences.colorHex(current.primary)),
            R.drawable.ic_palette, "appearance-color") { themeColors() }
        color.view.addView(View(this@SettingsActivity).apply {
            background = roundedFill(palette.primary, Ui.RADIUS_PILL, palette.outline)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, color.view.childCount - 1, LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginStart = dp(Ui.SPACE_MD) })
        add(page, "appearance-color", color, divider = false)

        // Tasks: whole-object saves of the private settings.
        page.addView(sectionHeader(getString(R.string.ui_task_section)))
        add(page, "default", settingRow(getString(R.string.settings_default_preset), getString(R.string.interaction_loading), R.drawable.ic_bookmark, "default") { defaultPreset() })
        access = choiceRow(getString(R.string.settings_access_mode),
            listOf(R.string.presets_standard, R.string.presets_cautious, R.string.settings_full_access).map(::getString), 0,
            R.drawable.ic_shield, "confirmation-mode") { index -> updater.apply { it.withAccess(AccessMode.entries[index]) } }
        add(page, "confirmation-mode", access.row)
        fullAccessNote = Banner(this).apply {
            view.tag = "full-access-note"; show(getString(R.string.settings_full_access_note), Tone.DANGER, R.drawable.ic_warning); hide()
        }
        page.addView(fullAccessNote.view, LinearLayout.LayoutParams(-1, -2).apply {
            marginStart = dp(Ui.SCREEN_MARGIN); marginEnd = dp(Ui.SCREEN_MARGIN); bottomMargin = dp(Ui.SPACE_SM)
        })
        add(page, "tool-groups", settingRow(getString(R.string.presets_tools), null, R.drawable.ic_tune, "tool-groups") { toolGroups() })
        add(page, "limits", settingRow(getString(R.string.ui_budget), null, R.drawable.ic_timer, "limits") { limits() }, divider = false)
        page.addView(pageCaption(getString(R.string.settings_policy_note)))

        page.addView(sectionHeader(getString(R.string.settings_section_tools)))
        add(page, "presets", settingRow(getString(R.string.presets_title), getString(R.string.ui_preset_optional), R.drawable.ic_layers, "presets") { open(PresetsActivity::class.java) })
        add(page, "memory", settingRow(getString(R.string.memory_title), null, R.drawable.ic_lightbulb, "memory") { open(MemoryActivity::class.java) })
        add(page, "roots", settingRow(getString(R.string.script_roots_title), null, R.drawable.ic_folder, "roots") { open(ScriptRootsActivity::class.java) })
        add(page, "mcp-servers", settingRow(getString(R.string.mcp_servers), null, R.drawable.ic_hub, "mcp-servers") { open(McpServersActivity::class.java) }, divider = false)

        page.addView(sectionHeader(getString(R.string.settings_section_quick)))
        add(page, "voice", switchRow(getString(R.string.settings_voice), null, R.drawable.ic_mic, false, "voice") { enabled ->
            updater.apply { it.withVoice(enabled) }
        })
        add(page, "floating", switchRow(getString(R.string.settings_floating), getString(R.string.floating_setting_note), R.drawable.ic_bubble, false, "floating") { enabled ->
            if (enabled && !Settings.canDrawOverlays(this@SettingsActivity)) {
                rows.getValue("floating").switch!!.isChecked = false
                awaitingOverlayPermission = true
                runCatching { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
                    .onFailure { awaitingOverlayPermission = false; snackbar(scaffold.root, getString(R.string.settings_open_failed)) }
            } else updater.apply { it.withFloating(enabled) }
        }, divider = false)

        page.addView(sectionHeader(getString(R.string.settings_data)))
        for ((kind, title, icon) in listOf(Triple("history", R.string.history_title, R.drawable.ic_history),
            Triple("presets", R.string.presets_title, R.drawable.ic_layers), Triple("memory", R.string.memory_title, R.drawable.ic_lightbulb))) {
            val row = settingRow(getString(title), getString(R.string.interaction_loading), icon, "data-$kind", chevron = false)
            val clear = textButton(getString(R.string.settings_clear), "clear-$kind", danger = true) { clearData(kind, title) }.apply {
                contentDescription = getString(R.string.settings_clear) + ", " + getString(title); isEnabled = false
            }
            row.view.addView(clear)
            clearButtons[kind] = clear
            add(page, "data-$kind", row, divider = kind != "memory")
        }

        page.addView(sectionHeader(getString(R.string.app_settings_updates)))
        val info = aiAgentPluginRuntimeInfo()
        val preferences = AppUpdateSettings(this@SettingsActivity)
        add(page, "update", settingRow(getString(R.string.update_check), getString(R.string.app_update_current_version, info.versionName), R.drawable.ic_restart, "update") { updates.check() })
        add(page, "update-automatic", switchRow(getString(R.string.app_update_automatic), getString(R.string.app_update_automatic_summary),
            R.drawable.ic_download, preferences.automatic, "update-automatic") { preferences.automatic = it })
        add(page, "update-ignored", settingRow(getString(R.string.app_update_manage_ignored),
            getString(R.string.app_update_ignored_count, preferences.ignored.size), R.drawable.ic_block, "update-ignored") { ignoredUpdates() })
        add(page, "history", settingRow(getString(R.string.release_history_title), getString(R.string.app_update_release_history_summary), R.drawable.ic_article, "history") {
            open(ReleaseHistoryActivity::class.java)
        }, divider = false)

        page.addView(sectionHeader(getString(R.string.settings_section_information)))
        add(page, "about", settingRow(getString(R.string.ui_about), getString(R.string.about_summary), R.drawable.ic_info, "about") { open(AboutActivity::class.java) }, divider = false)
        for (key in listOf("default", "confirmation-mode", "tool-groups", "limits", "voice", "floating")) rows.getValue(key).setEnabled(false)
    }

    private fun Kit.pageCaption(value: CharSequence): TextView = text(value, Ui.TEXT_SECONDARY, palette.muted).apply {
        setPaddingRelative(dp(Ui.SCREEN_MARGIN), dp(Ui.SPACE_XS), dp(Ui.SCREEN_MARGIN), dp(Ui.SPACE_SM))
    }
    private fun add(page: LinearLayout, key: String, row: SettingRow, divider: Boolean = true) {
        rows[key] = row; page.addView(row.view, LinearLayout.LayoutParams(-1, -2))
        if (divider) page.addView(kit.hairline(Ui.SCREEN_MARGIN + Ui.ICON_SIZE + Ui.SPACE_LG))
    }
    private fun followSummary(mode: String, label: Int, resolved: String) =
        if (mode == "host") getString(R.string.app_settings_follow_autojs6_summary, resolved) else getString(label)
    private fun choose(title: Int, labels: List<Int>, selection: Int, selected: (Int) -> Unit) {
        prompt = kit.singleChoiceDialog(getString(title), labels.map(::getString), selection, selected)
    }
    private fun open(type: Class<*>) { startActivity(Intent(this, type)) }

    private fun saveAppearance(value: AppearancePreferences) {
        runCatching { value.save(this) }.onSuccess { appearancePreferences = value; recreate() }
            .onFailure { kit.snackbar(scaffold.root, getString(R.string.settings_error)) }
    }
    private fun themeColors() {
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
    private fun save(settings: io.github.supermonster003.autojs6.plugin.ai.agent.store.AgentSettings, done: (Boolean) -> Unit) {
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
        for (key in listOf("default", "confirmation-mode", "tool-groups", "limits", "voice", "floating")) rows.getValue(key).setEnabled(true)
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
        rows.getValue("voice").switch!!.isChecked = settings.voice
        rows.getValue("floating").switch!!.isChecked = settings.floating && Settings.canDrawOverlays(this)
        rows.values.forEach(SettingRow::refreshDescription)
    }
    private fun presetLabel(name: String) = if (name == "default") getString(R.string.workbench_default_preset) else name

    private fun defaultPreset() {
        val names = snapshot?.getAsJsonArray("presets")?.map { it.asString } ?: return
        prompt = kit.singleChoiceDialog(getString(R.string.settings_default_preset), names.map(::presetLabel), names.indexOf(snapshot?.string("defaultName"))) {
            request("default", jsonObject("name" to names[it].json())) { refresh() }
        }
    }
    private fun toolGroups() {
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
    private fun limits() {
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
    private fun clearData(kind: String, title: Int) {
        prompt = kit.confirmDialog(getString(title), getString(if (kind == "presets") R.string.settings_clear_presets else R.string.settings_clear_confirm),
            getString(R.string.settings_clear), destructive = true) { request("clear", jsonObject("store" to kind.json())) { refresh() } }
    }
    private fun ignoredUpdates() {
        val settings = AppUpdateSettings(this)
        val ignored = settings.ignored.sorted()
        prompt = if (ignored.isEmpty()) kit.messageDialog(getString(R.string.app_update_manage_ignored), getString(R.string.app_update_no_ignored))
        else kit.multiChoiceDialog(getString(R.string.app_update_manage_ignored), ignored, BooleanArray(ignored.size), getString(R.string.app_update_stop_ignoring)) { selected ->
            settings.unignore(ignored.filterIndexed { index, _ -> selected[index] })
            rows.getValue("update-ignored").setSummary(getString(R.string.app_update_ignored_count, AppUpdateSettings(this).ignored.size))
        }
    }

    companion object {
        private val languageLabels = listOf(R.string.app_settings_follow_autojs6, R.string.app_settings_follow_system,
            R.string.app_language_zh_hans, R.string.app_language_zh_hant_hk, R.string.app_language_zh_hant_tw, R.string.app_language_en,
            R.string.app_language_fr, R.string.app_language_es, R.string.app_language_ja, R.string.app_language_ko, R.string.app_language_ru, R.string.app_language_ar)
        private val modeLabels = listOf(R.string.app_settings_follow_autojs6, R.string.app_settings_follow_system,
            R.string.app_settings_always_light, R.string.app_settings_always_dark)
        private val limitLabels = mapOf("maxSteps" to R.string.presets_steps, "maxModelCalls" to R.string.presets_calls,
            SettingsDraft.DURATION to R.string.settings_duration_minutes, "maxTotalTokens" to R.string.presets_tokens)
    }
}
