package io.github.supermonster003.autojs6.plugin.three.stove.agent.mcp

import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.RiskLevel
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class McpProfileTest {
    @Test fun defaultProfileIsDisabledSensitiveAndMetadataDoesNotContainCredentials() {
        val profile = McpServerProfile("local", "Fixture", "http://localhost/mcp", bearerToken = "fixture-secret")
        assertFalse(profile.enabled); assertEquals(RiskLevel.SENSITIVE, profile.risk)
        assertTrue(profile.selectedTools.isEmpty())
        assertFalse(profile.toString().contains("fixture-secret"))
        val metadata = McpProfileCodec.encode(profile)
        assertFalse(metadata.has("bearerToken")); assertTrue(metadata.flag("hasBearerToken")!!)
        assertEquals(profile, McpProfileCodec.decode(McpProfileCodec.encode(profile, true)))
        assertNull(McpProfileCodec.decode(McpProfileCodec.encode(profile.copy(bearerToken = null), true)).bearerToken)
    }
    @Test fun onlyHttpsOrExplicitLoopbackHttpEndpointsAreAcceptedWithoutRewritingEscapedPaths() {
        assertEquals("https://example.com/a%2Fb/%E4%B8%AD", McpEndpoints.validate("HTTPS://Example.COM/a%2Fb/%E4%B8%AD"))
        for (endpoint in listOf("http://localhost/mcp", "http://127.0.0.1:1234/mcp", "http://[::1]/mcp")) assertTrue(McpEndpoints.isLoopback(endpoint))
        assertFalse(McpEndpoints.isLoopback("https://example.com/mcp"))
        for (endpoint in listOf("http://192.168.0.1/mcp", "http://127.1/mcp", "http://localhost.evil/mcp", "http://127.0.0.2/mcp", "ftp://localhost/mcp",
            "https://user:secret@example.com/mcp", "https://example.com/mcp?token=x", "https://example.com/mcp#x", "https://example.com:0/mcp", "https://example.com\\mcp")) {
            assertTrue(runCatching { McpEndpoints.validate(endpoint) }.isFailure)
        }
    }
    @Test fun privateCodecRejectsUnknownFieldsInvalidRiskAndCredentialControls() {
        val base = McpProfileCodec.encode(McpServerProfile("local", "Fixture", "http://localhost/mcp"), true)
        for ((key, value) in listOf("unknown" to true.json(), "enabled" to "true".json(), "risk" to "invalid".json(), "bearerToken" to "a\r\nb".json())) {
            val bad = base.deepCopy().apply { add(key, value) }
            assertTrue(runCatching { McpProfileCodec.decode(bad) }.isFailure)
        }
        assertTrue(runCatching { McpServerProfile("Upper", "Fixture", "http://localhost/mcp") }.isFailure)
        assertTrue(runCatching { McpServerProfile("local", "Fixture", "http://localhost/mcp", selectedTools = listOf("echo", "echo")) }.isFailure)
    }
    @Test fun noncanonicalToolNamesReceiveStableBoundedAliasesWithoutLosingIdentity() {
        assertEquals("mcp_local_echo", McpToolSource.qualified("local", "echo"))
        val names = listOf("getWeather", "get-weather", "get.weather", "GetWeather", "x".repeat(128))
        val aliases = names.map { McpToolSource.qualified("local", it) }
        assertEquals(names.size, aliases.distinct().size)
        aliases.forEach { assertTrue(it.matches(Regex("[a-z][a-z0-9_]{1,63}"))) }
        assertEquals(aliases, names.map { McpToolSource.qualified("local", it) })
    }
}
