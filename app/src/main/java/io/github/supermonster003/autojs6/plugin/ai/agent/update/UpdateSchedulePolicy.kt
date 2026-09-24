package io.github.supermonster003.autojs6.plugin.ai.agent.update

/** Manual-only, cached for 24 hours after success. Failures and cancellations do not consume the cache. */
internal object UpdateSchedulePolicy {
    const val INTERVAL_MS = 24L * 60 * 60 * 1000
    fun manualFetchDue(last: Long?, now: Long) = last == null || now < last || now - last >= INTERVAL_MS
    fun automaticCheckAllowed(metered: Boolean?): Boolean = false // No automatic checks, including unknown or metered networks.
}
