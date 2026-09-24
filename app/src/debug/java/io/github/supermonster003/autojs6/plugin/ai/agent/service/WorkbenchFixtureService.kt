package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.app.Service
import android.content.Intent
import android.os.*
import io.github.supermonster003.autojs6.plugin.ai.agent.toPluginInfo
import io.github.supermonster003.autojs6.plugin.ai.agent.capabilitiesBundle
import org.autojs.plugin.ai.agent.api.*
import org.autojs.plugin.host.capability.api.IHostCapabilityBroker

/** Debug-only, unexported injection point. No fixture class or component is packaged in release. */
class WorkbenchFixtureService : Service() {
    private val binder = object : IAiAgentPlugin.Stub() {
        private fun runtime(): AgentRuntime {
            check(Binder.getCallingUid() == Process.myUid())
            return AgentRuntime.get(this@WorkbenchFixtureService)
        }
        override fun getInfo() = runtime().info.toPluginInfo()
        override fun getCapabilities() = runtime().info.capabilitiesBundle()
        override fun attach(configuration: Bundle?, model: IAiAgentModelBroker?, capabilities: IHostCapabilityBroker?, callback: IAiAgentLinkCallback?): IAiAgentLink {
            val runtime = runtime()
            val config = LinkConfiguration.parse(AgentWire.control(configuration, AiAgentContract.KEY_LINK_CONFIG_JSON))
            return runtime.attach(config, requireNotNull(model), requireNotNull(capabilities), requireNotNull(callback), Process.myUid()).local
        }
    }
    override fun onBind(intent: Intent?) = binder
}
