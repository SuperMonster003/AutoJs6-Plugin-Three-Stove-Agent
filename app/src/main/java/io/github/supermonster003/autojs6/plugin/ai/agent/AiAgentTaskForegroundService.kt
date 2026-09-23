package io.github.supermonster003.autojs6.plugin.ai.agent

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.Cancellation
import io.github.supermonster003.autojs6.plugin.ai.agent.service.AgentRuntime
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.LauncherActivity

/** Task-only foreground lifetime. The preparation barrier waits until startForeground succeeds. */
class AiAgentTaskForegroundService : Service() {
    private lateinit var runtime: AgentRuntime
    override fun onCreate() { super.onCreate(); runtime = AgentRuntime.get(this); running = this }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) intent.getStringExtra(EXTRA_RUN_ID)?.let { runtime.current?.cancelLocal(it) }
        publish()
        return START_NOT_STICKY
    }
    private fun publish() {
        val runs = runtime.current?.liveRuns().orEmpty()
        if (runs.isEmpty()) {
            // startForegroundService must promote even when cancellation wins before onStartCommand.
            runCatching { promote(notification(null)) }
            complete(false); running = null
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return
        }
        val row = runs.firstOrNull { it.string("state") != "queued" } ?: runs.first()
        try { promote(notification(row)); complete(true) }
        catch (_: Exception) {
            complete(false); runs.forEach { it.string("runId")?.let { id -> runtime.current?.cancelLocal(id) } }
            running = null; stopSelf()
        }
    }
    private fun promote(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIFICATION_ID, notification)
    }
    @Suppress("DEPRECATION")
    private fun notification(row: com.google.gson.JsonObject?): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.task_channel), NotificationManager.IMPORTANCE_LOW))
        fun builder() = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        val view = PendingIntent.getActivity(this, 0, Intent(this, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val waiting = row?.string("state") in setOf("waiting_input", "waiting_confirmation")
        val progress = if (waiting) getString(R.string.task_waiting) else getString(R.string.task_running, (row?.number("step")?.toInt() ?: 0) + 1)
        val summary = AgentJson.truncate(row?.string("goal").orEmpty(), 160)
        val text = row?.string("progress")?.let { AgentJson.truncate(it, 160) } ?: progress
        val public = builder().setSmallIcon(R.drawable.ic_task).setContentTitle(getString(R.string.app_name)).setContentText(progress).build()
        return builder().setSmallIcon(R.drawable.ic_task).setContentTitle(summary.ifBlank { getString(R.string.app_name) })
            .setContentText(text).setStyle(Notification.BigTextStyle().bigText("$progress\n$text"))
            .setContentIntent(view).setOngoing(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(public)
            .setCategory(Notification.CATEGORY_PROGRESS).apply {
                row?.string("runId")?.let { id ->
                    val stop = PendingIntent.getService(this@AiAgentTaskForegroundService, id.hashCode(),
                        Intent(this@AiAgentTaskForegroundService, AiAgentTaskForegroundService::class.java).setAction(ACTION_STOP).putExtra(EXTRA_RUN_ID, id),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    addAction(Notification.Action.Builder(null, getString(R.string.task_stop), stop).build())
                }
                if (waiting) addAction(Notification.Action.Builder(null, getString(R.string.task_view), view).build())
            }.build()
    }
    override fun onDestroy() {
        if (running === this) {
            running = null
            runtime.current?.liveRuns()?.forEach { row -> row.string("runId")?.let { runtime.current?.cancelLocal(it) } }
            complete(false)
        }
        // A retired instance may be destroyed after ensure() has queued the next start.
        // Only the current instance owns those pending callbacks.
        super.onDestroy()
    }
    companion object {
        private const val CHANNEL_ID = "agent-tasks"
        private const val NOTIFICATION_ID = 551
        private const val ACTION_STOP = "io.github.supermonster003.autojs6.plugin.ai.agent.STOP_TASK"
        private const val EXTRA_RUN_ID = "runId"
        private val main = Handler(Looper.getMainLooper())
        private var running: AiAgentTaskForegroundService? = null
        private val waiting = linkedMapOf<Any, (Boolean) -> Unit>()
        private fun complete(ok: Boolean) { val callbacks = waiting.values.toList(); waiting.clear(); callbacks.forEach { it(ok) } }
        internal fun ensure(context: Context, callback: (Boolean) -> Unit): Cancellation {
            val token = Any()
            val cancelled = java.util.concurrent.atomic.AtomicBoolean()
            main.post {
                if (cancelled.get()) return@post
                waiting[token] = { if (!cancelled.get()) callback(it) }
                val service = running
                if (service != null) service.publish()
                else try {
                    val intent = Intent(context, AiAgentTaskForegroundService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
                } catch (_: Exception) { complete(false) }
            }
            return Cancellation { cancelled.set(true); main.post { waiting.remove(token) } }
        }
        internal fun changed() { main.post { running?.publish() } }
    }
}
