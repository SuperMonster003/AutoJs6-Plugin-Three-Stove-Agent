package org.autojs.plugin.three.stove.agent.fakehost

import android.app.Service
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/** A real installed-host UID, with deterministic brokers and no network/device permissions. */
class FakeHostService : Service() {
    private val worker = Executors.newSingleThreadExecutor()
    private var connection: ServiceConnection? = null
    private var link: IAiAgentLink? = null
    private var mode = "hold"
    private val models = AtomicInteger(); private val tools = AtomicInteger()
    private val continuations = AtomicInteger(); private val cancellations = AtomicInteger()
    private var nativeRequest: Pair<String, IAiAgentModelCallback>? = null
    private var nativeSequence = 0
    private var nativeIds = emptyList<String>()
    private val nativeMode get() = mode.startsWith("native-")
    private val visionMode get() = mode.contains("vision")
    @Volatile private var imagesReceived = 0
    @Volatile private var pluginUid = -1
    @Volatile private var observedDenial = false
    private fun enforce() { check(Binder.getCallingUid() == Process.myUid()) }
    private val callback = object : IAiAgentLinkCallback.Stub() {
        override fun onStatus(status: Bundle?) { pluginUid = Binder.getCallingUid() }
        override fun onEvent(event: Bundle?) = Unit
    }
    private val model = object : IAiAgentModelBroker.Stub() {
        override fun getBrokerInfo() = envelope(C.KEY_MODEL_BROKER_INFO_JSON,
            JSONObject("""{"available":true,"providerId":"fake-host","maximumInputBytes":131072,"maximumOutputBytes":65536,"maximumResponseSchemaBytes":16384}""").apply {
                if (nativeMode) { put("toolCallingVersion", 1); put("maximumToolRounds", 16)
                    put("maximumToolResultBytes", 65536); put("maximumToolResultBatchBytes", 131072) }
                if (visionMode) {
                    put("visionVersion", 1); put("maximumImages", 4); put("maximumImageBytes", 4194304)
                    put("maximumTotalImageBytes", 8388608); put("maximumImageEdge", 4096); put("maximumImagePixels", 16777216)
                    put("maximumSessionImages", 16); put("maximumSessionImageBytes", 33554432)
                }
            }.toString()).apply {
            putString(H.KEY_GRANT_JSON, """{"maxInputBytesPerRequest":131072,"maxTotalTokens":1000000,"consumedTokens":0}""")
        }
        override fun listTargets(request: Bundle, callback: IAiAgentModelCallback) {
            val id = JSONObject(request.getString(C.KEY_MODEL_REQUEST_JSON)!!).getString("requestId")
            worker.execute {
                callback.onEvent(envelope(C.KEY_MODEL_EVENT_JSON, JSONObject().put("requestId", id).put("sequence", 1).put("type", "started").toString()))
                callback.onEvent(envelope(C.KEY_MODEL_EVENT_JSON, JSONObject().put("requestId", id).put("sequence", 2).put("type", "completed")
                    .put("targets", org.json.JSONArray("""[{"targetId":"fixture:fake-host","displayName":"Fake host","locality":2,"configured":true,"available":true,"maximumContextBytes":131072,"capabilityIds":[],"supportedControls":["maximum-output-tokens"]}]""").apply {
                        if (nativeMode || mode == "legacy-tools") getJSONObject(0).put("capabilityIds", org.json.JSONArray().put("tools"))
                        if (visionMode) getJSONObject(0).getJSONArray("capabilityIds").put("vision")
                    }).toString()))
            }
        }
        override fun generate(request: Bundle, callback: IAiAgentModelCallback) {
            val body = JSONObject(request.getString(C.KEY_MODEL_REQUEST_JSON)!!)
            val index = models.incrementAndGet()
            if (visionMode) {
                check(body.optBoolean("vision") == (Build.VERSION.SDK_INT >= 30))
                if (index > 1) receiveImages(request, body.getJSONArray("imageRefs"), true)
            }
            if (nativeMode) {
                check(body.has("tools") && !body.getBoolean("structuredJson") && !body.has("responseSchema"))
                check(body.getJSONArray("tools").toString().contains("device_info"))
                worker.execute {
                    nativeRequest = body.getString("requestId") to callback; nativeSequence = 0
                    nativeEvent("started")
                    // The optional total first appears at completion; it must not recharge round one.
                    nativeEvent("usage", JSONObject().put("usage", JSONObject().put("inputTokens", 10).put("outputTokens", 5)))
                    nativeIds = if (visionMode) listOf("capture") else listOf("first", "second")
                    val calls = org.json.JSONArray()
                    nativeIds.forEach { id -> calls.put(JSONObject().put("callId", id).put("name", if (visionMode) "screen_capture" else "device_info")
                        .put("arguments", JSONObject().apply { if (mode == "native-repair" && id == "second") put("unknown", true) })) }
                    nativeEvent("tool_calls", JSONObject().put("round", 1).put("calls", calls))
                }
                return
            }
            check(!body.has("tools")) // Even a tools-capable target cannot opt in on an old host.
            if (index > 1) observedDenial = body.toString().contains("TOOL_DISABLED") || body.toString().contains("CAPABILITY_DENIED")
            if (mode == "hold") return
            val decision = if (index == 1 && visionMode && Build.VERSION.SDK_INT >= 30) """{"kind":"tool","tool":"screen_capture","arguments":{}}"""
                else if (index == 1 && mode == "denied") """{"kind":"tool","tool":"device_info","arguments":{}}"""
                else if (index == 1 && mode == "disabled-tool") """{"kind":"tool","tool":"shell_exec","arguments":{"command":"echo forbidden"}}"""
                else """{"kind":"done","done":{"status":"completed","summary":"Fake host complete","evidence":["Contract fixture"]}}"""
            worker.execute {
                val id = body.getString("requestId")
                callback.onEvent(envelope(C.KEY_MODEL_EVENT_JSON, JSONObject().put("requestId", id).put("sequence", 1).put("type", "started").toString()))
                callback.onEvent(envelope(C.KEY_MODEL_EVENT_JSON, JSONObject().put("requestId", id).put("sequence", 2).put("type", "completed")
                    .put("text", decision).put("targetId", "fixture:fake-host").put("finishReason", 0).toString()))
            }
        }
        override fun cancel(reference: Bundle?) { cancellations.incrementAndGet() }
        override fun submitToolResults(request: Bundle?) {
            check(nativeMode)
            val body = JSONObject(requireNotNull(request).getString(C.KEY_MODEL_REQUEST_JSON)!!)
            if (visionMode) receiveImages(request, body.getJSONArray("results").getJSONObject(0).getJSONArray("imageRefs"), false)
            worker.execute {
                check(body.getString("requestId") == nativeRequest!!.first && body.getInt("round") == 1)
                val results = body.getJSONArray("results")
                check((0 until results.length()).map { results.getJSONObject(it).getString("callId") } == nativeIds)
                check((0 until results.length()).all { results.getJSONObject(it).getBoolean("isError") == !visionMode })
                observedDenial = results.toString().contains(if (mode == "native-repair") "TOOL_ARGUMENTS_INVALID" else "CAPABILITY_DENIED")
                continuations.incrementAndGet()
                nativeEvent("usage", JSONObject().put("usage", JSONObject().put("inputTokens", 25).put("outputTokens", 9).put("totalTokens", 34)))
                nativeEvent("completed", JSONObject().put("text", """{"kind":"done","done":{"status":"completed","summary":"Native fixture complete","evidence":["Tool results received"]}}""")
                    .put("targetId", "fixture:fake-host").put("finishReason", 0))
                nativeRequest = null
            }
        }
        override fun destroy(reason: Bundle?) = Unit
    }
    private fun nativeEvent(type: String, fields: JSONObject = JSONObject()) {
        val (id, callback) = checkNotNull(nativeRequest)
        callback.onEvent(envelope(C.KEY_MODEL_EVENT_JSON, fields.put("requestId", id).put("type", type).put("sequence", ++nativeSequence).toString()))
    }
    @Suppress("DEPRECATION") private fun receiveImages(request: Bundle, refs: org.json.JSONArray, initial: Boolean) {
        val descriptors = request.getParcelableArray(C.KEY_MODEL_IMAGE_FDS)!!.map { it as ParcelFileDescriptor }
        try {
            check(refs.length() == 1 && descriptors.size == 1)
            val ref = refs.getJSONObject(0); check(ref.getInt("descriptorIndex") == 0 && ref.has("messageIndex") == initial)
            val bytes = ParcelFileDescriptor.AutoCloseInputStream(descriptors.single()).use { it.readBytes() }
            check(ref.getLong("byteLength") == bytes.size.toLong() && ref.getString("mimeType") == "image/jpeg")
            check(ref.getString("sha256") == MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) })
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            check(bounds.outMimeType == "image/jpeg" && bounds.outWidth == 1280 && bounds.outHeight == 720)
            check(ref.getInt("width") == 1280 && ref.getInt("height") == 720)
            imagesReceived++
        } finally { descriptors.forEach { runCatching { it.close() } } }
    }
    private fun screenshot(id: String, callback: IHostCapabilityCallback) {
        val bitmap = Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.CYAN) }
        val bytes = try { ByteArrayOutputStream().also { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }.toByteArray() } finally { bitmap.recycle() }
        val file = File.createTempFile("synthetic-screen-", ".png", cacheDir)
        try {
            file.writeBytes(bytes)
            android.system.Os.chmod(file.absolutePath, 0x180) // 0600, matching a host-private screenshot.
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                check(android.system.Os.fstat(fd.fileDescriptor).st_mode and 0x3f == 0) // Only the owner may reopen this inode.
                file.delete()
                callback.onResponse(Bundle().apply {
                    putBoolean(H.KEY_BRIDGE_RESPONSE_OK, true); putParcelable(H.KEY_BRIDGE_PAYLOAD_FD, fd)
                    putLong(H.KEY_BRIDGE_PAYLOAD_BYTES, bytes.size.toLong()); putString(H.KEY_BRIDGE_PAYLOAD_MIME, "image/png")
                    putString(H.KEY_BRIDGE_RESPONSE_JSON, JSONObject().put("id", id).put("ok", true).put("result", JSONObject()
                        .put("schema", "autojs6-bridge-accessibility-screenshot-v1").put("width", 1600).put("height", 900).put("mime", "image/png").put("bytes", bytes.size)
                        .put("payload", JSONObject().put("kind", "descriptor").put("oneShot", true).put("bytes", bytes.size).put("mime", "image/png"))).toString())
                })
            }
        } finally { file.delete() }
    }
    private val capability = object : IHostCapabilityBroker.Stub() {
        override fun getBrokerInfo() = Bundle().apply {
            putInt(H.KEY_CONTRACT_VERSION, H.CONTRACT_VERSION)
            putStringArray(H.KEY_GRANT_METHODS, arrayOf("device.info", "accessibility.screenshot"))
            putStringArray(H.KEY_GRANT_PERMISSIONS, arrayOf("device", "accessibility", "screen_capture"))
            putInt(H.KEY_GRANT_MAX_REQUEST_BYTES, 32768); putLong(H.KEY_GRANT_MAX_TIMEOUT_MS, 30000)
        }
        override fun dispatch(request: Bundle, callback: IHostCapabilityCallback) {
            tools.incrementAndGet()
            if (mode == "native-hold") return
            val id = JSONObject(request.getString(H.KEY_BRIDGE_REQUEST_JSON)!!).getString("id")
            if (visionMode) { worker.execute { screenshot(id, callback) }; return }
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
                models.set(0); tools.set(0); continuations.set(0); cancellations.set(0); observedDenial = false; nativeRequest = null
                imagesReceived = 0
                val ready = CountDownLatch(1); var plugin: IAiAgentPlugin? = null
                val bound = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) { plugin = IAiAgentPlugin.Stub.asInterface(service); ready.countDown() }
                    override fun onServiceDisconnected(name: ComponentName) = Unit
                }
                check(bindService(Intent().setComponent(ComponentName(PLUGIN, "$PLUGIN.ThreeStoveAgentPluginService")), bound, Context.BIND_AUTO_CREATE))
                connection = bound
                check(ready.await(10, TimeUnit.SECONDS))
                link = checkNotNull(plugin).attach(configuration, model, capability, callback)
                Bundle().apply { putBinder("link", checkNotNull(link).asBinder()); putInt("pid", Process.myPid()); putInt("uid", Process.myUid()) }
            }.get(15, TimeUnit.SECONDS)
        }
        override fun stats(): Bundle { enforce(); return Bundle().apply {
            putInt("models", models.get()); putInt("tools", tools.get()); putBoolean("observedDenial", observedDenial); putInt("pluginUid", pluginUid)
            putInt("continuations", continuations.get()); putInt("cancellations", cancellations.get())
            putInt("imagesReceived", imagesReceived)
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
        const val PLUGIN = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
        fun envelope(key: String, json: String) = Bundle().apply { putInt(C.KEY_CONTRACT_VERSION, C.CONTRACT_VERSION); putString(key, json) }
    }
}
