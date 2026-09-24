package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.content.Context
import android.os.*
import io.github.supermonster003.autojs6.plugin.ai.agent.aiAgentPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.ai.agent.AiAgentTaskForegroundService
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.autojs.plugin.ai.agent.api.*
import org.autojs.plugin.host.capability.api.IHostCapabilityBroker
import java.io.File

/** One instance in :agent, shared by the exported link, private UI binding and foreground service. */
internal class AgentRuntime private constructor(val context: Context) {
    val info = context.aiAgentPluginRuntimeInfo()
    val verifier = HostCallerVerifier(context)
    fun asset(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }
    val catalog = ToolCatalog(asset("catalog/tools.json"))
    val prompts = PromptCatalog(::asset, catalog)
    val runnerText = asset("runner/texts.json")
    private val policyAssets = listOf("catalog/sensitive-keywords.json", "catalog/payment-keywords.json", "catalog/order-intent-keywords.json").associateWith(::asset)
    fun policy(groups: Set<String>) = ToolPolicy.fromAssets({ checkNotNull(policyAssets[it]) },
        ToolGroup.entries.associateWith { it.id in groups }, availableTools = BinderRunTools.IMPLEMENTED + "script_run")
    val archive by lazy { RunArchive(File(context.filesDir, "runs"), File(context.filesDir, "agent-runs")) }
    val memories = PrivateMemorySource(File(context.filesDir, "agent-memory.json"))
    val presets by lazy { PresetRepository(File(context.filesDir, "agent-presets.json")) }
    @Volatile var current: HostLink? = null; private set
    fun taskChanged() = AiAgentTaskForegroundService.changed()
    @Synchronized fun attach(config: LinkConfiguration, model: IAiAgentModelBroker, capability: IHostCapabilityBroker,
                             callback: IAiAgentLinkCallback, uid: Int): HostLink {
        current?.disconnect(AiAgentContract.LINK_STATE_HOST_UNAVAILABLE)
        presets // Start loading on its own worker before admission; never read disk on Binder.
        return HostLink(this, config, model, capability, callback, uid).also { current = it; it.activate() }
    }
    fun status(): Bundle = current?.status(presentation = true) ?: AgentWire.envelope(AiAgentContract.KEY_STATUS_JSON,
        jsonObject("state" to AiAgentContract.LINK_STATE_DETACHED.json(), "attachedAt" to 0.json(), "queuedCount" to 0.json(),
            "pluginVersion" to info.versionName.json()).toString())
    companion object {
        @Volatile private var instance: AgentRuntime? = null
        fun get(context: Context): AgentRuntime = instance ?: synchronized(this) {
            instance ?: AgentRuntime(context.applicationContext).also { instance = it }
        }
    }
}
