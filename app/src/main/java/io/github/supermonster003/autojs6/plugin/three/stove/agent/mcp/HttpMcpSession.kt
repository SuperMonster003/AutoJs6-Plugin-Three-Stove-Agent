package io.github.supermonster003.autojs6.plugin.three.stove.agent.mcp

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.RunLimits
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.CodingErrorAction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Fixed classifications only: exception messages never contain URL, headers or remote error text. */
internal class McpFailure(val code: String) : IOException(code)
internal fun mcpFail(code: String = "MCP_PROTOCOL_ERROR"): Nothing = throw McpFailure(code)

internal class McpOperation(timeoutMs: Long) {
    private val deadline = System.nanoTime() + timeoutMs.coerceIn(1, RunLimits.TOOL_TIMEOUT_MS) * 1_000_000
    private val connections = ConcurrentHashMap.newKeySet<HttpURLConnection>()
    private val cancelled = AtomicBoolean()
    @Volatile private var expired = false
    @Volatile var onCancel: (() -> Unit)? = null
    private val timer = clock.schedule({ expired = true; cancel() }, timeoutMs.coerceIn(1, RunLimits.TOOL_TIMEOUT_MS), TimeUnit.MILLISECONDS)
    fun remaining(): Int {
        if (cancelled.get()) mcpFail(if (expired) "MCP_TIMEOUT" else "MCP_CANCELLED")
        val ms = (deadline - System.nanoTime()) / 1_000_000
        if (ms <= 0) mcpFail("MCP_TIMEOUT")
        return ms.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1)
    }
    fun attach(connection: HttpURLConnection) {
        connections.add(connection)
        try { remaining() } catch (failure: McpFailure) { connection.disconnect(); connections.remove(connection); throw failure }
    }
    fun detach(connection: HttpURLConnection) { connections.remove(connection) }
    fun cancel() {
        if (cancelled.compareAndSet(false, true)) {
            // Capture the active request before disconnect can clear it. Do not block the UI or deadline thread.
            runCatching { onCancel?.invoke() }
            connections.forEach { connection -> disconnects.execute { runCatching { connection.disconnect() } } }
        }
    }
    fun finish() { onCancel = null; timer.cancel(false) }
    companion object {
        private val clock = ScheduledThreadPoolExecutor(1) { task -> Thread(task, "agent-mcp-deadline").apply { isDaemon = true } }
            .apply { removeOnCancelPolicy = true }
        private val disconnects = Executors.newCachedThreadPool { task -> Thread(task, "agent-mcp-disconnect").apply { isDaemon = true } }
    }
}

/** One initialized, per-run Streamable HTTP session. There is deliberately no action retry. */
internal class HttpMcpSession(profile: McpServerProfile, private val clientVersion: String) {
    init { require(clientVersion.isNotBlank() && clientVersion.length <= 64) }
    private val endpoint = McpEndpoints.validate(profile.endpoint)
    private var token: String? = profile.bearerToken
    // A late response still needs redaction after session cleanup clears the outgoing credential.
    private val redactionSecret = profile.bearerToken
    private var frozenTools: Map<String, JsonObject>? = null
    private val sequence = AtomicLong()
    private val closed = AtomicBoolean()
    /** Set only once a selected definition was seen to differ (or the session is gone); the tools stay unavailable for this run. */
    private val changed = AtomicBoolean()
    /** A notifications/tools/list_changed arrived; the next listing re-verifies the frozen selection instead of failing (roadmap P13). */
    private val notified = AtomicBoolean()
    @Volatile private var sessionId: String? = null
    @Volatile private var version = VERSIONS.first()
    @Volatile private var initialized = false
    @Volatile var activeRequestId: Long? = null
        private set

    fun initialize(operation: McpOperation) {
        val result = request("initialize", jsonObject("protocolVersion" to version.json(), "capabilities" to jsonObject(),
            "clientInfo" to jsonObject("name" to "AutoJs6 3-Stove Agent".json(), "version" to clientVersion.json())), operation)
        val negotiated = result.string("protocolVersion") ?: mcpFail()
        if (negotiated !in VERSIONS || result.getAsJsonObject("capabilities")?.get("tools")?.isJsonObject != true) mcpFail()
        version = negotiated
        initialized = true
        notification("notifications/initialized", null, operation)
    }

    fun list(operation: McpOperation): List<JsonObject> {
        if (!initialized || changed.get()) mcpFail("MCP_CATALOG_CHANGED")
        notified.set(false)
        val values = mutableListOf<JsonObject>()
        val names = mutableSetOf<String>()
        val cursors = mutableSetOf<String>()
        var cursor: String? = null
        var bytes = 0
        repeat(MAX_PAGES) {
            val params = jsonObject().apply { cursor?.let { addProperty("cursor", it) } }
            val result = request("tools/list", params, operation)
            // A change announced while paging makes this listing untrustworthy; the caller sees the failure and never acts on it.
            if (changed.get() || notified.get()) mcpFail("MCP_CATALOG_CHANGED")
            bytes += result.toString().utf8Size()
            if (bytes > MAX_RESPONSE_BYTES) mcpFail("MCP_LIMIT_EXCEEDED")
            val tools = result["tools"]?.takeIf { it.isJsonArray }?.asJsonArray ?: mcpFail()
            for (item in tools) {
                if (!item.isJsonObject || values.size >= MAX_DISCOVERED_TOOLS) mcpFail("MCP_LIMIT_EXCEEDED")
                val tool = item.asJsonObject
                val name = tool.string("name") ?: mcpFail()
                if (name.isBlank() || name.utf8Size() > 128 || !names.add(name)) mcpFail()
                if (containsSecret(tool.toString())) mcpFail()
                values += tool.deepCopy()
            }
            cursor = result["nextCursor"]?.takeUnless { it.isJsonNull }?.let {
                if (!it.isJsonPrimitive || !it.asJsonPrimitive.isString) mcpFail()
                it.asString.also { value -> if (value.isBlank() || value.utf8Size() > 1024 || !cursors.add(value)) mcpFail() }
            }
            if (cursor == null) return values
        }
        mcpFail("MCP_LIMIT_EXCEEDED")
    }

    fun call(name: String, arguments: JsonObject, operation: McpOperation): JsonObject {
        if (!initialized || changed.get()) mcpFail("MCP_CATALOG_CHANGED")
        frozenTools?.let { frozen ->
            val current = list(operation).associateBy { it.string("name") }
            if (frozen.any { (key, definition) -> current[key] != definition }) {
                changed.set(true)
                mcpFail("MCP_CATALOG_CHANGED")
            }
        }
        return request("tools/call", jsonObject("name" to name.json(), "arguments" to arguments.deepCopy()), operation)
    }

    fun freezeTools(tools: List<JsonObject>, selected: List<String>) {
        check(frozenTools == null)
        val definitions = tools.associateBy { it.string("name") }
        frozenTools = selected.associateWith { definitions[it]?.deepCopy() ?: mcpFail("MCP_CATALOG_CHANGED") }
    }

    fun cancelRemote(requestId: Long) {
        if (closed.get() || !initialized) return
        val operation = McpOperation(1500)
        try { notification("notifications/cancelled", jsonObject("requestId" to requestId.json()), operation) }
        catch (_: Exception) { /* Cancellation is best effort, never retried and never claims rollback. */ }
        finally { operation.finish() }
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        val operation = McpOperation(1500)
        try { if (initialized && sessionId != null) exchange("DELETE", null, null, operation, terminating = true) }
        catch (_: Exception) { /* Remote session cleanup is bounded and best effort. */ }
        finally { token = null; sessionId = null; operation.finish() }
    }
    fun discard() { closed.set(true); token = null; sessionId = null }
    fun catalogChanged() = changed.get()
    fun changeAnnounced() = notified.get()

    fun redact(text: String): String = redactionSecret?.let { Redaction.replaceSecret(text, it, "[credential redacted]") } ?: text
    private fun containsSecret(text: String): Boolean = token?.let { Redaction.containsSecret(text, it) } == true

    private fun request(method: String, params: JsonObject, operation: McpOperation): JsonObject {
        if (closed.get()) mcpFail("MCP_UNAVAILABLE")
        val id = sequence.incrementAndGet()
        activeRequestId = id
        try {
            return exchange("POST", jsonObject("jsonrpc" to "2.0".json(), "id" to id.json(), "method" to method.json(), "params" to params), id, operation)
                ?: mcpFail()
        } finally { activeRequestId = null }
    }
    private fun notification(method: String, params: JsonObject?, operation: McpOperation) {
        exchange("POST", jsonObject("jsonrpc" to "2.0".json(), "method" to method.json()).apply { params?.let { add("params", it) } }, null, operation)
    }

    private fun exchange(method: String, message: JsonObject?, id: Long?, operation: McpOperation, terminating: Boolean = false): JsonObject? {
        if (closed.get() && !terminating) mcpFail("MCP_UNAVAILABLE")
        val bytes = message?.toString()?.toByteArray(Charsets.UTF_8)
        if ((bytes?.size ?: 0) > MAX_REQUEST_BYTES) mcpFail("MCP_LIMIT_EXCEEDED")
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.requestMethod = method
        connection.connectTimeout = operation.remaining()
        connection.readTimeout = operation.remaining()
        connection.setRequestProperty("Accept", "application/json, text/event-stream")
        connection.setRequestProperty("MCP-Protocol-Version", version)
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        sessionId?.let { connection.setRequestProperty("Mcp-Session-Id", it) }
        if (bytes != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setFixedLengthStreamingMode(bytes.size)
        }
        operation.attach(connection)
        try {
            if (bytes != null) connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            operation.remaining()
            if (status == 401 || status == 403) mcpFail("MCP_AUTH_REQUIRED")
            if (status == 404 && sessionId != null) mcpFail("MCP_CATALOG_CHANGED")
            if (status == 429) mcpFail("MCP_RATE_LIMITED")
            if (status in 300..399) mcpFail("MCP_REDIRECT_REJECTED")
            if (status !in 200..299) {
                // Only classify fixed JSON-RPC codes; never return remote error text or headers.
                connection.errorStream?.use { input ->
                    runCatching { parseText(readUtf8(input), null, errorOnly = true) }.exceptionOrNull()?.let {
                        if (it is McpFailure && it.code in setOf("MCP_PAIRING_REQUIRED", "MCP_PAIRING_DENIED")) throw it
                    }
                }
                mcpFail("MCP_UNAVAILABLE")
            }
            val returnedSession = connection.getHeaderField("Mcp-Session-Id")
            if (returnedSession != null) {
                if (returnedSession.length !in 1..256 || returnedSession.any { it.code !in 0x21..0x7e }) mcpFail()
                if (sessionId != null && sessionId != returnedSession) mcpFail()
                sessionId = returnedSession
            }
            if (terminating || id == null && status in setOf(200, 202, 204)) return null
            if (id == null || status != 200) mcpFail()
            val contentType = connection.contentType?.substringBefore(';')?.trim()?.lowercase() ?: mcpFail()
            return connection.inputStream.use { input ->
                when (contentType) {
                    "application/json" -> parseText(readUtf8(input), id) ?: mcpFail()
                    "text/event-stream" -> readSse(input, id, operation)
                    else -> mcpFail()
                }
            }
        } catch (failure: McpFailure) { throw failure }
        catch (_: SocketTimeoutException) { mcpFail("MCP_TIMEOUT") }
        catch (_: Exception) {
            operation.remaining()
            mcpFail("MCP_UNAVAILABLE")
        } finally {
            operation.detach(connection)
            runCatching { connection.disconnect() }
        }
    }

    private fun readSse(input: InputStream, id: Long, operation: McpOperation): JsonObject {
        reader(input).use { reader ->
            val data = StringBuilder()
            var events = 0
            while (true) {
                operation.remaining()
                val line = reader.readLine() ?: break
                if (line.isEmpty()) {
                    if (data.isNotEmpty()) {
                        if (++events > 256) mcpFail("MCP_LIMIT_EXCEEDED")
                        parseText(data.toString().removeSuffix("\n"), id)?.let { return it }
                        data.setLength(0)
                    }
                } else if (line.startsWith("data:")) data.append(line.substring(5).removePrefix(" ")).append('\n')
            }
            if (data.isNotEmpty()) parseText(data.toString().removeSuffix("\n"), id)?.let { return it }
        }
        mcpFail()
    }

    private fun parseText(text: String, id: Long?, errorOnly: Boolean = false): JsonObject? {
        val value = runCatching { AgentJson.objectOf(text, MAX_RESPONSE_BYTES, 65_536) }.getOrElse { mcpFail() }
        if (value.string("jsonrpc") != "2.0") mcpFail()
        if (value.has("method")) {
            if (value.has("id")) mcpFail() // Server-initiated requests/sampling are not supported.
            when (value.string("method") ?: mcpFail()) {
                "notifications/tools/list_changed" -> notified.set(true)
                "notifications/progress", "notifications/message" -> Unit
                else -> Unit // Unsolicited notifications never trigger actions.
            }
            return null
        }
        if (!errorOnly && runCatching { value.number("id") }.getOrNull() != id) mcpFail()
        value["error"]?.let { error ->
            val code = error.takeIf { it.isJsonObject }?.asJsonObject?.let { runCatching { it.number("code") }.getOrNull() }
            mcpFail(when (code) { -32001L -> "MCP_AUTH_REQUIRED"; -32002L -> "MCP_PAIRING_REQUIRED"; -32003L -> "MCP_PAIRING_DENIED";
                -32004L -> "MCP_RATE_LIMITED"; else -> "MCP_PROTOCOL_ERROR" })
        }
        if (errorOnly) return null
        return value["result"]?.takeIf { it.isJsonObject }?.asJsonObject ?: mcpFail()
    }

    private fun reader(input: InputStream) = InputStreamReader(LimitedInput(input), Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)).buffered()
    private fun readUtf8(input: InputStream) = reader(input).use { it.readText() }
    private class LimitedInput(input: InputStream) : FilterInputStream(input) {
        private var count = 0
        override fun read(): Int = `in`.read().also { if (it >= 0 && ++count > MAX_RESPONSE_BYTES) mcpFail("MCP_LIMIT_EXCEEDED") }
        override fun read(bytes: ByteArray, offset: Int, length: Int): Int = `in`.read(bytes, offset, minOf(length, MAX_RESPONSE_BYTES + 1 - count)).also {
            if (it > 0) count += it
            if (count > MAX_RESPONSE_BYTES) mcpFail("MCP_LIMIT_EXCEEDED")
        }
    }
    companion object {
        val VERSIONS = listOf("2025-11-25", "2025-06-18", "2025-03-26")
        const val MAX_REQUEST_BYTES = 256 * 1024
        const val MAX_RESPONSE_BYTES = 1024 * 1024
        const val MAX_DISCOVERED_TOOLS = 128
        const val MAX_PAGES = 8
    }
}
