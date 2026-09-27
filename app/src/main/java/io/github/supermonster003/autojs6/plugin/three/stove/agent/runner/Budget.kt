package io.github.supermonster003.autojs6.plugin.three.stove.agent.runner

import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract

/** Pure-core ceilings bound to the host contract (appendix B.5), so a relocked AAR cannot drift from the loop. */
object RunLimits {
    const val STEPS = ThreeStoveAgentContract.MAX_STEPS
    const val MODEL_CALLS = ThreeStoveAgentContract.MAX_MODEL_CALLS
    const val QUEUED_RUNS = ThreeStoveAgentContract.MAX_RUN_QUEUE
    const val DURATION_MS = ThreeStoveAgentContract.MAX_DURATION_MS
    const val DETACHED_DURATION_MS = ThreeStoveAgentContract.MAX_DETACHED_DURATION_MS
    const val DEFAULT_TOOL_TIMEOUT_MS = ThreeStoveAgentContract.DEFAULT_TOOL_TIMEOUT_MS
    const val TOOL_TIMEOUT_MS = ThreeStoveAgentContract.MAX_TOOL_TIMEOUT_MS
    const val TOKENS = ThreeStoveAgentContract.DEFAULT_MAX_TOTAL_TOKENS_PER_LINK
    const val JOURNAL_BYTES = ThreeStoveAgentContract.MAX_RUN_JOURNAL_BYTES
    /** Plugin defaults (P2.3, relaxed on maintainer feedback in P14.3); settings, presets and requests can only narrow them. */
    const val DEFAULT_DURATION_MS = 15 * 60_000L
    const val DEFAULT_DETACHED_DURATION_MS = 30 * 60_000L
    /** Whole preparation window (model selection, catalog, memory, MCP) before the first decision. */
    const val PREPARATION_MS = 15_000L
    /** MCP discovery gets a strict share of the window so a slow server cannot starve the rest. */
    const val MCP_PREPARATION_MS = 8_000L
}

data class BudgetLimits(
    val maxSteps: Int = 60,
    val maxModelCalls: Int = 90,
    val maxDurationMs: Long = RunLimits.DEFAULT_DURATION_MS,
    val maxTotalTokens: Long = 500_000,
    val stepToolTimeoutMs: Long = RunLimits.DEFAULT_TOOL_TIMEOUT_MS,
    val confirmationTimeoutMs: Long = 120_000,
    val askTimeoutMs: Long = 10 * 60_000,
) {
    init {
        require(maxSteps in 1..RunLimits.STEPS && maxModelCalls in 1..RunLimits.MODEL_CALLS)
        require(maxDurationMs in 1..RunLimits.DETACHED_DURATION_MS && maxTotalTokens in 1..RunLimits.TOKENS)
        require(stepToolTimeoutMs in 1..RunLimits.TOOL_TIMEOUT_MS)
        require(confirmationTimeoutMs in 1..RunLimits.DURATION_MS && askTimeoutMs in 1..RunLimits.DURATION_MS)
    }
    fun validateOwnership(detached: Boolean) {
        require(maxDurationMs <= if (detached) RunLimits.DETACHED_DURATION_MS else RunLimits.DURATION_MS)
    }
    companion object {
        fun defaults(detached: Boolean) = BudgetLimits(maxDurationMs = if (detached) RunLimits.DEFAULT_DETACHED_DURATION_MS else RunLimits.DEFAULT_DURATION_MS)
    }
}

data class ModelUsage(val inputTokens: Long? = null, val outputTokens: Long? = null, val totalTokens: Long? = null) {
    init { require(listOfNotNull(inputTokens, outputTokens, totalTokens).all { it >= 0 }) }
}

class BudgetExceeded(val dimension: String) : IllegalStateException("BUDGET_EXCEEDED: $dimension")

/** Confined to the run scheduler. Admission and settlement use the same monotonic clock. */
class Budget(val limits: BudgetLimits, private val startedMs: Long, private val nowMs: () -> Long) {
    var steps = 0; private set
    var modelCalls = 0; private set
    var toolCalls = 0; private set
    var inputTokens = 0L; private set
    var outputTokens = 0L; private set
    var totalTokens = 0L; private set
    var estimated = false; private set
    private var reservation: ModelReservation? = null
    private var tokenLimit = limits.maxTotalTokens
    fun narrowTokens(maximum: Long) { require(maximum >= 0); tokenLimit = minOf(tokenLimit, maximum); check() }

    class ModelReservation internal constructor(val inputEstimate: Long, val maximumOutputTokens: Int)
    val durationMs: Long get() = (nowMs() - startedMs).coerceAtLeast(0)
    val remainingMs: Long get() = (limits.maxDurationMs - durationMs).coerceAtLeast(0)

    fun check() {
        if (durationMs >= limits.maxDurationMs) throw BudgetExceeded("duration")
        if (totalTokens > tokenLimit) throw BudgetExceeded("tokens")
    }
    fun beginStep() {
        check()
        if (steps >= limits.maxSteps) throw BudgetExceeded("steps")
        steps++
    }
    fun reserveModel(inputBytes: Int, desiredOutputTokens: Int, imageTokens: Long = 0): ModelReservation {
        check()
        require(inputBytes >= 0 && desiredOutputTokens > 0 && imageTokens >= 0)
        check(reservation == null) { "Only one model call may be in flight" }
        if (modelCalls >= limits.maxModelCalls) throw BudgetExceeded("modelCalls")
        val input = saturatedAdd(estimate(inputBytes), imageTokens)
        if (input >= tokenLimit - totalTokens) throw BudgetExceeded("tokens")
        val remaining = tokenLimit - totalTokens - input
        if (remaining <= 0) throw BudgetExceeded("tokens")
        modelCalls++
        return ModelReservation(input, minOf(remaining, desiredOutputTokens.toLong()).toInt()).also { reservation = it }
    }
    fun settleModel(ticket: ModelReservation, usage: ModelUsage?, outputBytes: Int) {
        require(outputBytes >= 0)
        check(reservation === ticket) { "Model call was already settled" }
        reservation = null
        charge(usage, outputBytes, ticket.inputEstimate)
    }
    /** Late cumulative usage while a native invocation waits for tools, not another model call. */
    fun settleProgress(usage: ModelUsage?, outputBytes: Int) {
        require(outputBytes >= 0)
        if (outputBytes == 0 && listOfNotNull(usage?.inputTokens, usage?.outputTokens, usage?.totalTokens).all { it == 0L }) return
        charge(usage, outputBytes, 0)
    }
    private fun charge(usage: ModelUsage?, outputBytes: Int, inputEstimate: Long) {
        val input = usage?.inputTokens ?: inputEstimate
        val output = usage?.outputTokens ?: estimate(outputBytes)
        inputTokens = saturatedAdd(inputTokens, input)
        outputTokens = saturatedAdd(outputTokens, output)
        totalTokens = saturatedAdd(totalTokens, maxOf(saturatedAdd(input, output), usage?.totalTokens ?: 0))
        estimated = estimated || usage?.inputTokens == null || usage.outputTokens == null
    }
    fun abandonModel() { reservation?.let { settleModel(it, null, 0) } }
    fun beginTool() { check(); toolCalls++ }
    fun toolTimeout(registeredScriptTimeoutMs: Long? = null): Long {
        require(registeredScriptTimeoutMs == null || registeredScriptTimeoutMs in 1..RunLimits.TOOL_TIMEOUT_MS)
        return minOf(registeredScriptTimeoutMs ?: limits.stepToolTimeoutMs, remainingMs)
    }
    fun usageJson() = jsonObject("modelCalls" to modelCalls.json(),
        "inputTokens" to saturatedAdd(inputTokens, reservation?.inputEstimate ?: 0).json(), "outputTokens" to outputTokens.json(),
        "totalTokens" to saturatedAdd(totalTokens, reservation?.inputEstimate ?: 0).json(), "estimated" to (estimated || reservation != null).json())
    fun remainingJson() = jsonObject("steps" to (limits.maxSteps - steps).json(),
        "modelCalls" to (limits.maxModelCalls - modelCalls).json(), "durationMs" to remainingMs.json(),
        "tokens" to (tokenLimit - totalTokens - (reservation?.inputEstimate ?: 0)).coerceAtLeast(0).json())

    companion object {
        fun estimate(bytes: Int): Long { require(bytes >= 0); return (bytes.toLong() * 2 + 4) / 5 }
        private fun saturatedAdd(a: Long, b: Long) = if (a > Long.MAX_VALUE - b) Long.MAX_VALUE else a + b
    }
}
