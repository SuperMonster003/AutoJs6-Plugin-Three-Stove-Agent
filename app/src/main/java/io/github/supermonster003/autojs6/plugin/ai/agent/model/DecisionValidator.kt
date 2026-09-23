package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*

sealed interface AgentDecision {
    val reasoning: String?
    data class Tool(val name: String, val arguments: JsonObject, override val reasoning: String?) : AgentDecision
    data class Ask(val question: String, val kind: String, val choices: List<String>, val memoryKey: String?, override val reasoning: String?) : AgentDecision
    data class Done(val status: String, val summary: String, val evidence: List<String>, val unfinished: List<String>, val orderStatus: String?, override val reasoning: String?) : AgentDecision
}

class DecisionValidator(private val catalog: ToolCatalog) {
    private val handlers = ToolHandlers(catalog)

    fun validate(parsed: ParsedDecision, policy: ToolPolicy, format: DecisionFormat): AgentDecision {
        val root = parsed.value
        closed(root, setOf("kind", "reasoning", "tool", "arguments", "ask", "done"))
        val kind = text(root, "kind", 8, required = true)
        val reasoning = optional(root, "reasoning")?.let {
            if (!it.isJsonPrimitive || !it.asJsonPrimitive.isString) invalid("reasoning must be a string.")
            val value = it.asString
            AgentJson.checkUnicode(value)
            value.substring(0, value.offsetByCodePoints(0, minOf(600, value.codePointCount(0, value.length))))
        }
        val allowed = when (kind) {
            "tool" -> setOf("tool", "arguments")
            "ask" -> setOf("ask")
            "done" -> setOf("done")
            else -> invalid("kind must be tool, ask or done.")
        }
        if (setOf("tool", "arguments", "ask", "done").any { it !in allowed && optional(root, it) != null }) {
            invalid("Only the branch selected by kind may contain values.")
        }
        return when (kind) {
            "tool" -> {
                val name = text(root, "tool", 64, required = true)!!
                val spec = try { policy.requireEnabled(catalog, name) } catch (error: ToolFailure) {
                    throw DecisionFailure(error.code, error.hint)
                }
                val raw = optional(root, "arguments") ?: invalid("Tool decisions require arguments, including an empty object when appropriate.")
                val decoded = if (format.argumentsEncoding == ArgumentsEncoding.JSON_STRING) {
                    if (!raw.isJsonPrimitive || !raw.asJsonPrimitive.isString) invalid("arguments must be a JSON-encoded object string in this format.")
                    try { AgentJson.objectOf(raw.asString) } catch (_: Exception) { invalid("arguments must contain one valid JSON object.") }
                } else {
                    if (!raw.isJsonObject) invalid("arguments must be an object in this format.")
                    raw.asJsonObject
                }
                val normalized = if (format.nullableOptionals && format.argumentsEncoding == ArgumentsEncoding.OBJECT) {
                    restoreOmittedOptionals(decoded, spec.inputSchema).asJsonObject
                } else decoded.deepCopy()
                val arguments = try {
                    val checked = spec.validator.validate(normalized).asJsonObject
                    handlers.prepare(name, checked, policy) // Also enforce target one-of and composite-plan admission.
                    checked
                } catch (error: ToolFailure) { throw DecisionFailure(error.code, error.hint) }
                catch (_: IllegalArgumentException) { throw DecisionFailure("TOOL_ARGUMENTS_INVALID", "Arguments must match the selected tool input schema.") }
                AgentDecision.Tool(name, arguments, reasoning)
            }
            "ask" -> {
                val ask = branch(root, "ask")
                closed(ask, setOf("question", "kind", "choices", "memoryKey"))
                val question = text(ask, "question", 500, required = true)!!
                val askKind = text(ask, "kind", 16) ?: "text"
                if (askKind !in setOf("text", "choice", "confirm")) invalid("Unknown ask kind.")
                val choices = strings(ask, "choices")
                if (askKind == "choice" && choices.isEmpty()) invalid("A choice question needs 1 to 8 choices.")
                if (askKind != "choice" && choices.isNotEmpty()) invalid("choices are only valid for a choice question.")
                if (choices.distinct().size != choices.size) invalid("Choices must be distinct.")
                AgentDecision.Ask(question, askKind, choices, text(ask, "memoryKey", 64), reasoning)
            }
            else -> {
                val done = branch(root, "done")
                closed(done, setOf("status", "summary", "evidence", "unfinished", "orderStatus"))
                val status = text(done, "status", 16, required = true)!!
                if (status !in setOf("completed", "partial", "failed", "blocked")) invalid("Unknown done status.")
                val orderStatus = text(done, "orderStatus", 32)
                if (orderStatus != null && orderStatus !in setOf("none", "cart", "pending_payment", "submitted", "paid")) invalid("Unknown order status.")
                AgentDecision.Done(status, text(done, "summary", 1000, required = true)!!,
                    strings(done, "evidence"), strings(done, "unfinished"), orderStatus, reasoning)
                // P4 checks observed completion/evidence and order-state semantics; parsing never executes them.
            }
        }
    }

    private fun restoreOmittedOptionals(value: JsonElement, schema: JsonObject): JsonElement {
        if (value.isJsonArray) {
            val itemSchema = schema.getAsJsonObject("items") ?: return value.deepCopy()
            return JsonArray().apply { value.asJsonArray.forEach { add(restoreOmittedOptionals(it, itemSchema)) } }
        }
        if (!value.isJsonObject) return value.deepCopy()
        val props = schema.getAsJsonObject("properties") ?: return value.deepCopy()
        val required = schema.getAsJsonArray("required")?.map { it.asString }?.toSet().orEmpty()
        return JsonObject().apply {
            value.asJsonObject.entrySet().forEach { (key, item) ->
                val child = props.getAsJsonObject(key)
                // Unknown null keys remain unknown; required/null-capable values must still be validated.
                val nullable = child?.get("type")?.let { it.isJsonArray && it.asJsonArray.any { type -> type.asString == "null" } } == true
                if (!(item.isJsonNull && key !in required && child != null && !nullable)) {
                    add(key, if (child == null) item.deepCopy() else restoreOmittedOptionals(item, child))
                }
            }
        }
    }

    private fun optional(value: JsonObject, key: String): JsonElement? = value[key]?.takeUnless { it.isJsonNull }
    private fun branch(value: JsonObject, key: String): JsonObject = optional(value, key)?.takeIf { it.isJsonObject }?.asJsonObject
        ?: invalid("The selected branch requires its object.")
    private fun closed(value: JsonObject, keys: Set<String>) {
        if (value.keySet().any { it !in keys }) invalid("Unexpected decision property.")
    }
    private fun text(value: JsonObject, key: String, maximum: Int, required: Boolean = false): String? {
        val item = optional(value, key) ?: return if (required) invalid("A required text field is missing.") else null
        if (!item.isJsonPrimitive || !item.asJsonPrimitive.isString) invalid("A decision text field has the wrong type.")
        val result = item.asString
        AgentJson.checkUnicode(result)
        if (result.isBlank() || result.codePointCount(0, result.length) > maximum) invalid("A decision text field is empty or exceeds its character limit.")
        return result
    }
    private fun strings(value: JsonObject, key: String): List<String> {
        val items = optional(value, key) ?: return emptyList()
        if (!items.isJsonArray || items.asJsonArray.size() > 8) invalid("A decision list must contain at most 8 text entries.")
        return items.asJsonArray.map { text(jsonObject("entry" to it), "entry", 200, required = true)!! }
    }
    private fun invalid(hint: String): Nothing = throw DecisionFailure("DECISION_UNPARSABLE", hint)
}

sealed interface DecisionAttempt {
    data class Accepted(val decision: AgentDecision, val parseMode: ParseMode) : DecisionAttempt
    data class Repair(val observation: JsonObject, val attempt: Int) : DecisionAttempt
    data class Exhausted(val error: DecisionFailure) : DecisionAttempt
}

/** One instance per step. A valid decision or exhausted allowance seals the step. */
class DecisionRepairSession(private val validator: DecisionValidator, private val policy: ToolPolicy, private val format: DecisionFormat) {
    var repairsUsed = 0
        private set
    private var sealed = false
    fun evaluate(text: String): DecisionAttempt {
        check(!sealed) { "Decision step is already settled" }
        return try {
            val parsed = DecisionParser.parse(text, format.degraded)
            val decision = validator.validate(parsed, policy, format)
            sealed = true
            DecisionAttempt.Accepted(decision, parsed.parseMode)
        } catch (failure: DecisionFailure) {
            if (repairsUsed == MAX_REPAIRS) {
                sealed = true
                DecisionAttempt.Exhausted(DecisionFailure("DECISION_UNPARSABLE", "Decision repair allowance exhausted."))
            } else {
                repairsUsed++
                DecisionAttempt.Repair(jsonObject("error" to failure.code.json(), "hint" to failure.hint.json(),
                    "repairAttempt" to repairsUsed.json(), "remainingRepairs" to (MAX_REPAIRS - repairsUsed).json()), repairsUsed)
            }
        }
    }
    companion object { const val MAX_REPAIRS = 2 }
}
