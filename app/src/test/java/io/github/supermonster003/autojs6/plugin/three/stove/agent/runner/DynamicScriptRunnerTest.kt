package io.github.supermonster003.autojs6.plugin.three.stove.agent.runner

import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.RunnerFixture.Companion.tool
import org.junit.Assert.*
import org.junit.Test

class DynamicScriptRunnerTest {
    private val source = "// " + "source-audit-".repeat(510) + "\nai.agent.result(42);"
    private fun decision(code: String = source) = tool("script_run_source", jsonObject("source" to code.json(), "timeoutMs" to 60000.json()).toString())
    @Test fun onceApprovalRequiresTheNextSourceToBeReviewedAndFullTextSurvivesHistory() {
        val f = RunnerFixture(); f.tools.metadata = { ToolMetadata(scriptTimeoutMs = 60000) }
        val run = f.start(); f.reply(decision())
        val pending = f.events.last { it.type == "confirmation" }.payload
        assertEquals(source, pending.getAsJsonObject("arguments").string("source"))
        assertTrue(pending.flag("allowRunScope")!!); assertTrue(StepJournal.bytes(pending) < 32768)
        assertTrue(f.tools.executions.isEmpty())
        run.confirm(f.request("confirmation"), true); f.scheduler.drain()
        assertEquals(60000L, f.tools.timeouts.single())
        val step = f.journal(run).getAsJsonArray("steps").single().asJsonObject
        assertEquals(source, step.getAsJsonObject("arguments").string("source"))
        assertEquals("STRICT", step.getAsJsonObject("decision").string("parseMode"))
        assertTrue(StepJournal.bytes(step) < 32768)
        f.reply(decision("ai.agent.result('different source');"))
        assertEquals(2, f.events.count { it.type == "confirmation" }); assertEquals(1, f.tools.executions.size)
        run.confirm(f.request("confirmation"), false); f.scheduler.drain()
        assertEquals(1, f.tools.executions.size)
        assertEquals("USER_DENIED", f.journal(run).getAsJsonArray("steps").last().asJsonObject.string("error"))
    }
    @Test fun sourceStepPreservesExactEscapedTextWhenOtherMetadataMustShrink() {
        val code = "// " + "\\\n".repeat(1700)
        val arguments = jsonObject("source" to code.json(), "timeoutMs" to 300000.json())
        val journal = StepJournal()
        val entry = journal.append(StepRecord(1, "tool", jsonObject("reasoning" to "\u0001".repeat(4000).json()),
            tool = "script_run_source", arguments = arguments, observation = "\u0001".repeat(4000)))
        assertEquals(arguments, entry.getAsJsonObject("arguments"))
        assertTrue(StepJournal.bytes(entry) <= 24 * 1024)
        assertEquals(code, journal.history().single().getAsJsonObject("arguments").string("source"))
    }
    @Test fun dynamicRiskCannotBeLoweredButExplicitSessionApprovalCanBeRemembered() {
        val policy = ToolPolicy(riskOverrides = mapOf("script_run_source" to RiskLevel.READ_ONLY))
        val gate = ConfirmationGate(policy, ConfirmationMode.DEFAULT)
        val spec = F.catalog()["script_run_source"]!!
        val assessment = gate.assess(spec, ToolMetadata())
        assertEquals(RiskLevel.SENSITIVE, assessment.risk); assertTrue(assessment.required)
        assertTrue(gate.allow(assessment, ConfirmationScope.ONCE))
        assertTrue(gate.assess(spec, ToolMetadata()).required)
        assertTrue(gate.allow(assessment, ConfirmationScope.RUN))
        assertFalse(gate.assess(spec, ToolMetadata()).required)
    }
    @Test fun sessionApprovalCoversChangedSourceAndExpiresWithTheTask() {
        val f = RunnerFixture(); val run = f.start(); f.reply(decision())
        assertFalse(f.contexts.first().guidance.has("confirmationMode"))
        run.confirm(f.request("confirmation"), true, ConfirmationScope.RUN); f.scheduler.drain()
        f.reply(decision("ai.agent.result(43);"))
        assertEquals(1, f.events.count { it.type == "confirmation" }); assertEquals(2, f.tools.executions.size)
        run.cancel(); f.scheduler.drain()
        f.start(); f.reply(decision())
        assertEquals(2, f.events.count { it.type == "confirmation" }); assertEquals(2, f.tools.executions.size)
    }
    @Test fun fullAccessExecutesSensitiveSourcesWithoutConfirmationButKeepsJournalAndCancellation() {
        val f = RunnerFixture(); f.tools.metadata = { ToolMetadata(forceConfirmation = true) }
        val run = f.start(f.options(mode = ConfirmationMode.FULL_ACCESS))
        f.reply(decision()); f.reply(decision("ai.agent.result(44);"))
        assertTrue(f.events.none { it.type == "confirmation" }); assertEquals(2, f.tools.executions.size)
        // The model learns the user's policy from runtime guidance; the default policy stays implicit.
        assertEquals("full_access", f.contexts.first().guidance.string("confirmationMode"))
        assertEquals("auto", f.journal(run).getAsJsonArray("steps")[0].asJsonObject.string("confirmation"))
        assertEquals(source, f.journal(run).getAsJsonArray("steps")[0].asJsonObject.getAsJsonObject("arguments").string("source"))
        run.cancel(); f.scheduler.drain(); assertEquals(RunState.CANCELLED, run.state)
    }
    @Test fun laterPasswordProtectionMarksSourceAsUnsuitableForExactSourceExport() {
        val journal = StepJournal()
        journal.append(StepRecord(1, "tool", jsonObject(), tool = "script_run_source",
            arguments = jsonObject("source" to "console.log('private-password');".json())))
        journal.protectText("private-password")
        val record = journal.history().single()
        assertEquals(true, record.flag("sourceRedacted"))
        assertFalse(record.toString().contains("private-password"))
    }
    @Test fun expandingRedactionIsBoundedAndNeverDropsNativeAttribution() {
        val source = "// " + "a".repeat(8180)
        val arguments = jsonObject("source" to source.json())
        val decision = jsonObject("kind" to "tool".json(), "tool" to "script_run_source".json(), "arguments" to arguments,
            "parseMode" to "NATIVE_TOOL".json(), "repairs" to 1.json(), "degraded" to false.json())
        for (protectBefore in listOf(true, false)) {
            val journal = StepJournal()
            if (protectBefore) journal.protectText("a")
            journal.append(StepRecord(1, "tool", decision, tool = "script_run_source", arguments = arguments))
            if (!protectBefore) journal.protectText("a")
            val step = journal.history().single()
            assertEquals(true, step.flag("sourceRedacted"))
            assertTrue(StepJournal.bytes(step) <= 24 * 1024)
            val metadata = step.getAsJsonObject("decision")
            assertEquals("tool", metadata.string("kind")); assertEquals("script_run_source", metadata.string("tool"))
            assertEquals("NATIVE_TOOL", metadata.string("parseMode")); assertEquals(1L, metadata.number("repairs"))
            assertEquals(false, metadata.flag("degraded"))
        }
    }
    @Test fun cancelWhileWaitingNeverExecutesSource() {
        val f = RunnerFixture(); val run = f.start(); f.reply(decision())
        run.cancel(); f.scheduler.drain()
        run.confirm(f.request("confirmation"), true); f.scheduler.drain()
        assertTrue(f.tools.executions.isEmpty()); assertEquals(RunState.CANCELLED, run.state)
        assertEquals(source, f.journal(run).getAsJsonArray("steps").single().asJsonObject.getAsJsonObject("arguments").string("source"))
    }
    @Test fun sourceContainingProtectedPasswordCannotExecuteBehindARedactedApproval() {
        val f = RunnerFixture()
        f.tools.metadata = { ToolMetadata(passwordField = it.name == "ui_set_text") }
        val run = f.start()
        f.reply(tool("ui_set_text", """{"selector":{"id":"test:id/input"},"text":"protected-password"}"""))
        f.reply(decision("console.log('protected-password');"))
        assertTrue(f.events.none { it.type == "confirmation" })
        assertEquals(1, f.tools.executions.size)
        val step = f.journal(run).getAsJsonArray("steps").last().asJsonObject
        assertEquals("TOOL_ARGUMENTS_INVALID", step.string("error"))
        assertEquals(true, step.flag("sourceRedacted")); assertFalse(step.toString().contains("protected-password"))
    }
}
