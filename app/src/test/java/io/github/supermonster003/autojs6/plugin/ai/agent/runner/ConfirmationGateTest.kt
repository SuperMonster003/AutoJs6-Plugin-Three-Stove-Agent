package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class ConfirmationGateTest {
    private val catalog = F.catalog()
    private val policy = ToolPolicy.fromAssets(F::asset, ToolGroup.entries.associateWith { true }, true)
    private fun gate(mode: ConfirmationMode = ConfirmationMode.DEFAULT) = ConfirmationGate(policy, mode)
    private fun assess(gate: ConfirmationGate, name: String, metadata: ToolMetadata = ToolMetadata()) = gate.assess(checkNotNull(catalog[name]), metadata)
    @Test fun defaultAndCautiousModesRespectRiskAndReadOnlyTools() {
        for (mode in ConfirmationMode.entries) {
            val gate = gate(mode)
            assertFalse(assess(gate, "ui_dump").required)
            assertEquals(mode == ConfirmationMode.CAUTIOUS, assess(gate, "ui_click").required)
            assertTrue(assess(gate, "files_write").required)
        }
    }
    @Test fun runPermissionIsScopedToOneToolRiskAndTask() {
        val gate = gate(ConfirmationMode.CAUTIOUS)
        val plain = assess(gate, "ui_click")
        assertTrue(gate.allow(plain, ConfirmationScope.RUN))
        assertFalse(assess(gate, "ui_click").required)
        assertTrue(assess(gate, "ui_long_click").required)
        val sensitive = assess(gate, "ui_click", ToolMetadata(RiskContext(nodeText = "Delete")))
        assertEquals(RiskLevel.SENSITIVE, sensitive.risk); assertTrue(sensitive.required)
        assertTrue(gate.allow(sensitive, ConfirmationScope.ONCE))
        assertTrue(assess(gate, "ui_click", ToolMetadata(RiskContext(nodeText = "Delete"))).required)
        assertTrue(assess(gate(ConfirmationMode.CAUTIOUS), "ui_click").required)
    }
    @Test fun paymentCannotReuseEvenAnExistingSensitiveRunPermission() {
        val gate = gate()
        gate.allow(assess(gate, "ui_click", ToolMetadata(RiskContext(nodeText = "Delete"))), ConfirmationScope.RUN)
        val payment = assess(gate, "ui_click", ToolMetadata(RiskContext(nodeText = "支付")))
        assertTrue(payment.required); assertFalse(payment.allowRunScope)
        assertFalse(gate.allow(payment, ConfirmationScope.RUN)); assertTrue(gate.allow(payment, ConfirmationScope.ONCE))
        assertTrue(assess(gate, "ui_click", ToolMetadata(RiskContext(nodeText = "支付"))).required)
        assertFalse(assess(gate, "ui_dump", ToolMetadata(RiskContext(nodeText = "支付"), payment = true)).required)
    }
    @Test fun transactionConfirmationAlwaysRequiresASeparatePaymentDecision() {
        for (mode in ConfirmationMode.entries) for (label in listOf("确认交易", "確認交易", "Confirm transaction")) {
            val gate = gate(mode)
            gate.allow(assess(gate, "ui_click"), ConfirmationScope.RUN)
            gate.allow(assess(gate, "ui_click", ToolMetadata(RiskContext(nodeText = "Delete"))), ConfirmationScope.RUN)
            val metadata = ToolMetadata(RiskContext(nodeText = label, packageName = "com.sankuai.meituan.takeoutnew"))
            val transaction = assess(gate, "ui_click", metadata)
            assertEquals(RiskLevel.SENSITIVE, transaction.risk)
            assertTrue(transaction.required); assertFalse(transaction.allowRunScope)
            assertFalse(gate.allow(transaction, ConfirmationScope.RUN))
            assertTrue(gate.allow(transaction, ConfirmationScope.ONCE))
            assertTrue(assess(gate, "ui_click", metadata).required)
        }
    }
    @Test fun tenLanguagePaymentCatalogPromotesActionsToSensitive() {
        val words = AgentJson.objectOf(F.asset("catalog/payment-keywords.json"))
        assertEquals(10, words.size())
        for ((_, entries) in words.entrySet()) for (word in entries.asJsonArray) {
            val result = assess(gate(), "ui_click", ToolMetadata(RiskContext(nodeDescription = word.asString)))
            assertEquals(word.asString, RiskLevel.SENSITIVE, result.risk)
            assertTrue(result.required); assertFalse(result.allowRunScope)
        }
    }
    @Test fun forcedScriptAndMemoryConfirmationCannotBecomeBlanketPermissions() {
        val gate = gate()
        for ((name, metadata) in listOf("script_run" to ToolMetadata(forceConfirmation = true), "memory_propose" to ToolMetadata())) {
            val decision = assess(gate, name, metadata)
            assertTrue(decision.required); assertFalse(gate.allow(decision, ConfirmationScope.RUN))
        }
        assertEquals(RiskLevel.READ_ONLY, assess(gate, "script_run", ToolMetadata(RiskContext(registeredScriptRisk = RiskLevel.READ_ONLY))).risk)
        assertTrue(assess(gate, "script_run", ToolMetadata(RiskContext(registeredScriptRisk = RiskLevel.SENSITIVE))).required)
    }
    @Test fun policyCopiesMutableRiskInputsAndPreventsSensitiveDowngrades() {
        val packages = mutableSetOf("test.pay")
        val keywords = mutableSetOf("erase")
        val overrides = mutableMapOf("files_write" to RiskLevel.READ_ONLY)
        val custom = ToolPolicy(keywords = keywords, paymentPackages = packages, riskOverrides = overrides)
        packages.clear(); keywords.clear(); overrides.clear()
        assertTrue(custom.isPayment(RiskContext(packageName = "test.pay")))
        assertEquals(RiskLevel.SENSITIVE, custom.risk(checkNotNull(catalog["ui_click"]), RiskContext(nodeText = "Erase")))
        assertEquals(RiskLevel.SENSITIVE, custom.risk(checkNotNull(catalog["files_write"])))
    }
    @Test fun passwordDescriptionsAndArgumentsMaskTextWithoutMutatingInput() {
        val gate = gate(); val metadata = ToolMetadata(RiskContext(nodeText = "private"), passwordField = true)
        assertFalse(gate.description(checkNotNull(catalog["ui_set_text"]), metadata, "zh").contains("private"))
        val arguments = jsonObject("nodeRef" to "#n1".json(), "text" to "private".json())
        assertEquals("***", gate.arguments(arguments, metadata).string("text"))
        assertEquals("private", arguments.string("text"))
    }
}
