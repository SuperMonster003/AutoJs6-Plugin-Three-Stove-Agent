package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*

/** Host events only. Sequence, transcript and cumulative usage remain private to one invocation lock. */
internal class NativeModelEvents(private val id: String, private val target: String, private val maximumBytes: Int,
                                 private val names: Set<String>, private val maximumRounds: Int) {
    sealed interface Event {
        data object More : Event
        class Calls(val calls: List<NativeToolCall>, val progress: PortResult.Failure) : Event
        class Terminal(val result: PortResult<ModelReply>) : Event
    }
    private var sequence = 0L
    private var chunkSequence = 0L
    private var ended = false
    var round = 0; private set
    private var pending = false
    private val usedIds = hashSetOf<String>()
    private val transcript = StringBuilder()
    private var turnStart = 0
    private var textBytes = 0
    private var argumentBytes = 0
    private var reportedBytes = 0
    private var usage: ModelUsage? = null
    private var reportedUsage: ModelUsage? = null
    val retainedBytes: Int get() = textBytes + argumentBytes

    fun resume() { check(pending && !ended); pending = false }
    fun accept(json: String): Event = try {
        check(!ended)
        val event = AgentJson.objectOf(json, 2 * 1024 * 1024)
        require(event.string("requestId") == id && event.number("sequence") == sequence + 1)
        val type = requireNotNull(event.string("type"))
        require(if (sequence == 0L) type == "started" else type != "started")
        sequence++
        when (type) {
            "started" -> { fields(event); Event.More }
            "usage" -> { fields(event, "usage"); updateUsage(event.getAsJsonObject("usage")); Event.More }
            "chunk" -> {
                fields(event, "chunkSequence", "text")
                require(!pending && event.number("chunkSequence") == ++chunkSequence)
                val text = requireNotNull(event.string("text"))
                textBytes += text.toByteArray(Charsets.UTF_8).size
                if (retainedBytes > maximumBytes) returnTerminal(RunError.LIMIT_EXCEEDED)
                else { transcript.append(text); Event.More }
            }
            "tool_calls" -> {
                fields(event, "round", "calls", "usage")
                require(!pending && round < maximumRounds && event.number("round") == (round + 1).toLong())
                val entries = requireNotNull(event.getAsJsonArray("calls")); require(entries.size() in 1..32)
                val calls = entries.map { entry ->
                    val call = entry.asJsonObject
                    require(call.keySet() == setOf("callId", "name", "arguments"))
                    val callId = requireNotNull(call.string("callId"))
                    require(callId.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9._:-]{0,127}")) && usedIds.add(callId))
                    val name = requireNotNull(call.string("name")); require(name in names)
                    val arguments = requireNotNull(call.getAsJsonObject("arguments"))
                    argumentBytes += StepJournal.bytes(arguments)
                    NativeToolCall(callId, name, arguments)
                }
                if (retainedBytes > maximumBytes) returnTerminal(RunError.LIMIT_EXCEEDED) else {
                    if (event.has("usage")) updateUsage(event.getAsJsonObject("usage"))
                    round++; pending = true; turnStart = transcript.length
                    Event.Calls(calls, takeProgress())
                }
            }
            "completed" -> {
                fields(event, "text", "targetId", "finishReason")
                require(!pending && event.string("targetId") == target && event.number("finishReason") in 0..Int.MAX_VALUE.toLong())
                val text = requireNotNull(event.string("text"))
                require(chunkSequence == 0L || text == transcript.toString())
                textBytes = text.toByteArray(Charsets.UTF_8).size
                if (retainedBytes > maximumBytes) returnTerminal(RunError.LIMIT_EXCEEDED) else {
                    ended = true
                    val progress = takeProgress()
                    Event.Terminal(PortResult.Success(ModelReply(if (chunkSequence == 0L) text else text.substring(turnStart),
                        progress.usage, outputBytes = progress.outputBytes)))
                }
            }
            "failed", "cancelled" -> {
                fields(event, "code", "reason")
                val error = RunError.entries.firstOrNull { it.name == event.string("code") }
                require(error in ERRORS && (type != "cancelled" || error == RunError.CANCELLED))
                val reason = event.string("reason")?.also { require(it.matches(Regex("[A-Z][A-Z0-9_]{0,63}"))) }
                ended = true
                Event.Terminal(takeProgress().copy(error = error!!, reason = reason?.takeIf { it == "REQUEST_REJECTED" }))
            }
            else -> returnTerminal(RunError.INVALID_REQUEST)
        }
    } catch (_: Exception) { returnTerminal(RunError.INVALID_REQUEST) }

    fun abort(error: RunError): PortResult.Failure { ended = true; return takeProgress().copy(error = error) }
    fun progress(): PortResult.Failure {
        fun delta(current: Long?, previous: Long?) = current?.let { it - (previous ?: 0) }
        fun total(value: ModelUsage?): Long? {
            if (value == null || listOfNotNull(value.inputTokens, value.outputTokens, value.totalTokens).isEmpty()) return null
            val input = value.inputTokens ?: 0; val output = value.outputTokens ?: 0
            val sum = if (input > Long.MAX_VALUE - output) Long.MAX_VALUE else input + output
            return maxOf(sum, value.totalTokens ?: 0)
        }
        val next = usage?.let { ModelUsage(delta(it.inputTokens, reportedUsage?.inputTokens),
            delta(it.outputTokens, reportedUsage?.outputTokens), delta(total(it), total(reportedUsage))) }
        return PortResult.Failure(RunError.CANCELLED, usage = next, outputBytes = (retainedBytes - reportedBytes).coerceAtLeast(0))
    }
    fun takeProgress(): PortResult.Failure = progress().also { reportedUsage = usage; reportedBytes = retainedBytes }
    private fun returnTerminal(error: RunError) = Event.Terminal(abort(error))
    private fun updateUsage(data: JsonObject?) {
        requireNotNull(data)
        require(data.keySet().all { it in setOf("inputTokens", "outputTokens", "totalTokens", "durationMs") })
        fun count(key: String, previous: Long?): Long? = if (!data.has(key)) previous else requireNotNull(data.number(key)).also {
            require(it >= (previous ?: 0))
        }
        count("durationMs", null)
        usage = ModelUsage(count("inputTokens", usage?.inputTokens), count("outputTokens", usage?.outputTokens), count("totalTokens", usage?.totalTokens))
    }
    private fun fields(value: JsonObject, vararg extra: String) {
        require(value.keySet().all { it in extra || it in setOf("requestId", "type", "sequence") })
    }
    companion object {
        private val ERRORS = setOf(RunError.LINK_DETACHED, RunError.HOST_UNAVAILABLE, RunError.QUOTA_EXCEEDED,
            RunError.RATE_LIMITED, RunError.LIMIT_EXCEEDED, RunError.TARGET_UNSUPPORTED, RunError.TARGET_UNAVAILABLE,
            RunError.MODEL_FAILED, RunError.MODEL_TIMEOUT, RunError.CANCELLED, RunError.INVALID_REQUEST)
    }
}
