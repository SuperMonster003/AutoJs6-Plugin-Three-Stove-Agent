package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class DoneRulesTest {
    private fun rules(locale: String = "en") = DoneRules(RunnerText(F.asset("runner/texts.json"), locale))
    private fun done(status: String = "completed", evidence: List<String> = listOf("Observed result"), unfinished: List<String> = emptyList(), order: String? = null) =
        AgentDecision.Done(status, "Original summary", evidence, unfinished, order, null)

    @Test fun completionWithoutEvidenceBecomesPartialWithAnExplanationInEveryLocale() {
        for (locale in AgentJson.objectOf(F.asset("runner/texts.json")).keySet()) {
            val checked = rules(locale).normalize(done(evidence = emptyList()))
            assertEquals("partial", checked.decision.status)
            assertNotEquals("Original summary", checked.decision.summary)
            assertTrue(checked.decision.evidence.isEmpty())
            assertTrue(checked.decision.unfinished.single().isNotBlank())
            assertTrue(checked.decision.unfinished.single().length <= 200)
            assertEquals(listOf("EVIDENCE_MISSING"), checked.rules)
        }
    }
    @Test fun partialRequiresUnfinishedWorkAndDoesNotInventTaskProgress() {
        val checked = rules().normalize(done("partial", evidence = emptyList()))
        assertEquals("partial", checked.decision.status)
        assertEquals("Original summary", checked.decision.summary)
        assertEquals(listOf("UNFINISHED_MISSING"), checked.rules)
        assertTrue(checked.decision.evidence.isEmpty()); assertEquals(1, checked.decision.unfinished.size)
    }
    @Test fun unresolvedWorkPreventsCompletionAndRepairsStayWithinEightEntries() {
        val pending = List(8) { "Remaining $it" }
        val checked = rules().normalize(done(unfinished = pending))
        assertEquals("partial", checked.decision.status); assertEquals(pending, checked.decision.unfinished)
        val missing = rules().normalize(done(evidence = emptyList(), unfinished = pending))
        assertEquals(8, missing.decision.unfinished.size)
        assertEquals(listOf("EVIDENCE_MISSING", "UNFINISHED_COMPLETION"), missing.rules)
    }
    @Test fun validTerminalDecisionsKeepTheirEvidenceSummaryAndOrderState() {
        for (status in listOf("completed", "partial", "failed", "blocked")) {
            val value = done(status, unfinished = if (status == "partial") listOf("Confirm payment") else emptyList(), order = "pending_payment")
            val checked = rules().normalize(value)
            assertEquals(value, checked.decision); assertTrue(checked.rules.isEmpty())
        }
    }
    @Test fun orderTasksRequireAStateEvenForNonCompletedDecisionsWithoutGuessingNone() {
        val rules = rules(); rules.requireOrderStatus()
        for (status in listOf("completed", "partial", "failed", "blocked")) {
            assertThrows(DecisionFailure::class.java) { rules.validate(done(status)) }
            for (state in listOf("none", "cart", "pending_payment", "submitted", "paid")) rules.validate(done(status, order = state))
        }
        assertTrue(rules.orderStatusRequired)
    }
    @Test fun orderIntentUsesTenLanguageTermsWithoutMatchingPayloadOrSortOrder() {
        val policy = ToolPolicy.fromAssets(F::asset)
        for (goal in listOf("Buy a latte", "Order a latte", "Pay for coffee", "请帮我下单星巴克拿铁", "請購買拿鐵", "請幫我下單咖啡",
            "Acheter un café", "Comprar café", "コーヒーを注文して", "커피를 주문해 주세요", "Купить кофе", "شراء قهوة")) {
            assertTrue(goal, policy.isOrderGoal(goal)); assertTrue(goal, policy.withOcrAvailability(true).isOrderGoal(goal))
        }
        for (goal in listOf("Inspect the payload", "Replay the test", "Sort names in ascending order", "Open the keyboard", "打开计算器")) assertFalse(goal, policy.isOrderGoal(goal))
        assertEquals(10, AgentJson.objectOf(F.asset("catalog/order-intent-keywords.json")).size())
    }
}
