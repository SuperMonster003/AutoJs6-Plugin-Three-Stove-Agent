package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.done
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.tool
import org.junit.Assert.*
import org.junit.Test

class AgentRunnerDoneRulesTest {
    @Test fun missingEvidenceDowngradesWithoutExtraModelCallsAndJournalMatchesTheResult() {
        val f = RunnerFixture(); f.enqueue(done(evidence = emptyList())); val run = f.start()
        assertEquals(RunState.PARTIAL, run.state); assertEquals(1, f.model.calls.size)
        assertTrue(run.result!!.getAsJsonArray("evidence").isEmpty)
        assertFalse(run.result!!.getAsJsonArray("unfinished").isEmpty)
        val step = f.journal(run).getAsJsonArray("steps").single().asJsonObject
        assertEquals("partial", step.getAsJsonObject("decision").getAsJsonObject("done").string("status"))
        assertTrue(step.string("observation")!!.contains("EVIDENCE_MISSING"))
        assertEquals(1, f.events.count { it.type == "done" })
    }
    @Test fun budgetPartialAlsoContainsUnfinishedWork() {
        val f = RunnerFixture(); f.enqueue(tool("ui_dump")); val run = f.start(f.options(BudgetLimits(maxSteps = 1)))
        assertEquals(RunState.PARTIAL, run.state)
        assertEquals("BUDGET_EXCEEDED", run.result!!.getAsJsonObject("error").string("code"))
        assertFalse(run.result!!.getAsJsonArray("unfinished").isEmpty)
    }
    @Test fun orderGoalMissingStateRepairsWithinTheSameStepThenKeepsObservedState() {
        val f = RunnerFixture(); f.enqueue(done(), done(orderStatus = "pending_payment"))
        val run = f.start(RunOptions("请帮我下单一杯拿铁, 停在待付款", f.format))
        assertEquals(RunState.COMPLETED, run.state)
        assertEquals("pending_payment", run.result!!.string("orderStatus"))
        assertEquals(2, f.model.calls.size); assertEquals(1L, run.result!!.number("steps"))
        assertEquals(1L, f.contexts.last().repair!!.number("repairAttempt"))
        assertEquals(true, f.contexts.first().guidance.flag("orderStatusRequired"))
        assertTrue(f.tools.executions.isEmpty())
    }
    @Test fun unresolvedOrderStateExhaustsTwoRepairsWithoutFabricatingAnOrder() {
        val f = RunnerFixture(); f.enqueue(done(), done("partial"), done("blocked"))
        val run = f.start(RunOptions("Buy a latte", f.format))
        assertEquals(RunState.FAILED, run.state)
        assertEquals("DECISION_UNPARSABLE", run.result!!.getAsJsonObject("error").string("code"))
        assertEquals(3, f.model.calls.size); assertEquals(1L, run.result!!.number("steps"))
        assertFalse(run.result!!.has("orderStatus")); assertTrue(f.tools.executions.isEmpty())
    }
    @Test fun paymentInspectionRequiresStateEvenIfUserDeniesTheAction() {
        val f = RunnerFixture(); f.tools.metadata = { ToolMetadata(RiskContext(nodeText = "Pay")) }
        f.enqueue(tool("ui_click", """{"nodeRef":"#n1"}"""), done("partial"),
            done("partial", unfinished = listOf("Payment was declined"), orderStatus = "pending_payment"))
        val run = f.start(); run.confirm(f.request("confirmation"), false); f.scheduler.drain()
        assertEquals(RunState.PARTIAL, run.state); assertTrue(f.tools.executions.isEmpty())
        assertEquals("pending_payment", run.result!!.string("orderStatus"))
        assertEquals(true, f.contexts.last().guidance.flag("orderStatusRequired"))
        assertEquals(3, f.model.calls.size)
    }
    @Test fun syntaxAndOrderRepairsShareOneAllowanceAndCannotBypassModelBudget() {
        for (maxCalls in listOf(2, 3)) {
            val f = RunnerFixture(); f.enqueue("bad", done(), done(orderStatus = "none"))
            val run = f.start(RunOptions("Buy coffee", f.format, limits = BudgetLimits(maxModelCalls = maxCalls)))
            assertEquals(maxCalls, f.model.calls.size)
            assertEquals(if (maxCalls == 2) RunState.FAILED else RunState.COMPLETED, run.state)
            if (maxCalls == 3) assertEquals(2L, f.contexts.last().repair!!.number("repairAttempt"))
            else assertEquals("BUDGET_EXCEEDED", run.result!!.getAsJsonObject("error").string("code"))
            assertEquals(1L, run.result!!.number("steps")); assertTrue(f.tools.executions.isEmpty())
        }
    }
    @Test fun cancellationDuringOrderRepairDiscardsLateCompletion() {
        val f = RunnerFixture(); f.enqueue(done())
        val run = f.start(RunOptions("Buy coffee", f.format))
        assertNotNull(f.contexts.last().repair)
        val pending = f.model.calls.last(); assertTrue(run.cancel()); f.scheduler.drain()
        pending.succeed(ModelReply(done(orderStatus = "paid"))); f.scheduler.drain()
        assertEquals(RunState.CANCELLED, run.state); assertFalse(run.result!!.has("orderStatus"))
        assertEquals(1, f.events.count { it.type == "done" })
    }
}
