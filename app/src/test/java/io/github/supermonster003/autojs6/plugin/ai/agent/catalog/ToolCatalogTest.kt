package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class ToolCatalogTest {
    private val catalog = F.catalog()
    private val handler = ToolHandlers(catalog)

    @Test fun catalogAndKeywordSnapshotsFreezeTheApprovedSurface() {
        val snapshot = JsonArray().apply { catalog.tools.forEach { add(it.snapshot()) } }
        assertEquals(AgentJson.parse(F.snapshot("tool-catalog.snapshot.json"), 256 * 1024), snapshot)
        assertEquals(30, catalog.tools.size)
        assertNull(catalog["ask_user"])
        assertNull(catalog["script_run_source"])
        assertNull(catalog["screen_capture"])
        assertEquals(AgentJson.parse(F.snapshot("sensitive-keywords.snapshot.json")), AgentJson.parse(F.asset("catalog/sensitive-keywords.json")))
        assertEquals(10, AgentJson.objectOf(F.asset("catalog/sensitive-keywords.json")).size())
    }

    @Test fun disabledGroupsAreHiddenAndRejectedBeforeArgumentValidation() {
        val policy = ToolPolicy()
        val visible = AgentJson.parse(catalog.render(policy)).asJsonArray.map { it.asJsonObject.string("name") }
        for (name in listOf("files_read", "files_write", "shell_exec", "ui_gesture", "ui_click_xy", "ocr_screen")) {
            assertFalse(name, name in visible)
            F.fails("TOOL_DISABLED") { handler.prepare(name, JsonObject(), policy) }
        }
        F.fails("TOOL_UNKNOWN") { handler.prepare("script_run_source", JsonObject(), F.policy()) }
        assertTrue(AgentJson.parse(catalog.render(F.policy())).asJsonArray.size() == 30)
    }

    @Test fun riskOverridesCannotLowerSensitiveToolsOrRegisteredScriptRisk() {
        val policy = ToolPolicy(riskOverrides = mapOf("files_write" to RiskLevel.READ_ONLY, "script_run" to RiskLevel.READ_ONLY, "clipboard_get" to RiskLevel.SENSITIVE))
        assertEquals(RiskLevel.SENSITIVE, policy.risk(catalog["files_write"]!!))
        assertEquals(RiskLevel.SENSITIVE, policy.risk(catalog["script_run"]!!, RiskContext(registeredScriptRisk = RiskLevel.SENSITIVE)))
        assertEquals(RiskLevel.SENSITIVE, policy.risk(catalog["clipboard_get"]!!))
        assertEquals(RiskLevel.NORMAL, policy.risk(catalog["script_run"]!!))
    }

    @Test fun observedSensitiveTextAndPaymentPackagesPromoteActionsInTenLanguages() {
        val keywords = AgentJson.objectOf(F.asset("catalog/sensitive-keywords.json"))
        val policy = ToolPolicy(keywords = ToolPolicy.readKeywords(keywords.toString()), paymentPackages = setOf("test.pay"))
        keywords.entrySet().forEach { (_, list) ->
            for (word in list.asJsonArray) assertEquals(word.asString, RiskLevel.SENSITIVE,
                policy.risk(catalog["ui_click"]!!, RiskContext(nodeDescription = word.asString)))
        }
        assertEquals(RiskLevel.SENSITIVE, policy.risk(catalog["ui_set_text"]!!, RiskContext(packageName = "test.pay")))
        assertEquals(RiskLevel.READ_ONLY, policy.risk(catalog["clipboard_get"]!!, RiskContext(packageName = "test.pay")))
    }

    @Test fun targetsRequireExactlyOneSelectorOrReferenceAndNeverAcceptCoordinatesInAct() {
        for (text in listOf("{}", "{\"selector\":{}}", "{\"nodeRef\":\"bad\"}",
            "{\"nodeRef\":\"#n1\",\"selector\":{\"text\":\"ok\"}}", "{\"x\":1,\"y\":2}",
            "{\"selector\":{\"text\":\"ok\"},\"snapshotId\":\"s1\"}")) {
            F.fails("TOOL_ARGUMENTS_INVALID") { handler.prepare("ui_click", AgentJson.objectOf(text), F.policy()) }
        }
    }

    @Test fun schemaRejectsUnknownFieldsTypesBoundsAndPreservesCallerArguments() {
        for (text in listOf("{\"maxNodes\":0}", "{\"maxDepth\":33}", "{\"maxNodes\":\"20\"}", "{\"root\":true}", "{\"maxNodes\":1.5}")) {
            F.fails("TOOL_ARGUMENTS_INVALID") { handler.prepare("ui_dump", AgentJson.objectOf(text), F.policy()) }
        }
        val arguments = JsonObject()
        val plan = handler.prepare("ui_dump", arguments, F.policy()) as ToolPlan.Call
        assertEquals(0, arguments.size())
        assertEquals(200L, plan.request.args[0].asJsonObject.number("maxNodes"))
        assertEquals("compact", plan.request.args[0].asJsonObject.string("format"))
    }

    @Test fun schemaRejectsUnsupportedKeywordsAndInvalidDefaults() {
        for (schema in listOf("{\"type\":\"object\",\"additionalProperties\":true}",
            "{\"type\":\"string\",\"format\":\"uri\"}", "{\"type\":\"integer\",\"minimum\":0,\"default\":-1}")) {
            assertThrows(IllegalArgumentException::class.java) { InputSchema(AgentJson.objectOf(schema)) }
        }
    }

    @Test fun compositeFlowsKeepAppendAndPollingSeparateFromSideEffects() {
        val append = handler.prepare("ui_set_text", AgentJson.objectOf("{\"selector\":{\"id\":\"test:id/input\"},\"text\":\"more\",\"append\":true}"), F.policy()) as ToolPlan.AppendText
        assertEquals("more", append.text)
        assertEquals("test:id/input", append.target.string("id"))
        val poll = handler.prepare("ui_wait_for", AgentJson.objectOf("{\"selector\":{\"text\":\"busy\"},\"state\":\"disappear\",\"timeoutMs\":80}"), F.policy()) as ToolPlan.Poll
        assertEquals("disappear", poll.state)
        assertEquals(80L, poll.request.timeoutMs)
        val key = handler.prepare("ui_press_key", AgentJson.objectOf("{\"key\":\"quick_settings\"}"), F.policy()) as ToolPlan.Call
        assertEquals("quickSettings", key.request.method)
        assertEquals(listOf("keys"), key.request.permissions)
    }

    @Test fun scriptParameterBytesAndSelectorSemanticsAreCheckedBeforeInvocation() {
        val args = jsonObject("id" to "example".json(), "parameters" to jsonObject("text" to "中".repeat(6000).json()))
        F.fails("TOOL_ARGUMENTS_INVALID") { handler.prepare("script_run", args, F.policy()) }
        for (text in listOf("{\"selector\":{\"textMatches\":\"[\"}}", "{\"selector\":{\"boundsInside\":{\"left\":3,\"right\":1,\"top\":0,\"bottom\":1}}}"))
            F.fails("TOOL_ARGUMENTS_INVALID") { handler.prepare("ui_click", AgentJson.objectOf(text), F.policy()) }
        F.fails("TOOL_ARGUMENTS_INVALID") { handler.prepare("ocr_screen", AgentJson.objectOf("{\"region\":[1,2,0,4]}"), F.policy()) }
    }

    @Test fun observationTruncationCountsEscapingAndPreservesValidUnicodeJson() {
        for (text in listOf("😀中文".repeat(2000), "\\\"\n".repeat(4000))) {
            val output = ToolObservation.success(jsonObject("text" to text.json()), 1024)
            assertTrue(output.toByteArray(Charsets.UTF_8).size <= 1024)
            assertTrue(AgentJson.objectOf(output).flag("truncated")!!)
        }
    }

    @Test fun bridgeErrorsPreserveStableCodesWithoutLeakingExceptionBodies() {
        val expected = mapOf("process-dead" to "LINK_DETACHED", "unavailable" to "HOST_UNAVAILABLE", "permission-denied" to "CAPABILITY_DENIED",
            "capability-denied" to "CAPABILITY_DENIED", "resource-limit" to "LIMIT_EXCEEDED", "rate-limited" to "RATE_LIMITED",
            "invalid-request" to "TOOL_ARGUMENTS_INVALID", "timeout" to "SCRIPT_TIMEOUT", "runtime-error" to "SCRIPT_FAILED", "provider-failed" to "HOST_UNAVAILABLE")
        expected.forEach { (category, code) ->
            val result = ToolObservation.failure(category, "secret provider response", "agent")
            assertEquals(code, AgentJson.objectOf(result).string("error"))
            assertFalse(result.contains("secret"))
        }
        assertEquals("NODE_REF_STALE", AgentJson.objectOf(ToolObservation.failure("invalid-request", "NODE_REF_STALE")).string("error"))
    }
}
