package io.github.supermonster003.autojs6.plugin.three.stove.agent.model

import com.google.gson.JsonArray
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*

/** One host request, multiple independently admitted model rounds, one immutable overall deadline. */
internal class NativeModelInvocation(private val broker: ModelBrokerTransport, private val target: ModelTarget,
                                      private val scheduler: RunScheduler, private val id: String,
                                      private val initialBytes: Int, private val maximumInputBytes: Int, private val outputLimit: Int,
                                      names: Set<String>, callback: (PortResult<ModelReply>) -> Unit,
                                      private val vision: Boolean = false, initialImageTokens: Long = 0,
                                      initialImageCount: Int = 0, initialImageBytes: Long = 0) : NativeContinuation {
    override val limits = requireNotNull(target.nativeTools)
    private val lock = Any()
    private val events = NativeModelEvents(id, target.targetId, target.maximumOutputBytes, names, limits.rounds)
    private var current: Round? = Round(callback)
    private var deliveredRound: Round? = null
    private var pendingCalls = emptyList<NativeToolCall>()
    private var resultBytes = 0
    private var retainedImageTokens = initialImageTokens
    private var imageCount = initialImageCount
    private var imageBytes = initialImageBytes
    private var ended = false
    private var failure: RunError? = null
    private var idleProgress: PortResult.Failure? = null
    private var failureListener: ((RunError) -> Unit)? = null
    private var deadline = Cancellation.NONE
    private var dispatches = 0
    private var cancelPending = false
    private var cancelSent = false

    private inner class Round(val callback: (PortResult<ModelReply>) -> Unit) : ModelCallCancellation {
        var delivered = false
        var claimed = false
        var snapshot: PortResult.Failure? = null
        override fun cancel() { if (synchronized(lock) { current === this || (deliveredRound === this && !claimed) }) abort(RunError.CANCELLED) }
        override fun progress() = synchronized(lock) { snapshot ?: events.progress() }
    }
    override fun claim() { synchronized(lock) { deliveredRound?.claimed = true } }
    fun dispatch(request: String, timeoutMs: Long, images: List<ModelImage> = emptyList()): Cancellation {
        val round = checkNotNull(current)
        val timer = scheduler.schedule(timeoutMs) { abort(RunError.MODEL_TIMEOUT) }
        synchronized(lock) { if (ended) timer.cancel() else deadline = timer }
        dispatch { broker.generate(request, images, ::onEvent, ::abort) }
        return round
    }
    override fun inputBytes(results: List<NativeToolResult>): Int = synchronized(lock) {
        resultEnvelope(results) // Bound and correlate before the runner admits another model call.
        val size = initialBytes.toLong() + events.retainedBytes + resultBytes + results.sumOf { StepJournal.bytes(it.wire()).toLong() }
        if (size > maximumInputBytes) throw ContextLimitExceeded()
        size.toInt()
    }
    override fun imageTokens(results: List<NativeToolResult>): Long = synchronized(lock) {
        resultEnvelope(results)
        retainedImageTokens + results.sumOf { result -> result.images.sumOf { it.estimatedTokens } }
    }
    override fun resume(results: List<NativeToolResult>, maximumOutputTokens: Int, timeoutMs: Long,
                        callback: (PortResult<ModelReply>) -> Unit): Cancellation {
        var request: String? = null
        var error: RunError? = null
        val round = synchronized(lock) {
            if (ended) { error = failure ?: RunError.INVALID_REQUEST; null }
            else if (timeoutMs < 1000) { error = RunError.MODEL_TIMEOUT; null }
            // The append-only host method cannot change the original output ceiling. Never send a
            // continuation when the new token reservation is smaller than that ceiling.
            else if (maximumOutputTokens < outputLimit) { error = RunError.BUDGET_EXCEEDED; null }
            else try {
                check(current == null)
                inputBytes(results)
                request = resultEnvelope(results)
                resultBytes += results.sumOf { StepJournal.bytes(it.wire()) }
                retainedImageTokens += results.sumOf { result -> result.images.sumOf { it.estimatedTokens } }
                imageCount += results.sumOf { it.images.size }
                imageBytes += results.sumOf { result -> result.images.sumOf { it.byteCount } }
                events.resume(); pendingCalls = emptyList()
                Round(callback).also { current = it }
            } catch (_: ContextLimitExceeded) { error = RunError.LIMIT_EXCEEDED; null }
            catch (_: Exception) { error = RunError.INVALID_REQUEST; null }
        }
        if (round == null) {
            val rejected = synchronized(lock) {
                if (!ended && current == null) Round(callback).also { current = it } else null
            }
            if (rejected != null) { abort(checkNotNull(error)); return rejected }
            val failed = takeProgress()
            callback(failed.copy(error = checkNotNull(error)))
            return Cancellation.NONE
        }
        dispatch { broker.submitToolResults(checkNotNull(request), results.flatMap { it.images }, ::abort) }
        return round
    }
    private fun resultEnvelope(results: List<NativeToolResult>): String {
        require(pendingCalls.isNotEmpty() && results.size == pendingCalls.size)
        require(results.map { it.id }.distinct().size == results.size && results.map { it.id }.toSet() == pendingCalls.map { it.id }.toSet())
        results.forEach { AgentJson.checkUnicode(it.output); require(it.output.utf8Size() <= limits.resultBytes) }
        val images = results.flatMap { it.images }
        require(images.isEmpty() || vision)
        if (images.isNotEmpty()) requireNotNull(target.vision).validate(images, imageCount, imageBytes)
        return jsonObject("requestId" to id.json(), "round" to events.round.json(), "results" to JsonArray().apply {
            var index = 0
            results.forEach { add(it.wire(index)); index += it.images.size }
        }).toString().also { if (it.utf8Size() > limits.batchBytes) throw ContextLimitExceeded() }
    }
    override fun onFailure(callback: (RunError) -> Unit) {
        val error = synchronized(lock) { failureListener = callback; failure }
        error?.let(callback)
    }
    override fun takeProgress(): PortResult.Failure = synchronized(lock) {
        idleProgress?.also { idleProgress = null } ?: events.takeProgress()
    }
    override fun cancel() = abort(RunError.CANCELLED)

    private fun onEvent(json: String) {
        var delivery: Pair<Round, PortResult<ModelReply>>? = null
        var notify: Pair<(RunError) -> Unit, RunError>? = null
        synchronized(lock) {
            if (ended) return
            when (val event = events.accept(json)) {
                NativeModelEvents.Event.More -> Unit
                is NativeModelEvents.Event.Calls -> {
                    val round = current
                    if (round == null) { abort(RunError.INVALID_REQUEST); return }
                    pendingCalls = event.calls
                    val result = PortResult.Success(ModelReply("", event.progress.usage, NativeToolTurn(event.calls, this), event.progress.outputBytes))
                    round.delivered = true; round.snapshot = event.progress; current = null; deliveredRound = round
                    delivery = round to result
                }
                is NativeModelEvents.Event.Terminal -> {
                    ended = true; deadline.cancel()
                    val failed = event.result as? PortResult.Failure
                    failure = failed?.error
                    if (failed != null) cancelPending = true
                    val round = current
                    if (round != null) {
                        round.delivered = true
                        round.snapshot = failed ?: (event.result as PortResult.Success).value.let {
                            PortResult.Failure(RunError.CANCELLED, usage = it.usage, outputBytes = it.outputBytes)
                        }
                        current = null; delivery = round to event.result
                    } else if (failed != null) {
                        idleProgress = failed
                        failureListener?.let { notify = it to failed.error }
                    }
                }
            }
        }
        delivery?.let { deliver(it.first, it.second) }
        notify?.let { (listener, error) -> listener(error) }
        sendCancel()
    }
    private fun abort(error: RunError) {
        var delivery: Pair<Round, PortResult.Failure>? = null
        var notify: ((RunError) -> Unit)? = null
        synchronized(lock) {
            if (ended) return
            ended = true; failure = error; deadline.cancel(); cancelPending = true
            val result = events.abort(error)
            current?.let { round ->
                round.delivered = true; round.snapshot = result; delivery = round to result
            } ?: run { idleProgress = result; notify = failureListener }
            current = null
        }
        delivery?.let { deliver(it.first, it.second) }
        notify?.invoke(error)
        sendCancel()
    }
    private fun deliver(round: Round, result: PortResult<ModelReply>) {
        try { round.callback(result) } catch (_: Exception) { abort(RunError.INVALID_REQUEST) }
    }
    private fun dispatch(action: () -> Unit) {
        synchronized(lock) { if (ended) return; dispatches++ }
        try { action() } catch (_: Exception) { abort(RunError.HOST_UNAVAILABLE) }
        finally { synchronized(lock) { dispatches-- }; sendCancel() }
    }
    private fun sendCancel() {
        val send = synchronized(lock) {
            if (cancelPending && !cancelSent && dispatches == 0) { cancelSent = true; true } else false
        }
        if (send) runCatching { broker.cancel(id) }
    }
}
