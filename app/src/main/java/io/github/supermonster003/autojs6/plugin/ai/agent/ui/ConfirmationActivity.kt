package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.PendingIntent
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C

/** A request-specific entry shared by notifications and the later opt-in floating card. */
class ConfirmationActivity : HostAppearanceActivity() {
    override val dialogTheme = true
    private lateinit var agent: AgentConnection
    private lateinit var card: PendingCard
    private lateinit var content: LinearLayout
    private lateinit var message: TextView
    private val visibility by lazy { InteractionVisibility(this) }
    private var runId: String? = null
    private var requestId: String? = null
    private var displayedStep = 0L
    private var memoryAfterStep = Long.MAX_VALUE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setFinishOnTouchOutside(false)
        title = getString(R.string.app_name)
        runId = intent.getStringExtra(EXTRA_RUN_ID)?.takeIf { it.length in 1..128 }
        requestId = (savedInstanceState?.getString("currentRequest") ?: intent.getStringExtra(EXTRA_REQUEST_ID))?.takeIf { it.length in 1..128 }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; layoutDirection = resources.configuration.layoutDirection
            val padding = (20 * resources.displayMetrics.density).toInt(); setPadding(padding, padding, padding, padding)
        }
        message = TextView(this).apply { setText(R.string.interaction_loading); content.addView(this) }
        val pending = LinearLayout(this).apply { id = R.id.workbench_pending; orientation = LinearLayout.VERTICAL; content.addView(this) }
        content.addView(Button(this).apply { setText(R.string.interaction_later); isAllCaps = false; setOnClickListener { finish() } })
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(content) })
        agent = AgentConnection(this, ::render).apply { selectedId = runId; preferRunning = false }
        card = PendingCard(pending) { body, complete ->
            if (body.flag("remember") == true) memoryAfterStep = displayedStep + 1
            agent.command({ it.respond(AgentConnection.request(C.KEY_RUN_RESPONSE_JSON, body)) }) { result ->
                complete(result.isSuccess)
                if (result.isSuccess) {
                    // A checked answer creates a fresh request; follow its separate memory confirmation.
                    if (body.flag("remember") == true) requestId = null else finish()
                } else { message.setText(R.string.workbench_request_failed); message.visibility = View.VISIBLE }
            }
        }
        card.restore(savedInstanceState)
        memoryAfterStep = savedInstanceState?.getLong("memoryAfterStep", Long.MAX_VALUE) ?: Long.MAX_VALUE
        if (savedInstanceState?.getBoolean("awaitingMemory") == true) requestId = null
        tint(content)
        if (runId == null || intent.getStringExtra(EXTRA_REQUEST_ID)?.takeIf { it.length in 1..128 } == null) finish()
    }
    override fun onStart() { super.onStart(); agent.start() }
    override fun onResume() { super.onResume(); visibility.start() }
    override fun onPause() { visibility.stop(); super.onPause() }
    override fun onStop() { agent.stop(); super.onStop() }
    override fun onDestroy() { agent.close(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        card.save(outState); outState.putBoolean("awaitingMemory", requestId == null)
        outState.putString("currentRequest", requestId)
        outState.putLong("memoryAfterStep", memoryAfterStep)
        super.onSaveInstanceState(outState)
    }
    private fun render(snapshot: WorkbenchSnapshot) {
        val row = snapshot.run?.takeIf { it.string("runId") == runId }
        displayedStep = row?.number("step") ?: 0L
        val pending = row?.getAsJsonObject("pending")
        // If preparation rejected the user proposal, there is no confirmation to follow.
        if (requestId == null && row?.getAsJsonArray("steps")?.lastOrNull()?.asJsonObject?.let {
                (it.number("index") ?: 0) > memoryAfterStep && it.string("tool") == "memory_propose" &&
                    it.getAsJsonObject("decision")?.string("source") == "user"
            } == true) { finish(); return }
        if (requestId == null && pending?.string("tool") == "memory_propose" && pending.flag("submitted") != true)
            requestId = pending.string("requestId")
        val matches = pending != null && pending.string("requestId") == requestId && pending.flag("submitted") != true
        val display = row?.takeIf { matches }
        card.render(display); visibility.render(display)
        message.visibility = if (display == null) View.VISIBLE else View.GONE
        if (display == null) message.setText(if (requestId == null && row != null && WorkbenchText.active(row))
            R.string.interaction_loading else R.string.interaction_expired)
        tint(content)
    }
    companion object {
        private const val EXTRA_RUN_ID = "runId"
        private const val EXTRA_REQUEST_ID = "requestId"
        internal fun intent(context: Context, runId: String, requestId: String) = Intent(context, ConfirmationActivity::class.java)
            .setData(Uri.Builder().scheme("agent-interaction").authority("request").appendPath(runId).appendPath(requestId).build())
            .putExtra(EXTRA_RUN_ID, runId).putExtra(EXTRA_REQUEST_ID, requestId)
        internal fun pendingIntent(context: Context, runId: String, requestId: String): PendingIntent = PendingIntent.getActivity(context, 0,
            intent(context, runId, requestId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
