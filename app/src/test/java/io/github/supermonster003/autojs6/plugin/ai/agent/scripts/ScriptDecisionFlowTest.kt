package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.ask
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.done
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.tool
import org.autojs.plugin.ai.agent.api.AiAgentContract
import org.junit.Assert.*
import org.junit.Test

class ScriptDecisionFlowTest {
    private class Session(val registration: JsonObject = ScriptFixtures.entry().apply {
        add("parameters", AgentJson.objectOf("""{"type":"object","properties":{"address":{"type":"string"},"count":{"type":"integer","default":1}},"required":["address","count"]}"""))
    }) {
        val scheduler = VirtualScheduler()
        val catalog = F.catalog()
        val policy = ToolPolicy()
        val model = FakeModel()
        val delegate = FakeTools() // Execution is explicitly simulated; production ScriptInvoker belongs to P3.3.
        val contexts = mutableListOf<RunContext>()
        val events = mutableListOf<RunEvent>()
        val source = ScriptCatalogSource { call, reply ->
            require(call.method in setOf("listScripts", "readManifest"))
            reply(PortResult.Success(if (call.method == "listScripts") jsonArray(registration) else registration.deepCopy()))
            Cancellation.NONE
        }
        val tools = RegisteredScriptTools(ScriptCatalogClient(scheduler::nowMs), emptySet(), source, delegate, DecisionValidator(catalog), true, scheduler::nowMs)
        val queue = RunQueue(scheduler, catalog, policy, RunContextCompiler { contexts += it; ModelInput(JsonArray()) }, model, tools) { RunnerText(F.asset("runner/texts.json"), it) }
        val run = queue.submit(RunOptions("Run the registered coffee task", DecisionSchema.degraded())) { events += it }.also { scheduler.drain() }
        fun reply(decision: String) { model.calls.last().succeed(ModelReply(decision, ModelUsage(20, 10, 30))); scheduler.drain() }
        fun request(type: String) = events.last { it.type == type }.payload.string("requestId")!!
        fun journal(): JsonObject { var value: JsonObject? = null; run.readJournal { value = it }; scheduler.drain(); return value!! }
        fun script(parameters: JsonObject = JsonObject()) = tool("script_run", jsonObject("id" to "clean-downloads".json(), "parameters" to parameters).toString())
    }

    @Test fun missingValuesBecomeAnAskTurnThenConfirmedEffectiveParameters() {
        val s = Session()
        s.reply(s.script())
        assertEquals(2, s.model.calls.size); assertNull(s.contexts.last().repair)
        assertTrue(s.contexts.last().observation!!.contains("SCRIPT_PARAMETERS_MISSING"))
        assertTrue(s.events.none { it.type == "confirmation" }); assertTrue(s.delegate.executions.isEmpty())
        s.reply(ask(memoryKey = "address"))
        assertEquals(RunState.WAITING_INPUT, s.run.state)
        s.run.respond(s.request("input"), "Office front desk".json()); s.scheduler.drain()
        val answer = AgentJson.objectOf(s.contexts.last().observation!!).getAsJsonObject("result")
        assertEquals("address", answer.string("memoryKey")); assertTrue(answer.flag("memoryProposalOnly")!!)
        s.reply(s.script(jsonObject("address" to "Office front desk".json())))
        assertEquals(RunState.WAITING_CONFIRMATION, s.run.state)
        val event = s.events.last { it.type == "confirmation" }.payload
        assertTrue(event.string("description")!!.contains("Remove old installers"))
        assertEquals(1L, event.getAsJsonObject("arguments").getAsJsonObject("parameters").number("count"))
        var status: ReplyStatus? = null
        s.run.confirm(s.request("confirmation"), true, ConfirmationScope.RUN) { status = it }; s.scheduler.drain()
        assertEquals(ReplyStatus.INVALID, status); assertTrue(s.delegate.executions.isEmpty())
        s.run.confirm(s.request("confirmation"), true); s.scheduler.drain()
        assertEquals(1, s.delegate.executions.size)
        assertEquals("Office front desk", (s.delegate.executions.single().first.opaqueContext as PreparedScript).parameters.string("address"))
        s.reply(done())
        assertEquals(RunState.COMPLETED, s.run.state)
        assertEquals(4L, s.run.result!!.getAsJsonObject("usage").number("modelCalls"))
        assertTrue(s.journal().getAsJsonArray("steps").none { it.asJsonObject.getAsJsonObject("decision").number("repairs") != 0L })
    }
    @Test fun directAskIsAllowedAndDenyNeverCallsTheExecutionAdapter() {
        val s = Session(); s.reply(ask(memoryKey = "address"))
        s.run.respond(s.request("input"), "Office".json()); s.scheduler.drain()
        s.reply(s.script(jsonObject("address" to "Office".json())))
        s.run.confirm(s.request("confirmation"), false); s.scheduler.drain()
        assertTrue(s.delegate.executions.isEmpty())
        assertTrue(s.contexts.last().observation!!.contains("USER_DENIED"))
        s.reply(done("blocked", "User declined")); assertEquals(RunState.BLOCKED, s.run.state)
    }
    @Test fun sensitiveRegistrationAlwaysConfirmsIncludingRepeatedCalls() {
        val s = Session(ScriptFixtures.entry().apply { addProperty("risk", "sensitive"); addProperty("confirm", "never") })
        repeat(2) { index ->
            s.reply(s.script()); assertEquals(RunState.WAITING_CONFIRMATION, s.run.state)
            assertEquals(index, s.delegate.executions.size)
            s.run.confirm(s.request("confirmation"), true); s.scheduler.drain()
        }
        assertEquals(2, s.delegate.executions.size); s.run.cancel(); s.scheduler.drain()
    }
    @Test fun readonlyAndNormalNeverRegistrationsNeedNoDefaultModeConfirmation() {
        for (risk in listOf("readonly", "normal")) {
            val s = Session(ScriptFixtures.entry().apply { addProperty("risk", risk); addProperty("confirm", "never") })
            s.reply(s.script())
            assertTrue(s.events.none { it.type == "confirmation" }); assertEquals(1, s.delegate.executions.size)
            s.run.cancel(); s.scheduler.drain()
        }
    }
    @Test fun cancelledConfirmationCannotStartAnExecutionFromALateReply() {
        val s = Session(ScriptFixtures.entry()); s.reply(s.script()); val request = s.request("confirmation")
        s.run.cancel(); s.scheduler.drain(); var status: ReplyStatus? = null
        s.run.confirm(request, true) { status = it }; s.scheduler.drain()
        assertEquals(ReplyStatus.NOT_WAITING, status); assertTrue(s.delegate.executions.isEmpty())
    }
    @Test fun escapedDescriptionsAndLargeParametersFitTheBinderEventWithoutClippingValues() {
        val s = Session(ScriptFixtures.entry(description = "description\u0001".repeat(4000)).apply {
            add("parameters", AgentJson.objectOf("""{"type":"object","properties":{"value":{"type":"string"}},"required":[],"additionalProperties":false}"""))
        })
        val value = "x".repeat(16000)
        s.reply(s.script(jsonObject("value" to value.json())))
        val event = s.events.last { it.type == "confirmation" }.payload.apply { addProperty("runId", s.run.id); addProperty("sequence", 100); addProperty("type", "confirmation") }
        assertTrue(StepJournal.bytes(event) <= AiAgentContract.MAX_EVENT_JSON_BYTES)
        assertEquals(value, event.getAsJsonObject("arguments").getAsJsonObject("parameters").string("value"))
        assertTrue(event.string("description")!!.endsWith("...")); s.run.cancel(); s.scheduler.drain()
    }
}
