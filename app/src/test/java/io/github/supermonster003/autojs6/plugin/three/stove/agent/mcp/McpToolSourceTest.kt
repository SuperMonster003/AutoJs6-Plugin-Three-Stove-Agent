package io.github.supermonster003.autojs6.plugin.three.stove.agent.mcp

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class McpToolSourceTest {
    @Test fun discoveryExplainsUnsupportedSchemasAndAcceptsNoncanonicalNames() {
        McpFixture().use { fixture ->
            fixture.tools = listOf(McpFixture.tool("getWeather"), McpFixture.tool("echo", jsonObject("type" to "object".json(), "oneOf" to jsonArray())))
            val discovery = awaitPort<McpDiscovery> { fixture.source().probe(fixture.profile(), 3000, it) }.success()
            assertTrue(discovery.tools[0].supported)
            assertFalse(discovery.tools[1].supported); assertEquals("MCP_UNSUPPORTED_SCHEMA", discovery.tools[1].reason)
            assertTrue(fixture.calls.isEmpty())
        }
    }
    @Test fun selectionIsExplicitAndLocalRiskOverridesServerAnnotations() {
        McpFixture().use { fixture ->
            fixture.tools = listOf(McpFixture.tool("echo").apply { add("annotations", jsonObject("readOnlyHint" to true.json())) }, McpFixture.tool("unselected"))
            val snapshot = snapshot(fixture)
            try {
                val spec = snapshot.catalog["mcp_local_echo"]!!
                assertEquals(RiskLevel.SENSITIVE, spec.risk); assertFalse(spec.defaultEnabled)
                assertFalse(spec.readOnlyHint); assertEquals(ToolGroup.MCP, spec.group)
                assertNull(snapshot.catalog["mcp_local_unselected"])
                assertFalse(ToolPolicy().isEnabled(spec))
                assertEquals(1, snapshot.catalog.tools.count { it.external != null })
            } finally { snapshot.close.cancel(); snapshot.close.cancel() }
        }
    }
    @Test fun taskRequiredOrMalformedExecutionToolsAreUnavailableButOptionalAndForbiddenWorkNormally() {
        McpFixture().use { fixture ->
            fixture.tools = listOf("required", "optional", "forbidden", "invalid").map { support ->
                McpFixture.tool("tool_$support").apply { add("execution", jsonObject("taskSupport" to support.json())) }
            }
            val tools = awaitPort<McpDiscovery> { fixture.source().probe(fixture.profile(), 3000, it) }.success().tools
            assertEquals("MCP_TASKS_REQUIRED", tools[0].reason)
            assertTrue(tools[1].supported); assertTrue(tools[2].supported)
            assertEquals("MCP_UNSUPPORTED_EXECUTION", tools[3].reason)
        }
    }
    @Test fun disabledServersNeverConnectAndSelectedLimitRejectsBeforeConnecting() {
        McpFixture().use { fixture ->
            val noTools = awaitPort<McpSnapshot> { fixture.source().prepare(F.catalog(), listOf(fixture.profile().copy(enabled = false)), 3000, it) }.success()
            noTools.close.cancel(); assertTrue(fixture.methods.isEmpty())
            val profiles = listOf(fixture.profile((1..32).map { "tool_$it" }), fixture.profile().copy(id = "other"))
            val failure = awaitPort<McpSnapshot> { fixture.source().prepare(F.catalog(), profiles, 3000, it) } as PortResult.Failure
            assertEquals("MCP_LIMIT_EXCEEDED", failure.mcpReason); assertTrue(fixture.methods.isEmpty())
        }
    }
    @Test fun originalCaseAndArgumentsReachCallButBinaryAndCredentialOutputAreNotForwarded() {
        McpFixture().use { fixture ->
            fixture.tools = listOf(McpFixture.tool("get.weather"))
            fixture.onRequest = { exchange, request ->
                if (request.string("method") == "tools/call") {
                    fixture.result(exchange, request, jsonObject("content" to jsonArray(
                        jsonObject("type" to "text".json(), "text" to "answer fixture-secret".json()),
                        jsonObject("type" to "image".json(), "data" to "private-image-base64".json(), "mimeType" to "image/png".json())),
                        "structuredContent" to jsonObject("value" to "fixture-secret".json()), "isError" to true.json())); true
                } else false
            }
            val snapshot = snapshot(fixture, fixture.profile(listOf("get.weather"), "fixture-secret"))
            try {
                val reply = execute(snapshot, "get.weather", jsonObject("query" to "sample".json()))
                assertEquals(RunError.TOOL_FAILED, reply.error); assertTrue(reply.images.isEmpty())
                val result = reply.result.toString()
                assertTrue(result.contains("credential redacted")); assertFalse(result.contains("fixture-secret")); assertFalse(result.contains("private-image-base64"))
                assertTrue(reply.result.asJsonObject.flag("untrusted")!!)
                val params = fixture.calls.single().getAsJsonObject("params")
                assertEquals("get.weather", params.string("name")); assertEquals("sample", params.getAsJsonObject("arguments").string("query"))
                assertEquals(2, fixture.methods.count { it == "tools/list" })
            } finally { snapshot.close.cancel() }
        }
    }
    @Test fun changedDefinitionWhileWaitingForConfirmationPreventsActionAndNeverRebinds() {
        McpFixture().use { fixture ->
            val snapshot = snapshot(fixture)
            try {
                val adapter = snapshot.wrap(NoDelegate)
                val prepared = prepare(adapter, "echo")
                fixture.tools = listOf(McpFixture.tool("echo").apply { addProperty("description", "changed") })
                val reply = awaitPort<ToolReply> { adapter.execute(prepared, 3000, it) }.success()
                assertEquals(RunError.TOOL_FAILED, reply.error); assertEquals("MCP_CATALOG_CHANGED", reply.result.asJsonObject.string("reason"))
                assertTrue(fixture.calls.isEmpty()); assertEquals(1, fixture.methods.count { it == "initialize" })
                assertTrue(awaitPort<PreparedTool> { adapter.prepare(invocation("echo"), 3000, it) } is PortResult.Failure)
            } finally { snapshot.close.cancel() }
        }
    }
    @Test fun listChangedNotificationInvalidatesSessionAndDoesNotReplayCompletedAction() {
        McpFixture().use { fixture ->
            fixture.onRequest = { exchange, request ->
                if (request.string("method") == "tools/call") {
                    fixture.result(exchange, request, jsonObject("content" to jsonArray()), true,
                        "data: {\"jsonrpc\":\"2.0\",\"method\":\"notifications/tools/list_changed\"}\n\n"); true
                } else false
            }
            val snapshot = snapshot(fixture)
            try {
                assertNull(execute(snapshot, "echo").error)
                val failure = awaitPort<PreparedTool> { snapshot.wrap(NoDelegate).prepare(invocation("echo"), 3000, it) } as PortResult.Failure
                assertEquals("MCP_CATALOG_CHANGED", failure.mcpReason); assertEquals(1, fixture.calls.size)
            } finally { snapshot.close.cancel() }
        }
    }
    @Test fun connectionLossNeverReplaysAnActionOrClaimsHostDeath() {
        McpFixture().use { fixture ->
            fixture.onRequest = { exchange, request -> if (request.string("method") == "tools/call") { exchange.close(); true } else false }
            val snapshot = snapshot(fixture)
            try {
                val reply = execute(snapshot, "echo")
                assertEquals(RunError.TOOL_FAILED, reply.error); assertEquals(1, fixture.calls.size)
                assertEquals(1, fixture.methods.count { it == "initialize" })
            } finally { snapshot.close.cancel() }
        }
    }
    @Test fun cancellingActiveCallSendsBestEffortNotificationAndDoesNotDeliverLateReply() {
        McpFixture().use { fixture ->
            val entered = CountDownLatch(1); val release = CountDownLatch(1); val notified = CountDownLatch(1); val callback = CountDownLatch(1)
            fixture.onRequest = { exchange, request -> when (request.string("method")) {
                "tools/call" -> { entered.countDown(); release.await(3, TimeUnit.SECONDS); fixture.result(exchange, request, jsonObject("content" to jsonArray())); true }
                "notifications/cancelled" -> { notified.countDown(); false }
                else -> false
            } }
            val snapshot = snapshot(fixture)
            try {
                val adapter = snapshot.wrap(NoDelegate)
                val handle = adapter.execute(prepare(adapter, "echo"), 2000) { callback.countDown() }
                assertTrue(entered.await(2, TimeUnit.SECONDS)); handle.cancel()
                assertTrue(notified.await(2, TimeUnit.SECONDS))
                release.countDown(); assertFalse(callback.await(300, TimeUnit.MILLISECONDS)); assertEquals(1, fixture.calls.size)
            } finally { release.countDown(); snapshot.close.cancel() }
        }
    }
    @Test fun escapedLargeResultsStayBoundedAndStructuredDataIsAnObservation() {
        McpFixture().use { fixture ->
            fixture.onRequest = { exchange, request -> if (request.string("method") == "tools/call") {
                fixture.result(exchange, request, jsonObject("content" to jsonArray(jsonObject("type" to "text".json(), "text" to "\n".repeat(40000).json())),
                    "structuredContent" to jsonObject("large" to "x".repeat(20000).json()))); true
            } else false }
            val snapshot = snapshot(fixture)
            try {
                val reply = execute(snapshot, "echo")
                assertTrue(reply.result.asJsonObject.flag("truncated")!!)
                assertTrue(reply.result.toString().toByteArray().size < 40 * 1024)
                assertTrue(reply.result.asJsonObject.has("structuredContent"))
            } finally { snapshot.close.cancel() }
        }
    }
    private fun snapshot(fixture: McpFixture, profile: McpServerProfile = fixture.profile()) =
        awaitPort<McpSnapshot> { fixture.source().prepare(F.catalog(), listOf(profile), 3000, it) }.success()
    private fun invocation(name: String, args: JsonObject = jsonObject()) =
        ToolInvocation(McpToolSource.qualified("local", name), args, ToolPlan.External("local", name, args))
    private fun prepare(adapter: RunTools, name: String, args: JsonObject = jsonObject()) =
        awaitPort<PreparedTool> { adapter.prepare(invocation(name, args), 3000, it) }.success()
    private fun execute(snapshot: McpSnapshot, name: String, args: JsonObject = jsonObject()): ToolReply {
        val adapter = snapshot.wrap(NoDelegate)
        return awaitPort<ToolReply> { adapter.execute(prepare(adapter, name, args), 3000, it) }.success()
    }
}
