package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

/** Deliberately small schema dialect. Unknown schema keywords fail at catalog load. */
class InputSchema(private val schema: JsonObject) {
    init { checkSchema(schema, 0) }

    fun validate(value: JsonElement): JsonElement = validateNode(schema, value, "arguments", 0)

    private fun validateNode(spec: JsonObject, value: JsonElement, path: String, depth: Int): JsonElement {
        require(depth <= 24) { "Argument nesting exceeds limit" }
        val types = spec["type"].let { if (it.isJsonArray) it.asJsonArray.map(JsonElement::getAsString) else listOf(it.asString) }
        require(types.any { matches(it, value) }) { "$path: wrong type" }
        spec["enum"]?.let { require(it.asJsonArray.any { entry -> entry == value }) { "$path: unknown enum value" } }
        when {
            value.isJsonObject -> {
                val props = spec.getAsJsonObject("properties") ?: JsonObject()
                val extra = spec["additionalProperties"]
                val result = value.asJsonObject.deepCopy()
                spec.getAsJsonArray("required")?.forEach { key -> require(result.has(key.asString)) { "$path.${key.asString}: required" } }
                props.entrySet().forEach { (key, child) ->
                    if (!result.has(key) && child.asJsonObject.has("default")) result.add(key, child.asJsonObject["default"].deepCopy())
                }
                result.entrySet().toList().forEach { (key, item) ->
                    val child = props[key] ?: extra?.takeIf { it.isJsonObject }
                    require(child != null) { "$path: unknown property" }
                    result.add(key, validateNode(child.asJsonObject, item, "$path.$key", depth + 1))
                }
                return result
            }
            value.isJsonArray -> {
                val size = value.asJsonArray.size()
                require(size >= (spec.number("minItems") ?: 0) && size <= (spec.number("maxItems") ?: 1024)) { "$path: array length" }
                return JsonArray().apply { value.asJsonArray.forEach { add(validateNode(spec.getAsJsonObject("items"), it, "$path[]", depth + 1)) } }
            }
            value.isJsonPrimitive && value.asJsonPrimitive.isString -> {
                val text = value.asString
                AgentJson.checkUnicode(text)
                val length = text.codePointCount(0, text.length)
                require(length >= (spec.number("minLength") ?: 0) && length <= (spec.number("maxLength") ?: 65_536)) { "$path: string length" }
            }
            value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> {
                val number = value.asBigDecimal
                spec["minimum"]?.let { require(number >= it.asBigDecimal) { "$path: below minimum" } }
                spec["maximum"]?.let { require(number <= it.asBigDecimal) { "$path: above maximum" } }
            }
        }
        return value.deepCopy()
    }

    private fun checkSchema(spec: JsonObject, depth: Int) {
        require(depth <= 24 && spec.keySet().all { it in KEYWORDS }) { "Unsupported schema" }
        val type = spec["type"] ?: error("Schema type required")
        val types = if (type.isJsonArray) type.asJsonArray.map(JsonElement::getAsString) else listOf(type.asString)
        require(types.isNotEmpty() && types.all { it in TYPES }) { "Unsupported type" }
        if ("object" in types) {
            val extra = spec["additionalProperties"]
            require(extra != null && (extra.isJsonObject || extra == false.json())) { "Object must constrain extra keys" }
            val props = spec.getAsJsonObject("properties") ?: JsonObject()
            props.entrySet().forEach { checkSchema(it.value.asJsonObject, depth + 1) }
            if (extra.isJsonObject) checkSchema(extra.asJsonObject, depth + 1)
            spec.getAsJsonArray("required")?.forEach { require(props.has(it.asString)) { "Unknown required property" } }
        }
        if ("array" in types) checkSchema(spec.getAsJsonObject("items") ?: error("Array items required"), depth + 1)
        spec["default"]?.let { validateNode(spec, it, "default", depth) }
    }

    private fun matches(type: String, value: JsonElement): Boolean = when (type) {
        "null" -> value.isJsonNull
        "object" -> value.isJsonObject
        "array" -> value.isJsonArray
        "string" -> value.isJsonPrimitive && value.asJsonPrimitive.isString
        "boolean" -> value.isJsonPrimitive && value.asJsonPrimitive.isBoolean
        "number" -> value.isJsonPrimitive && value.asJsonPrimitive.isNumber
        "integer" -> value.isJsonPrimitive && value.asJsonPrimitive.isNumber && value.asBigDecimal.stripTrailingZeros().scale() <= 0
        else -> false
    }

    companion object {
        private val TYPES = setOf("object", "array", "string", "number", "integer", "boolean", "null")
        private val KEYWORDS = setOf("type", "properties", "required", "additionalProperties", "items", "enum", "default", "description", "minLength", "maxLength", "minItems", "maxItems", "minimum", "maximum")
    }
}
