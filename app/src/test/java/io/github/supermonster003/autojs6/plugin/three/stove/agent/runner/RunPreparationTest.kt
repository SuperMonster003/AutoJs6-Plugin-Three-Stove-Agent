package io.github.supermonster003.autojs6.plugin.three.stove.agent.runner

import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolPolicy
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
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
        f.scheduler.advance(RunLimits.PREPARATION_MS)
        assertEquals(RunState.FAILED, run.state)
        assertEquals("TARGET_UNAVAILABLE", run.result!!.getAsJsonObject("error").string("code"))
        assertTrue(f.model.calls.isEmpty())
    }
    @Test fun mcpDiscoveryDeadlineLeavesRoomForTheRestOfPreparation() {
        assertTrue(RunLimits.MCP_PREPARATION_MS < RunLimits.PREPARATION_MS)
        val f = RunnerFixture()
        val run = f.queue.submitPrepared(RunOptions("goal", f.format), ToolPolicy(), RunPreparation { callback ->
            f.scheduler.schedule(RunLimits.MCP_PREPARATION_MS) { callback(PortResult.Failure(RunError.TOOL_FAILED, mcpReason = "MCP_TIMEOUT")) }
        })
        f.scheduler.advance(RunLimits.MCP_PREPARATION_MS)
        assertEquals(RunState.FAILED, run.state)
        assertEquals("TOOL_FAILED", run.result!!.getAsJsonObject("error").string("code"))
        assertTrue(run.result!!.string("summary")!!.endsWith("[MCP_TIMEOUT]"))
        assertTrue(f.model.calls.isEmpty())
    }
    @Test fun internalFailuresRecordTheExceptionClassWithoutItsMessage() {
        val f = RunnerFixture()
        val run = f.queue.submitPrepared(RunOptions("goal", f.format), ToolPolicy(), RunPreparation { callback ->
            callback(PortResult.Success(RunComponents(RunContextCompiler { throw IllegalStateException("private detail") }, f.model, f.tools)))
            Cancellation.NONE
        })
        f.scheduler.drain()
        assertEquals(RunState.FAILED, run.state)
        assertEquals("INVALID_REQUEST", run.result!!.getAsJsonObject("error").string("code"))
        val journal = f.journal(run)
        val step = journal.getAsJsonArray("steps").single().asJsonObject
        assertEquals("error", step.string("kind")); assertEquals("INVALID_REQUEST", step.string("error"))
        assertEquals("runtime", step.getAsJsonObject("decision").string("source"))
        assertEquals("IllegalStateException", step.getAsJsonObject("decision").string("failure"))
        assertFalse(journal.toString().contains("private detail")); assertTrue(f.model.calls.isEmpty())
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
