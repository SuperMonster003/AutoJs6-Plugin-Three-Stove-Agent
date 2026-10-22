package io.github.supermonster003.autojs6.plugin.three.stove.agent.plugintools

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.three.stove.agent.mcp.NoDelegate
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class PluginToolSourceTest {
    private class FakeBridge : PluginToolBridge {
        class Call(val request: BridgeCall, val callback: (PortResult<JsonElement>, JsonObject?) -> Unit) { var cancellations = 0 }
        val calls = mutableListOf<Call>()
        var listing: JsonElement? = null
        override fun call(request: BridgeCall, callback: (PortResult<JsonElement>, JsonObject?) -> Unit): Cancellation {
            val call = Call(request, callback); calls += call
            if (request.method == "list") listing?.let { callback(PortResult.Success(it), null) }
            return Cancellation { call.cancellations++ }
        }
        fun invokes() = calls.filter { it.request.method == "invoke" }
        fun cancels() = calls.filter { it.request.method == "cancel" }
    }

    private class Capture<T> {
        val values = mutableListOf<PortResult<T>>()
        val callback: (PortResult<T>) -> Unit = { values += it }
        fun single(): T = (values.single() as PortResult.Success).value
    }

    private val provider = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
    private val scheduler = VirtualScheduler()
    private val bridge = FakeBridge()
    // 2027-01-15 08:00:00.123 UTC.
    private val source = PluginToolSource(bridge, scheduler, { 1_800_000_000_123L }, TimeZone.getTimeZone("UTC"))
    private val everything = ToolPolicy(ToolGroup.entries.associateWith { true }, ocrAvailable = true)
    private val pluginOnly = ToolPolicy(ToolGroup.entries.associateWith { it == ToolGroup.USER || it == ToolGroup.PLUGIN || it == ToolGroup.PLUGIN_WRITE })

    private fun schema(vararg properties: Pair<String, JsonElement>, required: List<String> = emptyList()) = jsonObject(
        "type" to "object".json(), "properties" to jsonObject(*properties), "required" to JsonArray().apply { required.forEach(::add) },
        "additionalProperties" to false.json())

    private fun listed(tool: String, risk: String = "readonly", timeoutMs: Long = 120_000, input: JsonObject = schema(),
                       outputs: JsonArray = JsonArray(), shortName: String = "speech") = jsonObject(
        "name" to "${shortName}_$tool".json(), "tool" to tool.json(), "shortName" to shortName.json(), "provider" to provider.json(),
        "trust" to "official".json(), "description" to "Fixture $tool".json(), "inputSchema" to input, "risk" to risk.json(),
        "timeoutMs" to timeoutMs.json(), "maxConcurrency" to 1.json(), "outputs" to outputs,
        "imageParameters" to JsonArray(), "fileParameters" to JsonArray(), "directoryParameters" to JsonArray())

    private fun listen() = listed("listen", input = schema("language" to jsonObject("type" to "string".json())))
    private fun speak() = listed("speak", timeoutMs = 300_000, input = schema("text" to jsonObject("type" to "string".json(), "minLength" to 1.json()), required = listOf("text")))
    private fun synthesize() = listed("synthesize", "write", 300_000, schema("text" to jsonObject("type" to "string".json()), required = listOf("text")),
        jsonArray(jsonObject("name" to "audio".json(), "kind" to "file".json(), "mimeType" to "audio/wav".json())))

    private fun listing(vararg tools: JsonObject) = jsonObject("schema" to 1.json(), "tools" to jsonArray(*tools), "providers" to JsonArray())

    private fun snapshot(vararg tools: JsonObject, policy: ToolPolicy = everything) =
        checkNotNull(source.snapshot(listing(*tools), F.catalog(), policy, RunLimits.TOOL_TIMEOUT_MS))

    private fun invocation(name: String, arguments: JsonObject) = ToolInvocation(name, arguments, ToolPlan.External(provider, name, arguments))

    private fun prepare(adapter: RunTools, name: String, arguments: JsonObject): PortResult<PreparedTool> =
        Capture<PreparedTool>().also { adapter.prepare(invocation(name, arguments), 10_000, it.callback) }.values.single()

    private fun execute(snapshot: PluginToolSnapshot, name: String, arguments: JsonObject, timeoutMs: Long = 60_000): Pair<Capture<ToolReply>, Cancellation> {
        val adapter = snapshot.wrap(NoDelegate)
        val prepared = (prepare(adapter, name, arguments) as PortResult.Success).value
        val reply = Capture<ToolReply>()
        return reply to adapter.execute(prepared, timeoutMs, reply.callback)
    }

    @Test fun admissionNeedsAPluginGroupAndTheListAndInvokeGrant() {
        val methods = setOf("plugin_tools.list", "plugin_tools.invoke", "plugin_tools.cancel")
        assertTrue(PluginToolSource.admitted(listOf("plugin"), methods, setOf("plugin_tools")))
        assertTrue(PluginToolSource.admitted(listOf("plugin_write"), methods, setOf("plugin_tools")))
        assertFalse(PluginToolSource.admitted(listOf("observe", "mcp"), methods, setOf("plugin_tools")))
        assertFalse(PluginToolSource.admitted(listOf("plugin"), methods - "plugin_tools.invoke", setOf("plugin_tools")))
        assertFalse(PluginToolSource.admitted(listOf("plugin"), methods, setOf("files")))
    }

    @Test fun onDeviceModelsKeepTheBuiltInCatalogBecauseThePluginToolsDoNotFit() {
        val listed = snapshot(listen(), speak())
        assertSame(listed, PluginToolSource.offered(listed, ModelLocality.REMOTE))
        assertSame(listed, PluginToolSource.offered(listed, ModelLocality.HYBRID))
        assertNull(PluginToolSource.offered(listed, ModelLocality.ON_DEVICE))
        assertNull(PluginToolSource.offered(null, ModelLocality.REMOTE))
    }

    @Test fun readOnlyToolsJoinThePluginGroupAndWriteToolsConfirmInTheWriteGroup() {
        val (listenEntry, listenSpec) = PluginToolSource.convert(listen())!!
        assertEquals(ToolGroup.PLUGIN, listenSpec.group); assertEquals(RiskLevel.READ_ONLY, listenSpec.risk); assertTrue(listenSpec.defaultEnabled)
        assertEquals(ExternalToolRoute(provider, "speech_listen", ExternalSource.PLUGIN), listenSpec.external)
        assertTrue(listenSpec.description("en").startsWith("Untrusted AutoJs6 plugin tool from $provider: Fixture listen"))
        assertEquals("listen", listenEntry.tool); assertEquals("speech", listenEntry.shortName); assertFalse(listenEntry.takesOutputPaths)
        assertFalse(listenSpec.inputSchema.getAsJsonObject("properties").has(PluginToolSource.OUTPUT_PATHS))
        for (risk in listOf("write", "destructive", "network")) {
            val spec = PluginToolSource.convert(listed("other", risk))!!.second
            assertEquals(ToolGroup.PLUGIN_WRITE, spec.group); assertEquals(RiskLevel.SENSITIVE, spec.risk); assertFalse(spec.defaultEnabled)
        }
        val (entry, spec) = PluginToolSource.convert(synthesize())!!
        assertTrue(entry.takesOutputPaths)
        val outputPaths = spec.inputSchema.getAsJsonObject("properties").getAsJsonObject(PluginToolSource.OUTPUT_PATHS)
        assertEquals(false.json(), outputPaths["additionalProperties"]); assertTrue(outputPaths.getAsJsonObject("properties").has("audio"))
        assertTrue(spec.description("en").contains("Writes audio (audio/wav) into the workspace"))
    }

    @Test fun malformedOrUnsupportedListingsAreSkipped() {
        fun rejected(change: JsonObject.() -> Unit) = assertNull(PluginToolSource.convert(listen().apply(change)))
        rejected { addProperty("name", "Speech_listen") }
        rejected { addProperty("name", "s") }
        rejected { addProperty("provider", "not a package") }
        rejected { addProperty("risk", "admin") }
        rejected { addProperty("timeoutMs", 0) }
        rejected { remove("shortName") }
        rejected { add("inputSchema", jsonObject("type" to "array".json())) }
        rejected { add("inputSchema", schema("text" to jsonObject("type" to "string".json(), "description" to "x".repeat(17 * 1024).json()))) }
        rejected { add("inputSchema", schema("value" to jsonObject("type" to "string".json(), "unknownKeyword" to true.json()))) }
        rejected { add("outputs", jsonArray(jsonObject("name" to "a".json(), "kind" to "stream".json()))) }
        rejected { add("outputs", jsonArray(*Array(2) { jsonObject("name" to "a".json(), "kind" to "file".json(), "mimeType" to "text/plain".json()) })) }
        val long = PluginToolSource.convert(listen().apply { addProperty("description", "d".repeat(4000)) })!!.second
        assertTrue(long.description("en").utf8Size() <= PluginToolSource.MAX_DESCRIPTION_BYTES)
    }

    @Test fun specialParametersAreExplainedAndHostOnlyKeywordsAreRemoved() {
        val input = schema(
            "image" to jsonObject("type" to "string".json(), "description" to "Picture.".json(), "x-autojs6-type" to "image".json()),
            "folder" to jsonObject("type" to "string".json(), "x-autojs6-type" to "directory".json(), "x-autojs6-files" to jsonArray("a.txt".json(), "b.txt".json())),
            "audio" to jsonObject("type" to "string".json()),
            "score" to jsonObject("type" to "number".json(), "exclusiveMinimum" to 0.json(), "maximum" to 1.json()),
        )
        val (entry, spec) = PluginToolSource.convert(listed("inspect", input = input).apply { add("fileParameters", jsonArray("audio".json())) })!!
        val properties = spec.inputSchema.getAsJsonObject("properties")
        assertFalse(properties.toString().contains("x-autojs6")); assertFalse(properties.toString().contains("exclusiveMinimum"))
        assertTrue(properties.getAsJsonObject("image").string("description")!!.startsWith("Picture. Accepts \"screen\""))
        assertTrue(properties.getAsJsonObject("folder").string("description")!!.endsWith("The directory must contain: a.txt, b.txt."))
        assertEquals("A file path relative to the workspace.", properties.getAsJsonObject("audio").string("description"))
        assertEquals(setOf("image"), entry.imageParameters); assertEquals(setOf("folder", "audio"), entry.pathParameters)
    }

    @Test fun listingIsSortedSkipsCollisionsAndHonorsDisabledGroups() {
        val snapshot = checkNotNull(source.snapshot(listing(speak(), synthesize(), listen(), listen(), listed("dump", shortName = "ui"), jsonObject("name" to 1.json())),
            F.catalog(), pluginOnly, RunLimits.TOOL_TIMEOUT_MS))
        assertEquals(listOf("speech_listen", "speech_speak", "speech_synthesize"), snapshot.entries.keys.toList())
        assertEquals(3, snapshot.catalog.tools.count { it.external?.source == ExternalSource.PLUGIN })
        assertEquals(F.catalog()["ui_dump"]!!.snapshot(), snapshot.catalog["ui_dump"]!!.snapshot())
        val readOnly = ToolPolicy(ToolGroup.entries.associateWith { it != ToolGroup.PLUGIN_WRITE })
        assertEquals(listOf("speech_listen"), source.snapshot(listing(synthesize(), listen()), F.catalog(), readOnly, RunLimits.TOOL_TIMEOUT_MS)!!.entries.keys.toList())
        val none = ToolPolicy(ToolGroup.entries.associateWith { it != ToolGroup.PLUGIN && it != ToolGroup.PLUGIN_WRITE })
        assertNull(source.snapshot(listing(synthesize(), listen()), F.catalog(), none, RunLimits.TOOL_TIMEOUT_MS))
        assertNull(source.snapshot(listing(), F.catalog(), everything, RunLimits.TOOL_TIMEOUT_MS))
    }

    @Test fun listingStopsAtThePerSourceAndNativeDefinitionLimits() {
        val many = (10..49).map { listed("tool_$it") }.toTypedArray()
        assertEquals(PluginToolSource.MAX_TOOLS, source.snapshot(listing(*many), F.catalog(), pluginOnly, RunLimits.TOOL_TIMEOUT_MS)!!.entries.size)
        val full = source.snapshot(listing(*many), F.catalog(), everything, RunLimits.TOOL_TIMEOUT_MS)!!
        val policy = everything.withExternalTools(full.catalog.tools.filter { it.external != null })
        assertTrue(full.entries.size < PluginToolSource.MAX_TOOLS)
        assertTrue(full.catalog.nativeDefinitions(policy.withVisionAvailability(true), "en").size() <= 64)
        assertEquals("speech_tool_10", full.entries.keys.first())
    }

    @Test fun prepareListsOnceAndAnswersOnlyOnce() {
        bridge.listing = listing(listen())
        val results = mutableListOf<PluginToolSnapshot?>()
        source.prepare(F.catalog(), everything, 3_000) { results += it }
        val list = bridge.calls.single().request
        assertEquals(BridgeCall("plugin_tools", "list", JsonArray(), listOf("plugin_tools"), 3_000), list)
        assertEquals(listOf("speech_listen"), results.single()!!.entries.keys.toList())
        scheduler.advance(PluginToolSource.LIST_TIMEOUT_MS); assertEquals(1, results.size)
    }

    @Test fun failedRefusedOrLateListingsLeaveTheTaskWithoutPluginTools() {
        val failed = mutableListOf<PluginToolSnapshot?>()
        source.prepare(F.catalog(), everything, RunLimits.TOOL_TIMEOUT_MS) { failed += it }
        assertEquals(4_500L, bridge.calls.single().request.timeoutMs)
        bridge.calls.single().callback(PortResult.Failure(RunError.CAPABILITY_DENIED), null)
        assertEquals(listOf<PluginToolSnapshot?>(null), failed)

        val late = mutableListOf<PluginToolSnapshot?>()
        source.prepare(F.catalog(), everything, RunLimits.TOOL_TIMEOUT_MS) { late += it }
        scheduler.advance(PluginToolSource.LIST_TIMEOUT_MS - 1); assertTrue(late.isEmpty())
        scheduler.advance(1); assertEquals(listOf<PluginToolSnapshot?>(null), late); assertEquals(1, bridge.calls.last().cancellations)
        bridge.calls.last().callback(PortResult.Success(listing(listen())), null); assertEquals(1, late.size)

        val unreadable = mutableListOf<PluginToolSnapshot?>()
        source.prepare(F.catalog(), everything, RunLimits.TOOL_TIMEOUT_MS) { unreadable += it }
        bridge.calls.last().callback(PortResult.Success(JsonPrimitive("tools")), null); assertEquals(listOf<PluginToolSnapshot?>(null), unreadable)

        val stopped = mutableListOf<PluginToolSnapshot?>()
        source.prepare(F.catalog(), everything, RunLimits.TOOL_TIMEOUT_MS) { stopped += it }.cancel()
        assertEquals(1, bridge.calls.last().cancellations)
        bridge.calls.last().callback(PortResult.Success(listing(listen())), null); scheduler.advance(PluginToolSource.LIST_TIMEOUT_MS)
        assertTrue(stopped.isEmpty())
    }

    @Test fun preparationValidatesArgumentsAndGivesThePluginItsOwnStepLimit() {
        val snapshot = snapshot(listen(), speak(), synthesize())
        val adapter = snapshot.wrap(NoDelegate)
        val prepared = (prepare(adapter, "speech_listen", jsonObject()) as PortResult.Success).value
        assertEquals(minOf(122_000L, RunLimits.TOOL_TIMEOUT_MS), prepared.metadata.pluginTimeoutMs)
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, (prepare(adapter, "speech_speak", jsonObject()) as PortResult.Failure).error)
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, (prepare(adapter, "speech_listen", jsonObject("extra" to true.json())) as PortResult.Failure).error)
        val wrongRoute = ToolInvocation("speech_listen", jsonObject(), ToolPlan.External("other.package", "speech_listen", jsonObject()))
        val refused = Capture<PreparedTool>().also { adapter.prepare(wrongRoute, 10_000, it.callback) }
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, (refused.values.single() as PortResult.Failure).error)
        for (path in listOf("../a.wav", "/sdcard/a.wav", "C:/a.wav", "a\\b.wav")) {
            val result = prepare(adapter, "speech_synthesize", jsonObject("text" to "hi".json(), "outputPaths" to jsonObject("audio" to path.json())))
            assertEquals(path, RunError.TOOL_ARGUMENTS_INVALID, (result as PortResult.Failure).error)
        }
        val unknownOutput = prepare(adapter, "speech_synthesize", jsonObject("text" to "hi".json(), "outputPaths" to jsonObject("video" to "a.mp4".json())))
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, (unknownOutput as PortResult.Failure).error)
        val short = PluginToolSource(bridge, scheduler).snapshot(listing(listen()), F.catalog(), everything, 30_000)!!
        assertEquals(30_000L, short.stepTimeout(short.entries.getValue("speech_listen")))
    }

    @Test fun otherToolsGoToTheDelegateAndForeignPreparationsAreRefused() {
        val snapshot = snapshot(listen())
        val delegate = FakeTools()
        val adapter = snapshot.wrap(delegate)
        val dump = ToolInvocation("ui_dump", jsonObject(), ToolHandlers(F.catalog()).prepare("ui_dump", jsonObject(), F.policy()))
        adapter.prepare(dump, 1_000) {}
        assertEquals(listOf("ui_dump"), delegate.inspections.map { it.first.name })
        val forged = PreparedTool(invocation("speech_listen", jsonObject()), ToolMetadata())
        val reply = Capture<ToolReply>().also { adapter.execute(forged, 60_000, it.callback) }
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, (reply.values.single() as PortResult.Failure).error)
        val other = snapshot(listen()).wrap(NoDelegate)
        val foreign = (prepare(other, "speech_listen", jsonObject()) as PortResult.Success).value
        val crossed = Capture<ToolReply>().also { adapter.execute(foreign, 60_000, it.callback) }
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, (crossed.values.single() as PortResult.Failure).error)
        assertTrue(bridge.invokes().isEmpty())
    }

    @Test fun invocationCarriesTheGrantTokensAndDefaultOutputPaths() {
        val snapshot = snapshot(listen(), synthesize())
        execute(snapshot, "speech_listen", jsonObject("language" to "en".json()), 10_000)
        val listen = bridge.invokes().single().request
        assertEquals(listOf("plugin_tools"), listen.permissions); assertEquals(9_500L, listen.timeoutMs)
        assertEquals("speech_listen", listen.args[0].asString); assertEquals(jsonObject("language" to "en".json()), listen.args[1])
        val options = listen.args[2].asJsonObject
        assertTrue(options.string("invocationId")!!.matches(Regex("stove-[0-9a-f-]{36}")))
        assertEquals(8_500L, options.number("timeoutMs")); assertEquals(false, options.flag("overwrite")); assertFalse(options.has("outputs"))

        execute(snapshot, "speech_synthesize", jsonObject("text" to "hi".json()))
        execute(snapshot, "speech_synthesize", jsonObject("text" to "hi".json()))
        execute(snapshot, "speech_synthesize", jsonObject("text" to "hi".json(), "outputPaths" to jsonObject("audio" to "out/a.wav".json())))
        val writes = bridge.invokes().drop(1).map { it.request }
        assertTrue(writes.all { it.permissions == listOf("plugin_tools", "files", "files.write") })
        assertEquals(listOf("plugin-outputs/speech/synthesize-20270115-080000-123.wav", "plugin-outputs/speech/synthesize-20270115-080000-123-1.wav", "out/a.wav"),
            writes.map { it.args[2].asJsonObject.getAsJsonObject("outputs").string("audio") })
        assertTrue(writes.all { it.args[1] == jsonObject("text" to "hi".json()) })
    }

    @Test fun screenImagesAndWorkspaceFilesRequestOnlyTheMatchingTokens() {
        val input = schema("image" to jsonObject("type" to "string".json()))
        val snapshot = snapshot(listed("recognize", input = input, shortName = "vision").apply { add("imageParameters", jsonArray("image".json())) })
        execute(snapshot, "vision_recognize", jsonObject("image" to "screen".json()))
        execute(snapshot, "vision_recognize", jsonObject("image" to "pictures/a.png".json()))
        assertEquals(listOf(listOf("plugin_tools", "screen_capture"), listOf("plugin_tools", "files")), bridge.invokes().map { it.request.permissions })
        val adapter = snapshot.wrap(NoDelegate)
        assertTrue(prepare(adapter, "vision_recognize", jsonObject("image" to "../a.png".json())) is PortResult.Failure)
    }

    @Test fun resultsAreUntrustedBoundedObservations() {
        val snapshot = snapshot(listen(), synthesize())
        val (reply, _) = execute(snapshot, "speech_synthesize", jsonObject("text" to "hi".json()))
        bridge.invokes().single().callback(PortResult.Success(jsonObject(
            "result" to jsonObject("format" to "wav".json()), "text" to "hello".json(), "durationMs" to 1234.json(),
            "outputs" to jsonArray(jsonObject("name" to "audio".json(), "path" to "plugin-outputs/speech/a.wav".json(), "bytes" to 10.json(),
                "mimeType" to "audio/wav".json(), "nested" to jsonObject())), "missingOutputs" to jsonArray("other".json()), "extra" to true.json())), null)
        val value = reply.single()
        assertNull(value.error)
        assertEquals(jsonObject("untrusted" to true.json(), "source" to "plugin".json(), "tool" to "speech_synthesize".json(), "provider" to provider.json(),
            "result" to jsonObject("format" to "wav".json()), "text" to "hello".json(),
            "outputs" to jsonArray(jsonObject("name" to "audio".json(), "path" to "plugin-outputs/speech/a.wav".json(), "bytes" to 10.json(), "mimeType" to "audio/wav".json())),
            "missingOutputs" to jsonArray("other".json()), "durationMs" to 1234.json(), "truncated" to false.json()), value.result)

        val (large, _) = execute(snapshot, "speech_listen", jsonObject())
        bridge.invokes().last().callback(PortResult.Success(jsonObject("result" to jsonObject("text" to "x".repeat(20_000).json()), "text" to "y".repeat(20_000).json())), null)
        val bounded = large.single().result.asJsonObject
        assertEquals(true, bounded.flag("truncated"))
        assertTrue(bounded["result"].toString().utf8Size() <= PluginToolSnapshot.RESULT_BYTES + 256)
        assertTrue(bounded.string("text")!!.utf8Size() <= PluginToolSnapshot.TEXT_BYTES)
        assertTrue(bounded.toString().utf8Size() <= ToolObservation.DEFAULT_MAX_BYTES)

        val (unreadable, _) = execute(snapshot, "speech_listen", jsonObject())
        bridge.invokes().last().callback(PortResult.Success(JsonPrimitive("done")), null)
        assertEquals(RunError.TOOL_FAILED, unreadable.single().error)
        assertEquals("providerFailed", unreadable.single().result.asJsonObject.string("error"))
    }

    @Test fun pluginErrorsBecomeToolFailuresButALostHostBlocksTheTask() {
        val snapshot = snapshot(listen())
        val (coded, _) = execute(snapshot, "speech_listen", jsonObject())
        bridge.invokes().last().callback(PortResult.Failure(RunError.TOOL_FAILED), jsonObject("category" to "providerFailed".json(),
            "message" to "plugin_tools.invoke: speech_listen failed with NO_SPEECH: Nothing was heard.".json()))
        val failure = coded.single()
        assertEquals(RunError.TOOL_FAILED, failure.error)
        assertEquals(jsonObject("untrusted" to true.json(), "source" to "plugin".json(), "tool" to "speech_listen".json(), "error" to "providerFailed".json(),
            "code" to "NO_SPEECH".json(), "message" to "Nothing was heard.".json()), failure.result)

        val (plain, _) = execute(snapshot, "speech_listen", jsonObject())
        bridge.invokes().last().callback(PortResult.Failure(RunError.TOOL_FAILED), jsonObject("category" to "invalidRequest".json(), "message" to ("m".repeat(2000)).json()))
        val message = plain.single().result.asJsonObject
        assertFalse(message.has("code")); assertTrue(message.string("message")!!.utf8Size() <= PluginToolSnapshot.MESSAGE_BYTES)

        val (dead, _) = execute(snapshot, "speech_listen", jsonObject())
        bridge.invokes().last().callback(PortResult.Failure(RunError.TOOL_FAILED), jsonObject("category" to H.ERROR_PROCESS_DEAD.json(), "message" to "gone".json()))
        assertEquals(RunError.HOST_UNAVAILABLE, (dead.values.single() as PortResult.Failure).error)

        val (denied, _) = execute(snapshot, "speech_listen", jsonObject())
        bridge.invokes().last().callback(PortResult.Failure(RunError.CAPABILITY_DENIED), null)
        assertEquals(RunError.CAPABILITY_DENIED, (denied.values.single() as PortResult.Failure).error)
    }

    @Test fun aSlowPluginTimesOutBeforeTheStepLimitAndIsAskedToStop() {
        val snapshot = snapshot(listen())
        val (reply, _) = execute(snapshot, "speech_listen", jsonObject(), 10_000)
        val invoke = bridge.invokes().single()
        scheduler.advance(9_749); assertTrue(reply.values.isEmpty()); assertTrue(bridge.cancels().isEmpty())
        scheduler.advance(1)
        val timeout = reply.single()
        assertEquals(RunError.TOOL_FAILED, timeout.error)
        assertEquals("timeout", timeout.result.asJsonObject.string("error")); assertEquals("TIMEOUT", timeout.result.asJsonObject.string("code"))
        assertEquals(1, invoke.cancellations)
        val cancel = bridge.cancels().single().request
        assertEquals(jsonArray(invoke.request.args[2].asJsonObject["invocationId"]), cancel.args); assertEquals(listOf("plugin_tools"), cancel.permissions)
        invoke.callback(PortResult.Success(jsonObject("result" to true.json())), null)
        assertEquals(1, reply.values.size)

        val (hurried, _) = execute(snapshot, "speech_listen", jsonObject(), PluginToolSnapshot.MIN_STEP_MS - 1)
        assertEquals("TIMEOUT", hurried.single().result.asJsonObject.string("code")); assertEquals(1, bridge.invokes().size)
    }

    @Test fun stoppingTheTaskCancelsTheInvocationWithoutAnAnswerOrReplay() {
        val snapshot = snapshot(listen())
        val (reply, cancellation) = execute(snapshot, "speech_listen", jsonObject(), 10_000)
        val invoke = bridge.invokes().single()
        cancellation.cancel(); cancellation.cancel()
        assertEquals(1, invoke.cancellations); assertEquals(1, bridge.cancels().size)
        assertEquals(invoke.request.args[2].asJsonObject["invocationId"], bridge.cancels().single().request.args[0])
        scheduler.advance(20_000)
        invoke.callback(PortResult.Success(jsonObject("result" to true.json())), null)
        assertTrue(reply.values.isEmpty()); assertEquals(1, bridge.invokes().size); assertEquals(1, bridge.cancels().size)
    }

    @Test fun outputExtensionsFollowTheDeclaredMimeType() {
        assertEquals("wav", PluginToolSource.extension("audio/wav"))
        assertEquals("png", PluginToolSource.extension("IMAGE/PNG; charset=binary"))
        assertEquals("txt", PluginToolSource.extension("text/plain"))
        assertEquals("bin", PluginToolSource.extension("application/x-unknown"))
    }
}
