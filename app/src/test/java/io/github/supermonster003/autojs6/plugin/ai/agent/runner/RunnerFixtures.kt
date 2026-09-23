package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.util.PriorityQueue

internal class VirtualScheduler : RunScheduler {
    private class Task(val time: Long, val order: Long, val action: () -> Unit) { var cancelled = false }
    private val tasks = PriorityQueue(compareBy<Task>({ it.time }, { it.order }))
    private var order = 0L
    private var time = 0L
    override fun nowMs() = time
    override fun execute(action: () -> Unit) { schedule(0, action) }
    override fun schedule(delayMs: Long, action: () -> Unit): Cancellation {
        require(delayMs >= 0)
        val task = Task(time + delayMs, ++order, action); tasks.add(task)
        return Cancellation { task.cancelled = true }
    }
    fun drain() = advance(0)
    fun advance(delta: Long) {
        val target = time + delta
        var count = 0
        while (tasks.peek()?.time?.let { it <= target } == true) {
            check(++count < 10_000) { "Test scheduler failed to quiesce" }
            val task = tasks.remove(); time = task.time
            if (!task.cancelled) task.action()
        }
        time = target
    }
}

internal class Pending<T>(val callback: (PortResult<T>) -> Unit) {
    var cancellations = 0
    val cancellation = Cancellation { cancellations++ }
    fun succeed(value: T) = callback(PortResult.Success(value))
    fun fail(error: RunError) = callback(PortResult.Failure(error))
}

internal class FakeModel : RunModel {
    val calls = mutableListOf<Pending<ModelReply>>()
    val inputs = mutableListOf<ModelInput>()
    val timeouts = mutableListOf<Long>()
    val outputLimits = mutableListOf<Int>()
    val replies = ArrayDeque<ModelReply>()
    var onGenerate: (() -> Unit)? = null
    override fun generate(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long, callback: (PortResult<ModelReply>) -> Unit): Cancellation {
        val call = Pending(callback); calls += call; inputs += input; timeouts += timeoutMs; outputLimits += maximumOutputTokens
        if (replies.isNotEmpty()) call.succeed(replies.removeFirst())
        onGenerate?.invoke()
        return call.cancellation
    }
}

internal class FakeTools : RunTools {
    val inspections = mutableListOf<Pair<ToolInvocation, Pending<PreparedTool>>>()
    val executions = mutableListOf<Pair<PreparedTool, Pending<ToolReply>>>()
    val timeouts = mutableListOf<Long>()
    var autoPrepare = true
    var autoExecute = true
    var metadata: (ToolInvocation) -> ToolMetadata = { ToolMetadata() }
    var action: (PreparedTool) -> JsonElement = { true.json() }
    override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
        val call = Pending(callback); inspections += invocation to call
        if (autoPrepare) call.succeed(PreparedTool(invocation, metadata(invocation)))
        return call.cancellation
    }
    override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
        val call = Pending(callback); executions += prepared to call; timeouts += timeoutMs
        if (autoExecute) call.succeed(ToolReply(action(prepared)))
        return call.cancellation
    }
}

internal class RunnerFixture(policy: ToolPolicy = ToolPolicy.fromAssets(F::asset, ToolGroup.entries.associateWith { true }, true)) {
    val scheduler = VirtualScheduler()
    val catalog = F.catalog()
    val format = DecisionSchema(catalog).generate(ModelProtocol.LOCAL, policy)
    val model = FakeModel()
    val tools = FakeTools()
    val contexts = mutableListOf<RunContext>()
    val events = mutableListOf<RunEvent>()
    var onEvent: ((RunEvent) -> Unit)? = null
    val queue = RunQueue(scheduler, catalog, policy, RunContextCompiler { context ->
        contexts += context
        ModelInput(jsonArray(jsonObject("role" to "user".json(), "content" to jsonObject("goal" to context.goal.json(),
            "observation" to (context.observation ?: "").json(), "remaining" to context.remainingBudget,
            "repair" to (context.repair ?: JsonNull.INSTANCE)).toString().json())))
    }, model, tools) { RunnerText(F.asset("runner/texts.json"), it) }
    fun options(limits: BudgetLimits = BudgetLimits(), mode: ConfirmationMode = ConfirmationMode.DEFAULT, modelTimeout: Long = 300_000) =
        RunOptions("Test the task runner", format, limits = limits, confirmationMode = mode, modelTimeoutMs = modelTimeout)
    fun submit(options: RunOptions = options()): AgentRunner = queue.submit(options) { event -> events += event; onEvent?.invoke(event) }
    fun start(options: RunOptions = options()) = submit(options).also { scheduler.drain() }
    fun reply(text: String, usage: ModelUsage? = ModelUsage(20, 10, 30)) { model.calls.last().succeed(ModelReply(text, usage)); scheduler.drain() }
    fun enqueue(vararg decisions: String) { decisions.forEach { model.replies += ModelReply(it, ModelUsage(20, 10, 30)) } }
    fun request(type: String) = events.last { it.type == type }.payload.string("requestId")!!
    fun journal(run: AgentRunner): JsonObject { var result: JsonObject? = null; run.readJournal { result = it }; scheduler.drain(); return result!! }
    companion object {
        fun tool(name: String, arguments: String = "{}", reasoning: String? = null) = jsonObject("kind" to "tool".json(), "tool" to name.json(),
            "arguments" to AgentJson.objectOf(arguments)).apply { reasoning?.let { addProperty("reasoning", it) } }.toString()
        fun done(status: String = "completed", summary: String = "Verified", evidence: List<String> = listOf("Observed final state"), unfinished: List<String> = emptyList(), orderStatus: String? = null) =
            jsonObject("kind" to "done".json(), "done" to jsonObject("status" to status.json(), "summary" to summary.json(),
                "evidence" to JsonArray().apply { evidence.forEach(::add) },
                "unfinished" to JsonArray().apply { unfinished.forEach(::add) }).apply { orderStatus?.let { addProperty("orderStatus", it) } }).toString()
        fun ask(kind: String = "text", memoryKey: String? = null) = jsonObject("kind" to "ask".json(),
            "ask" to jsonObject("kind" to kind.json(), "question" to "Which value?".json()).apply {
                if (kind == "choice") add("choices", jsonArray("one".json(), "two".json()))
                memoryKey?.let { addProperty("memoryKey", it) }
            }).toString()
    }
}
