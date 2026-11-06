package io.github.supermonster003.autojs6.plugin.three.stove.agent.runner

import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.RunnerFixture.Companion.done
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.RunnerFixture.Companion.tool
import org.junit.Assert.*
import org.junit.Test

/**
 * Roadmap P25 (issue #1): a provider that accepts a response schema but does not enforce it (Zhipu GLM answers inside a
 * Markdown fence with HTTP 200) no longer fails every task with DECISION_UNPARSABLE. The first JSON object of such a
 * reply is used, the step records EXTRACTED, the format and the request are unchanged, and the workbench is told once.
 */
class UnenforcedSchemaTest {
    private val catalog = F.catalog()
    private val validator = DecisionValidator(catalog)
    private val policy = ToolPolicy(ToolGroup.entries.associateWith { it == ToolGroup.USER })
    private val schema = DecisionSchema(catalog)
    private val en = AgentJson.objectOf(F.asset("runner/texts.json")).getAsJsonObject("en")
    private val strictHint = "Return exactly one valid JSON object without a fence or trailing text."

    private fun fenced(decision: String) = "```json\n$decision\n```"
    private fun session(format: DecisionFormat) = DecisionRepairSession(validator, policy, format)
    private fun progress(f: RunnerFixture) = f.events.filter { it.type == "progress" }.map { it.payload.string("message") }
    private fun steps(f: RunnerFixture) = f.events.filter { it.type == "step" }.map { it.payload.getAsJsonObject("decision") }

    @Test fun aFencedReplyToASchemaRequestIsAcceptedAsExtractedAndFlagsTheProvider() {
        for (protocol in listOf(ModelProtocol.OPENAI, ModelProtocol.ANTHROPIC, ModelProtocol.GEMINI, ModelProtocol.LOCAL)) {
            val format = schema.generate(protocol, policy)
            assertNotNull(protocol.name, format.responseSchemaJson); assertFalse(format.degraded)
            val session = session(format)
            val accepted = session.evaluate(fenced(done())) as DecisionAttempt.Accepted
            assertEquals(ParseMode.EXTRACTED, accepted.parseMode)
            assertTrue(accepted.decision is AgentDecision.Done)
            assertTrue(session.schemaIgnored); assertEquals(0, session.repairsUsed); assertTrue(session.rejections.isEmpty())
        }
    }

    @Test fun surroundingProseCountsLikeAFence() {
        val session = session(schema.generate(ModelProtocol.OPENAI, policy))
        val accepted = session.evaluate("Here is my decision:\n${done()}\nDone.") as DecisionAttempt.Accepted
        assertEquals(ParseMode.EXTRACTED, accepted.parseMode); assertTrue(session.schemaIgnored)
    }

    @Test fun aBareObjectStaysStrictAndDoesNotFlagTheProvider() {
        val session = session(schema.generate(ModelProtocol.OPENAI, policy))
        assertEquals(ParseMode.STRICT, (session.evaluate(" \n${done()}\r\n") as DecisionAttempt.Accepted).parseMode)
        assertFalse(session.schemaIgnored)
    }

    @Test fun aReplyWithoutAValidFirstObjectKeepsTheStrictHintAndTheRepairAllowance() {
        for (reply in listOf("nothing happened", "```json\n{bad}\n```", "{bad}${done()}", "```json\n${done().dropLast(1)}\n```")) {
            val session = session(schema.generate(ModelProtocol.OPENAI, policy))
            val repair = session.evaluate(reply) as DecisionAttempt.Repair
            assertEquals("DECISION_UNPARSABLE", repair.observation.string("error"))
            assertEquals(reply, strictHint, repair.observation.string("hint"))
            assertFalse(reply, session.schemaIgnored)
            // The repaired reply is still fenced: accepted on the same allowance, and the provider is flagged now.
            assertEquals(ParseMode.EXTRACTED, (session.evaluate(fenced(done())) as DecisionAttempt.Accepted).parseMode)
            assertTrue(session.schemaIgnored); assertEquals(1, session.repairsUsed)
            assertEquals(listOf(DecisionRejection.DECISION_UNPARSABLE), session.rejections)
        }
    }

    @Test fun anExtractedDecisionStillGoesThroughTheValidatorAndThePolicy() {
        val session = session(schema.generate(ModelProtocol.OPENAI, policy))
        val repair = session.evaluate(fenced(tool("shell_exec", """{"cmd":"ls"}"""))) as DecisionAttempt.Repair
        assertEquals("TOOL_DISABLED", repair.observation.string("error"))
        assertTrue(session.schemaIgnored)
        assertEquals(listOf(DecisionRejection.TOOL_DISABLED), session.rejections)
    }

    @Test fun degradedAndNativeFormatsNeverFlagAProvider() {
        val degraded = session(DecisionSchema.degraded(ModelProtocol.OPENAI))
        assertEquals(ParseMode.EXTRACTED, (degraded.evaluate(fenced(done())) as DecisionAttempt.Accepted).parseMode)
        assertFalse(degraded.schemaIgnored)
        // The text reply of a native tool round has no schema either: a fenced done is accepted, a printed tool call still is not.
        val native = session(DecisionSchema.native(ModelProtocol.OPENAI))
        assertEquals(ParseMode.EXTRACTED, (native.evaluate(fenced(done())) as DecisionAttempt.Accepted).parseMode)
        assertFalse(native.schemaIgnored)
        val printed = session(DecisionSchema.native(ModelProtocol.OPENAI))
            .evaluate(fenced(tool("report_progress", """{"message":"Working"}"""))) as DecisionAttempt.Repair
        assertTrue(printed.observation.string("hint")!!.startsWith("Use the native tool interface"))
    }

    @Test fun theRunnerUsesTheExtractedDecisionsJournalsThemAndTellsTheWorkbenchOnce() {
        val f = RunnerFixture()
        assertNotNull(f.format.responseSchemaJson)
        f.enqueue(fenced(tool("ui_dump")), fenced(done()))
        val run = f.start()
        assertEquals(RunState.COMPLETED, run.state)
        assertEquals(2, f.model.calls.size)
        assertEquals(listOf("ui_dump"), f.tools.executions.map { it.first.invocation.name })
        assertEquals(listOf("EXTRACTED", "EXTRACTED"), steps(f).map { it.string("parseMode") })
        assertEquals(listOf(false, false), steps(f).map { it.flag("degraded") })
        assertEquals(listOf(0L, 0L), steps(f).map { it.number("repairs") })
        assertEquals(listOf(en.string("schema_ignored")), progress(f))
        // The format is not switched: every request still carries the schema, and the fence never reaches the journal.
        assertTrue(f.contexts.all { it.format?.responseSchemaJson != null })
        assertFalse(f.journal(run).toString().contains("```"))
    }

    @Test fun aFencedDecisionWithInvalidArgumentsIsRepairedWithinTheSameAllowance() {
        val f = RunnerFixture()
        f.enqueue(fenced(tool("ui_dump", """{"unknown":1}""")), done())
        val run = f.start()
        assertEquals(RunState.COMPLETED, run.state); assertEquals(2, f.model.calls.size)
        val step = steps(f).single()
        assertEquals("STRICT", step.string("parseMode")); assertEquals(1L, step.number("repairs"))
        assertEquals(listOf("TOOL_ARGUMENTS_INVALID"), step.getAsJsonArray("rejections").map { it.asString })
        assertEquals("TOOL_ARGUMENTS_INVALID", f.contexts[1].repair?.string("error"))
        assertEquals(listOf(en.string("schema_ignored")), progress(f))
    }

    @Test fun aBareReplyEmitsNoProgressLine() {
        val f = RunnerFixture(); f.enqueue(done()); val run = f.start()
        assertEquals(RunState.COMPLETED, run.state)
        assertEquals("STRICT", steps(f).single().string("parseMode"))
        assertTrue(progress(f).isEmpty())
    }

    @Test fun anOnlineProviderThatIgnoresTheSchemaCompletesInOneCallWhileTheRequestKeepsTheSchema() {
        val scheduler = VirtualScheduler()
        val format = schema.generate(ModelProtocol.OPENAI, policy)
        val target = ModelTarget("provider", "profile:glm", ModelLocality.REMOTE, ModelProtocol.OPENAI, true, 64 * 1024, supportsStreaming = true)
        val broker = TestModelBroker().apply { script = { call -> call.started(); call.usage(); call.done(fenced(done())) } }
        val client = ModelClient(broker, target, policy, SchemaFallbacks(schema), scheduler)
        val compiler = ContextCompiler(PromptCatalog(F::asset, catalog), catalog, policy, target, format, ContextLimits(maximumBytes = 64 * 1024))
        val events = mutableListOf<RunEvent>()
        val queue = RunQueue(scheduler, catalog, policy, compiler, client, FakeTools()) { RunnerText(F.asset("runner/texts.json"), it) }
        val run = queue.submit(RunOptions("Verify the task", format, modelTimeoutMs = 3000), events::add).also { scheduler.drain() }
        assertEquals(RunState.COMPLETED, run.state)
        assertEquals(1, broker.calls.size)
        assertEquals(true, broker.calls.single().request.flag("structuredJson"))
        assertTrue(broker.calls.single().request.has("responseSchema"))
        assertEquals("EXTRACTED", events.first { it.type == "step" }.payload.getAsJsonObject("decision").string("parseMode"))
        assertEquals(en.string("schema_ignored"), events.single { it.type == "progress" }.payload.string("message"))
        assertEquals(1L, run.result!!.getAsJsonObject("usage").number("modelCalls"))
    }
}
