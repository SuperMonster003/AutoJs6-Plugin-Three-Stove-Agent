package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.text.InputFilter
import android.os.Bundle
import android.text.InputType
import android.widget.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

/** Inline interaction, rebuilt only for a new request so polling cannot erase an answer being typed. */
internal class PendingCard(private val container: LinearLayout, private val submit: (JsonObject, (Boolean) -> Unit) -> Unit) {
    private var shown: String? = null
    private var restoredKey: String? = null
    private var restoredAnswer: String? = null
    fun restore(state: Bundle?) { restoredKey = state?.getString("answerKey"); restoredAnswer = state?.getString("answerDraft") }
    fun save(state: Bundle) {
        container.findViewById<EditText>(R.id.workbench_answer)?.let { state.putString("answerKey", shown); state.putString("answerDraft", it.text.toString()) }
    }
    fun render(run: JsonObject?) {
        val pending = run?.getAsJsonObject("pending")
        val key = listOf(run?.string("runId"), pending?.string("requestId"), pending?.flag("submitted"), run?.string("interaction")).toString()
        if (shown == key) return
        shown = key; container.removeAllViews()
        if (pending == null || pending.flag("submitted") == true) return
        val context = container.context
        fun label(text: String) { container.addView(TextView(context).apply { this.text = text; setTextIsSelectable(true) }) }
        if (run.string("interaction") != "plugin") { label(context.getString(R.string.workbench_script_interaction)); return }
        val buttons = mutableListOf<Button>()
        fun send(value: JsonElement? = null, allowed: Boolean? = null) {
            val body = jsonObject("runId" to run["runId"], "requestId" to pending["requestId"])
            if (allowed != null) { body.addProperty("allowed", allowed); body.addProperty("scope", "once") } else body.add("value", value)
            buttons.forEach { it.isEnabled = false }
            submit(body) { success -> if (!success) buttons.forEach { it.isEnabled = true } }
        }
        fun button(text: String, click: () -> Unit) { container.addView(Button(context).apply {
            this.text = text; minHeight = (48 * resources.displayMetrics.density).toInt()
            setOnClickListener { click() }; buttons += this
        }) }
        if (pending.string("type") == "confirmation") {
            if (pending.string("tool") == "script_run" && pending.getAsJsonObject("arguments")?.has("parameters") == true)
                container.addView(ScriptConfirmationView.create(context, pending))
            else { label(pending.string("description").orEmpty()); label(pending["arguments"]?.toString().orEmpty()) }
            button(context.getString(R.string.task_allow)) { send(allowed = true) }
            button(context.getString(R.string.task_deny)) { send(allowed = false) }
        } else {
            label(pending.string("question").orEmpty())
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
    }
}
