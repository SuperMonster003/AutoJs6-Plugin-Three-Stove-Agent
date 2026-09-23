package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.os.*
import io.github.supermonster003.autojs6.plugin.ai.agent.AiAgentTaskForegroundService
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.*
import org.autojs.plugin.ai.agent.api.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.autojs.plugin.host.capability.api.IHostCapabilityBroker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Link-owned queue and broker adapters. Control methods validate and enqueue; all remote work is asynchronous. */
internal class HostLink(private val runtime: AgentRuntime, initialConfig: LinkConfiguration,
                        private val remoteModel: IAiAgentModelBroker, private val remoteTools: IHostCapabilityBroker,
                        private val callback: IAiAgentLinkCallback, private val ownerUid: Int) {
    private val scheduler = SerialRunScheduler()
    private val workers = LinkWorkers()
    private val scripts = ScriptCatalogClient(scheduler::nowMs)
    private val model = BinderModelBroker(runtime.context, remoteModel, ownerUid, workers)
    private val archive get() = runtime.archive
    private val attachedAt = System.currentTimeMillis()
    private val fallbacks = SchemaFallbacks(DecisionSchema(runtime.catalog))
    private val clients = linkedMapOf<String, ModelClient>() // Used only on IO workers under its monitor.
    private val active = ConcurrentHashMap<String, AgentRunner>()
    private val sinks = ConcurrentHashMap<String, RunSink>()
    private val retired = AtomicBoolean()
    @Volatile private var config = initialConfig
    @Volatile private var state = C.LINK_STATE_ATTACHED
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
    val binder: IAiAgentLink = endpoint(hostValidatedRoots = true) { runtime.verifier.enforceOwner(ownerUid) }
    val local: IAiAgentLink = endpoint { if (Binder.getCallingUid() != Process.myUid()) throw SecurityException("Private link") }

    fun activate() {
        try { watched.forEach { it.linkToDeath(death, 0) } } catch (_: Exception) { disconnect(C.LINK_STATE_HOST_UNAVAILABLE) }
        notifyStatus()
    }
    fun status(): Bundle {
        val runs = active.values.filter { !it.state.terminal }
        return AgentWire.envelope(C.KEY_STATUS_JSON, jsonObject("state" to state.json(), "attachedAt" to attachedAt.json(),
            "runningRunId" to (runs.firstOrNull { it.state != RunState.QUEUED }?.id?.json() ?: JsonNull.INSTANCE),
            "queuedCount" to runs.count { it.state == RunState.QUEUED }.coerceAtMost(RunLimits.QUEUED_RUNS).json(),
            "pluginVersion" to runtime.info.versionName.json(), "scriptRoots" to JsonArray().apply { config.roots.sorted().forEach(::add) }).toString())
    }
    fun liveRuns(): List<JsonObject> = active.values.filter { !it.state.terminal }.mapNotNull { archive.summary(it.id) }
    fun cancelLocal(id: String) { active[id]?.cancel() }
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
        workers.close(); scheduler.close()
        runtime.taskChanged()
    }
    private fun notifyStatus() { runCatching { workers.callbacks.execute { runCatching { callback.onStatus(status()) } } } }

    private fun prepare(request: StartRequest, policy: ToolPolicy, configuration: LinkConfiguration) = RunPreparation { complete ->
        val stopped = AtomicBoolean()
        val selecting = AtomicReference<Cancellation>(Cancellation.NONE)
        val catalogLoading = AtomicReference<Cancellation>(Cancellation.NONE)
        fun finish(value: PortResult<RunComponents>) { if (!stopped.get()) complete(value) }
        val foreground = AiAgentTaskForegroundService.ensure(runtime.context) { promoted ->
        if (!promoted) { finish(PortResult.Failure(RunError.CAPABILITY_DENIED)); return@ensure }
        try { workers.io.execute {
            if (stopped.get()) return@execute
            try {
                val grant = remoteTools.brokerInfo
                if (grant.hasFileDescriptors()) { AgentWire.closeDescriptors(grant); throw WireFailure(C.ERROR_INVALID_REQUEST) }
                require(!grant.hasFileDescriptors() && grant.getInt(H.KEY_CONTRACT_VERSION) == H.CONTRACT_VERSION)
                val methods = requireNotNull(grant.getStringArray(H.KEY_GRANT_METHODS)).toSet().also { require(it.size <= 256) }
                val permissions = requireNotNull(grant.getStringArray(H.KEY_GRANT_PERMISSIONS)).toSet().also { require(it.size <= 128) }
                val maxRequest = grant.getInt(H.KEY_GRANT_MAX_REQUEST_BYTES).also { require(it in 1..512 * 1024) }
                val maxTimeout = grant.getLong(H.KEY_GRANT_MAX_TIMEOUT_MS).also { require(it in 1..300_000) }
                val toolAdapter = BinderRunTools(remoteTools, ownerUid, workers, scheduler, runtime.catalog,
                    { state == C.LINK_STATE_ATTACHED }, methods.intersect(configuration.methods ?: methods),
                    permissions.intersect(configuration.permissions ?: permissions), maxRequest, maxTimeout)
                val catalogAllowed = "agent.listScripts" in methods && "agent" in permissions &&
                    configuration.methods?.contains("agent.listScripts") != false && configuration.permissions?.contains("agent") != false &&
                    policy.isEnabled(checkNotNull(runtime.catalog["script_catalog"]))
                val scriptTools = ScriptCatalogTools(scripts, request.scriptRoots, ScriptCatalogSource(toolAdapter::dispatch), toolAdapter, catalogAllowed)
                val scriptRunAllowed = catalogAllowed && listOf("agent.readManifest", "agent.execRegistered").all {
                    it in methods && configuration.methods?.contains(it) != false
                } && policy.isEnabled(checkNotNull(runtime.catalog["script_run"]))
                val registeredTools = RegisteredScriptTools(scripts, request.scriptRoots, ScriptCatalogSource(toolAdapter::dispatch),
                    scriptTools, DecisionValidator(runtime.catalog), scriptRunAllowed, scheduler::nowMs)
                if (stopped.get()) return@execute
                val handle = model.select(request.target) { outcome ->
                    when (outcome) {
                        is PortResult.Failure -> finish(outcome)
                        is PortResult.Success -> try {
                            val selected = outcome.value
                            val original = selected.target
                            val schema = fallbacks.select(original.schemaTarget, policy)
                            val target = if ((schema.responseSchemaJson?.toByteArray(Charsets.UTF_8)?.size ?: 0) <= selected.maximumSchemaBytes) original
                                else ModelTarget(original.providerId, original.targetId, original.locality, original.protocol, false,
                                    original.maximumContextBytes, original.maximumOutputBytes, original.supportsStreaming, original.supportsOutputLimit)
                            val key = listOf(target.providerId, target.targetId, target.locality, target.structuredJson, target.maximumContextBytes,
                                target.maximumOutputBytes, target.supportsOutputLimit, target.supportsStreaming, request.groups.sorted()).toString()
                            val client = synchronized(clients) {
                                clients.getOrPut(key) {
                                    if (clients.size >= 32) clients.remove(clients.keys.first())
                                    ModelClient(model, target, policy, fallbacks, scheduler) { false }
                                }
                            }
                            val format = client.initialFormat(request.options.format)
                            fun compiled(presentation: ScriptPresentation?) {
                                if (stopped.get()) return
                                try { finish(PortResult.Success(RunComponents(
                                    ContextCompiler(runtime.prompts, runtime.catalog, policy, target, format,
                                        ContextLimits(grantMaximumBytes = minOf(configuration.maxInput, selected.maximumInputBytes)), request.context,
                                        scripts = presentation), client, registeredTools, selected.maximumTokens))) }
                                catch (_: Exception) { finish(PortResult.Failure(RunError.INVALID_REQUEST)) }
                            }
                            if (!policy.isEnabled(checkNotNull(runtime.catalog["script_catalog"]))) compiled(null)
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
        Cancellation { stopped.set(true); foreground.cancel(); selecting.get().cancel(); catalogLoading.get().cancel() }
    }
    @Synchronized private fun start(json: String, callback: IAiAgentRunCallback?): Bundle {
        if (state != C.LINK_STATE_ATTACHED) throw WireFailure(if (state == C.LINK_STATE_DETACHED) C.ERROR_LINK_DETACHED else C.ERROR_HOST_UNAVAILABLE)
        val configuration = config
        val request = StartRequest.parse(json, configuration)
        val policy = runtime.policy(request.groups)
        val sink = RunSink(callback)
        val run = try { queue.submitPrepared(request.options, policy, prepare(request, policy, configuration), { admitted ->
            archive.admit(admitted, request); active[admitted.id] = admitted; sinks[admitted.id] = sink
        }, ::onEvent) } catch (e: Exception) { sink.close(); throw e }
        runtime.taskChanged()
        notifyStatus()
        return AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, jsonObject("runId" to run.id.json(), "state" to run.state.wire.json()).toString())
    }
    private fun onEvent(event: RunEvent) {
        archive.event(event)
        val run = active[event.runId]
        if (event.type in setOf("step", "done", "error")) run?.readJournal { archive.journal(event.runId, it) }
        sinks[event.runId]?.send(event)
        if (event.type == "done" && run?.state?.terminal == true) {
            active.remove(event.runId)
            val sink = sinks.remove(event.runId)
            runCatching { workers.callbacks.execute { sink?.close() } }
        }
        if (event.type == "state" || event.type in setOf("input", "confirmation", "step", "done", "error")) {
            notifyStatus(); runtime.taskChanged()
        }
        if (state != C.LINK_STATE_ATTACHED) scheduler.execute(::retireIfIdle)
    }
    private fun respond(json: String): Bundle = with(ControlRequests) {
        val value = AgentJson.objectOf(json, C.MAX_EVENT_JSON_BYTES)
        closed(value, setOf("runId", "requestId", "value", "allowed", "scope"))
        val id = runId(value)
        val run = active[id] ?: throw WireFailure(C.ERROR_RUN_NOT_FOUND)
        val pending = archive.pending(id) ?: throw WireFailure(C.ERROR_RUN_NOT_INTERACTIVE)
        val requestId = text(value, "requestId", maximum = 128) ?: throw WireFailure(C.ERROR_INVALID_REQUEST)
        if (pending.string("requestId") != requestId) throw WireFailure(C.ERROR_RUN_NOT_INTERACTIVE)
        if (pending.string("type") == "confirmation") {
            require(!value.has("value"))
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
                else -> throw WireFailure(C.ERROR_RUN_NOT_INTERACTIVE)
            }
            if (!archive.claimPending(id, requestId)) throw WireFailure(C.ERROR_RUN_NOT_INTERACTIVE)
            run.respond(requestId, answer)
        }
        AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, jsonObject("runId" to id.json(), "accepted" to true.json()).toString())
    }
    private fun endpoint(hostValidatedRoots: Boolean = false, enforce: () -> Unit) = object : IAiAgentLink.Stub() {
        private fun check(bundle: Bundle? = null) { try { enforce() } catch (e: SecurityException) { AgentWire.closeDescriptors(bundle); throw e } }
        private fun read(bundle: Bundle?, key: String) = AgentWire.control(bundle, key)
        private fun result(body: () -> Bundle): Bundle = try { body() } catch (e: Exception) {
            AgentWire.error(when (e) { is WireFailure -> e.code; is RunAdmissionFailure -> e.error.name; else -> C.ERROR_INVALID_REQUEST })
        }
        override fun getStatus(): Bundle { check(); return status() }
        override fun startRun(request: Bundle?, callback: IAiAgentRunCallback?): Bundle { check(request); return result { start(read(request, C.KEY_RUN_REQUEST_JSON), callback) } }
        override fun respond(response: Bundle?): Bundle { check(response); return result { respond(read(response, C.KEY_RUN_RESPONSE_JSON)) } }
        override fun cancelRun(reference: Bundle?) {
            check(reference)
            val value = AgentJson.objectOf(read(reference, C.KEY_RUN_REF_JSON))
            ControlRequests.closed(value, setOf("runId", "reason")); ControlRequests.text(value, "reason")
            active[ControlRequests.runId(value)]?.cancel()
        }
        override fun listRuns(query: Bundle?): Bundle { check(query); return result {
            val value = AgentJson.objectOf(read(query, C.KEY_RUN_REQUEST_JSON))
            ControlRequests.closed(value, setOf("limit", "offset"))
            val limit = ControlRequests.number(value, "limit", 20, 50).toInt()
            val offset = if (value.has("offset")) requireNotNull(value.number("offset")).also { require(it in 0..1000) }.toInt() else 0
            AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, archive.list(limit, offset).toString())
        } }
        override fun getRun(reference: Bundle?): Bundle { check(reference); return result {
            val value = AgentJson.objectOf(read(reference, C.KEY_RUN_REF_JSON))
            ControlRequests.closed(value, setOf("runId", "limit"))
            val id = ControlRequests.runId(value)
            val row = archive.get(id, ControlRequests.number(value, "limit", 50, 50).toInt()) ?: throw WireFailure(C.ERROR_RUN_NOT_FOUND)
            AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, row.toString())
        } }
        override fun listPresets(query: Bundle?): Bundle { check(query); return result {
            require(AgentJson.objectOf(read(query, C.KEY_RUN_REQUEST_JSON)).size() == 0)
            AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, jsonObject("presets" to jsonArray(jsonObject("id" to "default".json(), "toolGroups" to
                JsonArray().apply { config.groups.sorted().forEach { add(it) } }))).toString())
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
    private inner class RunSink(private val remote: IAiAgentRunCallback?) : AutoCloseable {
        private val dead = AtomicBoolean(remote == null)
        private val death = IBinder.DeathRecipient { close() }
        init { runCatching { remote?.asBinder()?.linkToDeath(death, 0) }.onFailure { dead.set(true) } }
        fun send(event: RunEvent) {
            if (dead.get()) return
            val json = event.payload.apply { addProperty("runId", event.runId); addProperty("sequence", event.sequence); addProperty("type", event.type) }.toString()
            if (json.toByteArray(Charsets.UTF_8).size > C.MAX_EVENT_JSON_BYTES) { close(); return }
            runCatching { workers.callbacks.execute {
                if (!dead.get()) runCatching { remote?.onRunEvent(AgentWire.envelope(C.KEY_RUN_EVENT_JSON, json)) }.onFailure { close() }
            } }.onFailure { close() }
        }
        override fun close() { if (!dead.getAndSet(true)) runCatching { remote?.asBinder()?.unlinkToDeath(death, 0) } }
    }
}
