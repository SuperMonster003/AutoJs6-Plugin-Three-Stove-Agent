package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.*
import android.os.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.model.AgentJson
import io.github.supermonster003.autojs6.plugin.ai.agent.service.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import java.util.concurrent.Executors

internal class MemoryConnection(private val context: Context, private val ready: () -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var endpoint: IMemoryStore? = null
    val connected get() = endpoint != null
    private var bound = false
    private var generation = 0
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) { endpoint = IMemoryStore.Stub.asInterface(binder); ready() }
        override fun onServiceDisconnected(name: ComponentName?) { endpoint = null; generation++ }
    }
    fun start() { generation++; bound = context.bindService(Intent(context, AgentLocalService::class.java).setAction(MemoryEndpoint.ACTION), connection, Context.BIND_AUTO_CREATE) }
    fun stop() { generation++; if (bound) context.unbindService(connection); bound = false; endpoint = null; main.removeCallbacksAndMessages(null) }
    fun close() { worker.shutdown() }
    fun query(body: JsonObject, complete: (Result<JsonObject>) -> Unit) {
        val current = endpoint ?: run { complete(Result.failure(IllegalStateException("Memory unavailable"))); return }
        val expected = generation; var finished = false
        fun deliver(result: Result<JsonObject>) { if (!finished && bound && expected == generation) { finished = true; complete(result) } }
        val timeout = Runnable { deliver(Result.failure(IllegalStateException("Memory timeout"))) }; main.postDelayed(timeout, 15000)
        try { current.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, body), object : IMemoryStoreCallback.Stub() {
            override fun onResult(response: Bundle?) {
                val payload = runCatching {
                    response?.getString(C.KEY_ERROR_CODE)?.let { AgentWire.closeDescriptors(response); error(it) }
                    AgentWire.take(response, C.KEY_RUN_RESPONSE_JSON, C.KEY_PAYLOAD_FD, 32768, MemoryEndpoint.MAX_RESPONSE_BYTES)
                }
                try { worker.execute {
                    val result = payload.mapCatching { it.use { owned -> AgentJson.objectOf(owned.read(), MemoryEndpoint.MAX_RESPONSE_BYTES) } }
                    main.post { main.removeCallbacks(timeout); deliver(result) }
                } } catch (_: Exception) { payload.getOrNull()?.close() }
            }
        }) } catch (failure: Exception) { main.removeCallbacks(timeout); deliver(Result.failure(failure)) }
    }
}
