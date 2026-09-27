package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** One reviewable parameter. [literal] marks numbers, booleans, null and empty containers, shown apart from text. */
internal data class ArgumentRow(val name: String, val value: String, val literal: Boolean)

/**
 * Flattens tool arguments into readable rows for confirmation. Nothing is dropped, shortened or
 * reordered unless asked: users approve exactly what will run. Nested values use dotted names and
 * list indexes; anything deeper than [MAX_DEPTH] stays as JSON text.
 */
internal object ArgumentRows {
    const val MAX_DEPTH = 6

    fun rows(arguments: JsonElement?, sorted: Boolean = false): List<ArgumentRow> {
        val result = mutableListOf<ArgumentRow>()
        fun children(value: JsonObject) = value.entrySet().let { if (sorted) it.sortedBy { entry -> entry.key } else it.toList() }
        fun visit(name: String, value: JsonElement, depth: Int) {
            when {
                value.isJsonNull -> result += ArgumentRow(name, "null", true)
                value.isJsonPrimitive -> value.asJsonPrimitive.let { primitive ->
                    result += if (primitive.isString) ArgumentRow(name, primitive.asString, false) else ArgumentRow(name, primitive.toString(), true)
                }
                depth >= MAX_DEPTH -> result += ArgumentRow(name, value.toString(), true)
                value.isJsonObject -> {
                    val entries = children(value.asJsonObject)
                    if (entries.isEmpty()) result += ArgumentRow(name, "{}", true)
                    entries.forEach { (key, child) -> visit("$name.$key", child, depth + 1) }
                }
                value.isJsonArray -> {
                    val items = value.asJsonArray
                    if (items.isEmpty) result += ArgumentRow(name, "[]", true)
                    items.forEachIndexed { index, child -> visit("$name[$index]", child, depth + 1) }
                }
            }
        }
        when {
            arguments == null || arguments.isJsonNull -> Unit
            arguments.isJsonObject -> children(arguments.asJsonObject).forEach { (key, child) -> visit(key, child, 1) }
            else -> visit("value", arguments, 0)
        }
        return result
    }
}
