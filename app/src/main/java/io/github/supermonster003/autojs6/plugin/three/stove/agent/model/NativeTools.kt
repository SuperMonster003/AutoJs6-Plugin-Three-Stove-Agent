package io.github.supermonster003.autojs6.plugin.three.stove.agent.model

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*

/** Optional V1 extension, negotiated independently of the base Agent contract. */
data class NativeToolLimits(val rounds: Int = 16, val resultBytes: Int = 64 * 1024, val batchBytes: Int = 128 * 1024) {
    init { require(rounds in 1..16 && resultBytes in 128..64 * 1024 && batchBytes in 128..128 * 1024) }
    companion object {
        fun fromBroker(info: JsonObject): NativeToolLimits? {
            if (info.number("toolCallingVersion") != 1L) return null
            fun limit(key: String, maximum: Int) = requireNotNull(info.number(key)).also { require(it > 0) }.coerceAtMost(maximum.toLong()).toInt()
            return NativeToolLimits(limit("maximumToolRounds", 16), limit("maximumToolResultBytes", 64 * 1024),
                limit("maximumToolResultBatchBytes", 128 * 1024))
        }
    }
}

class NativeToolCall(val id: String, val name: String, arguments: JsonObject) {
    private val data = arguments.deepCopy()
    val decision: JsonObject get() = jsonObject("kind" to "tool".json(), "tool" to name.json(), "arguments" to data.deepCopy())
    override fun toString() = "NativeToolCall(name=$name)"
}
class NativeToolResult(val id: String, val output: String, val isError: Boolean, images: List<ModelImage> = emptyList()) {
    val images = images.toList()
    fun wire(firstImageIndex: Int = 0) = jsonObject("callId" to id.json(), "output" to output.json(), "isError" to isError.json()).apply {
        if (images.isNotEmpty()) add("imageRefs", imageReferences(images, firstImageIndex))
    }
    override fun toString() = "NativeToolResult(bytes=${output.utf8Size()}, isError=$isError)"
}
class NativeToolTurn(calls: List<NativeToolCall>, val continuation: NativeContinuation) {
    val calls = calls.toList()
    init { require(calls.size in 1..32 && calls.map { it.id }.distinct().size == calls.size) }
    override fun toString() = "NativeToolTurn(calls=${calls.size})"
}

/** Owned by one run. No device operation occurs here. Continuations reuse the original broker callback. */
interface NativeContinuation : Cancellation {
    val limits: NativeToolLimits
    /** Transfers a delivered pause from the model operation to the run, closing cancellation races. */
    fun claim()
    fun inputBytes(results: List<NativeToolResult>): Int
    fun imageTokens(results: List<NativeToolResult>): Long = 0
    fun resume(results: List<NativeToolResult>, maximumOutputTokens: Int, timeoutMs: Long,
               callback: (PortResult<ModelReply>) -> Unit): Cancellation
    /** A host failure/deadline must also interrupt a paused confirmation or tool operation. */
    fun onFailure(callback: (RunError) -> Unit)
    /** Consume late usage while no model operation is in flight, without charging another model call. */
    fun takeProgress(): PortResult.Failure
}
