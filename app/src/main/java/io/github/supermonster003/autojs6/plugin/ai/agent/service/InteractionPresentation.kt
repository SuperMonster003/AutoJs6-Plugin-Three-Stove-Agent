package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.app.*
import android.content.Context
import android.os.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.ConfirmationActivity
import java.util.concurrent.TimeUnit

/** Main-thread presentation state in :agent. The runner remains the sole owner of deadlines and replies. */
internal class InteractionPresentation(private val runtime: AgentRuntime) : IInteractionUi.Stub() {
    private val main = Handler(Looper.getMainLooper())
    private data class Lease(val runId: String, val requestId: String, val death: IBinder.DeathRecipient)
    private val visible = mutableMapOf<IBinder, Lease>()
    private val context: Context get() = runtime.context
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private var posted: String? = null
    init { manager.cancel(NOTIFICATION_ID) }

    override fun present(owner: IBinder?, runId: String?, requestId: String?) {
        if (Binder.getCallingUid() != Process.myUid()) throw SecurityException("Private interaction UI")
        require(owner != null && (runId == null || runId.length in 1..128) && (requestId == null || requestId.length in 1..128))
        main.post {
            visible.remove(owner)?.let { runCatching { owner.unlinkToDeath(it.death, 0) } }
            if (runId != null && requestId != null && visible.size < 16) {
                val death = IBinder.DeathRecipient { main.post { visible.remove(owner); publish() } }
                runCatching { owner.linkToDeath(death, 0); visible[owner] = Lease(runId, requestId, death) }
            }
            publish()
        }
    }
    fun changed() { main.post(::publish) }

    @Suppress("DEPRECATION")
    private fun publish() {
        val waiting = runtime.current?.liveRuns().orEmpty().firstNotNullOfOrNull { row ->
            val id = row.string("runId") ?: return@firstNotNullOfOrNull null
            val pending = runtime.archive.pendingForUi(id) ?: return@firstNotNullOfOrNull null
            val request = pending.string("requestId") ?: return@firstNotNullOfOrNull null
            if (runtime.archive.interaction(id) != "plugin" || pending.flag("submitted") == true ||
                visible.values.any { it.runId == id && (it.requestId == request || it.requestId == "*") }) null else Triple(id, request, pending)
        }
        val remaining = waiting?.third?.number("deadlineMs")?.minus(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())) ?: 0
        if (waiting == null || remaining <= 0) {
            manager.cancel(NOTIFICATION_ID); posted = null; return
        }
        val (id, request, pending) = waiting
        if (posted == request) return
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, context.getString(R.string.interaction_channel), NotificationManager.IMPORTANCE_HIGH))
        fun builder() = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL_ID) else Notification.Builder(context)
        val confirmation = pending.string("type") == "confirmation"
        val title = context.getString(if (confirmation) R.string.interaction_confirmation else R.string.interaction_question)
        val content = AgentJson.truncate(pending.string(if (confirmation) "description" else "question").orEmpty(), 160)
        val view = ConfirmationActivity.pendingIntent(context, id, request)
        val public = builder().setSmallIcon(R.drawable.ic_task).setContentTitle(context.getString(R.string.app_name))
            .setContentText(title).build()
        val notification = builder().setSmallIcon(R.drawable.ic_task).setContentTitle(title).setContentText(content)
            .setStyle(Notification.BigTextStyle().bigText(content)).setContentIntent(view)
            .setCategory(Notification.CATEGORY_REMINDER).setPriority(Notification.PRIORITY_HIGH)
            .setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(public).setOnlyAlertOnce(true).setOngoing(true)
            .addAction(Notification.Action.Builder(null, context.getString(if (confirmation) R.string.task_view else R.string.task_reply), view).build())
            .apply { if (Build.VERSION.SDK_INT >= 26) setTimeoutAfter(remaining) }.build()
        // Notification denial must never change the confirmation decision or abort the task.
        runCatching { manager.notify(NOTIFICATION_ID, notification); posted = request }
    }
    companion object {
        const val ACTION = "io.github.supermonster003.autojs6.plugin.ai.agent.INTERACTION_UI"
        const val CHANNEL_ID = "agent-interactions"
        const val NOTIFICATION_ID = 552
    }
}
