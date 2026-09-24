package io.github.supermonster003.autojs6.plugin.ai.agent.store

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.service.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsCodecTest {
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
        for ((key, value) in listOf("version" to 3.json(), "extra" to true.json(), "voice" to "true".json(), "floating" to "true".json()))
            reject { SettingsCodec.decode(valid.deepCopy().apply { add(key, value) }.toString()) }
        for (budget in listOf("""{"maxSteps":201}""", """{"maxSteps":1.5}""", """{"maxSteps":0}""", """{"unknown":1}"""))
            reject { SettingsCodec.decode(valid.deepCopy().apply { add("budget", AgentJson.parse(budget)) }.toString()) }
        reject { SettingsCodec.decode(valid.deepCopy().apply { add("toolGroups", AgentJson.parse("[\"shell\",\"shell\"]")) }.toString()) }
        reject { SettingsCodec.decode(" ".repeat(4097)) }
    }
    @Test fun oldSettingsMigrateWithoutEnablingAnOverlayOrChangingAuthority() {
        val chosen = AgentSettings(cautious = true, voice = false, toolGroups = setOf("observe"), budget = mapOf("maxSteps" to 7))
        val legacy = SettingsCodec.json(chosen).apply { addProperty("version", 1); remove("floating") }
        assertEquals(chosen, SettingsCodec.decode(legacy.toString()))
        assertFalse(SettingsCodec.decode(legacy.toString()).floating)
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
