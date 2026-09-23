package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class ActionToolsTest {
    private class Fixture(observe: Boolean = true) {
        val scheduler = VirtualScheduler()
        val observations = ObservationTools()
        val calls = mutableListOf<BridgeCall>()
        var sequence = 0
        var reply: (BridgeCall) -> PortResult<JsonElement>? = { call -> PortResult.Success(when (call.method) {
            "inspectNode" -> inspection()
            "dump" -> dump("hidden-${++sequence}")
            else -> true.json()
        }) }
        val pending = mutableListOf<(PortResult<JsonElement>) -> Unit>()
        val tools = ActionTools(scheduler, observations, { call, callback ->
            calls += call; pending += callback; reply(call)?.let(callback); Cancellation.NONE
        }, observe)
        fun invocation(name: String, args: String) = AgentJson.objectOf(args).let { ToolInvocation(name, it, ToolHandlers(F.catalog()).prepare(name, it, F.policy())) }
        fun prepare(name: String = "ui_click", args: String = """{"selector":{"text":"Go"}}"""): PortResult<PreparedTool> {
            var value: PortResult<PreparedTool>? = null
            tools.prepare(invocation(name, args), 10000) { value = it }; scheduler.drain()
            return checkNotNull(value)
        }
        fun execute(prepared: PreparedTool): PortResult<ToolReply> {
            var value: PortResult<ToolReply>? = null
            tools.execute(prepared, 10000) { value = it }; scheduler.drain(); scheduler.advance(3500)
            return checkNotNull(value)
        }
    }
    @Test fun inspectionSuppliesRiskAndBindsTheExecutedTargetWithoutChangingModelArguments() {
        val f = Fixture()
        val prepared = (f.prepare() as PortResult.Success).value
        assertEquals(listOf("inspectNode"), f.calls.map { it.method })
        assertEquals("Pay now", prepared.metadata.context.nodeText)
        assertFalse(prepared.invocation.arguments.toString().contains("actionToken"))
        f.execute(prepared)
        val target = f.calls.single { it.method == "click" }.args[0].asJsonObject
        assertEquals("bound", target.string("snapshotId")); assertEquals("private-token", target.string("actionToken"))
        assertFalse(target.has("text"))
    }
    @Test fun modelReferenceAlwaysUsesTheLastPublishedSnapshotRatherThanHiddenHostDumps() {
        val f = Fixture()
        f.observations.transform(f.invocation("ui_dump", "{}"), dump("published"))
        f.prepare(args = """{"nodeRef":"#n1"}""")
        assertEquals("published", f.calls.first().args[0].asJsonObject.string("snapshotId"))
        assertEquals("published", f.observations.nodes.resolve("#n1").snapshotId)
    }
    @Test fun staleReferencesFailBeforeAnyDispatchAndNeverFallBackToSelectors() {
        val f = Fixture()
        assertEquals(RunError.NODE_REF_STALE, (f.prepare(args = """{"nodeRef":"#n1"}""") as PortResult.Failure).error)
        assertTrue(f.calls.isEmpty())
    }
    @Test fun passwordMetadataMasksInputAndAppendStaysInsideTheHost() {
        val f = Fixture(false)
        f.reply = { PortResult.Success(inspection().apply { addProperty("password", true) }) }
        val prepared = (f.prepare("ui_set_text", """{"selector":{"id":"input"},"text":"secret","append":true}""") as PortResult.Success).value
        assertTrue(prepared.metadata.passwordField); assertEquals("", prepared.metadata.context.nodeText)
        assertEquals("***", ConfirmationGate(F.policy(), ConfirmationMode.DEFAULT).arguments(prepared.invocation.arguments, prepared.metadata).string("text"))
        f.reply = { PortResult.Success(true.json()) }; f.execute(prepared)
        val set = f.calls.last()
        assertEquals("setText", set.method); assertTrue(set.args[2].asJsonObject.flag("append")!!)
        assertEquals("secret", set.args[1].asString); assertFalse(f.calls.any { it.method == "findOne" })
    }
    @Test fun paymentAndUncertainTargetsRequireIndividualConfirmation() {
        val f = Fixture(false)
        val spec = F.catalog()["ui_click"]!!
        val policy = ToolPolicy.fromAssets(F::asset)
        val gate = ConfirmationGate(policy, ConfirmationMode.DEFAULT)
        val prepared = (f.prepare() as PortResult.Success).value
        val assessment = gate.assess(spec, prepared.metadata)
        assertTrue(assessment.required); assertFalse(assessment.allowRunScope)
        f.reply = { PortResult.Success(inspection().apply { addProperty("text", "Go"); addProperty("uncertain", true) }) }
        assertFalse(gate.assess(spec, (f.prepare() as PortResult.Success).value.metadata).allowRunScope)
    }
    @Test fun repeatedScrollStopsAtTheFirstFalseAndReportsActualAttempts() {
        val f = Fixture(false)
        val prepared = (f.prepare("ui_scroll", """{"selector":{"scrollable":true},"direction":"forward","times":5}""") as PortResult.Success).value
        var count = 0; f.reply = { PortResult.Success((++count < 2).json()) }
        val result = (f.execute(prepared) as PortResult.Success).value.result.asJsonObject
        assertFalse(result.flag("ok")!!); assertFalse(result.flag("actionResult")!!); assertEquals(2L, result.number("attempts")); assertEquals(2, count)
    }
    @Test fun clipboardVoidAcknowledgementSucceedsAndReadsPreserveText() {
        val f = Fixture(false)
        val prepared = (f.prepare("clipboard_set", """{"text":"hello"}""") as PortResult.Success).value
        f.reply = { PortResult.Success(JsonNull.INSTANCE) }
        assertTrue((f.execute(prepared) as PortResult.Success).value.result.asJsonObject.flag("ok")!!)
        val read = (f.prepare("clipboard_get", "{}") as PortResult.Success).value
        f.reply = { PortResult.Success("hello".json()) }
        assertEquals("hello", (f.execute(read) as PortResult.Success).value.result.asJsonObject.string("actionResult"))
    }
    @Test fun aPreparationCannotBeExecutedByAnotherTask() {
        val first = Fixture(); val second = Fixture()
        val prepared = (first.prepare() as PortResult.Success).value
        assertEquals(RunError.INVALID_REQUEST, (second.execute(prepared) as PortResult.Failure).error)
        assertTrue(second.calls.isEmpty())
    }
    @Test fun cancellingPendingInspectionDiscardsLateRepliesWithoutActing() {
        val f = Fixture(); f.reply = { null }
        var completed = false
        val handle = f.tools.prepare(f.invocation("ui_click", """{"selector":{"text":"Go"}}"""), 1000) { completed = true }
        handle.cancel(); f.pending.single()(PortResult.Success(inspection())); f.scheduler.advance(2000)
        assertFalse(completed); assertEquals(1, f.calls.size)
    }
    @Test fun inspectionHasAnIndependentDeadlineEvenIfTheHostNeverReplies() {
        val f = Fixture(); f.reply = { null }
        var result: PortResult<PreparedTool>? = null
        f.tools.prepare(f.invocation("ui_click", """{"selector":{"text":"Go"}}"""), 50) { result = it }
        f.scheduler.advance(50)
        assertEquals(RunError.BUDGET_EXCEEDED, (result as PortResult.Failure).error)
    }
    @Test fun readbackFailureKeepsTheAcknowledgedActionWithoutInventingWindowState() {
        val f = Fixture(); val prepared = (f.prepare("app_launch", """{"packageName":"example.app"}""") as PortResult.Success).value
        f.reply = { if (it.method == "dump") PortResult.Failure(RunError.A11Y_SERVICE_NOT_RUNNING) else PortResult.Success(true.json()) }
        val result = (f.execute(prepared) as PortResult.Success).value.result.asJsonObject
        assertTrue(result.flag("ok")!!); assertTrue(result["windowChanged"].isJsonNull)
        assertEquals(1, f.calls.count { it.method == "launchPackage" })
    }
    @Test fun hostWindowIdentityDistinguishesDialogsWithinTheSameActivity() {
        val first = CompactNodeText.parse(dump("one").apply { addProperty("windowIdentity", "dialog1") })
        val second = CompactNodeText.parse(dump("two").apply { addProperty("windowIdentity", "dialog2") })
        assertNotEquals(first.window, second.window)
    }
    companion object {
        fun inspection() = jsonObject("target" to jsonObject("nodeRef" to "#n1".json(), "snapshotId" to "bound".json(), "actionToken" to "private-token".json()),
            "text" to "Pay now".json(), "desc" to "".json(), "packageName" to "example".json(), "password" to false.json(), "enabled" to true.json(), "uncertain" to false.json())
    }
}
