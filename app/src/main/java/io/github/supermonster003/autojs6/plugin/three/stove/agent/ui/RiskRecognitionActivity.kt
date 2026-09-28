package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.os.Bundle
import android.text.InputType
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.RiskRules
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolPolicy
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.SettingsCodec
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import java.util.Locale

/**
 * Risk recognition (roadmap P13): the payment application list and the sensitive keyword table that
 * promote screen actions to sensitive. Packaged entries are shown and cannot be removed; the user's
 * additions apply at once through the private settings, like every other settings screen.
 */
class RiskRecognitionActivity : HostAppearanceActivity() {
    private lateinit var connection: SettingsConnection
    internal lateinit var updater: SettingsUpdater
    private lateinit var scaffold: Scaffold
    private lateinit var packageList: LinearLayout
    private lateinit var keywordList: LinearLayout
    /** Rows that follow the settings snapshot, keyed by tag; disabled until it loads. */
    internal val rows = linkedMapOf<String, SettingRow>()
    /** The last opened dialog, exposed for instrumentation. */
    internal var prompt: AlertDialog? = null; private set
    internal val builtInPackages: List<String> by lazy { ToolPolicy.readPackages(asset(ToolPolicy.PAYMENT_PACKAGES_ASSET)).sorted() }
    private val builtInKeywordTable: JsonObject by lazy { AgentJson.objectOf(asset(ToolPolicy.SENSITIVE_KEYWORDS_ASSET)) }
    private val builtInKeywords: Set<String> by lazy { ToolPolicy.readKeywords(builtInKeywordTable.toString()).map { it.lowercase(Locale.ROOT) }.toSet() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updater = SettingsUpdater(::save, ::render) { kit.snackbar(scaffold.root, getString(R.string.settings_error)); refresh() }
        scaffold = buildScaffold(getString(R.string.risk_title))
        val page = scaffold.content
        page.addView(kit.pageCaption(getString(R.string.risk_instruction)))

        page.addView(kit.sectionHeader(getString(R.string.risk_packages_section)))
        page.addView(kit.settingRow(getString(R.string.risk_builtin), builtInPackages.joinToString(", "), R.drawable.ic_shield, "risk-builtin-packages", chevron = false).view)
        page.addView(kit.hairline(Ui.SCREEN_MARGIN + Ui.ICON_SIZE + Ui.SPACE_LG))
        packageList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "risk-packages" }
        page.addView(packageList, LinearLayout.LayoutParams(-1, -2))
        add(page, "risk-add-package", kit.settingRow(getString(R.string.risk_add_package), null, R.drawable.ic_tune, "risk-add-package") { addPackage() })

        page.addView(kit.sectionHeader(getString(R.string.risk_keywords_section)))
        page.addView(kit.settingRow(getString(R.string.risk_builtin), getString(R.string.risk_builtin_keywords_summary, localKeywords().joinToString(", ")),
            R.drawable.ic_shield, "risk-builtin-keywords", chevron = false).view)
        page.addView(kit.hairline(Ui.SCREEN_MARGIN + Ui.ICON_SIZE + Ui.SPACE_LG))
        keywordList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "risk-keywords" }
        page.addView(keywordList, LinearLayout.LayoutParams(-1, -2))
        add(page, "risk-add-keyword", kit.settingRow(getString(R.string.risk_add_keyword), null, R.drawable.ic_tune, "risk-add-keyword") { addKeyword() })
        page.addView(kit.pageCaption(getString(R.string.settings_policy_note)))
        rows.values.forEach { it.setEnabled(false) }
        setContentView(scaffold.root)
        connection = SettingsConnection(this, ::refresh)
    }
    override fun onStart() { super.onStart(); connection.start() }
    override fun onStop() { connection.stop(); prompt?.dismiss(); prompt = null; super.onStop() }

    private fun asset(path: String) = assets.open(path).bufferedReader().use { it.readText() }
    private fun add(page: LinearLayout, key: String, row: SettingRow) { rows[key] = row; page.addView(row.view, LinearLayout.LayoutParams(-1, -2)) }

    /** The packaged keywords of the screen's language; the other nine languages apply as well. */
    internal fun localKeywords(): List<String> {
        val locale = resources.configuration.locales[0]
        val key = when (locale.language) {
            "zh" -> if (locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO")) {
                if (locale.country == "HK" || locale.country == "MO") "zh-Hant-HK" else "zh-Hant-TW"
            } else "zh-Hans"
            else -> locale.language.takeIf { builtInKeywordTable.has(it) } ?: "en"
        }
        return builtInKeywordTable.getAsJsonArray(key).map { it.asString }
    }

    private fun refresh() {
        connection.query(jsonObject("operation" to "get".json())) { result ->
            result.onSuccess { updater.load(SettingsCodec.decode(it.getAsJsonObject("settings").toString())) }
                .onFailure { kit.snackbar(scaffold.root, getString(R.string.settings_error)) }
        }
    }
    private fun save(settings: AgentSettings, done: (Boolean) -> Unit) {
        connection.query(jsonObject("operation" to "save".json(), "settings" to SettingsCodec.json(settings))) { done(it.isSuccess) }
    }
    private fun render(draft: SettingsDraft) {
        fill(packageList, draft.settings.riskPackages.sorted(), "risk-package") { value -> updater.apply { it.withRiskPackage(value, false) } }
        fill(keywordList, draft.settings.riskKeywords.sorted(), "risk-keyword") { value -> updater.apply { it.withRiskKeyword(value, false) } }
        rows.values.forEach { it.setEnabled(true) }
    }
    private fun fill(list: LinearLayout, values: List<String>, prefix: String, remove: (String) -> Unit) {
        list.removeAllViews()
        for (value in values) {
            val row = kit.settingRow(value, null, null, "$prefix-$value", chevron = false)
            row.view.addView(kit.textButton(getString(R.string.risk_remove), "$prefix-remove-$value", danger = true) { remove(value) }.apply {
                contentDescription = getString(R.string.risk_remove) + ", " + value
            })
            list.addView(row.view, LinearLayout.LayoutParams(-1, -2))
            list.addView(kit.hairline(Ui.SCREEN_MARGIN + Ui.ICON_SIZE + Ui.SPACE_LG))
        }
    }

    internal fun addPackage() {
        val current = updater.current?.settings ?: return
        prompt = kit.inputDialog(getString(R.string.risk_add_package), null, hint = getString(R.string.risk_package_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, maxLength = RiskRules.MAX_PACKAGE_LENGTH,
            validate = { value ->
                val candidate = value.trim()
                when {
                    !RiskRules.isPackage(candidate) -> getString(R.string.risk_invalid_package)
                    candidate in builtInPackages || candidate in current.riskPackages -> getString(R.string.risk_duplicate)
                    current.riskPackages.size >= RiskRules.MAX_ENTRIES -> getString(R.string.risk_limit_reached, RiskRules.MAX_ENTRIES)
                    else -> null
                }
            }) { value -> updater.apply { it.withRiskPackage(value.trim(), true) } }
    }
    internal fun addKeyword() {
        val current = updater.current?.settings ?: return
        prompt = kit.inputDialog(getString(R.string.risk_add_keyword), null, hint = getString(R.string.risk_keyword_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, maxLength = RiskRules.MAX_KEYWORD_LENGTH,
            validate = { value ->
                val candidate = value.trim()
                val known = current.riskKeywords.map { it.lowercase(Locale.ROOT) }
                when {
                    !RiskRules.isKeyword(candidate) -> getString(R.string.risk_invalid_keyword)
                    candidate.lowercase(Locale.ROOT) in builtInKeywords || candidate.lowercase(Locale.ROOT) in known -> getString(R.string.risk_duplicate)
                    current.riskKeywords.size >= RiskRules.MAX_ENTRIES -> getString(R.string.risk_limit_reached, RiskRules.MAX_ENTRIES)
                    else -> null
                }
            }) { value -> updater.apply { it.withRiskKeyword(value.trim(), true) } }
    }
}
