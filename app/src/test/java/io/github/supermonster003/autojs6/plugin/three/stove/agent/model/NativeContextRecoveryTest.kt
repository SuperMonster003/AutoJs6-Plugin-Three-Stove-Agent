package io.github.supermonster003.autojs6.plugin.three.stove.agent.model

import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class NativeContextRecoveryTest {
    private class Fixture(rounds: Int = 16, maximumBytes: Int = 64 * 1024) {
        val scheduler = VirtualScheduler()
        val catalog = F.catalog()
        val policy = ToolPolicy()
        val target = ModelTarget("p", "profile:test", ModelLocality.REMOTE, ModelProtocol.UNKNOWN, false, 128 * 1024,
            supportsStreaming = true, nativeTools = NativeToolLimits(rounds = rounds))
        val broker = NativeTestBroker()
        val client = ModelClient(broker, target, policy, SchemaFallbacks(DecisionSchema(catalog)), scheduler)
        val format = client.initialFormat(DecisionSchema.degraded())
        val compiler = ContextCompiler(PromptCatalog(F::asset, catalog), catalog, policy, target, format,
            ContextLimits(maximumBytes = maximumBytes))
        val contexts = mutableListOf<RunContext>()
        val tools = FakeTools()
        val events = mutableListOf<RunEvent>()
        val queue = RunQueue(scheduler, catalog, policy, object : RunContextCompiler {
            override fun compile(context: RunContext): ModelInput {
                contexts += context
                return compiler.compile(context)
            }
            override fun observe(tool: String, result: com.google.gson.JsonElement) = compiler.observe(tool, result)
        }, client, tools) { RunnerText(F.asset("runner/texts.json"), it) }

        fun start(limits: BudgetLimits = BudgetLimits(), timeout: Long = 3000) = queue.submit(
            RunOptions("Inspect the successive pages and verify completion", format, limits = limits, modelTimeoutMs = timeout),
            events::add).also { scheduler.drain() }
        fun error(run: AgentRunner) = run.result!!.getAsJsonObject("error").string("code")

        fun pages(count: Int = 8) {
            tools.action = { tool ->
                if (tool.invocation.name == ToolNames.UI_CLICK) true.json()
                else jsonObject("text" to ("page-${tools.executions.size}:" + "x".repeat(20_000)).json())
            }
            fun next(call: TestModelBroker.Call, round: Int) {
                call.usage(round * 100L, round * 10L)
                when {
                    tools.executions.isEmpty() -> NativeTestBroker.tools(call, round,
                        NativeTestBroker.call("action", ToolNames.UI_CLICK, """{"selector":{"text":"Continue"}}"""))
                    tools.executions.size < count -> NativeTestBroker.tools(call, round,
                        NativeTestBroker.call("page-${tools.executions.size}", ToolNames.UI_DUMP))
                    else -> call.done()
                }
            }
            broker.start = { it.started(); next(it, 1) }
            broker.resume = { call, result -> next(call, result.number("round")!!.toInt() + 1) }
        }
    }

    @Test fun largeObservationsRebuildNativeContextWithoutReplayingCompletedActions() {
        val f = Fixture(); f.pages()
        val run = f.start()
        assertEquals(run.result.toString(), RunState.COMPLETED, run.state)
        assertTrue(f.broker.calls.size > 1)
        assertEquals(8, f.tools.executions.size)
        assertEquals(1, f.tools.executions.count { it.first.invocation.name == ToolNames.UI_CLICK })
        assertTrue(f.broker.calls.all { it.request.has("tools") })
        assertEquals(f.broker.calls.size - 1, f.broker.cancels.size)
        f.contexts.drop(1).forEach { context ->
            assertTrue(context.history.isNotEmpty())
            assertTrue(context.observation!!.contains("page-"))
            assertTrue(context.format!!.nativeTools)
        }
        assertEquals(9L, run.result!!.getAsJsonObject("usage").number("modelCalls"))
        assertEquals(990L, run.result!!.getAsJsonObject("usage").number("totalTokens"))
    }

    @Test fun negotiatedRoundLimitStartsANewBoundedNativeRequest() {
        val f = Fixture(rounds = 2); f.pages(6)
        f.tools.action = { true.json() }
        val run = f.start()
        assertEquals(run.result.toString(), RunState.COMPLETED, run.state)
        assertEquals(4, f.broker.calls.size)
        assertEquals(6, f.tools.executions.size)
        assertTrue(f.broker.submissions.all { it.number("round")!! < 2 })
        assertEquals(7L, run.result!!.getAsJsonObject("usage").number("modelCalls"))
    }

    @Test fun rebuildingStartsANewRoundWithTheFullModelTimeout() {
        val f = Fixture(rounds = 1)
        f.batchObservation()
        f.tools.autoExecute = false
        val run = f.start(timeout = 3000)
        f.scheduler.advance(2000)
        f.tools.executions.single().second.succeed(ToolReply(true.json())); f.scheduler.drain()
        // The rebuilt request is a new round: it carries the full model timeout, not the leftover of the first request (P13).
        assertEquals(2, f.broker.calls.size)
        assertEquals(3000L, f.broker.calls.last().request.number("timeoutMs"))
        f.scheduler.advance(2999)
        assertNull(run.result)
        f.scheduler.advance(1)
        assertEquals("MODEL_TIMEOUT", f.error(run))
        assertEquals(1, f.tools.executions.size)
        assertEquals(2, f.broker.cancels.size)
    }

    @Test fun rebuildingRequiresANewModelBudgetAdmission() {
        val f = Fixture(rounds = 1); f.batchObservation()
        val run = f.start(BudgetLimits(maxModelCalls = 1))
        assertEquals(RunState.PARTIAL, run.state)
        assertEquals("BUDGET_EXCEEDED", f.error(run))
        assertEquals(1, f.broker.calls.size)
        assertEquals(1, f.tools.executions.size)
        assertEquals(1, f.broker.cancels.size)
    }

    @Test fun newNativeRequestsStillRequireConfirmationAndIgnoreOldCallbacks() {
        val f = Fixture(rounds = 1); f.batchObservation()
        val first = f.broker.start
        f.broker.start = { call ->
            if (f.broker.calls.size == 1) first(call) else {
                call.started()
                NativeTestBroker.tools(call, 1,
                    NativeTestBroker.call("action", ToolNames.UI_CLICK, """{"selector":{"text":"Continue"}}"""))
            }
        }
        f.tools.metadata = { ToolMetadata(forceConfirmation = it.name == ToolNames.UI_CLICK) }
        val run = f.start()
        assertEquals(RunState.WAITING_CONFIRMATION, run.state)
        assertEquals(1, f.tools.executions.size)
        f.broker.calls.first().fail(RunError.HOST_UNAVAILABLE); f.scheduler.drain()
        assertEquals(RunState.WAITING_CONFIRMATION, run.state)
        run.cancel(); f.scheduler.drain()
        val request = f.events.last { it.type == "confirmation" }.payload.string("requestId")!!
        run.confirm(request, true); f.broker.calls.last().done(); f.scheduler.drain()
        assertEquals(RunState.CANCELLED, run.state)
        assertEquals(1, f.tools.executions.size)
        assertEquals(2, f.broker.cancels.size)
    }

    @Test fun failureAfterRebuildingNeverFallsBackToJson() {
        val f = Fixture(); f.pages()
        val first = f.broker.start
        f.broker.start = { call ->
            if (f.broker.calls.size == 1) first(call)
            else { call.started(); call.fail(RunError.TARGET_UNSUPPORTED) }
        }
        val run = f.start()
        assertEquals("TARGET_UNSUPPORTED", f.error(run))
        assertEquals(2, f.broker.calls.size)
        assertTrue(f.broker.calls.all { it.request.has("tools") })
        assertEquals(1, f.tools.executions.count { it.first.invocation.name == ToolNames.UI_CLICK })
    }

    @Test fun rebuildingCannotResetTheDecisionRepairAllowance() {
        val f = Fixture(rounds = 1)
        f.broker.start = { call ->
            call.started(); call.usage(10, 5)
            NativeTestBroker.tools(call, 1, NativeTestBroker.call("invalid", args = """{"unknown":1}"""))
        }
        val run = f.start()
        assertEquals("DECISION_UNPARSABLE", f.error(run))
        assertEquals(3, f.broker.calls.size)
        assertEquals(3L, run.result!!.getAsJsonObject("usage").number("modelCalls"))
        assertEquals(45L, run.result!!.getAsJsonObject("usage").number("totalTokens"))
        assertTrue(f.tools.inspections.isEmpty())
        assertTrue(f.contexts.drop(1).all { it.repair != null })
    }

    private fun Fixture.batchObservation() {
        broker.start = { call ->
            call.started()
            if (broker.calls.size == 1) NativeTestBroker.tools(call, 1, NativeTestBroker.call("observe", ToolNames.UI_DUMP))
        }
    }
}
