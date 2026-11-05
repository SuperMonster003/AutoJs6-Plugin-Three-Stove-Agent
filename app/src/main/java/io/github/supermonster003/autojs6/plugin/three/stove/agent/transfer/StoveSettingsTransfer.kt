package io.github.supermonster003.autojs6.plugin.three.stove.agent.transfer

import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.RiskRules
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.three.stove.agent.mcp.McpProfileCodec
import io.github.supermonster003.autojs6.plugin.three.stove.agent.mcp.McpServerProfile
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.SettingsCodec
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.AppearancePreferences
import org.autojs.plugin.common.api.PluginSettingsTransferProvider.Exported
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import org.autojs.plugin.common.api.AutoJs6PluginSettingsTransferContract as Contract

/**
 * What [StoveSettingsTransfer] reads and writes: the agent settings (`agent-settings.json`), the appearance of the
 * plugin's own screens (`app-appearance.json`) and the encrypted MCP server profiles (`mcp-profiles.enc`).
 */
internal interface StoveSettingsStorage {

    /** The saved agent settings (defaults when never saved), or null when they cannot be read. */
    fun settings(): AgentSettings?

    /** Saves [settings] through their owner; throws when the write fails. */
    fun writeSettings(settings: AgentSettings)

    fun appearance(): AppearancePreferences

    /** Throws when the write fails. */
    fun writeAppearance(appearance: AppearancePreferences)

    /** The MCP server profiles, or null when the encrypted store cannot be opened (for example a lost Keystore key). */
    fun mcpProfiles(): List<McpServerProfile>?

    /**
     * Replaces the profiles read by the last [mcpProfiles]. False, with nothing written, while a task runs or after the
     * profiles changed since that read (the MCP settings page refuses to save then as well); throws when the write fails.
     */
    fun writeMcpProfiles(profiles: List<McpServerProfile>): Boolean
}

/**
 * The settings document of AutoJs6's settings transfer (contract v1, roadmap P20 / D93 - D96, host settings roadmap
 * P18.3). Three stores:
 *
 * - `settings`, the agent settings of the settings page as typed values: `toolGroups`, `failureAlerts`,
 *   `completionAlerts`, `riskPackages`, `riskKeywords` stringSet, `budget` json (an object of the budget limits the
 *   user changed, see [SettingsCodec.ceilings]; `{}` keeps every default), `accessMode` string (`standard`,
 *   `cautious` or `full`), `voice` and `floating` bool.
 * - `appearance`, the plugin's own language, dark mode and theme color: `language` and `darkMode` string (one of
 *   [AppearancePreferences.languages] / [AppearancePreferences.modes]), `color` string (`host` to follow AutoJs6, or
 *   `#RRGGBB`).
 * - `mcp`, one json entry per MCP server profile keyed by its id: `name`, `endpoint`, `enabled`, `risk`,
 *   `selectedTools` and, when the profile has one, `bearerToken`; only present when there are profiles.
 *
 * A bearer token is a credential, so a document with one is sensitive and AutoJs6 seals it. Import is lenient:
 * unknown stores and keys, other tags and values the settings pages could not have saved are skipped (an unknown store
 * counts its entries, a store or an entry that is not an object counts 1), and so is `accessMode` `full`: full access
 * only comes from the settings page of this device (D43, D94). Unknown tool group ids (from a later version) are
 * dropped from `toolGroups`. Keys the document leaves out keep their values; imported MCP profiles replace the profile
 * with the same id and join the others, a new one beyond [McpServerProfile.MAX_PROFILES] is skipped, and so are all of
 * them while a task runs or when the MCP store cannot be opened. So the same document can be imported again. Each store
 * is written once; a failed write fails the import. Pure JVM.
 */
internal class StoveSettingsTransfer(private val storage: StoveSettingsStorage) {

    fun exportSettings(): Exported {
        val settings = JSONObject()
        storage.settings()?.let { value ->
            settings.put(
                STORE_SETTINGS, JSONObject()
                    .put(KEY_TOOL_GROUPS, stringSet(value.toolGroups))
                    .put(KEY_BUDGET, typed(Contract.TYPE_JSON, JSONObject().apply { value.budget.toSortedMap().forEach { (key, limit) -> put(key, limit) } }))
                    .put(KEY_ACCESS_MODE, typed(Contract.TYPE_STRING, value.accessMode))
                    .put(KEY_VOICE, typed(Contract.TYPE_BOOL, value.voice))
                    .put(KEY_FLOATING, typed(Contract.TYPE_BOOL, value.floating))
                    .put(KEY_FAILURE_ALERTS, stringSet(value.failureAlerts))
                    .put(KEY_COMPLETION_ALERTS, stringSet(value.completionAlerts))
                    .put(KEY_RISK_PACKAGES, stringSet(value.riskPackages))
                    .put(KEY_RISK_KEYWORDS, stringSet(value.riskKeywords)),
            )
        }
        val appearance = storage.appearance()
        settings.put(
            STORE_APPEARANCE, JSONObject()
                .put(KEY_LANGUAGE, typed(Contract.TYPE_STRING, appearance.language))
                .put(KEY_DARK_MODE, typed(Contract.TYPE_STRING, appearance.darkMode))
                .put(KEY_COLOR, typed(Contract.TYPE_STRING, appearance.color?.let(AppearancePreferences::colorHex) ?: COLOR_HOST)),
        )
        val profiles = storage.mcpProfiles().orEmpty()
        if (profiles.isNotEmpty()) {
            val mcp = JSONObject()
            profiles.forEach { profile ->
                val record = JSONObject(McpProfileCodec.encode(profile, includeSecret = true).toString())
                record.remove(RECORD_ID)
                mcp.put(profile.id, typed(Contract.TYPE_JSON, record))
            }
            settings.put(STORE_MCP, mcp)
        }
        val entries = settings.keys().asSequence().sumOf { settings.getJSONObject(it).length() }
        return Exported(settings, entries, sensitive = profiles.any { it.bearerToken != null })
    }

    /** Writes the valid entries of [settings] and answers how many were written and skipped; see the class. */
    fun importSettings(settings: JSONObject): Counts {
        val counts = Counts()
        settings.keys().forEach { storeName ->
            if (storeName !in STORES) counts.skipped += (settings.opt(storeName) as? JSONObject)?.length() ?: 1
        }
        STORES.forEach { storeName ->
            val store = settings.opt(storeName) ?: return@forEach
            if (store !is JSONObject) {
                counts.skipped++
                return@forEach
            }
            when (storeName) {
                STORE_SETTINGS -> importSettingsStore(store, counts)
                STORE_APPEARANCE -> importAppearance(store, counts)
                STORE_MCP -> importMcp(store, counts)
            }
        }
        return counts
    }

    private fun importSettingsStore(store: JSONObject, counts: Counts) {
        var value = storage.settings() ?: run {
            counts.skipped += store.length()
            return
        }
        var accepted = 0
        store.keys().forEach { key ->
            val next = when (key) {
                KEY_TOOL_GROUPS -> strings(store.tagged(key, Contract.TYPE_STRING_SET))
                    ?.let { ids -> value.copy(toolGroups = ids.filterTo(LinkedHashSet()) { id -> ToolGroup.entries.any { it.id == id } }) }
                KEY_BUDGET -> budgetOf(store.tagged(key, Contract.TYPE_JSON))?.let { value.copy(budget = it) }
                KEY_ACCESS_MODE -> when (store.tagged(key, Contract.TYPE_STRING)) {
                    ACCESS_STANDARD -> value.copy(cautious = false, fullAccess = false)
                    ACCESS_CAUTIOUS -> value.copy(cautious = true, fullAccess = false)
                    else -> null // Including "full" (D94).
                }
                KEY_VOICE -> (store.tagged(key, Contract.TYPE_BOOL) as? Boolean)?.let { value.copy(voice = it) }
                KEY_FLOATING -> (store.tagged(key, Contract.TYPE_BOOL) as? Boolean)?.let { value.copy(floating = it) }
                KEY_FAILURE_ALERTS -> channels(store.tagged(key, Contract.TYPE_STRING_SET))?.let { value.copy(failureAlerts = it) }
                KEY_COMPLETION_ALERTS -> channels(store.tagged(key, Contract.TYPE_STRING_SET))?.let { value.copy(completionAlerts = it) }
                KEY_RISK_PACKAGES -> riskEntries(store.tagged(key, Contract.TYPE_STRING_SET), RiskRules::isPackage)?.let { value.copy(riskPackages = it) }
                KEY_RISK_KEYWORDS -> riskEntries(store.tagged(key, Contract.TYPE_STRING_SET), RiskRules::isKeyword)?.let { value.copy(riskKeywords = it) }
                else -> null
            }
            if (next == null) counts.skipped++ else {
                value = next
                accepted++
            }
        }
        if (accepted == 0) return
        storage.writeSettings(value)
        counts.written += accepted
    }

    private fun importAppearance(store: JSONObject, counts: Counts) {
        var value = storage.appearance()
        var accepted = 0
        store.keys().forEach { key ->
            val text = store.tagged(key, Contract.TYPE_STRING) as? String
            val next = when (key) {
                KEY_LANGUAGE -> text?.takeIf { it in AppearancePreferences.languages }?.let { value.copy(language = it) }
                KEY_DARK_MODE -> text?.takeIf { it in AppearancePreferences.modes }?.let { value.copy(darkMode = it) }
                KEY_COLOR -> when {
                    text == COLOR_HOST -> value.copy(color = null)
                    text != null && text.matches(COLOR_SHAPE) -> AppearancePreferences.parseColor(text)?.let { value.copy(color = it) }
                    else -> null
                }
                else -> null
            }
            if (next == null) counts.skipped++ else {
                value = next
                accepted++
            }
        }
        if (accepted == 0) return
        storage.writeAppearance(value)
        counts.written += accepted
    }

    private fun importMcp(store: JSONObject, counts: Counts) {
        val current = storage.mcpProfiles() ?: run {
            counts.skipped += store.length()
            return
        }
        val profiles = LinkedHashMap<String, McpServerProfile>()
        current.forEach { profiles[it.id] = it }
        var accepted = 0
        store.keys().asSequence().sorted().forEach { id ->
            val profile = (store.tagged(id, Contract.TYPE_JSON) as? JSONObject)?.let { profileOf(id, it) }
            if (profile == null || (id !in profiles && profiles.size >= McpServerProfile.MAX_PROFILES)) counts.skipped++ else {
                profiles[id] = profile
                accepted++
            }
        }
        if (accepted == 0) return
        if (!storage.writeMcpProfiles(profiles.values.toList())) {
            counts.skipped += accepted
            return
        }
        counts.written += accepted
    }

    class Counts {
        var written = 0
        var skipped = 0
    }

    companion object {

        const val STORE_SETTINGS = "settings"
        const val STORE_APPEARANCE = "appearance"
        const val STORE_MCP = "mcp"

        /** The stores in the order they are written. */
        val STORES: List<String> = listOf(STORE_SETTINGS, STORE_APPEARANCE, STORE_MCP)

        const val KEY_TOOL_GROUPS = "toolGroups"
        const val KEY_BUDGET = "budget"
        const val KEY_ACCESS_MODE = "accessMode"
        const val KEY_VOICE = "voice"
        const val KEY_FLOATING = "floating"
        const val KEY_FAILURE_ALERTS = "failureAlerts"
        const val KEY_COMPLETION_ALERTS = "completionAlerts"
        const val KEY_RISK_PACKAGES = "riskPackages"
        const val KEY_RISK_KEYWORDS = "riskKeywords"
        const val KEY_LANGUAGE = "language"
        const val KEY_DARK_MODE = "darkMode"
        const val KEY_COLOR = "color"

        const val ACCESS_STANDARD = "standard"
        const val ACCESS_CAUTIOUS = "cautious"
        const val COLOR_HOST = "host"

        private const val RECORD_ID = "id"
        private val COLOR_SHAPE = Regex("#[0-9A-Fa-f]{6}")

        private fun typed(tag: String, value: Any): JSONObject = JSONObject().put(Contract.VALUE_KEY_TYPE, tag).put(Contract.VALUE_KEY_VALUE, value)

        private fun stringSet(values: Set<String>): JSONObject = typed(Contract.TYPE_STRING_SET, JSONArray(values.sorted()))

        /** The value of the entry [name] when it is an object of the [tag]; null otherwise. */
        private fun JSONObject.tagged(name: String, tag: String): Any? {
            val entry = opt(name) as? JSONObject ?: return null
            if (entry.opt(Contract.VALUE_KEY_TYPE) != tag) return null
            return entry.opt(Contract.VALUE_KEY_VALUE)?.takeUnless { it == JSONObject.NULL }
        }

        /** The distinct strings of a JSON array, or null when it holds anything else. */
        private fun strings(value: Any?): Set<String>? {
            val array = value as? JSONArray ?: return null
            val strings = (0 until array.length()).map { array.opt(it) as? String ?: return null }
            return strings.toCollection(LinkedHashSet())
        }

        private fun channels(value: Any?): Set<String>? = strings(value)?.takeIf { it.all { channel -> channel in AgentSettings.ALERT_CHANNELS } }

        private fun riskEntries(value: Any?, valid: (String) -> Boolean): Set<String>? =
            strings(value)?.takeIf { it.size <= RiskRules.MAX_ENTRIES && it.all(valid) }

        /** The budget limits as the settings page saves them: known keys, whole numbers within their ceilings. */
        private fun budgetOf(value: Any?): Map<String, Long>? {
            val limits = value as? JSONObject ?: return null
            val budget = LinkedHashMap<String, Long>()
            limits.keys().forEach { key ->
                val ceiling = SettingsCodec.ceilings[key] ?: return null
                budget[key] = wholeNumber(limits.opt(key))?.takeIf { it in 1..ceiling } ?: return null
            }
            return budget
        }

        /** A JSON number without a fraction (`2000.0` counts, `15.5` does not). */
        private fun wholeNumber(value: Any?): Long? = when (value) {
            is Int -> value.toLong()
            is Long -> value
            is Number -> runCatching { BigDecimal(value.toString()).longValueExact() }.getOrNull()
            else -> null
        }

        /** The profile of the entry [id], validated like a save on the MCP settings page; null when it is not valid. */
        private fun profileOf(id: String, record: JSONObject): McpServerProfile? {
            if (record.has(RECORD_ID)) return null
            return runCatching {
                val value = JsonParser.parseString(record.toString()).asJsonObject.apply { addProperty(RECORD_ID, id) }
                McpProfileCodec.decode(value)
            }.getOrNull()
        }
    }
}
