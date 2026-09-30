package io.github.supermonster003.autojs6.plugin.three.stove.agent.service

import android.os.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ThreeStoveAgentTaskForegroundService
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.mcp.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.nodes.ObservationCapabilities
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.scripts.*
import org.autojs.plugin.three.stove.agent.api.*
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.autojs.plugin.host.capability.api.IHostCapabilityBroker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Link-owned queue and broker adapters. Control methods validate and enqueue; all remote work is asynchronous. */
internal class HostLink(private val runtime: AgentRuntime, initialConfig: LinkConfiguration,
                        private val remoteModel: IThreeStoveAgentModelBroker, private val remoteTools: IHostCapabilityBroker,
                        private val callback: IThreeStoveAgentLinkCallback, private val ownerUid: Int) {
    private val scheduler = SerialRunScheduler()
    private val workers = LinkWorkers()
    private val scripts = ScriptCatalogClient(scheduler::nowMs)
    private val model = BinderModelBroker(runtime.context, remoteModel, ownerUid, workers)
    private val mcp = McpToolSource(workers.io, runtime.info.versionName)
    private val archive get() = runtime.archive
    private val attachedAt = System.currentTimeMillis()
    private val fallbacks = linkedMapOf<String, SchemaFallbacks>()
    private val clients = linkedMapOf<String, ModelClient>() // Used only on IO workers under its monitor.
    private val active = ConcurrentHashMap<String, AgentRunner>()
    private val sinks = ConcurrentHashMap<String, RunSink>()
    private val retired = AtomicBoolean()
    @Volatile private var config = initialConfig
    @Volatile private var state = C.LINK_STATE_ATTACHED
    @Volatile private var modelName: String? = null
    private val previewStarted = AtomicBoolean()
    private val death = IBinder.DeathRecipient { disconnect(C.LINK_STATE_HOST_UNAVAILABLE) }
    private val watched = listOf(remoteModel.asBinder(), remoteTools.asBinder(), callback.asBinder()).distinct()
    private val emptyPolicy = ToolPolicy(availableTools = emptySet())
    private val unusedModel = object : RunModel {
        override fun generate(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long, callback: (PortResult<ModelReply>) -> Unit): Cancellation = error("Preparation required")
    }
    private val unusedTools = object : RunTools {
        override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation = error("Preparation required")
        override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation = error("Preparation required")
    }
    private val queue = RunQueue(scheduler, runtime.catalog, emptyPolicy, RunContextCompiler { error("Preparation required") },
        unusedModel, unusedTools, { RunnerText(runtime.runnerText, it) })
    val binder: IThreeStoveAgentLink = endpoint(hostValidatedRoots = true) { runtime.verifier.enforceOwner(ownerUid) }
    val local: IThreeStoveAgentLink = endpoint { if (Binder.getCallingUid() != Process.myUid()) throw SecurityException("Private link") }

    fun activate() {
        try { watched.forEach { it.linkToDeath(death, 0) } } catch (_: Exception) { disconnect(C.LINK_STATE_HOST_UNAVAILABLE) }
        notifyStatus()
    }
    fun status(presentation: Boolean = false): Bundle {
        if (presentation && state == C.LINK_STATE_ATTACHED && previewStarted.compareAndSet(false, true)) {
            model.select(null) { if (it is PortResult.Success) modelName = it.value.displayName }
        }
        val runs = active.values.filter { !it.state.terminal }
        return AgentWire.envelope(C.KEY_STATUS_JSON, jsonObject("state" to state.json(), "attachedAt" to attachedAt.json(),
            "runningRunId" to (runs.firstOrNull { it.state != RunState.QUEUED }?.id?.json() ?: JsonNull.INSTANCE),
            "queuedCount" to runs.count { it.state == RunState.QUEUED }.coerceAtMost(RunLimits.QUEUED_RUNS).json(),
            "pluginVersion" to runtime.info.versionName.json(), "scriptRoots" to JsonArray().apply { config.roots.sorted().forEach(::add) })
            .apply { if (presentation) {
                modelName?.let { addProperty("modelName", it) }
                addProperty("voiceEnabled", runCatching { runtime.settings.snapshot().voice }.getOrDefault(false))
                addProperty("fullAccessEnabled", runCatching { runtime.settings.snapshot().fullAccess }.getOrDefault(false))
                addProperty("accessMode", runCatching { runtime.settings.snapshot().accessMode }.getOrDefault("standard"))
                addProperty("floatingEnabled", runCatching { runtime.settings.snapshot().floating }.getOrDefault(false))
            } }.toString())
    }
    fun liveRuns(): List<JsonObject> = active.values.filter { !it.state.terminal }.mapNotNull { archive.summary(it.id) }
    fun cancelLocal(id: String) { active[id]?.cancel() }
    fun presetConfiguration(): LinkConfiguration = config.withSettings(runtime.settings.snapshot())
    fun targets(callback: (PortResult<List<SelectedModel>>) -> Unit): Cancellation = if (state == C.LINK_STATE_ATTACHED) model.list(callback) else {
        callback(PortResult.Failure(RunError.HOST_UNAVAILABLE)); Cancellation.NONE
    }
    fun presetList(): Bundle {
        val snapshot = runtime.presets.snapshot()
        return AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, jsonObject("defaultName" to snapshot.defaultName.json(), "presets" to JsonArray().apply {
            snapshot.presets.forEach { preset -> add(jsonObject("id" to preset.name.json(), "toolGroups" to JsonArray().apply {
                preset.groups(presetConfiguration().groups).sorted().forEach(::add)
            })) }
        }).toString())
    }
    @Synchronized fun disconnect(next: String) {
        if (state != C.LINK_STATE_ATTACHED) return
        state = next
        if (next == C.LINK_STATE_DETACHED) queue.detach() else queue.hostUnavailable()
        model.close()
        scripts.close()
        watched.forEach { runCatching { it.unlinkToDeath(death, 0) } }
        notifyStatus()
        scheduler.execute(::retireIfIdle)
    }
    private fun retireIfIdle() {
        if (state == C.LINK_STATE_ATTACHED || active.values.any { !it.state.terminal } || !retired.compareAndSet(false, true)) return
        sinks.values.forEach(RunSink::close); sinks.clear()
        runCatching { workers.io.execute { runCatching { remoteModel.destroy(AgentWire.reason(state)) }; runCatching { remoteTools.destroy(AgentWire.reason(state)) } } }
        synchronized(clients) { clients.clear() }
        synchronized(fallbacks) { fallbacks.clear() }
        workers.close(); scheduler.close()
        runtime.taskChanged()
    }
    private fun notifyStatus() {
        runtime.presentationChanged()
        runCatching { workers.callbacks.execute { runCatching { callback.onStatus(status()) } } }
    }

    private fun prepare(request: StartRequest, basePolicy: ToolPolicy, configuration: LinkConfiguration, runId: () -> String) = RunPreparation { complete ->
        val stopped = AtomicBoolean()
        val completed = AtomicBoolean()
        val selecting = AtomicReference<Cancellation>(Cancellation.NONE)
        val catalogLoading = AtomicReference<Cancellation>(Cancellation.NONE)
        val memoryLoading = AtomicReference<Cancellation>(Cancellation.NONE)
        val mcpLoading = AtomicReference<Cancellation>(Cancellation.NONE)
        val external = AtomicReference<McpSnapshot?>(null)
        val cleanup = Cancellation { external.getAndSet(null)?.close?.cancel() }
        fun finish(value: PortResult<RunComponents>) {
            if (!completed.compareAndSet(false, true)) return
            if (stopped.get() || value is PortResult.Failure) cleanup.cancel()
            if (!stopped.get()) complete(value)
        }
        fun prepared(snapshot: McpSnapshot?) {
        if (snapshot != null) external.set(snapshot)
        if (stopped.get()) { cleanup.cancel(); return }
        try { workers.io.execute {
            if (stopped.get()) return@execute
            try {
                val catalog = snapshot?.catalog ?: runtime.catalog
                val prompts = if (snapshot == null) runtime.prompts else PromptCatalog(runtime::asset, catalog)
                val catalogFallbacks = synchronized(fallbacks) { fallbacks.getOrPut(catalog.fingerprint) {
                    if (fallbacks.size >= 32) fallbacks.remove(fallbacks.keys.first())
                    SchemaFallbacks(DecisionSchema(catalog))
                } }
                val grant = remoteTools.brokerInfo
                if (grant.hasFileDescriptors()) { AgentWire.closeDescriptors(grant); throw WireFailure(C.ERROR_INVALID_REQUEST) }
                require(!grant.hasFileDescriptors() && grant.getInt(H.KEY_CONTRACT_VERSION) == H.CONTRACT_VERSION)
                val methods = requireNotNull(grant.getStringArray(H.KEY_GRANT_METHODS)).toSet().also { require(it.size <= 256) }
                val permissions = requireNotNull(grant.getStringArray(H.KEY_GRANT_PERMISSIONS)).toSet().also { require(it.size <= 128) }
                val maxRequest = grant.getInt(H.KEY_GRANT_MAX_REQUEST_BYTES).also { require(it in 1..H.MAX_BRIDGE_INLINE_JSON_BYTES) }
                val maxTimeout = grant.getLong(H.KEY_GRANT_MAX_TIMEOUT_MS).also { require(it in 1..H.MAX_TOOL_TIMEOUT_MS) }
                val effectiveMethods = methods.intersect(configuration.methods ?: methods)
                val effectivePermissions = permissions.intersect(configuration.permissions ?: permissions)
                val optional = grant.getStringArray(H.KEY_AVAILABLE_OPTIONAL_METHODS)?.also { values -> require(values.size <= 256 && values.all { it.length <= 128 }) }?.toSet().orEmpty()
                val dynamicAvailable = DynamicScriptSource.available(optional, effectiveMethods, effectivePermissions)
                val observationPolicy = basePolicy.withExternalTools(catalog.tools.filter { it.external != null })
                    .withOcrAvailability(ObservationCapabilities.ocrAvailable(optional, effectiveMethods, effectivePermissions))
                    .withAvailableTools(catalog.tools.filter { it.name != ToolNames.SCRIPT_RUN_SOURCE || dynamicAvailable }.map { it.name }.toSet())
                val toolAdapter = BinderRunTools(remoteTools, ownerUid, workers, scheduler, catalog,
                    { state == C.LINK_STATE_ATTACHED }, effectiveMethods, effectivePermissions, maxRequest, maxTimeout,
                    ScreenCaptureTransport(runtime.context, remoteTools, ownerUid, workers) { state == C.LINK_STATE_ATTACHED })
                val catalogAllowed = "agent.listScripts" in methods && "agent" in permissions &&
                    configuration.methods?.contains("agent.listScripts") != false && configuration.permissions?.contains("agent") != false &&
                    observationPolicy.isEnabled(checkNotNull(catalog[ToolNames.SCRIPT_CATALOG]))
                val scriptTools = ScriptCatalogTools(scripts, request.scriptRoots, ScriptCatalogSource(toolAdapter::dispatch), toolAdapter, catalogAllowed)
                val scriptRunAllowed = catalogAllowed && listOf("agent.readManifest", "agent.execRegistered", "engines.stop").all {
                    it in methods && configuration.methods?.contains(it) != false
                } && listOf("agent.exec", "engines", "engines.exec").all { it in permissions && configuration.permissions?.contains(it) != false } &&
                    observationPolicy.isEnabled(checkNotNull(catalog[ToolNames.SCRIPT_RUN]))
                val registeredTools = RegisteredScriptTools(scripts, request.scriptRoots, ScriptCatalogSource(toolAdapter::dispatch),
                    scriptTools, DecisionValidator(catalog), scriptRunAllowed, scheduler::nowMs)
                val builtInTools = MemoryTools(runtime.memories, request.preset, request.memoryScope, runId, { state == C.LINK_STATE_ATTACHED },
                    DynamicScriptTools(ScriptExecutionTools(registeredTools, ScriptInvoker(ScriptCatalogSource(toolAdapter::dispatch), runId, request.preset)),
                        ScriptCatalogSource(toolAdapter::dispatch), dynamicAvailable && observationPolicy.isEnabled(checkNotNull(catalog[ToolNames.SCRIPT_RUN_SOURCE])),
                        runId, request.preset, maxTimeout))
                val executionTools = snapshot?.wrap(builtInTools) ?: builtInTools
                if (stopped.get()) return@execute
                val handle = model.select(request.target) { outcome ->
                    when (outcome) {
                        is PortResult.Failure -> finish(outcome)
                        is PortResult.Success -> try {
                            val selected = outcome.value
                            modelName = selected.displayName
                            runCatching { archive.model(runId(), selected) }
                            val original = selected.target
                            val policy = observationPolicy.withVisionAvailability(original.vision != null && Build.VERSION.SDK_INT >= 30 &&
                                "accessibility.screenshot" in effectiveMethods && effectivePermissions.containsAll(listOf("accessibility", "screen_capture")))
                            val schema = catalogFallbacks.select(original.schemaTarget, policy)
                            val target = if ((schema.responseSchemaJson?.toByteArray(Charsets.UTF_8)?.size ?: 0) <= selected.maximumSchemaBytes) original
                                else ModelTarget(original.providerId, original.targetId, original.locality, original.protocol, false,
                                    original.maximumContextBytes, original.maximumOutputBytes, original.supportsStreaming, original.supportsOutputLimit, original.nativeTools, original.vision)
                            val key = listOf(target.providerId, target.targetId, target.locality, target.structuredJson, target.maximumContextBytes,
                                target.maximumOutputBytes, target.supportsOutputLimit, target.supportsStreaming, target.nativeTools, target.vision,
                                request.groups.sorted(), policy.ocrAvailable, policy.visionAvailable, dynamicAvailable, catalog.fingerprint).toString()
                            val client = synchronized(clients) {
                                clients.getOrPut(key) {
                                    if (clients.size >= 32) clients.remove(clients.keys.first())
                                    ModelClient(model, target, policy, catalogFallbacks, scheduler)
                                }
                            }
                            val format = client.initialFormat(request.options.format)
                            fun compiled(presentation: ScriptPresentation?) {
                                if (stopped.get()) return
                                val memoryCall = runtime.memories.snapshot(request.preset, request.memory, request.memoryScope) { memory ->
                                    if (stopped.get()) return@snapshot
                                    try { finish(PortResult.Success(RunComponents(
                                        ContextCompiler(prompts, catalog, policy, target, format,
                                            ContextLimits(grantMaximumBytes = minOf(configuration.maxInput, selected.maximumInputBytes)), request.context,
                                            memories = memory.entries, scripts = presentation, memoryTruncated = memory.truncated,
                                            memoryUnavailable = memory.unavailable, memoryScopes = if ("memory" !in request.groups) emptyList() else when (request.memoryScope) {
                                                "global_and_preset" -> listOf("global", request.preset)
                                                "global" -> listOf("global")
                                                "preset" -> listOf(request.preset)
                                                else -> emptyList()
                                            }), client, executionTools, selected.maximumTokens, policy, catalog, cleanup))) }
                                    catch (_: Exception) { finish(PortResult.Failure(RunError.INVALID_REQUEST)) }
                                }
                                memoryLoading.set(memoryCall); if (stopped.get()) memoryCall.cancel()
                            }
                            if (!policy.isEnabled(checkNotNull(catalog[ToolNames.SCRIPT_CATALOG]))) compiled(null)
                            else {
                                val catalogCall = scriptTools.present(request.options.goal, false, true, minOf(5000, maxTimeout)) { result ->
                                    when (result) {
                                        is PortResult.Success -> compiled(result.value)
                                        is PortResult.Failure -> if (result.error.hostLost) finish(result)
                                            else compiled(ScriptPresentation.unavailable(result.error.name))
                                    }
                                }
                                catalogLoading.set(catalogCall); if (stopped.get()) catalogCall.cancel()
                            }
                        } catch (_: Exception) { finish(PortResult.Failure(RunError.INVALID_REQUEST)) }
                    }
                }
                selecting.set(handle); if (stopped.get()) handle.cancel()
            } catch (_: Exception) { finish(PortResult.Failure(RunError.HOST_UNAVAILABLE)) }
        } } catch (_: Exception) { finish(PortResult.Failure(RunError.HOST_UNAVAILABLE)) }
        }
        val foreground = ThreeStoveAgentTaskForegroundService.ensure(runtime.context) { promoted ->
            if (!promoted) { finish(PortResult.Failure(RunError.CAPABILITY_DENIED)); return@ensure }
            if (stopped.get()) return@ensure
            if (ToolGroup.MCP.id !in request.groups) prepared(null)
            else runtime.mcp.query { profiles ->
                if (!stopped.get()) profiles.fold({ configured ->
                    val handle = mcp.prepare(runtime.catalog, configured, RunLimits.MCP_PREPARATION_MS) { outcome ->
                        when (outcome) {
                            is PortResult.Success -> prepared(outcome.value)
                            is PortResult.Failure -> finish(outcome)
                        }
                    }
                    mcpLoading.set(handle); if (stopped.get()) handle.cancel()
                }, { finish(PortResult.Failure(RunError.TOOL_FAILED, mcpReason = "MCP_UNAVAILABLE")) })
            }
        }
        Cancellation { stopped.set(true); foreground.cancel(); selecting.get().cancel(); catalogLoading.get().cancel(); memoryLoading.get().cancel(); mcpLoading.get().cancel(); cleanup.cancel() }
    }
    @Synchronized private fun start(json: String, callback: IThreeStoveAgentRunCallback?, pluginUi: Boolean): Bundle {
        val configuration = config
        return synchronized(runtime.admissionLock) {
            check(!runtime.maintenance)
            RunLauncher.start(state, configuration, json, runtime.presets.snapshot(), runtime.settings.snapshot(), pluginUi) { request ->
                admit(request, configuration, callback)
            }
        }
    }
    private fun admit(request: StartRequest, configuration: LinkConfiguration, callback: IThreeStoveAgentRunCallback?): Bundle {
        val policy = runtime.policy(request.groups, runtime.settings.snapshot())
        val sink = RunSink(callback)
        val admittedId = AtomicReference<String>()
        val run = try { queue.submitPrepared(request.options, policy, prepare(request, policy, configuration) { checkNotNull(admittedId.get()) }, { admitted ->
            admittedId.set(admitted.id)
            archive.admit(admitted, request); active[admitted.id] = admitted; sinks[admitted.id] = sink
        }, ::onEvent) } catch (e: Exception) { sink.close(); throw e }
        runtime.taskChanged()
        notifyStatus()
        return AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, jsonObject("runId" to run.id.json(), "state" to run.state.wire.json()).toString())
    }
    private fun onEvent(event: RunEvent) {
        val run = active[event.runId]
        archive.record(event, run)
        sinks[event.runId]?.send(event)
        if (event.type == "done" && run?.state?.terminal == true && active.remove(event.runId, run)) {
            runCatching { runtime.alerts.runEnded(event.runId, event.payload) }
            val sink = sinks.remove(event.runId)
            runCatching { workers.callbacks.execute { sink?.close() } }
        }
        if (event.type == "state" || event.type in setOf("input", "confirmation", "step", "done", "error")) {
            notifyStatus(); runtime.taskChanged()
        }
        if (state != C.LINK_STATE_ATTACHED) scheduler.execute(::retireIfIdle)
    }
    private fun respond(json: String, interaction: String): Bundle = with(ControlRequests) {
        val value = AgentJson.objectOf(json, C.MAX_EVENT_JSON_BYTES)
        closed(value, setOf("runId", "requestId", "value", "allowed", "scope") + if (interaction == "plugin") setOf("remember") else emptySet())
        val id = runId(value)
        val run = active[id] ?: throw WireFailure(C.ERROR_RUN_NOT_FOUND)
        if (archive.interaction(id) != interaction) throw WireFailure(C.ERROR_RUN_NOT_INTERACTIVE)
        val pending = archive.pending(id) ?: throw WireFailure(C.ERROR_RUN_NOT_INTERACTIVE)
        val requestId = text(value, "requestId", maximum = 128) ?: throw WireFailure(C.ERROR_INVALID_REQUEST)
        if (pending.string("requestId") != requestId) throw WireFailure(C.ERROR_RUN_NOT_INTERACTIVE)
        if (pending.string("type") == "confirmation") {
            require(!value.has("value") && !value.has("remember"))
            val allowed = requireNotNull(value.flag("allowed"))
            val scope = text(value, "scope", "once", 8).also { require(it in setOf("once", "run")) }
            require(scope != "run" || pending.flag("allowRunScope") == true)
            if (!archive.claimPending(id, requestId)) throw WireFailure(C.ERROR_RUN_NOT_INTERACTIVE)
            run.confirm(requestId, allowed, if (scope == "run") ConfirmationScope.RUN else ConfirmationScope.ONCE)
        } else {
            require(!value.has("allowed") && !value.has("scope"))
            val answer = requireNotNull(value["value"])
            require(StepJournal.bytes(answer) <= 4096)
            when (pending.string("kind")) {
                "confirm" -> require(answer.isJsonPrimitive && answer.asJsonPrimitive.isBoolean)
                "choice" -> require(answer.isJsonPrimitive && answer.asJsonPrimitive.isString && pending.getAsJsonArray("choices").contains(answer))
                "text" -> require(answer.isJsonPrimitive && answer.asJsonPrimitive.isString && answer.asString.isNotBlank())
                "plan" -> require(answer.isJsonArray && answer.asJsonArray.size() in 1..AgentDecision.Plan.MAX_STEPS &&
                    answer.asJsonArray.all { it.isJsonPrimitive && it.asJsonPrimitive.isString && it.asString.isNotBlank() })
                else -> throw WireFailure(C.ERROR_RUN_NOT_INTERACTIVE)
            }
            val remember = if (value.has("remember")) requireNotNull(value.flag("remember")) else false
            val scope = if (remember) {
                requireNotNull(pending.string("memoryKey"))
                requireNotNull(archive.pendingForUi(id)?.string("rememberScope"))
            } else null
            if (!archive.claimPending(id, requestId)) throw WireFailure(C.ERROR_RUN_NOT_INTERACTIVE)
            run.respond(requestId, answer, scope)
        }
        AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, jsonObject("runId" to id.json(), "accepted" to true.json()).toString())
    }
    private fun endpoint(hostValidatedRoots: Boolean = false, enforce: () -> Unit) = object : IThreeStoveAgentLink.Stub() {
        private fun check(bundle: Bundle? = null) { try { enforce() } catch (e: SecurityException) { AgentWire.closeDescriptors(bundle); throw e } }
        private fun read(bundle: Bundle?, key: String) = AgentWire.control(bundle, key)
        private fun result(body: () -> Bundle): Bundle = try { body() } catch (e: Exception) {
            AgentWire.error(when (e) { is WireFailure -> e.code; is RunAdmissionFailure -> e.error.name; else -> C.ERROR_INVALID_REQUEST })
        }
        override fun getStatus(): Bundle { check(); return status(presentation = !hostValidatedRoots) }
        override fun startRun(request: Bundle?, callback: IThreeStoveAgentRunCallback?): Bundle { check(request); return result {
            // Only the private endpoint (workbench, floating ball) starts plugin UI tasks.
            start(read(request, C.KEY_RUN_REQUEST_JSON), callback, pluginUi = !hostValidatedRoots)
        } }
        override fun respond(response: Bundle?): Bundle { check(response); return result {
            respond(read(response, C.KEY_RUN_RESPONSE_JSON), if (hostValidatedRoots) "script" else "plugin")
        } }
        override fun cancelRun(reference: Bundle?) {
            check(reference)
            val value = AgentJson.objectOf(read(reference, C.KEY_RUN_REF_JSON))
            ControlRequests.closed(value, setOf("runId", "reason")); ControlRequests.text(value, "reason")
            active[ControlRequests.runId(value)]?.cancel()
        }
        override fun listRuns(query: Bundle?): Bundle { check(query); return RunQueries(archive).list(query) }
        override fun getRun(reference: Bundle?): Bundle { check(reference); return RunQueries(archive).get(reference) }
        override fun listPresets(query: Bundle?): Bundle { check(query); return result {
            require(AgentJson.objectOf(read(query, C.KEY_RUN_REQUEST_JSON)).size() == 0)
            presetList()
        } }
        override fun updateConfig(configuration: Bundle?) {
            check(configuration)
            val next = LinkConfiguration.parse(read(configuration, C.KEY_LINK_CONFIG_JSON))
            synchronized(this@HostLink) {
                require(state == C.LINK_STATE_ATTACHED && next.narrows(config, hostValidatedRoots))
                active.values.forEach { it.cancel() }; scripts.invalidate(); config = next
            }
        }
        override fun detach(reason: Bundle?) {
            check(reason)
            val value = AgentJson.objectOf(read(reason, H.KEY_REASON_JSON))
            ControlRequests.closed(value, setOf("reason", "code")); ControlRequests.text(value, "reason"); ControlRequests.text(value, "code")
            disconnect(C.LINK_STATE_DETACHED)
        }
    }
    private inner class RunSink(private val remote: IThreeStoveAgentRunCallback?) : AutoCloseable {
        private val dead = AtomicBoolean(remote == null)
        private val death = IBinder.DeathRecipient { close() }
        init { runCatching { remote?.asBinder()?.linkToDeath(death, 0) }.onFailure { dead.set(true) } }
        fun send(event: RunEvent) {
            if (dead.get()) return
            val json = event.payload.apply { addProperty("runId", event.runId); addProperty("sequence", event.sequence); addProperty("type", event.type) }.toString()
            if (json.utf8Size() > C.MAX_EVENT_JSON_BYTES) { close(); return }
            runCatching { workers.callbacks.execute {
                if (!dead.get()) runCatching { remote?.onRunEvent(AgentWire.envelope(C.KEY_RUN_EVENT_JSON, json)) }.onFailure { close() }
            } }.onFailure { close() }
        }
        override fun close() { if (!dead.getAndSet(true)) runCatching { remote?.asBinder()?.unlinkToDeath(death, 0) } }
    }
}
