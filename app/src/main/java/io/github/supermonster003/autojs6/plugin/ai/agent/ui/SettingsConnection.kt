package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.*
import android.os.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.model.AgentJson
import io.github.supermonster003.autojs6.plugin.ai.agent.service.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C

/** Bounded asynchronous settings, with callbacks fenced to the visible screen lifetime. */
internal class SettingsConnection(private val context: Context, private val ready: () -> Unit) : ServiceConnection {
    private val main = Handler(Looper.getMainLooper())
    private var endpoint: IAgentSettings? = null
    private var bound = false
    private var active = false
    private var generation = 0
    fun start() {
        active = true; generation++
        val replacement = endpointOverride
        if (replacement != null) { endpoint = replacement; ready() }
        else bound = context.bindService(Intent(context, AgentLocalService::class.java).setAction(SettingsEndpoint.ACTION), this, Context.BIND_AUTO_CREATE)
    }
    fun stop() {
        active = false; generation++; if (bound) context.unbindService(this)
        bound = false; endpoint = null; main.removeCallbacksAndMessages(null)
    }
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) { if (active) { endpoint = IAgentSettings.Stub.asInterface(service); ready() } }
    override fun onServiceDisconnected(name: ComponentName?) { endpoint = null; generation++ }
    fun query(body: JsonObject, complete: (Result<JsonObject>) -> Unit) {
        val current = endpoint ?: run { complete(Result.failure(IllegalStateException("Settings unavailable"))); return }
        val expected = generation
        var finished = false
        fun deliver(result: Result<JsonObject>) { if (!finished && active && expected == generation) { finished = true; complete(result) } }
        val timeout = Runnable { deliver(Result.failure(IllegalStateException("Settings timeout"))) }
        main.postDelayed(timeout, 15_000)
        try { current.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, body), object : IPresetStoreCallback.Stub() {
            override fun onResult(response: Bundle?) {
                val result = runCatching {
                    response?.getString(C.KEY_ERROR_CODE)?.let { AgentWire.closeDescriptors(response); error(it) }
                    AgentJson.objectOf(AgentWire.inline(response, C.KEY_RUN_RESPONSE_JSON, SettingsEndpoint.MAX_RESPONSE_BYTES), SettingsEndpoint.MAX_RESPONSE_BYTES)
                }
                main.post { main.removeCallbacks(timeout); deliver(result) }
            }
        }) } catch (failure: Exception) { main.removeCallbacks(timeout); deliver(Result.failure(failure)) }
    }
    companion object {
        // Controlled instrumentation can exercise real isolated stores without clearing personal data.
        @Volatile internal var endpointOverride: IAgentSettings? = null
    }
}
