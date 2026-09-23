package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class ScriptInvokerTest {
    private class Source : ScriptCatalogSource {
        val calls = mutableListOf<Pair<BridgeCall, Pending<JsonElement>>>()
        override fun load(call: BridgeCall, callback: (PortResult<JsonElement>) -> Unit): Cancellation {
            val pending = Pending(callback); calls += call to pending; return pending.cancellation
        }
    }
    private fun prepared(): PreparedTool {
        val script = PreparedScript(ScriptFixtures.snapshot(ScriptFixtures.entry()).entries.single(), jsonObject("days" to 30.json(), "label" to "private-office".json()))
        val args = script.arguments()
        val invocation = ToolInvocation("script_run", args, ToolHandlers(F.catalog()).prepare("script_run", args, ToolPolicy()))
        return PreparedTool(invocation, ToolMetadata(script = script, scriptTimeoutMs = 60000), script)
    }
    private fun outcome(kind: String = "success", reported: Boolean = true, result: JsonElement = jsonObject("count" to 3.json())) =
        jsonObject("outcome" to kind.json(), "finished" to true.json(), "executionId" to 42.json(), "resultReported" to reported.json(),
            "result" to result, "consoleTail" to JsonArray(), "consoleCaptureMode" to "global-window".json())

    @Test fun executionUsesInspectedCanonicalPathArgumentsAndManifestAndWaitsForReply() {
        val source = Source(); val prepared = prepared(); val replies = mutableListOf<PortResult<ToolReply>>()
        val cancellation = ScriptInvoker(source, { "run-1" }, "default").execute(prepared, 59000, replies::add)
        assertTrue(replies.isEmpty())
        val (call, pending) = source.calls.single(); val script = prepared.metadata.script!!
        assertEquals("execRegistered", call.method); assertEquals(script.registration.path, call.args[0].asString)
        assertEquals(script.parameters, call.args[1]); val options = call.args[2].asJsonObject
        assertEquals(script.registration.snapshot(), options["expectedManifest"])
        assertEquals("run-1", options.string("agentRunId")); assertEquals(59000L, options.number("timeoutMs"))
        pending.succeed(outcome()); pending.succeed(outcome()); cancellation.cancel()
        assertEquals(1, replies.size); assertEquals(1, source.calls.size)
        assertEquals(3L, (replies.single() as PortResult.Success).value.result.asJsonObject.getAsJsonObject("result").number("count"))
    }
    @Test fun cancellationStopsExactlyTheOwnedTokenAndIgnoresLateResponse() {
        val source = Source(); var callbacks = 0
        val cancellation = ScriptInvoker(source, { "run-1" }, "default").execute(prepared(), 60000) { callbacks++ }
        cancellation.cancel(); cancellation.cancel()
        assertEquals(2, source.calls.size)
        val stop = source.calls.last().first
        assertEquals("engines", stop.module); assertEquals("stop", stop.method)
        assertEquals(source.calls.first().first.args[2].asJsonObject["agentInvocationId"], stop.args[0].asJsonObject["agentInvocationId"])
        source.calls.first().second.succeed(outcome())
        assertEquals(0, callbacks); assertEquals(1, source.calls.first().second.cancellations)
    }
    @Test fun timeoutAndFailureStopTheExecutionAndProduceTypedObservations() {
        for ((kind, error) in listOf("timeout" to RunError.SCRIPT_TIMEOUT, "exception" to RunError.SCRIPT_FAILED, "stopped" to RunError.CANCELLED)) {
            val source = Source(); var reply: PortResult<ToolReply>? = null
            ScriptInvoker(source, { "run-1" }, "default").execute(prepared(), 60000) { reply = it }
            source.calls.first().second.succeed(outcome(kind))
            val value = (reply as PortResult.Success).value
            assertEquals(error, value.script!!.error)
            assertEquals(error.name, value.result.asJsonObject.getAsJsonObject("error").string("code"))
            assertEquals("stop", source.calls.last().first.method)
        }
    }
    @Test fun transportFailureAndMalformedRepliesStillStopAndCannotCompleteTwice() {
        for (bad in listOf<JsonElement?>(null, JsonNull.INSTANCE, outcome().apply { remove("resultReported") },
            outcome().apply { addProperty("result", "x".repeat(65536)) })) {
            val source = Source(); val replies = mutableListOf<PortResult<ToolReply>>()
            ScriptInvoker(source, { "run-1" }, "default").execute(prepared(), 60000, replies::add)
            val pending = source.calls.first().second
            if (bad == null) pending.fail(RunError.SCRIPT_TIMEOUT) else pending.succeed(bad)
            pending.succeed(outcome())
            assertEquals(1, replies.size); assertTrue(replies.single() is PortResult.Failure)
            assertEquals("stop", source.calls.last().first.method)
        }
    }
    @Test fun nullReportIsRetainedButAbsentReportIsOmitted() {
        val script = prepared().metadata.script!!
        assertNull(ScriptOutcome.parse(script, outcome(reported = false, result = JsonNull.INSTANCE)).scriptResult)
        val reported = ScriptOutcome.parse(script, outcome(result = JsonNull.INSTANCE)).scriptResult!!
        assertEquals(script.registration.id, reported.string("id")); assertEquals(script.registration.path, reported.string("path"))
        assertTrue(reported["result"].isJsonNull); assertEquals(42L, reported.number("executionId"))
        reported.addProperty("id", "changed")
        assertEquals(script.registration.id, ScriptOutcome.parse(script, outcome()).scriptResult!!.string("id"))
    }
    @Test fun consoleTailIsBoundedRedactedAndKeepsNewestLinesInOrder() {
        val raw = outcome(result = jsonObject("apiKey" to "result-secret".json(), "nested" to jsonArray("Bearer credential".json())))
        raw.add("consoleTail", JsonArray().apply { repeat(65) { add("$it private-office password=log-secret") } })
        val parsed = ScriptOutcome.parse(prepared().metadata.script!!, raw)
        val tail = parsed.observation.getAsJsonArray("consoleTail")
        assertEquals(40, tail.size()); assertTrue(tail[0].asString.startsWith("25 ")); assertTrue(tail.last().asString.startsWith("64 "))
        val all = parsed.observation.toString()
        for (secret in listOf("private-office", "log-secret", "result-secret", "credential")) assertFalse(all.contains(secret))
        raw.add("consoleTail", JsonArray().apply { repeat(40) { add("\u0001".repeat(1000)) } })
        assertTrue(StepJournal.bytes(ScriptOutcome.parse(prepared().metadata.script!!, raw).observation.getAsJsonArray("consoleTail")) <= 8194)
    }
    @Test fun multilineConsoleEntriesStillRespectTheFortyLineLimit() {
        val raw = outcome().apply { add("consoleTail", jsonArray((0..99).joinToString("\n").json())) }
        val tail = ScriptOutcome.parse(prepared().metadata.script!!, raw).observation.getAsJsonArray("consoleTail")
        assertEquals(40, tail.size()); assertEquals("60", tail[0].asString); assertEquals("99", tail.last().asString)
    }
    @Test fun uninspectedScriptCannotReachTheHost() {
        val source = Source(); val valid = prepared(); var result: PortResult<ToolReply>? = null
        ScriptInvoker(source, { "run-1" }, "default").execute(PreparedTool(valid.invocation, valid.metadata), 1000) { result = it }
        assertEquals(RunError.INVALID_REQUEST, (result as PortResult.Failure).error); assertTrue(source.calls.isEmpty())
    }
}
