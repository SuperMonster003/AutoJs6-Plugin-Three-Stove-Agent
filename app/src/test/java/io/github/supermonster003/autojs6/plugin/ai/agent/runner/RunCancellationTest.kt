package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.tool
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.done
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.ask
import org.junit.Assert.*
import org.junit.Test

class RunCancellationTest {
    private fun assertSettled(f: RunnerFixture, run: AgentRunner, state: RunState = RunState.CANCELLED) {
        assertEquals(state, run.state)
        val events = f.events.filter { it.runId == run.id }
        assertEquals(1, events.count { it.type == "done" }); assertEquals("done", events.last().type)
        assertEquals((1L..events.size.toLong()).toList(), events.map { it.sequence })
    }
    @Test fun cancellationBeforeStartDoesNotDispatchAnything() {
        val f = RunnerFixture(); val run = f.submit()
        assertTrue(run.cancel()); assertFalse(run.cancel()); f.scheduler.drain()
        assertSettled(f, run); assertTrue(f.model.calls.isEmpty()); assertTrue(f.tools.inspections.isEmpty())
        assertEquals(0L, run.result!!.number("steps"))
    }
    @Test fun cancellationAtEveryAsyncWaitIgnoresLateAndDuplicateCallbacks() {
        for (phase in listOf("model", "prepare", "execute", "input", "confirmation")) {
            val f = RunnerFixture()
            f.tools.autoPrepare = phase != "prepare"; f.tools.autoExecute = phase != "execute"
            if (phase != "model") f.enqueue(when (phase) {
                "input" -> ask()
                "confirmation" -> tool("files_write", """{"path":"x","content":"x"}""")
                else -> tool("ui_dump")
            })
            val run = f.start(); assertTrue(run.cancel()); f.scheduler.drain(); assertSettled(f, run)
            val eventCount = f.events.size
            when (phase) {
                "model" -> {
                    val pending = f.model.calls.single(); assertEquals(1, pending.cancellations)
                    repeat(2) { pending.succeed(ModelReply(tool("ui_dump"))) }
                }
                "prepare" -> {
                    val (invocation, pending) = f.tools.inspections.single(); assertEquals(1, pending.cancellations)
                    repeat(2) { pending.succeed(PreparedTool(invocation, ToolMetadata())) }
                }
                "execute" -> {
                    val pending = f.tools.executions.single().second; assertEquals(1, pending.cancellations)
                    repeat(2) { pending.succeed(ToolReply(true.json())) }
                }
                "input" -> run.respond(f.request("input"), "later".json())
                "confirmation" -> run.confirm(f.request("confirmation"), true)
            }
            f.scheduler.advance(700_000)
            assertEquals(eventCount, f.events.size); assertEquals(1, f.model.calls.size)
            assertEquals(if (phase == "execute") 1 else 0, f.tools.executions.size)
            assertSettled(f, run)
        }
    }
    @Test fun cancelArrivingBeforePortReturnsCancelsTheReturnedHandle() {
        val f = RunnerFixture(); lateinit var run: AgentRunner
        f.model.onGenerate = { run.cancel() }
        run = f.submit(); f.scheduler.drain()
        assertEquals(1, f.model.calls.single().cancellations); assertSettled(f, run)
    }
    @Test fun cancellationFlagWinsOverAlreadyQueuedSuccessfulReply() {
        val f = RunnerFixture(); val run = f.start()
        f.model.calls.single().succeed(ModelReply(tool("ui_dump")))
        assertTrue(run.cancel()); f.scheduler.drain()
        assertTrue(f.tools.inspections.isEmpty()); assertSettled(f, run)
    }
    @Test fun duplicateSuccessCannotDoubleExecuteOrFinish() {
        val f = RunnerFixture(); val run = f.start()
        val callback = f.model.calls.single()
        repeat(2) { callback.succeed(ModelReply(tool("ui_dump"))) }; f.scheduler.drain()
        assertEquals(1, f.tools.executions.size); assertEquals(2, f.model.calls.size)
        f.reply(done()); val count = f.events.size
        callback.fail(RunError.HOST_UNAVAILABLE); f.scheduler.drain()
        assertEquals(count, f.events.size); assertSettled(f, run, RunState.COMPLETED)
    }
    @Test fun reentrantProgressCancellationPreventsItsToolDispatch() {
        val f = RunnerFixture(); lateinit var run: AgentRunner
        f.enqueue(tool("report_progress", """{"message":"Next step"}"""))
        f.onEvent = { if (it.type == "progress") run.cancel() }
        run = f.submit(); f.scheduler.drain()
        assertTrue(f.tools.executions.isEmpty()); assertEquals(0L, run.result!!.number("toolCalls"))
        assertSettled(f, run)
    }
    @Test fun observerFailureDoesNotStopAnOwnedRun() {
        val f = RunnerFixture(); f.onEvent = { throw IllegalStateException("Observer disappeared") }
        f.enqueue(tool("ui_dump"), done()); val run = f.start()
        assertSettled(f, run, RunState.COMPLETED); assertEquals(1, f.tools.executions.size)
    }
    @Test fun cancellationFromDoneStepEventWinsBeforeTerminalPublication() {
        val f = RunnerFixture(); lateinit var run: AgentRunner
        f.onEvent = { if (it.type == "step") run.cancel() }
        f.enqueue(done()); run = f.submit(); f.scheduler.drain()
        assertSettled(f, run)
    }
    @Test fun unresolvedTextIsRedactedWhenInspectionIsCancelledOrFails() {
        for (cancel in listOf(false, true)) {
            val f = RunnerFixture(); f.tools.autoPrepare = false
            f.enqueue(tool("ui_set_text", """{"nodeRef":"#n1","text":"private-value"}"""))
            val run = f.start()
            if (cancel) run.cancel() else f.tools.inspections.single().second.fail(RunError.HOST_UNAVAILABLE)
            f.scheduler.drain()
            assertFalse(f.journal(run).toString().contains("private-value"))
            assertSettled(f, run, if (cancel) RunState.CANCELLED else RunState.BLOCKED)
        }
    }
}
