package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.text.InputFilter
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.util.concurrent.TimeUnit

/** Inline interaction, rebuilt only for a new request so polling cannot erase an answer being typed. */
internal class PendingCard(private val container: LinearLayout, private val submit: (JsonObject, (Boolean) -> Unit) -> Unit) {
    private var shown: String? = null
    private var restoredKey: String? = null
    private var restoredAnswer: String? = null
    private var restoredRemember = false
    private var deadline: Long? = null
    private var countdown: TextView? = null
    private var sending = false
    private val buttons = mutableListOf<Button>()
    private fun expired() = deadline?.let { it <= TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) } == true
    private val tick = object : Runnable {
        override fun run() {
            countdown?.let { label ->
                val remaining = (checkNotNull(deadline) - TimeUnit.NANOSECONDS.toMillis(System.nanoTime())).coerceAtLeast(0)
                label.text = if (remaining == 0L) container.context.getString(R.string.interaction_expired) else
                    container.context.getString(R.string.interaction_remaining, (remaining + 999) / 1000)
                buttons.forEach { it.isEnabled = !sending && remaining > 0 }
                if (remaining > 0 && container.isAttachedToWindow) container.postDelayed(this, 500)
            }
        }
    }
    init { container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) { container.removeCallbacks(tick); tick.run() }
        override fun onViewDetachedFromWindow(view: View) { container.removeCallbacks(tick) }
    }) }
    fun restore(state: Bundle?) {
        restoredKey = state?.getString("answerKey"); restoredAnswer = state?.getString("answerDraft")
        restoredRemember = state?.getBoolean("answerRemember") == true
    }
    fun save(state: Bundle) {
        state.putString("answerKey", shown)
        container.findViewById<EditText>(R.id.workbench_answer)?.let { state.putString("answerDraft", it.text.toString()) }
        state.putBoolean("answerRemember", container.findViewById<CheckBox>(R.id.interaction_remember)?.isChecked == true)
    }
    fun render(run: JsonObject?) {
        val pending = run?.getAsJsonObject("pending")
        val key = listOf(run?.string("runId"), pending?.string("requestId"), pending?.flag("submitted"), run?.string("interaction")).toString()
        if (shown == key) return
        shown = key; container.removeCallbacks(tick); countdown = null; deadline = null; sending = false
        buttons.clear(); container.removeAllViews()
        if (pending == null || pending.flag("submitted") == true) return
        val context = container.context
        fun label(text: String) = TextView(context).apply { this.text = text; setTextIsSelectable(true); container.addView(this) }
        if (run.string("interaction") != "plugin") { label(context.getString(R.string.workbench_script_interaction)); return }
        deadline = pending.number("deadlineMs")
        if (deadline != null) countdown = label("").apply { id = R.id.interaction_countdown; setTextIsSelectable(false) }
        var remember: CheckBox? = null
        fun send(value: JsonElement? = null, allowed: Boolean? = null, scope: String = "once") {
            if (sending || expired()) return
            val body = jsonObject("runId" to run["runId"], "requestId" to pending["requestId"])
            if (allowed != null) { body.addProperty("allowed", allowed); body.addProperty("scope", scope) }
            else { body.add("value", value); if (remember?.isChecked == true) body.addProperty("remember", true) }
            sending = true; buttons.forEach { it.isEnabled = false }
            submit(body) { success -> if (shown == key && !success) {
                sending = false; buttons.forEach { it.isEnabled = !expired() }
            } }
        }
        fun button(text: String, click: () -> Unit) { container.addView(Button(context).apply {
            this.text = text; isAllCaps = false; minHeight = (48 * resources.displayMetrics.density).toInt()
            setOnClickListener { click() }; buttons += this
        }) }
        if (pending.string("type") == "confirmation") {
            val risk = when (pending.string("risk")) {
                "read_only" -> R.string.interaction_risk_read_only
                "normal" -> R.string.interaction_risk_normal
                else -> R.string.interaction_risk_sensitive
            }
            label(context.getString(R.string.interaction_risk, context.getString(risk)))
            if (pending.string("tool") == "script_run" && pending.getAsJsonObject("arguments")?.has("parameters") == true)
                container.addView(ScriptConfirmationView.create(context, pending))
            else { label(pending.string("description").orEmpty()); label(pending["arguments"]?.toString().orEmpty()) }
            button(context.getString(R.string.task_allow)) { send(allowed = true) }
            button(context.getString(R.string.task_deny)) { send(allowed = false) }
            if (pending.flag("allowRunScope") == true) button(context.getString(R.string.interaction_allow_run)) { send(allowed = true, scope = "run") }
        } else {
            label(pending.string("question").orEmpty())
            if (pending.has("memoryKey")) {
                remember = CheckBox(context).apply {
                    id = R.id.interaction_remember; setText(R.string.interaction_remember)
                    isEnabled = pending.has("rememberScope"); isChecked = isEnabled && key == restoredKey && restoredRemember
                    container.addView(this)
                }
                label(context.getString(if (remember.isEnabled) R.string.interaction_remember_review else R.string.interaction_memory_disabled))
            }
            when (pending.string("kind")) {
                "choice" -> pending.getAsJsonArray("choices").forEach { choice -> button(choice.asString) { send(choice) } }
                "confirm" -> {
                    button(context.getString(android.R.string.yes)) { send(true.json()) }
                    button(context.getString(android.R.string.no)) { send(false.json()) }
                }
                else -> {
                    val field = EditText(context).apply {
                        id = R.id.workbench_answer; hint = context.getString(R.string.task_reply); minLines = 2; maxLines = 6
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        filters = arrayOf(InputFilter.LengthFilter(1000))
                        if (key == restoredKey) setText(restoredAnswer)
                    }
                    container.addView(field)
                    button(context.getString(R.string.task_reply)) {
                        if (field.text.isNotBlank()) send(field.text.toString().json()) else field.error = context.getString(R.string.workbench_answer_required)
                    }
                }
            }
        }
        tick.run()
    }
}
