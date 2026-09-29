package io.github.supermonster003.autojs6.plugin.three.stove.agent.service

import android.content.Context
import android.os.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.threeStoveAgentPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ThreeStoveAgentTaskForegroundService
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import org.autojs.plugin.three.stove.agent.api.*
import org.autojs.plugin.host.capability.api.IHostCapabilityBroker
import java.io.File

/** One instance in :agent, shared by the exported link, private UI binding and foreground service. */
internal class AgentRuntime internal constructor(val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var initialized = false
    private var floating: io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.FloatingBall? = null
    val info = context.threeStoveAgentPluginRuntimeInfo()
    val verifier = HostCallerVerifier(context)
    fun asset(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }
    val catalog = ToolCatalog.fromAssets(::asset)
    val prompts = PromptCatalog(::asset, catalog)
    val runnerText = asset("runner/texts.json")
    private val policyAssets = listOf(ToolPolicy.SENSITIVE_KEYWORDS_ASSET, "catalog/payment-keywords.json", "catalog/order-intent-keywords.json",
        ToolPolicy.PAYMENT_PACKAGES_ASSET).associateWith(::asset)
    /** Packaged tables widened by the private risk recognition settings (P13); nothing here can remove a built-in entry. */
    fun policy(groups: Set<String>, settings: AgentSettings) = ToolPolicy.fromAssets({ checkNotNull(policyAssets[it]) },
        ToolGroup.entries.associateWith { it.id in groups }, paymentPackages = settings.riskPackages, availableTools = ToolNames.ALL,
        extraKeywords = settings.riskKeywords)
    val archive by lazy { RunArchive(File(context.filesDir, "runs"), File(context.filesDir, "agent-runs")) }
    val memories = MemoryRepository(File(context.filesDir, "memories"), File(context.filesDir, "agent-memory.json"))
    val presets by lazy { PresetRepository(File(context.filesDir, "agent-presets.json"), ::presentationChanged) }
    val settings = SettingsRepository(File(context.filesDir, "agent-settings.json"), ::presentationChanged)
    val mcp = McpRepository(context, ::presentationChanged)
    val admissionLock = Any()
    @Volatile var maintenance = false; private set
    fun beginMaintenance(): Boolean = synchronized(admissionLock) {
        if (maintenance || current?.liveRuns()?.isNotEmpty() == true) false else { maintenance = true; true }
    }
    fun endMaintenance() { synchronized(admissionLock) { maintenance = false } }
    @Volatile var current: HostLink? = null; private set
    val interactions by lazy { InteractionPresentation(this) }
    val alerts by lazy { TaskAlerts(this) }
    fun taskChanged() { ThreeStoveAgentTaskForegroundService.changed(); interactions.changed(); presentationChanged() }
    fun presentationChanged() { main.post {
        if (!initialized) return@post
        if (runCatching { settings.snapshot().floating }.getOrDefault(false)) {
            if (floating == null) floating = io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.FloatingBall(this)
            floating?.changed()
        } else { floating?.close(); floating = null }
    } }
    init { initialized = true; presentationChanged() }
    @Synchronized fun attach(config: LinkConfiguration, model: IThreeStoveAgentModelBroker, capability: IHostCapabilityBroker,
                             callback: IThreeStoveAgentLinkCallback, uid: Int): HostLink {
        current?.disconnect(ThreeStoveAgentContract.LINK_STATE_HOST_UNAVAILABLE)
        presets // Start loading on its own worker before admission; never read disk on Binder.
        return HostLink(this, config, model, capability, callback, uid).also { current = it; it.activate() }
    }
    fun status(): Bundle = current?.status(presentation = true) ?: AgentWire.envelope(ThreeStoveAgentContract.KEY_STATUS_JSON,
        jsonObject("state" to ThreeStoveAgentContract.LINK_STATE_DETACHED.json(), "attachedAt" to 0.json(), "queuedCount" to 0.json(),
            "pluginVersion" to info.versionName.json(), "voiceEnabled" to runCatching { settings.snapshot().voice }.getOrDefault(false).json(),
            "accessMode" to runCatching { settings.snapshot().accessMode }.getOrDefault("standard").json(),
            "floatingEnabled" to runCatching { settings.snapshot().floating }.getOrDefault(false).json()).toString())
    companion object {
        @Volatile private var instance: AgentRuntime? = null
        fun get(context: Context): AgentRuntime = instance ?: synchronized(this) {
            instance ?: AgentRuntime(context.applicationContext).also { instance = it }
        }
    }
}
