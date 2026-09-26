package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.AlertDialog
import android.widget.EditText
import android.widget.LinearLayout
import io.github.supermonster003.autojs6.plugin.ai.agent.R

internal class AppearanceSettings(private val activity: HostAppearanceActivity) {
    private var settings = AppearancePreferences.read(activity)
    internal var dialog: AlertDialog? = null; private set
    fun close() { dialog?.dismiss(); dialog = null }
    fun build(parent: LinearLayout) {
        AgentUi.section(parent, R.string.app_settings_appearance)
        val card = AgentUi.card(parent)
        val current = activity.appearance!!
        AgentUi.row(card, activity.getString(R.string.app_settings_language),
            summary(settings.language, languageLabels[AppearancePreferences.languages.indexOf(settings.language)],
                java.util.Locale.forLanguageTag(current.language).getDisplayName(activity.resources.configuration.locales[0])), "appearance-language") {
            choices(R.string.app_settings_language, languageLabels, AppearancePreferences.languages.indexOf(settings.language)) {
                save(settings.copy(language = AppearancePreferences.languages[it]))
            }
        }
        AgentUi.row(card, activity.getString(R.string.app_settings_dark_mode),
            summary(settings.darkMode, modeLabels[AppearancePreferences.modes.indexOf(settings.darkMode)],
                activity.getString(if (current.dark) R.string.app_settings_always_dark else R.string.app_settings_always_light)), "appearance-dark") {
            choices(R.string.app_settings_dark_mode, modeLabels, AppearancePreferences.modes.indexOf(settings.darkMode)) {
                save(settings.copy(darkMode = AppearancePreferences.modes[it]))
            }
        }
        AgentUi.row(card, activity.getString(R.string.app_settings_theme_color),
            if (settings.color == null) activity.getString(R.string.app_settings_follow_autojs6_summary, AppearancePreferences.colorHex(current.primary))
            else AppearancePreferences.colorHex(settings.color!!), "appearance-color") { colors() }
    }
    private fun summary(mode: String, label: Int, resolved: String) = if (mode == "host") activity.getString(R.string.app_settings_follow_autojs6_summary, resolved) else activity.getString(label)
    private fun choices(title: Int, labels: List<Int>, selection: Int, selected: (Int) -> Unit) {
        dialog = AlertDialog.Builder(activity).setTitle(title).setSingleChoiceItems(labels.map(activity::getString).toTypedArray(), selection) { prompt, index ->
            prompt.dismiss(); selected(index)
        }.setNegativeButton(android.R.string.cancel, null).showStyled()
    }
    private fun save(value: AppearancePreferences) {
        runCatching { value.save(activity) }.onSuccess { settings = value; activity.recreate() }
            .onFailure { android.widget.Toast.makeText(activity, R.string.settings_error, android.widget.Toast.LENGTH_LONG).show() }
    }
    private fun colors() {
        val seeds = listOf<Int?>(null, AppearancePreferences.DEFAULT_COLOR, 0xff007c8a.toInt(), 0xff2e7d32.toInt(), 0xff7e57c2.toInt(), 0xffc86b0a.toInt())
        val labels = listOf(R.string.app_settings_follow_autojs6, R.string.app_settings_theme_blue, R.string.app_settings_theme_teal,
            R.string.app_settings_theme_green, R.string.app_settings_theme_purple, R.string.ui_theme_amber, R.string.app_settings_theme_custom)
        choices(R.string.app_settings_theme_color, labels, seeds.indexOf(settings.color).let { if (it < 0) seeds.size else it }) {
            if (it < seeds.size) save(settings.copy(color = seeds[it])) else customColor()
        }
    }
    private fun customColor() {
        val body = AgentUi.column(activity)
        val field = EditText(activity).apply {
            setHint(R.string.app_settings_custom_color_hint); contentDescription = activity.getString(R.string.app_settings_custom_color_title)
            setText(AppearancePreferences.colorHex(settings.color ?: activity.appearance!!.primary))
            inputType = android.text.InputType.TYPE_CLASS_TEXT; filters = arrayOf(android.text.InputFilter.LengthFilter(7))
            body.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        AgentUi.style(body)
        dialog = AlertDialog.Builder(activity).setTitle(R.string.app_settings_custom_color_title).setView(body)
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, null).create().also { prompt ->
                prompt.setOnShowListener { prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val color = AppearancePreferences.parseColor(field.text.toString())
                    if (color == null) field.error = activity.getString(R.string.app_settings_custom_color_error)
                    else { prompt.dismiss(); save(settings.copy(color = color)) }
                } }; prompt.showStyled()
            }
    }
    private companion object {
        val languageLabels = listOf(R.string.app_settings_follow_autojs6, R.string.app_settings_follow_system,
            R.string.app_language_zh_hans, R.string.app_language_zh_hant_hk, R.string.app_language_zh_hant_tw, R.string.app_language_en,
            R.string.app_language_fr, R.string.app_language_es, R.string.app_language_ja, R.string.app_language_ko, R.string.app_language_ru, R.string.app_language_ar)
        val modeLabels = listOf(R.string.app_settings_follow_autojs6, R.string.app_settings_follow_system,
            R.string.app_settings_always_light, R.string.app_settings_always_dark)
    }
}
