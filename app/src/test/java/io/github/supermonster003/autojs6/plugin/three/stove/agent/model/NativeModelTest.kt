package io.github.supermonster003.autojs6.plugin.three.stove.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

internal class NativeTestBroker : ModelBrokerTransport {
    val calls = mutableListOf<TestModelBroker.Call>()
    val submissions = mutableListOf<JsonObject>()
    val cancels = mutableListOf<String>()
    var start: (TestModelBroker.Call) -> Unit = {}
    var resume: (TestModelBroker.Call, JsonObject) -> Unit = { _, _ -> }
    override fun generate(requestJson: String, onEvent: (String) -> Unit) {
        TestModelBroker.Call(AgentJson.objectOf(requestJson, 2 * 1024 * 1024), onEvent).also { calls += it; start(it) }
    }
    override fun submitToolResults(requestJson: String, onFailure: (RunError) -> Unit) {
        val value = AgentJson.objectOf(requestJson, 128 * 1024); submissions += value
        resume(calls.single { it.request.string("requestId") == value.string("requestId") }, value)
    }
    override fun cancel(requestId: String) { cancels += requestId }
    companion object {
        fun call(id: String, name: String = "device_info", args: String = "{}") = jsonObject("callId" to id.json(), "name" to name.json(), "arguments" to AgentJson.objectOf(args))
        fun tools(call: TestModelBroker.Call, round: Int = 1, vararg entries: JsonObject) = call.send("tool_calls",
            jsonObject("round" to round.json(), "calls" to JsonArray().apply { entries.forEach(::add) }))
    }
}

class NativeModelTest {
    private val catalog = F.catalog()
    private val policy = ToolPolicy()
    private val scheduler = VirtualScheduler()
    private val broker = NativeTestBroker()
    private fun target(limits: NativeToolLimits? = NativeToolLimits()) = ModelTarget("p", "profile:test", ModelLocality.REMOTE,
        ModelProtocol.UNKNOWN, false, 128 * 1024, supportsStreaming = true, nativeTools = limits)
    private fun client(selected: ModelTarget = target()) = ModelClient(broker, selected, policy, SchemaFallbacks(DecisionSchema(catalog)), scheduler) { true }
    private fun input(maximum: Int = 128 * 1024) = ModelInput(jsonArray(jsonObject("role" to "user".json(), "content" to "Test".json())),
        format = DecisionSchema.native(ModelProtocol.UNKNOWN), tools = catalog.nativeDefinitions(policy, "en"), maximumContextBytes = maximum)
    private fun begin(timeout: Long = 3000, maximum: Int = 128 * 1024): Pair<NativeToolTurn, MutableList<PortResult<ModelReply>>> {
        broker.start = { it.started(); it.usage(10, 5); NativeTestBroker.tools(it, 1, NativeTestBroker.call("one")) }
        val replies = mutableListOf<PortResult<ModelReply>>()
        client().generate(input(maximum), 100, timeout, replies::add)
        return (replies.single() as PortResult.Success).value.nativeTurn!! to replies
    }

    @Test fun nativeNegotiationRequiresBothTargetCapabilityAndKnownBrokerExtension() {
        val info = AgentJson.objectOf("""{"toolCallingVersion":1,"maximumToolRounds":16,"maximumToolResultBytes":65536,"maximumToolResultBatchBytes":131072}""")
        val entry = AgentJson.objectOf("""{"targetId":"profile:test","locality":2,"configured":true,"available":true,"maximumContextBytes":65536,"capabilityIds":["tools"],"supportedControls":["maximum-output-tokens"]}""")
        assertNull(ModelTarget.fromCatalog("p", entry).nativeTools)
        assertNotNull(ModelTarget.fromCatalog("p", entry, brokerTools = NativeToolLimits.fromBroker(info)).nativeTools)
        entry.add("capabilityIds", JsonArray())
        assertNull(ModelTarget.fromCatalog("p", entry, brokerTools = NativeToolLimits.fromBroker(info)).nativeTools)
        info.addProperty("toolCallingVersion", 2); assertNull(NativeToolLimits.fromBroker(info))
        assertTrue(client(target(null)).initialFormat(DecisionSchema.degraded()).degraded)
        assertTrue(client().initialFormat(DecisionSchema.degraded()).nativeTools)
    }
    @Test fun toolDefinitionsAndTheirBytesComeFromTheEnabledCatalog() {
        val format = client().initialFormat(DecisionSchema.degraded())
        val compiler = ContextCompiler(PromptCatalog(F::asset, catalog), catalog, policy, target(), format)
        val compiled = compiler.compile(RunContext("Test", emptyList(), null, null, JsonObject()))
        assertEquals(catalog.nativeDefinitions(policy, "en"), compiled.tools)
        assertFalse(compiled.tools.any { it.asJsonObject.string("name") == "shell_exec" })
        assertEquals(StepJournal.bytes(compiled.messages) + StepJournal.bytes(compiled.tools), compiled.inputBytes)
        assertFalse(format.degraded)
        assertNull(format.responseSchemaJson)
    }
    @Test fun sameBrokerCallbackContinuesAndCumulativeUsageBecomesRoundDeltas() {
        val (turn, replies) = begin()
        val request = broker.calls.single().request
        assertEquals(catalog.nativeDefinitions(policy, "en"), request.getAsJsonArray("tools"))
        assertFalse(request.flag("structuredJson")!!); assertFalse(request.has("responseSchema"))
        assertEquals(ModelUsage(10, 5, 15), (replies.single() as PortResult.Success).value.usage)
        broker.resume = { call, _ -> call.usage(25, 9); call.usage(25, 9); call.done() }
        val results = listOf(NativeToolResult("one", "{}", false))
        assertTrue(turn.continuation.inputBytes(results) > input().inputBytes)
        turn.continuation.resume(results, 100, 3000, replies::add)
        assertEquals(1, broker.calls.size); assertEquals(1, broker.submissions.size)
        assertEquals(ModelUsage(15, 4, 19), (replies.last() as PortResult.Success).value.usage)
        assertNull((replies.last() as PortResult.Success).value.nativeTurn)
        assertTrue(broker.cancels.isEmpty())
    }
    @Test fun preambleIsVerifiedButOnlyFinalTurnTextIsParsed() {
        val replies = mutableListOf<PortResult<ModelReply>>()
        broker.start = { it.started(); it.send("chunk", jsonObject("chunkSequence" to 1.json(), "text" to "Looking.".json()))
            NativeTestBroker.tools(it, 1, NativeTestBroker.call("one")) }
        client().generate(input(), 100, 3000, replies::add)
        val turn = (replies.single() as PortResult.Success).value.nativeTurn!!
        broker.resume = { call, _ -> call.send("chunk", jsonObject("chunkSequence" to 2.json(), "text" to RunnerFixture.done().json()))
            call.done("Looking." + RunnerFixture.done()) }
        turn.continuation.resume(listOf(NativeToolResult("one", "{}", false)), 100, 3000, replies::add)
        assertEquals(RunnerFixture.done(), (replies.last() as PortResult.Success).value.text)
    }
    @Test fun originalDeadlineIncludesTimeWaitingForResults() {
        val (turn, replies) = begin()
        val failures = mutableListOf<RunError>(); turn.continuation.onFailure(failures::add)
        scheduler.advance(2999)
        turn.continuation.resume(listOf(NativeToolResult("one", "{}", false)), 100, 3000, replies::add)
        scheduler.advance(1)
        assertEquals(RunError.MODEL_TIMEOUT, (replies.last() as PortResult.Failure).error)
        assertEquals(1, broker.cancels.size); assertTrue(failures.isEmpty())
    }
    @Test fun pausedTimeoutAndLateHostFailureReachTheRunListenerExactlyOnce() {
        val (turn, _) = begin()
        val failures = mutableListOf<RunError>(); turn.continuation.onFailure(failures::add)
        scheduler.advance(3000); broker.calls.single().fail(RunError.HOST_UNAVAILABLE); turn.continuation.cancel()
        assertEquals(listOf(RunError.MODEL_TIMEOUT), failures); assertEquals(1, broker.cancels.size)
    }
    @Test fun malformedSequenceDuplicateIdsUnknownToolsAndRegressingUsageFailBeforeAnyContinuation() {
        val mutations: List<(TestModelBroker.Call) -> Unit> = listOf(
            { NativeTestBroker.tools(it, 2, NativeTestBroker.call("x")) },
            { NativeTestBroker.tools(it, 1, NativeTestBroker.call("x"), NativeTestBroker.call("x")) },
            { NativeTestBroker.tools(it, 1, NativeTestBroker.call("x", "shell_exec")) },
            { it.usage(10, 5); it.usage(9, 5) },
            { it.send("chunk", jsonObject("chunkSequence" to 2.json(), "text" to "x".json())) },
        )
        mutations.forEach { mutate ->
            val replies = mutableListOf<PortResult<ModelReply>>()
            broker.start = { it.started(); mutate(it) }
            client().generate(input(), 100, 3000, replies::add)
            assertEquals(RunError.INVALID_REQUEST, (replies.single() as PortResult.Failure).error)
        }
        assertTrue(broker.submissions.isEmpty())
    }
    @Test fun duplicateBatchWhilePausedAndRepeatedCallIdAcrossRoundsAreRejected() {
        val (turn, _) = begin()
        val failures = mutableListOf<RunError>(); turn.continuation.onFailure(failures::add)
        NativeTestBroker.tools(broker.calls.single(), 2, NativeTestBroker.call("two"))
        assertEquals(listOf(RunError.INVALID_REQUEST), failures)
        val (second, replies) = begin()
        broker.resume = { call, _ -> NativeTestBroker.tools(call, 2, NativeTestBroker.call("one")) }
        second.continuation.resume(listOf(NativeToolResult("one", "{}", false)), 100, 3000, replies::add)
        assertEquals(RunError.INVALID_REQUEST, (replies.last() as PortResult.Failure).error)
    }
    @Test fun resultBatchMustMatchAndLoweredOutputBudgetCannotBypassReservation() {
        val (turn, replies) = begin()
        assertThrows(IllegalArgumentException::class.java) { turn.continuation.inputBytes(listOf(NativeToolResult("other", "{}", false))) }
        turn.continuation.resume(listOf(NativeToolResult("one", "{}", false)), 99, 3000, replies::add)
        assertEquals(RunError.BUDGET_EXCEEDED, (replies.last() as PortResult.Failure).error)
        assertTrue(broker.submissions.isEmpty()); assertEquals(1, broker.cancels.size)
    }
    @Test fun resumedContextIncludesDefinitionsResultsAndPriorModelOutput() {
        val limit = input().inputBytes + 20
        val (turn, _) = begin(maximum = limit)
        assertThrows(ContextLimitExceeded::class.java) { turn.continuation.inputBytes(listOf(NativeToolResult("one", "x".repeat(30), false))) }
        turn.continuation.cancel(); assertTrue(broker.submissions.isEmpty())
    }
    @Test fun idleUsageCanBeConsumedOnceWithoutRechargingEarlierRounds() {
        val (turn, _) = begin()
        broker.calls.single().usage(20, 8)
        assertEquals(ModelUsage(10, 3, 13), turn.continuation.takeProgress().usage)
        assertEquals(ModelUsage(0, 0, 0), turn.continuation.takeProgress().usage)
        turn.continuation.cancel()
    }
    @Test fun totalTokensAppearingInALaterSnapshotDoesNotChargeEarlierInputAndOutputAgain() {
        broker.start = { call -> call.started()
            call.send("usage", jsonObject("usage" to jsonObject("inputTokens" to 10.json(), "outputTokens" to 5.json())))
            NativeTestBroker.tools(call, 1, NativeTestBroker.call("one"))
        }
        val replies = mutableListOf<PortResult<ModelReply>>()
        client().generate(input(), 100, 3000, replies::add)
        val turn = (replies.single() as PortResult.Success).value.nativeTurn!!
        assertEquals(ModelUsage(10, 5, 15), (replies.single() as PortResult.Success).value.usage)
        broker.resume = { call, _ -> call.usage(25, 9); call.done() }
        turn.continuation.resume(listOf(NativeToolResult("one", "{}", false)), 100, 3000, replies::add)
        assertEquals(ModelUsage(15, 4, 19), (replies.last() as PortResult.Success).value.usage)
    }
}
