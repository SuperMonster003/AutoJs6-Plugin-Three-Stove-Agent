package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class ScriptParametersTest {
    private fun schema(property: String, required: Boolean = true) = AgentJson.objectOf("""{"type":"object","properties":{"value":$property},"required":${if (required) "[\"value\"]" else "[]"}}""")
    private fun check(property: String, value: String) = ScriptParameters(schema(property)).validate(AgentJson.objectOf("""{"value":$value}"""))
    private fun reason(check: ScriptParameterCheck) = AgentJson.objectOf((check as ScriptParameterCheck.Invalid).problem.observation())
        .getAsJsonArray("parameters")[0].asJsonObject.string("reason")

    @Test fun scalarTypesNeverCoerceStringsBooleansNullOrContainers() {
        for ((type, valid, invalid) in listOf(
            Triple("string", "\"1\"", listOf("1", "true", "null", "[]", "{}")),
            Triple("boolean", "false", listOf("\"false\"", "0", "null")),
            Triple("number", "1.5", listOf("\"1.5\"", "true", "null")),
            Triple("integer", "1.0", listOf("1.5", "\"1\"", "false")))) {
            val property = """{"type":"$type"}"""
            assertTrue(check(property, valid) is ScriptParameterCheck.Valid)
            for (value in invalid) assertEquals("$type / $value", "TYPE", reason(check(property, value)))
        }
    }
    @Test fun requiredDefaultsAreFilledBeforeRequiredCheckWithoutChangingInputs() {
        val schema = schema("""{"type":"integer","default":3,"minimum":1}""")
        val before = schema.deepCopy(); val input = JsonObject()
        val checked = ScriptParameters(schema).validate(input) as ScriptParameterCheck.Valid
        assertEquals(3L, checked.parameters.number("value")); assertEquals(before, schema); assertEquals(0, input.size())
        checked.parameters.addProperty("value", 9)
        assertEquals(3L, checked.parameters.number("value"))
        assertEquals(4L, (ScriptParameters(schema).validate(AgentJson.objectOf("""{"value":4}""")) as ScriptParameterCheck.Valid).parameters.number("value"))
        assertEquals("TYPE", reason(ScriptParameters(schema).validate(AgentJson.objectOf("""{"value":null}"""))))
    }
    @Test fun optionalAbsentValuesStayAbsentAndEmptySchemaRejectsExtras() {
        assertEquals(0, (ScriptParameters(schema("""{"type":"string"}""", false)).validate(JsonObject()) as ScriptParameterCheck.Valid).parameters.size())
        assertEquals("UNKNOWN_PARAMETER", reason(ScriptParameters(AgentJson.objectOf("""{"type":"object"}""")).validate(AgentJson.objectOf("""{"value":"secret"}"""))))
    }
    @Test fun missingParametersProduceActionableFeedbackWithoutEchoingValues() {
        val checked = ScriptParameters(schema("""{"type":"string"}""")).validate(AgentJson.objectOf("""{"undeclared":"private input"}"""))
        val observation = AgentJson.objectOf((checked as ScriptParameterCheck.Invalid).problem.observation())
        assertEquals("SCRIPT_PARAMETERS_MISSING", observation.string("detail"))
        assertEquals("value", observation.getAsJsonArray("parameters")[0].asJsonObject.string("parameter"))
        assertTrue(observation.string("hint")!!.contains("ask")); assertFalse(observation.toString().contains("private input"))
    }
    @Test fun enumEqualityIsNumericButRetainsTypeAndValidatesAllMembers() {
        assertTrue(check("""{"type":"number","enum":[1,2]}""", "1.0") is ScriptParameterCheck.Valid)
        assertEquals("ENUM", reason(check("""{"type":"integer","enum":[1,2]}""", "3")))
        for (spec in listOf("""{"type":"number","enum":[1,1.0]}""", """{"type":"number","enum":[1,"2"]}""",
            """{"type":"string","enum":[]}""", """{"type":"integer","enum":[1],"default":2}""")) {
            assertThrows(IllegalArgumentException::class.java) { ScriptParameters(schema(spec)) }
        }
    }
    @Test fun numericBoundsAreInclusiveAndNumbersMustBeFinite() {
        val spec = """{"type":"number","minimum":-2.5,"maximum":3}"""
        for (value in listOf("-2.5", "3", "3e0")) assertTrue(check(spec, value) is ScriptParameterCheck.Valid)
        assertEquals("MINIMUM", reason(check(spec, "-2.6"))); assertEquals("MAXIMUM", reason(check(spec, "3.01")))
        val nonFinite = JsonObject().apply { add("value", JsonPrimitive(Double.POSITIVE_INFINITY)) }
        assertEquals("TYPE", reason(ScriptParameters(schema("""{"type":"number"}""")).validate(nonFinite)))
        assertThrows(IllegalArgumentException::class.java) { ScriptParameters(schema("""{"type":"number","minimum":2,"maximum":1}""")) }
    }
    @Test fun stringBoundsCountUnicodeCodePointsAndPreserveNewlines() {
        val spec = """{"type":"string","minLength":2,"maxLength":2}"""
        assertTrue(check(spec, "\"\uD83D\uDE00中\"") is ScriptParameterCheck.Valid)
        assertTrue(check(spec, "\"a\\n\"") is ScriptParameterCheck.Valid)
        assertEquals("MIN_LENGTH", reason(check(spec, "\"\uD83D\uDE00\"")))
        assertEquals("MAX_LENGTH", reason(check(spec, "\"\uD83D\uDE00中文\"")))
    }
    @Test fun unsupportedSchemasCannotBeAdmitted() {
        val invalid = listOf("""{"type":"array"}""", """{"type":"object"}""", """{"type":["string","null"]}""",
            """{"type":"string","pattern":".*"}""", """{"type":"string","minimum":1}""",
            """{"type":"integer","minLength":1}""", """{"type":"string","minLength":-1}""",
            """{"type":"string","maxLength":1.5}""", """{"type":"boolean","default":"false"}""")
        for (property in invalid) assertNotNull(property, runCatching { ScriptParameters(schema(property)) }.exceptionOrNull())
        for (root in listOf("""{"type":"object","additionalProperties":true}""", """{"type":"object","required":["x"]}""",
            """{"type":"object","properties":{"bad-key":{"type":"string"}}}""",
            """{"type":"object","properties":{"x":{"type":"string"}},"required":["x","x"]}""")) {
            assertNotNull(root, runCatching { ScriptParameters(AgentJson.objectOf(root)) }.exceptionOrNull())
        }
    }
    @Test fun bothOriginalAndDefaultedParametersRespectTheByteLimit() {
        val validator = ScriptParameters(schema("""{"type":"string"}"""))
        val input = JsonObject().apply { addProperty("value", "中".repeat(6000)) }
        assertEquals("TOO_LARGE", reason(validator.validate(input)))
        val spec = jsonObject("type" to "string".json(), "default" to "中".repeat(6000).json())
        assertEquals("TOO_LARGE", reason(ScriptParameters(schema(spec.toString())).validate(JsonObject())))
        val edge = JsonObject().apply { addProperty("value", "x".repeat(ScriptParameters.MAX_BYTES - 12)) }
        assertTrue(validator.validate(edge) is ScriptParameterCheck.Valid)
        edge.addProperty("value", "x".repeat(ScriptParameters.MAX_BYTES - 11))
        assertEquals("TOO_LARGE", reason(validator.validate(edge)))
    }
    @Test fun errorFeedbackIsBoundedEvenForLongUndeclaredNames() {
        val input = JsonObject().apply { repeat(100) { addProperty("x$it", "never echo") }; addProperty("n".repeat(5000), true) }
        val feedback = (ScriptParameters(AgentJson.objectOf("""{"type":"object"}""")).validate(input) as ScriptParameterCheck.Invalid).problem.observation()
        assertTrue(feedback.toByteArray().size < 4096); assertFalse(feedback.contains("never echo"))
        assertTrue(AgentJson.objectOf(feedback).number("omitted")!! > 0)
    }
}
