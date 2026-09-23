package io.github.supermonster003.autojs6.plugin.ai.agent.model

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DecisionParserTest(private val label: String, private val input: String, private val degraded: Boolean, private val expected: ParseMode?) {
    @Test fun parseMatrix() {
        if (expected == null) {
            val error = assertThrows(DecisionFailure::class.java) { DecisionParser.parse(input, degraded) }
            assertEquals("DECISION_UNPARSABLE", error.code)
        } else {
            val result = DecisionParser.parse(input, degraded)
            assertEquals(expected, result.parseMode)
            assertEquals("done", result.value.string("kind"))
        }
    }

    companion object {
        private const val GOOD = """{"kind":"done","done":{"status":"completed","summary":"中😀"}}"""
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases(): List<Array<Any?>> = listOf(
            arrayOf("strict JSON", GOOD, false, ParseMode.STRICT),
            arrayOf("whitespace", " \n$GOOD\r\n", false, ParseMode.STRICT),
            arrayOf("degraded exact JSON", GOOD, true, ParseMode.STRICT),
            arrayOf("fence", "```json\n$GOOD\n```", true, ParseMode.EXTRACTED),
            arrayOf("prefix and suffix", "My decision:\n$GOOD\nDone.", true, ParseMode.EXTRACTED),
            arrayOf("strict fence", "```json\n$GOOD\n```", false, null),
            arrayOf("strict multiple objects", "$GOOD$GOOD", false, null),
            arrayOf("degraded first object only", "$GOOD{bad}", true, ParseMode.EXTRACTED),
            arrayOf("invalid first object is not skipped", "{bad}$GOOD", true, null),
            arrayOf("quoted braces and escaped quote", "prefix {\"kind\":\"done\",\"reasoning\":\"a}\\\"{b\\\\c\"} suffix", true, ParseMode.EXTRACTED),
            arrayOf("duplicate keys", """{"kind":"done","kind":"tool"}""", false, null),
            arrayOf("duplicate nested keys", """{"kind":"done","done":{"status":"completed","status":"failed"}}""", true, null),
            arrayOf("comments", """{"kind":"done"/*x*/}""", true, null),
            arrayOf("single quotes", "{'kind':'done'}", false, null),
            arrayOf("unpaired escaped surrogate", """{"kind":"done","reasoning":"\uD800"}""", true, null),
            arrayOf("unpaired raw surrogate", "prefix\uD800$GOOD", true, null),
            arrayOf("truncated", GOOD.dropLast(1), true, null),
            arrayOf("trailing comma", """{"kind":"done",}""", true, null),
            arrayOf("null", "null", false, null),
            arrayOf("array", "[$GOOD]", false, null),
            arrayOf("empty", "", true, null),
            arrayOf("non JSON", "nothing happened", true, null),
            arrayOf("byte bound", "中".repeat(22_000) + GOOD, true, null),
            arrayOf("nesting bound", "{\"kind\":\"done\",\"x\":" + "[".repeat(40) + "0" + "]".repeat(40) + "}", true, null),
            arrayOf("number bound", """{"kind":"done","x":1e99999}""", false, null),
        )
    }
}
