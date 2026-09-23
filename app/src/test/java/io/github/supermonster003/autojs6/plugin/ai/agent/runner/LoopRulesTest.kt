package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class LoopRulesTest {
    private val catalog = F.catalog()
    private fun admit(rules: LoopRules, name: String = "ui_click", args: String = "{\"nodeRef\":\"#n1\"}", metadata: ToolMetadata = ToolMetadata()): Boolean {
        val input = AgentJson.objectOf(args)
        return rules.admit(catalog[name]!!, PreparedTool(ToolInvocation(name, input, ToolHandlers(catalog).prepare(name, input, F.policy())), metadata))
    }
    private fun sample(changed: Boolean = false, partial: Boolean = false, baseline: Boolean = false, observed: Boolean = true) =
        jsonObject("windowChanged" to false.json(), "changes" to jsonObject("changed" to changed.json(), "partial" to partial.json(), "baseline" to baseline.json()),
            "stability" to jsonObject("observed" to observed.json()))

    @Test fun thirdEquivalentActionIsBlockedAcrossReadsAndJsonKeyOrder() {
        val rules = LoopRules()
        assertTrue(admit(rules, args = """{"selector":{"text":"Go","depth":0}}"""))
        repeat(4) { assertTrue(admit(rules, "ui_dump", "{}")) }
        assertTrue(admit(rules, args = """{"selector":{"depth":0.0,"text":"Go"}}"""))
        assertFalse(admit(rules, args = """{"selector":{"text":"Go","depth":0}}"""))
        assertEquals(3L, rules.guidance().number("repeatedActionCount"))
    }
    @Test fun nodeReferenceOrdinalsAndTransportSnapshotsCannotResetEquivalentActions() {
        val rules = LoopRules()
        val metadata = ToolMetadata(actionIdentity = "stable-node-identity")
        assertTrue(admit(rules, args = """{"nodeRef":"#n1","snapshotId":"first"}""", metadata = metadata))
        assertTrue(admit(rules, args = """{"nodeRef":"#n9","snapshotId":"second"}""", metadata = metadata))
        assertFalse(admit(rules, args = """{"nodeRef":"#n4","snapshotId":"third"}""", metadata = metadata))
        assertFalse(rules.guidance().toString().contains("stable-node-identity"))
    }
    @Test fun newTargetPackageToolOrParameterStartsANewStreak() {
        val rules = LoopRules()
        repeat(2) { assertTrue(admit(rules, metadata = ToolMetadata(actionIdentity = "one"))) }
        assertTrue(admit(rules, metadata = ToolMetadata(actionIdentity = "two")))
        assertTrue(admit(rules, metadata = ToolMetadata(RiskContext(packageName = "different"), actionIdentity = "two")))
        assertTrue(admit(rules, "ui_long_click"))
        repeat(2) { assertTrue(admit(rules, "ui_set_text", """{"nodeRef":"#n1","text":"A"}""")) }
        assertTrue(admit(rules, "ui_set_text", """{"nodeRef":"#n1","text":"B"}"""))
    }
    @Test fun readOnlyRegisteredScriptsDoNotEraseOrAccumulateActionRepetitions() {
        val rules = LoopRules()
        repeat(2) { assertTrue(admit(rules)) }
        repeat(4) { assertTrue(admit(rules, "script_run", """{"id":"read","parameters":{}}""", ToolMetadata(RiskContext(registeredScriptRisk = RiskLevel.READ_ONLY)))) }
        assertFalse(admit(rules))
    }
    @Test fun threeCompleteUnchangedActionReadbacksRequestADifferentStrategy() {
        val rules = LoopRules(); val action = catalog["ui_click"]!!
        repeat(2) { rules.succeeded(action, sample()); assertEquals(false, rules.guidance().flag("changeStrategy")) }
        rules.succeeded(catalog["ui_dump"]!!, jsonObject("changes" to sample().getAsJsonObject("changes")))
        rules.succeeded(action, sample())
        assertEquals(true, rules.guidance().flag("changeStrategy"))
        assertEquals(3L, rules.guidance().number("unchangedActions"))
        assertEquals(false, rules.guidance().flag("observeRequired"))
    }
    @Test fun contentProgressUnknownPartialBaselineAndFailureBreakUnchangedStreaks() {
        for (value in listOf(sample(changed = true), sample(partial = true), sample(baseline = true), JsonNull.INSTANCE)) {
            val rules = LoopRules(); val action = catalog["ui_click"]!!
            repeat(2) { rules.succeeded(action, sample()) }
            rules.succeeded(action, value)
            assertEquals(0L, rules.guidance().number("unchangedActions"))
        }
        val rules = LoopRules(); val action = catalog["ui_click"]!!
        repeat(3) { rules.succeeded(action, sample()) }; rules.failed(action)
        assertEquals(false, rules.guidance().flag("changeStrategy"))
        repeat(3) { rules.succeeded(action, sample()) }
        rules.succeeded(catalog["ui_dump"]!!, jsonObject("changes" to sample(changed = true).getAsJsonObject("changes")))
        assertEquals(false, rules.guidance().flag("changeStrategy"))
    }
    @Test fun missingActionReadbackRequestsObservationUntilAnObservationArrives() {
        val rules = LoopRules(); val action = catalog["ui_click"]!!
        rules.started(action); assertEquals(true, rules.guidance().flag("observeRequired"))
        rules.succeeded(action, sample(observed = false)); assertEquals(true, rules.guidance().flag("observeRequired"))
        rules.succeeded(catalog["report_progress"]!!, true.json()); assertEquals(true, rules.guidance().flag("observeRequired"))
        rules.succeeded(catalog["ui_dump"]!!, JsonObject()); assertEquals(false, rules.guidance().flag("observeRequired"))
    }
    @Test fun clipboardReadDoesNotSatisfyMissingScreenObservation() {
        val rules = LoopRules()
        rules.started(catalog["ui_click"]!!)
        rules.succeeded(catalog["clipboard_get"]!!, "some text".json())
        assertEquals(true, rules.guidance().flag("observeRequired"))
    }
}
