package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

/** Deliberately small schema dialect. Unknown schema keywords fail at catalog load. */
class InputSchema(schema: JsonObject, private val external: Boolean = false) {
    private val schema = schema.deepCopy()
    init { checkSchema(schema, 0) }

    fun validate(value: JsonElement): JsonElement = validateNode(schema, value, "arguments", 0)

    private fun validateNode(spec: JsonObject, value: JsonElement, path: String, depth: Int): JsonElement {
        require(depth <= 24) { "Argument nesting exceeds limit" }
        // External empty/annotation-only schemas mean any JSON value. Built-ins remain typed.
        if (!spec.has("type")) return validateAny(value, depth)
        val types = spec["type"].let { if (it.isJsonArray) it.asJsonArray.map(JsonElement::getAsString) else listOf(it.asString) }
        require(types.any { matches(it, value) }) { "$path: wrong type" }
        spec["enum"]?.let { require(it.asJsonArray.any { entry -> entry == value }) { "$path: unknown enum value" } }
        when {
            value.isJsonObject -> {
                val props = spec.getAsJsonObject("properties") ?: JsonObject()
                val extra = spec["additionalProperties"]
                val result = value.asJsonObject.deepCopy()
                spec.getAsJsonArray("required")?.forEach { key -> require(result.has(key.asString)) { "$path.${key.asString}: required" } }
                if (!external) props.entrySet().forEach { (key, child) ->
                    if (!result.has(key) && child.asJsonObject.has("default")) result.add(key, child.asJsonObject["default"].deepCopy())
                }
                result.entrySet().toList().forEach { (key, item) ->
                    val child = props[key] ?: extra?.takeIf { it.isJsonObject }
                    if (child == null) {
                        require(external && (extra == null || extra == true.json())) { "$path: unknown property" }
                        result.add(key, validateAny(item, depth + 1))
                    } else result.add(key, validateNode(child.asJsonObject, item, "$path.$key", depth + 1))
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
        for (key in listOf("description", "title")) spec[key]?.let { require(it.isJsonPrimitive && it.asJsonPrimitive.isString) { "Invalid schema annotation" } }
        for (key in listOf("minLength", "maxLength", "minItems", "maxItems")) spec[key]?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber && it.asBigDecimal.stripTrailingZeros().scale() <= 0 && it.asBigDecimal.signum() >= 0 &&
                it.asBigDecimal <= Long.MAX_VALUE.toBigDecimal()) { "Invalid schema length" }
        }
        for (key in listOf("minimum", "maximum")) spec[key]?.let { require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber) { "Invalid schema bound" } }
        spec["enum"]?.let { values ->
            require(values.isJsonArray && !values.asJsonArray.isEmpty && values.asJsonArray.size() <= 1024) { "Invalid schema enum" }
            require(values.asJsonArray.toList().distinct().size == values.asJsonArray.size()) { "Duplicate schema enum value" }
        }
        val type = spec["type"] ?: run {
            require(external && spec.keySet().all { it in ANNOTATIONS }) { "Schema type required" }
            return
        }
        require((type.isJsonPrimitive && type.asJsonPrimitive.isString) || (type.isJsonArray && type.asJsonArray.all { it.isJsonPrimitive && it.asJsonPrimitive.isString })) { "Invalid schema type" }
        val types = if (type.isJsonArray) type.asJsonArray.map(JsonElement::getAsString) else listOf(type.asString)
        require(types.isNotEmpty() && types.distinct().size == types.size && types.all { it in TYPES }) { "Unsupported type" }
        spec["properties"]?.let { require(it.isJsonObject) { "Invalid schema properties" } }
        spec["required"]?.let { required ->
            require(required.isJsonArray && required.asJsonArray.all { it.isJsonPrimitive && it.asJsonPrimitive.isString }) { "Invalid required properties" }
            require(required.asJsonArray.map { it.asString }.distinct().size == required.asJsonArray.size()) { "Duplicate required property" }
        }
        // Child schemas are checked even if the current type makes the keyword inapplicable.
        spec.getAsJsonObject("properties")?.entrySet()?.forEach { require(it.value.isJsonObject); checkSchema(it.value.asJsonObject, depth + 1) }
        spec["items"]?.let { require(it.isJsonObject); checkSchema(it.asJsonObject, depth + 1) }
        spec["additionalProperties"]?.let { extra ->
            require(extra.isJsonObject || (extra.isJsonPrimitive && extra.asJsonPrimitive.isBoolean)) { "Invalid additionalProperties" }
            if (extra.isJsonObject) checkSchema(extra.asJsonObject, depth + 1)
        }
        if ("object" in types) {
            val extra = spec["additionalProperties"]
            require(external || (extra != null && (extra.isJsonObject || extra == false.json()))) { "Object must constrain extra keys" }
            val props = spec.getAsJsonObject("properties") ?: JsonObject()
            if (!external || extra == false.json()) spec.getAsJsonArray("required")?.forEach { require(props.has(it.asString)) { "Unknown required property" } }
        }
        if ("array" in types) require(spec.has("items")) { "Array items required" }
        if (!external) spec["default"]?.let { validateNode(spec, it, "default", depth) }
    }

    private fun validateAny(value: JsonElement, depth: Int): JsonElement {
        require(depth <= 24) { "Argument nesting exceeds limit" }
        when {
            value.isJsonObject -> value.asJsonObject.entrySet().forEach { AgentJson.checkUnicode(it.key); validateAny(it.value, depth + 1) }
            value.isJsonArray -> { require(value.asJsonArray.size() <= 1024); value.asJsonArray.forEach { validateAny(it, depth + 1) } }
            value.isJsonPrimitive && value.asJsonPrimitive.isString -> { AgentJson.checkUnicode(value.asString); require(value.asString.codePointCount(0, value.asString.length) <= 65_536) }
        }
        return value.deepCopy()
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
        private val ANNOTATIONS = setOf("default", "description", "title")
        private val KEYWORDS = setOf("type", "properties", "required", "additionalProperties", "items", "enum", "default", "description", "title", "minLength", "maxLength", "minItems", "maxItems", "minimum", "maximum")
    }
}
