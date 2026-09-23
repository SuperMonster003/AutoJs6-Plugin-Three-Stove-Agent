package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

fun interface ScriptCatalogSource {
    fun load(call: BridgeCall, callback: (PortResult<JsonElement>) -> Unit): Cancellation
}

/** One instance per authenticated link. Only completed, validated snapshots are cached. */
class ScriptCatalogClient(private val nowMs: () -> Long) : AutoCloseable {
    private data class Cached(val snapshot: ScriptCatalogSnapshot, val at: Long)
    private class Slot { var cached: Cached? = null }
    private val slots = linkedMapOf<List<String>, Slot>()
    private val pending = linkedSetOf<Request>()
    private var closed = false

    fun load(roots: Set<String>, refresh: Boolean, timeoutMs: Long, source: ScriptCatalogSource,
             callback: (PortResult<ScriptCatalogSnapshot>) -> Unit): Cancellation {
        require(roots.size <= 32 && roots.all { it.startsWith('/') && it.toByteArray(Charsets.UTF_8).size <= 1024 && '\u0000' !in it })
        require(timeoutMs in 1..300_000)
        val key = roots.sorted()
        val request: Request
        synchronized(this) {
            if (closed) { callback(PortResult.Failure(RunError.HOST_UNAVAILABLE)); return Cancellation.NONE }
            val cached = slots[key]?.cached
            if (!refresh && cached != null && nowMs() - cached.at in 0 until TTL_MS) {
                callback(PortResult.Success(cached.snapshot)); return Cancellation.NONE
            }
            if (pending.size >= 9) { callback(PortResult.Failure(RunError.LIMIT_EXCEEDED)); return Cancellation.NONE }
            // A later refresh owns this key; late older replies cannot overwrite its snapshot.
            val slot = Slot()
            slots.remove(key)
            if (slots.size >= 4) slots.remove(slots.keys.first())
            slots[key] = slot
            request = Request(key, slot, callback); pending.add(request)
        }
        val options = jsonObject("roots" to JsonArray().apply { add("."); key.forEach(::add) }, "limit" to ScriptCatalogSnapshot.MAX_ENTRIES.json())
        try {
            if (request.stopped.get()) return Cancellation.NONE
            val cancellation = source.load(ToolHandlers.bridge("agent.listScripts", jsonArray(options), timeoutMs)) reply@{ outcome ->
                if (request.stopped.get()) return@reply
                val result = when (outcome) {
                    is PortResult.Failure -> outcome
                    is PortResult.Success -> try { PortResult.Success(ScriptCatalogSnapshot.parse(outcome.value)) }
                    catch (_: Exception) { PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID) }
                }
                request.finish(result)
            }
            request.transport.set(cancellation)
            if (request.stopped.get()) cancellation.cancel()
        } catch (_: Exception) { request.finish(PortResult.Failure(RunError.HOST_UNAVAILABLE)) }
        return Cancellation { request.stop() }
    }

    fun invalidate() {
        val old = synchronized(this) { slots.clear(); pending.toList() }
        old.forEach { it.stop(RunError.CANCELLED) }
    }
    override fun close() { synchronized(this) { closed = true }; invalidate() }

    private inner class Request(val key: List<String>, val slot: Slot, val callback: (PortResult<ScriptCatalogSnapshot>) -> Unit) {
        val stopped = AtomicBoolean()
        val transport = AtomicReference(Cancellation.NONE)
        fun finish(result: PortResult<ScriptCatalogSnapshot>) {
            synchronized(this@ScriptCatalogClient) {
                if (!stopped.compareAndSet(false, true)) return
                pending.remove(this)
                if (!closed && slots[key] === slot && result is PortResult.Success) slot.cached = Cached(result.value, nowMs())
            }
            callback(result)
        }
        fun stop(error: RunError? = null) {
            synchronized(this@ScriptCatalogClient) {
                if (!stopped.compareAndSet(false, true)) return
                pending.remove(this)
            }
            transport.get().cancel()
            error?.let { callback(PortResult.Failure(it)) }
        }
    }
    companion object { const val TTL_MS = 60_000L }
}
