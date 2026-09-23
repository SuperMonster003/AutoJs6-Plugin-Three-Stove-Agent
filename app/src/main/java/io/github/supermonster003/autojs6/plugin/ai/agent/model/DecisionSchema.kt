package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*

/** Wire protocol, not Provider package identity or the model's display name. */
enum class ModelProtocol { LOCAL, OPENAI, ANTHROPIC, GEMINI, UNKNOWN }
enum class ArgumentsEncoding { OBJECT, JSON_STRING }

data class DecisionFormat(
    val protocol: ModelProtocol,
    val argumentsEncoding: ArgumentsEncoding,
    val responseSchemaJson: String?,
    val reason: String,
) {
    val degraded: Boolean get() = responseSchemaJson == null
    val nullableOptionals: Boolean get() = !degraded && protocol == ModelProtocol.OPENAI
}

class DecisionSchema(private val catalog: ToolCatalog) {
    fun generate(protocol: ModelProtocol, policy: ToolPolicy, forceString: Boolean = false): DecisionFormat {
        if (protocol == ModelProtocol.UNKNOWN) return degraded(protocol, "PROTOCOL_UNKNOWN")
        val enabled = catalog.tools.filter(policy::isEnabled).sortedBy { it.name }
        if (protocol == ModelProtocol.LOCAL) {
            return encoded(protocol, ArgumentsEncoding.OBJECT, envelope(schemaType("object"), enabled), "LOCAL_OBJECT")
        }
        // Dynamic manifest parameter names cannot be represented by a closed online object schema.
        val closed = enabled.all { closedObjects(it.inputSchema) }
        if (!forceString && closed) {
            val alternatives = enabled.map { it.inputSchema }.distinctBy { it.toString() }
            val arguments = when (alternatives.size) {
                0 -> objectSchema(JsonObject())
                1 -> alternatives.single()
                else -> jsonObject("anyOf" to JsonArray().apply { alternatives.forEach(::add) })
            }
            val candidate = online(envelope(arguments, enabled), protocol)
            if (candidate.toString().toByteArray(Charsets.UTF_8).size <= MAX_SCHEMA_BYTES &&
                (protocol != ModelProtocol.ANTHROPIC || withinAnthropicComplexity(candidate))) {
                return encoded(protocol, ArgumentsEncoding.OBJECT, candidate, "ONLINE_OBJECT")
            }
        }
        return encoded(protocol, ArgumentsEncoding.JSON_STRING,
            online(envelope(schemaType("string"), enabled), protocol),
            if (forceString) "REQUEST_REJECTED" else if (!closed) "DYNAMIC_PARAMETERS" else "SCHEMA_COMPLEXITY")
    }

    private fun encoded(protocol: ModelProtocol, encoding: ArgumentsEncoding, schema: JsonObject, reason: String): DecisionFormat {
        val text = schema.toString()
        check(text.toByteArray(Charsets.UTF_8).size <= MAX_SCHEMA_BYTES)
        return DecisionFormat(protocol, encoding, text, reason)
    }

    private fun envelope(arguments: JsonObject, enabled: List<ToolSpec>): JsonObject {
        val tool = schemaType("string").apply {
            if (enabled.isNotEmpty()) add("enum", JsonArray().apply { enabled.forEach { add(it.name) } })
        }
        return objectSchema(jsonObject(
            "kind" to enumSchema(if (enabled.isEmpty()) listOf("ask", "done") else listOf("tool", "ask", "done")),
            "reasoning" to schemaType("string"), "tool" to tool, "arguments" to arguments,
            "ask" to ASK_SCHEMA.deepCopy(), "done" to DONE_SCHEMA.deepCopy(),
        ), "kind")
    }

    private fun closedObjects(schema: JsonObject): Boolean {
        if (schema.string("type") == "object" && schema["additionalProperties"] != false.json()) return false
        return children(schema).all(::closedObjects)
    }

    private fun online(schema: JsonObject, protocol: ModelProtocol): JsonObject {
        val result = schema.deepCopy()
        // Validate all value limits locally; these constraints vary across compatible endpoints/models.
        listOf("minLength", "maxLength", "minItems", "maxItems", "minimum", "maximum", "default").forEach(result::remove)
        result.getAsJsonObject("properties")?.let { props ->
            val required = result.getAsJsonArray("required")?.map { it.asString }?.toSet().orEmpty()
            props.entrySet().toList().forEach { (key, child) ->
                val transformed = online(child.asJsonObject, protocol)
                props.add(key, if (protocol == ModelProtocol.OPENAI && key !in required) nullable(transformed) else transformed)
            }
            if (protocol == ModelProtocol.OPENAI) result.add("required", JsonArray().apply { props.keySet().forEach(::add) })
        }
        result.getAsJsonObject("items")?.let { result.add("items", online(it, protocol)) }
        result.getAsJsonArray("anyOf")?.let { union ->
            result.add("anyOf", JsonArray().apply { union.forEach { add(online(it.asJsonObject, protocol)) } })
        }
        if (protocol == ModelProtocol.GEMINI) {
            result.remove("additionalProperties")
            // responseSchema is OpenAPI Schema, not the newer responseJsonSchema field.
            result["type"]?.takeIf { it.isJsonArray }?.asJsonArray?.let { types ->
                val nonNull = types.map { it.asString }.filter { it != "null" }
                result.remove("type")
                if (nonNull.size == 1) result.addProperty("type", nonNull.single())
                else result.add("anyOf", JsonArray().apply { nonNull.forEach { add(schemaType(it)) } })
                if (types.any { it.asString == "null" }) result.addProperty("nullable", true)
            }
        }
        return result
    }

    private fun nullable(schema: JsonObject): JsonObject = jsonObject("anyOf" to jsonArray(schema, schemaType("null")))

    private fun withinAnthropicComplexity(schema: JsonObject): Boolean {
        var optional = 0
        var unions = 0
        fun visit(node: JsonObject) {
            node.getAsJsonObject("properties")?.let {
                optional += it.size() - (node.getAsJsonArray("required")?.size() ?: 0)
            }
            if (node.has("anyOf") || node["type"]?.isJsonArray == true) unions++
            children(node).forEach(::visit)
        }
        visit(schema)
        return optional <= 24 && unions <= 16
    }

    private fun children(schema: JsonObject): List<JsonObject> = buildList {
        schema.getAsJsonObject("properties")?.entrySet()?.forEach { add(it.value.asJsonObject) }
        schema.getAsJsonObject("items")?.let(::add)
        schema.getAsJsonArray("anyOf")?.forEach { add(it.asJsonObject) }
    }

    companion object {
        const val MAX_SCHEMA_BYTES = 16 * 1024
        fun degraded(protocol: ModelProtocol = ModelProtocol.UNKNOWN, reason: String = "STRUCTURED_JSON_UNAVAILABLE") =
            DecisionFormat(protocol, ArgumentsEncoding.OBJECT, null, reason)

        private fun schemaType(type: String) = jsonObject("type" to type.json())
        private fun enumSchema(values: List<String>) = schemaType("string").apply {
            add("enum", JsonArray().apply { values.forEach(::add) })
        }
        private fun objectSchema(properties: JsonObject, vararg required: String) = jsonObject(
            "type" to "object".json(), "additionalProperties" to false.json(), "properties" to properties,
            "required" to JsonArray().apply { required.forEach(::add) },
        )
        private fun strings() = jsonObject("type" to "array".json(), "items" to schemaType("string"))
        private val ASK_SCHEMA = objectSchema(jsonObject(
            "question" to schemaType("string"), "kind" to enumSchema(listOf("text", "choice", "confirm")),
            "choices" to strings(), "memoryKey" to schemaType("string"),
        ), "question")
        private val DONE_SCHEMA = objectSchema(jsonObject(
            "status" to enumSchema(listOf("completed", "partial", "failed", "blocked")), "summary" to schemaType("string"),
            "evidence" to strings(), "unfinished" to strings(),
            "orderStatus" to enumSchema(listOf("none", "cart", "pending_payment", "submitted", "paid")),
        ), "status", "summary")

        /** Small prompt contract; actual tool parameter schemas come only from ToolCatalog. */
        fun promptContract(format: DecisionFormat): String = jsonObject(
            "kind" to jsonArray("tool".json(), "ask".json(), "done".json()),
            "argumentsEncoding" to format.argumentsEncoding.name.json(),
            "nullableOptionals" to format.nullableOptionals.json(), "degraded" to format.degraded.json(),
            "reasoningMaxCharacters" to 600.json(), "ask" to ASK_SCHEMA.deepCopy(), "done" to DONE_SCHEMA.deepCopy(),
        ).toString()
    }
}

data class SchemaTarget(val providerId: String, val targetId: String, val protocol: ModelProtocol, val structuredJson: Boolean)

/** Per-link bounded memory. P2.4 will perform the actual broker retry and account for its model call. */
class SchemaFallbacks(private val schema: DecisionSchema) {
    private val rejected = LinkedHashSet<SchemaTarget>()
    @Synchronized fun select(target: SchemaTarget, policy: ToolPolicy): DecisionFormat {
        require(target.providerId.length in 1..256 && target.targetId.length in 1..256)
        return if (!target.structuredJson) DecisionSchema.degraded(target.protocol)
        else schema.generate(target.protocol, policy, target in rejected)
    }

    @Synchronized fun onRejected(target: SchemaTarget, previous: DecisionFormat, reason: String, policy: ToolPolicy): DecisionFormat? {
        if (reason != "REQUEST_REJECTED" || previous.degraded || previous.argumentsEncoding != ArgumentsEncoding.OBJECT ||
            target.protocol != previous.protocol || target.protocol !in setOf(ModelProtocol.OPENAI, ModelProtocol.ANTHROPIC, ModelProtocol.GEMINI) ||
            !target.structuredJson || target in rejected) return null
        if (rejected.size == 32) rejected.remove(rejected.first())
        rejected.add(target)
        return select(target, policy)
    }

    @Synchronized fun clear() = rejected.clear()
}
