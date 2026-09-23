package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.JsonObject

enum class ParseMode { STRICT, EXTRACTED }
data class ParsedDecision(val value: JsonObject, val parseMode: ParseMode)

/** Model syntax failures carry only fixed diagnostics, never fragments of the response. */
class DecisionFailure(val code: String, val hint: String) : IllegalArgumentException("$code: $hint")

object DecisionParser {
    fun parse(text: String, degraded: Boolean = false): ParsedDecision {
        if (text.length > AgentJson.MAX_MODEL_BYTES || text.toByteArray(Charsets.UTF_8).size > AgentJson.MAX_MODEL_BYTES) {
            fail("Decision exceeds the response byte limit.")
        }
        try { AgentJson.checkUnicode(text) } catch (_: IllegalArgumentException) { fail("Decision contains invalid Unicode.") }
        try { return ParsedDecision(AgentJson.objectOf(text), ParseMode.STRICT) } catch (_: Exception) {
            if (!degraded) fail("Return exactly one valid JSON object without a fence or trailing text.")
        }
        // Select the first object, even if invalid. Never skip it in favor of a later executable decision.
        val start = text.indexOf('{')
        if (start < 0) fail("No JSON object was found.")
        var depth = 0
        var quoted = false
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            if (quoted) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == '"') quoted = false
            } else when (char) {
                '"' -> quoted = true
                '{' -> { if (++depth > 32) fail("Decision nesting exceeds the limit.") }
                '}' -> if (--depth == 0) {
                    val value = try { AgentJson.objectOf(text.substring(start, index + 1)) } catch (_: Exception) {
                        fail("The first JSON object is invalid; repair that decision.")
                    }
                    return ParsedDecision(value, ParseMode.EXTRACTED)
                }
            }
        }
        fail("The first JSON object is incomplete.")
    }

    private fun fail(hint: String): Nothing = throw DecisionFailure("DECISION_UNPARSABLE", hint)
}
