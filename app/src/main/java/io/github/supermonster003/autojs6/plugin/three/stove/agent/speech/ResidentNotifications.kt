package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.LauncherActivity
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.SettingsActivity
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.TaskEntries
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.TaskEntry
import java.util.Date

/** Notifications of the resident listener (roadmap D63, D64, D66, D67, D76, D81); the draft text is never logged. */
internal object ResidentNotifications {
    const val LISTENING_ID = 552
    private const val DRAFT_ID = 553
    private const val STOPPED_ID = 554
    private const val RESUME_ID = 555
    private const val LISTENING_CHANNEL = "resident-listener"
    private const val ALERT_CHANNEL = "resident-listener-alerts"
    private const val DRAFT_CHANNEL = "voice-drafts"
    private const val DRAFT_TIMEOUT_MILLIS = 10 * 60 * 1000L

    /** [PAUSED]: unplugged with "only while charging"; [BLOCKED]: the wake session waits for a visible screen (D82). */
    enum class State { CONNECTING, WAITING, LISTENING, PAUSED, BLOCKED }

    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)

    private fun builder(context: Context, channel: String): Notification.Builder {
        if (Build.VERSION.SDK_INT >= 26) {
            val (name, importance) = when (channel) {
                LISTENING_CHANNEL -> R.string.settings_resident to NotificationManager.IMPORTANCE_LOW
                DRAFT_CHANNEL -> R.string.voice_draft_channel to NotificationManager.IMPORTANCE_HIGH
                else -> R.string.resident_alert_channel to NotificationManager.IMPORTANCE_DEFAULT
            }
            manager(context).createNotificationChannel(NotificationChannel(channel, context.getString(name), importance))
            return Notification.Builder(context, channel)
        }
        @Suppress("DEPRECATION") return Notification.Builder(context)
    }

    private fun settings(context: Context, request: Int) = PendingIntent.getActivity(context, request,
        Intent(context, SettingsActivity::class.java).putExtra(SettingsActivity.EXTRA_FOCUS, SettingsActivity.FOCUS_RESIDENT)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** The ongoing notification of the foreground service, with Turn off; [deadline] is when the listening time ends (D81). */
    fun listening(context: Context, state: State, keyword: String, deadline: Long?): Notification {
        val text = when (state) {
            State.CONNECTING -> context.getString(R.string.resident_connecting)
            State.WAITING -> context.getString(R.string.resident_waiting, keyword)
            State.LISTENING -> context.getString(R.string.resident_listening)
            State.PAUSED -> context.getString(R.string.resident_paused)
            State.BLOCKED -> context.getString(R.string.resident_blocked)
        }
        val off = PendingIntent.getService(context, 0, Intent(context, ResidentListenerService::class.java)
            .setAction(ResidentListenerService.ACTION_TURN_OFF), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return builder(context, LISTENING_CHANNEL).setSmallIcon(R.drawable.ic_mic).setContentTitle(context.getString(R.string.settings_resident))
            .setContentText(text).setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(settings(context, 1)).setOngoing(true).setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_PUBLIC)
            .apply { deadline?.let { setSubText(until(context, it)) } }
            .addAction(Notification.Action.Builder(null, context.getString(R.string.resident_turn_off), off).build())
            .apply { if (Build.VERSION.SDK_INT >= 31) setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE) }
            .build()
    }

    fun updateListening(context: Context, state: State, keyword: String, deadline: Long?) =
        runCatching { manager(context).notify(LISTENING_ID, listening(context, state, keyword, deadline)) }

    /** "Turns off at 10:42" in the device's time format (D81). */
    fun until(context: Context, deadline: Long): String =
        context.getString(R.string.resident_until, DateFormat.getTimeFormat(context).format(Date(deadline)))

    /** "Draft ready" (D63, D64): the first 40 code points, nothing on the lock screen, gone after ten minutes. */
    fun draft(context: Context, text: String) {
        val open = PendingIntent.getActivity(context, text.hashCode(), TaskEntries.intent(context, TaskEntry(text)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = context.getString(R.string.voice_draft_ready)
        val public = builder(context, DRAFT_CHANNEL).setSmallIcon(R.drawable.ic_mic).setContentTitle(title).build()
        val notification = builder(context, DRAFT_CHANNEL).setSmallIcon(R.drawable.ic_mic).setContentTitle(title)
            .setContentText(VoiceDraftRoute.preview(text)).setContentIntent(open).setAutoCancel(true)
            .setCategory(Notification.CATEGORY_MESSAGE).setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(public)
            .apply {
                // API 24 / 25 have no channels: the priority makes it a heads-up notification.
                if (Build.VERSION.SDK_INT >= 26) setTimeoutAfter(DRAFT_TIMEOUT_MILLIS)
                else @Suppress("DEPRECATION") setPriority(Notification.PRIORITY_HIGH)
            }.build()
        runCatching { manager(context).notify(DRAFT_ID, notification) }
    }

    /** The listener stopped on its own (D76); the switch stays on and the next visible start retries. */
    fun stopped(context: Context, reason: ResidentStop, other: String?) {
        val text = when (reason) {
            ResidentStop.CONFLICT -> context.getString(R.string.resident_stopped_conflict, other.orEmpty())
            ResidentStop.UNAVAILABLE -> context.getString(R.string.resident_stopped_unavailable)
            ResidentStop.INCOMPATIBLE -> context.getString(R.string.resident_stopped_incompatible)
            ResidentStop.MICROPHONE -> context.getString(R.string.resident_stopped_microphone)
            ResidentStop.MODEL -> context.getString(R.string.resident_stopped_model)
            ResidentStop.KEYWORDS -> context.getString(R.string.resident_stopped_keywords)
            ResidentStop.BUSY -> context.getString(R.string.resident_stopped_busy)
            ResidentStop.ENDED -> context.getString(R.string.resident_stopped_ended)
        } + "\n" + context.getString(R.string.resident_stopped_retry)
        val notification = builder(context, ALERT_CHANNEL).setSmallIcon(R.drawable.ic_mic).setContentTitle(context.getString(R.string.resident_stopped_title))
            .setContentText(text).setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(settings(context, 2)).setAutoCancel(true)
            .setCategory(Notification.CATEGORY_STATUS).build()
        runCatching { manager(context).notify(STOPPED_ID, notification) }
    }

    /** After a reboot or an update (D67): opening the workbench resumes the listener. */
    fun resume(context: Context) {
        val open = PendingIntent.getActivity(context, 3, Intent(context, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = builder(context, ALERT_CHANNEL).setSmallIcon(R.drawable.ic_mic).setContentTitle(context.getString(R.string.resident_resume_title))
            .setContentText(context.getString(R.string.resident_resume_text)).setContentIntent(open).setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER).build()
        runCatching { manager(context).notify(RESUME_ID, notification) }
    }

    /** The listener runs again or was turned off: the stop and resume reminders are stale. */
    fun cancelReminders(context: Context) = runCatching { manager(context).cancel(STOPPED_ID); manager(context).cancel(RESUME_ID) }
}
