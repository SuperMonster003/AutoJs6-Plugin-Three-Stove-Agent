package io.github.supermonster003.autojs6.plugin.three.stove.agent.model

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class TestModelBroker : ModelBrokerTransport {
    class Call(val request: JsonObject, val callback: (String) -> Unit) {
        private var sequence = 0
        fun send(type: String, fields: JsonObject = JsonObject()) = callback(jsonObject(
            "requestId" to request["requestId"], "type" to type.json(), "sequence" to (++sequence).json()
        ).apply { fields.entrySet().forEach { add(it.key, it.value) } }.toString())
        fun started() = send("started")
        fun usage(input: Long = 10, output: Long = 5) = send("usage", jsonObject("usage" to
            jsonObject("inputTokens" to input.json(), "outputTokens" to output.json(), "totalTokens" to (input + output).json())))
        fun done(text: String = RunnerFixture.done()) = send("completed", jsonObject("text" to text.json(),
            "targetId" to request["targetId"], "finishReason" to 1.json()))
        fun fail(code: RunError, reason: String? = null) = send("failed", jsonObject("code" to code.name.json()).apply {
            reason?.let { addProperty("reason", it) }
        })
    }
    val calls = mutableListOf<Call>()
    val cancels = mutableListOf<String>()
    var script: (Call) -> Unit = {}
    override fun generate(requestJson: String, onEvent: (String) -> Unit) {
        Call(AgentJson.objectOf(requestJson, 2 * 1024 * 1024), onEvent).also { calls += it; script(it) }
    }
    override fun cancel(requestId: String) { cancels += requestId }
}

class ModelClientTest {
    private val catalog = F.catalog()
    private val policy = ToolPolicy(ToolGroup.entries.associateWith { it == ToolGroup.USER })
    private val schema = DecisionSchema(catalog)
    private val target = ModelTarget("provider", "local:test", ModelLocality.ON_DEVICE, ModelProtocol.LOCAL, true, 65536, supportsStreaming = true)
    private fun input(format: DecisionFormat = schema.generate(ModelProtocol.LOCAL, policy), maxOutput: Int? = null) =
        ModelInput(jsonArray(jsonObject("role" to "user".json(), "content" to "Test".json())),
            format.responseSchemaJson?.toByteArray(Charsets.UTF_8)?.size ?: 0, format, maxOutput)
    private fun client(broker: TestModelBroker, scheduler: RunScheduler = VirtualScheduler()) =
        ModelClient(broker, target, policy, SchemaFallbacks(schema), scheduler)

    @Test fun successfulStreamMatchesFullTextAndPreservesUsage() {
        val broker = TestModelBroker().apply { script = { call ->
            call.started()
            call.send("chunk", jsonObject("chunkSequence" to 1.json(), "text" to "Hi ".json()))
            call.send("chunk", jsonObject("chunkSequence" to 2.json(), "text" to "世界😀".json()))
            call.usage(); call.done("Hi 世界😀")
        } }
        val replies = mutableListOf<PortResult<ModelReply>>()
        client(broker).generate(input(maxOutput = 24), 40, 3000, replies::add)
        val reply = (replies.single() as PortResult.Success).value
        assertEquals("Hi 世界😀", reply.text); assertEquals(ModelUsage(10, 5, 15), reply.usage)
        val request = broker.calls.single().request
        assertEquals(24L, request.number("maximumOutputTokens")); assertEquals(3000L, request.number("timeoutMs"))
        assertEquals(true, request.flag("stream")); assertEquals(true, request.flag("structuredJson"))
        assertEquals(input().format!!.responseSchemaJson, request["responseSchema"].toString())
        assertTrue(broker.cancels.isEmpty())
        assertEquals(setOf("requestId", "targetId", "messages", "structuredJson", "responseSchema", "maximumOutputTokens", "stream", "timeoutMs"), request.keySet())
    }

    @Test fun plainNonStreamingResponseCanOmitUsage() {
        val broker = TestModelBroker().apply { script = { it.started(); it.done("Text") } }
        val value = client(broker).awaitReply(input(DecisionSchema.degraded(ModelProtocol.LOCAL)), 20, 1000)
        assertNull((value as PortResult.Success).value.usage)
        assertFalse(broker.calls.single().request.has("responseSchema"))
    }

    @Test fun missingStartedGappedSequencesOutOfOrderChunksAndMismatchedIdsAreRejected() {
        val scripts: List<(TestModelBroker.Call) -> Unit> = listOf(
            { it.done() },
            { it.started(); it.send("chunk", jsonObject("chunkSequence" to 2.json(), "text" to "x".json())) },
            { it.started(); it.send("chunk", jsonObject("sequence" to 3.json(), "chunkSequence" to 1.json(), "text" to "x".json())) },
            { it.started(); it.send("chunk", jsonObject("requestId" to "wrong".json(), "chunkSequence" to 1.json(), "text" to "x".json())) },
            { it.started(); it.started() },
            { it.started(); it.send("usage", jsonObject("usage" to JsonObject())); it.send("chunk", jsonObject("chunkSequence" to 1.json(), "text" to "x".json())) },
            { it.started(); it.send("chunk", jsonObject("chunkSequence" to 1.json(), "text" to "x".json())); it.done("different") },
            { it.started(); it.send("completed", jsonObject("text" to "x".json(), "targetId" to "local:other".json(), "finishReason" to 1.json())) },
        )
        scripts.forEach { script ->
            val broker = TestModelBroker().apply { this.script = script }
            assertEquals(RunError.INVALID_REQUEST, (client(broker).awaitReply(input(), 20, 1000) as PortResult.Failure).error)
            assertEquals(listOf(broker.calls.single().request.string("requestId")), broker.cancels)
        }
    }

    @Test fun duplicateTerminalIsRejectedWithoutRetractingOrRepeatingPublishedResult() {
        val collector = ModelEventSequence("test", "local:test", 65536)
        assertSame(ModelEventSequence.Event.More, collector.accept("""{"requestId":"test","type":"started","sequence":1}"""))
        assertTrue(collector.accept("""{"requestId":"test","type":"completed","sequence":2,"text":"ok","targetId":"local:test","finishReason":1}""") is ModelEventSequence.Event.Terminal)
        assertTrue(collector.accept("""{"requestId":"test","type":"completed","sequence":3,"text":"again","targetId":"local:test","finishReason":1}""") is ModelEventSequence.Event.Rejected)
        val broker = TestModelBroker().apply { script = { it.started(); it.done("ok"); it.done("again") } }
        val replies = mutableListOf<PortResult<ModelReply>>()
        client(broker).generate(input(), 20, 1000, replies::add)
        assertEquals("ok", (replies.single() as PortResult.Success).value.text)
        assertEquals(1, broker.cancels.size)
    }

    @Test fun malformedTypesDuplicateKeysAndUnboundedUsageFailClosed() {
        val payloads = listOf(
            """{"usage":{"inputTokens":-1}}""", """{"usage":{"inputTokens":"10"}}""",
            """{"usage":{"inputTokens":1.5}}""", """{"usage":{"totalTokens":9223372036854775808}}""",
            """{"usage":{"outputTokens":null}}""", """{"usage":{"private":1}}"""
        )
        payloads.forEach { payload ->
            val broker = TestModelBroker().apply { script = { it.started(); it.send("usage", AgentJson.objectOf(payload)) } }
            assertEquals(RunError.INVALID_REQUEST, (client(broker).awaitReply(input(), 20, 1000) as PortResult.Failure).error)
        }
        val broker = TestModelBroker().apply { script = { it.callback("""{"type":"started","type":"completed"}""") } }
        assertEquals(RunError.INVALID_REQUEST, (client(broker).awaitReply(input(), 20, 1000) as PortResult.Failure).error)
    }

    @Test fun outputBytesAreBoundedBeforeAccumulatingOrAcceptingCompletion() {
        for (stream in listOf(false, true)) {
            val broker = TestModelBroker().apply { script = { it.started()
                if (stream) it.send("chunk", jsonObject("chunkSequence" to 1.json(), "text" to "中".repeat(22_000).json()))
                else it.done("中".repeat(22_000))
            } }
            assertEquals(RunError.LIMIT_EXCEEDED, (client(broker).awaitReply(input(), 20, 1000) as PortResult.Failure).error)
        }
    }

    @Test fun failureRetainsUsageAndOnlyAllowsStableFallbackReason() {
        val broker = TestModelBroker().apply { script = { it.started(); it.usage(); it.fail(RunError.MODEL_FAILED, "REQUEST_REJECTED") } }
        val failure = client(broker).awaitReply(input(), 20, 1000) as PortResult.Failure
        assertEquals(RunError.MODEL_FAILED, failure.error); assertEquals("REQUEST_REJECTED", failure.reason)
        assertEquals(ModelUsage(10, 5, 15), failure.usage)
        broker.script = { it.started(); it.fail(RunError.MODEL_FAILED, "PROVIDER_FAILED") }
        // Fixed host reasons other than REQUEST_REJECTED are kept for the terminal summary; only REQUEST_REJECTED drives a format fallback.
        assertEquals("PROVIDER_FAILED", (client(broker).awaitReply(input(), 20, 1000) as PortResult.Failure).reason)
        broker.script = { it.started(); it.fail(RunError.MODEL_FAILED, "private error body") }
        assertEquals(RunError.INVALID_REQUEST, (client(broker).awaitReply(input(), 20, 1000) as PortResult.Failure).error)
    }

    @Test fun timeoutAndExplicitCancellationEachSettleOnceAndIgnoreLateEvents() {
        for (timeout in listOf(false, true)) {
            val broker = TestModelBroker().apply { script = { it.started(); it.usage() } }
            val scheduler = VirtualScheduler()
            val replies = mutableListOf<PortResult<ModelReply>>()
            val handle = client(broker, scheduler).generate(input(), 20, 1000, replies::add)
            if (timeout) scheduler.advance(1000) else { handle.cancel(); handle.cancel(); scheduler.advance(1000) }
            broker.calls.single().done()
            val failure = replies.single() as PortResult.Failure
            assertEquals(if (timeout) RunError.MODEL_TIMEOUT else RunError.CANCELLED, failure.error)
            assertEquals(ModelUsage(10, 5, 15), failure.usage)
            assertEquals(1, broker.cancels.size)
        }
    }

    @Test fun requestValidationHappensBeforeDispatchAndShortDeadlineIsNotRoundedUp() {
        val broker = TestModelBroker()
        val client = client(broker)
        assertEquals(RunError.MODEL_TIMEOUT, (client.awaitReply(input(), 20, 999) as PortResult.Failure).error)
        assertEquals(RunError.INVALID_REQUEST, (client.awaitReply(input(), 0, 1000) as PortResult.Failure).error)
        val bad = ModelInput(jsonArray(jsonObject("role" to "assistant".json(), "content" to "x".json())), format = DecisionSchema.degraded(ModelProtocol.LOCAL))
        assertEquals(RunError.INVALID_REQUEST, (client.awaitReply(bad, 20, 1000) as PortResult.Failure).error)
        assertTrue(broker.calls.isEmpty())
    }

    @Test fun interruptedWaiterCancelsTheCallAndKeepsItsInterruptFlag() {
        val ready = CountDownLatch(1)
        val broker = TestModelBroker().apply { script = { it.started(); ready.countDown() } }
        val result = AtomicReference<PortResult<ModelReply>>()
        val restored = AtomicReference<Boolean>()
        val worker = Thread { result.set(client(broker).awaitReply(input(), 20, 30_000)); restored.set(Thread.currentThread().isInterrupted) }
        worker.start(); assertTrue(ready.await(3, TimeUnit.SECONDS)); worker.interrupt(); worker.join(3000)
        assertFalse(worker.isAlive); assertEquals(true, restored.get())
        assertEquals(RunError.CANCELLED, (result.get() as PortResult.Failure).error); assertEquals(1, broker.cancels.size)
    }

    @Test fun deadlineTimeoutKeepsObservedUsageAndCancelsTheBroker() {
        val broker = TestModelBroker().apply { script = { it.started(); it.usage() } }
        val scheduler = VirtualScheduler()
        val result = client(broker, scheduler).awaitReply(input(), 20, 1000, graceMs = 200) { scheduler.advance(1000) } as PortResult.Failure
        assertEquals(RunError.MODEL_TIMEOUT, result.error); assertEquals(ModelUsage(10, 5, 15), result.usage)
        assertEquals(1, broker.cancels.size)
    }

    @Test fun transportExceptionsAreSanitizedAndCancelExceptionsAreIsolated() {
        val broker = object : ModelBrokerTransport {
            override fun generate(requestJson: String, onEvent: (String) -> Unit) { error("private exception") }
            override fun cancel(requestId: String) { error("another private exception") }
        }
        val client = ModelClient(broker, target, policy, SchemaFallbacks(schema), VirtualScheduler())
        assertEquals(RunError.HOST_UNAVAILABLE, (client.awaitReply(input(), 20, 1000) as PortResult.Failure).error)
    }

    @Test fun concurrentCancellationAndTerminalEventsPublishExactlyOnce() {
        SerialRunScheduler().use { scheduler ->
            repeat(40) {
                val broker = TestModelBroker().apply { script = { it.started() } }
                val replies = java.util.concurrent.CopyOnWriteArrayList<PortResult<ModelReply>>()
                val handle = client(broker, scheduler).generate(input(), 20, 10_000, replies::add)
                val start = CountDownLatch(1)
                val completing = Thread { start.await(); broker.calls.single().done("ok") }
                val cancelling = Thread { start.await(); handle.cancel() }
                completing.start(); cancelling.start(); start.countDown()
                completing.join(3000); cancelling.join(3000)
                assertFalse(completing.isAlive || cancelling.isAlive)
                assertEquals(1, replies.size); assertTrue(broker.cancels.size <= 1)
            }
        }
    }

    @Test fun nonStreamingTargetsUseAskAndMissingOutputControlIsNotMisclassifiedAsSchemaFailure() {
        val broker = TestModelBroker().apply { script = { it.started(); it.done("ok") } }
        val singleShot = ModelTarget("provider", "local:test", ModelLocality.ON_DEVICE, ModelProtocol.LOCAL, true, 65536)
        val client = ModelClient(broker, singleShot, policy, SchemaFallbacks(schema), VirtualScheduler())
        assertTrue(client.awaitReply(input(), 20, 1000) is PortResult.Success)
        assertEquals(false, broker.calls.single().request.flag("stream"))
        val unbounded = ModelTarget("provider", "local:test", ModelLocality.ON_DEVICE, ModelProtocol.LOCAL, true, 65536, supportsOutputLimit = false)
        val unavailable = ModelClient(broker, unbounded, policy, SchemaFallbacks(schema), VirtualScheduler())
        val failure = unavailable.awaitReply(input(), 20, 1000) as PortResult.Failure
        assertEquals(RunError.TARGET_UNSUPPORTED, failure.error)
        assertNull(unavailable.fallbackFormat(input().format!!, failure)); assertEquals(1, broker.calls.size)
    }
}
