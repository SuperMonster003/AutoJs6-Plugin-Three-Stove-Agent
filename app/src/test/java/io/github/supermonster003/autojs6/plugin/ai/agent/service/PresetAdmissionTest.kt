package io.github.supermonster003.autojs6.plugin.ai.agent.service

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import org.junit.Assert.*
import org.junit.Test

class PresetAdmissionTest {
    private val config = LinkConfiguration.parse("""{"scriptRoots":["/sdcard/a","/sdcard/b"],"grantSummary":{"toolGroups":["observe","act","memory"],"maxTotalTokens":10000}}""")
    private val office = Preset("office", "profile:online", setOf("observe", "memory"), mapOf("maxSteps" to 5, "maxTotalTokens" to 8000),
        "cautious", "Fixed", setOf("/sdcard/a"), "global_and_preset")
    private fun request(options: String = "{}", preset: Preset = office): StartRequest = StartRequest.parse("""{"goal":"test","options":$options}""", config,
        PresetSnapshot(preset.name, listOf(Preset("default"), preset)))
    @Test fun omittedPresetUsesSelectedDefaultAndExplicitDefaultRemainsAvailable() {
        val admitted = request()
        assertEquals("office", admitted.preset); assertEquals("profile:online", admitted.target)
        assertEquals(5, admitted.options.limits.maxSteps); assertEquals(8000, admitted.options.limits.maxTotalTokens)
        assertEquals(setOf("/sdcard/a"), admitted.scriptRoots); assertEquals(ConfirmationMode.CAUTIOUS, admitted.options.confirmationMode)
        assertEquals("default", request("""{"preset":"default"}""").preset)
        assertThrows(IllegalArgumentException::class.java) { request("""{"preset":"removed"}""") }
    }
    @Test fun taskOverridesCannotWidenPresetGroupsRootsBudgetsOrConfirmation() {
        for (options in listOf("""{"tools":["act"]}""", """{"tools":{"enable":["act"]}}""", """{"budget":{"maxSteps":6}}""",
            """{"budget":{"maxTotalTokens":8001}}""", """{"scriptRoots":["/sdcard/b"]}""", """{"confirm":"default"}"""))
            assertThrows(options, IllegalArgumentException::class.java) { request(options) }
        val narrowed = request("""{"tools":[],"scriptRoots":[],"budget":{"maxSteps":2},"memory":false}""")
        assertTrue(narrowed.groups.isEmpty()); assertTrue(narrowed.scriptRoots.isEmpty()); assertFalse(narrowed.memory)
        assertEquals(2, narrowed.options.limits.maxSteps)
    }
    @Test fun laterGlobalRevocationsIntersectStoredPresetAndCannotBeRestoredByTaskOptions() {
        val expanded = office.copy(toolGroups = setOf("observe", "gesture", "shell"), scriptRoots = setOf("/sdcard/a", "/removed"))
        assertEquals(setOf("observe"), request(preset = expanded).groups)
        assertEquals(setOf("/sdcard/a"), request(preset = expanded).scriptRoots)
        assertThrows(IllegalArgumentException::class.java) { request("""{"tools":["shell"]}""", expanded) }
        assertThrows(IllegalArgumentException::class.java) { request("""{"scriptRoots":["/removed"]}""", expanded) }
    }
    @Test fun fixedContextSurvivesPerTaskTextAndParametersAndCombinedSizeIsBounded() {
        val admitted = request("""{"context":"Task text","parameters":{"key":"value"}}""")
        val context = AgentJson.objectOf(admitted.context)
        assertEquals("Fixed\n\nTask text", context.string("context")); assertEquals("value", context.getAsJsonObject("parameters").string("key"))
        assertEquals("Fixed", request("""{"context":""}""").context)
        assertThrows(IllegalArgumentException::class.java) { request("""{"context":"${"a".repeat(8192)}"}""") }
    }
    @Test fun presetDurationIsClampedByTaskOwnershipAndHostTokenGrant() {
        val preset = office.copy(budget = mapOf("maxDurationMs" to 1800000, "maxTotalTokens" to 300000))
        assertEquals(600000, request(preset = preset).options.limits.maxDurationMs)
        assertEquals(1800000, request("""{"detached":true}""", preset).options.limits.maxDurationMs)
        assertEquals(10000, request(preset = preset).options.limits.maxTotalTokens)
    }
    @Test fun sensitiveAndPaymentConfirmationCannotBeDisabledByEitherPresetPolicy() {
        val catalog = F.catalog(); val tool = catalog["ui_click"]!!
        val policy = ToolPolicy()
        for (mode in listOf("default", "cautious")) {
            val gate = ConfirmationGate(policy, request(preset = office.copy(confirmPolicy = mode)).options.confirmationMode)
            val payment = gate.assess(tool, ToolMetadata(payment = true))
            assertEquals(RiskLevel.SENSITIVE, payment.risk); assertTrue(payment.required); assertFalse(payment.allowRunScope)
            assertFalse(gate.allow(payment, ConfirmationScope.RUN)); assertTrue(gate.assess(tool, ToolMetadata(payment = true)).required)
            assertTrue(gate.assess(catalog["script_run"]!!, ToolMetadata(context = RiskContext(registeredScriptRisk = RiskLevel.SENSITIVE))).required)
        }
    }
    @Test fun memoryOptOutCannotBeReenabledAndScopesOnlyReduceVisibleMemory() {
        assertFalse(request("""{"memory":true}""", office.copy(memoryScope = "none")).memory)
        val entries = listOf("global", "office", "home").mapIndexed { index, scope -> jsonObject("key" to "k$index".json(), "value" to scope.json(),
            "scope" to scope.json(), "sourceRunId" to "00000000-0000-0000-0000-000000000001".json(), "createdAt" to 0.json(), "updatedAt" to 1.json()) }
        val data = jsonObject("version" to 1.json(), "entries" to jsonArray(*entries.toTypedArray())).toString()
        assertEquals(listOf("global"), MemoryContext.decode(data, "office", true, false).entries.map { it.asJsonObject.string("scope") })
        assertEquals(listOf("office"), MemoryContext.decode(data, "office", false, true).entries.map { it.asJsonObject.string("scope") })
        assertEquals(0, MemoryContext.decode(data, "office", false, false).entries.size())
    }
    @Test fun publicAndUiLaunchersUseIdenticalPresetAdmission() {
        val snapshot = PresetSnapshot("office", listOf(Preset("default"), office))
        val ui = RunLauncher.start(C.LINK_STATE_ATTACHED, config, RunLauncher.uiRequest("test", "office", "en"), snapshot) { it }
        val script = request()
        assertEquals(script.options.limits, ui.options.limits); assertEquals(script.context, ui.context); assertEquals(script.groups, ui.groups)
    }
}
