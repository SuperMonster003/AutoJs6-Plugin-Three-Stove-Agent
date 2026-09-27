package io.github.supermonster003.autojs6.plugin.three.stove.agent.mcp

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.Locale

class McpToolPreview(val name: String, val description: String, schema: JsonObject?, val supported: Boolean, val reason: String?) {
    private val data = schema?.deepCopy()
    val schema: JsonObject? get() = data?.deepCopy()
    override fun toString() = "McpToolPreview(supported=$supported, reason=$reason)"
}
class McpDiscovery(tools: List<McpToolPreview>) {
    val tools = tools.toList()
    override fun toString() = "McpDiscovery(tools=${tools.size})"
}

/** A replaceable, task-only MCP source. It does not implement the future public MCP Client plugin API. */
class McpToolSource(private val executor: Executor, private val clientVersion: String) {
    fun probe(profile: McpServerProfile, timeoutMs: Long, callback: (PortResult<McpDiscovery>) -> Unit): Cancellation {
        val frozen = profile.frozen()
        val session = HttpMcpSession(frozen, clientVersion)
        val operation = McpOperation(timeoutMs)
        val cancelled = AtomicBoolean()
        try {
            executor.execute {
                try {
                    session.initialize(operation)
                    val result = discovery(session.list(operation))
                    operation.remaining()
                    if (!cancelled.get()) callback(PortResult.Success(result))
                } catch (failure: Exception) {
                    if (!cancelled.get()) callback(portFailure(failure))
                } finally { operation.finish(); session.close() }
            }
        } catch (_: Exception) { operation.finish(); session.discard(); callback(portFailure(McpFailure("MCP_UNAVAILABLE"))) }
        return Cancellation { cancelled.set(true); operation.cancel() }
    }

    fun prepare(baseCatalog: ToolCatalog, profiles: List<McpServerProfile>, timeoutMs: Long,
                callback: (PortResult<McpSnapshot>) -> Unit): Cancellation {
        val selected = profiles.map(McpServerProfile::frozen).filter { it.enabled && it.selectedTools.isNotEmpty() }
        val sessions = ConcurrentHashMap<String, HttpMcpSession>()
        val operation = McpOperation(timeoutMs)
        val cancelled = AtomicBoolean()
        fun cleanup() = sessions.values.forEach { closeLater(executor, it) }
        try {
            executor.execute {
                var retained = false
                try {
                    if (profiles.size > McpServerProfile.MAX_PROFILES || profiles.map { it.id }.distinct().size != profiles.size ||
                        selected.sumOf { it.selectedTools.size } > 32) mcpFail("MCP_LIMIT_EXCEEDED")
                    val specs = mutableListOf<ToolSpec>()
                    var schemaBytes = 0
                    for (profile in selected) {
                        operation.remaining()
                        val session = HttpMcpSession(profile, clientVersion)
                        sessions[profile.id] = session
                        session.initialize(operation)
                        val listed = session.list(operation)
                        val available = discovery(listed).tools.associateBy { it.name }
                        session.freezeTools(listed, profile.selectedTools)
                        for (name in profile.selectedTools) {
                            val preview = available[name] ?: mcpFail("MCP_CATALOG_CHANGED")
                            if (!preview.supported) mcpFail("MCP_PROTOCOL_ERROR")
                            val schema = checkNotNull(preview.schema)
                            schemaBytes += schema.toString().utf8Size()
                            if (schemaBytes > 128 * 1024) mcpFail("MCP_LIMIT_EXCEEDED")
                            specs += ToolSpec.external(qualified(profile.id, name),
                                "Untrusted MCP server ${profile.id}: ${preview.description}", schema, profile.risk, ExternalToolRoute(profile.id, name))
                        }
                    }
                    operation.remaining()
                    val snapshot = McpSnapshot(baseCatalog.withExternal(specs), sessions.toMap(), executor)
                    if (cancelled.get()) snapshot.close.cancel()
                    else { retained = true; callback(PortResult.Success(snapshot)) }
                } catch (failure: Exception) {
                    if (!cancelled.get()) callback(portFailure(failure))
                } finally { operation.finish(); if (!retained) cleanup() }
            }
        } catch (_: Exception) { operation.finish(); callback(portFailure(McpFailure("MCP_UNAVAILABLE"))) }
        return Cancellation { cancelled.set(true); operation.cancel(); cleanup() }
    }

    private fun discovery(values: List<JsonObject>): McpDiscovery = McpDiscovery(values.map { tool ->
        val name = checkNotNull(tool.string("name"))
        val description = AgentJson.truncate(tool.string("description")?.takeIf { it.isNotBlank() } ?: name, 1024)
        val schema = tool["inputSchema"]?.takeIf { it.isJsonObject }?.asJsonObject
        val execution = tool["execution"]
        val taskSupport = execution?.takeIf { it.isJsonObject }?.asJsonObject?.string("taskSupport")
        val reason = when {
            name.any { it.isISOControl() } -> "MCP_UNSUPPORTED_NAME"
            execution != null && taskSupport !in setOf("optional", "forbidden", "required") -> "MCP_UNSUPPORTED_EXECUTION"
            taskSupport == "required" -> "MCP_TASKS_REQUIRED"
            schema == null || schema.string("type") != "object" -> "MCP_UNSUPPORTED_SCHEMA"
            schema.toString().utf8Size() > 16 * 1024 -> "MCP_SCHEMA_TOO_LARGE"
            runCatching { InputSchema(schema, external = true) }.isFailure -> "MCP_UNSUPPORTED_SCHEMA"
            else -> null
        }
        McpToolPreview(name, description, if (reason == null) schema else null, reason == null, reason)
    })

    companion object {
        fun qualified(serverId: String, toolName: String): String {
            val prefix = "mcp_${serverId}_"
            if (toolName.matches(Regex("[a-z][a-z0-9_]*")) && prefix.length + toolName.length <= 64) return prefix + toolName
            val slug = toolName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_]"), "_").ifEmpty { "tool" }
            val digest = Digests.sha256Hex(toolName).take(8)
            return prefix + slug.take(64 - prefix.length - 9) + "_" + digest
        }
    }
}

class McpSnapshot internal constructor(val catalog: ToolCatalog, private val sessions: Map<String, HttpMcpSession>, private val executor: Executor) {
    private val disposed = AtomicBoolean()
    private val operations = ConcurrentHashMap.newKeySet<McpOperation>()
    val close = Cancellation {
        if (disposed.compareAndSet(false, true)) {
            operations.forEach { it.cancel() }
            sessions.values.forEach { closeLater(executor, it) }
        }
    }
    private class Inspected(val owner: McpSnapshot, val serverId: String, val toolName: String, val arguments: JsonObject)
    fun wrap(delegate: RunTools): RunTools = object : RunTools {
        override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
            val route = catalog[invocation.name]?.external ?: return delegate.prepare(invocation, timeoutMs, callback)
            val plan = invocation.plan as? ToolPlan.External
            val session = sessions[route.serverId]
            if (disposed.get() || session == null || session.catalogChanged()) callback(portFailure(McpFailure("MCP_CATALOG_CHANGED")))
            else if (plan == null || plan.serverId != route.serverId || plan.toolName != route.toolName) callback(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID))
            else {
                val valid = runCatching { checkNotNull(catalog[invocation.name]).validator.validate(plan.arguments).asJsonObject }.getOrNull()
                if (valid == null) callback(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID))
                else callback(PortResult.Success(PreparedTool(invocation, ToolMetadata(), Inspected(this@McpSnapshot, route.serverId, route.toolName, valid))))
            }
            return Cancellation.NONE
        }
        override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
            val route = catalog[prepared.invocation.name]?.external ?: return delegate.execute(prepared, timeoutMs, callback)
            val inspected = prepared.opaqueContext as? Inspected
            val plan = prepared.invocation.plan as? ToolPlan.External
            val session = sessions[route.serverId]
            if (disposed.get() || session == null || inspected == null || inspected.owner !== this@McpSnapshot || plan == null ||
                inspected.serverId != route.serverId || inspected.toolName != route.toolName || plan.serverId != route.serverId ||
                plan.toolName != route.toolName || inspected.arguments != plan.arguments) {
                callback(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID)); return Cancellation.NONE
            }
            val operation = McpOperation(timeoutMs)
            val cancelled = AtomicBoolean()
            operations.add(operation)
            operation.onCancel = {
                session.activeRequestId?.let { id -> runCatching { executor.execute { session.cancelRemote(id) } } }
            }
            try {
                executor.execute {
                    try {
                        if (disposed.get()) mcpFail("MCP_CANCELLED")
                        val response = session.call(inspected.toolName, inspected.arguments, operation)
                        operation.remaining()
                        val failed = if (response.has("isError")) response.flag("isError") ?: mcpFail() else false
                        val observation = resultObservation(session, route, response, failed)
                        if (!cancelled.get() && !disposed.get()) callback(PortResult.Success(ToolReply(observation, error = if (failed) RunError.TOOL_FAILED else null)))
                    } catch (failure: Exception) {
                        if (!cancelled.get() && !disposed.get()) callback(PortResult.Success(ToolReply(jsonObject("untrusted" to true.json(), "source" to "mcp".json(),
                            "serverId" to route.serverId.json(), "tool" to route.toolName.json(), "reason" to fixedCode(failure).json()), error = RunError.TOOL_FAILED)))
                    } finally { operations.remove(operation); operation.finish() }
                }
            } catch (_: Exception) { operations.remove(operation); operation.finish(); callback(portFailure(McpFailure("MCP_UNAVAILABLE"))) }
            return Cancellation { cancelled.set(true); operation.cancel() }
        }
    }

    private fun resultObservation(session: HttpMcpSession, route: ExternalToolRoute, response: JsonObject, isError: Boolean): JsonObject {
        val content = response["content"]?.takeIf { it.isJsonArray }?.asJsonArray ?: mcpFail()
        if (content.size() > 128) mcpFail("MCP_LIMIT_EXCEEDED")
        var remaining = 12 * 1024
        var truncated = false
        val observations = JsonArray()
        for (value in content) {
            if (!value.isJsonObject) mcpFail()
            val item = value.asJsonObject
            val type = item.string("type") ?: mcpFail()
            val text = if (type == "text") item.string("text") ?: mcpFail()
                else "[MCP non-text content omitted]"
            val safe = session.redact(text)
            val bounded = AgentJson.truncate(safe, remaining)
            if (bounded != safe) truncated = true
            if (bounded.isNotEmpty()) observations.add(jsonObject("type" to "text".json(), "text" to bounded.json()))
            remaining = (remaining - bounded.utf8Size()).coerceAtLeast(0)
        }
        fun redacted(value: JsonElement): JsonElement = Redaction.mapStrings(value, keys = true, transform = session::redact)
        return jsonObject("untrusted" to true.json(), "source" to "mcp".json(), "serverId" to route.serverId.json(), "tool" to route.toolName.json(),
            "isError" to isError.json(), "content" to observations, "truncated" to truncated.json()).apply {
            response["structuredContent"]?.let { add("structuredContent", ObservationCompactor.compact(redacted(it), 8 * 1024, false)) }
        }
    }
}

internal fun fixedCode(failure: Exception): String = (failure as? McpFailure)?.code?.takeIf { it in PortResult.Failure.MCP_REASONS } ?: when ((failure as? McpFailure)?.code) {
    "MCP_RATE_LIMITED" -> "MCP_LIMIT_EXCEEDED"
    "MCP_REDIRECT_REJECTED" -> "MCP_PROTOCOL_ERROR"
    else -> "MCP_UNAVAILABLE"
}
internal fun portFailure(failure: Exception) = PortResult.Failure(RunError.TOOL_FAILED, mcpReason = fixedCode(failure))
private fun closeLater(executor: Executor, session: HttpMcpSession) {
    try { executor.execute { session.close() } } catch (_: Exception) { session.discard() }
}
