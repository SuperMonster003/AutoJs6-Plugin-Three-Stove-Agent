package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import com.google.gson.JsonElement
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.service.BinderRunTools
import org.junit.Assert.*
import org.junit.Test

class AccessibilityPreparationTest {
    private val invocation = ToolInvocation("ui_dump", jsonObject(), ToolPlan.Call(ToolHandlers.bridge("accessibility.dump", jsonArray())))
    private class Fixture {
        val scheduler = VirtualScheduler()
        val requests = mutableListOf<BridgeCall>()
        val pending = mutableListOf<Pending<JsonElement>>()
        val inspected = mutableListOf<Long>()
        val result = mutableListOf<PortResult<PreparedTool>>()
        val preparation = AccessibilityPreparation(scheduler) { request, complete ->
            requests += request
            Pending(complete).also { pending += it }.cancellation
        }
    }
    @Test fun hostStartupCompletesBeforeInspectionAndUsesTheOriginalDeadline() {
        val f = Fixture()
        f.preparation.prepare(5000, f.result::add) { remaining, reply ->
            f.inspected += remaining; reply(PortResult.Success(PreparedTool(invocation, ToolMetadata()))); Cancellation.NONE
        }
        assertTrue(f.inspected.isEmpty())
        assertEquals("accessibility", f.requests.single().module); assertEquals("ensureEnabled", f.requests.single().method)
        assertTrue(f.requests.single().args.isEmpty); assertEquals(5000L, f.requests.single().timeoutMs)
        f.scheduler.advance(1200); f.pending.single().succeed(true.json()); f.scheduler.drain()
        assertEquals(listOf(3800L), f.inspected); assertTrue(f.result.single() is PortResult.Success)
        f.pending.single().succeed(true.json()); f.scheduler.advance(5000)
        assertEquals(1, f.inspected.size); assertEquals(1, f.result.size)
    }
    @Test fun failedStartupReportsManualFallbackWithoutInspectingOrExecuting() {
        val f = Fixture()
        f.preparation.prepare(30000, f.result::add) { _, _ -> fail("Must not inspect after failed startup"); Cancellation.NONE }
        assertEquals(10000L, f.requests.single().timeoutMs)
        f.pending.single().succeed(false.json()); f.scheduler.drain()
        assertEquals(RunError.A11Y_SERVICE_NOT_RUNNING, (f.result.single() as PortResult.Failure).error)
    }
    @Test fun brokerFailureAndDeathArePreserved() {
        for (error in listOf(RunError.HOST_UNAVAILABLE, RunError.CAPABILITY_DENIED)) {
            val f = Fixture()
            f.preparation.prepare(5000, f.result::add) { _, _ -> fail("No inspection"); Cancellation.NONE }
            f.pending.single().fail(error); f.scheduler.drain()
            assertEquals(error, (f.result.single() as PortResult.Failure).error)
        }
    }
    @Test fun cancellationAndTimeoutFenceLateSuccessfulStartup() {
        for (cancel in listOf(true, false)) {
            val f = Fixture()
            val handle = f.preparation.prepare(1000, f.result::add) { _, _ -> fail("Late inspection"); Cancellation.NONE }
            if (cancel) handle.cancel() else f.scheduler.advance(1000)
            f.pending.single().succeed(true.json()); f.scheduler.drain()
            if (cancel) assertTrue(f.result.isEmpty())
            else assertEquals(RunError.BUDGET_EXCEEDED, (f.result.single() as PortResult.Failure).error)
            assertTrue(f.pending.single().cancellations > 0)
        }
    }
    @Test fun stoppedHostServiceMapsToTheAccessibilityFallbackOnly() {
        fun error(module: String, category: String = "unavailable", message: String =
            "AutoJs6 $module bridge requires an enabled accessibility capability provider in the current process.") =
            jsonObject("message" to message.json(), "category" to category.json(), "module" to module.json())
        assertEquals(RunError.A11Y_SERVICE_NOT_RUNNING, BinderRunTools.bridgeError(error("accessibility")))
        assertEquals(RunError.A11Y_SERVICE_NOT_RUNNING, BinderRunTools.bridgeError(error("keys")))
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, BinderRunTools.bridgeError(error("files")))
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, BinderRunTools.bridgeError(error("accessibility", message = "Screenshot requires API 30.")))
        assertEquals(RunError.CAPABILITY_DENIED, BinderRunTools.bridgeError(error("accessibility", "capability-denied")))
    }
    @Test fun setupIsLimitedToOperationsThatNeedAccessibility() {
        fun required(name: String, plan: ToolPlan) = AccessibilityPreparation.required(ToolInvocation(name, jsonObject(), plan))
        assertTrue(AccessibilityPreparation.required(invocation))
        assertTrue(required("screen_capture", ToolPlan.Call(ToolHandlers.bridge("accessibility.screenshot", jsonArray()))))
        assertTrue(required("app_current", ToolPlan.Call(ToolHandlers.bridge("app.currentWindow", jsonArray()))))
        assertTrue(required("ui_press_key", ToolPlan.Call(ToolHandlers.bridge("keys.quickSettings", jsonArray()))))
        assertTrue(required("ui_set_text", ToolPlan.AppendText(jsonObject(), "value")))
        assertFalse(required("device_info", ToolPlan.Call(ToolHandlers.bridge("device.info", jsonArray()))))
        assertFalse(required("script_run_source", ToolPlan.DynamicScript("ai.agent.result(1)", 1000)))
        assertFalse(required("mcp_tool", ToolPlan.External("server", "tool", jsonObject())))
    }
}
