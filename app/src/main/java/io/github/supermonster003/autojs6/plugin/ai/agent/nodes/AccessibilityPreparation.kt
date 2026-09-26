package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import com.google.gson.JsonElement
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/** Uses the host's unattended startup before inspection, never replays an executed action. */
internal class AccessibilityPreparation(
    private val scheduler: RunScheduler,
    private val dispatch: (BridgeCall, (PortResult<JsonElement>) -> Unit) -> Cancellation,
) {
    fun prepare(timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit,
                inspect: (Long, (PortResult<PreparedTool>) -> Unit) -> Cancellation): Cancellation {
        val closed = AtomicBoolean()
        val received = AtomicBoolean()
        val handles = ConcurrentLinkedQueue<Cancellation>()
        val end = scheduler.nowMs() + timeoutMs
        fun own(handle: Cancellation) { handles.add(handle); if (closed.get()) handle.cancel() }
        fun finish(result: PortResult<PreparedTool>) {
            if (closed.compareAndSet(false, true)) { handles.forEach { it.cancel() }; callback(result) }
        }
        own(scheduler.schedule(timeoutMs) { finish(PortResult.Failure(RunError.BUDGET_EXCEEDED)) })
        try {
            own(dispatch(ToolHandlers.bridge("accessibility.ensureEnabled", jsonArray(), minOf(timeoutMs, 10_000))) { result ->
                if (closed.get() || !received.compareAndSet(false, true)) return@dispatch
                scheduler.execute {
                    if (closed.get()) return@execute
                    val remaining = end - scheduler.nowMs()
                    when {
                        remaining <= 0 -> finish(PortResult.Failure(RunError.BUDGET_EXCEEDED))
                        result is PortResult.Failure -> finish(result)
                        (result as PortResult.Success).value != true.json() -> finish(PortResult.Failure(RunError.A11Y_SERVICE_NOT_RUNNING))
                        else -> try { own(inspect(remaining, ::finish)) }
                            catch (_: Exception) { finish(PortResult.Failure(RunError.HOST_UNAVAILABLE)) }
                    }
                }
            })
        } catch (_: Exception) { finish(PortResult.Failure(RunError.HOST_UNAVAILABLE)) }
        return Cancellation { closed.set(true); handles.forEach { it.cancel() } }
    }

    companion object {
        fun required(invocation: ToolInvocation): Boolean = when (val plan = invocation.plan) {
            is ToolPlan.Call -> "accessibility" in plan.request.permissions || invocation.name == "ui_press_key"
            is ToolPlan.Poll -> "accessibility" in plan.request.permissions
            is ToolPlan.Repeat -> "accessibility" in plan.request.permissions
            is ToolPlan.AppendText -> true
            else -> false
        }
    }
}
