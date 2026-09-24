package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class ActionStabilityCancellationTest {
    private class Fixture {
        val scheduler = VirtualScheduler()
        val observations = ObservationTools()
        var actions = 0
        var reads = 0
        var moving = false
        var holdReadback = false
        var locked = false
        var pendingRead: ((PortResult<JsonElement>) -> Unit)? = null
        val tools = ActionTools(scheduler, observations, { request, callback ->
            if (request.method == "dump") {
                reads++
                if (locked) callback(PortResult.Failure(RunError.SCREEN_LOCKED))
                else if (holdReadback && actions > 0) pendingRead = callback
                else callback(PortResult.Success(dump("s$reads", rows = listOf("#n1 TextView \"${if (moving) reads else actions}\" c=(10,10)"))))
            } else { actions++; callback(PortResult.Success(true.json())) }
            Cancellation.NONE
        }, true)
        fun prepared(): PreparedTool {
            val args = jsonObject("packageName" to "example.app".json())
            val invocation = ToolInvocation("app_launch", args, ToolHandlers(F.catalog()).prepare("app_launch", args, F.policy()))
            var result: PreparedTool? = null
            tools.prepare(invocation, 5000) { result = (it as PortResult.Success).value }
            return result!!
        }
    }
    @Test fun aLockedBaselineCannotBeIgnoredBeforeDispatchingAnAction() {
        val f = Fixture(); f.locked = true; var result: PortResult<ToolReply>? = null
        f.tools.execute(f.prepared(), 5000) { result = it }; f.scheduler.drain()
        assertEquals(RunError.SCREEN_LOCKED, (result as PortResult.Failure).error)
        assertEquals(0, f.actions); assertEquals(1, f.reads)
        f.locked = false; f.scheduler.advance(6000)
        assertEquals(0, f.actions)
        f.tools.execute(f.prepared(), 5000) { result = it }; f.scheduler.advance(3000)
        assertTrue(result is PortResult.Success); assertEquals(1, f.actions)
    }
    @Test fun cancellationAfterAcknowledgementStopsStabilizationWithoutRepeatingTheAction() {
        val f = Fixture(); var done = false
        val handle = f.tools.execute(f.prepared(), 5000) { done = true }
        f.scheduler.drain(); assertEquals(1, f.actions); assertFalse(done)
        val reads = f.reads; handle.cancel(); f.scheduler.advance(6000)
        assertEquals(reads, f.reads); assertEquals(1, f.actions); assertFalse(done)
    }
    @Test fun theOperationBudgetIncludesStabilizationAndDoesNotRetryAnAcknowledgedAction() {
        val f = Fixture(); var result: PortResult<ToolReply>? = null
        f.tools.execute(f.prepared(), 300) { result = it }; f.scheduler.advance(300)
        assertEquals(RunError.BUDGET_EXCEEDED, (result as PortResult.Failure).error)
        val reads = f.reads; f.scheduler.advance(5000)
        assertEquals(1, f.actions); assertEquals(reads, f.reads)
    }
    @Test fun unstableScreensReturnAcknowledgementAndAnExplicitUnstableSampleAtThreeSeconds() {
        val f = Fixture(); f.moving = true; var result: ToolReply? = null
        f.tools.execute(f.prepared(), 5000) { result = (it as PortResult.Success).value }; f.scheduler.advance(3000)
        val value = result!!.result.asJsonObject
        assertTrue(value.flag("ok")!!); assertFalse(value.getAsJsonObject("stability").flag("stable")!!)
        assertEquals(3000L, value.getAsJsonObject("stability").number("waitedMs")); assertEquals(1, f.actions)
    }
    @Test fun explicitWaitRefreshesChangesAtItsOwnCompletionPointWithoutAnotherQuietDelay() {
        val f = Fixture()
        f.observations.actionBaseline(CompactNodeText.parse(dump("baseline")))
        var result: ToolReply? = null
        f.tools.afterWait(jsonObject("matched" to true.json(), "state" to "appear".json()), 1000) { result = (it as PortResult.Success).value }
        f.scheduler.drain()
        val value = result!!.result.asJsonObject
        assertEquals(0L, f.scheduler.nowMs()); assertTrue(value.flag("matched")!!)
        assertEquals("baseline", value.getAsJsonObject("sinceLastAction").string("previousSnapshotId"))
        assertEquals(0, f.actions)
    }
    @Test fun aStalledReadbackFinishesAtTheStabilityDeadlineAndIgnoresItsLateReply() {
        val f = Fixture(); f.holdReadback = true
        var result: ToolReply? = null; var completions = 0
        f.tools.execute(f.prepared(), 5000) { completions++; result = (it as PortResult.Success).value }
        f.scheduler.advance(3000)
        assertEquals(1, completions)
        val value = result!!.result.asJsonObject
        assertTrue(value.flag("ok")!!); assertTrue(value["windowChanged"].isJsonNull)
        assertFalse(value.getAsJsonObject("stability").flag("observed")!!)
        assertEquals(3000L, value.getAsJsonObject("stability").number("waitedMs"))
        f.pendingRead!!(PortResult.Success(dump("late"))); f.scheduler.advance(5000)
        assertEquals(1, completions); assertEquals(1, f.actions); assertEquals(2, f.reads)
    }
}
