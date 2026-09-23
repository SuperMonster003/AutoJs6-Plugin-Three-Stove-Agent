package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.ToolPolicy
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class RunPreparationTest {
    @Test fun admissionRegistersBeforeEventsAndCancellationFencesLatePreparation() {
        val f = RunnerFixture()
        val pending = mutableListOf<Pending<RunComponents>>()
        var admitted = false
        val run = f.queue.submitPrepared(RunOptions("goal", f.format), ToolPolicy(), RunPreparation { callback ->
            Pending(callback).also { pending += it }.cancellation
        }, { admitted = true }, { assertTrue(admitted) })
        f.scheduler.drain(); assertEquals(RunState.RUNNING, run.state); assertTrue(f.model.calls.isEmpty())
        assertTrue(run.cancel()); f.scheduler.drain(); assertEquals(1, pending.single().cancellations)
        pending.single().succeed(RunComponents(RunContextCompiler { error("Must not compile") }, f.model, f.tools))
        f.scheduler.drain(); assertEquals(RunState.CANCELLED, run.state); assertTrue(f.model.calls.isEmpty())
    }
    @Test fun preparationHasADeadlineAndDoesNotSpendModelCalls() {
        val f = RunnerFixture()
        val run = f.queue.submitPrepared(RunOptions("goal", f.format), ToolPolicy(), RunPreparation { Cancellation.NONE })
        f.scheduler.advance(15_000)
        assertEquals(RunState.FAILED, run.state)
        assertEquals("TARGET_UNAVAILABLE", run.result!!.getAsJsonObject("error").string("code"))
        assertTrue(f.model.calls.isEmpty())
    }
    @Test fun hostTokenCeilingIsAppliedBeforeFirstModelAdmission() {
        val f = RunnerFixture()
        val run = f.queue.submitPrepared(RunOptions("goal", f.format), ToolPolicy(), RunPreparation { callback ->
            callback(PortResult.Success(RunComponents(RunContextCompiler { ModelInput(jsonArray(jsonObject("role" to "user".json(), "content" to "goal".json()))) }, f.model, f.tools, 0)))
            Cancellation.NONE
        })
        f.scheduler.drain()
        assertEquals("BUDGET_EXCEEDED", run.result!!.getAsJsonObject("error").string("code")); assertTrue(f.model.calls.isEmpty())
    }
}
