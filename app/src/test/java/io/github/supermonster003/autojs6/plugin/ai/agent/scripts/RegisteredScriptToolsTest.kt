package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class RegisteredScriptToolsTest {
    private val catalog = F.catalog()
    private val policy = ToolPolicy()
    private fun invocation(args: String = """{"id":"clean-downloads","parameters":{}}"""): ToolInvocation {
        val values = AgentJson.objectOf(args)
        return ToolInvocation("script_run", values, ToolHandlers(catalog).prepare("script_run", values, policy))
    }
    private class Source : ScriptCatalogSource {
        val calls = mutableListOf<Pair<BridgeCall, Pending<JsonElement>>>()
        override fun load(call: BridgeCall, callback: (PortResult<JsonElement>) -> Unit): Cancellation {
            val pending = Pending(callback); calls += call to pending; return pending.cancellation
        }
    }
    private fun tools(source: Source, delegate: RunTools = FakeTools(), allowed: Boolean = true, roots: Set<String> = emptySet()) =
        RegisteredScriptTools(ScriptCatalogClient { 0 }, roots, source, delegate, DecisionValidator(catalog), allowed) { 0 }

    @Test fun idIsResolvedInsideTaskRootsAndFreshManifestSuppliesDefaultsAndRisk() {
        val source = Source(); val tools = tools(source, roots = setOf("/storage/emulated/0/extra")); val invocation = invocation()
        var result: PortResult<PreparedTool>? = null
        tools.prepare(invocation, 5000) { result = it }
        assertEquals(jsonArray(".".json(), "/storage/emulated/0/extra".json()), source.calls[0].first.args[0].asJsonObject["roots"])
        val old = ScriptFixtures.entry().apply { addProperty("risk", "readonly"); addProperty("confirm", "never") }
        source.calls[0].second.succeed(jsonArray(old))
        assertEquals("readManifest", source.calls[1].first.method)
        assertEquals(old["path"], source.calls[1].first.args[0])
        val fresh = ScriptFixtures.entry().apply { addProperty("risk", "sensitive"); getAsJsonObject("parameters").getAsJsonObject("properties").getAsJsonObject("days").addProperty("default", 7) }
        source.calls[1].second.succeed(fresh)
        val prepared = (result as PortResult.Success).value
        assertSame(invocation, prepared.invocation); assertEquals(RiskLevel.SENSITIVE, prepared.metadata.context.registeredScriptRisk)
        assertTrue(prepared.metadata.forceConfirmation); assertEquals(7L, prepared.metadata.script!!.parameters.number("days"))
        assertEquals(60000L, prepared.metadata.scriptTimeoutMs)
        fresh.addProperty("description", "changed"); prepared.metadata.script.parameters.addProperty("days", 99)
        assertEquals(7L, prepared.metadata.script.parameters.number("days"))
        val gate = ConfirmationGate(policy, ConfirmationMode.DEFAULT)
        assertTrue(gate.description(catalog["script_run"]!!, prepared.metadata, "en").contains("Remove old installers"))
        assertEquals(listOf("days" to "7"), ScriptConfirmation.rows(gate.arguments(invocation.arguments, prepared.metadata)))
        assertFalse(gate.assess(catalog["script_run"]!!, prepared.metadata).allowRunScope)
    }
    @Test fun unauthorizedUnknownAndAmbiguousIdsNeverReadAManifest() {
        val denied = Source(); var result: PortResult<PreparedTool>? = null
        tools(denied, allowed = false).prepare(invocation(), 5000) { result = it }
        assertEquals(RunError.CAPABILITY_DENIED, (result as PortResult.Failure).error); assertTrue(denied.calls.isEmpty())
        for (entries in listOf(JsonArray(), jsonArray(ScriptFixtures.entry(), ScriptFixtures.entry().apply { addProperty("path", "/storage/emulated/0/other.js") }))) {
            val source = Source(); tools(source).prepare(invocation(), 5000) { result = it }
            source.calls[0].second.succeed(entries)
            assertEquals(RunError.SCRIPT_NOT_REGISTERED, (result as PortResult.Failure).error); assertEquals(1, source.calls.size)
        }
    }
    @Test fun changedIdentityOrInvalidSchemaCannotReachConfirmation() {
        for (change in listOf<(JsonObject) -> Unit>({ it.addProperty("id", "replaced") }, { it.addProperty("path", "/storage/emulated/0/other.js") },
            { it.getAsJsonObject("parameters").addProperty("additionalProperties", true) })) {
            val source = Source(); var result: PortResult<PreparedTool>? = null
            tools(source).prepare(invocation(), 5000) { result = it }
            source.calls[0].second.succeed(jsonArray(ScriptFixtures.entry()))
            source.calls[1].second.succeed(ScriptFixtures.entry().also(change))
            assertEquals(RunError.SCRIPT_NOT_REGISTERED, (result as PortResult.Failure).error)
        }
    }
    @Test fun missingParametersAreTypedFeedbackAndPreparationHasNoExecutionSideEffects() {
        val source = Source(); val delegate = FakeTools(); var result: PortResult<PreparedTool>? = null
        tools(source, delegate).prepare(invocation(), 5000) { result = it }
        val row = ScriptFixtures.entry().apply { add("parameters", AgentJson.objectOf("""{"type":"object","properties":{"address":{"type":"string"}},"required":["address"]}""")) }
        source.calls[0].second.succeed(jsonArray(row)); source.calls[1].second.succeed(row)
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, (result as PortResult.Failure).error)
        assertTrue((result as PortResult.Failure).scriptParameters!!.observation().contains("SCRIPT_PARAMETERS_MISSING"))
        assertTrue(delegate.inspections.isEmpty()); assertTrue(delegate.executions.isEmpty())
    }
    @Test fun cancellationAndDuplicateCallbacksCannotPrepareTwice() {
        for (cancelAfterCatalog in listOf(false, true)) {
            val source = Source(); var completions = 0
            val handle = tools(source).prepare(invocation(), 5000) { completions++ }
            if (cancelAfterCatalog) source.calls[0].second.succeed(jsonArray(ScriptFixtures.entry()))
            val last = source.calls.last().second
            handle.cancel(); last.succeed(if (cancelAfterCatalog) ScriptFixtures.entry() else jsonArray(ScriptFixtures.entry()))
            assertEquals(0, completions); assertEquals(1, last.cancellations)
        }
        val source = Source(); var completions = 0
        tools(source).prepare(invocation(), 5000) { completions++ }
        repeat(2) { source.calls[0].second.succeed(jsonArray(ScriptFixtures.entry())) }
        assertEquals(2, source.calls.size)
        repeat(2) { source.calls[1].second.succeed(ScriptFixtures.entry()) }
        assertEquals(1, completions)
    }
    @Test fun synchronousCatalogCompletionCannotReplaceThePendingManifestCancellation() {
        var cancelled = 0; var completed = 0
        val source = ScriptCatalogSource { call, reply ->
            if (call.method == "listScripts") { reply(PortResult.Success(jsonArray(ScriptFixtures.entry()))); Cancellation.NONE }
            else Cancellation { cancelled++ }
        }
        val adapter = RegisteredScriptTools(ScriptCatalogClient { 0 }, emptySet(), source, FakeTools(), DecisionValidator(catalog), true) { 0 }
        val handle = adapter.prepare(invocation(), 5000) { completed++ }
        handle.cancel(); assertEquals(1, cancelled); assertEquals(0, completed)
    }
    @Test fun confirmationRowsPreserveTypesFullValuesAndDeterministicOrder() {
        val args = AgentJson.objectOf("""{"parameters":{"z":false,"a":"<b>line\nquoted\"</b>","n":1.5}}""")
        assertEquals(listOf("a" to "\"<b>line\\nquoted\\\"</b>\"", "n" to "1.5", "z" to "false"), ScriptConfirmation.rows(args))
        args.getAsJsonObject("parameters").addProperty("a", "x".repeat(8000))
        assertEquals(8002, ScriptConfirmation.rows(args).first().second.length)
    }
}
