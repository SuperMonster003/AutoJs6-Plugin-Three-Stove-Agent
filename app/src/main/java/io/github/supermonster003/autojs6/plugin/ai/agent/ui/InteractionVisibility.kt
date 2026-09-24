package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.*
import android.os.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.service.*

/** Only the resumed screen that actually renders this request can suppress its background notification. */
internal class InteractionVisibility(private val context: Context, private val followsRun: Boolean = false) : ServiceConnection {
    private val owner = Binder()
    private var endpoint: IInteractionUi? = null
    private var bound = false
    private var request: Pair<String, String>? = null
    fun render(run: JsonObject?) {
        val pending = run?.getAsJsonObject("pending")
        val next = if (run?.string("interaction") == "plugin" && WorkbenchText.active(run))
            run.string("runId")?.let { id ->
                if (followsRun) id to "*" else pending?.takeIf { it.flag("submitted") != true }?.string("requestId")?.let { id to it }
            } else null
        if (next != request) { request = next; publish() }
    }
    fun start() {
        if (!bound) bound = context.bindService(Intent(context, AgentLocalService::class.java).setAction(InteractionPresentation.ACTION), this, Context.BIND_AUTO_CREATE)
    }
    fun stop() {
        runCatching { endpoint?.present(owner, null, null) }; endpoint = null
        if (bound) { bound = false; context.unbindService(this) }
    }
    private fun publish() { runCatching { endpoint?.present(owner, request?.first, request?.second) } }
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (bound) { endpoint = IInteractionUi.Stub.asInterface(service); publish() }
    }
    override fun onServiceDisconnected(name: ComponentName?) { endpoint = null }
}
