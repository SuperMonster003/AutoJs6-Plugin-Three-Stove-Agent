package io.github.supermonster003.autojs6.plugin.three.stove.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class DynamicScriptToolsTest {
    private class Source : ScriptCatalogSource {
        val calls = mutableListOf<Pair<BridgeCall, Pending<JsonElement>>>()
        override fun load(call: BridgeCall, callback: (PortResult<JsonElement>) -> Unit): Cancellation =
            Pending(callback).also { calls += call to it }.cancellation
    }
    private fun invocation(source: String = "ai.agent.result(42);", timeout: Long = 60000): ToolInvocation {
        val arguments = jsonObject("source" to source.json(), "timeoutMs" to timeout.json())
        return ToolInvocation("script_run_source", arguments, ToolHandlers(F.catalog()).prepare("script_run_source", arguments, F.policy()))
    }
    private fun prepared(adapter: DynamicScriptTools, invocation: ToolInvocation = invocation()): PreparedTool {
        var result: PortResult<PreparedTool>? = null
        adapter.prepare(invocation, 1000) { result = it }
        return (result as PortResult.Success).value
    }
    private fun outcome(kind: String = "success") = jsonObject("outcome" to kind.json(), "finished" to true.json(),
        "executionId" to 7.json(), "resultReported" to true.json(), "result" to 42.json(), "consoleTail" to JsonArray())

    @Test fun defaultDisabledAndOldHostOrNarrowedGrantsCannotExposeSourceExecution() {
        val spec = F.catalog()["script_run_source"]!!
        assertEquals(RiskLevel.SENSITIVE, spec.risk); assertFalse(ToolPolicy().isEnabled(spec))
        val methods = setOf("engines.execScript", "engines.stop")
        val permissions = setOf("engines", "engines.exec", "agent", "agent.exec")
        assertFalse(DynamicScriptSource.available(emptySet(), methods, permissions))
        assertTrue(DynamicScriptSource.available(methods, methods, permissions))
        for (permission in permissions) assertFalse(DynamicScriptSource.available(methods, methods, permissions - permission))
        for (method in methods) assertFalse(DynamicScriptSource.available(methods, methods - method, permissions))
        assertFalse(F.policy().withAvailableTools(setOf("app_current")).isEnabled(spec))
        val source = Source(); val adapter = DynamicScriptTools(FakeTools(), source, false, { "run" }, "default")
        var reply: PortResult<PreparedTool>? = null
        adapter.prepare(invocation(), 1000) { reply = it }
        assertEquals(RunError.CAPABILITY_DENIED, (reply as PortResult.Failure).error); assertTrue(source.calls.isEmpty())
    }
    @Test fun sourceBoundIncludesJsonEscapingAndPreservesUnicode() {
        for (text in listOf("", "  \n", "a\u0000b", "x".repeat(8193), "中".repeat(2731), "\\".repeat(4096), "\ud800")) {
            assertThrows(IllegalArgumentException::class.java) { DynamicScriptSource.validate(text) }
        }
        val text = "// 中文\nconst x = '😀';\nai.agent.result(x);"
        assertEquals(text, DynamicScriptSource.validate(text))
        val arguments = jsonObject("source" to text.json())
        assertEquals(60000L, (ToolHandlers(F.catalog()).prepare("script_run_source", arguments, F.policy()) as ToolPlan.DynamicScript).timeoutMs)
        assertFalse(arguments.has("timeoutMs"))
    }
    @Test fun preparationIsReadOnlyAndExecutionBindsExactSourceTimeoutAndInvocation() {
        val source = Source(); val adapter = DynamicScriptTools(FakeTools(), source, true, { "run-one" }, "default", 40000)
        val approved = prepared(adapter); assertTrue(source.calls.isEmpty())
        assertEquals(40000L, approved.metadata.scriptTimeoutMs)
        val replies = mutableListOf<PortResult<ToolReply>>()
        val cancellation = adapter.execute(approved, 35000, replies::add)
        val (call, pending) = source.calls.single()
        assertEquals("engines", call.module); assertEquals("execScript", call.method)
        assertEquals("ai.agent.result(42);", call.args[1].asString)
        assertEquals(35000L, call.args[2].asJsonObject.number("timeoutMs"))
        assertEquals("run-one", call.args[2].asJsonObject.string("agentRunId"))
        assertTrue(call.permissions.containsAll(listOf("engines.exec", "agent.exec")))
        pending.succeed(outcome()); pending.succeed(outcome()); cancellation.cancel()
        assertEquals(1, replies.size); assertEquals(1, source.calls.size)
        val result = (replies.single() as PortResult.Success).value
        assertEquals(42L, result.result.asJsonObject.number("result")); assertNull(result.script!!.scriptResult)
    }
    @Test fun stopBeforeExecutionIdIsKnownUsesOnlyOwnedTokenAndIgnoresLateResults() {
        val source = Source(); val adapter = DynamicScriptTools(FakeTools(), source, true, { "run" }, "default")
        var callbacks = 0
        val pending = adapter.execute(prepared(adapter), 1000) { callbacks++ }
        pending.cancel(); pending.cancel()
        assertEquals(2, source.calls.size)
        val start = source.calls.first(); val stop = source.calls.last().first
        assertEquals("stop", stop.method)
        assertEquals(start.first.args[2].asJsonObject["agentInvocationId"], stop.args[0].asJsonObject["agentInvocationId"])
        assertEquals(1, start.second.cancellations)
        start.second.succeed(outcome()); assertEquals(0, callbacks)
    }
    @Test fun timeoutExceptionAndMalformedRepliesNeverReportSuccessfulCompletion() {
        for (kind in listOf("timeout", "exception", "stopped", "running")) {
            val source = Source(); val adapter = DynamicScriptTools(FakeTools(), source, true, { "run" }, "default")
            var result: PortResult<ToolReply>? = null
            adapter.execute(prepared(adapter), 1000) { result = it }
            source.calls.first().second.succeed(outcome(kind))
            if (kind == "running") assertEquals(RunError.SCRIPT_FAILED, (result as PortResult.Failure).error)
            else assertNotNull((result as PortResult.Success).value.script!!.error)
            assertEquals("stop", source.calls.last().first.method)
        }
    }
    @Test fun synchronousFailureCancelsReturnedTransportAndStopsExactlyOnce() {
        val calls = mutableListOf<BridgeCall>(); var cancellations = 0; var replies = 0
        val source = ScriptCatalogSource { call, callback ->
            calls += call
            if (call.method == "execScript") callback(PortResult.Failure(RunError.HOST_UNAVAILABLE))
            Cancellation { cancellations++ }
        }
        val adapter = DynamicScriptTools(FakeTools(), source, true, { "run" }, "default")
        adapter.execute(prepared(adapter), 1000) { replies++ }.cancel()
        assertEquals(1, replies); assertEquals(1, cancellations); assertEquals(listOf("execScript", "stop"), calls.map { it.method })
    }
    @Test fun unpreparedSourceCannotStartExecution() {
        val source = Source(); val adapter = DynamicScriptTools(FakeTools(), source, true, { "run" }, "default")
        var reply: PortResult<ToolReply>? = null
        adapter.execute(PreparedTool(invocation(), ToolMetadata()), 1000) { reply = it }
        assertEquals(RunError.INVALID_REQUEST, (reply as PortResult.Failure).error); assertTrue(source.calls.isEmpty())
    }
}
