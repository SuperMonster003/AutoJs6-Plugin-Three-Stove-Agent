package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.done
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.tool
import org.junit.Assert.*
import org.junit.Test

class AgentRunnerLoopRulesTest {
    @Test fun thirdRepeatedActionStopsBeforeExecutionAndEmitsOneBlockedResult() {
        val f = RunnerFixture()
        val click = tool("ui_click", """{"nodeRef":"#n1"}""")
        f.enqueue(click, tool("ui_dump"), click, tool("ui_dump"), click)
        val run = f.start()
        assertEquals(RunState.BLOCKED, run.state)
        assertEquals(2, f.tools.executions.count { it.first.invocation.name == "ui_click" })
        assertEquals(1, f.events.count { it.type == "done" })
        assertFalse(run.result!!.has("error"))
        assertTrue(f.journal(run).toString().contains("REPEATED_ACTION"))
        assertEquals(2L, f.contexts.last().guidance.number("repeatedActionCount"))
    }
    @Test fun repeatedProposalIsBlockedBeforeAThirdConfirmationEvenAfterDenials() {
        val f = RunnerFixture(); val click = tool("ui_click", """{"nodeRef":"#n1"}""")
        f.enqueue(click, click, click)
        val run = f.start(f.options(mode = ConfirmationMode.CAUTIOUS))
        repeat(2) { run.confirm(f.request("confirmation"), false); f.scheduler.drain() }
        assertEquals(RunState.BLOCKED, run.state)
        assertEquals(2, f.events.count { it.type == "confirmation" })
        assertTrue(f.tools.executions.isEmpty())
    }
    @Test fun differentActionsWithNoProgressExposeStrategyGuidanceOnTheNextDecision() {
        val f = RunnerFixture()
        f.tools.action = { AgentJson.objectOf("""{"windowChanged":false,"changes":{"changed":false,"partial":false,"baseline":false},"stability":{"observed":true}}""") }
        for (node in 1..3) f.enqueue(tool("ui_click", """{"nodeRef":"#n$node"}"""))
        val run = f.start()
        assertEquals(RunState.RUNNING, run.state)
        assertEquals(true, f.contexts.last().guidance.flag("changeStrategy"))
        assertEquals(3L, f.contexts.last().guidance.number("unchangedActions"))
        f.reply(done("partial")); assertEquals(RunState.PARTIAL, run.state)
    }
    @Test fun queuedRunsDoNotShareRepetitionCounts() {
        val f = RunnerFixture(); val click = tool("ui_click", """{"nodeRef":"#n1"}""")
        repeat(2) { f.enqueue(click, click, done()) }
        val first = f.submit(); val second = f.submit(); f.scheduler.drain()
        assertEquals(RunState.COMPLETED, first.state); assertEquals(RunState.COMPLETED, second.state)
        assertEquals(4, f.tools.executions.size)
        assertEquals(0L, f.contexts[3].guidance.number("repeatedActionCount"))
    }
    @Test fun cancellationAtTheBlockedStepStillHasOnlyOneTerminalEvent() {
        val f = RunnerFixture(); val click = tool("ui_click", """{"nodeRef":"#n1"}""")
        f.enqueue(click, click, click)
        val run = f.submit()
        f.onEvent = { if (it.type == "step" && it.payload.toString().contains("REPEATED_ACTION")) run.cancel() }
        f.scheduler.drain()
        assertEquals(RunState.CANCELLED, run.state); assertEquals(2, f.tools.executions.size)
        assertEquals(1, f.events.count { it.type == "done" })
    }
}
