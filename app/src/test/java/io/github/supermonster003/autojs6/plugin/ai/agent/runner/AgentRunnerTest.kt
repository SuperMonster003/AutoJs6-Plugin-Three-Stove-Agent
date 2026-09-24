package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.tool
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.done
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.ask
import org.junit.Assert.*
import org.junit.Test

class AgentRunnerTest {
    private fun error(run: AgentRunner) = run.result!!.getAsJsonObject("error").string("code")
    private fun uniqueTerminal(f: RunnerFixture, run: AgentRunner) {
        val events = f.events.filter { it.runId == run.id }
        assertEquals((1L..events.size.toLong()).toList(), events.map { it.sequence })
        assertEquals(1, events.count { it.type == "done" }); assertEquals("done", events.last().type)
        assertTrue(events.all { StepJournal.bytes(it.payload) < 32 * 1024 })
    }
    @Test fun offlineD32SettingsWifiWorkflowObservesActsAndReadsBack() {
        val f = RunnerFixture()
        var foreground = "home"; var wifi = false
        f.tools.action = { prepared ->
            val call = (prepared.invocation.plan as? ToolPlan.Call)?.request
            when (prepared.invocation.name) {
                "app_launch" -> { assertEquals("launchPackage", call!!.method); foreground = call.args[0].asString; true.json() }
                "ui_dump" -> { assertEquals("com.android.settings", foreground); jsonObject("snapshotId" to "settings".json(), "nodeRef" to "#n1".json(), "checked" to wifi.json()) }
                "ui_click" -> { assertEquals("#n1", call!!.args[0].asJsonObject.string("nodeRef")); wifi = !wifi; true.json() }
                "ui_wait_for" -> { assertTrue(prepared.invocation.plan is ToolPlan.Poll); assertTrue(wifi); jsonObject("matched" to true.json()) }
                else -> throw AssertionError("Unexpected tool")
            }
        }
        f.enqueue(tool("app_launch", """{"packageName":"com.android.settings"}"""), tool("ui_dump"),
            tool("ui_click", """{"nodeRef":"#n1","snapshotId":"settings"}"""),
            tool("ui_wait_for", """{"selector":{"text":"Wi-Fi"},"state":"appear"}"""), tool("ui_dump"),
            done(evidence = listOf("The final settings observation reports checked=true")))
        val run = f.start()
        assertEquals(RunState.COMPLETED, run.state); assertTrue(wifi)
        assertEquals(6L, run.result!!.number("steps")); assertEquals(5L, run.result!!.number("toolCalls"))
        assertEquals(6L, run.result!!.getAsJsonObject("usage").number("modelCalls"))
        assertTrue(f.contexts.last().observation!!.contains("\"checked\":true"))
        assertEquals(34L, f.contexts.last().remainingBudget.number("steps"))
        assertEquals(6, f.journal(run).getAsJsonArray("steps").size()); assertTrue(f.queue.runs().isEmpty())
        uniqueTerminal(f, run)
    }
    @Test fun allModelDoneStatusesAreTerminalAndImmutable() {
        for (status in listOf("completed", "partial", "failed")) {
            val f = RunnerFixture(); f.enqueue(done(status)); val run = f.start()
            assertEquals(status, run.state.wire); assertEquals(status, run.result!!.string("status"))
            run.result!!.addProperty("status", "mutated")
            assertEquals(status, run.result!!.string("status")); assertFalse(run.cancel())
            uniqueTerminal(f, run)
        }
    }
    @Test fun malformedDecisionsUseExactlyTwoRepairRetriesAndNoTools() {
        val f = RunnerFixture(); f.enqueue("bad", "{}", "{broken")
        val run = f.start()
        assertEquals(RunState.FAILED, run.state); assertEquals("DECISION_UNPARSABLE", error(run))
        assertEquals(3, f.model.calls.size); assertTrue(f.tools.inspections.isEmpty())
        assertEquals(1L, run.result!!.number("steps")); assertNotNull(f.contexts[1].repair)
        val record = f.journal(run).getAsJsonArray("steps").single().asJsonObject
        assertEquals("error", record.string("kind")); assertEquals("validator", record.getAsJsonObject("decision").string("source"))
        assertEquals(List(3) { "DECISION_UNPARSABLE" }, record.getAsJsonObject("decision").getAsJsonArray("rejections").map { it.asString })
        uniqueTerminal(f, run)
    }
    @Test fun repairCanRecoverAndChargesCallsWithinTheSameStep() {
        val f = RunnerFixture(); f.enqueue(tool("not_a_tool"), done())
        val run = f.start()
        assertEquals(RunState.COMPLETED, run.state); assertEquals(1L, run.result!!.number("steps"))
        val record = f.journal(run).getAsJsonArray("steps")[0].asJsonObject
        assertEquals(1L, record.getAsJsonObject("decision").number("repairs"))
        assertEquals(2L, record.getAsJsonObject("usage").number("modelCalls"))
    }
    @Test fun askAnswersValidateTypeChoicesAndRequestIdentity() {
        for ((kind, invalid, valid) in listOf(Triple("text", "".json(), "office".json()),
            Triple("choice", "missing".json(), "two".json()), Triple("confirm", "yes".json(), true.json()))) {
            val f = RunnerFixture(); f.enqueue(ask(kind, "office")); val run = f.start()
            assertEquals(RunState.WAITING_INPUT, run.state)
            var status: ReplyStatus? = null
            run.respond("wrong", valid) { status = it }; f.scheduler.drain(); assertEquals(ReplyStatus.NOT_WAITING, status)
            run.respond(f.request("input"), invalid) { status = it }; f.scheduler.drain(); assertEquals(ReplyStatus.INVALID, status)
            assertEquals(1, f.model.calls.size)
            val request = f.request("input")
            run.respond(request, valid) { status = it }; f.scheduler.drain(); assertEquals(ReplyStatus.ACCEPTED, status)
            assertEquals(RunState.RUNNING, run.state)
            val response = AgentJson.objectOf(f.contexts.last().observation!!).getAsJsonObject("result")
            assertEquals(valid, response["answer"]); assertEquals(true, response.flag("memoryProposalOnly"))
            run.respond(request, valid) { status = it }; f.scheduler.drain(); assertEquals(ReplyStatus.NOT_WAITING, status)
            f.reply(done()); uniqueTerminal(f, run)
        }
    }
    @Test fun inputAndConfirmationTimeoutsReturnObservationsWithoutExecution() {
        for (confirmation in listOf(false, true)) {
            val f = RunnerFixture()
            f.enqueue(if (confirmation) tool("files_write", """{"path":"file.txt","content":"test"}""") else ask())
            val run = f.start(f.options(BudgetLimits(askTimeoutMs = 5, confirmationTimeoutMs = 5)))
            val request = f.request(if (confirmation) "confirmation" else "input")
            f.scheduler.advance(5)
            assertEquals(RunState.RUNNING, run.state); assertTrue(f.tools.executions.isEmpty())
            assertTrue(f.contexts.last().observation!!.contains("USER_TIMEOUT"))
            var status: ReplyStatus? = null
            run.confirm(request, true) { status = it }; f.scheduler.drain(); assertEquals(ReplyStatus.NOT_WAITING, status)
            f.reply(done("partial")); uniqueTerminal(f, run)
        }
    }
    @Test fun sensitiveOperationsWaitForApprovalAndDenialIsObserved() {
        for (allow in listOf(false, true)) {
            val f = RunnerFixture(); f.enqueue(tool("ui_click", """{"nodeRef":"#n1"}"""))
            f.tools.metadata = { ToolMetadata(RiskContext(nodeText = "Delete document")) }
            val run = f.start()
            assertEquals(RunState.WAITING_CONFIRMATION, run.state); assertTrue(f.tools.executions.isEmpty())
            val event = f.events.last { it.type == "confirmation" }.payload
            assertEquals("sensitive", event.string("risk")); assertTrue(event.string("description")!!.contains("Delete document"))
            run.confirm(f.request("confirmation"), allow); f.scheduler.drain()
            assertEquals(if (allow) 1 else 0, f.tools.executions.size)
            if (!allow) assertTrue(f.contexts.last().observation!!.contains("USER_DENIED"))
            f.reply(done()); uniqueTerminal(f, run)
        }
    }
    @Test fun paymentRejectsRunScopeAndRequiresConfirmationOnEachExecution() {
        val f = RunnerFixture(); f.enqueue(tool("ui_click", """{"nodeRef":"#n1"}"""), tool("ui_click", """{"nodeRef":"#n1"}"""))
        f.tools.metadata = { ToolMetadata(RiskContext(nodeText = "Pay")) }
        val run = f.start(); var status: ReplyStatus? = null
        run.confirm(f.request("confirmation"), true, ConfirmationScope.RUN) { status = it }; f.scheduler.drain()
        assertEquals(ReplyStatus.INVALID, status); assertTrue(f.tools.executions.isEmpty())
        val old = f.request("confirmation")
        run.confirm(old, true); f.scheduler.drain()
        assertEquals(1, f.tools.executions.size); assertEquals(RunState.WAITING_CONFIRMATION, run.state)
        run.confirm(old, true) { status = it }; f.scheduler.drain(); assertEquals(ReplyStatus.NOT_WAITING, status)
        run.confirm(f.request("confirmation"), false); f.scheduler.drain()
        f.reply(done("partial", orderStatus = "pending_payment")); assertEquals(1, f.tools.executions.size); uniqueTerminal(f, run)
    }
    @Test fun cautiousRunScopeReusesOnlyItsToolAndRisk() {
        val f = RunnerFixture()
        f.enqueue(tool("ui_click", """{"nodeRef":"#n1"}"""), tool("ui_click", """{"nodeRef":"#n2"}"""),
            tool("ui_long_click", """{"nodeRef":"#n2"}"""))
        val run = f.start(f.options(mode = ConfirmationMode.CAUTIOUS))
        run.confirm(f.request("confirmation"), true, ConfirmationScope.RUN); f.scheduler.drain()
        assertEquals(2, f.tools.executions.size); assertEquals(RunState.WAITING_CONFIRMATION, run.state)
        assertEquals("ui_long_click", f.events.last { it.type == "confirmation" }.payload.string("tool"))
        run.cancel(); f.scheduler.drain(); uniqueTerminal(f, run)
    }
    @Test fun passwordNeverAppearsInConfirmationStepOrFinalResult() {
        val f = RunnerFixture(); val secret = "test-secret"
        f.tools.metadata = { ToolMetadata(passwordField = true) }
        f.tools.action = { jsonObject("echo" to secret.json()) }
        f.enqueue(tool("ui_set_text", """{"nodeRef":"#n1","text":"$secret"}""", secret), done(summary = secret))
        val run = f.start(f.options(mode = ConfirmationMode.CAUTIOUS))
        run.confirm(f.request("confirmation"), true); f.scheduler.drain()
        assertEquals(RunState.COMPLETED, run.state)
        assertEquals(secret, f.tools.executions.single().first.invocation.arguments.string("text"))
        assertFalse(f.events.joinToString { it.payload.toString() }.contains(secret))
        assertFalse(f.journal(run).toString().contains(secret)); assertEquals("***", run.result!!.string("summary"))
        uniqueTerminal(f, run)
    }
    @Test fun everyBudgetDimensionStopsFurtherWorkAndReportsItsCause() {
        val cases = listOf(BudgetLimits(maxSteps = 1) to tool("ui_dump"),
            BudgetLimits(maxModelCalls = 1) to "bad", BudgetLimits(maxTotalTokens = 1) to done())
        for ((limits, response) in cases) {
            val f = RunnerFixture(); f.enqueue(response); val run = f.start(f.options(limits))
            assertEquals("BUDGET_EXCEEDED", error(run))
            assertEquals(if (f.tools.executions.isEmpty()) RunState.FAILED else RunState.PARTIAL, run.state)
            assertTrue(f.model.calls.size <= 1); uniqueTerminal(f, run)
        }
        val f = RunnerFixture(); f.enqueue(ask()); val run = f.start(f.options(BudgetLimits(maxDurationMs = 20)))
        f.scheduler.advance(20); assertEquals("BUDGET_EXCEEDED", error(run)); assertEquals(20L, run.result!!.number("durationMs"))
        uniqueTerminal(f, run)
    }
    @Test fun actualUsageOverBudgetStopsBeforeToolInspection() {
        val f = RunnerFixture(); val run = f.start(f.options(BudgetLimits(maxTotalTokens = 1000)))
        assertTrue(f.model.outputLimits.single() < 1000)
        f.reply(tool("ui_dump"), ModelUsage(1000, 1))
        assertEquals("BUDGET_EXCEEDED", error(run)); assertTrue(f.tools.inspections.isEmpty())
        assertEquals(1001L, run.result!!.getAsJsonObject("usage").number("totalTokens"))
        uniqueTerminal(f, run)
    }
    @Test fun modelFailureAndTimeoutDoNotRetryAndCancelOnlyTimedOutWork() {
        for (timeout in listOf(false, true)) {
            val f = RunnerFixture(); val run = f.start(f.options(modelTimeout = 5))
            if (timeout) f.scheduler.advance(5) else { f.model.calls.single().fail(RunError.MODEL_FAILED); f.scheduler.drain() }
            assertEquals(if (timeout) "MODEL_TIMEOUT" else "MODEL_FAILED", error(run))
            assertEquals(1, f.model.calls.size); assertEquals(if (timeout) 1 else 0, f.model.calls.single().cancellations)
            assertEquals(true, run.result!!.getAsJsonObject("usage").flag("estimated")); uniqueTerminal(f, run)
        }
    }
    @Test fun oversizedModelReplyFailsWithoutRepairOrToolDispatch() {
        val f = RunnerFixture(); val run = f.start(); f.reply("😀".repeat(20_000))
        assertEquals("LIMIT_EXCEEDED", error(run)); assertEquals(1, f.model.calls.size)
        assertTrue(f.tools.inspections.isEmpty()); uniqueTerminal(f, run)
    }
    @Test fun scriptTimeoutUsesTrustedRegistrationOnlyAndIsRecoverable() {
        for (script in listOf(false, true)) {
            val f = RunnerFixture(); f.tools.autoExecute = false
            f.tools.metadata = { ToolMetadata(scriptTimeoutMs = 50) }
            f.enqueue(if (script) tool("script_run", """{"id":"sample","parameters":{}}""") else tool("ui_dump"))
            val run = f.start(f.options(BudgetLimits(stepToolTimeoutMs = 5)))
            assertEquals(if (script) 50L else 5L, f.tools.timeouts.single())
            f.scheduler.advance(if (script) 50 else 5)
            assertEquals(1, f.tools.executions.single().second.cancellations)
            if (script) { assertEquals(RunState.RUNNING, run.state); assertTrue(f.contexts.last().observation!!.contains("SCRIPT_TIMEOUT")); f.reply(done("partial")) }
            else { assertEquals(RunState.FAILED, run.state); assertEquals("BUDGET_EXCEEDED", error(run)); assertTrue(run.result!!.string("summary")!!.contains("Tool time limit")) }
            uniqueTerminal(f, run)
        }
    }
    @Test fun staleNodeIsAnObservationButHostLossBlocksTheRun() {
        for (failure in listOf(RunError.NODE_REF_STALE, RunError.HOST_UNAVAILABLE, RunError.LINK_DETACHED)) {
            val f = RunnerFixture(); f.tools.autoExecute = false; f.enqueue(tool("ui_click", """{"nodeRef":"#n1"}"""))
            val run = f.start(); f.tools.executions.single().second.fail(failure); f.scheduler.drain()
            if (failure.hostLost) assertEquals(RunState.BLOCKED, run.state)
            else { assertTrue(f.contexts.last().observation!!.contains(failure.name)); f.reply(done("partial")) }
            uniqueTerminal(f, run)
        }
    }
    @Test fun preparationCannotReplaceTheAdmittedInvocation() {
        val f = RunnerFixture(); f.tools.autoPrepare = false; f.enqueue(tool("ui_dump")); val run = f.start()
        val (invocation, pending) = f.tools.inspections.single()
        pending.succeed(PreparedTool(ToolInvocation(invocation.name, invocation.arguments, invocation.plan), ToolMetadata()))
        f.scheduler.drain(); assertEquals("INVALID_REQUEST", error(run)); assertTrue(f.tools.executions.isEmpty()); uniqueTerminal(f, run)
    }
    @Test fun synchronousPortExceptionsBecomeFixedErrorsWithoutPrivateExceptionText() {
        for (phase in listOf("model", "prepare", "execute")) {
            val f = RunnerFixture()
            when (phase) {
                "model" -> f.model.onGenerate = { throw IllegalStateException("private-exception") }
                "prepare" -> { f.enqueue(tool("ui_dump")); f.tools.metadata = { throw IllegalStateException("private-exception") } }
                else -> { f.enqueue(tool("ui_dump")); f.tools.action = { throw IllegalStateException("private-exception") } }
            }
            val run = f.start()
            assertEquals(if (phase == "model") RunState.FAILED else RunState.BLOCKED, run.state)
            assertFalse(f.journal(run).toString().contains("private-exception")); uniqueTerminal(f, run)
        }
    }
    @Test fun heavilyEscapedDoneRetainsResultContractWithinEventLimit() {
        val f = RunnerFixture()
        val decision = AgentJson.objectOf(done(status = "partial", summary = "\u0001".repeat(1000), evidence = List(8) { "\u0002".repeat(200) }))
        decision.getAsJsonObject("done").add("unfinished", JsonArray().apply { repeat(8) { add("\u0003".repeat(200)) } })
        f.enqueue(decision.toString()); val run = f.start()
        assertEquals(RunState.PARTIAL, run.state); assertEquals(run.id, run.result!!.string("id"))
        assertEquals("partial", run.result!!.string("status")); assertTrue(run.result!!.has("usage"))
        assertTrue(StepJournal.bytes(run.result!!) <= 24 * 1024); uniqueTerminal(f, run)
    }
    @Test fun preparationDeadlineCancelsInspectionAndIdentifiesToolBudget() {
        val f = RunnerFixture(); f.tools.autoPrepare = false; f.enqueue(tool("ui_dump"))
        val run = f.start(f.options(BudgetLimits(stepToolTimeoutMs = 5)))
        f.scheduler.advance(5)
        assertEquals("BUDGET_EXCEEDED", error(run)); assertEquals(RunState.FAILED, run.state)
        assertEquals(1, f.tools.inspections.single().second.cancellations); assertTrue(f.tools.executions.isEmpty())
        assertTrue(run.result!!.string("summary")!!.contains("Tool time limit")); uniqueTerminal(f, run)
    }
}
