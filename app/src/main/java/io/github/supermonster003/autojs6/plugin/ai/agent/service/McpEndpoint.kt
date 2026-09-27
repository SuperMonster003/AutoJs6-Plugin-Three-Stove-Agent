package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.os.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.mcp.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C

/** Same-UID settings endpoint. Tokens are write-only; discovery never calls a model. */
internal class McpEndpoint(private val runtime: AgentRuntime) : IAgentSettings.Stub(), AutoCloseable {
    private val lifecycle = Any()
    @Volatile private var closed = false
    private val pending = AtomicInteger()
    private val probes = ConcurrentHashMap<String, Cancellation>()
    private val worker = ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS, ArrayBlockingQueue(4),
        { work -> Thread(work, "ai-agent-mcp-probe").apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy())
        .apply { allowCoreThreadTimeOut(true) }
    private val source = McpToolSource(worker, runtime.info.versionName)
    override fun close() {
        val active = synchronized(lifecycle) {
            if (closed) return
            closed = true; probes.values.toList()
        }
        active.forEach { runCatching { it.cancel() } }
        // Drain cancelled queued work so its session/deadline finally blocks still run.
        // Repository writes belong to AgentRuntime and must finish independently of this UI endpoint.
        worker.shutdown()
    }
    override fun query(request: Bundle?, callback: IPresetStoreCallback?) {
        if (Binder.getCallingUid() != Process.myUid()) { AgentWire.closeDescriptors(request); throw SecurityException("Private MCP settings") }
        if (closed) { AgentWire.closeDescriptors(request); callback?.onResult(AgentWire.error(C.ERROR_LINK_DETACHED)); return }
        val body = runCatching { AgentJson.objectOf(AgentWire.inline(request, C.KEY_RUN_REQUEST_JSON, MAX_REQUEST_BYTES), MAX_REQUEST_BYTES) }
            .getOrElse { callback?.onResult(AgentWire.error(C.ERROR_INVALID_REQUEST)); return }
        if (callback == null) return
        if (pending.incrementAndGet() > 8) { pending.decrementAndGet(); callback.onResult(AgentWire.error(C.ERROR_LIMIT_EXCEEDED)); return }
        val finished = AtomicBoolean(); var maintenance = false
        val registered = AtomicReference<Pair<String, Cancellation>?>()
        val death = IBinder.DeathRecipient { registered.get()?.second?.cancel() }
        fun finish(result: Result<JsonObject>) {
            if (!finished.compareAndSet(false, true)) return
            registered.getAndSet(null)?.let { (id, cancellation) -> probes.remove(id, cancellation) }
            runCatching { callback.asBinder().unlinkToDeath(death, 0) }
            if (maintenance) runtime.endMaintenance()
            try {
                val response = result.getOrNull()?.toString()
                callback.onResult(if (response != null && response.toByteArray(Charsets.UTF_8).size <= MAX_RESPONSE_BYTES)
                    AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, response) else AgentWire.error(C.ERROR_INVALID_REQUEST))
            } catch (_: Exception) { /* A closed screen must not leak remote or credential data. */ }
            finally { pending.decrementAndGet() }
        }
        fun fields(vararg keys: String) { require(body.keySet() == setOf("operation", *keys)) }
        fun state(profiles: List<McpServerProfile>) = jsonObject("revision" to runtime.mcp.revision.json(),
            "profiles" to JsonArray().apply { profiles.forEach { add(McpProfileCodec.encode(it)) } },
            "busy" to (runtime.maintenance || runtime.current?.liveRuns()?.isNotEmpty() == true).json())
        try {
            when (body.string("operation")) {
                "get" -> { fields(); runtime.mcp.query { finish(it.map(::state)) } }
                "save" -> {
                    fields("revision", "profile", "create", "token", "clearToken")
                    val row = requireNotNull(body["profile"]?.takeIf { it.isJsonObject }?.asJsonObject).deepCopy()
                    require(row.keySet() == setOf("id", "name", "endpoint", "enabled", "risk", "selectedTools"))
                    val token = requireNotNull(body.string("token")); val clear = requireNotNull(body.flag("clearToken"))
                    require(!(clear && token.isNotEmpty()))
                    val create = requireNotNull(body.flag("create")); val expected = requireNotNull(body.number("revision"))
                    check(runtime.beginMaintenance()); maintenance = true
                    runtime.mcp.mutate(expected, { rows ->
                        val id = requireNotNull(row.string("id")); val previous = rows.find { it.id == id }
                        require(create == (previous == null))
                        val endpoint = McpEndpoints.validate(requireNotNull(row.string("endpoint")))
                        row.addProperty("endpoint", endpoint)
                        // Editing the destination must not forward the previous server credential or selection.
                        if (previous != null && previous.endpoint != endpoint) row.add("selectedTools", JsonArray())
                        val nextToken = token.takeIf { it.isNotEmpty() } ?: previous?.bearerToken?.takeIf { !clear && previous.endpoint == endpoint }
                        nextToken?.let { row.addProperty("bearerToken", it) }
                        val next = McpProfileCodec.decode(row)
                        rows.filter { it.id != id } + next
                    }) { finish(it.map { jsonObject("saved" to true.json()) }) }
                }
                "delete" -> {
                    fields("revision", "id"); val id = requireNotNull(body.string("id")); val expected = requireNotNull(body.number("revision"))
                    check(runtime.beginMaintenance()); maintenance = true
                    runtime.mcp.mutate(expected, { rows -> require(rows.any { it.id == id }); rows.filter { it.id != id } }) {
                        finish(it.map { jsonObject("deleted" to true.json()) })
                    }
                }
                "cancel" -> {
                    fields("probeId"); val id = requireNotNull(body.string("probeId"))
                    probes.remove(id)?.cancel(); finish(Result.success(JsonObject()))
                }
                "probe" -> {
                    fields("id", "probeId", "revision")
                    val id = requireNotNull(body.string("id")); val expected = requireNotNull(body.number("revision"))
                    val key = requireNotNull(body.string("probeId")).also { require(it.matches(Regex("[a-f0-9-]{36}"))) }
                    val cancellation = AtomicReference<Cancellation>(Cancellation.NONE)
                    val cancelled = AtomicBoolean()
                    val cancel = Cancellation { cancelled.set(true); cancellation.get().cancel(); finish(Result.failure(IllegalStateException("Discovery cancelled"))) }
                    synchronized(lifecycle) {
                        check(!closed); require(probes.size < 2); check(!probes.containsKey(key))
                        registered.set(key to cancel); probes[key] = cancel
                        callback.asBinder().linkToDeath(death, 0)
                        // A callback death can race with registration on another Binder thread.
                        if (finished.get()) runCatching { callback.asBinder().unlinkToDeath(death, 0) }
                    }
                    runtime.mcp.query { profiles ->
                        try {
                            if (!cancelled.get()) {
                                check(runtime.mcp.revision == expected)
                                val profile = profiles.getOrThrow().single { it.id == id }
                                val operation = source.probe(profile, PROBE_TIMEOUT_MS) { result ->
                                    when (result) {
                                        is PortResult.Success -> finish(runCatching {
                                            check(runtime.mcp.revision == expected)
                                            jsonObject("tools" to JsonArray().apply { result.value.tools.forEach { tool ->
                                                add(jsonObject("name" to tool.name.json(), "supported" to tool.supported.json(),
                                                    "description" to AgentJson.truncate(tool.description.filter { !it.isISOControl() || it == '\n' }, 256).json()).apply {
                                                    tool.reason?.takeIf { it in UNSUPPORTED_REASONS }?.let { addProperty("reason", it) }
                                                })
                                            } })
                                        })
                                        is PortResult.Failure -> finish(Result.success(jsonObject("probeFailed" to true.json(), "code" to (result.mcpReason ?: result.error.name).json())))
                                    }
                                }
                                cancellation.set(operation); if (cancelled.get()) operation.cancel()
                            }
                        } catch (failure: Exception) { finish(Result.failure(failure)) }
                    }
                }
                else -> error("Invalid MCP operation")
            }
        } catch (failure: Exception) { finish(Result.failure(failure)) }
    }
    companion object {
        const val ACTION = "io.github.supermonster003.autojs6.plugin.ai.agent.MCP_SETTINGS"
        const val MAX_REQUEST_BYTES = 24 * 1024
        const val MAX_RESPONSE_BYTES = 192 * 1024
        const val PROBE_TIMEOUT_MS = 15_000L
        val UNSUPPORTED_REASONS = setOf("MCP_UNSUPPORTED_NAME", "MCP_UNSUPPORTED_SCHEMA", "MCP_SCHEMA_TOO_LARGE",
            "MCP_TASKS_REQUIRED", "MCP_UNSUPPORTED_EXECUTION")
    }
}
