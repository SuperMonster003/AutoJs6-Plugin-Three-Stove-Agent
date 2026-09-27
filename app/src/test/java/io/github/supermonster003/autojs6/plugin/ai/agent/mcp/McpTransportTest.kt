package io.github.supermonster003.autojs6.plugin.ai.agent.mcp

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class McpTransportTest {
    @Test fun initializeNegotiatesEachSupportedDateAndPropagatesSessionAndBearerHeaders() {
        for (protocol in HttpMcpSession.VERSIONS) McpFixture().use { fixture ->
            fixture.protocol = protocol
            val authenticated = AtomicInteger()
            fixture.onRequest = { exchange, _ ->
                if (exchange.requestHeaders.getFirst("Authorization") == "Bearer fixture-secret") authenticated.incrementAndGet()
                false
            }
            val discovery = awaitPort<McpDiscovery> { fixture.source().probe(fixture.profile(token = "fixture-secret"), 3000, it) }.success()
            assertEquals(listOf("echo"), discovery.tools.map { it.name })
            assertEquals(listOf("initialize", "notifications/initialized", "tools/list"), fixture.methods.take(3))
            assertEquals(McpFixture.CLIENT_VERSION, fixture.clientVersions.firstOrNull())
            assertEquals(protocol, fixture.versions[2]); assertEquals("fixture-session", fixture.sessions[2])
            assertEquals(3, authenticated.get())
        }
    }
    @Test fun boundedPostSseListAndPaginationWorkWithoutOpeningLegacyGetStream() {
        McpFixture().use { fixture ->
            fixture.onRequest = { exchange, request ->
                if (request.string("method") == "tools/list") {
                    val next = request.getAsJsonObject("params").has("cursor")
                    fixture.result(exchange, request, jsonObject("tools" to jsonArray(McpFixture.tool(if (next) "second" else "first"))).apply {
                        if (!next) addProperty("nextCursor", "next")
                    }, true, ": keepalive\n\ndata: {\"jsonrpc\":\"2.0\",\"method\":\"notifications/progress\"}\n\n")
                    true
                } else false
            }
            val tools = awaitPort<McpDiscovery> { fixture.source().probe(fixture.profile(), 3000, it) }.success().tools
            assertEquals(listOf("first", "second"), tools.map { it.name })
            assertEquals(2, fixture.methods.count { it == "tools/list" }); assertTrue(fixture.calls.isEmpty())
        }
    }
    @Test fun unknownProtocolAndServerInitiatedRequestsFailClosed() {
        McpFixture().use { fixture ->
            fixture.protocol = "2026-01-01"
            assertFailure(fixture, "MCP_PROTOCOL_ERROR")
            assertFalse(fixture.methods.contains("tools/list"))
        }
        McpFixture().use { fixture ->
            fixture.onRequest = { exchange, request ->
                if (request.string("method") == "tools/list") {
                    fixture.respond(exchange, 200, "data: {\"jsonrpc\":\"2.0\",\"id\":500,\"method\":\"sampling/createMessage\"}\n\n", "text/event-stream"); true
                } else false
            }
            assertFailure(fixture, "MCP_PROTOCOL_ERROR")
        }
    }
    @Test fun authPairingAndRemoteErrorsOnlyExposeFixedCodes() {
        for ((code, status, expected) in listOf(Triple(-32002L, 409, "MCP_PAIRING_REQUIRED"), Triple(-32003L, 200, "MCP_PAIRING_DENIED"),
            Triple(-32001L, 401, "MCP_AUTH_REQUIRED"), Triple(-32600L, 200, "MCP_PROTOCOL_ERROR"))) McpFixture().use { fixture ->
            fixture.onRequest = { exchange, request -> fixture.error(exchange, request, code, "remote-secret http://private/", status); true }
            val failed = assertFailure(fixture, expected)
            assertFalse(failed.toString().contains("remote-secret")); assertFalse(failed.toString().contains("http://"))
        }
    }
    @Test fun redirectsAreNotFollowedOrSentCredentials() {
        McpFixture().use { destination -> McpFixture().use { fixture ->
            fixture.onRequest = { exchange, _ ->
                exchange.responseHeaders.set("Location", destination.endpoint)
                exchange.sendResponseHeaders(307, -1); exchange.close(); true
            }
            assertFailure(fixture, "MCP_PROTOCOL_ERROR")
            assertTrue(destination.methods.isEmpty())
        } }
    }
    @Test fun duplicateToolsRepeatedCursorsAndTooManyItemsAreRejected() {
        McpFixture().use { fixture ->
            fixture.tools = listOf(McpFixture.tool("echo"), McpFixture.tool("echo"))
            assertFailure(fixture, "MCP_PROTOCOL_ERROR")
        }
        McpFixture().use { fixture ->
            fixture.tools = (0..128).map { McpFixture.tool("tool_$it") }
            assertFailure(fixture, "MCP_LIMIT_EXCEEDED")
        }
        McpFixture().use { fixture ->
            fixture.onRequest = { exchange, request ->
                if (request.string("method") == "tools/list") {
                    fixture.result(exchange, request, jsonObject("tools" to jsonArray(), "nextCursor" to "same".json())); true
                } else false
            }
            assertFailure(fixture, "MCP_PROTOCOL_ERROR")
            assertEquals(2, fixture.methods.count { it == "tools/list" })
        }
    }
    @Test fun oversizedResponseAndCredentialEchoInMetadataAreRejected() {
        McpFixture().use { fixture ->
            fixture.onRequest = { exchange, request ->
                if (request.string("method") == "tools/list") { fixture.respond(exchange, 200, " ".repeat(1024 * 1024 + 1)); true } else false
            }
            assertFailure(fixture, "MCP_LIMIT_EXCEEDED")
        }
        McpFixture().use { fixture ->
            fixture.tools = listOf(McpFixture.tool("echo").apply { addProperty("description", "fixture-secret") })
            val failed = awaitPort<McpDiscovery> { fixture.source().probe(fixture.profile(token = "fixture-secret"), 3000, it) } as PortResult.Failure
            assertEquals("MCP_PROTOCOL_ERROR", failed.mcpReason)
        }
    }
    @Test fun timeoutDoesNotStartAReplacementSession() {
        McpFixture().use { fixture ->
            val release = CountDownLatch(1)
            fixture.onRequest = { _, request -> if (request.string("method") == "initialize") { release.await(2, TimeUnit.SECONDS); true } else false }
            try {
                val failed = awaitPort<McpDiscovery> { fixture.source().probe(fixture.profile(), 150, it) } as PortResult.Failure
                assertEquals("MCP_TIMEOUT", failed.mcpReason)
                assertEquals(1, fixture.methods.count { it == "initialize" })
            } finally { release.countDown() }
        }
    }
    @Test fun cancellationReturnsPromptlyAndSuppressesLateProbeCallback() {
        McpFixture().use { fixture ->
            val entered = CountDownLatch(1); val release = CountDownLatch(1); val callback = CountDownLatch(1)
            fixture.onRequest = { _, request -> if (request.string("method") == "initialize") { entered.countDown(); release.await(2, TimeUnit.SECONDS); true } else false }
            val handle = fixture.source().probe(fixture.profile(), 1000) { callback.countDown() }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            val before = System.nanoTime(); handle.cancel()
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before) < 250)
            release.countDown(); assertFalse(callback.await(300, TimeUnit.MILLISECONDS))
        }
    }
    private fun assertFailure(fixture: McpFixture, reason: String): PortResult.Failure {
        val result = awaitPort<McpDiscovery> { fixture.source().probe(fixture.profile(), 3000, it) }
        assertTrue(result is PortResult.Failure)
        return (result as PortResult.Failure).also { assertEquals(RunError.TOOL_FAILED, it.error); assertEquals(reason, it.mcpReason) }
    }
}
