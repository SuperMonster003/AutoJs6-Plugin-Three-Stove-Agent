package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.StepJournal
import java.math.BigDecimal

/** Same scalar dialect as the host's AgentManifestParser. No coercion or nested parameters. */
class ScriptParameters(schema: JsonObject) {
    private val schema = schema.deepCopy()
    private val properties: JsonObject
    private val required: List<String>

    init {
        require(this.schema.keySet().all { it in setOf("type", "properties", "required", "additionalProperties") })
        require(this.schema.string("type") == "object")
        this.schema["additionalProperties"]?.let { require(it == false.json()) }
        properties = this.schema["properties"]?.asJsonObject ?: JsonObject()
        required = this.schema["required"]?.asJsonArray?.map { strictString(it) } ?: emptyList()
        require(required.distinct().size == required.size && required.all(properties::has))
        for ((name, raw) in properties.entrySet()) {
            require(name.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]*")) && raw.isJsonObject)
            val spec = raw.asJsonObject
            require(spec.keySet().all { it in KEYWORDS })
            val type = spec.string("type")
            require(type in setOf("string", "number", "integer", "boolean"))
            spec["description"]?.let(::strictString)
            for (key in listOf("minimum", "maximum")) spec[key]?.let {
                require(type in setOf("number", "integer")); number(it)
            }
            if (spec.has("minimum") && spec.has("maximum")) require(number(spec["minimum"]) <= number(spec["maximum"]))
            for (key in listOf("minLength", "maxLength")) spec[key]?.let {
                require(type == "string" && number(it).longValueExact() >= 0)
            }
            if (spec.has("minLength") && spec.has("maxLength")) require(number(spec["minLength"]) <= number(spec["maxLength"]))
            spec["enum"]?.let {
                require(it.isJsonArray && it.asJsonArray.size() > 0)
                val entries = it.asJsonArray.toList()
                entries.forEach { value -> require(problem(spec, value, checkEnum = false) == null) }
                for (index in entries.indices) require(entries.take(index).none { earlier -> equivalent(earlier, entries[index]) })
            }
            spec["default"]?.let { require(problem(spec, it) == null) }
        }
    }

    fun validate(arguments: JsonObject): ScriptParameterCheck {
        if (StepJournal.bytes(arguments) > MAX_BYTES) return ScriptParameterCheck.Invalid(ScriptParameterProblem(listOf(null to "TOO_LARGE")))
        val result = arguments.deepCopy()
        for ((name, spec) in properties.entrySet()) if (!result.has(name) && spec.asJsonObject.has("default")) result.add(name, spec.asJsonObject["default"].deepCopy())
        if (StepJournal.bytes(result) > MAX_BYTES) return ScriptParameterCheck.Invalid(ScriptParameterProblem(listOf(null to "TOO_LARGE")))
        val errors = mutableListOf<Pair<String?, String>>()
        for (name in required) if (!result.has(name)) errors += name to "REQUIRED"
        for ((name, value) in result.entrySet()) {
            val spec = properties[name]
            val error = if (spec == null) "UNKNOWN_PARAMETER" else problem(spec.asJsonObject, value)
            error?.let { errors += name to it }
        }
        return if (errors.isEmpty()) ScriptParameterCheck.Valid(result) else ScriptParameterCheck.Invalid(ScriptParameterProblem(errors))
    }

    private fun problem(spec: JsonObject, value: JsonElement, checkEnum: Boolean = true): String? {
        if (!value.isJsonPrimitive) return "TYPE"
        val primitive = value.asJsonPrimitive
        when (spec.string("type")) {
            "string" -> {
                if (!primitive.isString) return "TYPE"
                AgentJson.checkUnicode(value.asString)
                val length = value.asString.codePointCount(0, value.asString.length).toLong()
                if (spec["minLength"]?.let { length < number(it).longValueExact() } == true) return "MIN_LENGTH"
                if (spec["maxLength"]?.let { length > number(it).longValueExact() } == true) return "MAX_LENGTH"
            }
            "boolean" -> if (!primitive.isBoolean) return "TYPE"
            "number", "integer" -> {
                if (!primitive.isNumber || !primitive.asDouble.isFinite()) return "TYPE"
                val numeric = number(value)
                if (spec.string("type") == "integer" && numeric.stripTrailingZeros().scale() > 0) return "TYPE"
                if (spec["minimum"]?.let { numeric < number(it) } == true) return "MINIMUM"
                if (spec["maximum"]?.let { numeric > number(it) } == true) return "MAXIMUM"
            }
        }
        if (checkEnum && spec.has("enum") && spec.getAsJsonArray("enum").none { equivalent(value, it) }) return "ENUM"
        return null
    }

    private fun strictString(value: JsonElement): String {
        require(value.isJsonPrimitive && value.asJsonPrimitive.isString)
        return value.asString.also(AgentJson::checkUnicode)
    }
    private fun number(value: JsonElement): BigDecimal {
        require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber && value.asDouble.isFinite())
        return value.asBigDecimal
    }
    private fun equivalent(left: JsonElement, right: JsonElement): Boolean =
        if (left.isJsonPrimitive && right.isJsonPrimitive && left.asJsonPrimitive.isNumber && right.asJsonPrimitive.isNumber)
            number(left).compareTo(number(right)) == 0 else left == right

    companion object {
        const val MAX_BYTES = 16 * 1024
        private val KEYWORDS = setOf("type", "description", "enum", "default", "minimum", "maximum", "minLength", "maxLength")
    }
}

sealed interface ScriptParameterCheck {
    class Valid(parameters: JsonObject) : ScriptParameterCheck {
        private val data = parameters.deepCopy()
        val parameters: JsonObject get() = data.deepCopy()
    }
    class Invalid(val problem: ScriptParameterProblem) : ScriptParameterCheck
}

/** Typed, bounded feedback; never echoes values or parser exception messages. */
class ScriptParameterProblem internal constructor(errors: List<Pair<String?, String>>) {
    private val errors = errors.toList()
    fun observation(): String {
        val missing = errors.any { it.second == "REQUIRED" }
        val fields = JsonArray()
        for ((name, reason) in errors.take(16)) {
            if (name != null && name.toByteArray(Charsets.UTF_8).size > 128) continue
            fields.add(jsonObject("reason" to reason.json()).apply { name?.let { addProperty("parameter", it) } })
        }
        return jsonObject("error" to "TOOL_ARGUMENTS_INVALID".json(),
            "detail" to (if (missing) "SCRIPT_PARAMETERS_MISSING" else "SCRIPT_PARAMETERS_INVALID").json(),
            "parameters" to fields, "omitted" to (errors.size - fields.size()).json(),
            "hint" to (if (missing) "Use ask to obtain missing parameters. Never invent an answer. ask.memoryKey may propose remembering an answer; saving requires separate confirmation."
                else "Correct the named parameters using the registered schema; do not coerce types or add undeclared keys.").json()).toString()
    }
    override fun toString() = "ScriptParameterProblem(count=${errors.size})"
}
