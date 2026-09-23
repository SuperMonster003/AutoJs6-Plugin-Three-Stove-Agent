package io.github.supermonster003.autojs6.plugin.ai.agent

import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real Android scheduler and packaged assets, injected fake model/device ports; no Binder or real device actions. */
class AgentRunnerDeviceTest {
    private fun asset(path: String) = InstrumentationRegistry.getInstrumentation().targetContext.assets.open(path).bufferedReader().use { it.readText() }
    @Test fun packagedTransactionLabelAlwaysNeedsPaymentConfirmation() {
        val catalog = ToolCatalog(asset("catalog/tools.json")); val policy = ToolPolicy.fromAssets(::asset)
        val gate = ConfirmationGate(policy, ConfirmationMode.DEFAULT)
        val click = checkNotNull(catalog["ui_click"])
        gate.allow(gate.assess(click, ToolMetadata()), ConfirmationScope.RUN)
        val decision = gate.assess(click, ToolMetadata(RiskContext(nodeText = "确认交易")))
        assertEquals(RiskLevel.SENSITIVE, decision.risk)
        assertTrue(decision.required); assertFalse(decision.allowRunScope)
        assertFalse(gate.allow(decision, ConfirmationScope.RUN))
    }
    @Test fun settingsWifiScriptRunsToVerifiedResultOnAndroidScheduler() {
        val catalog = ToolCatalog(asset("catalog/tools.json")); val policy = ToolPolicy.fromAssets(::asset)
        val decisions = ArrayDeque(listOf(
            """{"kind":"tool","tool":"app_launch","arguments":{"packageName":"com.android.settings"}}""",
            """{"kind":"tool","tool":"ui_dump","arguments":{}}""",
            """{"kind":"tool","tool":"ui_click","arguments":{"nodeRef":"#n1"}}""",
            """{"kind":"tool","tool":"ui_dump","arguments":{}}""",
            """{"kind":"done","done":{"status":"completed","summary":"Wi-Fi is enabled","evidence":["checked=true"]}}"""))
        var settings = false; var wifi = false; var readBack = false
        val model = object : RunModel {
            override fun generate(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long, callback: (PortResult<ModelReply>) -> Unit): Cancellation {
                callback(PortResult.Success(ModelReply(decisions.removeFirst(), ModelUsage(10, 10)))); return Cancellation.NONE
            }
        }
        val tools = object : RunTools {
            override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
                callback(PortResult.Success(PreparedTool(invocation, ToolMetadata()))); return Cancellation.NONE
            }
            override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
                when (prepared.invocation.name) {
                    "app_launch" -> settings = true
                    "ui_click" -> { check(settings); wifi = true }
                    "ui_dump" -> if (wifi) readBack = true
                }
                callback(PortResult.Success(ToolReply(jsonObject("checked" to wifi.json())))); return Cancellation.NONE
            }
        }
        SerialRunScheduler().use { scheduler ->
            val done = CountDownLatch(1); val events = mutableListOf<RunEvent>()
            val queue = RunQueue(scheduler, catalog, policy, RunContextCompiler { ModelInput(jsonArray()) }, model, tools) { RunnerText(asset("runner/texts.json"), it) }
            val run = queue.submit(RunOptions("Enable Wi-Fi in settings", DecisionSchema(catalog).generate(ModelProtocol.LOCAL, policy))) {
                events += it; if (it.type == "done") done.countDown()
            }
            assertTrue(done.await(10, TimeUnit.SECONDS)); assertEquals(RunState.COMPLETED, run.state)
            assertTrue(readBack); assertEquals(4L, run.result!!.number("toolCalls"))
            assertEquals((1L..events.size.toLong()).toList(), events.map { it.sequence })
            assertEquals(1, events.count { it.type == "done" })
        }
    }
    @Test fun cancellationSettlesOnceAndAcceptsNoLateCallbacksAfterSchedulerCloses() {
        val catalog = ToolCatalog(asset("catalog/tools.json")); val policy = ToolPolicy.fromAssets(::asset)
        val started = CountDownLatch(1); val done = CountDownLatch(1); val cancels = AtomicInteger(); val terminals = AtomicInteger()
        lateinit var reply: (PortResult<ModelReply>) -> Unit
        val model = object : RunModel {
            override fun generate(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long, callback: (PortResult<ModelReply>) -> Unit): Cancellation {
                reply = callback; started.countDown(); return Cancellation { cancels.incrementAndGet() }
            }
        }
        val tools = object : RunTools {
            override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation = error("Unexpected tool inspection")
            override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation = error("Unexpected tool execution")
        }
        val scheduler = SerialRunScheduler()
        try {
            val queue = RunQueue(scheduler, catalog, policy, RunContextCompiler { ModelInput(jsonArray()) }, model, tools) { RunnerText(asset("runner/texts.json"), it) }
            val run = queue.submit(RunOptions("Cancellation", DecisionSchema(catalog).generate(ModelProtocol.LOCAL, policy))) {
                if (it.type == "done") { terminals.incrementAndGet(); done.countDown() }
            }
            assertTrue(started.await(10, TimeUnit.SECONDS)); assertTrue(run.cancel()); assertTrue(done.await(10, TimeUnit.SECONDS))
            assertEquals(RunState.CANCELLED, run.state); assertEquals(1, cancels.get())
        } finally { scheduler.close() }
        reply(PortResult.Success(ModelReply("{}"))); reply(PortResult.Failure(RunError.HOST_UNAVAILABLE))
        assertEquals(1, terminals.get())
    }
}
