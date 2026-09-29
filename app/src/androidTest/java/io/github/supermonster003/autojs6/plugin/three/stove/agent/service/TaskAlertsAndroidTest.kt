package io.github.supermonster003.autojs6.plugin.three.stove.agent.service

import android.app.Notification
import android.app.NotificationManager
import android.content.ContextWrapper
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.SettingsStore
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.FailureAlertActivity
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.RunDetailActivity
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

/** Isolated settings and run ids; never starts a real agent task or changes personal preferences. */
class TaskAlertsAndroidTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun waitFor(label: String, ready: () -> Boolean) {
        val until = SystemClock.elapsedRealtime() + 10000
        while (SystemClock.elapsedRealtime() < until) { if (ready()) return; SystemClock.sleep(40) }
        fail(label)
    }

    @Test fun completionNotificationUsesItsOwnChannelProtectsContentAndOpensTheTask() {
        val directory = File(context.cacheDir, "task-alerts-${UUID.randomUUID()}").apply { check(mkdirs()) }
        val fixture = object : ContextWrapper(context) { override fun getFilesDir() = directory }
        SettingsStore(File(directory, "agent-settings.json")).save(AgentSettings(
            failureAlerts = emptySet(), completionAlerts = setOf(AgentSettings.ALERT_NOTIFICATION)))
        val runtime = AgentRuntime(fixture)
        val id = UUID.randomUUID().toString()
        val manager = context.getSystemService(NotificationManager::class.java)
        fun notification() = manager.activeNotifications.firstOrNull { it.tag == TaskAlerts.COMPLETION_NOTIFICATION_TAG && it.id == id.hashCode() }
        try {
            assertTrue("Notification permission must be granted by the test environment", manager.areNotificationsEnabled())
            waitFor("Settings loaded") { runCatching { runtime.settings.snapshot() }.isSuccess }
            runtime.alerts.runEnded(id, jsonObject("status" to "completed".json(), "summary" to "Completion fixture summary".json()))
            waitFor("Completion notification posted") { notification() != null }
            val posted = notification()!!.notification
            assertEquals(Notification.CATEGORY_STATUS, posted.category)
            assertEquals(Notification.VISIBILITY_PRIVATE, posted.visibility)
            assertEquals("Completion fixture summary", posted.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertFalse(posted.publicVersion.extras.toString().contains("Completion fixture summary"))
            if (android.os.Build.VERSION.SDK_INT >= 26) assertEquals(TaskAlerts.COMPLETION_CHANNEL_ID, posted.channelId)
            val monitor = instrumentation.addMonitor(RunDetailActivity::class.java.name, null, false)
            try {
                posted.contentIntent.send()
                val opened = monitor.waitForActivityWithTimeout(10000)
                assertNotNull(opened); assertEquals(id, opened.intent.getStringExtra("runId"))
                instrumentation.runOnMainSync { opened.finish() }
            } finally { instrumentation.removeMonitor(monitor) }
            manager.cancel(TaskAlerts.COMPLETION_NOTIFICATION_TAG, id.hashCode())
            runtime.alerts.runEnded(id, jsonObject("status" to "cancelled".json(), "error" to jsonObject("code" to "CANCELLED".json())))
            runtime.status()
            runtime.archive.summary(id)
            instrumentation.waitForIdleSync()
            assertNull("Cancellation and querying history must not post a completion", notification())
        } finally {
            manager.cancel(TaskAlerts.COMPLETION_NOTIFICATION_TAG, id.hashCode())
            runtime.memories.close(); directory.deleteRecursively()
        }
    }

    @Test fun completionDialogShowsTheResultAndCanBeDismissed() {
        val intent = FailureAlertActivity.intent(context, UUID.randomUUID().toString(), "Task finished", "Completion dialog fixture", completion = true)
        ActivityScenario.launch<FailureAlertActivity>(intent).use { scenario ->
            scenario.onActivity {
                val root = it.findViewById<View>(android.R.id.content)
                assertNotNull(root.findViewWithTag<View>("completion-alert"))
                assertNull(root.findViewWithTag<View>("failure-alert"))
                assertEquals("Completion dialog fixture", root.findViewWithTag<TextView>("completion-summary").text.toString())
                root.findViewWithTag<View>("alert-close").performClick()
            }
        }
    }
}
