package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.app.Service
import android.content.Intent
import android.os.*
import org.autojs.plugin.ai.agent.api.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C

/** Private UI connection across main / :agent processes. It never substitutes for host attachment. */
class AgentLocalService : Service() {
    private lateinit var runtime: AgentRuntime
    override fun onCreate() { super.onCreate(); runtime = AgentRuntime.get(this) }
    private fun guard(bundle: Bundle? = null) {
        if (Binder.getCallingUid() != Process.myUid()) { AgentWire.closeDescriptors(bundle); throw SecurityException("Private UI endpoint") }
    }
    private fun call(bundle: Bundle?, operation: (IAiAgentLink) -> Bundle): Bundle {
        guard(bundle)
        val link = runtime.current?.local ?: run { AgentWire.closeDescriptors(bundle); return AgentWire.error(C.ERROR_LINK_DETACHED) }
        return operation(link)
    }
    private val binder = object : IAiAgentLink.Stub() {
        override fun getStatus(): Bundle { guard(); return runtime.status() }
        override fun startRun(request: Bundle?, callback: IAiAgentRunCallback?) = call(request) { it.startRun(request, callback) }
        override fun respond(response: Bundle?) = call(response) { it.respond(response) }
        override fun listRuns(query: Bundle?): Bundle { guard(query); return RunQueries(runtime.archive).list(query) }
        override fun getRun(reference: Bundle?): Bundle { guard(reference); return RunQueries(runtime.archive).get(reference) }
        override fun listPresets(query: Bundle?) = call(query) { it.listPresets(query) }
        override fun cancelRun(reference: Bundle?) { call(reference) { it.cancelRun(reference); Bundle() } }
        override fun updateConfig(configuration: Bundle?) { call(configuration) { it.updateConfig(configuration); Bundle() } }
        override fun detach(reason: Bundle?) { call(reason) { it.detach(reason); Bundle() } }
    }
    private val history by lazy { HistoryEndpoint(runtime.archive, cacheDir, runtime.catalog.tools.map { it.name }.toSet()) }
    private val presets by lazy { PresetEndpoint(runtime, cacheDir) }
    private val memory by lazy { MemoryEndpoint(runtime, cacheDir) }
    override fun onBind(intent: Intent?): IBinder = when (intent?.action) {
        HistoryEndpoint.ACTION -> history
        PresetEndpoint.ACTION -> presets
        MemoryEndpoint.ACTION -> memory
        else -> binder
    }
}
