package io.github.supermonster003.autojs6.plugin.ai.agent

import android.app.Service
import android.content.Intent
import android.os.*
import io.github.supermonster003.autojs6.plugin.ai.agent.service.*
import org.autojs.plugin.ai.agent.api.*
import org.autojs.plugin.host.capability.api.IHostCapabilityBroker

/** The exported service and loop live in :agent. Authentication precedes all attachment input. */
class AiAgentPluginService : Service() {
    private lateinit var runtime: AgentRuntime
    override fun onCreate() { super.onCreate(); runtime = AgentRuntime.get(this) }
    private val binder = object : IAiAgentPlugin.Stub() {
        override fun getInfo() = runtime.info.toPluginInfo()
        override fun getCapabilities() = runtime.info.capabilitiesBundle()
        override fun attach(configuration: Bundle?, model: IAiAgentModelBroker?, capabilities: IHostCapabilityBroker?, callback: IAiAgentLinkCallback?): IAiAgentLink {
            val uid = try { runtime.verifier.enforce() } catch (e: SecurityException) { AgentWire.closeDescriptors(configuration); throw e }
            val config = LinkConfiguration.parse(AgentWire.control(configuration, AiAgentContract.KEY_LINK_CONFIG_JSON, 8192))
            requireNotNull(model); requireNotNull(capabilities); requireNotNull(callback)
            return runtime.attach(config, model, capabilities, callback, uid).binder
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
