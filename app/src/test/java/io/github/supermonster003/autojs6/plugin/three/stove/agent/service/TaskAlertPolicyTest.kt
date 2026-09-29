package io.github.supermonster003.autojs6.plugin.three.stove.agent.service

import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import org.junit.Assert.*
import org.junit.Test

class TaskAlertPolicyTest {
    private val all = AgentSettings(failureAlerts = AgentSettings.ALERT_CHANNELS, completionAlerts = AgentSettings.ALERT_CHANNELS)

    @Test fun terminalResultsSelectOneEventAndCancellationStaysSilent() {
        assertEquals(AgentSettings.ALERT_CHANNELS, TaskAlertPolicy.channels(all, "completed", null, true))
        assertEquals(emptySet<String>(), TaskAlertPolicy.channels(all, "cancelled", "CANCELLED", true))
        assertEquals(AgentSettings.ALERT_CHANNELS, TaskAlertPolicy.channels(all, "partial", null, true))
        assertEquals(AgentSettings.ALERT_CHANNELS, TaskAlertPolicy.channels(all, "failed", "MODEL_FAILED", true))
        assertEquals(AgentSettings.ALERT_CHANNELS, TaskAlertPolicy.channels(all, "blocked", "HOST_UNAVAILABLE", true))
        assertEquals(AgentSettings.ALERT_CHANNELS, TaskAlertPolicy.channels(all, "partial", "BUDGET_EXCEEDED", true))
        assertEquals(TaskAlertKind.FAILURE, TaskAlertPolicy.kind("partial", "BUDGET_EXCEEDED"))
        assertEquals(TaskAlertKind.COMPLETION, TaskAlertPolicy.kind("partial", null))
        for (status in listOf("running", "queued", "waiting_input", "cancelled", "unknown"))
            assertEquals(emptySet<String>(), TaskAlertPolicy.channels(all, status, null, true))
        assertNull(TaskAlertPolicy.kind("completed", "MODEL_FAILED"))
        assertNull(TaskAlertPolicy.kind("failed", "CANCELLED"))
    }

    @Test fun failureDefaultsToAllChannelsAndFallsBackWhenADialogCannotAppear() {
        assertEquals(AgentSettings.ALERT_CHANNELS, TaskAlertPolicy.channels(AgentSettings(), "failed", "MODEL_FAILED", true))
        assertEquals(emptySet<String>(), TaskAlertPolicy.channels(AgentSettings(failureAlerts = emptySet()), "failed", "MODEL_FAILED", true))
        val dialogOnly = AgentSettings(failureAlerts = setOf(AgentSettings.ALERT_DIALOG))
        assertEquals(setOf(AgentSettings.ALERT_DIALOG), TaskAlertPolicy.channels(dialogOnly, "failed", "MODEL_FAILED", true))
        assertEquals(setOf(AgentSettings.ALERT_NOTIFICATION), TaskAlertPolicy.channels(dialogOnly, "failed", "MODEL_FAILED", false))
        val toastOnly = AgentSettings(failureAlerts = setOf(AgentSettings.ALERT_TOAST))
        assertEquals(setOf(AgentSettings.ALERT_TOAST), TaskAlertPolicy.channels(toastOnly, "failed", "MODEL_FAILED", false))
    }

    @Test fun completionDefaultsToNotificationAndToastWithIndependentChoicesAndDialogFallback() {
        val defaults = AgentSettings()
        assertEquals(setOf(AgentSettings.ALERT_NOTIFICATION, AgentSettings.ALERT_TOAST), TaskAlertPolicy.channels(defaults, "completed", null, true))
        val chosen = defaults.copy(failureAlerts = setOf(AgentSettings.ALERT_TOAST), completionAlerts = setOf(AgentSettings.ALERT_DIALOG))
        assertEquals(setOf(AgentSettings.ALERT_TOAST), TaskAlertPolicy.channels(chosen, "failed", "MODEL_FAILED", true))
        assertEquals(setOf(AgentSettings.ALERT_DIALOG), TaskAlertPolicy.channels(chosen, "completed", null, true))
        assertEquals(setOf(AgentSettings.ALERT_NOTIFICATION), TaskAlertPolicy.channels(chosen, "completed", null, false))
        assertEquals(emptySet<String>(), TaskAlertPolicy.channels(defaults.copy(completionAlerts = emptySet()), "completed", null, false))
    }
}
