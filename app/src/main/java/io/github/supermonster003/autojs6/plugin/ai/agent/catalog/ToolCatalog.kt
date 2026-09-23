package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.util.Locale

enum class RiskLevel { READ_ONLY, NORMAL, SENSITIVE }
enum class ToolGroup(val id: String, val defaultEnabled: Boolean) {
    OBSERVE("observe", true), OCR("ocr", true), ACT("act", true), GESTURE("gesture", false),
    SCRIPT("script", true), FILES("files", false), SHELL("shell", false), MEMORY("memory", true), USER("user", true);
    companion object { fun fromId(id: String) = entries.single { it.id == id } }
}

class ToolSpec internal constructor(private val data: JsonObject) {
    val name = checkNotNull(data.string("name"))
    val group = ToolGroup.fromId(checkNotNull(data.string("group")))
    val risk = RiskLevel.valueOf(checkNotNull(data.string("risk")))
    val defaultEnabled = checkNotNull(data.flag("defaultEnabled"))
    val readOnlyHint = checkNotNull(data.flag("readOnlyHint"))
    val destructiveHint = checkNotNull(data.flag("destructiveHint"))
    val outputHint = checkNotNull(data.string("outputHint"))
    val bridgeMapping: List<String> = data.getAsJsonArray("bridgeMapping").map { it.asString }
    val inputSchema: JsonObject get() = data.getAsJsonObject("inputSchema").deepCopy()
    val validator = InputSchema(inputSchema)
    fun description(language: String) = data.getAsJsonObject("description").get(if (language.startsWith("zh")) "zh" else "en").asString
    fun snapshot(): JsonObject = data.deepCopy()
}

class ToolCatalog(json: String) {
    val tools: List<ToolSpec>
    private val byName: Map<String, ToolSpec>
    init {
        val root = AgentJson.objectOf(json, 256 * 1024)
        require(root.number("format") == 1L)
        tools = root.getAsJsonArray("tools").map { ToolSpec(it.asJsonObject) }
        require(tools.isNotEmpty() && tools.size <= 64)
        require(tools.all { it.name.matches(Regex("[a-z][a-z0-9_]{1,63}")) && it.defaultEnabled == it.group.defaultEnabled })
        require(tools.all { it.inputSchema["additionalProperties"] == false.json() && it.bridgeMapping.isNotEmpty() })
        require(tools.all { it.readOnlyHint == (it.risk == RiskLevel.READ_ONLY) })
        require(tools.all { it.description("en").isNotBlank() && it.description("zh").isNotBlank() })
        byName = tools.associateBy { it.name }
        require(byName.size == tools.size) { "Duplicate tool name" }
    }
    operator fun get(name: String): ToolSpec? = byName[name]

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
    private val keywords: Set<String> = emptySet(),
    private val paymentPackages: Set<String> = emptySet(),
) {
    private val enabled = enabledGroups.toMap()
    private val overrides = riskOverrides.toMap()
    fun isEnabled(spec: ToolSpec): Boolean = (enabled[spec.group] ?: spec.defaultEnabled) && (spec.group != ToolGroup.OCR || ocrAvailable)
    fun requireEnabled(catalog: ToolCatalog, name: String): ToolSpec {
        val spec = catalog[name] ?: throw ToolFailure("TOOL_UNKNOWN", "Choose a listed tool.")
        if (!isEnabled(spec)) throw ToolFailure("TOOL_DISABLED", "This tool group is disabled or unavailable.")
        return spec
    }
    fun risk(spec: ToolSpec, context: RiskContext = RiskContext()): RiskLevel {
        val base = if (spec.name == "script_run") context.registeredScriptRisk ?: spec.risk else spec.risk
        val text = (context.nodeText + "\n" + context.nodeDescription).lowercase(Locale.ROOT)
        val elevated = spec.group == ToolGroup.ACT && spec.risk != RiskLevel.READ_ONLY &&
            (context.packageName in paymentPackages || keywords.any { text.contains(it.lowercase(Locale.ROOT)) })
        return maxOf(base, overrides[spec.name] ?: base, if (elevated) RiskLevel.SENSITIVE else base)
    }
    companion object {
        fun readKeywords(json: String): Set<String> = AgentJson.objectOf(json).entrySet()
            .flatMap { it.value.asJsonArray.map(JsonElement::getAsString) }.onEach { require(it.isNotBlank()) }.toSet()
    }
}

class ToolFailure(val code: String, val hint: String) : IllegalArgumentException("$code: $hint")
