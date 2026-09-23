package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.done
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.ask
import org.junit.Assert.*
import org.junit.Test

class RunQueueTest {
    @Test fun oneActiveAndEightQueuedAreAdmittedInFifoOrder() {
        val f = RunnerFixture(); val runs = List(9) { f.submit() }; f.scheduler.drain()
        assertEquals(RunState.RUNNING, runs[0].state); assertTrue(runs.drop(1).all { it.state == RunState.QUEUED })
        assertEquals(RunError.QUEUE_FULL, assertThrows(RunAdmissionFailure::class.java) { f.submit() }.error)
        assertEquals(1, f.model.calls.size)
        runs.forEachIndexed { index, run ->
            assertEquals(RunState.RUNNING, run.state)
            f.reply(done())
            assertEquals(RunState.COMPLETED, run.state); assertEquals(8 - index, f.queue.runs().size)
        }
        assertEquals(9, f.model.calls.size); assertTrue(f.queue.runs().isEmpty())
        assertEquals(9, runs.map { it.id }.distinct().size)
    }
    @Test fun queuedCancellationReleasesCapacityAndNeverStartsThatRun() {
        val f = RunnerFixture(); val runs = List(9) { f.submit() }; f.scheduler.drain()
        runs[2].cancel(); f.scheduler.drain()
        assertEquals(RunState.CANCELLED, runs[2].state); val replacement = f.submit()
        f.reply(done()); assertEquals(RunState.RUNNING, runs[1].state)
        f.reply(done()); assertEquals(RunState.RUNNING, runs[3].state)
        assertEquals(RunState.QUEUED, replacement.state); assertEquals(3, f.model.calls.size)
        f.queue.detach(); f.scheduler.drain(); assertTrue(f.queue.runs().isEmpty())
    }
    @Test fun waitingUserHoldsActiveSlotAndQueuedTimeDoesNotConsumeRunBudget() {
        val f = RunnerFixture(); f.enqueue(ask())
        val active = f.submit(); val queued = f.submit(f.options(BudgetLimits(maxDurationMs = 10)))
        f.scheduler.drain(); f.scheduler.advance(100)
        assertEquals(RunState.WAITING_INPUT, active.state); assertEquals(RunState.QUEUED, queued.state)
        active.cancel(); f.scheduler.drain()
        assertEquals(RunState.RUNNING, queued.state)
        f.reply(done()); assertEquals(0L, queued.result!!.number("durationMs"))
    }
    @Test fun hostDeathAndDetachBlockActiveAndQueuedRunsWithoutRestart() {
        for (detached in listOf(false, true)) {
            val f = RunnerFixture(); val runs = List(3) { f.submit() }; f.scheduler.drain()
            if (detached) f.queue.detach() else f.queue.hostUnavailable()
            f.scheduler.drain()
            assertTrue(runs.all { it.state == RunState.BLOCKED }); assertTrue(f.queue.runs().isEmpty())
            assertEquals(1, f.model.calls.single().cancellations)
            val error = if (detached) RunError.LINK_DETACHED else RunError.HOST_UNAVAILABLE
            assertEquals(error, assertThrows(RunAdmissionFailure::class.java) { f.submit() }.error)
            f.model.calls.single().succeed(ModelReply(done())); f.scheduler.advance(700_000)
            assertEquals(3, f.events.count { it.type == "done" }); assertEquals(1, f.model.calls.size)
            assertTrue(runs.all { it.result!!.getAsJsonObject("error").string("code") == error.name })
        }
    }
    @Test fun hostDeathBeforeQueuedStartsPreventsAllModelCalls() {
        val f = RunnerFixture(); val runs = List(3) { f.submit() }
        f.queue.hostUnavailable(); f.scheduler.drain()
        assertTrue(f.model.calls.isEmpty()); assertTrue(runs.all { it.state == RunState.BLOCKED })
    }
}
