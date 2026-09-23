package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/** A task-owned action adapter. Host inspection tokens never enter the model or step journal. */
class ActionTools(private val scheduler: RunScheduler, private val observations: ObservationTools,
                  private val dispatch: (BridgeCall, (PortResult<JsonElement>) -> Unit) -> Cancellation,
                  private val canObserve: Boolean) {
    private class Prepared(val owner: ActionTools, val call: BridgeCall, val times: Int)

    fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
        val operation = Operation<PreparedTool>(timeoutMs, callback)
        val plan = invocation.plan
        val call = when (plan) {
            is ToolPlan.Call -> plan.request
            is ToolPlan.Repeat -> plan.request
            is ToolPlan.AppendText -> ToolHandlers.bridge("accessibility.setText", jsonArray(plan.target, plan.text.json(), jsonObject("append" to true.json())))
            else -> { operation.finish(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID)); return operation }
        }
        val times = (plan as? ToolPlan.Repeat)?.times ?: 1
        if (invocation.name !in NODE_ACTIONS) {
            operation.finish(PortResult.Success(PreparedTool(invocation, ToolMetadata(forceConfirmation = invocation.name in GESTURES), Prepared(this, call, times))))
        } else {
            val target = call.args[0].asJsonObject.deepCopy()
            try {
                target.string("nodeRef")?.let { ref ->
                    // Never interpret an implicit model ref against a hidden inspection/dump on the host.
                    val resolved = observations.nodes.resolve(ref, target.string("snapshotId"))
                    target.addProperty("snapshotId", resolved.snapshotId)
                }
            } catch (_: NodeRefRegistry.Stale) { operation.finish(PortResult.Failure(RunError.NODE_REF_STALE)); return operation }
            operation.call(ToolHandlers.bridge("accessibility.inspectNode", jsonArray(target, call.method.json()))) { answer ->
                when (answer) {
                    is PortResult.Failure -> operation.finish(answer)
                    is PortResult.Success -> try {
                        val value = AgentJson.parse(answer.value.toString(), 24 * 1024).asJsonObject
                        val bound = value.getAsJsonObject("target")
                        require(bound.keySet() == setOf("nodeRef", "snapshotId", "actionToken"))
                        require(bound.string("nodeRef") == "#n1")
                        for (key in listOf("snapshotId", "actionToken")) require(bound.string(key)?.let { it.isNotBlank() && it.length <= 128 } == true)
                        val text = requireNotNull(value.string("text")); val desc = requireNotNull(value.string("desc"))
                        val pkg = requireNotNull(value.string("packageName")); val password = requireNotNull(value.flag("password"))
                        val enabled = requireNotNull(value.flag("enabled")); val uncertain = requireNotNull(value.flag("uncertain"))
                        require(text.toByteArray().size <= 4096 && desc.toByteArray().size <= 4096 && pkg.length <= 256)
                        val args = call.args.deepCopy().apply { set(0, bound.deepCopy()) }
                        operation.finish(PortResult.Success(PreparedTool(invocation,
                            ToolMetadata(RiskContext(if (password) "" else text, if (password) "" else desc, pkg),
                                passwordField = password, forceConfirmation = uncertain || !enabled), Prepared(this, call.copy(args = args), times))))
                    } catch (_: Exception) { operation.finish(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID)) }
                }
            }
        }
        return operation
    }

    fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
        val operation = Operation<ToolReply>(timeoutMs, callback)
        val action = prepared.opaqueContext as? Prepared
        if (action == null || action.owner !== this) { operation.finish(PortResult.Failure(RunError.INVALID_REQUEST)); return operation }
        var before: CompactNodeText.Snapshot? = null
        fun complete(value: JsonElement, count: Int) {
            val ok = if (prepared.invocation.name == "clipboard_get") true else value.isJsonPrimitive && value.asJsonPrimitive.isBoolean && value.asBoolean
            val result = jsonObject("ok" to ok.json(), "actionResult" to value, "windowChanged" to JsonNull.INSTANCE, "attempts" to count.json())
            operation.finish(PortResult.Success(ToolReply(result)))
        }
        fun perform() {
            var attempts = 0
            fun next() { operation.call(action.call) { answer -> when (answer) {
                is PortResult.Failure -> operation.finish(answer)
                is PortResult.Success -> {
                    attempts++
                    val value = if (prepared.invocation.name == "clipboard_set" && answer.value.isJsonNull) true.json() else answer.value
                    if (prepared.invocation.name == "clipboard_get") { complete(value, attempts); return@call }
                    if (!value.isJsonPrimitive || !value.asJsonPrimitive.isBoolean) { operation.finish(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID)); return@call }
                    if (value.asBoolean && attempts < action.times) next() else completeAction(operation, before, value, attempts)
                }
            } } }
            next()
        }
        if (prepared.invocation.name == "clipboard_get") perform()
        else readSnapshot(operation) { snapshot -> before = snapshot; observations.actionBaseline(snapshot); perform() }
        return operation
    }

    private fun completeAction(operation: Operation<ToolReply>, before: CompactNodeText.Snapshot?, result: JsonElement, attempts: Int) {
        val ok = result.asBoolean
        val response = jsonObject("ok" to ok.json(), "actionResult" to result, "windowChanged" to JsonNull.INSTANCE, "attempts" to attempts.json())
        val startedAt = scheduler.nowMs()
        val stability = ScreenStability(startedAt)
        var latest: CompactNodeText.Snapshot? = null
        fun complete(after: CompactNodeText.Snapshot?, stable: Boolean) {
            response.add("stability", jsonObject("stable" to stable.json(), "observed" to (after != null).json(),
                "waitedMs" to (scheduler.nowMs() - startedAt).json(), "partial" to (after?.truncated ?: true).json()))
            if (before != null && after != null) {
                response.addProperty("windowChanged", before.window != after.window)
                val changes = NodeRefRegistry().use { registry -> registry.record(before); registry.record(after) }
                response.add("changes", changes)
                observations.actionBaseline(before)
            }
            operation.finish(PortResult.Success(ToolReply(response)))
        }
        // A host read may never return; the sampling deadline must not depend on its callback.
        operation.delay(ScreenStability.MAX_WAIT_MS) { complete(latest, false) }
        fun poll() { readSnapshot(operation) { after ->
            latest = after
            if (after == null) operation.delay((ScreenStability.QUIET_MS - (scheduler.nowMs() - startedAt)).coerceAtLeast(0)) { complete(null, false) }
            else when (stability.sample(after, scheduler.nowMs())) {
                ScreenStability.State.WAITING -> operation.delay(ScreenStability.POLL_MS) { poll() }
                ScreenStability.State.STABLE -> complete(after, true)
                ScreenStability.State.TIMED_OUT -> complete(after, false)
            }
        } }
        poll()
    }

    /** An explicit condition wait supplies its own deadline; refresh at that completion point. */
    fun afterWait(value: JsonElement, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
        val operation = Operation<ToolReply>(timeoutMs, callback)
        readSnapshot(operation) { snapshot ->
            val result = value.asJsonObject.deepCopy()
            snapshot?.let { observations.sinceAction(it)?.let { changes -> result.add("sinceLastAction", changes) } }
            operation.finish(PortResult.Success(ToolReply(result)))
        }
        return operation
    }

    private fun <T> readSnapshot(operation: Operation<T>, receive: (CompactNodeText.Snapshot?) -> Unit) {
        if (!canObserve) { receive(null); return }
        operation.call(ToolHandlers.bridge("accessibility.dump", jsonArray(jsonObject("format" to "compact".json(), "maxNodes" to 200.json(), "maxDepth" to 32.json(), "visibleOnly" to true.json())))) { value ->
            when (value) {
                is PortResult.Failure -> if (value.error.hostLost) operation.finish(value) else receive(null)
                is PortResult.Success -> receive(runCatching { CompactNodeText.parse(value.value) }.getOrNull())
            }
        }
    }

    private inner class Operation<T>(timeoutMs: Long, private val callback: (PortResult<T>) -> Unit) : Cancellation {
        private val closed = AtomicBoolean()
        private val handles = ConcurrentLinkedQueue<Cancellation>()
        private val end = scheduler.nowMs() + timeoutMs
        init { own(scheduler.schedule(timeoutMs) { finish(PortResult.Failure(RunError.BUDGET_EXCEEDED)) }) }
        private fun own(handle: Cancellation) { handles.add(handle); if (closed.get()) handle.cancel() }
        fun delay(ms: Long, action: () -> Unit) { if (!closed.get()) own(scheduler.schedule(ms) { if (!closed.get()) action() }) }
        fun call(request: BridgeCall, receive: (PortResult<JsonElement>) -> Unit) {
            if (closed.get()) return
            val remaining = end - scheduler.nowMs()
            if (remaining <= 0) { finish(PortResult.Failure(RunError.BUDGET_EXCEEDED)); return }
            own(dispatch(request.copy(timeoutMs = minOf(request.timeoutMs, remaining))) { result ->
                scheduler.execute { if (!closed.get()) receive(result) }
            })
        }
        fun finish(value: PortResult<T>) { if (closed.compareAndSet(false, true)) { handles.forEach { it.cancel() }; callback(value) } }
        override fun cancel() { closed.set(true); handles.forEach { it.cancel() } }
    }

    companion object {
        val NODE_ACTIONS = setOf("ui_click", "ui_long_click", "ui_set_text", "ui_scroll")
        val GESTURES = setOf("ui_click_xy", "ui_swipe", "ui_gesture")
        val NAMES = NODE_ACTIONS + GESTURES + setOf("ui_press_key", "app_launch", "clipboard_get", "clipboard_set")
    }
}
