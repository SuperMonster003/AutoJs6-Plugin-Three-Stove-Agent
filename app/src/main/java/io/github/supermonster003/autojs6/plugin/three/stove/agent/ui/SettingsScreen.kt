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

/** Settings screen construction (roadmap P11): the rows and captions; SettingsActivity keeps state and events. */

internal fun SettingsActivity.buildPage(page: LinearLayout) = with(kit) {
    // Appearance: UI-process preferences, applied immediately by recreating the screen.
    page.addView(sectionHeader(getString(R.string.app_settings_appearance)))
    val current = appearance!!
    add(page, "appearance-language", settingRow(getString(R.string.app_settings_language), followSummary(appearancePreferences.language,
        SettingsActivity.languageLabels[AppearancePreferences.languages.indexOf(appearancePreferences.language)],
        java.util.Locale.forLanguageTag(current.language).getDisplayName(resources.configuration.locales[0])), R.drawable.ic_language, "appearance-language") {
        choose(R.string.app_settings_language, SettingsActivity.languageLabels, AppearancePreferences.languages.indexOf(appearancePreferences.language)) {
            saveAppearance(appearancePreferences.copy(language = AppearancePreferences.languages[it]))
        }
    })
    add(page, "appearance-dark", settingRow(getString(R.string.app_settings_dark_mode), followSummary(appearancePreferences.darkMode,
        SettingsActivity.modeLabels[AppearancePreferences.modes.indexOf(appearancePreferences.darkMode)],
        getString(if (current.dark) R.string.app_settings_always_dark else R.string.app_settings_always_light)), R.drawable.ic_dark_mode, "appearance-dark") {
        choose(R.string.app_settings_dark_mode, SettingsActivity.modeLabels, AppearancePreferences.modes.indexOf(appearancePreferences.darkMode)) {
            saveAppearance(appearancePreferences.copy(darkMode = AppearancePreferences.modes[it]))
        }
    })
    val color = settingRow(getString(R.string.app_settings_theme_color),
        appearancePreferences.color?.let(AppearancePreferences::colorHex)
            ?: getString(R.string.app_settings_follow_autojs6_summary, AppearancePreferences.colorHex(current.primary)),
        R.drawable.ic_palette, "appearance-color") { themeColors() }
    color.view.addView(View(this@buildPage).apply {
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
    add(page, "limits", settingRow(getString(R.string.ui_budget), null, R.drawable.ic_timer, "limits") { limits() })
    add(page, "risk", settingRow(getString(R.string.risk_title), null, R.drawable.ic_warning, "risk") { open(RiskRecognitionActivity::class.java) }, divider = false)
    page.addView(pageCaption(getString(R.string.settings_policy_note)))

    page.addView(sectionHeader(getString(R.string.settings_section_tools)))
    add(page, "presets", settingRow(getString(R.string.presets_title), getString(R.string.ui_preset_optional), R.drawable.ic_layers, "presets") { open(PresetsActivity::class.java) })
    add(page, "memory", settingRow(getString(R.string.memory_title), null, R.drawable.ic_lightbulb, "memory") { open(MemoryActivity::class.java) })
    add(page, "roots", settingRow(getString(R.string.script_roots_title), null, R.drawable.ic_folder, "roots") { open(ScriptRootsActivity::class.java) })
    add(page, "mcp-servers", settingRow(getString(R.string.mcp_servers), null, R.drawable.ic_hub, "mcp-servers") { open(McpServersActivity::class.java) }, divider = false)

    page.addView(sectionHeader(getString(R.string.settings_section_quick)))
    // The microphone only appears when a speech recognizer app exists; say so here instead of leaving the switch silent (maintainer feedback, 2026-09-29).
    add(page, "voice", switchRow(getString(R.string.settings_voice),
        getString(if (SpeechInput.available(this@buildPage)) R.string.settings_voice_note else R.string.settings_voice_unavailable), R.drawable.ic_mic, false, "voice") { enabled ->
        updater.apply { it.withVoice(enabled) }
    })
    add(page, "floating", switchRow(getString(R.string.settings_floating), getString(R.string.floating_setting_note), R.drawable.ic_bubble, false, "floating") { enabled ->
        if (enabled && !Settings.canDrawOverlays(this@buildPage)) {
            rows.getValue("floating").switch!!.isChecked = false
            awaitingOverlayPermission = true
            runCatching { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
                .onFailure { awaitingOverlayPermission = false; snackbar(scaffold.root, getString(R.string.settings_open_failed)) }
        } else updater.apply { it.withFloating(enabled) }
    }, divider = false)

    page.addView(sectionHeader(getString(R.string.settings_alerts_section)))
    val alertRows = listOf(Triple(AgentSettings.ALERT_NOTIFICATION, R.string.settings_alert_notification, R.drawable.ic_task),
        Triple(AgentSettings.ALERT_TOAST, R.string.settings_alert_toast, R.drawable.ic_bubble), Triple(AgentSettings.ALERT_DIALOG, R.string.settings_alert_dialog, R.drawable.ic_description))
    for ((channel, label, icon) in alertRows) add(page, "alert-$channel", switchRow(getString(label), null, icon, false, "alert-$channel") { enabled ->
        updater.apply { it.withFailureAlert(channel, enabled) }
    }, divider = channel != AgentSettings.ALERT_DIALOG)
    page.addView(pageCaption(getString(R.string.settings_alerts_note)))

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
    val info = threeStoveAgentPluginRuntimeInfo()
    val preferences = AppUpdateSettings(this@buildPage)
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
    for (key in SETTING_KEYS) rows.getValue(key).setEnabled(false)
}

internal fun Kit.pageCaption(value: CharSequence): TextView = text(value, Ui.TEXT_SECONDARY, palette.muted).apply {
    setPaddingRelative(dp(Ui.SCREEN_MARGIN), dp(Ui.SPACE_XS), dp(Ui.SCREEN_MARGIN), dp(Ui.SPACE_SM))
}

internal fun SettingsActivity.add(page: LinearLayout, key: String, row: SettingRow, divider: Boolean = true) {
    rows[key] = row; page.addView(row.view, LinearLayout.LayoutParams(-1, -2))
    if (divider) page.addView(kit.hairline(Ui.SCREEN_MARGIN + Ui.ICON_SIZE + Ui.SPACE_LG))
}

internal fun SettingsActivity.followSummary(mode: String, label: Int, resolved: String) =
    if (mode == "host") getString(R.string.app_settings_follow_autojs6_summary, resolved) else getString(label)
