package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import org.junit.Assert.*
import org.junit.Test

class DecisionValidatorTest {
    private val catalog = F.catalog()
    private val validator = DecisionValidator(catalog)
    private val objectFormat = DecisionSchema(catalog).generate(ModelProtocol.LOCAL, F.policy())
    private val stringFormat = DecisionSchema(catalog).generate(ModelProtocol.OPENAI, F.policy())

    private fun validate(text: String, format: DecisionFormat = objectFormat, policy: ToolPolicy = F.policy()) =
        validator.validate(DecisionParser.parse(text), policy, format)
    private fun fails(text: String, code: String = "DECISION_UNPARSABLE", format: DecisionFormat = objectFormat, policy: ToolPolicy = F.policy()) {
        assertEquals(code, assertThrows(DecisionFailure::class.java) { validate(text, format, policy) }.code)
    }

    @Test fun everyCatalogSchemaAcceptsItsToolDecisionInBothEncodings() {
        for ((name, args, _) in ToolHandlersTest.cases()) {
            for (format in listOf(objectFormat, stringFormat)) {
                val decision = jsonObject("kind" to "tool".json(), "tool" to name.json(),
                    "arguments" to if (format.argumentsEncoding == ArgumentsEncoding.OBJECT) AgentJson.parse(args) else args.json())
                assertEquals(name, (validate(decision.toString(), format) as AgentDecision.Tool).name)
            }
        }
    }

    @Test fun kindAndActiveBranchAreExclusive() {
        listOf(
            "{}", """{"kind":"execute"}""", """{"kind":"ask"}""", """{"kind":"done","done":[]}""",
            """{"kind":"tool","tool":"ui_dump"}""", """{"kind":"tool","arguments":{}}""",
            """{"kind":"tool","tool":"ui_dump","arguments":{},"ask":{}}""",
            """{"kind":"ask","ask":{"question":"Where?"},"arguments":{}}""",
            """{"kind":"done","done":{"status":"partial","summary":"waiting"},"tool":"ui_dump"}""",
            """{"kind":"ask","ask":{"question":"Where?"},"risk":"READ_ONLY"}""",
        ).forEach { fails(it) }
        assertTrue(validate("""{"kind":"tool","tool":"ui_dump","arguments":{},"ask":null,"done":null,"reasoning":null}""") is AgentDecision.Tool)
    }

    @Test fun disabledAndUnknownToolsNeverPassValidation() {
        fails("""{"kind":"tool","tool":"files_read","arguments":{"path":"x"}}""", "TOOL_DISABLED", policy = ToolPolicy())
        fails("""{"kind":"tool","tool":"script_run_source","arguments":{}}""", "TOOL_UNKNOWN")
        fails("""{"kind":"tool","tool":"ocr_screen","arguments":{}}""", "TOOL_DISABLED", policy = ToolPolicy())
    }

    @Test fun invalidArgumentsDoNotGetCoercedOrBypassHandlerAdmission() {
        listOf(
            """{"kind":"tool","tool":"ui_dump","arguments":{"maxNodes":"2"}}""",
            """{"kind":"tool","tool":"ui_dump","arguments":{"maxNodes":401}}""",
            """{"kind":"tool","tool":"ui_dump","arguments":{"unexpected":null}}""",
            """{"kind":"tool","tool":"ui_click","arguments":{"nodeRef":"#n1","selector":{"text":"ok"}}}""",
            """{"kind":"tool","tool":"ui_click","arguments":{"x":1,"y":2}}""",
            """{"kind":"tool","tool":"ui_click","arguments":{"selector":{}}}""",
        ).forEach { fails(it, "TOOL_ARGUMENTS_INVALID") }
    }

    @Test fun strictOptionalNullsAreRestoredUsingTheSelectedToolSchema() {
        val policy = ToolPolicy(ToolGroup.entries.associateWith { it == ToolGroup.ACT })
        val format = DecisionSchema(catalog).generate(ModelProtocol.OPENAI, policy)
        assertEquals(ArgumentsEncoding.OBJECT, format.argumentsEncoding)
        val result = validate("""{"kind":"tool","tool":"ui_set_text","arguments":{"nodeRef":"#n1","snapshotId":null,"selector":null,"text":"ok","append":null}}""", format, policy) as AgentDecision.Tool
        assertFalse(result.arguments.has("selector"))
        assertFalse(result.arguments.flag("append")!!)
        fails("""{"kind":"tool","tool":"ui_set_text","arguments":{"nodeRef":"#n1","text":null}}""", "TOOL_ARGUMENTS_INVALID", format, policy)
        fails("""{"kind":"tool","tool":"ui_click","arguments":{"nodeRef":"#n1","evil":null}}""", "TOOL_ARGUMENTS_INVALID", format, policy)
        val selected = validate("""{"kind":"tool","tool":"ui_click","arguments":{"nodeRef":null,"snapshotId":null,"selector":{"text":"ok","desc":null}}}""", format, policy) as AgentDecision.Tool
        assertEquals("""{"selector":{"text":"ok"}}""", selected.arguments.toString())
    }

    @Test fun realScriptNullParametersSurviveStringDecoding() {
        val input = """{"id":"coffee","parameters":{"milk":null,"count":1,"delivery":true}}"""
        val root = jsonObject("kind" to "tool".json(), "tool" to "script_run".json(), "arguments" to input.json())
        val result = validate(root.toString(), stringFormat) as AgentDecision.Tool
        assertTrue(result.arguments.getAsJsonObject("parameters")["milk"].isJsonNull)
        assertEquals(1L, result.arguments.getAsJsonObject("parameters").number("count"))
    }

    @Test fun argumentEncodingIsNegotiatedAndInnerJsonIsStrict() {
        fails("""{"kind":"tool","tool":"ui_dump","arguments":"{}"}""")
        fails("""{"kind":"tool","tool":"ui_dump","arguments":{}}""", format = stringFormat)
        for (inner in listOf("{}{}", "[]", "null", "{\"x\":1,\"x\":2}", "```{} ```")) {
            fails(jsonObject("kind" to "tool".json(), "tool" to "ui_dump".json(), "arguments" to inner.json()).toString(), format = stringFormat)
        }
    }

    @Test fun reasoningTruncationCountsUnicodeCodePoints() {
        val note = "😀中".repeat(400)
        val input = jsonObject("kind" to "tool".json(), "tool" to "ui_dump".json(), "arguments" to JsonObject(), "reasoning" to note.json())
        val result = validate(input.toString())
        assertEquals("😀中".repeat(300), result.reasoning)
        AgentJson.checkUnicode(result.reasoning!!)
        fails("""{"kind":"tool","tool":"ui_dump","arguments":{},"reasoning":1}""")
    }

    @Test fun asksValidateKindsChoicesAndMemoryProposal() {
        val ask = validate("""{"kind":"ask","ask":{"question":"配送到哪里?","kind":"choice","choices":["公司","家"],"memoryKey":"delivery"}}""") as AgentDecision.Ask
        assertEquals(listOf("公司", "家"), ask.choices)
        assertEquals("delivery", ask.memoryKey)
        assertEquals("text", (validate("""{"kind":"ask","ask":{"question":"Where?","choices":null}}""") as AgentDecision.Ask).kind)
        listOf(
            """{"question":"Where?","kind":"choice"}""", """{"question":"Where?","kind":"choice","choices":["x","x"]}""",
            """{"question":"Where?","kind":"confirm","choices":["yes"]}""", """{"question":" ","kind":"text"}""",
            """{"question":"Where?","kind":"other"}""", """{"question":"Where?","choices":{}}""", """{"question":"Where?","remember":true}""",
        ).forEach { fails("{\"kind\":\"ask\",\"ask\":$it}") }
    }

    @Test fun doneStructuresValidateBeforeLaterEvidencePolicy() {
        val done = validate("""{"kind":"done","done":{"status":"partial","summary":"Waiting for payment","evidence":["Payment page observed"],"unfinished":["Payment"],"orderStatus":"pending_payment"}}""") as AgentDecision.Done
        assertEquals("partial", done.status)
        assertEquals("pending_payment", done.orderStatus)
        // Observed completion is checked in P4, not inferred from the model's structural validity.
        assertTrue(validate("""{"kind":"done","done":{"status":"completed","summary":"done"}}""") is AgentDecision.Done)
        listOf(
            """{"status":"completed"}""", """{"status":"cancelled","summary":"x"}""",
            """{"status":"partial","summary":"x","orderStatus":"complete"}""",
            """{"status":"partial","summary":"x","evidence":[42]}""",
            """{"status":"partial","summary":"x","extra":null}""",
        ).forEach { fails("{\"kind\":\"done\",\"done\":$it}") }
    }

    @Test fun characterAndListLimitsAreEnforcedEvenWithoutServerLimits() {
        for ((key, maximum) in listOf("question" to 500, "memoryKey" to 64)) {
            val ask = jsonObject("question" to "Where?".json(), key to "😀".repeat(maximum).json())
            validate(jsonObject("kind" to "ask".json(), "ask" to ask).toString())
            ask.addProperty(key, "😀".repeat(maximum + 1))
            fails(jsonObject("kind" to "ask".json(), "ask" to ask).toString())
        }
        val done = jsonObject("status" to "partial".json(), "summary" to "中".repeat(1000).json())
        validate(jsonObject("kind" to "done".json(), "done" to done).toString())
        done.addProperty("summary", "中".repeat(1001))
        fails(jsonObject("kind" to "done".json(), "done" to done).toString())
        for (key in listOf("evidence", "unfinished")) for (entries in listOf(jsonArray("x".repeat(201).json()), JsonArray().apply { repeat(9) { add("x") } })) {
            fails(jsonObject("kind" to "done".json(), "done" to jsonObject("status" to "partial".json(), "summary" to "x".json(), key to entries)).toString())
        }
        fails(jsonObject("kind" to "ask".json(), "ask" to jsonObject("question" to "Which?".json(), "kind" to "choice".json(), "choices" to JsonArray().apply { repeat(9) { add("c$it") } })).toString())
    }

    @Test fun repairSessionAllowsExactlyTwoRepairsAndNeverEchoesBadContent() {
        for (format in listOf(objectFormat, DecisionSchema.degraded())) {
            val session = DecisionRepairSession(validator, ToolPolicy(), format)
            val first = session.evaluate("private password input") as DecisionAttempt.Repair
            assertEquals(1, first.attempt)
            assertFalse(first.observation.toString().contains("password"))
            assertEquals(2, (session.evaluate("{}") as DecisionAttempt.Repair).attempt)
            assertTrue(session.evaluate("{}") is DecisionAttempt.Exhausted)
            assertEquals(2, session.repairsUsed)
            assertThrows(IllegalStateException::class.java) { session.evaluate("{}") }
        }
    }

    @Test fun repairedValidDecisionSettlesTheStepAndRetainsParseMode() {
        val session = DecisionRepairSession(validator, ToolPolicy(), DecisionSchema.degraded())
        assertTrue(session.evaluate("""{"kind":"tool","tool":"shell_exec","arguments":{"cmd":"pwd"}}""") is DecisionAttempt.Repair)
        val accepted = session.evaluate("```json\n{\"kind\":\"tool\",\"tool\":\"ui_dump\",\"arguments\":{}}\n```") as DecisionAttempt.Accepted
        assertEquals(ParseMode.EXTRACTED, accepted.parseMode)
        assertEquals(1, session.repairsUsed)
        assertThrows(IllegalStateException::class.java) { session.evaluate("{}") }
    }
}
