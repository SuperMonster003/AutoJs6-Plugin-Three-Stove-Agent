package io.github.supermonster003.autojs6.plugin.ai.agent.model

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.ToolPolicy
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** P2.5 adapts these calls to IAiAgentModelBroker, including Bundle/FD ownership and death.
 * Both calls must return promptly. Events contain complete bounded JSON, never partial FD reads.
 * Implementations translate Binder death to HOST_UNAVAILABLE, with no raw exception logging. */
interface ModelBrokerTransport {
    fun generate(requestJson: String, onEvent: (String) -> Unit)
    fun cancel(requestId: String)
}

/** One selected public target per link-owned client. Each generate sends exactly one broker request. */
class ModelClient(
    private val broker: ModelBrokerTransport, private val target: ModelTarget, private val policy: ToolPolicy,
    private val fallbacks: SchemaFallbacks, private val scheduler: RunScheduler,
    private val blockingAllowed: () -> Boolean,
) : RunModel {
    private var structuredUnsupported = false

    @Synchronized override fun initialFormat(proposed: DecisionFormat): DecisionFormat =
        if (structuredUnsupported) DecisionSchema.degraded(target.protocol, "TARGET_UNSUPPORTED") else fallbacks.select(target.schemaTarget, policy)

    @Synchronized override fun fallbackFormat(previous: DecisionFormat, failure: PortResult.Failure): DecisionFormat? {
        if (previous.degraded || previous.protocol != target.protocol || !target.supportsOutputLimit) return null
        return when {
            failure.error == RunError.TARGET_UNSUPPORTED && !structuredUnsupported -> {
                structuredUnsupported = true
                DecisionSchema.degraded(target.protocol, "TARGET_UNSUPPORTED")
            }
            failure.error == RunError.MODEL_FAILED && failure.reason == "REQUEST_REJECTED" ->
                fallbacks.onRejected(target.schemaTarget, previous, failure.reason, policy)
            else -> null
        }
    }

    override fun generate(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long,
                          callback: (PortResult<ModelReply>) -> Unit): Cancellation {
        if (timeoutMs < 1000) { callback(PortResult.Failure(RunError.MODEL_TIMEOUT)); return Cancellation.NONE }
        // Omitting a required output ceiling would bypass the runner's token admission.
        if (!target.supportsOutputLimit) { callback(PortResult.Failure(RunError.TARGET_UNSUPPORTED)); return Cancellation.NONE }
        val id = "decision-${UUID.randomUUID()}"
        val request = try {
            require(timeoutMs <= 600_000 && maximumOutputTokens in 1..65_536)
            val format = requireNotNull(input.format)
            require(format.protocol == target.protocol && (!format.degraded || input.schemaBytes == 0))
            require(format.degraded || target.structuredJson)
            val messages = input.messages
            require(messages.size() in 1..256)
            messages.forEach {
                require(it.isJsonObject && it.asJsonObject.keySet() == setOf("role", "content"))
                require(it.asJsonObject.string("role") in setOf("system", "user", "assistant"))
                require(!it.asJsonObject.string("content").isNullOrEmpty())
            }
            require(messages.last().asJsonObject.string("role") == "user")
            if (input.inputBytes > target.maximumContextBytes) throw ContextLimitExceeded()
            jsonObject("requestId" to id.json(), "targetId" to target.targetId.json(), "messages" to messages,
                "structuredJson" to (!format.degraded).json(), "maximumOutputTokens" to
                    minOf(maximumOutputTokens, input.maximumOutputTokens ?: maximumOutputTokens).json(),
                "stream" to target.supportsStreaming.json(), "timeoutMs" to timeoutMs.json()).apply {
                format.responseSchemaJson?.let { add("responseSchema", AgentJson.objectOf(it, DecisionSchema.MAX_SCHEMA_BYTES)) }
            }.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= 2 * 1024 * 1024) }
        } catch (_: ContextLimitExceeded) { callback(PortResult.Failure(RunError.LIMIT_EXCEEDED)); return Cancellation.NONE }
        catch (_: Exception) { callback(PortResult.Failure(RunError.INVALID_REQUEST)); return Cancellation.NONE }
        return Invocation(id, callback).apply { dispatch(request, timeoutMs) }
    }

    /** Only a worker may block; the P2.5 adapter must exclude main/Binder/runner threads.
     * The callback and timeout paths do not depend on this waiting thread. */
    fun await(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long): PortResult<ModelReply> {
        check(blockingAllowed()) { "Model wait requires an independent worker thread" }
        val latch = CountDownLatch(1)
        val result = AtomicReference<PortResult<ModelReply>?>(null)
        val started = System.nanoTime()
        val cancellation = generate(input, maximumOutputTokens, timeoutMs) {
            if (result.compareAndSet(null, it)) latch.countDown()
        }
        try {
            val remaining = (timeoutMs - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).coerceAtLeast(0)
            if (!latch.await(remaining, TimeUnit.MILLISECONDS)) {
                if (cancellation is Invocation) cancellation.timeout() else cancellation.cancel()
                result.compareAndSet(null, (cancellation as? Invocation)?.outcome() ?: PortResult.Failure(RunError.MODEL_TIMEOUT))
            }
        } catch (_: InterruptedException) {
            cancellation.cancel()
            result.compareAndSet(null, (cancellation as? Invocation)?.outcome() ?: PortResult.Failure(RunError.CANCELLED))
            Thread.currentThread().interrupt()
        }
        return checkNotNull(result.get())
    }

    private inner class Invocation(private val id: String, private val callback: (PortResult<ModelReply>) -> Unit) : ModelCallCancellation {
        private val lock = Any()
        private val events = ModelEventSequence(id, target.targetId, target.maximumOutputBytes)
        private var completed = false
        private var terminalResult: PortResult<ModelReply>? = null
        private var dispatching = true
        private var cancelPending = false
        private var cancelSent = false
        private var timer = Cancellation.NONE

        fun dispatch(request: String, timeoutMs: Long) {
            val deadline = scheduler.schedule(timeoutMs) { abort(RunError.MODEL_TIMEOUT) }
            synchronized(lock) { if (completed) deadline.cancel() else timer = deadline }
            try { broker.generate(request, ::onEvent) }
            catch (_: Exception) { abort(RunError.HOST_UNAVAILABLE) }
            finally { synchronized(lock) { dispatching = false }; sendCancel() }
        }
        private fun onEvent(json: String) {
            var accepted: PortResult<ModelReply>? = null
            synchronized(lock) {
                when (val event = events.accept(json)) {
                    ModelEventSequence.Event.More -> Unit
                    is ModelEventSequence.Event.Terminal -> if (!completed) { completed = true; accepted = event.result }
                    is ModelEventSequence.Event.Rejected -> {
                        cancelPending = true
                        if (!completed) { completed = true; accepted = event.failure }
                    }
                }
                if (accepted != null) terminalResult = accepted
            }
            deliver(accepted)
        }
        private fun abort(error: RunError) {
            val accepted = synchronized(lock) {
                if (completed) null else { completed = true; cancelPending = true; events.abort(error).also { terminalResult = it } }
            }
            deliver(accepted)
        }
        private fun deliver(result: PortResult<ModelReply>?) {
            if (result != null) {
                synchronized(lock) { timer.cancel() }
                try { callback(result) } catch (_: Exception) { /* Consumer isolation; no model text in logs. */ }
            }
            sendCancel()
        }
        private fun sendCancel() {
            val send = synchronized(lock) {
                if (!dispatching && cancelPending && !cancelSent) { cancelSent = true; true } else false
            }
            if (send) try { broker.cancel(id) } catch (_: Exception) { /* Best effort after host loss. */ }
        }
        override fun cancel() = abort(RunError.CANCELLED)
        override fun progress(): PortResult.Failure = synchronized(lock) { events.progress() }
        fun outcome(): PortResult<ModelReply>? = synchronized(lock) { terminalResult }
        fun timeout() = abort(RunError.MODEL_TIMEOUT)
    }
}
