package io.github.supermonster003.autojs6.plugin.three.stove.agent.transfer

import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.RiskLevel
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.three.stove.agent.mcp.McpServerProfile
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.stove.agent.transfer.StoveSettingsTransfer.Companion as T
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import org.autojs.plugin.common.api.AutoJs6PluginSettingsTransferContract as Contract

/** The settings document of the settings transfer (roadmap P20, host settings roadmap P18.3). */
class StoveSettingsTransferTest {

    private class MemoryStorage(
        var settings: AgentSettings? = AgentSettings(),
        var appearance: AppearancePreferences = AppearancePreferences(),
        var profiles: List<McpServerProfile>? = emptyList(),
    ) : StoveSettingsStorage {

        val writes = mutableListOf<String>()
        var failing: String? = null
        var mcpBusy = false

        override fun settings(): AgentSettings? = settings
        override fun writeSettings(settings: AgentSettings) = write(T.STORE_SETTINGS) { this.settings = settings }
        override fun appearance(): AppearancePreferences = appearance
        override fun writeAppearance(appearance: AppearancePreferences) = write(T.STORE_APPEARANCE) { this.appearance = appearance }
        override fun mcpProfiles(): List<McpServerProfile>? = profiles

        override fun writeMcpProfiles(profiles: List<McpServerProfile>): Boolean {
            if (mcpBusy) return false
            write(T.STORE_MCP) { this.profiles = profiles }
            return true
        }

        private fun write(store: String, apply: () -> Unit) {
            if (store == failing) throw IOException("cannot write $store")
            writes += store
            apply()
        }
    }

    private fun profile(id: String, token: String? = null) =
        McpServerProfile(id, "Server $id", "https://$id.example.com/mcp", enabled = true, risk = RiskLevel.NORMAL, selectedTools = listOf("search", "fetch"), bearerToken = token)

    private fun stored() = MemoryStorage(
        settings = AgentSettings(
            toolGroups = setOf(ToolGroup.OBSERVE.id, ToolGroup.SHELL.id, ToolGroup.MCP.id),
            budget = mapOf("maxSteps" to 20L, "maxDurationMs" to 600_000L),
            cautious = true, voice = false, floating = true,
            failureAlerts = setOf(AgentSettings.ALERT_DIALOG),
            riskPackages = setOf("com.example.wallet"),
            riskKeywords = setOf("transfer", "转账"),
            completionAlerts = emptySet(),
        ),
        appearance = AppearancePreferences("ja", "dark", AppearancePreferences.parseColor("#3366CC")),
        profiles = listOf(profile("docs", token = "docs-secret"), profile("search")),
    )

    /** `JSONObject.similar` is missing from the `org.json` of `android.jar` that the unit tests compile against. */
    private fun canonical(value: Any?): Any? = when (value) {
        is JSONObject -> value.keys().asSequence().associateWith { canonical(value.get(it)) }.toSortedMap()
        is JSONArray -> (0 until value.length()).map { canonical(value.get(it)) }
        else -> value
    }

    private fun entry(type: String, value: Any): JSONObject = JSONObject().put(Contract.VALUE_KEY_TYPE, type).put(Contract.VALUE_KEY_VALUE, value)

    private fun document(vararg stores: Pair<String, JSONObject>) = JSONObject().apply { stores.forEach { (name, store) -> put(name, store) } }

    @Test
    fun `export types every key and a round trip restores them`() {
        val source = stored()
        val exported = StoveSettingsTransfer(source).exportSettings()
        val settings = exported.settings.getJSONObject(T.STORE_SETTINGS)
        assertEquals(entry(Contract.TYPE_STRING, "cautious").toString(), settings.getJSONObject(T.KEY_ACCESS_MODE).toString())
        assertEquals(Contract.TYPE_STRING_SET, settings.getJSONObject(T.KEY_TOOL_GROUPS).getString(Contract.VALUE_KEY_TYPE))
        assertEquals(listOf("mcp", "observe", "shell"), canonical(settings.getJSONObject(T.KEY_TOOL_GROUPS).get(Contract.VALUE_KEY_VALUE)))
        assertEquals(Contract.TYPE_JSON, settings.getJSONObject(T.KEY_BUDGET).getString(Contract.VALUE_KEY_TYPE))
        assertEquals(20, settings.getJSONObject(T.KEY_BUDGET).getJSONObject(Contract.VALUE_KEY_VALUE).getInt("maxSteps"))
        assertEquals(entry(Contract.TYPE_BOOL, false).toString(), settings.getJSONObject(T.KEY_VOICE).toString())
        assertEquals(9, settings.length())
        val appearance = exported.settings.getJSONObject(T.STORE_APPEARANCE)
        assertEquals(entry(Contract.TYPE_STRING, "#3366CC").toString(), appearance.getJSONObject(T.KEY_COLOR).toString())
        val mcp = exported.settings.getJSONObject(T.STORE_MCP)
        assertEquals(setOf("docs", "search"), mcp.keys().asSequence().toSet())
        val docs = mcp.getJSONObject("docs").getJSONObject(Contract.VALUE_KEY_VALUE)
        assertFalse(docs.has("id"))
        assertEquals("docs-secret", docs.getString("bearerToken"))
        assertEquals("normal", docs.getString("risk"))
        assertFalse(mcp.getJSONObject("search").getJSONObject(Contract.VALUE_KEY_VALUE).has("bearerToken"))
        assertEquals(9 + 3 + 2, exported.entries)

        val target = MemoryStorage()
        val document = JSONObject(exported.settings.toString())
        val counts = StoveSettingsTransfer(target).importSettings(document)
        assertEquals(14, counts.written)
        assertEquals(0, counts.skipped)
        assertEquals(source.settings, target.settings)
        assertEquals(source.appearance, target.appearance)
        assertEquals(source.profiles, target.profiles)
        assertEquals(T.STORES, target.writes)
        assertEquals(canonical(exported.settings), canonical(StoveSettingsTransfer(target).exportSettings().settings))

        // The same document imports again with the same outcome (lenient, idempotent import).
        val again = StoveSettingsTransfer(target).importSettings(document)
        assertEquals(14, again.written)
        assertEquals(source.profiles, target.profiles)
    }

    @Test
    fun `a bearer token makes the export sensitive`() {
        assertTrue(StoveSettingsTransfer(stored()).exportSettings().sensitive)
        val withoutToken = MemoryStorage(profiles = listOf(profile("search")))
        assertFalse(StoveSettingsTransfer(withoutToken).exportSettings().sensitive)
        val empty = StoveSettingsTransfer(MemoryStorage()).exportSettings()
        assertFalse(empty.sensitive)
        assertFalse(empty.settings.has(T.STORE_MCP))
        assertEquals(setOf(T.STORE_SETTINGS, T.STORE_APPEARANCE), empty.settings.keys().asSequence().toSet())
        val unavailable = StoveSettingsTransfer(MemoryStorage(settings = null, profiles = null)).exportSettings()
        assertEquals(setOf(T.STORE_APPEARANCE), unavailable.settings.keys().asSequence().toSet())
        assertEquals(3, unavailable.entries)
        assertFalse(unavailable.sensitive)
    }

    @Test
    fun `full access never comes from an import`() {
        val target = MemoryStorage()
        val full = document(T.STORE_SETTINGS to JSONObject().put(T.KEY_ACCESS_MODE, entry(Contract.TYPE_STRING, "full")))
        val counts = StoveSettingsTransfer(target).importSettings(full)
        assertEquals(0, counts.written)
        assertEquals(1, counts.skipped)
        assertFalse(target.settings!!.fullAccess)
        assertTrue(target.writes.isEmpty())

        // Lowering the mode is an ordinary choice, so a device with full access follows the document.
        val device = MemoryStorage(settings = AgentSettings(fullAccess = true))
        val standard = document(T.STORE_SETTINGS to JSONObject().put(T.KEY_ACCESS_MODE, entry(Contract.TYPE_STRING, "standard")))
        assertEquals(1, StoveSettingsTransfer(device).importSettings(standard).written)
        assertEquals("standard", device.settings!!.accessMode)
        val keep = MemoryStorage(settings = AgentSettings(fullAccess = true))
        StoveSettingsTransfer(keep).importSettings(full)
        assertEquals("full", keep.settings!!.accessMode)
    }

    @Test
    fun `unknown stores, keys, other tags and invalid values are skipped`() {
        val target = MemoryStorage()
        val settings = JSONObject()
            .put(T.KEY_VOICE, entry(Contract.TYPE_STRING, "yes"))
            .put(T.KEY_FLOATING, "true")
            .put(T.KEY_FAILURE_ALERTS, entry(Contract.TYPE_STRING_SET, JSONArray().put("notification").put("email")))
            .put(T.KEY_COMPLETION_ALERTS, entry(Contract.TYPE_STRING_SET, JSONArray().put("toast")))
            .put(T.KEY_RISK_PACKAGES, entry(Contract.TYPE_STRING_SET, JSONArray().put("not a package")))
            .put(T.KEY_RISK_KEYWORDS, entry(Contract.TYPE_STRING_SET, JSONArray((1..33).map { "word$it" })))
            .put(T.KEY_BUDGET, entry(Contract.TYPE_JSON, JSONObject().put("maxSteps", 10).put("maxCoffee", 1)))
            .put(T.KEY_TOOL_GROUPS, entry(Contract.TYPE_STRING_SET, JSONArray().put("observe").put("teleport")))
            .put("theme", entry(Contract.TYPE_STRING, "blue"))
        val appearance = JSONObject()
            .put(T.KEY_LANGUAGE, entry(Contract.TYPE_STRING, "tlh"))
            .put(T.KEY_DARK_MODE, entry(Contract.TYPE_STRING, "light"))
            .put(T.KEY_COLOR, entry(Contract.TYPE_STRING, "#12345"))
        val mcp = JSONObject()
            .put("Bad Id", entry(Contract.TYPE_JSON, JSONObject().put("name", "x").put("endpoint", "https://x.example.com/mcp")))
            .put("plain", entry(Contract.TYPE_JSON, JSONObject().put("name", "Plain").put("endpoint", "http://example.com/mcp")))
            .put("withid", entry(Contract.TYPE_JSON, JSONObject().put("id", "withid").put("name", "W").put("endpoint", "https://w.example.com/mcp")))
            .put("good", entry(Contract.TYPE_JSON, JSONObject().put("name", "Good").put("endpoint", "https://good.example.com/mcp")))
        val document = document(T.STORE_SETTINGS to settings, T.STORE_APPEARANCE to appearance, T.STORE_MCP to mcp,
            "prompts" to JSONObject().put("a", 1).put("b", 2))
        document.put("broken", "not an object")
        val counts = StoveSettingsTransfer(target).importSettings(document)
        // settings: completionAlerts, toolGroups (unknown id dropped); appearance: darkMode; mcp: good.
        assertEquals(4, counts.written)
        // settings 7 + appearance 2 + mcp 3 + prompts 2 + broken 1.
        assertEquals(15, counts.skipped)
        assertEquals(setOf("observe"), target.settings!!.toolGroups)
        assertEquals(setOf("toast"), target.settings!!.completionAlerts)
        assertEquals(AgentSettings().voice, target.settings!!.voice)
        assertEquals(AgentSettings().budget, target.settings!!.budget)
        assertEquals(AppearancePreferences(darkMode = "light"), target.appearance)
        assertEquals(listOf("good"), target.profiles!!.map { it.id })
        assertNull(target.profiles!!.single().bearerToken)
    }

    @Test
    fun `budget and color accept every value the settings page saves`() {
        val target = MemoryStorage(appearance = AppearancePreferences(color = AppearancePreferences.parseColor("#000000")))
        val document = document(
            T.STORE_SETTINGS to JSONObject().put(T.KEY_BUDGET, entry(Contract.TYPE_JSON, JSONObject().put("maxSteps", 2.0).put("maxTotalTokens", 5000))),
            T.STORE_APPEARANCE to JSONObject().put(T.KEY_COLOR, entry(Contract.TYPE_STRING, T.COLOR_HOST)),
        )
        assertEquals(2, StoveSettingsTransfer(target).importSettings(document).written)
        assertEquals(mapOf("maxSteps" to 2L, "maxTotalTokens" to 5000L), target.settings!!.budget)
        assertNull(target.appearance.color)
        val fraction = document(T.STORE_SETTINGS to JSONObject().put(T.KEY_BUDGET, entry(Contract.TYPE_JSON, JSONObject().put("maxSteps", 2.5))))
        assertEquals(1, StoveSettingsTransfer(target).importSettings(fraction).skipped)
        val zero = document(T.STORE_SETTINGS to JSONObject().put(T.KEY_BUDGET, entry(Contract.TYPE_JSON, JSONObject().put("maxSteps", 0))))
        assertEquals(1, StoveSettingsTransfer(target).importSettings(zero).skipped)
        val empty = document(T.STORE_SETTINGS to JSONObject().put(T.KEY_BUDGET, entry(Contract.TYPE_JSON, JSONObject())))
        assertEquals(1, StoveSettingsTransfer(target).importSettings(empty).written)
        assertEquals(emptyMap<String, Long>(), target.settings!!.budget)
    }

    @Test
    fun `imported profiles join the others and respect the limit`() {
        val existing = (1..7).map { profile("s$it") } + profile("docs", token = "old-secret")
        val target = MemoryStorage(profiles = existing)
        val mcp = JSONObject()
            .put("docs", entry(Contract.TYPE_JSON, JSONObject().put("name", "Docs").put("endpoint", "https://docs.example.com/mcp").put("bearerToken", "new-secret")))
            .put("extra", entry(Contract.TYPE_JSON, JSONObject().put("name", "Extra").put("endpoint", "https://extra.example.com/mcp")))
        val counts = StoveSettingsTransfer(target).importSettings(document(T.STORE_MCP to mcp))
        assertEquals(1, counts.written)
        assertEquals(1, counts.skipped)
        assertEquals(existing.map { it.id }, target.profiles!!.map { it.id })
        assertEquals("new-secret", target.profiles!!.single { it.id == "docs" }.bearerToken)
        assertEquals(RiskLevel.SENSITIVE, target.profiles!!.single { it.id == "docs" }.risk)
    }

    @Test
    fun `a busy or unavailable store skips its entries and a failed write fails the import`() {
        val busy = stored().apply { mcpBusy = true }
        val document = StoveSettingsTransfer(stored()).exportSettings().settings
        val counts = StoveSettingsTransfer(busy).importSettings(JSONObject(document.toString()))
        assertEquals(12, counts.written)
        assertEquals(2, counts.skipped)
        assertEquals(listOf(T.STORE_SETTINGS, T.STORE_APPEARANCE), busy.writes)

        val unavailable = MemoryStorage(settings = null, profiles = null)
        val skipped = StoveSettingsTransfer(unavailable).importSettings(JSONObject(document.toString()))
        assertEquals(3, skipped.written)
        assertEquals(11, skipped.skipped)
        assertEquals(listOf(T.STORE_APPEARANCE), unavailable.writes)

        val failing = MemoryStorage().apply { failing = T.STORE_APPEARANCE }
        assertThrows(IOException::class.java) { StoveSettingsTransfer(failing).importSettings(JSONObject(document.toString())) }
        assertEquals(listOf(T.STORE_SETTINGS), failing.writes)
    }
}
