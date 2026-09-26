package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*

/** A lossless signature projection of the catalog's parameter types, defaults and enums.
 * Numeric/string size limits remain enforced by InputSchema. Repeated selectors are defined once. */
object CompactToolDescriptions {
    fun render(catalog: ToolCatalog, policy: ToolPolicy): String {
        val tools = catalog.tools.filter(policy::isEnabled).sortedBy { it.name }
        val selectors = tools.filter { it.external == null }.mapNotNull { it.inputSchema.getAsJsonObject("properties")?.getAsJsonObject("selector") }
            .distinctBy { it.toString() }
        fun signature(schema: JsonObject, alias: Boolean = true, external: Boolean = false): String {
            if (alias && !external) selectors.indexOfFirst { it == schema }.takeIf { it >= 0 }?.let { return "selector$it" }
            schema.getAsJsonArray("enum")?.let { return it.joinToString("|") { value -> value.toString() } }
            schema["type"]?.takeIf { external && it.isJsonArray }?.asJsonArray?.let { types ->
                return types.joinToString("|") { type -> signature(schema.deepCopy().apply { add("type", type) }, false, true) }
            }
            if (!schema.has("type")) return "json"
            return when (schema.string("type")) {
                "object" -> {
                    val required = schema.getAsJsonArray("required")?.map { it.asString }.orEmpty()
                    val props = schema.getAsJsonObject("properties")
                    if (!external) return if (props == null) "object" else props.entrySet().joinToString(",", "{", "}") { (key, value) ->
                        key + (if (key in required) "" else "?") + ":" + signature(value.asJsonObject) +
                            (value.asJsonObject["default"]?.let { "=$it" } ?: "")
                    }
                    val fields = props?.entrySet()?.map { (key, value) ->
                        (if (key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) key else key.json().toString()) + (if (key in required) "" else "?") + ":" + signature(value.asJsonObject, external = true) +
                            (value.asJsonObject["default"]?.let { "=$it" } ?: "")
                    }.orEmpty()
                    val extra = schema["additionalProperties"]
                    val requiredExtras = required.filter { props?.has(it) != true }.map { key ->
                        (if (key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) key else key.json().toString()) + ":" +
                            (if (extra?.isJsonObject == true) signature(extra.asJsonObject, external = true) else "json")
                    }
                    val extraField = when { extra == false.json() -> emptyList(); extra?.isJsonObject == true -> listOf("*:" + signature(extra.asJsonObject, external = true)); else -> listOf("*:json") }
                    (fields + requiredExtras + extraField).joinToString(",", "{", "}")
                }
                "array" -> "[${signature(schema.getAsJsonObject("items"), external = external)}]"
                "integer" -> "int"
                "number" -> "number"
                "boolean" -> "bool"
                "null" -> "null"
                else -> "str"
            }
        }
        return buildString {
            append("?=optional; =value is the default; R=read-only,N=normal,S=sensitive. Runtime validates all limits.\n")
            selectors.forEachIndexed { index, selector -> append("selector$index=").append(signature(selector, false)).append('\n') }
            tools.forEach { tool ->
                append(tool.name).append(' ').append(when (policy.risk(tool)) {
                    RiskLevel.READ_ONLY -> "R"; RiskLevel.NORMAL -> "N"; RiskLevel.SENSITIVE -> "S"
                }).append(' ').append(signature(tool.inputSchema, external = tool.external != null))
                if (tool.external != null) append(" external-description=").append(AgentJson.truncate(tool.description("en"), 512).json())
                append('\n')
            }
        }.trimEnd()
    }
}
