package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*

/** Decodes the host's generation events, never Provider responses. Confined to one invocation lock. */
internal class ModelEventSequence(private val requestId: String, private val targetId: String, private val maximumOutputBytes: Int) {
    sealed interface Event {
        data object More : Event
        class Terminal(val result: PortResult<ModelReply>) : Event
        class Rejected(val failure: PortResult.Failure) : Event
    }
    private var sequence = 0L
    private var chunkSequence = 0L
    private var ended = false
    private var seenUsage = false
    private var usage: ModelUsage? = null
    private val chunks = StringBuilder()
    private var outputBytes = 0

    fun accept(json: String): Event {
        if (ended) return Event.Rejected(failure(RunError.INVALID_REQUEST))
        return try {
            val event = AgentJson.objectOf(json, 2 * 1024 * 1024)
            require(event.string("requestId") == requestId && event.number("sequence") == sequence + 1)
            val type = event.string("type") ?: error("Missing event type")
            require(if (sequence == 0L) type == "started" else type != "started")
            sequence++
            when (type) {
                "started" -> { fields(event, emptySet()); Event.More }
                "chunk" -> {
                    fields(event, setOf("text", "chunkSequence"))
                    require(!seenUsage && event.number("chunkSequence") == chunkSequence + 1)
                    val text = event.string("text") ?: error("Missing chunk")
                    chunkSequence++
                    val size = text.toByteArray(Charsets.UTF_8).size
                    if (size > maximumOutputBytes - outputBytes) return reject(RunError.LIMIT_EXCEEDED)
                    outputBytes += size; chunks.append(text)
                    Event.More
                }
                "usage" -> {
                    fields(event, setOf("usage"))
                    require(!seenUsage)
                    val data = event["usage"]?.takeIf { it.isJsonObject }?.asJsonObject ?: error("Missing usage")
                    require(data.keySet().all { it in setOf("inputTokens", "outputTokens", "totalTokens", "durationMs") })
                    fun count(key: String): Long? = if (!data.has(key)) null else checkNotNull(data.number(key)).also { require(it >= 0) }
                    count("durationMs")
                    usage = ModelUsage(count("inputTokens"), count("outputTokens"), count("totalTokens"))
                    seenUsage = true; Event.More
                }
                "completed" -> {
                    fields(event, setOf("text", "targetId", "finishReason"))
                    require(event.string("targetId") == targetId && event.number("finishReason") in 0..Int.MAX_VALUE.toLong())
                    val text = event.string("text") ?: error("Missing completion")
                    val size = text.toByteArray(Charsets.UTF_8).size
                    if (size > maximumOutputBytes) return reject(RunError.LIMIT_EXCEEDED)
                    require(chunkSequence == 0L || text == chunks.toString())
                    outputBytes = size; ended = true; chunks.setLength(0)
                    Event.Terminal(PortResult.Success(ModelReply(text, usage)))
                }
                "failed", "cancelled" -> {
                    fields(event, setOf("code", "reason"))
                    val code = event.string("code")?.let { value -> RunError.entries.firstOrNull { it.name == value } }
                    require(code in MODEL_ERRORS)
                    require(type != "cancelled" || code == RunError.CANCELLED)
                    val reason = if (!event.has("reason")) null else requireNotNull(event.string("reason")).also {
                        require(it.matches(Regex("[A-Z][A-Z0-9_]{0,63}")))
                    }
                    ended = true; chunks.setLength(0)
                    Event.Terminal(failure(code!!, if (reason == "REQUEST_REJECTED") reason else null))
                }
                else -> reject(RunError.INVALID_REQUEST)
            }
        } catch (_: Exception) { reject(RunError.INVALID_REQUEST) }
    }
    fun abort(error: RunError): PortResult.Failure { ended = true; chunks.setLength(0); return failure(error) }
    fun progress(): PortResult.Failure = failure(RunError.CANCELLED)
    private fun failure(error: RunError, reason: String? = null) = PortResult.Failure(error, reason, usage, outputBytes)
    private fun reject(error: RunError): Event.Rejected = Event.Rejected(abort(error))
    private fun fields(value: JsonObject, extra: Set<String>) {
        require(value.keySet().all { it in extra || it in setOf("requestId", "type", "sequence") })
    }
    companion object {
        private val MODEL_ERRORS = setOf(RunError.LINK_DETACHED, RunError.HOST_UNAVAILABLE, RunError.QUOTA_EXCEEDED,
            RunError.RATE_LIMITED, RunError.LIMIT_EXCEEDED, RunError.TARGET_UNSUPPORTED, RunError.TARGET_UNAVAILABLE,
            RunError.MODEL_FAILED, RunError.MODEL_TIMEOUT, RunError.CANCELLED, RunError.INVALID_REQUEST)
    }
}
