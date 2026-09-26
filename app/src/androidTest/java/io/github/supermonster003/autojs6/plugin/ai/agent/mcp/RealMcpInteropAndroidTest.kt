package io.github.supermonster003.autojs6.plugin.ai.agent.mcp

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Optional real MCP Server SDK interoperability. Never reads production credentials or runs a model. */
class RealMcpInteropAndroidTest {
    @Test fun installedLocalServerSupportsDiscoveryAndGuardedReadOnlyToolRoundTrip() {
        assumeTrue("Explicit disposable-emulator interoperability only",
            InstrumentationRegistry.getArguments().getString("p10RealMcp") == "true")
        val emulator = Build.HARDWARE in setOf("ranchu", "goldfish") &&
            (Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk", ignoreCase = true))
        assertTrue("Real MCP interoperability is restricted to a disposable Android emulator", emulator)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val input = File(context.filesDir, "p10-mcp-local-test.json")
        val config = try {
            assertTrue("Missing bounded private MCP test input", input.isFile && input.length() in 1..8192)
            runCatching { AgentJson.objectOf(input.readText(Charsets.UTF_8), 8192) }
                .getOrElse { throw AssertionError("Invalid private MCP test input") }
        } finally { if (input.exists()) check(input.delete()) { "Private MCP test input cleanup failed" } }
        assertTrue("Unexpected MCP fixture fields", config.keySet().all { it in setOf("endpoint", "bearerToken", "expectedTool", "expectedOutcome") })
        assertTrue("Only the non-mutating device_info fixture is allowed", config.string("expectedTool") == "device_info")
        val outcome = config.string("expectedOutcome") ?: "pairing_required"
        assertTrue("Invalid MCP fixture outcome", outcome in setOf("pairing_required", "tool_failed"))
        val profile = runCatching {
            val endpoint = requireNotNull(config.string("endpoint"))
            require(McpEndpoints.isLoopback(endpoint))
            McpServerProfile("interop", "Disposable emulator MCP", endpoint, enabled = true,
                selectedTools = listOf("device_info"), bearerToken = requireNotNull(config.string("bearerToken")))
        }.getOrElse { throw AssertionError("Invalid private MCP fixture configuration") }
        val workers = Executors.newCachedThreadPool { task -> Thread(task, "agent-mcp-interop").apply { isDaemon = true } }
        var snapshot: McpSnapshot? = null
        try {
            val source = McpToolSource(workers)
            val discovery = await<McpDiscovery> { source.probe(profile, 15_000, it) }
            assertTrue("Real SDK tools/list must expose multiple distinct tools", discovery.tools.size > 1 && discovery.tools.map { it.name }.distinct().size == discovery.tools.size)
            assertTrue("Real SDK device_info schema must be supported", discovery.tools.any { it.name == "device_info" && it.supported })
            val base = ToolCatalog(context.assets.open("catalog/tools.json").bufferedReader().use { it.readText() })
            val ready = await<McpSnapshot> { source.prepare(base, listOf(profile), 15_000, it) }.also { snapshot = it }
            val spec = ready.catalog[McpToolSource.qualified(profile.id, "device_info")]
            assertNotNull("Selected real SDK tool must enter the frozen catalogue", spec)
            assertTrue("External server risk must default to sensitive and disabled", spec!!.risk == RiskLevel.SENSITIVE && !spec.defaultEnabled)
            assertTrue("External route must preserve the selected original name", spec.external == ExternalToolRoute("interop", "device_info"))
            val adapter = ready.wrap(object : RunTools {
                override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation =
                    throw AssertionError("Unexpected host delegate")
                override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation =
                    throw AssertionError("Unexpected host delegate")
            })
            val args = jsonObject()
            val invocation = ToolInvocation(spec.name, args, ToolPlan.External("interop", "device_info", args))
            val prepared = await<PreparedTool> { adapter.prepare(invocation, 15_000, it) }
            val reply = await<ToolReply> { adapter.execute(prepared, 15_000, it) }
            assertTrue("The fixture must not claim real host execution success", reply.error == RunError.TOOL_FAILED)
            val result = reply.result.asJsonObject
            if (outcome == "pairing_required") {
                assertTrue("Unpaired server must return the fixed pairing classification; actual=${result.string("reason")}",
                    result.string("reason") == "MCP_PAIRING_REQUIRED")
            } else {
                assertTrue("Paired server must return an actual tool error, not a transport failure", result.flag("isError") == true && !result.has("reason"))
            }
            assertTrue("MCP observation must remain untrusted and contain no credential", result.flag("untrusted") == true && !result.toString().contains(profile.bearerToken!!))
            println("P10_MCP_INTEROP tools=${discovery.tools.size} supported=${discovery.tools.count { it.supported }} outcome=$outcome")
        } finally {
            snapshot?.close?.cancel()
            workers.shutdown()
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) workers.shutdownNow()
        }
    }

    private fun <T> await(start: ((PortResult<T>) -> Unit) -> Cancellation): T {
        val result = AtomicReference<PortResult<T>>()
        val done = CountDownLatch(1)
        val handle = start { result.set(it); done.countDown() }
        if (!done.await(20, TimeUnit.SECONDS)) { handle.cancel(); throw AssertionError("MCP interoperability callback timed out") }
        return when (val value = result.get()) {
            is PortResult.Success -> value.value
            is PortResult.Failure -> throw AssertionError("MCP interoperability failed: ${value.mcpReason ?: value.error.name}")
            else -> throw AssertionError("Missing MCP interoperability result")
        }
    }
}
