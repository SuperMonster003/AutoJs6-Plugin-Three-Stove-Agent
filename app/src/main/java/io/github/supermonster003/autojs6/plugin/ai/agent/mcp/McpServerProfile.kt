package io.github.supermonster003.autojs6.plugin.ai.agent.mcp

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.RiskLevel
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.net.URI
import java.util.Locale

/** Decrypted runtime configuration. Never include credentials in diagnostics or public metadata. */
data class McpServerProfile(
    val id: String,
    val name: String,
    val endpoint: String,
    val enabled: Boolean = false,
    val risk: RiskLevel = RiskLevel.SENSITIVE,
    val selectedTools: List<String> = emptyList(),
    val bearerToken: String? = null,
) {
    init {
        require(id.matches(Regex("[a-z][a-z0-9_]{0,11}"))) { "Invalid MCP server identifier" }
        bounded(name, 80, false)
        McpEndpoints.validate(endpoint)
        require(selectedTools.size <= 32 && selectedTools.distinct().size == selectedTools.size) { "Invalid MCP tool selection" }
        selectedTools.forEach { bounded(it, 128, false) }
        bearerToken?.let {
            bounded(it, 4096, false)
            require(it.none { c -> c.isWhitespace() || c.isISOControl() }) { "Invalid MCP credential" }
        }
    }
    fun frozen() = copy(selectedTools = selectedTools.toList())
    override fun toString() = "McpServerProfile(id=$id, enabled=$enabled, selected=${selectedTools.size})"
    companion object { const val MAX_PROFILES = 8 }
}

object McpEndpoints {
    fun validate(endpoint: String): String {
        bounded(endpoint, 2048, false)
        require(endpoint.none { it.isWhitespace() || it.isISOControl() }) { "Invalid MCP endpoint" }
        val uri = runCatching { URI(endpoint) }.getOrNull() ?: throw IllegalArgumentException("Invalid MCP endpoint")
        require(uri.isAbsolute && !uri.isOpaque && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) { "Invalid MCP endpoint" }
        require(uri.port == -1 || uri.port in 1..65535) { "Invalid MCP endpoint" }
        val host = uri.host?.lowercase(Locale.ROOT) ?: throw IllegalArgumentException("Invalid MCP endpoint")
        val scheme = uri.scheme.lowercase(Locale.ROOT)
        require(scheme == "https" || scheme == "http" && loopback(host)) { "MCP requires HTTPS outside loopback" }
        require(uri.rawAuthority?.contains('%') == false && uri.rawPath.orEmpty().none { it == '\\' }) { "Invalid MCP endpoint" }
        val authority = if (host.contains(':') && !host.startsWith('[')) "[$host]" else host
        val port = if (uri.port == -1) "" else ":${uri.port}"
        return URI("$scheme://$authority$port${uri.rawPath.orEmpty().ifEmpty { "/" }}").toASCIIString()
    }
    fun isLoopback(endpoint: String) = loopback(URI(validate(endpoint)).host.lowercase(Locale.ROOT))
    private fun loopback(host: String) = host in setOf("127.0.0.1", "localhost", "[::1]", "::1")
}

/** Only includeSecret=true is suitable for encrypted private persistence; default output is metadata. */
object McpProfileCodec {
    private val keys = setOf("id", "name", "endpoint", "enabled", "risk", "selectedTools", "bearerToken")
    fun encode(profile: McpServerProfile, includeSecret: Boolean = false): JsonObject = jsonObject(
        "id" to profile.id.json(), "name" to profile.name.json(), "endpoint" to profile.endpoint.json(),
        "enabled" to profile.enabled.json(), "risk" to profile.risk.name.lowercase(Locale.ROOT).json(),
        "selectedTools" to JsonArray().apply { profile.selectedTools.forEach { add(it) } },
    ).apply {
        if (includeSecret) profile.bearerToken?.let { addProperty("bearerToken", it) }
        else addProperty("hasBearerToken", profile.bearerToken != null)
    }
    fun decode(value: JsonObject): McpServerProfile {
        require(value.keySet().all { it in keys }) { "Invalid MCP profile" }
        val selected = value.get("selectedTools")?.also { require(it.isJsonArray) }?.asJsonArray ?: JsonArray()
        require(selected.size() <= 32 && selected.all { it.isJsonPrimitive && it.asJsonPrimitive.isString }) { "Invalid MCP selection" }
        require(!value.has("bearerToken") || value["bearerToken"].isJsonNull || value.string("bearerToken") != null) { "Invalid MCP credential" }
        return McpServerProfile(
            requireNotNull(value.string("id")), requireNotNull(value.string("name")),
            McpEndpoints.validate(requireNotNull(value.string("endpoint"))),
            if (value.has("enabled")) requireNotNull(value.flag("enabled")) else false,
            if (value.has("risk")) requireNotNull(RiskLevel.entries.firstOrNull { it.name.lowercase(Locale.ROOT) == value.string("risk") }) { "Invalid MCP risk" } else RiskLevel.SENSITIVE,
            selected.map { it.asString }, value.string("bearerToken"),
        )
    }
}

internal fun bounded(value: String, bytes: Int, blank: Boolean = true) {
    AgentJson.checkUnicode(value)
    require(value.utf8Size() <= bytes && (blank || value.isNotBlank()) && '\u0000' !in value) { "MCP text exceeds limit" }
}
