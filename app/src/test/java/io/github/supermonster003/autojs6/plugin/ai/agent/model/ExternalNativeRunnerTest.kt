package io.github.supermonster003.autojs6.plugin.ai.agent.model

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class ExternalNativeRunnerTest {
    @Test fun externalErrorTravelsThroughTheSameNativeContinuationWithoutReplayingActions() {
        val name = "mcp_local_echo"
        val catalog = F.catalog().withExternal(listOf(ToolSpec.external(name, "External echo", AgentJson.objectOf("{\"type\":\"object\"}"),
            RiskLevel.SENSITIVE, ExternalToolRoute("local", "echo"))))
        val policy = ToolPolicy(enabledGroups = ToolGroup.entries.associateWith { it == ToolGroup.MCP })
        val scheduler = VirtualScheduler(); val broker = NativeTestBroker(); val tools = FakeTools().apply { autoExecute = false }
        val target = ModelTarget("fixture", "profile:test", ModelLocality.REMOTE, ModelProtocol.UNKNOWN, false, 128 * 1024, nativeTools = NativeToolLimits())
        val model = ModelClient(broker, target, policy, SchemaFallbacks(DecisionSchema(catalog)), scheduler) { true }
        val format = model.initialFormat(DecisionSchema.degraded())
        val compiler = ContextCompiler(PromptCatalog(F::asset, catalog), catalog, policy, target, format)
        val events = mutableListOf<RunEvent>()
        val queue = RunQueue(scheduler, catalog, policy, compiler, model, tools) { RunnerText(F.asset("runner/texts.json"), it) }
        broker.start = { it.started(); NativeTestBroker.tools(it, 1, NativeTestBroker.call("one", name)) }
        broker.resume = { call, _ -> call.done(RunnerFixture.done("failed", "Remote operation failed", evidence = emptyList(), unfinished = listOf("Remote request was rejected"))) }
        val run = queue.submit(RunOptions("Exercise a remote fixture", format), events::add); scheduler.drain()
        assertEquals(RunState.WAITING_CONFIRMATION, run.state); assertTrue(tools.executions.isEmpty())
        val request = events.single { it.type == "confirmation" }.payload.string("requestId")!!
        run.confirm(request, true); scheduler.drain()
        tools.executions.single().second.succeed(ToolReply(jsonObject("isError" to true.json(), "reason" to "REMOTE_REJECTED".json()), error = RunError.TOOL_FAILED))
        scheduler.drain()
        assertEquals(1, broker.calls.size); assertEquals(1, tools.executions.size); assertEquals(RunState.FAILED, run.state)
        val result = broker.submissions.single().getAsJsonArray("results").single().asJsonObject
        assertEquals(true, result.flag("isError"))
        assertEquals(false, AgentJson.objectOf(result.string("output")!!).flag("ok"))
        assertEquals("TOOL_FAILED", events.first { it.type == "step" }.payload.string("error"))
    }
}
