package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*

/** A lossless signature projection of the catalog's parameter types, defaults and enums.
 * Numeric/string size limits remain enforced by InputSchema. Repeated selectors are defined once. */
object CompactToolDescriptions {
    fun render(catalog: ToolCatalog, policy: ToolPolicy): String {
        val tools = catalog.tools.filter(policy::isEnabled).sortedBy { it.name }
        val selectors = tools.mapNotNull { it.inputSchema.getAsJsonObject("properties")?.getAsJsonObject("selector") }
            .distinctBy { it.toString() }
        fun signature(schema: JsonObject, alias: Boolean = true): String {
            if (alias) selectors.indexOfFirst { it == schema }.takeIf { it >= 0 }?.let { return "selector$it" }
            schema.getAsJsonArray("enum")?.let { return it.joinToString("|") { value -> value.toString() } }
            return when (schema.string("type")) {
                "object" -> {
                    val required = schema.getAsJsonArray("required")?.map { it.asString }.orEmpty()
                    val props = schema.getAsJsonObject("properties")
                    if (props == null) "object" else props.entrySet().joinToString(",", "{", "}") { (key, value) ->
                        key + (if (key in required) "" else "?") + ":" + signature(value.asJsonObject) +
                            (value.asJsonObject["default"]?.let { "=$it" } ?: "")
                    }
                }
                "array" -> "[${signature(schema.getAsJsonObject("items"))}]"
                "integer" -> "int"
                "number" -> "number"
                "boolean" -> "bool"
                else -> "str"
            }
        }
        return buildString {
            append("?=optional; =value is the default; R=read-only,N=normal,S=sensitive. Runtime validates all limits.\n")
            selectors.forEachIndexed { index, selector -> append("selector$index=").append(signature(selector, false)).append('\n') }
            tools.forEach { tool ->
                append(tool.name).append(' ').append(when (policy.risk(tool)) {
                    RiskLevel.READ_ONLY -> "R"; RiskLevel.NORMAL -> "N"; RiskLevel.SENSITIVE -> "S"
                }).append(' ').append(signature(tool.inputSchema)).append('\n')
            }
        }.trimEnd()
    }
}
