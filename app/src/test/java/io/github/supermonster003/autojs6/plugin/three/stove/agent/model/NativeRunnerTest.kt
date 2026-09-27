package io.github.supermonster003.autojs6.plugin.three.stove.agent.model

import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class NativeRunnerTest {
    private class Fixture(nativeLimits: NativeToolLimits = NativeToolLimits()) {
        val scheduler = VirtualScheduler()
        val catalog = F.catalog()
        val policy = ToolPolicy()
        val target = ModelTarget("p", "profile:test", ModelLocality.REMOTE, ModelProtocol.UNKNOWN, false, 128 * 1024,
            supportsStreaming = true, nativeTools = nativeLimits)
        val broker = NativeTestBroker()
        val client = ModelClient(broker, target, policy, SchemaFallbacks(DecisionSchema(catalog)), scheduler) { true }
        val format = client.initialFormat(DecisionSchema.degraded())
        val compiler = ContextCompiler(PromptCatalog(F::asset, catalog), catalog, policy, target, format)
        val tools = FakeTools()
        val events = mutableListOf<RunEvent>()
        val queue = RunQueue(scheduler, catalog, policy, compiler, client, tools) { RunnerText(F.asset("runner/texts.json"), it) }
        fun start(limits: BudgetLimits = BudgetLimits(), timeout: Long = 3000, mode: ConfirmationMode = ConfirmationMode.DEFAULT) = queue.submit(
            RunOptions("Verify the task", format, limits = limits, modelTimeoutMs = timeout, confirmationMode = mode), events::add).also { scheduler.drain() }
        fun request() = events.last { it.type == "confirmation" }.payload.string("requestId")!!
        fun error(run: AgentRunner) = run.result!!.getAsJsonObject("error").string("code")
        fun batch(vararg calls: com.google.gson.JsonObject) { broker.start = {
            it.started(); it.usage(10, 5); NativeTestBroker.tools(it, 1, *calls)
        } }
        fun done() { broker.resume = { call, _ -> call.usage(25, 9); call.done() } }
    }
    @Test fun multipleToolsUseSeparateJournalStepsButOnlyOneModelCallPerRound() {
        val f = Fixture(); f.batch(NativeTestBroker.call("one"), NativeTestBroker.call("two")); f.done()
        val run = f.start()
        assertEquals(RunState.COMPLETED, run.state)
        assertEquals(2, f.tools.executions.size); assertEquals(1, f.broker.calls.size); assertEquals(1, f.broker.submissions.size)
        assertEquals(3L, run.result!!.number("steps")); assertEquals(2L, run.result!!.number("toolCalls"))
        assertEquals(2L, run.result!!.getAsJsonObject("usage").number("modelCalls"))
        assertEquals(34L, run.result!!.getAsJsonObject("usage").number("totalTokens"))
        val steps = f.events.filter { it.type == "step" }
        assertEquals(listOf("NATIVE_TOOL", "NATIVE_TOOL", "STRICT"), steps.map { it.payload.getAsJsonObject("decision").string("parseMode") })
        assertTrue(f.broker.submissions.single().toString().contains("remaining_budget"))
    }
    @Test fun entireBatchIsValidatedBeforeAnyToolAndSharesTwoRepairRetries() {
        val f = Fixture(); f.batch(NativeTestBroker.call("one"), NativeTestBroker.call("two", args = "{\"unknown\":1}"))
        f.broker.resume = { call, results -> NativeTestBroker.tools(call, results.number("round")!!.toInt() + 1,
            NativeTestBroker.call("bad-${results.number("round")}", args = "{\"unknown\":1}")) }
        val run = f.start()
        assertEquals("DECISION_UNPARSABLE", f.error(run)); assertEquals(1L, run.result!!.number("steps"))
        assertEquals(3L, run.result!!.getAsJsonObject("usage").number("modelCalls"))
        assertTrue(f.tools.inspections.isEmpty()); assertEquals(2, f.broker.submissions.size)
        assertTrue(f.broker.submissions.all { row -> row.getAsJsonArray("results").all { it.asJsonObject.flag("isError") == true } })
        assertEquals(1, f.broker.cancels.size)
    }
    @Test fun confirmationDenialReturnsErrorWithoutExecutionAndThenAllowsModelCompletion() {
        val f = Fixture(); f.batch(NativeTestBroker.call("one")); f.done()
        f.tools.metadata = { ToolMetadata(forceConfirmation = true) }
        val run = f.start(); assertEquals(RunState.WAITING_CONFIRMATION, run.state)
        assertTrue(f.tools.executions.isEmpty()); assertTrue(f.broker.submissions.isEmpty())
        run.confirm(f.request(), false); f.scheduler.drain()
        assertEquals(RunState.COMPLETED, run.state); assertTrue(f.tools.executions.isEmpty())
        val result = f.broker.submissions.single().getAsJsonArray("results").single().asJsonObject
        assertTrue(result.flag("isError")!!); assertTrue(result.string("output")!!.contains("USER_DENIED"))
    }
    @Test fun everyCallInBatchWaitsForItsOwnConfirmation() {
        val f = Fixture(); f.batch(NativeTestBroker.call("one"), NativeTestBroker.call("two")); f.done()
        f.tools.metadata = { ToolMetadata(forceConfirmation = true) }
        val run = f.start()
        run.confirm(f.request(), true); f.scheduler.drain()
        assertEquals(1, f.tools.executions.size); assertTrue(f.broker.submissions.isEmpty())
        run.confirm(f.request(), true); f.scheduler.drain()
        assertEquals(RunState.COMPLETED, run.state); assertEquals(2, f.tools.executions.size)
    }
    @Test fun paymentCannotExecuteAfterDenial() {
        val f = Fixture(); f.batch(NativeTestBroker.call("one", "ui_click", "{\"selector\":{\"text\":\"Pay\"}}"))
        f.tools.metadata = { ToolMetadata(payment = true) }
        f.broker.resume = { call, _ -> call.usage(25, 9); call.done(RunnerFixture.done("blocked", "Denied", unfinished = listOf("Payment denied"), orderStatus = "none")) }
        val run = f.start(); assertEquals(RunState.WAITING_CONFIRMATION, run.state)
        run.confirm(f.request(), false); f.scheduler.drain()
        assertEquals(RunState.BLOCKED, run.state); assertTrue(f.tools.executions.isEmpty())
        assertTrue(f.broker.submissions.single().toString().contains("USER_DENIED"))
    }
    @Test fun sessionApprovalAndFullAccessBothContinueNativeBatchesWithoutRepeatedPrompts() {
        for (fullAccess in listOf(false, true)) {
            val f = Fixture(); f.batch(NativeTestBroker.call("one"), NativeTestBroker.call("two")); f.done()
            f.tools.metadata = { ToolMetadata(forceConfirmation = true) }
            val run = f.start(mode = if (fullAccess) ConfirmationMode.FULL_ACCESS else ConfirmationMode.DEFAULT)
            if (!fullAccess) { run.confirm(f.request(), true, ConfirmationScope.RUN); f.scheduler.drain() }
            assertEquals(RunState.COMPLETED, run.state); assertEquals(2, f.tools.executions.size)
            assertEquals(if (fullAccess) 0 else 1, f.events.count { it.type == "confirmation" })
            assertEquals(1, f.broker.submissions.size)
        }
    }
    @Test fun maximumBatchCompactsEscapedOutputAndKeepsAllCallIds() {
        val f = Fixture(); f.batch(*(1..32).map { NativeTestBroker.call("call-$it") }.toTypedArray()); f.done()
        var count = 0
        f.tools.action = { if (++count == 1) jsonObject("text" to "\"\\\n".repeat(6000).json()) else true.json() }
        val run = f.start(); assertEquals(RunState.COMPLETED, run.state)
        val submitted = f.broker.submissions.single()
        assertEquals(32, submitted.getAsJsonArray("results").size())
        assertTrue(StepJournal.bytes(submitted) <= 128 * 1024)
        assertTrue(submitted.getAsJsonArray("results").first().asJsonObject.string("output")!!.contains("truncated"))
        assertEquals(33L, run.result!!.number("steps"))
    }
    @Test fun impossiblySmallNegotiatedBatchFailsBeforePreparationOrExecution() {
        val f = Fixture(NativeToolLimits(batchBytes = 128)); f.batch(NativeTestBroker.call("one"))
        val run = f.start()
        assertEquals("LIMIT_EXCEEDED", f.error(run)); assertTrue(f.tools.inspections.isEmpty())
        assertEquals(1, f.broker.cancels.size)
    }
    @Test fun printingAToolDecisionInNativeModeConsumesRepairAllowanceWithoutExecution() {
        val f = Fixture(); f.broker.start = { it.started(); it.done(RunnerFixture.tool("device_info")) }
        val run = f.start()
        assertEquals("DECISION_UNPARSABLE", f.error(run)); assertEquals(3, f.broker.calls.size)
        assertTrue(f.tools.inspections.isEmpty())
    }
    @Test fun cancellingWhileWaitingClosesNativeSessionAndLateCallbacksCannotExecute() {
        val f = Fixture(); f.batch(NativeTestBroker.call("one")); f.tools.metadata = { ToolMetadata(forceConfirmation = true) }
        val run = f.start(); val request = f.request()
        f.broker.calls.single().usage(20, 8)
        run.cancel(); f.scheduler.drain()
        run.confirm(request, true); f.broker.calls.single().done(); f.scheduler.drain()
        assertEquals(RunState.CANCELLED, run.state); assertTrue(f.tools.executions.isEmpty()); assertTrue(f.broker.submissions.isEmpty())
        assertEquals(28L, run.result!!.getAsJsonObject("usage").number("totalTokens")); assertEquals(1, f.broker.cancels.size)
    }
    @Test fun cancellationBetweenModelCallbackAndRunnerAcceptanceCannotOrphanThePause() {
        val f = Fixture()
        lateinit var run: AgentRunner
        f.broker.start = { call ->
            call.started(); call.usage(10, 5); NativeTestBroker.tools(call, 1, NativeTestBroker.call("one"))
            run.cancel()
        }
        run = f.queue.submit(RunOptions("Verify", f.format), f.events::add)
        f.scheduler.drain()
        assertEquals(RunState.CANCELLED, run.state); assertTrue(f.tools.inspections.isEmpty())
        assertEquals(1, f.broker.cancels.size)
        assertEquals(15L, run.result!!.getAsJsonObject("usage").number("totalTokens"))
    }
    @Test fun hostDeathWhilePreparingClosesOperationAndPreservesBlockedState() {
        val f = Fixture(); f.batch(NativeTestBroker.call("one")); f.tools.autoPrepare = false
        val run = f.start(); f.broker.calls.single().fail(RunError.HOST_UNAVAILABLE); f.scheduler.drain()
        assertEquals(RunState.BLOCKED, run.state); assertEquals(1, f.tools.inspections.single().second.cancellations)
        assertTrue(f.tools.executions.isEmpty()); assertEquals(1, f.broker.cancels.size)
    }
    @Test fun modelDeadlineWhileConfirmingCannotBeExtendedByTheInteraction() {
        val f = Fixture(); f.batch(NativeTestBroker.call("one")); f.tools.metadata = { ToolMetadata(forceConfirmation = true) }
        val run = f.start(timeout = 1000); f.scheduler.advance(1000)
        assertEquals("MODEL_TIMEOUT", f.error(run)); assertTrue(f.tools.executions.isEmpty()); assertEquals(1, f.broker.cancels.size)
    }
    @Test fun confirmationTimeoutIsAnErrorResultAndStillRequiresASeparateModelAdmission() {
        val f = Fixture(); f.batch(NativeTestBroker.call("one")); f.done(); f.tools.metadata = { ToolMetadata(forceConfirmation = true) }
        val run = f.start(BudgetLimits(confirmationTimeoutMs = 1000)); f.scheduler.advance(1000)
        assertEquals(RunState.COMPLETED, run.state)
        assertTrue(f.broker.submissions.single().toString().contains("USER_TIMEOUT")); assertTrue(f.tools.executions.isEmpty())
    }
    @Test fun modelCallAndStepBudgetsStopNativeBatchesAndCancelThePausedHost() {
        for (limits in listOf(BudgetLimits(maxModelCalls = 1), BudgetLimits(maxSteps = 1))) {
            val f = Fixture(); f.batch(NativeTestBroker.call("one"), NativeTestBroker.call("two")); f.done()
            val run = f.start(limits)
            assertEquals("BUDGET_EXCEEDED", f.error(run)); assertEquals(RunState.PARTIAL, run.state)
            assertEquals(if (limits.maxSteps == 1) 1 else 2, f.tools.executions.size)
            assertTrue(f.broker.submissions.isEmpty()); assertEquals(1, f.broker.cancels.size)
        }
    }
    @Test fun budgetFailureAtFirstPauseAlsoCancelsTheNotYetExecutedNativeTurn() {
        val f = Fixture(); f.broker.start = { it.started(); it.usage(999999, 1); NativeTestBroker.tools(it, 1, NativeTestBroker.call("one")) }
        val run = f.start()
        assertEquals("BUDGET_EXCEEDED", f.error(run)); assertTrue(f.tools.inspections.isEmpty()); assertEquals(1, f.broker.cancels.size)
    }
    @Test fun nativeUnsupportedBeforeToolsFallsBackToJsonAndIsCachedPerTarget() {
        val f = Fixture(); f.broker.start = { it.started()
            if (it.request.has("tools")) it.fail(RunError.TARGET_UNSUPPORTED) else it.done()
        }
        assertEquals(RunState.COMPLETED, f.start().state)
        assertEquals(listOf(true, false), f.broker.calls.map { it.request.has("tools") })
        assertEquals(RunState.COMPLETED, f.start().state); assertFalse(f.broker.calls.last().request.has("tools"))
    }
    @Test fun failureAfterSideEffectNeverRestartsThroughJsonOrReplaysAnAction() {
        val f = Fixture(); f.batch(NativeTestBroker.call("one"))
        f.broker.resume = { call, _ -> call.fail(RunError.TARGET_UNSUPPORTED) }
        val run = f.start()
        assertEquals("TARGET_UNSUPPORTED", f.error(run)); assertEquals(1, f.broker.calls.size); assertEquals(1, f.tools.executions.size)
    }
    @Test fun finalAskReleasesTheNativeConversationAndAnswerStartsAFreshBoundedRequest() {
        val f = Fixture(); f.batch(NativeTestBroker.call("one")); f.broker.resume = { call, _ -> call.usage(25, 9); call.done(RunnerFixture.ask()) }
        val run = f.start(); assertEquals(RunState.WAITING_INPUT, run.state)
        f.broker.start = { it.started(); it.done() }
        val request = f.events.last { it.type == "input" }.payload.string("requestId")!!
        run.respond(request, "Answer".json()); f.scheduler.drain()
        assertEquals(RunState.COMPLETED, run.state); assertEquals(2, f.broker.calls.size)
        assertTrue(f.broker.calls.last().request.toString().contains("Answer"))
    }
}
