package io.github.supermonster003.autojs6.plugin.three.stove.agent.mcp

import com.google.gson.JsonObject
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.junit.Assert.*
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Local protocol fixture only. It never connects to a configured user server. */
internal class McpFixture : AutoCloseable {
    val workers = Executors.newCachedThreadPool { task -> Thread(task, "mcp-fixture").apply { isDaemon = true } }
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val methods = CopyOnWriteArrayList<String>()
    val versions = CopyOnWriteArrayList<String>()
    val sessions = CopyOnWriteArrayList<String>()
    val clientVersions = CopyOnWriteArrayList<String>()
    val calls = CopyOnWriteArrayList<JsonObject>()
    var protocol = "2025-11-25"
    var tools = listOf(tool("echo"))
    var onRequest: ((HttpExchange, JsonObject) -> Boolean)? = null
    init {
        server.executor = workers
        server.createContext("/mcp") { exchange ->
            try {
                if (exchange.requestMethod == "DELETE") {
                    methods += "DELETE"; exchange.sendResponseHeaders(204, -1); exchange.close()
                } else {
                    val request = AgentJson.objectOf(exchange.requestBody.bufferedReader().use { it.readText() })
                    val method = request.string("method")!!
                    methods += method
                    versions += exchange.requestHeaders.getFirst("MCP-Protocol-Version").orEmpty()
                    sessions += exchange.requestHeaders.getFirst("Mcp-Session-Id").orEmpty()
                    if (method == "tools/call") calls += request.deepCopy()
                    if (method == "initialize") clientVersions += request.getAsJsonObject("params")?.getAsJsonObject("clientInfo")?.string("version").orEmpty()
                    if (onRequest?.invoke(exchange, request) != true) when (method) {
                        "initialize" -> {
                            exchange.responseHeaders.set("Mcp-Session-Id", "fixture-session")
                            result(exchange, request, jsonObject("protocolVersion" to protocol.json(), "capabilities" to jsonObject("tools" to jsonObject())))
                        }
                        "tools/list" -> result(exchange, request, jsonObject("tools" to jsonArray(*tools.toTypedArray())))
                        "tools/call" -> result(exchange, request, jsonObject("content" to jsonArray(jsonObject("type" to "text".json(), "text" to "ok".json()))))
                        else -> { exchange.sendResponseHeaders(202, -1); exchange.close() }
                    }
                }
            } catch (_: Exception) { exchange.close() }
        }
        server.start()
    }
    val endpoint get() = "http://127.0.0.1:${server.address.port}/mcp"
    fun profile(selected: List<String> = listOf("echo"), token: String? = null) =
        McpServerProfile("local", "Fixture", endpoint, true, selectedTools = selected, bearerToken = token)
    fun source() = McpToolSource(workers, CLIENT_VERSION)
    fun result(exchange: HttpExchange, request: JsonObject, result: JsonObject, sse: Boolean = false, before: String = "") {
        val body = jsonObject("jsonrpc" to "2.0".json(), "id" to request["id"], "result" to result).toString()
        respond(exchange, 200, if (sse) before + "event: message\ndata: $body\n\n" else body, if (sse) "text/event-stream" else "application/json")
    }
    fun error(exchange: HttpExchange, request: JsonObject, code: Long, message: String, status: Int = 200) =
        respond(exchange, status, jsonObject("jsonrpc" to "2.0".json(), "id" to request["id"], "error" to jsonObject("code" to code.json(), "message" to message.json())).toString())
    fun respond(exchange: HttpExchange, status: Int, body: String, contentType: String = "application/json") {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", contentType)
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }; exchange.close()
    }
    override fun close() { server.stop(0); workers.shutdownNow() }
    companion object {
        const val CLIENT_VERSION = "9.9.9-fixture"
        fun tool(name: String, schema: JsonObject = jsonObject("type" to "object".json(), "properties" to jsonObject())) =
            jsonObject("name" to name.json(), "description" to "Fixture tool".json(), "inputSchema" to schema)
    }
}

internal fun <T> awaitPort(start: ((PortResult<T>) -> Unit) -> Cancellation): PortResult<T> {
    val value = AtomicReference<PortResult<T>>()
    val done = CountDownLatch(1)
    val handle = start { value.set(it); done.countDown() }
    if (!done.await(8, TimeUnit.SECONDS)) { handle.cancel(); fail("MCP fixture callback did not complete") }
    return checkNotNull(value.get())
}
internal fun <T> PortResult<T>.success(): T {
    assertTrue("Expected MCP fixture success", this is PortResult.Success)
    return (this as PortResult.Success).value
}

internal object NoDelegate : RunTools {
    override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation = error("Unexpected delegate preparation")
    override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation = error("Unexpected delegate execution")
}
