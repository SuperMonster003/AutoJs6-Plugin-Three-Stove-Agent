package io.github.supermonster003.autojs6.plugin.three.stove.agent.service

import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import org.junit.Assert.*
import org.junit.Test

class FailureAlertPolicyTest {
    private val all = AgentSettings(failureAlerts = AgentSettings.ALERT_CHANNELS)

    @Test fun onlyAbnormalStopsAreAlerted() {
        assertEquals(emptySet<String>(), FailureAlertPolicy.channels(all, "completed", null, true))
        assertEquals(emptySet<String>(), FailureAlertPolicy.channels(all, "cancelled", "CANCELLED", true))
        assertEquals(emptySet<String>(), FailureAlertPolicy.channels(all, "partial", null, true))
        assertEquals(AgentSettings.ALERT_CHANNELS, FailureAlertPolicy.channels(all, "failed", "MODEL_FAILED", true))
        assertEquals(AgentSettings.ALERT_CHANNELS, FailureAlertPolicy.channels(all, "blocked", "HOST_UNAVAILABLE", true))
        assertEquals(AgentSettings.ALERT_CHANNELS, FailureAlertPolicy.channels(all, "partial", "BUDGET_EXCEEDED", true))
    }

    @Test fun defaultsToNotificationAndFallsBackWhenADialogCannotAppear() {
        assertEquals(setOf(AgentSettings.ALERT_NOTIFICATION), FailureAlertPolicy.channels(AgentSettings(), "failed", "MODEL_FAILED", true))
        assertEquals(emptySet<String>(), FailureAlertPolicy.channels(AgentSettings(failureAlerts = emptySet()), "failed", "MODEL_FAILED", true))
        val dialogOnly = AgentSettings(failureAlerts = setOf(AgentSettings.ALERT_DIALOG))
        assertEquals(setOf(AgentSettings.ALERT_DIALOG), FailureAlertPolicy.channels(dialogOnly, "failed", "MODEL_FAILED", true))
        assertEquals(setOf(AgentSettings.ALERT_NOTIFICATION), FailureAlertPolicy.channels(dialogOnly, "failed", "MODEL_FAILED", false))
        val toastOnly = AgentSettings(failureAlerts = setOf(AgentSettings.ALERT_TOAST))
        assertEquals(setOf(AgentSettings.ALERT_TOAST), FailureAlertPolicy.channels(toastOnly, "failed", "MODEL_FAILED", false))
    }
}
