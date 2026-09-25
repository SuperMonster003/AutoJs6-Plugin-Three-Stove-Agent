package org.autojs.plugin.ai.agent.fakehost

import android.app.Service
import android.content.*
import android.os.*
import org.autojs.plugin.ai.agent.api.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import org.autojs.plugin.host.capability.api.*
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** A real installed-host UID, with deterministic brokers and no network/device permissions. */
class FakeHostService : Service() {
    private val worker = Executors.newSingleThreadExecutor()
    private var connection: ServiceConnection? = null
    private var link: IAiAgentLink? = null
    private var mode = "hold"
    private val models = AtomicInteger(); private val tools = AtomicInteger()
    @Volatile private var pluginUid = -1
    @Volatile private var observedDenial = false
    private fun enforce() { check(Binder.getCallingUid() == Process.myUid()) }
    private val callback = object : IAiAgentLinkCallback.Stub() {
        override fun onStatus(status: Bundle?) { pluginUid = Binder.getCallingUid() }
        override fun onEvent(event: Bundle?) = Unit
    }
    private val model = object : IAiAgentModelBroker.Stub() {
        override fun getBrokerInfo() = envelope(C.KEY_MODEL_BROKER_INFO_JSON,
            """{"available":true,"providerId":"fake-host","maximumInputBytes":131072,"maximumOutputBytes":65536,"maximumResponseSchemaBytes":16384}""").apply {
            putString(H.KEY_GRANT_JSON, """{"maxInputBytesPerRequest":131072,"maxTotalTokens":1000000,"consumedTokens":0}""")
        }
        override fun listTargets(request: Bundle, callback: IAiAgentModelCallback) {
            val id = JSONObject(request.getString(C.KEY_MODEL_REQUEST_JSON)!!).getString("requestId")
            worker.execute {
                callback.onEvent(envelope(C.KEY_MODEL_EVENT_JSON, JSONObject().put("requestId", id).put("sequence", 1).put("type", "started").toString()))
                callback.onEvent(envelope(C.KEY_MODEL_EVENT_JSON, JSONObject().put("requestId", id).put("sequence", 2).put("type", "completed")
                    .put("targets", org.json.JSONArray("""[{"targetId":"fixture:fake-host","displayName":"Fake host","locality":2,"configured":true,"available":true,"maximumContextBytes":131072,"capabilityIds":[],"supportedControls":["maximum-output-tokens"]}]""")).toString()))
            }
        }
        override fun generate(request: Bundle, callback: IAiAgentModelCallback) {
            val body = JSONObject(request.getString(C.KEY_MODEL_REQUEST_JSON)!!)
            val index = models.incrementAndGet()
            if (index > 1) observedDenial = body.toString().contains("TOOL_DISABLED") || body.toString().contains("CAPABILITY_DENIED")
            if (mode == "hold") return
            val decision = if (index == 1 && mode == "denied") """{"kind":"tool","tool":"device_info","arguments":{}}"""
                else if (index == 1 && mode == "disabled-tool") """{"kind":"tool","tool":"shell_exec","arguments":{"command":"echo forbidden"}}"""
                else """{"kind":"done","done":{"status":"completed","summary":"Fake host complete","evidence":["Contract fixture"]}}"""
            worker.execute {
                val id = body.getString("requestId")
                callback.onEvent(envelope(C.KEY_MODEL_EVENT_JSON, JSONObject().put("requestId", id).put("sequence", 1).put("type", "started").toString()))
                callback.onEvent(envelope(C.KEY_MODEL_EVENT_JSON, JSONObject().put("requestId", id).put("sequence", 2).put("type", "completed")
                    .put("text", decision).put("targetId", "fixture:fake-host").put("finishReason", 0).toString()))
            }
        }
        override fun cancel(reference: Bundle?) = Unit
        override fun destroy(reason: Bundle?) = Unit
    }
    private val capability = object : IHostCapabilityBroker.Stub() {
        override fun getBrokerInfo() = Bundle().apply {
            putInt(H.KEY_CONTRACT_VERSION, H.CONTRACT_VERSION)
            putStringArray(H.KEY_GRANT_METHODS, arrayOf("device.info")); putStringArray(H.KEY_GRANT_PERMISSIONS, arrayOf("device"))
            putInt(H.KEY_GRANT_MAX_REQUEST_BYTES, 32768); putLong(H.KEY_GRANT_MAX_TIMEOUT_MS, 30000)
        }
        override fun dispatch(request: Bundle, callback: IHostCapabilityCallback) {
            tools.incrementAndGet()
            val id = JSONObject(request.getString(H.KEY_BRIDGE_REQUEST_JSON)!!).getString("id")
            worker.execute { callback.onResponse(Bundle().apply {
                putBoolean(H.KEY_BRIDGE_RESPONSE_OK, false)
                putString(H.KEY_BRIDGE_RESPONSE_JSON, JSONObject().put("id", id).put("ok", false)
                    .put("error", JSONObject().put("code", "ERR_AUTOJS6_BRIDGE_PERMISSION_DENIED").put("category", H.ERROR_CAPABILITY_DENIED).put("message", "Fixture grant denied")).toString())
            }) }
        }
        override fun destroy(reason: Bundle?) = Unit
    }
    private val driver = object : IFakeHostDriver.Stub() {
        override fun attach(configuration: Bundle, nextMode: String): Bundle {
            enforce()
            return worker.submit<Bundle> {
                check(connection == null); mode = nextMode
                models.set(0); tools.set(0); observedDenial = false
                val ready = CountDownLatch(1); var plugin: IAiAgentPlugin? = null
                val bound = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) { plugin = IAiAgentPlugin.Stub.asInterface(service); ready.countDown() }
                    override fun onServiceDisconnected(name: ComponentName) = Unit
                }
                check(bindService(Intent().setComponent(ComponentName(PLUGIN, "$PLUGIN.AiAgentPluginService")), bound, Context.BIND_AUTO_CREATE))
                connection = bound
                check(ready.await(10, TimeUnit.SECONDS))
                link = checkNotNull(plugin).attach(configuration, model, capability, callback)
                Bundle().apply { putBinder("link", checkNotNull(link).asBinder()); putInt("pid", Process.myPid()); putInt("uid", Process.myUid()) }
            }.get(15, TimeUnit.SECONDS)
        }
        override fun stats(): Bundle { enforce(); return Bundle().apply {
            putInt("models", models.get()); putInt("tools", tools.get()); putBoolean("observedDenial", observedDenial); putInt("pluginUid", pluginUid)
        } }
        override fun detach() {
            enforce(); link?.detach(envelope(H.KEY_REASON_JSON, "{}")); link = null
            connection?.let { unbindService(it) }; connection = null
        }
        override fun die() { enforce(); Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 100) }
    }
    override fun onBind(intent: Intent): IBinder = driver
    override fun onDestroy() { connection?.let { unbindService(it) }; worker.shutdownNow(); super.onDestroy() }
    companion object {
        const val PLUGIN = "io.github.supermonster003.autojs6.plugin.ai.agent"
        fun envelope(key: String, json: String) = Bundle().apply { putInt(C.KEY_CONTRACT_VERSION, C.CONTRACT_VERSION); putString(key, json) }
    }
}
