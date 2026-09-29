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

internal enum class TaskAlertKind { FAILURE, COMPLETION }

/** Pure decision for live terminal events. Cancellation and nonterminal snapshots never alert. */
internal object TaskAlertPolicy {
    fun kind(status: String, errorCode: String?): TaskAlertKind? = when {
        errorCode == RunError.CANCELLED.name -> null
        errorCode != null && status in setOf("failed", "blocked", "partial") -> TaskAlertKind.FAILURE
        errorCode == null && status in setOf("completed", "partial") -> TaskAlertKind.COMPLETION
        else -> null
    }

    fun channels(settings: AgentSettings, status: String, errorCode: String?, dialogAllowed: Boolean): Set<String> {
        val selected = when (kind(status, errorCode)) {
            TaskAlertKind.FAILURE -> settings.failureAlerts
            TaskAlertKind.COMPLETION -> settings.completionAlerts
            null -> return emptySet()
        }
        val dialog = AgentSettings.ALERT_DIALOG in selected
        return buildSet {
            // A dialog needs the overlay permission on Android 10+ (background activity start); fall back to a notification.
            if (AgentSettings.ALERT_NOTIFICATION in selected || (dialog && !dialogAllowed)) add(AgentSettings.ALERT_NOTIFICATION)
            if (AgentSettings.ALERT_TOAST in selected) add(AgentSettings.ALERT_TOAST)
            if (dialog && dialogAllowed) add(AgentSettings.ALERT_DIALOG)
        }
    }
}

/** Raises the chosen alerts from the :agent process only for a new terminal event, never history. */
internal class TaskAlerts(private val runtime: AgentRuntime) {
    private val main = Handler(Looper.getMainLooper())

    fun runEnded(runId: String, result: JsonObject) {
        val settings = runCatching { runtime.settings.snapshot() }.getOrNull() ?: return
        val context = runtime.context
        val status = result.string("status").orEmpty()
        val errorCode = result.getAsJsonObject("error")?.string("code")
        val kind = TaskAlertPolicy.kind(status, errorCode) ?: return
        val dialogAllowed = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context)
        val channels = TaskAlertPolicy.channels(settings, status, errorCode, dialogAllowed)
        if (channels.isEmpty()) return
        val goal = AgentJson.truncate(runtime.archive.summary(runId)?.string("goal").orEmpty(), 80)
        val summary = AgentJson.truncate(result.string("summary").orEmpty(), 240)
        main.post {
            val title = context.getString(if (kind == TaskAlertKind.COMPLETION) R.string.alert_completion_title else R.string.alert_title, goal)
            if (AgentSettings.ALERT_TOAST in channels) runCatching { Toast.makeText(context, title + "\n" + summary, Toast.LENGTH_LONG).show() }
            if (AgentSettings.ALERT_NOTIFICATION in channels) runCatching { notify(context, runId, title, summary, kind) }
            if (AgentSettings.ALERT_DIALOG in channels) {
                runCatching { context.startActivity(FailureAlertActivity.intent(context, runId, title, summary, kind == TaskAlertKind.COMPLETION)) }
                    .onFailure { runCatching { notify(context, runId, title, summary, kind) } }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun notify(context: Context, runId: String, title: String, summary: String, kind: TaskAlertKind) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val completion = kind == TaskAlertKind.COMPLETION
        val channelId = if (completion) COMPLETION_CHANNEL_ID else CHANNEL_ID
        val channelName = context.getString(if (completion) R.string.alert_completion_channel else R.string.alert_channel)
        val icon = if (completion) R.drawable.ic_check else R.drawable.ic_warning
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT))
        fun builder() = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, channelId) else Notification.Builder(context)
        val view = PendingIntent.getActivity(context, runId.hashCode(),
            Intent(context, RunDetailActivity::class.java).putExtra("runId", runId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val public = builder().setSmallIcon(icon).setContentTitle(channelName).build()
        manager.notify(if (completion) COMPLETION_NOTIFICATION_TAG else NOTIFICATION_TAG, runId.hashCode(), builder().setSmallIcon(icon).setContentTitle(title).setContentText(summary)
            .setStyle(Notification.BigTextStyle().bigText(summary)).setContentIntent(view).setAutoCancel(true)
            .setCategory(if (completion) Notification.CATEGORY_STATUS else Notification.CATEGORY_ERROR)
            .setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(public).build())
    }

    companion object {
        const val CHANNEL_ID = "agent-alerts"
        const val NOTIFICATION_TAG = "failure"
        const val COMPLETION_CHANNEL_ID = "agent-completions"
        const val COMPLETION_NOTIFICATION_TAG = "completion"
    }
}
