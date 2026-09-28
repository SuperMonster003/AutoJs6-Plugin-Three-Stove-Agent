package io.github.supermonster003.autojs6.plugin.three.stove.agent.runner

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.RunnerFixture.Companion.done
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.RunnerFixture.Companion.plan
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.RunnerFixture.Companion.tool
import org.junit.Assert.*
import org.junit.Test

/** Plan mode (roadmap P13): a reviewed plan precedes the first tool, travels with every prompt and can be replaced for another review. */
class AgentRunnerPlanTest {
    private val approved = jsonArray("Open Settings".json(), "Turn on Wi-Fi and verify".json())

    @Test fun aReviewedAndEditedPlanPrecedesTheFirstToolAndTravelsWithEveryPrompt() {
        val f = RunnerFixture(planMode = true); val run = f.start()
        assertTrue(f.options().format.planMode)
        assertEquals(true, f.contexts.last().guidance.flag("planRequired"))
        f.reply(tool("ui_dump"))
        val repair = f.contexts.last().repair!!
        assertEquals("DECISION_UNPARSABLE", repair.string("error")); assertTrue(repair.string("hint")!!.contains("plan"))
        assertTrue(f.tools.inspections.isEmpty())
        f.reply(plan("Open Settings", "Find the Wi-Fi switch", "Turn it on", "Verify the toggle"))
        assertEquals(RunState.WAITING_INPUT, run.state)
        val input = f.events.last { it.type == "input" }.payload
        assertEquals("plan", input.string("kind")); assertEquals(4, input.getAsJsonArray("steps").size())
        assertFalse(input.string("question").isNullOrBlank()); assertNotNull(input.number("timeoutMs"))
        var status: ReplyStatus? = null
        run.respond(input.string("requestId")!!, approved) { status = it }; f.scheduler.drain()
        assertEquals(ReplyStatus.ACCEPTED, status); assertEquals(RunState.RUNNING, run.state)
        val observation = AgentJson.objectOf(f.contexts.last().observation!!).getAsJsonObject("result")
        assertEquals(approved, observation.getAsJsonArray("plan")); assertEquals(true, observation.flag("approved")); assertEquals(true, observation.flag("edited"))
        val guidance = f.contexts.last().guidance
        assertNull(guidance["planRequired"]); assertEquals(approved, guidance.getAsJsonArray("plan"))
        f.reply(tool("ui_dump")); assertEquals(1, f.tools.executions.size)
        assertEquals(approved, f.contexts.last().guidance.getAsJsonArray("plan"))
        f.reply(done()); assertEquals(RunState.COMPLETED, run.state)
        val steps = f.journal(run).getAsJsonArray("steps")
        assertEquals(3, steps.size())
        assertEquals("plan", steps[0].asJsonObject.string("kind"))
        assertEquals(4, steps[0].asJsonObject.getAsJsonObject("decision").getAsJsonObject("plan").getAsJsonArray("steps").size())
        assertEquals(1L, steps[0].asJsonObject.getAsJsonObject("decision").number("repairs"))
        assertTrue(steps[0].asJsonObject.string("observation")!!.contains("Turn on Wi-Fi and verify"))
    }

    @Test fun planRepliesAreValidatedAndATimeoutSendsTheModelBackToPlanning() {
        val f = RunnerFixture(planMode = true); val run = f.start(f.options(BudgetLimits(askTimeoutMs = 50)))
        f.reply(plan("One", "Two", "Three"))
        val request = f.request("input")
        var status: ReplyStatus? = null
        for (value in listOf(JsonArray(), JsonArray().apply { repeat(9) { add("s$it") } }, jsonArray("".json()), jsonArray(1.json()), "One".json(),
            jsonArray("x".repeat(201).json()))) {
            run.respond(request, value) { status = it }; f.scheduler.drain(); assertEquals(value.toString(), ReplyStatus.INVALID, status)
        }
        run.respond(request, jsonArray("One".json()), "global") { status = it }; f.scheduler.drain(); assertEquals(ReplyStatus.INVALID, status)
        assertEquals(RunState.WAITING_INPUT, run.state); assertEquals(1, f.model.calls.size)
        f.scheduler.advance(50)
        assertEquals(RunState.RUNNING, run.state); assertTrue(f.contexts.last().observation!!.contains("USER_TIMEOUT"))
        assertEquals(true, f.contexts.last().guidance.flag("planRequired"))
        f.reply(plan("One", "Two", "Three")); assertEquals(RunState.WAITING_INPUT, run.state)
        run.respond(f.request("input"), jsonArray("One".json(), "Two".json(), "Three".json())); f.scheduler.drain()
        assertEquals(false, AgentJson.objectOf(f.contexts.last().observation!!).getAsJsonObject("result").flag("edited"))
        // A later plan is reviewed again, so the user keeps control when the model changes course.
        f.reply(plan("Different first step", "Then finish")); assertEquals(RunState.WAITING_INPUT, run.state)
        run.respond(f.request("input"), jsonArray("Different first step".json(), "Then finish".json())); f.scheduler.drain()
        assertEquals(jsonArray("Different first step".json(), "Then finish".json()), f.contexts.last().guidance.getAsJsonArray("plan"))
        f.reply(done()); assertEquals(RunState.COMPLETED, run.state)
    }

    @Test fun plansAreRejectedOutsidePlanModeAndNeverRequired() {
        val f = RunnerFixture(); val run = f.start()
        assertFalse(f.options().format.planMode); assertNull(f.contexts.last().guidance["planRequired"])
        f.reply(plan("One", "Two", "Three"))
        assertEquals("DECISION_UNPARSABLE", f.contexts.last().repair!!.string("error"))
        assertEquals(RunState.RUNNING, run.state); assertTrue(f.events.none { it.type == "input" })
        f.reply(tool("ui_dump")); assertEquals(1, f.tools.executions.size)
        f.reply(done()); assertEquals(RunState.COMPLETED, run.state)
    }

    @Test fun askingAndFinishingRemainPossibleBeforeThePlanIsReviewed() {
        val f = RunnerFixture(planMode = true); val run = f.start()
        f.reply(RunnerFixture.ask())
        assertEquals(RunState.WAITING_INPUT, run.state); assertEquals("text", f.events.last { it.type == "input" }.payload.string("kind"))
        run.respond(f.request("input"), "Office".json()); f.scheduler.drain()
        assertEquals(true, f.contexts.last().guidance.flag("planRequired"))
        f.reply(done("failed", "Nothing to do", evidence = emptyList(), unfinished = listOf("The goal was empty")))
        assertEquals(RunState.FAILED, run.state)
    }
}
