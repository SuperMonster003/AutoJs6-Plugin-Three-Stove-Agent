package io.github.supermonster003.autojs6.plugin.ai.agent.model

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.ToolPolicy
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Implemented by the Binder adapter (BinderModelBroker) over IAiAgentModelBroker, including Bundle/FD ownership and death.
 * Both calls must return promptly. Events contain complete bounded JSON, never partial FD reads.
 * Implementations translate Binder death to HOST_UNAVAILABLE, with no raw exception logging. */
interface ModelBrokerTransport {
    fun generate(requestJson: String, onEvent: (String) -> Unit)
    fun generate(requestJson: String, onEvent: (String) -> Unit, onFailure: (RunError) -> Unit) = generate(requestJson, onEvent)
    fun submitToolResults(requestJson: String, onFailure: (RunError) -> Unit) { onFailure(RunError.TARGET_UNSUPPORTED) }
    fun generate(requestJson: String, images: List<ModelImage>, onEvent: (String) -> Unit, onFailure: (RunError) -> Unit) {
        if (images.isEmpty()) generate(requestJson, onEvent, onFailure) else onFailure(RunError.TARGET_UNSUPPORTED)
    }
    fun submitToolResults(requestJson: String, images: List<ModelImage>, onFailure: (RunError) -> Unit) {
        if (images.isEmpty()) submitToolResults(requestJson, onFailure) else onFailure(RunError.TARGET_UNSUPPORTED)
    }
    fun cancel(requestId: String)
}

/** One selected public target per link-owned client. Each generate sends exactly one broker request. */
class ModelClient(
    private val broker: ModelBrokerTransport, private val target: ModelTarget, private val policy: ToolPolicy,
    private val fallbacks: SchemaFallbacks, private val scheduler: RunScheduler,
    private val blockingAllowed: () -> Boolean,
) : RunModel {
    private var structuredUnsupported = false
    private var toolsUnsupported = false

    @Synchronized override fun initialFormat(proposed: DecisionFormat): DecisionFormat =
        if (target.nativeTools != null && !toolsUnsupported) DecisionSchema.native(target.protocol)
        else if (structuredUnsupported) DecisionSchema.degraded(target.protocol, "TARGET_UNSUPPORTED") else fallbacks.select(target.schemaTarget, policy)

    @Synchronized override fun fallbackFormat(previous: DecisionFormat, failure: PortResult.Failure): DecisionFormat? {
        if (previous.degraded || previous.protocol != target.protocol || !target.supportsOutputLimit) return null
        if (previous.nativeTools) {
            if (failure.error != RunError.TARGET_UNSUPPORTED && !(failure.error == RunError.MODEL_FAILED && failure.reason == "REQUEST_REJECTED")) return null
            toolsUnsupported = true
            return fallbacks.select(target.schemaTarget, policy)
        }
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
            require(timeoutMs <= RunLimits.TOOL_TIMEOUT_MS && maximumOutputTokens in 1..65_536)
            val format = requireNotNull(input.format)
            require(format.protocol == target.protocol && (!format.degraded || input.schemaBytes == 0))
            require(format.degraded || format.nativeTools || target.structuredJson)
            require(!format.nativeTools || target.nativeTools != null)
            require(!input.vision || (target.vision != null && policy.visionAvailable))
            if (input.images.isNotEmpty()) requireNotNull(target.vision).validate(input.images)
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
                "structuredJson" to (format.responseSchemaJson != null).json(), "maximumOutputTokens" to
                    minOf(maximumOutputTokens, input.maximumOutputTokens ?: maximumOutputTokens).json(),
                "stream" to target.supportsStreaming.json(), "timeoutMs" to timeoutMs.json()).apply {
                format.responseSchemaJson?.let { add("responseSchema", AgentJson.objectOf(it, DecisionSchema.MAX_SCHEMA_BYTES)) }
                if (input.vision) addProperty("vision", true)
                if (input.images.isNotEmpty()) add("imageRefs", input.imageRefs)
                if (!input.tools.isEmpty) {
                    add("tools", input.tools); addProperty("maximumToolRounds", checkNotNull(target.nativeTools).rounds)
                }
            }.toString().also { require(it.utf8Size() <= 2 * 1024 * 1024) }
        } catch (_: ContextLimitExceeded) { callback(PortResult.Failure(RunError.LIMIT_EXCEEDED)); return Cancellation.NONE }
        catch (_: Exception) { callback(PortResult.Failure(RunError.INVALID_REQUEST)); return Cancellation.NONE }
        return if (input.tools.isEmpty) Invocation(id, callback).apply { dispatch(request, timeoutMs, input.images) }
        else NativeModelInvocation(broker, target, scheduler, id, input.inputBytes, minOf(target.maximumContextBytes, input.maximumContextBytes),
            minOf(maximumOutputTokens, input.maximumOutputTokens ?: maximumOutputTokens),
            input.tools.map { requireNotNull(it.asJsonObject.string("name")) }.toSet(), callback,
            input.vision, input.imageTokens, input.images.size, input.images.sumOf { it.byteCount }).dispatch(request, timeoutMs, input.images)
    }

    /** Synchronous wrapper used by JVM tests only; production always calls [generate] with a callback and the
     * Binder adapter passes blockingAllowed = false, so this throws there. The callback and timeout paths do not
     * depend on the waiting thread. */
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

        fun dispatch(request: String, timeoutMs: Long, images: List<ModelImage>) {
            val deadline = scheduler.schedule(timeoutMs) { abort(RunError.MODEL_TIMEOUT) }
            synchronized(lock) { if (completed) deadline.cancel() else timer = deadline }
            try { broker.generate(request, images, ::onEvent, ::abort) }
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
