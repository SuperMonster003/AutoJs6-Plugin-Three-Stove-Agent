package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.util.Locale
import java.security.MessageDigest

enum class RiskLevel { READ_ONLY, NORMAL, SENSITIVE }
enum class ToolGroup(val id: String, val defaultEnabled: Boolean) {
    OBSERVE("observe", true), OCR("ocr", true), ACT("act", true), GESTURE("gesture", false),
    SCRIPT("script", true), SCRIPT_DYNAMIC("script_dynamic", false), FILES("files", false), SHELL("shell", false), MEMORY("memory", true), USER("user", true), MCP("mcp", false);
    companion object { fun fromId(id: String) = entries.single { it.id == id } }
}

data class ExternalToolRoute(val serverId: String, val toolName: String) {
    init { require(serverId.isNotBlank() && toolName.isNotBlank()) }
}

class ToolSpec internal constructor(private val data: JsonObject, val external: ExternalToolRoute? = null) {
    val name = checkNotNull(data.string("name"))
    val group = ToolGroup.fromId(checkNotNull(data.string("group")))
    val risk = RiskLevel.valueOf(checkNotNull(data.string("risk")))
    val defaultEnabled = checkNotNull(data.flag("defaultEnabled"))
    val readOnlyHint = checkNotNull(data.flag("readOnlyHint"))
    val destructiveHint = checkNotNull(data.flag("destructiveHint"))
    /** Confirmed before every execution regardless of the default or cautious policy; only full access skips it. */
    val confirmAlways = data.flag("confirmAlways") ?: false
    val outputHint = checkNotNull(data.string("outputHint"))
    val bridgeMapping: List<String> = data.getAsJsonArray("bridgeMapping").map { it.asString }
    val inputSchema: JsonObject get() = data.getAsJsonObject("inputSchema").deepCopy()
    val validator = InputSchema(inputSchema, external = external != null)
    fun description(language: String) = data.getAsJsonObject("description").get(if (language.startsWith("zh")) "zh" else "en").asString
    fun snapshot(): JsonObject = data.deepCopy()
    companion object {
        /** Routing and risk come from a locally selected server, never tools/list annotations. */
        fun external(name: String, description: String, inputSchema: JsonObject, risk: RiskLevel, route: ExternalToolRoute): ToolSpec {
            require(inputSchema.string("type") == "object") { "External tool input must be an object schema" }
            require(description.isNotBlank() && description.toByteArray(Charsets.UTF_8).size <= 4096)
            return ToolSpec(jsonObject("name" to name.json(), "group" to ToolGroup.MCP.id.json(), "risk" to risk.name.json(),
                "defaultEnabled" to false.json(), "readOnlyHint" to (risk == RiskLevel.READ_ONLY).json(),
                "destructiveHint" to (risk != RiskLevel.READ_ONLY).json(), "outputHint" to "Untrusted external tool observation".json(),
                "bridgeMapping" to JsonArray(), "description" to jsonObject("en" to description.json(), "zh" to description.json()),
                "inputSchema" to inputSchema.deepCopy()), route)
        }
    }
}

class ToolCatalog private constructor(specifications: List<ToolSpec>) {
    constructor(json: String) : this(AgentJson.objectOf(json, 256 * 1024).let { root ->
        require(root.number("format") == 1L)
        root.getAsJsonArray("tools").map { ToolSpec(it.asJsonObject) }
    })
    val tools: List<ToolSpec> = specifications.toList()
    private val byName: Map<String, ToolSpec>
    init {
        require(tools.isNotEmpty() && tools.size <= 64)
        require(tools.count { it.external != null } <= 32)
        require(tools.all { it.name.matches(Regex("[a-z][a-z0-9_]{1,63}")) && it.defaultEnabled == it.group.defaultEnabled })
        require(tools.all { if (it.external == null) it.group != ToolGroup.MCP && it.inputSchema["additionalProperties"] == false.json() && it.bridgeMapping.isNotEmpty()
            else it.group == ToolGroup.MCP && it.name.startsWith("mcp_") && it.bridgeMapping.isEmpty() })
        require(tools.all { it.readOnlyHint == (it.risk == RiskLevel.READ_ONLY) })
        require(tools.all { !it.confirmAlways || (it.external == null && it.risk == RiskLevel.SENSITIVE) })
        require(tools.all { it.description("en").isNotBlank() && it.description("zh").isNotBlank() })
        byName = tools.associateBy { it.name }
        require(byName.size == tools.size) { "Duplicate tool name" }
    }
    operator fun get(name: String): ToolSpec? = byName[name]
    val fingerprint: String by lazy {
        val wire = JsonArray().apply { tools.sortedBy { it.name }.forEach { tool -> add(tool.snapshot().apply {
            tool.external?.let { add("external", jsonObject("serverId" to it.serverId.json(), "toolName" to it.toolName.json())) }
        }) } }.toString()
        MessageDigest.getInstance("SHA-256").digest(wire.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    fun withExternal(additions: List<ToolSpec>): ToolCatalog {
        require(additions.all { it.external != null })
        return ToolCatalog(tools + additions)
    }

    /** The host's native definitions are a projection of the same catalog used by local validation. */
    fun nativeDefinitions(policy: ToolPolicy, language: String): JsonArray = JsonArray().apply {
        tools.filter(policy::isEnabled).sortedBy { it.name }.forEach { spec ->
            add(jsonObject("name" to spec.name.json(), "description" to spec.description(language).json(), "inputSchema" to spec.inputSchema))
        }
    }

    fun render(policy: ToolPolicy, language: String = "en"): String = JsonArray().apply {
        tools.filter(policy::isEnabled).sortedWith(compareBy({ it.group.id }, { it.name })).forEach { spec ->
            add(jsonObject("name" to spec.name.json(), "group" to spec.group.id.json(),
                "risk" to policy.risk(spec).name.json(), "description" to spec.description(language).json(),
                "inputSchema" to spec.inputSchema, "outputHint" to spec.outputHint.json()))
        }
    }.toString()
}

/** This context is obtained from host observations/registration, never from model risk claims. */
data class RiskContext(val nodeText: String = "", val nodeDescription: String = "", val packageName: String = "", val registeredScriptRisk: RiskLevel? = null)

class ToolPolicy(
    enabledGroups: Map<ToolGroup, Boolean> = emptyMap(),
    val ocrAvailable: Boolean = false,
    riskOverrides: Map<String, RiskLevel> = emptyMap(),
    keywords: Set<String> = emptySet(),
    paymentPackages: Set<String> = emptySet(),
    paymentKeywords: Set<String> = emptySet(),
    availableTools: Set<String>? = null,
    orderKeywords: Set<String> = emptySet(),
    val visionAvailable: Boolean = false,
) {
    private val enabled = enabledGroups.toMap()
    private val overrides = riskOverrides.toMap()
    private val keywords = keywords.toSet()
    private val paymentPackages = paymentPackages.toSet()
    private val paymentKeywords = paymentKeywords.toSet()
    private val available = availableTools?.toSet()
    private val orderKeywords = orderKeywords.toSet()
    private val orderTerms = orderKeywords.map { GoalTerm(it.lowercase(Locale.ROOT)) }
    fun withAvailableTools(value: Set<String>) = ToolPolicy(enabled, ocrAvailable, overrides, keywords, paymentPackages, paymentKeywords,
        available?.intersect(value) ?: value, orderKeywords, visionAvailable)
    /** Only locally selected, validated external specifications may extend the host's built-in set. */
    fun withExternalTools(tools: List<ToolSpec>): ToolPolicy {
        require(tools.all { it.external != null && it.group == ToolGroup.MCP })
        return ToolPolicy(enabled, ocrAvailable, overrides, keywords, paymentPackages, paymentKeywords,
            available?.plus(tools.map { it.name }), orderKeywords, visionAvailable)
    }
    fun withOcrAvailability(value: Boolean) = ToolPolicy(enabled, value, overrides, keywords, paymentPackages, paymentKeywords, available, orderKeywords, visionAvailable)
    fun withVisionAvailability(value: Boolean) = ToolPolicy(enabled, ocrAvailable, overrides, keywords, paymentPackages, paymentKeywords, available, orderKeywords, value)
    fun isOrderGoal(goal: String): Boolean = goal.lowercase(Locale.ROOT).let { value -> orderTerms.any { it.matches(value) } }
    fun isPayment(context: RiskContext): Boolean {
        val text = (context.nodeText + "\n" + context.nodeDescription).lowercase(Locale.ROOT)
        return context.packageName in paymentPackages || paymentKeywords.any { text.contains(it.lowercase(Locale.ROOT)) }
    }
    fun isEnabled(spec: ToolSpec): Boolean = (available == null || spec.name in available) && (enabled[spec.group] ?: spec.defaultEnabled) &&
        (spec.group != ToolGroup.OCR || ocrAvailable) && (spec.name != ToolNames.SCREEN_CAPTURE || visionAvailable)
    fun requireEnabled(catalog: ToolCatalog, name: String): ToolSpec {
        val spec = catalog[name] ?: throw ToolFailure("TOOL_UNKNOWN", "Choose a listed tool.")
        if (!isEnabled(spec)) throw ToolFailure("TOOL_DISABLED", "This tool group is disabled or unavailable.")
        return spec
    }
    fun risk(spec: ToolSpec, context: RiskContext = RiskContext()): RiskLevel {
        val base = if (spec.name == ToolNames.SCRIPT_RUN) context.registeredScriptRisk ?: spec.risk else spec.risk
        val text = (context.nodeText + "\n" + context.nodeDescription).lowercase(Locale.ROOT)
        val elevated = spec.group == ToolGroup.ACT && spec.risk != RiskLevel.READ_ONLY &&
            (context.packageName in paymentPackages || keywords.any { text.contains(it.lowercase(Locale.ROOT)) })
        return maxOf(base, overrides[spec.name] ?: base, if (elevated) RiskLevel.SENSITIVE else base)
    }
    /** Literal word boundaries avoid compiling Unicode regexes on the synchronous Binder admission path. */
    private class GoalTerm(private val term: String) {
        init { require(term.isNotBlank()) }
        private val bounded = term.none { it in '\u3040'..'\u30ff' || it in '\u3400'..'\u9fff' || it in '\uac00'..'\ud7af' }
        fun matches(text: String): Boolean {
            var index = text.indexOf(term)
            while (index >= 0) {
                val end = index + term.length
                if (!bounded || ((index == 0 || !word(text.codePointBefore(index))) && (end == text.length || !word(text.codePointAt(end))))) return true
                index = text.indexOf(term, index + 1)
            }
            return false
        }
        private fun word(point: Int) = Character.isLetterOrDigit(point) || point == '_'.code
    }
    companion object {
        fun fromAssets(readAsset: (String) -> String, enabledGroups: Map<ToolGroup, Boolean> = emptyMap(),
                       ocrAvailable: Boolean = false, riskOverrides: Map<String, RiskLevel> = emptyMap(), paymentPackages: Set<String> = emptySet(), availableTools: Set<String>? = null) =
            ToolPolicy(enabledGroups, ocrAvailable, riskOverrides, readKeywords(readAsset("catalog/sensitive-keywords.json")),
                paymentPackages, readKeywords(readAsset("catalog/payment-keywords.json")), availableTools,
                readKeywords(readAsset("catalog/order-intent-keywords.json")))
        fun readKeywords(json: String): Set<String> = AgentJson.objectOf(json).entrySet()
            .flatMap { it.value.asJsonArray.map(JsonElement::getAsString) }.onEach { require(it.isNotBlank()) }.toSet()
    }
}

class ToolFailure(val code: String, val hint: String) : IllegalArgumentException("$code: $hint")
