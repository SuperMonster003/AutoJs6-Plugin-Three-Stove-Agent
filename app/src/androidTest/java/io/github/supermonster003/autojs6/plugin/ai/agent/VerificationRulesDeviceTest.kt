package io.github.supermonster003.autojs6.plugin.ai.agent

import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** E1 checks with packaged prompts and Android scheduling. Model/device ports are controlled fixtures. */
class VerificationRulesDeviceTest {
    private fun asset(path: String) = InstrumentationRegistry.getInstrumentation().targetContext.assets.open(path).bufferedReader().use { it.readText() }
    private class Result(val run: AgentRunner, val calls: Int, val actions: Int, val events: List<RunEvent>)
    private fun execute(decisions: List<String>, goal: String = "Verify the fixture", payment: Boolean = false): Result {
        val catalog = ToolCatalog(asset("catalog/tools.json")); val policy = ToolPolicy.fromAssets(::asset)
        val format = DecisionSchema(catalog).generate(ModelProtocol.LOCAL, policy)
        val target = ModelTarget("fixture", "local:test", ModelLocality.ON_DEVICE, ModelProtocol.LOCAL, true, 65536)
        val calls = AtomicInteger(); val actions = AtomicInteger()
        val model = object : RunModel {
            override fun generate(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long, callback: (PortResult<ModelReply>) -> Unit): Cancellation {
                check(input.inputBytes <= 7500 && Budget.estimate(input.inputBytes) + maximumOutputTokens <= 4096)
                check(input.messages.first().asJsonObject.string("content")!!.contains("orderStatusRequired"))
                callback(PortResult.Success(ModelReply(decisions[calls.getAndIncrement()], ModelUsage(20, 10))))
                return Cancellation.NONE
            }
        }
        val tools = object : RunTools {
            override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
                callback(PortResult.Success(PreparedTool(invocation, ToolMetadata(payment = payment)))); return Cancellation.NONE
            }
            override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
                actions.incrementAndGet()
                callback(PortResult.Success(ToolReply(AgentJson.objectOf("""{"ok":true,"windowChanged":false,"changes":{"changed":false,"partial":false,"baseline":false},"stability":{"observed":true}}"""))))
                return Cancellation.NONE
            }
        }
        SerialRunScheduler().use { scheduler ->
            val compiler = ContextCompiler(PromptCatalog(::asset, catalog), catalog, policy, target, format)
            val queue = RunQueue(scheduler, catalog, policy, compiler, model, tools) { RunnerText(asset("runner/texts.json"), it) }
            val inbox = LinkedBlockingQueue<RunEvent>(); val events = mutableListOf<RunEvent>()
            val run = queue.submit(RunOptions(goal, format), inbox::add)
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
            while (true) {
                val remaining = deadline - System.nanoTime(); assertTrue("Task did not finish", remaining > 0)
                val event = inbox.poll(remaining, TimeUnit.NANOSECONDS); assertNotNull("Missing task event", event); events += event
                when (event.type) {
                    "input" -> run.respond(event.payload.string("requestId")!!, "Latte".json())
                    "confirmation" -> run.confirm(event.payload.string("requestId")!!, false)
                    "done" -> return Result(run, calls.get(), actions.get(), events)
                }
            }
        }
    }
    @Test fun missingCompletionEvidenceIsPartialInThePackagedRunner() {
        val result = execute(listOf("""{"kind":"done","done":{"status":"completed","summary":"Done"}}"""))
        assertEquals(RunState.PARTIAL, result.run.state); assertEquals(1, result.calls); assertEquals(0, result.actions)
        assertFalse(result.run.result!!.getAsJsonArray("unfinished").isEmpty)
        assertTrue(result.events.single { it.type == "step" }.payload.toString().contains("EVIDENCE_MISSING"))
    }
    @Test fun repeatedActionsStopBeforeTheThirdDeviceDispatch() {
        val click = """{"kind":"tool","tool":"ui_click","arguments":{"selector":{"text":"Go"}}}"""
        val result = execute(List(3) { click })
        assertEquals(RunState.BLOCKED, result.run.state); assertEquals(3, result.calls); assertEquals(2, result.actions)
        assertEquals(1, result.events.count { it.type == "done" })
    }
    @Test fun missingInformationPaymentDenialAndOrderRepairPreservePendingPayment() {
        val result = execute(listOf(
            """{"kind":"ask","ask":{"kind":"choice","question":"Which drink?","choices":["Latte","Tea"]}}""",
            """{"kind":"tool","tool":"ui_click","arguments":{"selector":{"text":"Pay"}}}""",
            """{"kind":"done","done":{"status":"partial","summary":"Waiting"}}""",
            """{"kind":"done","done":{"status":"partial","summary":"Waiting for payment","evidence":["Pending payment page"],"unfinished":["Payment declined"],"orderStatus":"pending_payment"}}"""
        ), goal = "Order a drink, stop before payment", payment = true)
        assertEquals(RunState.PARTIAL, result.run.state); assertEquals(4, result.calls); assertEquals(0, result.actions)
        assertEquals("pending_payment", result.run.result!!.string("orderStatus"))
        assertEquals(1, result.events.count { it.type == "input" }); assertEquals(1, result.events.count { it.type == "confirmation" })
        assertEquals(1, result.events.count { it.type == "done" })
    }
}
