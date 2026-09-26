package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.content.*
import android.os.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.ScriptRootsActivity
import org.autojs.plugin.ai.agent.api.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import org.autojs.plugin.host.capability.api.*
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real :agent runtime and HostLink, using the existing same-UID, debug-only fixture entry. */
class DynamicScriptLinkAndroidTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val source = "/* " + "plain <b>source</b> ".repeat(180) + " */\nai.agent.result({ answer: 42 });"
    private val completed = """{"kind":"done","done":{"status":"completed","summary":"Dynamic fixture complete","evidence":["Fixture result received"]}}"""
    private fun envelope(key: String, value: JsonObject) = AgentWire.envelope(key, value.toString())
    private fun decode(bundle: Bundle): JsonObject {
        assertNull("Endpoint must accept fixture request", bundle.getString(C.KEY_ERROR_CODE))
        return AgentJson.objectOf(checkNotNull(bundle.getString(C.KEY_RUN_RESPONSE_JSON)), 32 * 1024)
    }
    private fun await(label: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 20_000
        while (SystemClock.elapsedRealtime() < deadline) { if (condition()) return; SystemClock.sleep(40) }
        fail(label)
    }
    private fun bind(intent: Intent, connections: MutableList<ServiceConnection>): IBinder {
        val ready = CountDownLatch(1)
        var binder: IBinder? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) { binder = service; ready.countDown() }
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        check(context.bindService(intent, connection, Context.BIND_AUTO_CREATE)); connections += connection
        assertTrue("Fixture binding", ready.await(15, TimeUnit.SECONDS)); return checkNotNull(binder)
    }
    private fun query(body: JsonObject, invoke: (Bundle, IPresetStoreCallback) -> Unit): JsonObject {
        val ready = CountDownLatch(1)
        var response: Bundle? = null
        invoke(envelope(C.KEY_RUN_REQUEST_JSON, body), object : IPresetStoreCallback.Stub() {
            override fun onResult(value: Bundle?) { response = value; ready.countDown() }
        })
        assertTrue("Private store response", ready.await(15, TimeUnit.SECONDS)); return decode(checkNotNull(response))
    }
    private inner class Model(private val dynamic: Boolean) : IAiAgentModelBroker.Stub() {
        val calls = AtomicInteger()
        val requests = CopyOnWriteArrayList<JsonObject>()
        override fun getBrokerInfo() = AgentWire.envelope(C.KEY_MODEL_BROKER_INFO_JSON,
            """{"available":true,"providerId":"dynamic-fixture","maximumInputBytes":131072,"maximumOutputBytes":65536,"maximumResponseSchemaBytes":16384}""").apply {
            putString(H.KEY_GRANT_JSON, """{"maxInputBytesPerRequest":131072,"maxTotalTokens":1000000,"consumedTokens":0}""")
        }
        private fun emit(callback: IAiAgentModelCallback, id: String, type: String, sequence: Int, data: JsonObject = JsonObject()) {
            data.addProperty("requestId", id); data.addProperty("type", type); data.addProperty("sequence", sequence)
            callback.onEvent(envelope(C.KEY_MODEL_EVENT_JSON, data))
        }
        override fun listTargets(request: Bundle, callback: IAiAgentModelCallback) {
            val id = AgentJson.objectOf(request.getString(C.KEY_MODEL_REQUEST_JSON)!!).string("requestId")!!
            emit(callback, id, "started", 1)
            emit(callback, id, "completed", 2, AgentJson.objectOf("""{"targets":[{"targetId":"dynamic-fixture:text","displayName":"Dynamic fixture","locality":2,"configured":true,"available":true,"maximumContextBytes":131072,"capabilityIds":[],"supportedControls":["maximum-output-tokens"]}]}"""))
        }
        override fun generate(request: Bundle, callback: IAiAgentModelCallback) {
            val value = AgentJson.objectOf(request.getString(C.KEY_MODEL_REQUEST_JSON)!!, 131072)
            requests += value
            val id = value.string("requestId")!!
            val text = if (calls.incrementAndGet() == 1 && dynamic) jsonObject("kind" to "tool".json(), "tool" to "script_run_source".json(),
                "arguments" to jsonObject("source" to source.json(), "timeoutMs" to 5000.json())).toString() else completed
            emit(callback, id, "started", 1)
            emit(callback, id, "usage", 2, jsonObject("usage" to jsonObject("inputTokens" to 1.json(), "outputTokens" to 1.json(), "totalTokens" to 2.json())))
            emit(callback, id, "completed", 3, jsonObject("text" to text.json(), "targetId" to "dynamic-fixture:text".json(), "finishReason" to 0.json()))
        }
        override fun cancel(reference: Bundle?) = Unit
        override fun submitToolResults(request: Bundle?) { error("Text fixture cannot receive native continuations") }
        override fun destroy(reason: Bundle?) = Unit
    }
    private class Capabilities(private val advertised: Boolean) : IHostCapabilityBroker.Stub() {
        val executions = AtomicInteger()
        val requests = CopyOnWriteArrayList<JsonObject>()
        override fun getBrokerInfo() = Bundle().apply {
            putInt(H.KEY_CONTRACT_VERSION, H.CONTRACT_VERSION)
            putStringArray(H.KEY_GRANT_METHODS, arrayOf("engines.execScript", "engines.stop"))
            putStringArray(H.KEY_GRANT_PERMISSIONS, arrayOf("engines", "engines.exec", "agent", "agent.exec"))
            putStringArray(H.KEY_AVAILABLE_OPTIONAL_METHODS, if (advertised) arrayOf("engines.execScript") else emptyArray())
            putInt(H.KEY_GRANT_MAX_REQUEST_BYTES, 32768); putLong(H.KEY_GRANT_MAX_TIMEOUT_MS, 30000)
        }
        override fun dispatch(request: Bundle, callback: IHostCapabilityCallback) {
            val value = AgentJson.objectOf(request.getString(H.KEY_BRIDGE_REQUEST_JSON)!!)
            requests += value
            val result = if (value.string("module") == "engines" && value.string("method") == "execScript") {
                executions.incrementAndGet()
                jsonObject("outcome" to "success".json(), "finished" to true.json(), "executionId" to 93.json(),
                    "resultReported" to true.json(), "result" to jsonObject("answer" to 42.json()), "consoleTail" to jsonArray())
            } else true.json()
            callback.onResponse(Bundle().apply {
                putBoolean(H.KEY_BRIDGE_RESPONSE_OK, true)
                putString(H.KEY_BRIDGE_RESPONSE_JSON, jsonObject("id" to value["id"], "ok" to true.json(), "result" to result).toString())
            })
        }
        override fun destroy(reason: Bundle?) = Unit
    }
    private fun fixture(advertised: Boolean, action: (IAiAgentLink, Model, Capabilities, String, CopyOnWriteArrayList<JsonObject>) -> Unit) {
        // Keep the app visibly foreground while the real service passes its promotion barrier.
        val screen = ActivityScenario.launch(ScriptRootsActivity::class.java)
        val connections = mutableListOf<ServiceConnection>()
        var link: IAiAgentLink? = null
        var settings: IAgentSettings? = null
        var original: JsonObject? = null
        var presets: IPresetStore? = null
        var created = false
        var runId: String? = null
        val presetName = "p93-${UUID.randomUUID()}"
        try {
            val plugin = IAiAgentPlugin.Stub.asInterface(bind(Intent().setClassName(context,
                context.packageName + ".service.WorkbenchFixtureService"), connections))
            settings = IAgentSettings.Stub.asInterface(bind(Intent(context, AgentLocalService::class.java).setAction(SettingsEndpoint.ACTION), connections))
            val settingsEndpoint = checkNotNull(settings)
            original = query(jsonObject("operation" to "get".json()), settingsEndpoint::query).getAsJsonObject("settings").deepCopy()
            val enabled = AgentSettings(toolGroups = setOf("script_dynamic", "user"), voice = false)
            query(jsonObject("operation" to "save".json(), "settings" to SettingsCodec.json(enabled)), settingsEndpoint::query)
            val model = Model(advertised)
            val capabilities = Capabilities(advertised)
            link = plugin.attach(envelope(C.KEY_LINK_CONFIG_JSON, jsonObject("grantSummary" to jsonObject("toolGroups" to jsonArray("script_dynamic".json(), "user".json())))),
                model, capabilities, object : IAiAgentLinkCallback.Stub() {
                    override fun onStatus(status: Bundle?) = Unit
                    override fun onEvent(event: Bundle?) = Unit
                })
            presets = IPresetStore.Stub.asInterface(bind(Intent(context, AgentLocalService::class.java).setAction(PresetEndpoint.ACTION), connections))
            query(jsonObject("operation" to "save".json(), "create" to true.json(), "preset" to
                PresetCodec.encodePreset(Preset(presetName, targetId = "dynamic-fixture:text", toolGroups = setOf("script_dynamic", "user"), memoryScope = "none"))), checkNotNull(presets)::query)
            created = true
            val events = CopyOnWriteArrayList<JsonObject>()
            val response = link.startRun(envelope(C.KEY_RUN_REQUEST_JSON, jsonObject("goal" to "Run the harmless dynamic fixture".json(),
                "options" to jsonObject("preset" to presetName.json(), "interaction" to "plugin".json(), "memory" to false.json(),
                    "budget" to jsonObject("maxSteps" to 3.json(), "maxModelCalls" to 4.json(), "maxDurationMs" to 60000.json())))),
                object : IAiAgentRunCallback.Stub() {
                    override fun onRunEvent(event: Bundle?) {
                        events += AgentJson.objectOf(checkNotNull(event?.getString(C.KEY_RUN_EVENT_JSON)), C.MAX_EVENT_JSON_BYTES)
                    }
                })
            runId = decode(response).string("runId")!!
            action(link, model, capabilities, runId, events)
        } finally {
            try {
                runId?.let { id ->
                    val endpoint = checkNotNull(link)
                    endpoint.cancelRun(envelope(C.KEY_RUN_REF_JSON, jsonObject("runId" to id.json())))
                    await("Fixture task stops before cleanup") {
                        decode(endpoint.getRun(envelope(C.KEY_RUN_REF_JSON, jsonObject("runId" to id.json())))).string("state") in
                            setOf("completed", "partial", "failed", "blocked", "cancelled")
                    }
                    val history = IRunHistory.Stub.asInterface(bind(Intent(context, AgentLocalService::class.java).setAction(HistoryEndpoint.ACTION), connections))
                    val removed = CountDownLatch(1)
                    var removal: Bundle? = null
                    history.query(envelope(C.KEY_RUN_REQUEST_JSON, jsonObject("operation" to "delete".json(), "runId" to id.json())),
                        object : IRunHistoryCallback.Stub() {
                            override fun onResult(response: Bundle?) { removal = response; removed.countDown() }
                        })
                    assertTrue("Fixture history cleanup", removed.await(15, TimeUnit.SECONDS)); decode(checkNotNull(removal))
                }
            } finally {
                try {
                    runCatching { link?.detach(AgentWire.envelope(H.KEY_REASON_JSON, """{"reason":"dynamic-fixture-finished"}""")) }
                    if (created) query(jsonObject("operation" to "delete".json(), "name" to presetName.json()), checkNotNull(presets)::query)
                } finally {
                    try {
                        if (original != null && settings != null) query(jsonObject("operation" to "save".json(), "settings" to original), settings::query)
                    } finally { connections.asReversed().forEach(context::unbindService); screen.close() }
                }
            }
        }
    }

    @Test fun advertisedDynamicToolUsesRealHostLinkConfirmationExecutionAndJournal() = fixture(true) { link, model, capabilities, runId, events ->
        await("Dynamic confirmation must be emitted") { events.any { it.string("type") == "confirmation" } }
        val confirmation = events.single { it.string("type") == "confirmation" }
        assertEquals(source, confirmation.getAsJsonObject("arguments").string("source"))
        assertEquals(true, confirmation.flag("allowRunScope")); assertEquals(0, capabilities.executions.get())
        val reply = jsonObject("runId" to runId.json(), "requestId" to confirmation["requestId"], "allowed" to true.json(), "scope" to "run".json())
        assertEquals(true, decode(link.respond(envelope(C.KEY_RUN_RESPONSE_JSON, reply))).flag("accepted"))
        await("Dynamic fixture completes") { events.any { it.string("type") == "done" } }
        assertEquals("completed", events.single { it.string("type") == "done" }.string("status"))
        assertEquals(1, capabilities.executions.get()); assertEquals(2, model.calls.get())
        val execution = capabilities.requests.single { it.string("method") == "execScript" }
        assertEquals(source, execution.getAsJsonArray("args")[1].asString)
        assertEquals(runId, execution.getAsJsonArray("args")[2].asJsonObject.string("agentRunId"))
        val row = decode(link.getRun(envelope(C.KEY_RUN_REF_JSON, jsonObject("runId" to runId.json()))))
        val step = row.getAsJsonArray("steps").map { it.asJsonObject }.single { it.string("tool") == "script_run_source" }
        assertEquals(source, step.getAsJsonObject("arguments").string("source")); assertEquals("allowed", step.string("confirmation"))
        val observation = AgentJson.objectOf(step.string("observation")!!)
        assertEquals(42L, observation.getAsJsonObject("result").getAsJsonObject("result").number("answer"))
        assertTrue(model.requests.first().getAsJsonArray("messages").any { it.asJsonObject.string("content")!!.contains("\"name\":\"script_run_source\"") })
    }

    @Test fun legacyHostWithoutDynamicAdvertisementHidesTheToolEvenWithGrantAndGroupEnabled() = fixture(false) { _, model, capabilities, _, events ->
        await("Legacy fixture completes") { events.any { it.string("type") == "done" } }
        assertEquals("completed", events.single { it.string("type") == "done" }.string("status"))
        assertEquals(1, model.calls.get()); assertEquals(0, capabilities.executions.get())
        assertFalse(events.any { it.string("type") == "confirmation" })
        assertFalse(model.requests.single().getAsJsonArray("messages").any { it.asJsonObject.string("content")!!.contains("\"name\":\"script_run_source\"") })
    }
}
