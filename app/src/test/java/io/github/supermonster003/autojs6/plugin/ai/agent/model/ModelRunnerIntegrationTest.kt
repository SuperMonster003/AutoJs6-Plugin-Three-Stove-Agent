package io.github.supermonster003.autojs6.plugin.ai.agent.model

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class ModelRunnerIntegrationTest {
    private class Fixture(protocol: ModelProtocol = ModelProtocol.LOCAL, contextLimit: Int = 64 * 1024) {
        val scheduler = VirtualScheduler()
        val catalog = F.catalog()
        val policy = ToolPolicy(ToolGroup.entries.associateWith { it == ToolGroup.USER })
        val format = DecisionSchema(catalog).generate(protocol, policy)
        val target = ModelTarget("provider", if (protocol == ModelProtocol.LOCAL) "local:test" else "profile:test",
            if (protocol == ModelProtocol.LOCAL) ModelLocality.ON_DEVICE else ModelLocality.REMOTE, protocol, true, 64 * 1024, supportsStreaming = true)
        val broker = TestModelBroker()
        val client = ModelClient(broker, target, policy, SchemaFallbacks(DecisionSchema(catalog)), scheduler) { true }
        val compiler = ContextCompiler(PromptCatalog(F::asset, catalog), catalog, policy, target, format, ContextLimits(maximumBytes = contextLimit))
        val events = mutableListOf<RunEvent>()
        val queue = RunQueue(scheduler, catalog, policy, compiler, client, FakeTools()) { RunnerText(F.asset("runner/texts.json"), it) }
        fun start(limits: BudgetLimits = BudgetLimits(), timeout: Long = 3000) = queue.submit(
            RunOptions("Verify the task", format, limits = limits, modelTimeoutMs = timeout), events::add).also { scheduler.drain() }
    }

    @Test fun unsupportedStructuredJsonFallsBackOnceAndNextRunUsesLinkCache() {
        val f = Fixture()
        f.broker.script = { call -> call.started(); call.usage()
            if (f.broker.calls.size == 1) call.fail(RunError.TARGET_UNSUPPORTED) else call.done("```json\n${RunnerFixture.done()}\n```")
        }
        val run = f.start()
        assertEquals(RunState.COMPLETED, run.state)
        assertEquals(listOf(true, false), f.broker.calls.map { it.request.flag("structuredJson") })
        assertEquals(2L, run.result!!.getAsJsonObject("usage").number("modelCalls"))
        assertEquals(30L, run.result!!.getAsJsonObject("usage").number("totalTokens"))
        assertEquals(1L, run.result!!.number("steps"))
        assertEquals(true, f.events.first { it.type == "step" }.payload.getAsJsonObject("decision").flag("degraded"))
        val prompt = f.broker.calls[1].request.getAsJsonArray("messages")[0].asJsonObject.string("content")!!
        assertTrue(prompt.contains("Degraded=true"))
        assertEquals(RunState.COMPLETED, f.start().state)
        assertFalse(f.broker.calls.last().request.flag("structuredJson")!!)
    }

    @Test fun requestRejectedUsesStringParametersAndRuntimeValidationUsesTheNewFormat() {
        val f = Fixture(ModelProtocol.ANTHROPIC)
        assertEquals(ArgumentsEncoding.OBJECT, f.format.argumentsEncoding)
        f.broker.script = { call -> call.started(); call.usage()
            when (f.broker.calls.size) {
                1 -> call.fail(RunError.MODEL_FAILED, "REQUEST_REJECTED")
                2 -> call.done("""{"kind":"tool","tool":"report_progress","arguments":"{\"message\":\"Working\"}"}""")
                else -> call.done(RunnerFixture.done())
            }
        }
        val run = f.start()
        assertEquals(RunState.COMPLETED, run.state)
        assertEquals(2L, run.result!!.number("steps")); assertEquals(3L, run.result!!.getAsJsonObject("usage").number("modelCalls"))
        val schema = f.broker.calls[1].request.getAsJsonObject("responseSchema")
        assertEquals("string", schema.getAsJsonObject("properties").getAsJsonObject("arguments").string("type"))
        val messages = f.broker.calls[2].request.getAsJsonArray("messages")
        val previous = messages.first { it.asJsonObject.string("role") == "assistant" }.asJsonObject.string("content")!!
        val replay = AgentJson.objectOf(previous)
        assertNotNull(replay.string("arguments")); assertFalse(replay.has("degraded"))
    }

    @Test fun formatChangesDoNotResetTwoRepairAttempts() {
        val f = Fixture(ModelProtocol.ANTHROPIC)
        f.broker.script = { call -> call.started(); call.usage()
            when (f.broker.calls.size) {
                2 -> call.fail(RunError.MODEL_FAILED, "REQUEST_REJECTED")
                4 -> call.fail(RunError.TARGET_UNSUPPORTED)
                else -> call.done("bad decision")
            }
        }
        val run = f.start()
        assertEquals(RunState.FAILED, run.state)
        assertEquals("DECISION_UNPARSABLE", run.result!!.getAsJsonObject("error").string("code"))
        assertEquals(5, f.broker.calls.size); assertEquals(1L, run.result!!.number("steps"))
        assertEquals(75L, run.result!!.getAsJsonObject("usage").number("totalTokens"))
        assertTrue(f.broker.calls[2].request.toString().contains("repairAttempt"))
    }

    @Test fun unsupportedAlreadyPlainOrGenericFailureDoesNotRetry() {
        for (error in listOf(RunError.MODEL_FAILED, RunError.MODEL_TIMEOUT, RunError.TARGET_UNAVAILABLE, RunError.TARGET_UNSUPPORTED)) {
            val f = Fixture(ModelProtocol.UNKNOWN)
            f.broker.script = { it.started(); it.fail(error) }
            val run = f.start()
            assertEquals(RunState.FAILED, run.state); assertEquals(error.name, run.result!!.getAsJsonObject("error").string("code"))
            assertEquals(1, f.broker.calls.size)
        }
    }

    @Test fun repeatedSchemaRejectionDoesNotLoop() {
        val f = Fixture(ModelProtocol.GEMINI)
        f.broker.script = { it.started(); it.fail(RunError.MODEL_FAILED, "REQUEST_REJECTED") }
        val run = f.start()
        assertEquals(RunState.FAILED, run.state); assertEquals(2, f.broker.calls.size)
        assertEquals("MODEL_FAILED", run.result!!.getAsJsonObject("error").string("code"))
    }

    @Test fun retryMustPassRemainingCallAndTokenBudgets() {
        for (limit in listOf("calls", "tokens")) {
            val f = Fixture()
            f.broker.script = { it.started(); it.usage(if (limit == "tokens") 6000 else 10); it.fail(RunError.TARGET_UNSUPPORTED) }
            val limits = if (limit == "calls") BudgetLimits(maxModelCalls = 1) else BudgetLimits(maxTotalTokens = 5000)
            val run = f.start(limits)
            assertEquals("BUDGET_EXCEEDED", run.result!!.getAsJsonObject("error").string("code"))
            assertEquals(1, f.broker.calls.size)
        }
    }

    @Test fun runnerDeadlineCancellationAndHostLossKeepAlreadyObservedUsage() {
        for (stop in listOf("timeout", "cancel", "host")) {
            val f = Fixture()
            f.broker.script = { it.started(); it.usage(42, 24) }
            val run = f.start(timeout = 1000)
            when (stop) { "timeout" -> f.scheduler.advance(1000); "cancel" -> run.cancel(); else -> f.queue.hostUnavailable() }
            f.scheduler.drain()
            val usage = run.result!!.getAsJsonObject("usage")
            assertEquals(66L, usage.number("totalTokens")); assertEquals(false, usage.flag("estimated"))
            assertEquals(1, f.broker.cancels.size)
            assertEquals(when (stop) { "timeout" -> RunState.FAILED; "cancel" -> RunState.CANCELLED; else -> RunState.BLOCKED }, run.state)
        }
    }

    @Test fun partialChunksWithoutUsageAreEstimatedEvenWhenRunnerDeadlineWins() {
        val f = Fixture()
        f.broker.script = { it.started(); it.send("chunk", jsonObject("chunkSequence" to 1.json(), "text" to "partial output".json())) }
        val run = f.start(timeout = 1000); f.scheduler.advance(1000)
        val usage = run.result!!.getAsJsonObject("usage")
        assertEquals(Budget.estimate(14), usage.number("outputTokens")); assertEquals(true, usage.flag("estimated"))
    }

    @Test fun impossibleContextFailsBeforeSpendingAModelCall() {
        val f = Fixture(contextLimit = 500)
        val run = f.start()
        assertEquals("LIMIT_EXCEEDED", run.result!!.getAsJsonObject("error").string("code"))
        assertTrue(f.broker.calls.isEmpty()); assertEquals(0L, run.result!!.getAsJsonObject("usage").number("modelCalls"))
    }

    @Test fun progressCallbackCancelBeforeGenerateReturnsStillSettlesUsageOnce() {
        val f = Fixture()
        lateinit var run: AgentRunner
        f.broker.script = { it.started(); it.usage(12, 4); run.cancel() }
        run = f.queue.submit(RunOptions("Cancel during dispatch", f.format)); f.scheduler.drain()
        assertEquals(RunState.CANCELLED, run.state)
        assertEquals(16L, run.result!!.getAsJsonObject("usage").number("totalTokens")); assertEquals(1, f.broker.cancels.size)
    }
}
