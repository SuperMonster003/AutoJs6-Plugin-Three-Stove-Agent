package io.github.supermonster003.autojs6.plugin.ai.agent.service

import io.github.supermonster003.autojs6.plugin.ai.agent.model.MemoryContext
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.Cancellation
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import java.io.File
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

/** All mutations and UI reads share this process-owned worker; admission uses a published snapshot. */
internal class MemoryRepository(directory: File, legacy: File? = null) : AutoCloseable {
    private val worker = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(32),
        { work -> Thread(work, "ai-agent-memory").apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy())
    private val store = MemoryStore(directory, legacy)
    @Volatile private var loaded: List<MemoryEntry>? = null
    init { worker.execute { runCatching { loaded = store.open() } } }
    fun snapshot(preset: String, enabled: Boolean, scope: String): MemoryContext {
        if (!enabled || scope == "none") return MemoryContext.EMPTY
        val values = loaded ?: return MemoryContext.UNAVAILABLE
        return MemoryContext.select(values, preset, scope != "preset", scope != "global")
    }
    /** Queue behind initialization so the first task after a process restart sees persisted entries. */
    fun snapshot(preset: String, enabled: Boolean, scope: String, complete: (MemoryContext) -> Unit): Cancellation {
        if (!enabled || scope == "none") { complete(MemoryContext.EMPTY); return Cancellation.NONE }
        return query({ MemoryContext.select(it.snapshot(), preset, scope != "preset", scope != "global") }) {
            complete(it.getOrDefault(MemoryContext.UNAVAILABLE))
        }
    }
    fun <T> query(work: (MemoryStore) -> T, complete: (Result<T>) -> Unit): Cancellation {
        val stopped = AtomicBoolean()
        try { worker.execute {
            if (stopped.get()) return@execute
            val result = runCatching { checkNotNull(loaded); work(store) }
            // A category clear can stop after some durable deletions; publish the actual remaining rows.
            runCatching { loaded = store.snapshot() }
            if (!stopped.get()) complete(result)
        } } catch (failure: Exception) { complete(Result.failure(failure)) }
        return Cancellation { stopped.set(true) }
    }
    override fun close() { worker.shutdown() }
}
