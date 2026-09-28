package io.github.supermonster003.autojs6.plugin.three.stove.agent.store

import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsCodecTest {
    @Test fun fullAccessRequiresExplicitPrivateSettingsAndPreservesToolAndBudgetLimits() {
        val chosen = AgentSettings(fullAccess = true, toolGroups = setOf("observe"), budget = mapOf("maxSteps" to 7))
        assertEquals(chosen, SettingsCodec.decode(SettingsCodec.encode(chosen)))
        val legacy = SettingsCodec.json(AgentSettings(cautious = true)).apply {
            addProperty("version", 2); remove("fullAccess"); remove("failureAlerts"); remove("riskPackages"); remove("riskKeywords")
        }
        assertFalse(SettingsCodec.decode(legacy.toString()).fullAccess)
        reject { SettingsCodec.decode(SettingsCodec.json(chosen).apply { addProperty("cautious", true) }.toString()) }
        reject { SettingsCodec.decode(SettingsCodec.json(chosen).apply { addProperty("fullAccess", "true") }.toString()) }
        val host = LinkConfiguration.parse("{}")
        val preset = PresetSnapshot("default", listOf(Preset("default", confirmPolicy = "cautious")))
        val request = StartRequest.parse("""{"goal":"fixture"}""", host, preset, chosen)
        assertEquals(ConfirmationMode.FULL_ACCESS, request.options.confirmationMode)
        assertEquals(setOf("observe"), request.groups); assertEquals(7, request.options.limits.maxSteps)
        // A caller can still narrow its own run; it can never widen a run to full access.
        assertEquals(ConfirmationMode.CAUTIOUS, StartRequest.parse("""{"goal":"fixture","options":{"confirm":"cautious"}}""", host, preset, chosen).options.confirmationMode)
        assertEquals(ConfirmationMode.FULL_ACCESS, StartRequest.parse("""{"goal":"fixture","options":{"confirm":"default"}}""", host, preset, chosen).options.confirmationMode)
        assertEquals(ConfirmationMode.CAUTIOUS, StartRequest.parse("""{"goal":"fixture"}""", host, preset, chosen.copy(fullAccess = false)).options.confirmationMode)
        reject { StartRequest.parse("""{"goal":"fixture","options":{"confirm":"full_access"}}""", host) }
        reject { StartRequest.parse("""{"goal":"fixture","options":{"fullAccess":true}}""", host) }
        reject { StartRequest.parse("""{"goal":"fixture","options":{"tools":["shell"]}}""", host, settings = chosen) }
    }
    @get:Rule val temp = TemporaryFolder()
    private fun reject(block: () -> Unit) { assertThrows(IllegalArgumentException::class.java, block) }
    @Test fun defaultsAndRoundTripPreserveExplicitAuthority() {
        val defaults = AgentSettings()
        assertFalse(defaults.toolGroups.any { it in setOf("files", "shell", "gesture") }); assertTrue("ocr" in defaults.toolGroups)
        val chosen = defaults.copy(toolGroups = setOf("files", "shell", "gesture"), cautious = true, voice = false,
            budget = mapOf("maxSteps" to 200, "maxDurationMs" to 3600000))
        assertEquals(chosen, SettingsCodec.decode(SettingsCodec.encode(chosen)))
    }
    @Test fun futureVersionsUnknownKeysWrongTypesAndOverBudgetFailClosed() {
        val valid = SettingsCodec.json(AgentSettings())
        for ((key, value) in listOf("version" to 6.json(), "extra" to true.json(), "voice" to "true".json(), "floating" to "true".json(),
            "failureAlerts" to AgentJson.parse("[\"email\"]"), "failureAlerts" to AgentJson.parse("[\"toast\",\"toast\"]"),
            "riskPackages" to AgentJson.parse("[\"nodots\"]"), "riskPackages" to AgentJson.parse("[\"a.b\",\"a.b\"]"), "riskPackages" to "a.b".json(),
            "riskKeywords" to AgentJson.parse("[\" padded\"]"), "riskKeywords" to AgentJson.parse("[\"\"]"), "riskKeywords" to AgentJson.parse("[\"${"x".repeat(33)}\"]")))
            reject { SettingsCodec.decode(valid.deepCopy().apply { add(key, value) }.toString()) }
        for (budget in listOf("""{"maxSteps":201}""", """{"maxSteps":1.5}""", """{"maxSteps":0}""", """{"unknown":1}"""))
            reject { SettingsCodec.decode(valid.deepCopy().apply { add("budget", AgentJson.parse(budget)) }.toString()) }
        reject { SettingsCodec.decode(valid.deepCopy().apply { add("toolGroups", AgentJson.parse("[\"shell\",\"shell\"]")) }.toString()) }
        reject { SettingsCodec.decode(" ".repeat(SettingsCodec.MAX_BYTES + 1)) }
    }
    @Test fun riskListsWidenPackagedTablesRoundTripAndStayBounded() {
        val chosen = AgentSettings(riskPackages = setOf("com.example.pay", "org.bank.app_2"), riskKeywords = setOf("remit", "汇款", "Wire Money"))
        assertEquals(chosen, SettingsCodec.decode(SettingsCodec.encode(chosen)))
        val encoded = SettingsCodec.json(chosen)
        assertEquals(5L, encoded.number("version")); assertEquals(listOf("com.example.pay", "org.bank.app_2"), encoded.getAsJsonArray("riskPackages").map { it.asString })
        // Version 4 files carry no lists; decoding them never invents entries.
        val legacy = encoded.deepCopy().apply { addProperty("version", 4); remove("riskPackages"); remove("riskKeywords") }
        assertEquals(chosen.copy(riskPackages = emptySet(), riskKeywords = emptySet()), SettingsCodec.decode(legacy.toString()))
        reject { SettingsCodec.decode(legacy.deepCopy().apply { add("riskPackages", AgentJson.parse("[\"a.b\"]")) }.toString()) }
        reject { AgentSettings(riskPackages = setOf("bad name")) }
        reject { AgentSettings(riskPackages = setOf("com.example.pay" + ".x".repeat(64))) }
        reject { AgentSettings(riskKeywords = setOf("two\nlines")) }
        reject { AgentSettings(riskPackages = (1..33).map { "com.example.pay$it" }.toSet()) }
        val full = AgentSettings(riskPackages = (1..32).map { "com.example.pay$it" }.toSet(), riskKeywords = (1..32).map { "keyword-$it" }.toSet())
        assertEquals(full, SettingsCodec.decode(SettingsCodec.encode(full)))
    }
    @Test fun oldSettingsMigrateWithoutEnablingAnOverlayOrChangingAuthority() {
        val chosen = AgentSettings(cautious = true, voice = false, toolGroups = setOf("observe"), budget = mapOf("maxSteps" to 7))
        val legacy = SettingsCodec.json(chosen).apply {
            addProperty("version", 1); remove("floating"); remove("fullAccess"); remove("failureAlerts"); remove("riskPackages"); remove("riskKeywords")
        }
        assertEquals(chosen, SettingsCodec.decode(legacy.toString()))
        assertFalse(SettingsCodec.decode(legacy.toString()).floating)
        assertEquals(setOf(AgentSettings.ALERT_NOTIFICATION), SettingsCodec.decode(legacy.toString()).failureAlerts)
        val alerted = chosen.copy(failureAlerts = setOf(AgentSettings.ALERT_TOAST, AgentSettings.ALERT_DIALOG))
        assertEquals(alerted, SettingsCodec.decode(SettingsCodec.encode(alerted)))
        assertEquals(emptySet<String>(), SettingsCodec.decode(SettingsCodec.encode(chosen.copy(failureAlerts = emptySet()))).failureAlerts)
        reject { AgentSettings(failureAlerts = setOf("email")) }
        val enabled = chosen.copy(floating = true)
        assertEquals(enabled, SettingsCodec.decode(SettingsCodec.encode(enabled)))
        reject { SettingsCodec.decode(legacy.deepCopy().apply { addProperty("floating", true) }.toString()) }
        reject { SettingsCodec.decode(SettingsCodec.json(chosen).apply { remove("floating") }.toString()) }
    }
    @Test fun settingsPersistAndRecoverBackupsWithoutReplacingCorruptFiles() {
        val file = File(temp.newFolder(), "settings.json"); val store = SettingsStore(file)
        assertEquals(AgentSettings(), store.open())
        val chosen = AgentSettings(cautious = true, voice = false)
        store.save(chosen); assertEquals(chosen, SettingsStore(file).open())
        File(file.path + ".bak").writeText(SettingsCodec.encode(chosen)); file.writeText("interrupted")
        assertEquals(chosen, SettingsStore(file).open()); assertFalse(File(file.path + ".bak").exists())
        file.writeText("{\"version\":999}"); reject { SettingsStore(file).open() }
        assertEquals("{\"version\":999}", file.readText())
    }
    @Test fun localGroupsCanEnableCatalogToolsButNeverOverrideExplicitHostRestrictions() {
        val settings = AgentSettings(toolGroups = setOf("gesture", "files", "shell"))
        fun start(config: String, options: String = "{}") = StartRequest.parse("""{"goal":"fixture","options":$options}""",
            LinkConfiguration.parse(config), settings = settings)
        assertEquals(settings.toolGroups, start("{}").groups)
        assertEquals(setOf("files"), start("""{"grantSummary":{"toolGroups":["observe","files"]}}""").groups)
        reject { start("""{"grantSummary":{"toolGroups":["observe"]}}""", """{"tools":["shell"]}""") }
        reject { start("{}", """{"tools":["observe"]}""") }
        assertEquals(setOf("files", "shell"), start("{}", """{"tools":{"disable":["gesture"]}}""").groups)
    }
    @Test fun budgetsRespectOwnershipHostPresetAndPerTaskCeilings() {
        val settings = AgentSettings(budget = SettingsCodec.ceilings, cautious = true)
        val host = LinkConfiguration.parse("""{"grantSummary":{"maxTotalTokens":12345}}""")
        fun start(options: String = "{}", presets: PresetSnapshot = PresetSnapshot.INITIAL) =
            StartRequest.parse("""{"goal":"fixture","options":$options}""", host, presets, settings)
        assertEquals(200, start().options.limits.maxSteps)
        assertEquals(RunLimits.DURATION_MS, start().options.limits.maxDurationMs)
        assertEquals(12345L, start().options.limits.maxTotalTokens)
        assertEquals(RunLimits.DETACHED_DURATION_MS, start("""{"detached":true}""").options.limits.maxDurationMs)
        assertEquals(ConfirmationMode.CAUTIOUS, start().options.confirmationMode)
        reject { start("""{"confirm":"default"}""") }
        reject { start("""{"budget":{"maxTotalTokens":12346}}""") }
        val presets = PresetSnapshot("default", listOf(Preset("default", budget = mapOf("maxSteps" to 2))))
        assertEquals(2, start(presets = presets).options.limits.maxSteps)
        reject { start("""{"budget":{"maxSteps":3}}""", presets) }
    }
    @Test fun clearingStoresPersistsEmptyMemoryAndOnlyTheBuiltInPreset() {
        val file = File(temp.newFolder(), "presets.json"); val presets = PresetStore(file); presets.open()
        presets.save(Preset("custom"), true, emptySet(), emptySet()); presets.setDefault("custom")
        assertTrue(presets.bytes() > 0); presets.clear()
        assertEquals(listOf("default"), PresetStore(file).open().presets.map { it.name })
        val dir = temp.newFolder(); val memory = MemoryStore(dir); memory.open()
        memory.put(MemoryEntry("destination", "Office fixture", "global", "00000000-0000-0000-0000-000000000001", 1, 1), null)
        assertTrue(memory.bytes() > 0); memory.clear()
        assertEquals(0L, memory.bytes()); assertTrue(MemoryStore(dir).open().isEmpty())
    }
    @Test fun presetBudgetsAboveStockDefaultsStillNarrowGlobalLimits() {
        val preset = PresetCodec.decodePreset(AgentJson.objectOf("""{"name":"default","budget":{"maxSteps":100}}"""))
        val presets = PresetSnapshot("default", listOf(preset))
        fun start(maximum: Long) = StartRequest.parse("""{"goal":"fixture"}""", LinkConfiguration.parse("{}"),
            presets, AgentSettings(budget = mapOf("maxSteps" to maximum)))
        assertEquals(100, start(200).options.limits.maxSteps)
        assertEquals(7, start(7).options.limits.maxSteps)
    }
    @Test fun removingAnExplicitHostGroupRestrictionIsNotANarrowingUpdate() {
        val previous = LinkConfiguration.parse("""{"grantSummary":{"toolGroups":["observe","act","ocr","script","memory","user"]}}""")
        val unrestricted = LinkConfiguration.parse("{}")
        assertEquals(previous.groups, unrestricted.groups)
        assertFalse(unrestricted.narrows(previous))
        assertTrue(previous.narrows(unrestricted))
    }
}
