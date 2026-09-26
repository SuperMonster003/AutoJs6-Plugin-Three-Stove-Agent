package io.github.supermonster003.autojs6.plugin.ai.agent.update

/** Manual results cache for 24 hours. Opt-in foreground checks attempt at most every 12 hours. */
internal object UpdateSchedulePolicy {
    const val INTERVAL_MS = 24L * 60 * 60 * 1000
    fun manualFetchDue(last: Long?, now: Long) = last == null || now < last || now - last >= INTERVAL_MS
    const val AUTOMATIC_INTERVAL_MS = 12L * 60 * 60 * 1000
    fun automaticFetchDue(enabled: Boolean, lastAttempt: Long?, now: Long) = enabled &&
        (lastAttempt == null || now < lastAttempt || now - lastAttempt >= AUTOMATIC_INTERVAL_MS)
}
