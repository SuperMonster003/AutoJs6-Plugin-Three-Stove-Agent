package io.github.supermonster003.autojs6.plugin.ai.agent.service

import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import java.io.File
import java.util.concurrent.*

internal class SettingsRepository(file: File, private val changed: () -> Unit = {}) {
    private val disk = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(16),
        { work -> Thread(work, "ai-agent-settings").apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy())
    private val store = SettingsStore(file)
    @Volatile private var loaded: AgentSettings? = null
    init { disk.execute { runCatching { loaded = store.open(); changed() } } }
    fun snapshot(): AgentSettings = checkNotNull(loaded) { "Settings unavailable" }
    fun query(next: AgentSettings? = null, complete: (Result<AgentSettings>) -> Unit) {
        try { disk.execute { complete(runCatching {
            snapshot(); if (next != null) { store.save(next); loaded = next; changed() }; snapshot()
        }) } } catch (failure: Exception) { complete(Result.failure(failure)) }
    }
}
