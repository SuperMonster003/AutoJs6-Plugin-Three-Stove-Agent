package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.*
import android.os.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.*
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C

/** The private endpoint works without an attached host. Leaving the screen cancels discovery. */
internal class McpConnection(private val context: Context, private val ready: () -> Unit) : ServiceConnection {
    private val main = Handler(Looper.getMainLooper())
    private var endpoint: IAgentSettings? = null
    private var bound = false
    private var active = false
    private var generation = 0
    private val probes = mutableSetOf<String>()
    fun start() {
        active = true; generation++
        endpointOverride?.let { endpoint = it; ready(); return }
        bound = context.bindService(Intent(context, AgentLocalService::class.java).setAction(McpEndpoint.ACTION), this, Context.BIND_AUTO_CREATE)
    }
    fun stop() {
        probes.toList().forEach(::cancel)
        active = false; generation++; if (bound) context.unbindService(this)
        bound = false; endpoint = null; main.removeCallbacksAndMessages(null)
    }
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) { if (active) { endpoint = IAgentSettings.Stub.asInterface(service); ready() } }
    override fun onServiceDisconnected(name: ComponentName?) { endpoint = null; generation++ }
    fun cancel(id: String) {
        probes.remove(id)
        runCatching { endpoint?.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON,
            jsonObject("operation" to "cancel".json(), "probeId" to id.json())), object : IPresetStoreCallback.Stub() {
            override fun onResult(response: Bundle?) { AgentWire.closeDescriptors(response) }
        }) }
    }
    fun query(body: JsonObject, complete: (Result<JsonObject>) -> Unit) {
        val current = endpoint ?: run { complete(Result.failure(IllegalStateException("MCP settings unavailable"))); return }
        val expected = generation; val probe = if (body.string("operation") == "probe") body.string("probeId") else null
        probe?.let(probes::add)
        var finished = false
        fun deliver(result: Result<JsonObject>) { if (!finished && active && expected == generation) { finished = true; probe?.let(probes::remove); complete(result) } }
        val timeout = Runnable { probe?.let(::cancel); deliver(Result.failure(IllegalStateException("MCP request timeout"))) }
        main.postDelayed(timeout, McpEndpoint.PROBE_TIMEOUT_MS + 5000)
        try { current.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, body), object : IPresetStoreCallback.Stub() {
            override fun onResult(response: Bundle?) {
                val result = runCatching {
                    response?.getString(C.KEY_ERROR_CODE)?.let { AgentWire.closeDescriptors(response); error("MCP request failed") }
                    AgentJson.objectOf(AgentWire.inline(response, C.KEY_RUN_RESPONSE_JSON, McpEndpoint.MAX_RESPONSE_BYTES), McpEndpoint.MAX_RESPONSE_BYTES)
                }
                main.post { main.removeCallbacks(timeout); deliver(result) }
            }
        }) } catch (_: Exception) { main.removeCallbacks(timeout); deliver(Result.failure(IllegalStateException("MCP request failed"))) }
    }
    companion object { @Volatile internal var endpointOverride: IAgentSettings? = null }
}
