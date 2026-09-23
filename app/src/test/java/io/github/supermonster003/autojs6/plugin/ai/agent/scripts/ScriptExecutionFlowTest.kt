package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.done
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.tool
import org.junit.Assert.*
import org.junit.Test

class ScriptExecutionFlowTest {
    private class Session {
        val scheduler = VirtualScheduler(); val catalog = F.catalog(); val policy = ToolPolicy(); val model = FakeModel()
        val contexts = mutableListOf<RunContext>(); val calls = mutableListOf<Pair<BridgeCall, Pending<JsonElement>>>()
        val registration = ScriptFixtures.entry().apply { addProperty("risk", "readonly"); addProperty("confirm", "never"); addProperty("timeoutMs", 2000) }
        val source = ScriptCatalogSource { call, callback ->
            val pending = Pending(callback); calls += call to pending
            when (call.method) {
                "listScripts" -> pending.succeed(jsonArray(registration))
                "readManifest" -> pending.succeed(registration.deepCopy())
                "stop" -> pending.succeed(true.json())
            }
            pending.cancellation
        }
        val inspected = RegisteredScriptTools(ScriptCatalogClient(scheduler::nowMs), emptySet(), source, FakeTools(), DecisionValidator(catalog), true, scheduler::nowMs)
        val tools = ScriptExecutionTools(inspected, ScriptInvoker(source, { "fixture" }, "default"))
        val queue = RunQueue(scheduler, catalog, policy, RunContextCompiler { contexts += it; ModelInput(JsonArray()) }, model, tools) { RunnerText(F.asset("runner/texts.json"), it) }
        val run = queue.submit(RunOptions("Run the registered task", DecisionSchema.degraded())) {}.also { scheduler.drain() }
        fun reply(value: String) { model.calls.last().succeed(ModelReply(value, ModelUsage(20, 10, 30))); scheduler.drain() }
        fun script() = reply(tool("script_run", """{"id":"clean-downloads","parameters":{}}"""))
        fun report(value: JsonElement = jsonObject("count" to 7.json()), reported: Boolean = true) {
            calls.last { it.first.method == "execRegistered" }.second.succeed(jsonObject("outcome" to "success".json(), "finished" to true.json(),
                "executionId" to 42.json(), "resultReported" to reported.json(), "result" to value, "consoleTail" to JsonArray()))
            scheduler.drain()
        }
        fun stopCount() = calls.count { it.first.method == "stop" }
    }
    @Test fun aReportedResultWaitsForModelDoneAndPreservesScriptIdentity() {
        val s = Session(); s.reply(tool("script_catalog")); s.reply(tool("report_progress", """{"message":"Running"}"""))
        s.script(); assertNull(s.run.result); s.report(); assertNull(s.run.result)
        assertTrue(s.contexts.last().observation!!.contains("resultReported"))
        s.reply(done(summary = "Counted seven items"))
        val result = s.run.result!!; assertEquals("Counted seven items", result.string("summary"))
        val script = result.getAsJsonObject("script")
        assertEquals("clean-downloads", script.string("id")); assertEquals(s.registration.string("path"), script.string("path"))
        assertEquals(42L, script.number("executionId")); assertEquals(7L, script.getAsJsonObject("result").number("count"))
        assertEquals(0, s.stopCount())
    }
    @Test fun multipleScriptsOrAnotherExecutedToolOmitTheSingleScriptShortcut() {
        for (anotherScript in listOf(true, false)) {
            val s = Session(); s.script(); s.report()
            if (anotherScript) { s.script(); s.report() } else s.reply(tool("device_info"))
            s.reply(done()); assertFalse(s.run.result!!.has("script"))
        }
    }
    @Test fun explicitNullIsAResultAndConsoleOnlyIsNot() {
        for (reported in listOf(true, false)) {
            val s = Session(); s.script(); s.report(JsonNull.INSTANCE, reported); s.reply(done())
            assertEquals(reported, s.run.result!!.has("script"))
            if (reported) assertTrue(s.run.result!!.getAsJsonObject("script")["result"].isJsonNull)
        }
    }
    @Test fun cancellingBeforeModelDoneCannotPresentAScriptResultAsCompleted() {
        val s = Session(); s.script(); s.report(); s.run.cancel(); s.scheduler.drain()
        assertEquals(RunState.CANCELLED, s.run.state); assertFalse(s.run.result!!.has("script"))
    }
    @Test fun runnerDeadlineSendsStopAndLateExecutionReplyCannotResumeIt() {
        val s = Session(); s.script(); val pending = s.calls.last().second
        s.scheduler.advance(2000)
        assertEquals(1, s.stopCount()); assertTrue(s.contexts.last().observation!!.contains("SCRIPT_TIMEOUT"))
        assertEquals(1, pending.cancellations); val modelCalls = s.model.calls.size
        s.report(); assertEquals(modelCalls, s.model.calls.size)
        s.reply(done("blocked", "The script timed out")); assertFalse(s.run.result!!.has("script"))
    }
    @Test fun taskCancellationStopsOnceAndFencesLateCallbacks() {
        val s = Session(); s.script(); s.run.cancel(); s.scheduler.drain(); s.report()
        assertEquals(1, s.stopCount()); assertEquals(RunState.CANCELLED, s.run.state); assertEquals(1, s.model.calls.size)
    }
    @Test fun clippingLargeSummaryDoesNotMislabelASmallScriptResult() {
        val script = jsonObject("id" to "example".json(), "path" to "/example.js".json(), "executionId" to 1.json(), "result" to JsonNull.INSTANCE)
        val value = StepJournal().finish(jsonObject("summary" to "\u0001".repeat(12000).json(), "script" to script))
        assertTrue(value.flag("truncated")!!); assertFalse(value.getAsJsonObject("script").has("resultTruncated"))
        assertEquals(script, value["script"])
    }
    @Test fun oversizedTerminalResultIsMarkedAndRetainsIdentity() {
        val s = Session(); s.script(); s.report(jsonObject("large" to "x".repeat(60000).json())); s.reply(done())
        val result = s.run.result!!; val script = result.getAsJsonObject("script")
        assertEquals("clean-downloads", script.string("id")); assertEquals(s.registration.string("path"), script.string("path"))
        assertEquals(42L, script.number("executionId")); assertTrue(script.flag("resultTruncated")!!)
        assertTrue(result.flag("truncated")!!); assertTrue(StepJournal.bytes(result) <= 24 * 1024)
    }
}
