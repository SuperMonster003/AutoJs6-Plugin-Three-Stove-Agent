package io.github.supermonster003.autojs6.plugin.ai.agent.service

import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import java.io.File
import java.util.concurrent.Executors

/** The sole store owner lives in :agent; Binder reads never perform or wait for disk IO. */
internal class PresetRepository(file: File, private val changed: () -> Unit = {}) {
    private val disk = Executors.newSingleThreadExecutor()
    private val store = PresetStore(file)
    @Volatile private var loaded: PresetSnapshot? = null
    init { disk.execute { runCatching { loaded = store.open(); changed() } } }
    fun snapshot(): PresetSnapshot = checkNotNull(loaded) { "Presets unavailable" }
    fun <T> query(work: (PresetStore, PresetSnapshot) -> T, complete: (Result<T>) -> Unit) {
        disk.execute { complete(runCatching { work(store, snapshot()) }) }
    }
    fun publish(snapshot: PresetSnapshot) { loaded = snapshot; changed() }
}
