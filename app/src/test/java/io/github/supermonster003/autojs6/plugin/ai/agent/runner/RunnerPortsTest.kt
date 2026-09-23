package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunnerFixture.Companion.done
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RunnerPortsTest {
    @Test fun invocationArgumentsAndEveryPreparedPlanAreImmutableAcrossAdapters() {
        val call = BridgeCall("a", "b", jsonArray("original".json()), mutableListOf("test"))
        val objectValue = jsonObject("text" to "original".json())
        val plans = listOf(ToolPlan.Call(call), ToolPlan.Poll(call, "appear", 10), ToolPlan.Repeat(call, 2),
            ToolPlan.AppendText(objectValue, "original"), ToolPlan.RegisteredScript(call, call), ToolPlan.Local("test", objectValue))
        for (plan in plans) {
            val invocation = ToolInvocation("sample", objectValue, plan)
            fun mutate(value: ToolPlan) { when (value) {
                is ToolPlan.Call -> value.request.args.add("changed")
                is ToolPlan.Poll -> value.request.args.add("changed")
                is ToolPlan.Repeat -> value.request.args.add("changed")
                is ToolPlan.AppendText -> value.target.addProperty("text", "changed")
                is ToolPlan.RegisteredScript -> value.execution.args.add("changed")
                is ToolPlan.Local -> value.arguments.addProperty("text", "changed")
            } }
            mutate(invocation.plan)
            assertFalse(invocation.plan.toString().contains("changed"))
            invocation.arguments.addProperty("text", "changed")
            assertEquals("original", invocation.arguments.string("text"))
        }
    }
    @Test fun repliesInputsAndEventsTakeSnapshotsAndAvoidPrivateToStringData() {
        val data = jsonObject("private" to "private-value".json())
        val reply = ToolReply(data); val event = RunEvent("run", 1, "step", data)
        val input = ModelInput(jsonArray(data)); data.addProperty("private", "changed")
        assertEquals("private-value", reply.result.asJsonObject.string("private"))
        assertEquals("private-value", event.payload.string("private"))
        assertEquals("private-value", input.messages[0].asJsonObject.string("private"))
        reply.result.asJsonObject.addProperty("private", "changed")
        assertEquals("private-value", reply.result.asJsonObject.string("private"))
        for (value in listOf(reply, event, input, ModelReply("private-value"), RunContext("private-value", emptyList(), null, null, jsonObject()))) {
            assertFalse(value.toString().contains("private-value"))
        }
    }
    @Test fun localizationsCoverTenLanguagesWithFallbacks() {
        val source = F.asset("runner/texts.json"); val rows = AgentJson.objectOf(source)
        assertEquals(10, rows.size())
        rows.entrySet().forEach { (locale, _) ->
            for (error in listOf(RunError.CANCELLED, RunError.HOST_UNAVAILABLE, RunError.BUDGET_EXCEEDED, RunError.DECISION_UNPARSABLE, RunError.MODEL_FAILED)) {
                assertTrue(RunnerText(source, locale).terminal(error).isNotBlank())
            }
        }
        assertEquals(RunnerText(source, "en").terminal(RunError.CANCELLED), RunnerText(source, "xx").terminal(RunError.CANCELLED))
        assertEquals(RunnerText(source, "zh-Hant-TW").terminal(RunError.CANCELLED), RunnerText(source, "zh-TW").terminal(RunError.CANCELLED))
    }
    @Test fun TurkishDefaultLocaleDoesNotChangeProtocolStates() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val f = RunnerFixture(); f.enqueue(done("failed")); val run = f.start()
            assertEquals(RunState.FAILED, run.state); assertEquals("failed", run.result!!.string("status"))
        } finally { Locale.setDefault(old) }
    }
    @Test fun serialSchedulerQueuesNestedWorkAndCancelsTimers() {
        SerialRunScheduler().use { scheduler ->
            val values = mutableListOf<Int>(); val complete = CountDownLatch(1)
            scheduler.execute {
                values += 1
                val timer = scheduler.schedule(10) { values += 99 }
                timer.cancel()
                scheduler.execute { values += 3; complete.countDown() }
                values += 2
            }
            assertTrue(complete.await(5, TimeUnit.SECONDS)); assertEquals(listOf(1, 2, 3), values)
        }
    }
    @Test fun concurrentCancelAndCompletionHaveOneConsistentWinner() {
        val catalog = F.catalog(); val policy = F.policy()
        val format = DecisionSchema(catalog).generate(ModelProtocol.LOCAL, policy)
        val text = RunnerText(F.asset("runner/texts.json"), "en")
        repeat(40) {
            val started = CountDownLatch(1); val terminal = CountDownLatch(1); val go = CountDownLatch(1)
            val accepted = AtomicBoolean(); val terminals = AtomicInteger()
            lateinit var respond: (PortResult<ModelReply>) -> Unit
            val model = object : RunModel {
                override fun generate(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long, callback: (PortResult<ModelReply>) -> Unit): Cancellation {
                    respond = callback; started.countDown(); return Cancellation.NONE
                }
            }
            lateinit var run: AgentRunner
            SerialRunScheduler().use { scheduler ->
                val queue = RunQueue(scheduler, catalog, policy, RunContextCompiler { ModelInput(jsonArray()) }, model, FakeTools()) { text }
                run = queue.submit(RunOptions("Race test", format)) {
                    if (it.type == "done") { terminals.incrementAndGet(); terminal.countDown() }
                }
                assertTrue(started.await(5, TimeUnit.SECONDS))
                val cancel = Thread { go.await(); accepted.set(run.cancel()) }.apply { start() }
                val complete = Thread { go.await(); respond(PortResult.Success(ModelReply(done()))) }.apply { start() }
                go.countDown(); cancel.join(5000); complete.join(5000)
                assertFalse(cancel.isAlive); assertFalse(complete.isAlive)
                assertTrue(terminal.await(5, TimeUnit.SECONDS))
                assertEquals(if (accepted.get()) RunState.CANCELLED else RunState.COMPLETED, run.state)
            }
            respond(PortResult.Success(ModelReply(done())))
            var status: ReplyStatus? = null
            run.confirm("late", true) { status = it }; assertEquals(ReplyStatus.NOT_WAITING, status)
            run.respond("late", true.json()) { status = it }; assertEquals(ReplyStatus.NOT_WAITING, status)
            var journalStatus: String? = null
            run.readJournal { journalStatus = it.getAsJsonObject("result").string("status") }
            assertEquals(run.state.wire, journalStatus); assertEquals(1, terminals.get())
        }
    }
}
