package io.github.supermonster003.autojs6.plugin.ai.agent

import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Packaged prompts and Android threads with the real compiler/client/runner and a scripted broker transport.
 * P2.5 owns Binder attachment/FD transport; these tests are E2 and do not invoke a real model. */
class ModelClientDeviceTest {
    private fun asset(path: String) = InstrumentationRegistry.getInstrumentation().targetContext.assets.open(path).bufferedReader().use { it.readText() }
    private val target = ModelTarget("provider", "local:test", ModelLocality.ON_DEVICE, ModelProtocol.LOCAL, true, 65536)
    private fun noTools() = object : RunTools {
        override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation = error("Unexpected tool")
        override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation = error("Unexpected tool")
    }

    @Test fun localContextFallbackAndUsageRoundTripOnAndroid() {
        val catalog = ToolCatalog(asset("catalog/tools.json")); val policy = ToolPolicy.fromAssets(::asset)
        val schema = DecisionSchema(catalog); val format = schema.generate(ModelProtocol.LOCAL, policy)
        val calls = AtomicInteger(); val structured = mutableListOf<Boolean>()
        val broker = object : ModelBrokerTransport {
            override fun generate(requestJson: String, onEvent: (String) -> Unit) {
                val request = AgentJson.objectOf(requestJson); val number = calls.incrementAndGet()
                structured += request.flag("structuredJson")!!
                val messages = request.getAsJsonArray("messages")
                val size = StepJournal.bytes(messages) + if (request.has("responseSchema")) StepJournal.bytes(request["responseSchema"]) else 0
                check(size <= 7500 && Budget.estimate(size) + request.number("maximumOutputTokens")!! <= 4096)
                var sequence = 0
                fun event(type: String, fields: com.google.gson.JsonObject = com.google.gson.JsonObject()) = onEvent(jsonObject(
                    "requestId" to request["requestId"], "type" to type.json(), "sequence" to (++sequence).json()
                ).apply { fields.entrySet().forEach { add(it.key, it.value) } }.toString())
                event("started")
                event("usage", jsonObject("usage" to jsonObject("inputTokens" to 100.json(), "outputTokens" to 10.json())))
                if (number == 1) event("failed", jsonObject("code" to "TARGET_UNSUPPORTED".json()))
                else event("completed", jsonObject("targetId" to target.targetId.json(), "finishReason" to 1.json(), "text" to
                    "```json\n{\"kind\":\"done\",\"done\":{\"status\":\"completed\",\"summary\":\"Verified fixture\"}}\n```".json()))
            }
            override fun cancel(requestId: String) = Unit
        }
        SerialRunScheduler().use { scheduler ->
            val compiler = ContextCompiler(PromptCatalog(::asset, catalog), catalog, policy, target, format)
            val client = ModelClient(broker, target, policy, SchemaFallbacks(schema), scheduler) { Looper.myLooper() != Looper.getMainLooper() }
            val queue = RunQueue(scheduler, catalog, policy, compiler, client, noTools()) { RunnerText(asset("runner/texts.json"), it) }
            val done = CountDownLatch(1)
            val run = queue.submit(RunOptions("验证任务结果", format)) { if (it.type == "done") done.countDown() }
            assertTrue(done.await(10, TimeUnit.SECONDS)); assertEquals(RunState.COMPLETED, run.state)
            assertEquals(listOf(true, false), structured)
            assertEquals(220L, run.result!!.getAsJsonObject("usage").number("totalTokens"))
            assertEquals(2L, run.result!!.getAsJsonObject("usage").number("modelCalls"))
        }
    }

    @Test fun workerWaitRejectsMainThreadAndRealTimeoutCancelsBroker() {
        val catalog = ToolCatalog(asset("catalog/tools.json")); val policy = ToolPolicy.fromAssets(::asset)
        val schema = DecisionSchema(catalog); val format = schema.generate(ModelProtocol.LOCAL, policy)
        val calls = AtomicInteger(); val cancels = AtomicInteger()
        val broker = object : ModelBrokerTransport {
            override fun generate(requestJson: String, onEvent: (String) -> Unit) { calls.incrementAndGet() }
            override fun cancel(requestId: String) { cancels.incrementAndGet() }
        }
        SerialRunScheduler().use { scheduler ->
            val client = ModelClient(broker, target, policy, SchemaFallbacks(schema), scheduler) { Looper.myLooper() != Looper.getMainLooper() }
            val compiler = ContextCompiler(PromptCatalog(::asset, catalog), catalog, policy, target, format)
            val input = compiler.compile(RunContext("Test timeout", emptyList(), null, null, com.google.gson.JsonObject()))
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertThrows(IllegalStateException::class.java) { client.await(input, 20, 1000) }
            }
            assertEquals(0, calls.get())
            assertEquals(RunError.MODEL_TIMEOUT, (client.await(input, 20, 1000) as PortResult.Failure).error)
            assertEquals(1, calls.get()); assertEquals(1, cancels.get())
        }
    }
}
