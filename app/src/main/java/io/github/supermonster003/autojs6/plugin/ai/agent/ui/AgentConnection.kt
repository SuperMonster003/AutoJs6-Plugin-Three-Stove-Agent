package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.*
import android.os.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.service.AgentLocalService
import io.github.supermonster003.autojs6.plugin.ai.agent.service.AgentWire
import org.autojs.plugin.ai.agent.api.IAiAgentLink
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import java.util.concurrent.Executors

internal data class WorkbenchSnapshot(val status: JsonObject, val runs: List<JsonObject>, val run: JsonObject?, val presets: List<String>, val defaultPreset: String = "default")

/** A visible screen's private connection. Bounded polling stops with the screen; it does not own tasks. */
internal class AgentConnection(private val context: Context, private val receive: (WorkbenchSnapshot) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val completions = Handler(Looper.getMainLooper())
    private var closed = false
    private val worker = Executors.newSingleThreadExecutor()
    private var link: IAiAgentLink? = null
    private var bound = false
    private var generation = 0
    private var polling = false
    private val pollAgain = Runnable { refresh() }
    var selectedId: String? = null
    var preferRunning = true
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) { link = IAiAgentLink.Stub.asInterface(binder); refresh() }
        override fun onServiceDisconnected(name: ComponentName?) {
            generation++; polling = false; main.removeCallbacks(pollAgain); link = null
            receive(WorkbenchSnapshot(jsonObject("state" to "host-unavailable".json()), emptyList(), null, emptyList()))
        }
    }
    fun start() { generation++; bound = context.bindService(Intent(context, AgentLocalService::class.java), connection, Context.BIND_AUTO_CREATE) }
    fun stop() {
        generation++; polling = false; main.removeCallbacksAndMessages(null)
        if (bound) context.unbindService(connection)
        bound = false; link = null
    }
    fun close() { closed = true; completions.removeCallbacksAndMessages(null); worker.shutdown() }
    fun refresh() {
        val current = link ?: return
        if (polling || !bound) return
        main.removeCallbacks(pollAgain)
        val expected = generation
        val selected = selectedId
        val prefer = preferRunning
        polling = true
        worker.execute {
            val snapshot = runCatching {
                val status = decode(current.status, C.KEY_STATUS_JSON)
                val recent = decode(current.listRuns(request(C.KEY_RUN_REQUEST_JSON, jsonObject("limit" to 20.json()))))
                    .getAsJsonArray("runs").map { it.asJsonObject }
                val id = if (prefer) status.string("runningRunId") ?: selected ?: recent.firstOrNull()?.string("runId") else selected
                val run = id?.let { runCatching { decode(current.getRun(request(C.KEY_RUN_REF_JSON, jsonObject("runId" to it.json())))) }.getOrNull() }
                val presets = if (status.string("state") == C.LINK_STATE_ATTACHED) runCatching {
                    decode(current.listPresets(request(C.KEY_RUN_REQUEST_JSON)))
                }.getOrNull() else null
                WorkbenchSnapshot(status, recent, run, presets?.getAsJsonArray("presets")?.map { it.asJsonObject.string("id")!! }.orEmpty(), presets?.string("defaultName") ?: "default")
            }.getOrElse { WorkbenchSnapshot(jsonObject("state" to "host-unavailable".json()), emptyList(), null, emptyList()) }
            main.post {
                if (expected != generation || current !== link || !bound) return@post
                polling = false; receive(snapshot)
                main.postDelayed(pollAgain, if (snapshot.runs.any { WorkbenchText.active(it) }) 500 else 2000)
            }
        }
    }
    fun command(action: (IAiAgentLink) -> Bundle, completeWhileStopped: Boolean = false, complete: (Result<JsonObject>) -> Unit) {
        val current = link
        if (current == null) { complete(Result.failure(IllegalStateException(C.ERROR_LINK_DETACHED))); return }
        val expected = generation
        worker.execute {
            val result = runCatching { decode(action(current)) }
            // A confirmed app launch may stop its confirmation activity before the reply arrives.
            // Its close acknowledgement must survive onStop, but never onDestroy.
            val handler = if (completeWhileStopped) completions else main
            handler.post {
                if (!closed && (completeWhileStopped || expected == generation && bound)) {
                    complete(result)
                    if (expected == generation && bound) refresh()
                }
            }
        }
    }
    companion object {
        fun request(key: String, body: JsonObject = JsonObject()) = AgentWire.envelope(key, body.toString())
        fun decode(bundle: Bundle, key: String = C.KEY_RUN_RESPONSE_JSON): JsonObject {
            bundle.getString(C.KEY_ERROR_CODE)?.let { throw IllegalStateException(it) }
            return AgentJson.objectOf(AgentWire.inline(bundle, key))
        }
    }
}
