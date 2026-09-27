package io.github.supermonster003.autojs6.plugin.three.stove.agent.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.provider.Settings
import android.widget.Toast
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.RunError
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.FailureAlertActivity
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.RunDetailActivity

/** Pure decision: which channels report a terminal result. Cancellation and completion are never alerted. */
internal object FailureAlertPolicy {
    fun channels(settings: AgentSettings, status: String, errorCode: String?, dialogAllowed: Boolean): Set<String> {
        val failed = errorCode != null && errorCode != RunError.CANCELLED.name && status in setOf("failed", "blocked", "partial")
        if (!failed) return emptySet()
        val selected = settings.failureAlerts
        val dialog = AgentSettings.ALERT_DIALOG in selected
        return buildSet {
            // A dialog needs the overlay permission on Android 10+ (background activity start); fall back to a notification.
            if (AgentSettings.ALERT_NOTIFICATION in selected || (dialog && !dialogAllowed)) add(AgentSettings.ALERT_NOTIFICATION)
            if (AgentSettings.ALERT_TOAST in selected) add(AgentSettings.ALERT_TOAST)
            if (dialog && dialogAllowed) add(AgentSettings.ALERT_DIALOG)
        }
    }
}

/** Raises the chosen alerts from the :agent process when a task stops abnormally (roadmap P14.1). */
internal class FailureAlerts(private val runtime: AgentRuntime) {
    private val main = Handler(Looper.getMainLooper())

    fun runEnded(runId: String, result: JsonObject) {
        val settings = runCatching { runtime.settings.snapshot() }.getOrNull() ?: return
        val context = runtime.context
        val dialogAllowed = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context)
        val channels = FailureAlertPolicy.channels(settings, result.string("status").orEmpty(), result.getAsJsonObject("error")?.string("code"), dialogAllowed)
        if (channels.isEmpty()) return
        val goal = AgentJson.truncate(runtime.archive.summary(runId)?.string("goal").orEmpty(), 80)
        val summary = AgentJson.truncate(result.string("summary").orEmpty(), 240)
        main.post {
            val title = context.getString(R.string.alert_title, goal)
            if (AgentSettings.ALERT_TOAST in channels) runCatching { Toast.makeText(context, title + "\n" + summary, Toast.LENGTH_LONG).show() }
            if (AgentSettings.ALERT_NOTIFICATION in channels) runCatching { notify(context, runId, title, summary) }
            if (AgentSettings.ALERT_DIALOG in channels) {
                runCatching { context.startActivity(FailureAlertActivity.intent(context, runId, title, summary)) }
                    .onFailure { runCatching { notify(context, runId, title, summary) } }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun notify(context: Context, runId: String, title: String, summary: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.alert_channel), NotificationManager.IMPORTANCE_DEFAULT))
        fun builder() = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL_ID) else Notification.Builder(context)
        val view = PendingIntent.getActivity(context, runId.hashCode(),
            Intent(context, RunDetailActivity::class.java).putExtra("runId", runId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val public = builder().setSmallIcon(R.drawable.ic_warning).setContentTitle(context.getString(R.string.alert_channel)).build()
        manager.notify(NOTIFICATION_TAG, runId.hashCode(), builder().setSmallIcon(R.drawable.ic_warning).setContentTitle(title).setContentText(summary)
            .setStyle(Notification.BigTextStyle().bigText(summary)).setContentIntent(view).setAutoCancel(true)
            .setCategory(Notification.CATEGORY_ERROR).setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(public).build())
    }

    companion object {
        const val CHANNEL_ID = "agent-alerts"
        const val NOTIFICATION_TAG = "failure"
    }
}
