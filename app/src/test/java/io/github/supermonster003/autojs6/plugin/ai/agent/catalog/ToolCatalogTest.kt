package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunError
import io.github.supermonster003.autojs6.plugin.ai.agent.service.BinderRunTools
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.junit.Assert.*
import org.junit.Test

class ToolCatalogTest {
    private val catalog = F.catalog()
    private val handler = ToolHandlers(catalog)

    @Test fun catalogAndKeywordSnapshotsFreezeTheApprovedSurface() {
        val snapshot = JsonArray().apply { catalog.tools.forEach { add(it.snapshot()) } }
        assertEquals(AgentJson.parse(F.snapshot("tool-catalog.snapshot.json"), 256 * 1024), snapshot)
        assertEquals(32, catalog.tools.size)
        assertNull(catalog["ask_user"])
        assertNotNull(catalog["script_run_source"])
        assertNotNull(catalog["screen_capture"])
        assertEquals(AgentJson.parse(F.snapshot("sensitive-keywords.snapshot.json")), AgentJson.parse(F.asset("catalog/sensitive-keywords.json")))
        assertEquals(10, AgentJson.objectOf(F.asset("catalog/sensitive-keywords.json")).size())
    }

    @Test fun toolNameConstantsAndConfirmAlwaysMatchThePackagedCatalog() {
        assertEquals(catalog.tools.map { it.name }.toSet(), ToolNames.ALL)
        assertEquals(setOf(ToolNames.MEMORY_PROPOSE, ToolNames.SCRIPT_RUN_SOURCE), catalog.tools.filter { it.confirmAlways }.map { it.name }.toSet())
        assertTrue(catalog.tools.filter { it.confirmAlways }.all { it.risk == RiskLevel.SENSITIVE && it.external == null })
        assertEquals(28, ToolNames.HOST_DISPATCHED.size); assertEquals(32, ToolNames.ALL.size)
    }

    @Test fun disabledGroupsAreHiddenAndRejectedBeforeArgumentValidation() {
        val policy = ToolPolicy()
        val visible = AgentJson.parse(catalog.render(policy)).asJsonArray.map { it.asJsonObject.string("name") }
        for (name in listOf("files_read", "files_write", "shell_exec", "ui_gesture", "ui_click_xy", "ocr_screen", "script_run_source")) {
            assertFalse(name, name in visible)
            F.fails("TOOL_DISABLED") { handler.prepare(name, JsonObject(), policy) }
        }
        F.fails("TOOL_UNKNOWN") { handler.prepare("unknown_tool", JsonObject(), F.policy()) }
        assertTrue(AgentJson.parse(catalog.render(F.policy())).asJsonArray.size() == 31)
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

    @Test fun bridgeErrorsMapHostCategoriesToStableCodesWithoutExceptionBodies() {
        fun error(category: String, message: String = "secret provider response", module: String = "agent") =
            jsonObject("message" to message.json(), "category" to category.json(), "module" to module.json())
        val expected = mapOf(H.ERROR_PROCESS_DEAD to RunError.HOST_UNAVAILABLE, H.ERROR_PERMISSION_DENIED to RunError.CAPABILITY_DENIED,
            H.ERROR_CAPABILITY_DENIED to RunError.CAPABILITY_DENIED, H.ERROR_RESOURCE_LIMIT to RunError.LIMIT_EXCEEDED,
            H.ERROR_RATE_LIMITED to RunError.RATE_LIMITED, "invalid-request" to RunError.TOOL_ARGUMENTS_INVALID,
            "runtime-error" to RunError.TOOL_ARGUMENTS_INVALID, "provider-failed" to RunError.TOOL_ARGUMENTS_INVALID)
        expected.forEach { (category, code) -> assertEquals(category, code, BinderRunTools.bridgeError(error(category))) }
        assertEquals(RunError.SCRIPT_TIMEOUT, BinderRunTools.bridgeError(error(H.ERROR_TIMEOUT), scriptExecution = true))
        assertEquals(RunError.NODE_NOT_FOUND, BinderRunTools.bridgeError(error(H.ERROR_TIMEOUT)))
        // Only the stable prefix of the host message is interpreted; the body after it never becomes a code.
        assertEquals(RunError.NODE_REF_STALE, BinderRunTools.bridgeError(error("invalid-request", "NODE_REF_STALE: secret provider response")))
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, BinderRunTools.bridgeError(error("invalid-request", "secret: NODE_REF_STALE")))
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, BinderRunTools.bridgeError(null))
    }
}
