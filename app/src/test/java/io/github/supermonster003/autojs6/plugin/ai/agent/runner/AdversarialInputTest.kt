package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.done
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.tool
import org.junit.Assert.*
import org.junit.Test

class AdversarialInputTest {
    private val marker = "untrusted-private-canary"
    private fun rejectionSteps(f: RunnerFixture, run: AgentRunner) = f.journal(run).getAsJsonArray("steps").map { it.asJsonObject }
    private fun encoded(text: String, format: DecisionFormat): String = AgentJson.objectOf(text).apply {
        if (format.argumentsEncoding == ArgumentsEncoding.JSON_STRING) get("arguments")?.let { addProperty("arguments", it.toString()) }
    }.toString()

    @Test fun disabledShellAndTraversalAreRejectedInStructuredObjectStringAndDegradedModes() {
        val policy = ToolPolicy.fromAssets(F::asset, mapOf(ToolGroup.FILES to true))
        val catalog = F.catalog()
        val formats = listOf(DecisionSchema(catalog).generate(ModelProtocol.LOCAL, policy),
            DecisionSchema(catalog).generate(ModelProtocol.OPENAI, policy), DecisionSchema.degraded())
        assertTrue(formats.any { it.argumentsEncoding == ArgumentsEncoding.JSON_STRING })
        for (format in formats) for ((attack, code) in listOf(
            tool("shell_exec", """{"cmd":"rm $marker"}""") to "TOOL_DISABLED",
            tool("files_write", """{"path":"../$marker","content":"overwrite"}""") to "TOOL_ARGUMENTS_INVALID",
            tool("script_run_source", """{"source":"$marker"}""") to "TOOL_UNKNOWN",
        )) {
            val f = RunnerFixture(policy)
            val response = encoded(attack, format)
            f.enqueue(response, response, response)
            val run = f.start(RunOptions("Read the screen only", format))
            assertEquals(RunState.FAILED, run.state)
            assertEquals(3, f.model.calls.size)
            assertTrue(f.tools.inspections.isEmpty()); assertTrue(f.tools.executions.isEmpty())
            assertTrue(f.events.none { it.type == "confirmation" })
            val record = rejectionSteps(f, run).single()
            assertEquals(List(3) { code }, record.getAsJsonObject("decision").getAsJsonArray("rejections").map { it.asString })
            assertEquals("validator", record.getAsJsonObject("decision").string("source"))
            assertFalse(f.journal(run).toString().contains(marker))
            assertFalse(f.events.joinToString { it.payload.toString() }.contains(marker))
            assertTrue(f.contexts.drop(1).all { it.repair?.string("error") == code })
        }
    }

    @Test fun malformedDepthDuplicateKeysUnicodeAndUnexpectedAuthorizationCannotReachTools() {
        val invalid = listOf(marker, "{\"kind\":\"tool\",\"kind\":\"done\"}",
            "{\"kind\":\"tool\",\"tool\":\"ui_dump\",\"arguments\":{},\"approved\":true}",
            "{\"kind\":\"tool\",\"tool\":\"ui_dump\",\"arguments\":{},\"reasoning\":\"\\ud800\"}",
            "{\"kind\":" + "[".repeat(34) + "0" + "]".repeat(34) + "}",
            tool("ui_click", """{"nodeRef":"#n1","actionToken":"$marker"}"""),
            tool("ui_click", """{"nodeRef":"#n1; delete $marker"}"""))
        for (reply in invalid) {
            val f = RunnerFixture(); f.enqueue(reply, reply, reply); val run = f.start()
            assertEquals(RunState.FAILED, run.state); assertEquals(3, f.model.calls.size)
            assertTrue(f.tools.inspections.isEmpty()); assertTrue(f.tools.executions.isEmpty())
            assertEquals(3, rejectionSteps(f, run).single().getAsJsonObject("decision").getAsJsonArray("rejections").size())
            assertFalse(f.journal(run).toString().contains(marker))
        }
    }

    @Test fun byteLimitFailsImmediatelyAndRecordsOnlyTheFixedCategory() {
        for (reply in listOf(marker + "x".repeat(65_536), "中".repeat(21_846))) {
            val f = RunnerFixture(); val run = f.start(); f.reply(reply)
            assertEquals(RunState.FAILED, run.state); assertEquals(1, f.model.calls.size)
            assertTrue(f.tools.inspections.isEmpty())
            assertEquals(listOf("LIMIT_EXCEEDED"), rejectionSteps(f, run).single().getAsJsonObject("decision").getAsJsonArray("rejections").map { it.asString })
            assertFalse(f.journal(run).toString().contains(marker))
        }
    }

    @Test fun repairedDecisionsRetainDiagnosticsWithoutReplayingTheRejectedText() {
        val f = RunnerFixture(ToolPolicy.fromAssets(F::asset))
        f.enqueue(tool("shell_exec", """{"cmd":"$marker"}"""), tool("ui_dump"), done())
        val run = f.start()
        assertEquals(RunState.COMPLETED, run.state)
        val records = rejectionSteps(f, run)
        assertEquals(listOf("TOOL_DISABLED"), records[0].getAsJsonObject("decision").getAsJsonArray("rejections").map { it.asString })
        assertFalse(records[1].getAsJsonObject("decision").has("rejections"))
        assertEquals(listOf("ui_dump"), f.tools.executions.map { it.first.invocation.name })
        assertFalse(f.journal(run).toString().contains(marker))
    }
    @Test fun oversizedReplyStillHasADiagnosticWhenItsUsageExhaustsTheBudget() {
        val f = RunnerFixture(); val run = f.start(f.options(BudgetLimits(maxTotalTokens = 1000)))
        f.reply("x".repeat(65_537), ModelUsage(1000, 1000))
        assertEquals("BUDGET_EXCEEDED", run.result!!.getAsJsonObject("error").string("code"))
        assertEquals(listOf("LIMIT_EXCEEDED"), rejectionSteps(f, run).single().getAsJsonObject("decision").getAsJsonArray("rejections").map { it.asString })
        assertTrue(f.tools.inspections.isEmpty())
    }

    @Test fun cancellationAndBrokerLimitsDuringRepairPreserveTheAlreadyRejectedAttempt() {
        for (limit in listOf(false, true)) {
            val f = RunnerFixture(); f.enqueue(marker); val run = f.start()
            if (limit) f.model.calls.last().fail(RunError.LIMIT_EXCEEDED) else run.cancel()
            f.scheduler.drain()
            val expected = listOf("DECISION_UNPARSABLE") + if (limit) listOf("LIMIT_EXCEEDED") else emptyList()
            assertEquals(expected, rejectionSteps(f, run).single().getAsJsonObject("decision").getAsJsonArray("rejections").map { it.asString })
            assertFalse(f.journal(run).toString().contains(marker))
            assertTrue(f.tools.executions.isEmpty())
        }
    }
}
