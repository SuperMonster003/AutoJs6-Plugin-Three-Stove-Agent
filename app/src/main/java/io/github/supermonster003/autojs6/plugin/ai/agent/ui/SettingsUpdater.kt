package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import io.github.supermonster003.autojs6.plugin.ai.agent.store.AgentSettings

/**
 * Serializes immediate-apply settings changes. Each change is shown at once (optimistically), saved
 * as a whole object one at a time, and composed on the last confirmed value. A failed save drops the
 * pending changes, re-renders the confirmed state and reports the failure.
 */
internal class SettingsUpdater(
    private val save: (AgentSettings, (Boolean) -> Unit) -> Unit,
    private val render: (SettingsDraft) -> Unit,
    private val failed: () -> Unit,
) {
    private val queue = ArrayDeque<(SettingsDraft) -> SettingsDraft>()
    private var inFlight: ((SettingsDraft) -> SettingsDraft)? = null
    var confirmed: SettingsDraft? = null
        private set

    /** What the screen shows: the confirmed value with every pending change applied. */
    val current: SettingsDraft?
        get() = confirmed?.let { base -> (listOfNotNull(inFlight) + queue).fold(base) { draft, change -> change(draft) } }
    val busy: Boolean get() = inFlight != null || queue.isNotEmpty()

    /** Accepts a fresh value from storage unless local changes are still being written. */
    fun load(value: AgentSettings) {
        if (busy) return
        confirmed = SettingsDraft(value)
        render(checkNotNull(current))
    }

    /** Returns false for an invalid change, which is never queued. */
    fun apply(change: (SettingsDraft) -> SettingsDraft): Boolean {
        val base = current ?: return false
        if (runCatching { change(base) }.isFailure) return false
        queue.addLast(change)
        render(checkNotNull(current))
        pump()
        return true
    }

    private fun pump() {
        if (inFlight != null) return
        val base = confirmed ?: return
        val next = queue.removeFirstOrNull() ?: return
        val target = runCatching { next(base) }.getOrElse { rollback(); return }
        inFlight = next
        save(target.settings) { success ->
            inFlight = null
            if (success) { confirmed = target; pump() } else rollback()
        }
    }

    private fun rollback() {
        queue.clear(); inFlight = null
        confirmed?.let(render)
        failed()
    }
}
