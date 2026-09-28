package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import java.util.concurrent.TimeUnit
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolNames

/**
 * Inline question or confirmation, rebuilt only for a new request so polling cannot erase an answer
 * being typed. Answer buttons stay direct children of [container], in the order Allow once, Always
 * allow for this session, Deny.
 */
internal class PendingCard(
    private val container: LinearLayout,
    private val kit: Kit = Kit.of(container.context),
    /** False inside a surface that is already a card (confirmation dialog, floating card). */
    private val framed: Boolean = true,
    private val submit: (JsonObject, (Boolean) -> Unit) -> Unit,
) {
    private var shown: String? = null
    private var restoredKey: String? = null
    private var restoredAnswer: String? = null
    private var restoredRemember = false
    private var sourceExpanded = false
    private var restoredSourceExpanded = false
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
        restoredSourceExpanded = state?.getBoolean("sourceExpanded") == true
    }
    fun save(state: Bundle) {
        state.putString("answerKey", shown)
        container.findViewById<EditText>(R.id.workbench_answer)?.let { state.putString("answerDraft", it.text.toString()) }
        state.putBoolean("answerRemember", container.findViewById<CheckBox>(R.id.interaction_remember)?.isChecked == true)
        state.putBoolean("sourceExpanded", sourceExpanded)
    }
    fun render(run: JsonObject?) {
        val pending = run?.getAsJsonObject("pending")
        val key = listOf(run?.string("runId"), pending?.string("requestId"), pending?.flag("submitted"), run?.string("interaction")).toString()
        if (shown == key) return
        shown = key; container.removeCallbacks(tick); countdown = null; deadline = null; sending = false
        buttons.clear(); container.removeAllViews()
        sourceExpanded = key == restoredKey && restoredSourceExpanded
        if (pending == null || pending.flag("submitted") == true) { frame(null); return }
        val context = container.context
        if (run.string("interaction") != "plugin") {
            frame(Tone.NEUTRAL); add(body(context.getString(R.string.workbench_script_interaction))); return
        }
        deadline = pending.number("deadlineMs")
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
        if (pending.string("type") == "confirmation") {
            val (risk, tone) = when (pending.string("risk")) {
                "read_only" -> R.string.interaction_risk_read_only to Tone.NEUTRAL
                "normal" -> R.string.interaction_risk_normal to Tone.ACCENT
                else -> R.string.interaction_risk_sensitive to Tone.WARNING
            }
            frame(tone)
            header(context.getString(risk), tone, R.drawable.ic_shield)
            add(title(context.getString(R.string.interaction_confirmation)))
            pending.string("tool")?.let { tool -> add(caption(ToolPresentation.label(context, tool))) }
            val tool = pending.string("tool")
            when {
                tool == ToolNames.SCRIPT_RUN_SOURCE -> add(DynamicScriptConfirmationView.create(context, pending.getAsJsonObject("arguments"), sourceExpanded) { sourceExpanded = it })
                tool == ToolNames.SCRIPT_RUN && pending.getAsJsonObject("arguments")?.has("parameters") == true -> add(ScriptConfirmationView.create(context, pending))
                else -> {
                    pending.string("description")?.takeIf { it.isNotBlank() }?.let { add(body(it)) }
                    add(kit.parameterTable(ArgumentRows.rows(pending["arguments"])), top = Ui.SPACE_SM)
                }
            }
            button(kit.filledButton(context.getString(R.string.task_allow)) { send(allowed = true) }, first = true)
            val session = pending.flag("allowRunScope") == true
            if (session) button(kit.tonalButton(context.getString(R.string.interaction_allow_run)) { send(allowed = true, scope = "run") })
            button(kit.outlinedButton(context.getString(R.string.task_deny), danger = true) { send(allowed = false) })
            if (session) add(caption(context.getString(R.string.interaction_allow_run_note)))
        } else {
            val plan = pending.string("kind") == "plan"
            frame(Tone.ACCENT)
            header(null, Tone.ACCENT, if (plan) R.drawable.ic_layers else R.drawable.ic_help)
            add(title(context.getString(if (plan) R.string.interaction_plan else R.string.interaction_question)))
            if (plan) add(body(context.getString(R.string.interaction_plan_note)))
            else add(body(pending.string("question").orEmpty()).apply { textSize = Ui.TEXT_ITEM })
            if (pending.has("memoryKey")) {
                remember = MaterialCheckBox(kit.context).apply {
                    id = R.id.interaction_remember; setText(R.string.interaction_remember)
                    textSize = Ui.TEXT_BODY; setTextColor(kit.palette.text); buttonTintList = kit.controlTintList()
                    minHeight = kit.dp(Ui.TOUCH_TARGET)
                    isEnabled = pending.has("rememberScope"); isChecked = isEnabled && key == restoredKey && restoredRemember
                }
                add(remember, top = Ui.SPACE_SM)
                add(caption(context.getString(if (remember.isEnabled) R.string.interaction_remember_review else R.string.interaction_memory_disabled)))
            }
            when (pending.string("kind")) {
                "plan" -> {
                    // One step per line, numbered for reading; numbering and bullets are stripped before the reply.
                    val steps = pending.getAsJsonArray("steps")?.map { it.asString }.orEmpty()
                    val (field, edit) = kit.textField(if (key == restoredKey) restoredAnswer else steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n"),
                        context.getString(R.string.interaction_plan_steps), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 2048, singleLine = false)
                    edit.id = R.id.workbench_answer; edit.minLines = 3; edit.maxLines = 10
                    edit.filters = arrayOf(InputFilter.LengthFilter(2048))
                    add(field, top = Ui.SPACE_MD)
                    button(kit.filledButton(context.getString(R.string.interaction_plan_execute)) {
                        val lines = edit.text.toString().lines().map { it.trim().replace(Regex("^(?:\\d+[.)]|[-*])\\s*"), "").trim() }.filter { it.isNotEmpty() }
                        if (lines.isNotEmpty() && lines.size <= 8 && lines.all { it.codePointCount(0, it.length) <= 200 }) { field.error = null; send(JsonArray().apply { lines.forEach(::add) }) }
                        else field.error = context.getString(R.string.interaction_plan_invalid)
                    }, first = true)
                }
                "choice" -> pending.getAsJsonArray("choices").forEachIndexed { index, choice ->
                    button(kit.tonalButton(choice.asString) { send(choice) }.apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL }, first = index == 0)
                }
                "confirm" -> {
                    button(kit.tonalButton(context.getString(android.R.string.yes)) { send(true.json()) }, first = true)
                    button(kit.tonalButton(context.getString(android.R.string.no)) { send(false.json()) })
                }
                else -> {
                    val (field, edit) = kit.textField(if (key == restoredKey) restoredAnswer else null, context.getString(R.string.task_reply),
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 1000, singleLine = false)
                    edit.id = R.id.workbench_answer; edit.minLines = 2; edit.maxLines = 6
                    edit.filters = arrayOf(InputFilter.LengthFilter(1000))
                    add(field, top = Ui.SPACE_MD)
                    button(kit.filledButton(context.getString(R.string.task_reply)) {
                        if (edit.text?.isNotBlank() == true) { field.error = null; send(edit.text.toString().json()) }
                        else field.error = context.getString(R.string.workbench_answer_required)
                    }, first = true)
                }
            }
        }
        tick.run()
    }

    /** The card surface, tinted by the request's tone; an empty card takes no space. */
    private fun frame(tone: Tone?) {
        if (tone == null || !framed) { container.background = null; container.setPaddingRelative(0, 0, 0, 0); return }
        val stroke = when (tone) { Tone.WARNING -> kit.palette.warning; Tone.DANGER -> kit.palette.danger; Tone.ACCENT -> kit.palette.accent; else -> kit.palette.outline }
        container.background = kit.roundedFill(kit.palette.surface, Ui.RADIUS_CARD, AgentColorPolicy.withAlpha(stroke, 0x66))
        container.setPaddingRelative(kit.dp(Ui.SPACE_LG), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_LG), kit.dp(Ui.SPACE_LG))
    }
    private fun header(label: String?, tone: Tone, icon: Int) {
        val row = LinearLayout(kit.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val color = kit.toneColors(tone).second
        row.addView(ImageView(container.context).apply {
            setImageDrawable(kit.tintedDrawable(icon, color)); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(kit.dp(20), kit.dp(20)).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
        label?.let { row.addView(kit.badge(it, tone)) }
        // The countdown takes the remaining width and wraps there, so large text never pushes it out.
        val remaining = kit.text("", Ui.TEXT_CAPTION, kit.palette.muted).apply {
            gravity = Gravity.END; textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_NONE
        }
        row.addView(remaining, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = kit.dp(Ui.SPACE_SM) })
        if (deadline != null) countdown = remaining.apply { id = R.id.interaction_countdown }
        add(row)
    }
    private fun title(text: String) = kit.text(text, Ui.TEXT_TITLE, kit.palette.text, medium = true).apply {
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, kit.dp(Ui.SPACE_SM), 0, 0)
        if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
    }
    private fun body(text: String) = kit.paragraph(text)
    private fun caption(text: String) = kit.note(text)
    private fun add(view: View, top: Int = 0) {
        container.addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(top) })
    }
    private fun button(view: MaterialButton, first: Boolean = false) {
        buttons += view
        container.addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(if (first) Ui.SPACE_LG else Ui.SPACE_SM) })
    }
}
