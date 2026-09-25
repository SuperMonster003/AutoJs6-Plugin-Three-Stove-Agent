package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class VisionModelTest {
    private val catalog = F.catalog()
    private val policy = ToolPolicy().withVisionAvailability(true)
    private fun image(size: Int = 512) = ModelImage(ByteArray(size) { (it % 251).toByte() }, 1280, 720)
    private fun target(native: Boolean = false, limits: VisionLimits = VisionLimits()) = ModelTarget("p", "profile:test", ModelLocality.REMOTE,
        ModelProtocol.UNKNOWN, false, 128 * 1024, supportsStreaming = true, nativeTools = if (native) NativeToolLimits() else null, vision = limits)
    private fun compiler(selected: ModelTarget = target(), enabled: ToolPolicy = policy) = ContextCompiler(
        PromptCatalog(F::asset, catalog), catalog, enabled, selected, if (selected.nativeTools == null) DecisionSchema.degraded() else DecisionSchema.native(ModelProtocol.UNKNOWN))
    private fun context(images: List<ModelImage> = emptyList(), repair: JsonObject? = null) = RunContext("Inspect the fixture", emptyList(),
        ToolObservation.success(jsonObject("captured" to true.json(), "width" to 1280.json())), repair, JsonObject(), images = images)

    @Test fun visionRequiresKnownHostExtensionAndExactTargetCapability() {
        val info = AgentJson.objectOf("""{"visionVersion":1,"maximumImages":4,"maximumImageBytes":4194304,"maximumTotalImageBytes":8388608,"maximumImageEdge":4096,"maximumImagePixels":16777216,"maximumSessionImages":16,"maximumSessionImageBytes":33554432}""")
        val row = AgentJson.objectOf("""{"targetId":"profile:test","locality":2,"configured":true,"available":true,"maximumContextBytes":65536,"capabilityIds":["vision"],"supportedControls":["maximum-output-tokens"]}""")
        assertNull(ModelTarget.fromCatalog("p", row).vision)
        assertEquals(VisionLimits(), ModelTarget.fromCatalog("p", row, brokerVision = VisionLimits.fromBroker(info)).vision)
        row.add("capabilityIds", jsonArray("vision-output".json()))
        assertNull(ModelTarget.fromCatalog("p", row, brokerVision = VisionLimits.fromBroker(info)).vision)
        info.addProperty("visionVersion", 2); assertNull(VisionLimits.fromBroker(info))
        info.addProperty("visionVersion", 1); info.addProperty("maximumImages", 0)
        assertThrows(IllegalArgumentException::class.java) { VisionLimits.fromBroker(info) }
    }
    @Test fun screenshotToolNeedsVisionAndObserveGroupAndUsesHostOnly() {
        val spec = catalog["screen_capture"]!!
        assertFalse(ToolPolicy().isEnabled(spec)); assertTrue(policy.isEnabled(spec))
        assertFalse(ToolPolicy(ToolGroup.entries.associateWith { it != ToolGroup.OBSERVE }, visionAvailable = true).isEnabled(spec))
        val call = (ToolHandlers(catalog).prepare("screen_capture", JsonObject(), policy) as ToolPlan.Call).request
        assertEquals("accessibility", call.module); assertEquals("screenshot", call.method)
        assertEquals(jsonArray(jsonObject("format" to "png".json())), call.args)
        assertThrows(IllegalArgumentException::class.java) { ToolHandlers(catalog).prepare("screen_capture", jsonObject("savePath" to "x".json()), policy) }
    }
    @Test fun imageBytesAreImmutableAndNeverPartOfDiagnosticOrTextMessages() {
        val bytes = ByteArray(512) { 65 }; val picture = ModelImage(bytes, 1280, 720)
        val reference = picture.reference(0, 2); bytes.fill(0)
        assertEquals(reference, picture.reference(0, 2))
        assertTrue(ByteArrayOutputStream().also(picture::writeTo).toByteArray().all { it == 65.toByte() })
        assertEquals(4704L, picture.estimatedTokens)
        assertFalse(picture.toString().contains(reference.string("sha256")!!))
        val input = compiler().compile(context(listOf(picture)))
        assertEquals(1, input.images.size); assertTrue(input.vision)
        assertEquals(StepJournal.bytes(input.messages) + input.schemaBytes + StepJournal.bytes(input.imageRefs), input.inputBytes)
        assertFalse(input.messages.toString().contains(reference.string("sha256")!!))
    }
    @Test fun repairKeepsOnlyTheCurrentObservationImageAndFreshCompilationHasNone() {
        val c = compiler(); val picture = image()
        val first = c.compile(context(listOf(picture), jsonObject("remainingRepairs" to 1.json())))
        val index = first.imageRefs.single().asJsonObject.number("messageIndex")!!.toInt()
        assertTrue(first.messages[index].asJsonObject.string("content")!!.contains("captured"))
        assertTrue(index < first.messages.size() - 2)
        assertTrue(first.messages.first().asJsonObject.string("content")!!.contains("1280"))
        val second = c.compile(context())
        assertTrue(second.images.isEmpty()); assertTrue(second.imageRefs.isEmpty)
        assertEquals(1, first.images.size)
        assertThrows(IllegalArgumentException::class.java) { compiler(enabled = ToolPolicy()).compile(context(listOf(picture))) }
    }
    @Test fun perImageBatchAndRetainedSessionLimitsRejectBeforeDispatch() {
        val one = image()
        assertThrows(ContextLimitExceeded::class.java) { VisionLimits(imageBytes = 511).validate(listOf(one)) }
        assertThrows(ContextLimitExceeded::class.java) { VisionLimits(edge = 1279).validate(listOf(one)) }
        assertThrows(ContextLimitExceeded::class.java) { VisionLimits(pixels = 1280L * 720 - 1).validate(listOf(one)) }
        assertThrows(ContextLimitExceeded::class.java) { VisionLimits().validate(List(5) { one }) }
        assertThrows(ContextLimitExceeded::class.java) { VisionLimits(batchBytes = 1000).validate(listOf(one, one)) }
        assertThrows(ContextLimitExceeded::class.java) { VisionLimits().validate(listOf(one), 16, 8192) }
        assertThrows(ContextLimitExceeded::class.java) { VisionLimits(sessionBytes = 1023).validate(listOf(one), 1, 512) }
    }
    @Test fun imageAdmissionUsesTokensNotEncodedBytesAndActualUsageReplacesEstimate() {
        val b = Budget(BudgetLimits(maxTotalTokens = 5000), 0) { 0 }
        val ticket = b.reserveModel(100, 1000, image().estimatedTokens)
        assertEquals(4744L, ticket.inputEstimate); assertEquals(256, ticket.maximumOutputTokens)
        b.settleModel(ticket, ModelUsage(20, 5, 25), 10)
        assertEquals(25L, b.totalTokens)
        assertThrows(BudgetExceeded::class.java) { b.reserveModel(100, 100, 5000) }
        assertEquals(1, b.modelCalls)
        assertThrows(BudgetExceeded::class.java) { b.reserveModel(100, 100, Long.MAX_VALUE) }
        val estimate = b.reserveModel(100, 100, image().estimatedTokens)
        b.settleModel(estimate, null, 0); assertEquals(4769L, b.totalTokens)
    }

    private class Broker : ModelBrokerTransport {
        lateinit var call: TestModelBroker.Call
        val initial = mutableListOf<List<ModelImage>>()
        val submissions = mutableListOf<Pair<JsonObject, List<ModelImage>>>()
        var start: (TestModelBroker.Call) -> Unit = { it.started(); it.done() }
        var resumed: (TestModelBroker.Call, JsonObject) -> Unit = { c, _ -> c.done() }
        var cancelled = 0
        override fun generate(requestJson: String, onEvent: (String) -> Unit) = error("Use attachment-aware transport")
        override fun generate(requestJson: String, images: List<ModelImage>, onEvent: (String) -> Unit, onFailure: (RunError) -> Unit) {
            initial += images; call = TestModelBroker.Call(AgentJson.objectOf(requestJson, 128 * 1024), onEvent); start(call)
        }
        override fun submitToolResults(requestJson: String, images: List<ModelImage>, onFailure: (RunError) -> Unit) {
            val json = AgentJson.objectOf(requestJson, 128 * 1024); submissions += json to images; resumed(call, json)
        }
        override fun cancel(requestId: String) { cancelled++ }
    }
    @Test fun jsonRequestAssociatesImageWithObservationAndPreservesTextOnlyCompatibility() {
        val broker = Broker(); val replies = mutableListOf<PortResult<ModelReply>>()
        val client = ModelClient(broker, target(), policy, SchemaFallbacks(DecisionSchema(catalog)), VirtualScheduler()) { true }
        client.generate(compiler().compile(context(listOf(image()))), 100, 3000, replies::add)
        assertEquals(1, broker.initial.single().size); assertTrue(broker.call.request.flag("vision")!!)
        assertEquals(1, broker.call.request.getAsJsonArray("imageRefs").size())
        assertTrue(replies.single() is PortResult.Success)
        val old = TestModelBroker().apply { script = { it.started(); it.done() } }
        val plainTarget = ModelTarget("p", "profile:test", ModelLocality.REMOTE, ModelProtocol.UNKNOWN, false, 128 * 1024)
        ModelClient(old, plainTarget, ToolPolicy(), SchemaFallbacks(DecisionSchema(catalog)), VirtualScheduler()) { true }
            .generate(compiler(plainTarget, ToolPolicy()).compile(context()), 100, 3000, replies::add)
        assertFalse(old.calls.single().request.has("vision")); assertFalse(old.calls.single().request.has("imageRefs"))
    }
    @Test fun nativeResultsKeepCallCorrelationGlobalIndicesAndRepeatedImageCost() {
        val broker = Broker().apply { start = { it.started(); NativeTestBroker.tools(it, 1,
            NativeTestBroker.call("a", "screen_capture"), NativeTestBroker.call("b"), NativeTestBroker.call("c", "screen_capture")) } }
        val replies = mutableListOf<PortResult<ModelReply>>()
        ModelClient(broker, target(true), policy, SchemaFallbacks(DecisionSchema(catalog)), VirtualScheduler()) { true }
            .generate(compiler(target(true)).compile(context()), 100, 3000, replies::add)
        val turn = (replies.last() as PortResult.Success).value.nativeTurn!!.continuation
        val images = listOf(image(), image(1000))
        val results = listOf(NativeToolResult("a", "{}", false, listOf(images[0])), NativeToolResult("b", "{}", false),
            NativeToolResult("c", "{}", false, listOf(images[1])))
        assertEquals(9408L, turn.imageTokens(results)); assertTrue(broker.call.request.flag("vision")!!)
        broker.resumed = { c, _ -> NativeTestBroker.tools(c, 2, NativeTestBroker.call("d")) }
        turn.resume(results, 100, 3000, replies::add)
        val (json, attached) = broker.submissions.single(); assertEquals(images, attached)
        val rows = json.getAsJsonArray("results")
        assertEquals(0L, rows[0].asJsonObject.getAsJsonArray("imageRefs")[0].asJsonObject.number("descriptorIndex"))
        assertFalse(rows[1].asJsonObject.has("imageRefs"))
        assertEquals(1L, rows[2].asJsonObject.getAsJsonArray("imageRefs")[0].asJsonObject.number("descriptorIndex"))
        assertFalse(rows[2].asJsonObject.getAsJsonArray("imageRefs")[0].asJsonObject.has("messageIndex"))
        val next = (replies.last() as PortResult.Success).value.nativeTurn!!.continuation
        assertEquals(9408L, next.imageTokens(listOf(NativeToolResult("d", "{}", false))))
        next.cancel(); assertEquals(1, broker.cancelled)
    }
    @Test fun nativeSessionImageCeilingAppliesAcrossRounds() {
        val target = target(true, VisionLimits(sessionImages = 1))
        val broker = Broker().apply { start = { it.started(); NativeTestBroker.tools(it, 1, NativeTestBroker.call("a", "screen_capture")) } }
        val replies = mutableListOf<PortResult<ModelReply>>()
        ModelClient(broker, target, policy, SchemaFallbacks(DecisionSchema(catalog)), VirtualScheduler()) { true }
            .generate(compiler(target).compile(context()), 100, 3000, replies::add)
        val first = (replies.last() as PortResult.Success).value.nativeTurn!!.continuation
        broker.resumed = { c, _ -> NativeTestBroker.tools(c, 2, NativeTestBroker.call("b", "screen_capture")) }
        first.resume(listOf(NativeToolResult("a", "{}", false, listOf(image()))), 100, 3000, replies::add)
        val second = (replies.last() as PortResult.Success).value.nativeTurn!!.continuation
        assertThrows(ContextLimitExceeded::class.java) { second.inputBytes(listOf(NativeToolResult("b", "{}", false, listOf(image())))) }
        second.cancel(); assertEquals(1, broker.submissions.size)
    }
    @Test fun runnerReplacesPicturesAfterFailedObservationAndNeverJournalsEncodedBytes() {
        val f = RunnerFixture(policy); f.tools.autoExecute = false
        val run = f.start(); f.reply(RunnerFixture.tool("screen_capture"))
        val picture = image(); val metadata = jsonObject("captured" to true.json(), "width" to 1280.json())
        f.tools.executions.last().second.succeed(ToolReply(metadata, images = listOf(picture))); f.scheduler.drain()
        assertEquals(listOf(picture), f.contexts.last().images)
        val journal = f.journal(run).toString()
        assertTrue(journal.contains("captured")); assertFalse(journal.contains(picture.reference(0).string("sha256")!!))
        f.reply(RunnerFixture.tool("device_info")); f.tools.executions.last().second.fail(RunError.TOOL_ARGUMENTS_INVALID); f.scheduler.drain()
        assertTrue(f.contexts.last().images.isEmpty()); assertTrue(f.contexts.last().observation!!.contains("TOOL_ARGUMENTS_INVALID"))
        run.cancel(); f.scheduler.drain(); assertEquals(RunState.CANCELLED, run.state)
    }
    @Test fun runnerDoesNotAcceptAnImageFromAnUnrelatedTool() {
        val f = RunnerFixture(policy); f.tools.autoExecute = false
        val run = f.start(); f.reply(RunnerFixture.tool("device_info"))
        f.tools.executions.last().second.succeed(ToolReply(true.json(), images = listOf(image()))); f.scheduler.drain()
        assertEquals(RunState.FAILED, run.state); assertEquals("INVALID_REQUEST", run.result!!.getAsJsonObject("error").string("code"))
    }
}
