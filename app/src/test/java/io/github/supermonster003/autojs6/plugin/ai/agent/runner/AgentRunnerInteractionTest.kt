package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.ask
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.done
import org.junit.Assert.*
import org.junit.Test

class AgentRunnerInteractionTest {
    @Test fun rememberingEachAnswerKindCreatesASeparateOnceOnlyBudgetedProposal() {
        for ((kind, value) in listOf("text" to "Office".json(), "choice" to "two".json(), "confirm" to true.json())) {
            val f = RunnerFixture(); f.enqueue(ask(kind, "destination")); val run = f.start()
            val request = f.request("input")
            run.respond(request, value, "global"); f.scheduler.drain()
            assertEquals(RunState.WAITING_CONFIRMATION, run.state)
            assertEquals(1, f.model.calls.size); assertTrue(f.tools.executions.isEmpty())
            val proposal = f.events.last { it.type == "confirmation" }.payload
            assertEquals("memory_propose", proposal.string("tool")); assertEquals(false, proposal.flag("allowRunScope"))
            assertEquals(value.asString, proposal.getAsJsonObject("arguments").string("value"))
            var status: ReplyStatus? = null
            run.respond(request, value, "global") { status = it }; f.scheduler.drain(); assertEquals(ReplyStatus.NOT_WAITING, status)
            run.confirm(f.request("confirmation"), true, ConfirmationScope.RUN) { status = it }
            f.scheduler.drain(); assertEquals(ReplyStatus.INVALID, status); assertTrue(f.tools.executions.isEmpty())
            run.confirm(f.request("confirmation"), true); f.scheduler.drain(); assertEquals(1, f.tools.executions.size)
            assertEquals(2, f.model.calls.size)
            val history = f.contexts.last().history
            assertEquals(2, history.size); assertTrue(history[0].string("observation")!!.contains(value.asString))
            assertEquals("user", history[1].getAsJsonObject("decision").string("source"))
            assertEquals(0L, history[1].getAsJsonObject("usage").number("modelCalls"))
            f.reply(done()); assertEquals(3L, run.result!!.number("steps"))
        }
    }
    @Test fun deniedAndTimedOutProposalsKeepAnswerInHistoryWithoutSaving() {
        for (timeout in listOf(false, true)) {
            val f = RunnerFixture(); f.enqueue(ask(memoryKey = "destination"))
            val run = f.start(f.options(BudgetLimits(confirmationTimeoutMs = 20)))
            run.respond(f.request("input"), "Office".json(), "default"); f.scheduler.drain()
            val request = f.request("confirmation")
            if (timeout) f.scheduler.advance(20) else { run.confirm(request, false); f.scheduler.drain() }
            assertTrue(f.tools.executions.isEmpty()); assertEquals(2, f.model.calls.size)
            assertTrue(f.contexts.last().observation!!.contains(if (timeout) "USER_TIMEOUT" else "USER_DENIED"))
            assertTrue(f.contexts.last().history.first().string("observation")!!.contains("Office"))
            run.confirm(request, true); f.scheduler.drain(); assertTrue(f.tools.executions.isEmpty())
        }
    }
    @Test fun rememberingRequiresMemoryKeyAndEnabledToolWithoutConsumingAnswer() {
        for (enabled in listOf(false, true)) {
            val f = RunnerFixture(ToolPolicy(ToolGroup.entries.associateWith { it != ToolGroup.MEMORY || enabled }))
            f.enqueue(ask(memoryKey = if (enabled) null else "destination")); val run = f.start()
            var status: ReplyStatus? = null
            run.respond(f.request("input"), "Office".json(), "global") { status = it }; f.scheduler.drain()
            assertEquals(ReplyStatus.INVALID, status); assertEquals(RunState.WAITING_INPUT, run.state)
            run.respond(f.request("input"), "Office".json()); f.scheduler.drain()
            assertEquals(2, f.model.calls.size); assertTrue(f.tools.inspections.isEmpty())
        }
    }
    @Test fun userProposalCannotExceedTheStepBudget() {
        val f = RunnerFixture(); f.enqueue(ask(memoryKey = "destination"))
        val run = f.start(f.options(BudgetLimits(maxSteps = 1)))
        run.respond(f.request("input"), "Office".json(), "global"); f.scheduler.drain()
        assertEquals("BUDGET_EXCEEDED", run.result!!.getAsJsonObject("error").string("code"))
        assertTrue(f.tools.inspections.isEmpty()); assertTrue(f.tools.executions.isEmpty())
    }
    @Test fun deadlineIsMonotonicAndPrivateAndLateAnswersCannotCreateMemory() {
        val f = RunnerFixture(); f.scheduler.advance(1000); f.enqueue(ask(memoryKey = "destination"))
        val run = f.start(f.options(BudgetLimits(askTimeoutMs = 50)))
        val event = f.events.last { it.type == "input" }
        assertEquals(1050L, event.interactionDeadlineMs); assertFalse(event.payload.has("deadlineMs"))
        f.scheduler.advance(50)
        run.respond(f.request("input"), "Office".json(), "global"); f.scheduler.drain()
        assertTrue(f.tools.inspections.isEmpty()); assertTrue(f.contexts.last().observation!!.contains("USER_TIMEOUT"))
    }
}
