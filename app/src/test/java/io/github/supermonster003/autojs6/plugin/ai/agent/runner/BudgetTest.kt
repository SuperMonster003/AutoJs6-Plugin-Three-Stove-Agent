package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class BudgetTest {
    private fun exceeds(dimension: String, block: () -> Unit) {
        val error = assertThrows(BudgetExceeded::class.java, block)
        assertEquals(dimension, error.dimension)
    }
    @Test fun defaultsAndOwnershipRespectContractCeilings() {
        val limits = BudgetLimits()
        assertEquals(40, limits.maxSteps); assertEquals(60, limits.maxModelCalls)
        assertEquals(600_000, limits.maxDurationMs); assertEquals(300_000, limits.maxTotalTokens)
        assertEquals(30_000, limits.stepToolTimeoutMs); assertEquals(120_000, limits.confirmationTimeoutMs)
        assertEquals(600_000, limits.askTimeoutMs)
        assertEquals(1_800_000, BudgetLimits.defaults(true).maxDurationMs)
        BudgetLimits(maxDurationMs = RunLimits.DETACHED_DURATION_MS).validateOwnership(true)
        assertThrows(IllegalArgumentException::class.java) { BudgetLimits(maxDurationMs = RunLimits.DETACHED_DURATION_MS).validateOwnership(false) }
        listOf<() -> Unit>({ BudgetLimits(maxSteps = 201) }, { BudgetLimits(maxModelCalls = 301) },
            { BudgetLimits(maxTotalTokens = 1_000_001) }, { BudgetLimits(stepToolTimeoutMs = 300_001) },
            { BudgetLimits(maxDurationMs = 3_600_001) }, { BudgetLimits(confirmationTimeoutMs = 0) },
            { ModelUsage(inputTokens = -1) }).forEach { assertThrows(IllegalArgumentException::class.java, it) }
    }
    @Test fun stepsCallsAndTokenAdmissionCountOnlyAdmittedWork() {
        val budget = Budget(BudgetLimits(maxSteps = 1, maxModelCalls = 1, maxTotalTokens = 100), 0) { 0 }
        budget.beginStep(); exceeds("steps") { budget.beginStep() }; assertEquals(1, budget.steps)
        val ticket = budget.reserveModel(100, 90)
        assertEquals(60, ticket.maximumOutputTokens)
        assertEquals(60L, budget.remainingJson().number("tokens"))
        assertThrows(IllegalStateException::class.java) { budget.reserveModel(0, 1) }
        budget.settleModel(ticket, ModelUsage(40, 60, 99), 0)
        assertEquals(100, budget.totalTokens); assertFalse(budget.estimated)
        exceeds("modelCalls") { budget.reserveModel(0, 1) }
        assertThrows(IllegalStateException::class.java) { budget.settleModel(ticket, null, 0) }
        val insufficient = Budget(BudgetLimits(maxTotalTokens = 40), 0) { 0 }
        exceeds("tokens") { insufficient.reserveModel(100, 1) }; assertEquals(0, insufficient.modelCalls)
    }
    @Test fun missingUsageAndAbandonedRequestsRemainAccountedFor() {
        val budget = Budget(BudgetLimits(), 0) { 0 }
        val first = budget.reserveModel(101, 10)
        assertEquals(41L, budget.usageJson().number("inputTokens"))
        assertEquals(true, budget.usageJson().flag("estimated"))
        budget.settleModel(first, ModelUsage(outputTokens = 5, totalTokens = 80), 999)
        assertEquals(80, budget.totalTokens); assertEquals(41, budget.inputTokens); assertEquals(5, budget.outputTokens)
        budget.reserveModel(10, 10); budget.abandonModel(); budget.abandonModel()
        assertEquals(84, budget.totalTokens); assertEquals(2, budget.modelCalls); assertTrue(budget.estimated)
    }
    @Test fun reportedOverageAndOverflowStopFurtherAdmissions() {
        val budget = Budget(BudgetLimits(maxTotalTokens = 100), 0) { 0 }
        budget.settleModel(budget.reserveModel(1, 1), ModelUsage(Long.MAX_VALUE, Long.MAX_VALUE), 0)
        assertEquals(Long.MAX_VALUE, budget.totalTokens)
        exceeds("tokens") { budget.beginStep() }
        assertEquals(0L, budget.remainingJson().number("tokens"))
    }
    @Test fun monotonicDurationIncludesWaitingAndClampsToolTimeouts() {
        var time = 1000L
        val budget = Budget(BudgetLimits(maxDurationMs = 500_000), time) { time }
        assertEquals(30_000, budget.toolTimeout()); assertEquals(300_000, budget.toolTimeout(300_000))
        time += 499_900
        assertEquals(100, budget.toolTimeout(300_000)); assertEquals(499_900, budget.durationMs)
        time += 100
        exceeds("duration") { budget.check() }; assertEquals(0, budget.remainingMs)
    }
}
