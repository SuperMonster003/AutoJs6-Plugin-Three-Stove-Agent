package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.SettingsCodec

internal enum class AccessMode { STANDARD, CAUTIOUS, FULL }

/**
 * Immediate-apply edits of the private task settings. Each change produces a complete, valid
 * [AgentSettings]; the screen shows durations in minutes while storage keeps milliseconds.
 */
internal data class SettingsDraft(val settings: AgentSettings) {
    val accessMode: AccessMode get() = when {
        settings.fullAccess -> AccessMode.FULL
        settings.cautious -> AccessMode.CAUTIOUS
        else -> AccessMode.STANDARD
    }

    fun withAccess(mode: AccessMode) = copy(settings = settings.copy(cautious = mode == AccessMode.CAUTIOUS, fullAccess = mode == AccessMode.FULL))

    fun withGroup(id: String, enabled: Boolean): SettingsDraft {
        require(ToolGroup.entries.any { it.id == id })
        return copy(settings = settings.copy(toolGroups = if (enabled) settings.toolGroups + id else settings.toolGroups - id))
    }

    fun limit(key: String): Long? = settings.budget[key]

    /** [value] null restores the automatic default for [key]. */
    fun withLimit(key: String, value: Long?): SettingsDraft {
        val ceiling = requireNotNull(SettingsCodec.ceilings[key])
        require(value == null || value in 1..ceiling)
        return copy(settings = settings.copy(budget = if (value == null) settings.budget - key else settings.budget + (key to value)))
    }

    /** Whole minutes, rounded up so a stored legacy millisecond value never displays as zero. */
    fun durationMinutes(): Long? = settings.budget[DURATION]?.let { (it + 59_999) / 60_000 }

    fun withDurationMinutes(minutes: Long?): SettingsDraft = withLimit(DURATION, minutes?.let {
        require(it in 1..MAX_DURATION_MINUTES); it * 60_000
    })

    fun withVoice(enabled: Boolean) = copy(settings = settings.copy(voice = enabled))
    fun withFailureAlert(channel: String, enabled: Boolean): SettingsDraft {
        require(channel in AgentSettings.ALERT_CHANNELS)
        return copy(settings = settings.copy(failureAlerts = if (enabled) settings.failureAlerts + channel else settings.failureAlerts - channel))
    }
    fun withFloating(enabled: Boolean) = copy(settings = settings.copy(floating = enabled))

    companion object {
        const val DURATION = "maxDurationMs"
        val MAX_DURATION_MINUTES = requireNotNull(SettingsCodec.ceilings[DURATION]) / 60_000
    }
}
