package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ThreeStoveAgentPlugin
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.AgentJson
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.AgentRuntime

/**
 * Text that reaches a draft by voice (roadmap D78): the digital assistant's VOICE_COMMAND and the resident listener's
 * dictation. Untrusted input: plain text only, control characters become spaces (line breaks stay), and dictated text is
 * cut at the draft limit instead of being rejected like an oversized share.
 */
internal object VoiceDraftText {
    /** The [TaskEntry] limit. */
    const val MAX_BYTES = 4096

    /** The cleaned draft, or null when nothing remains. */
    fun clean(value: CharSequence?): String? {
        val text = value?.toString() ?: return null
        val out = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val char = text[index++]
            when {
                char.isHighSurrogate() && index < text.length && text[index].isLowSurrogate() -> { out.append(char).append(text[index++]) }
                char.isSurrogate() -> out.append(' ')
                char == '\n' -> out.append('\n')
                char == '\r' -> if (index < text.length && text[index] == '\n') Unit else out.append('\n')
                Character.isISOControl(char) || char == '\u2028' || char == '\u2029' -> out.append(' ')
                else -> out.append(char)
            }
        }
        return AgentJson.truncate(out.toString().trim(), MAX_BYTES).trim().takeIf { it.isNotEmpty() }
    }

    /**
     * The draft of a VOICE_COMMAND intent. Extras other than the text are never read, the recognition language
     * ([ThreeStoveAgentPlugin.EXTRA_VOICE_LANGUAGE]) included: the task language follows the UI and models read any language.
     */
    fun read(intent: Intent): String? = runCatching {
        if (intent.action != ThreeStoveAgentPlugin.VOICE_COMMAND_ACTION) null else clean(intent.getCharSequenceExtra(Intent.EXTRA_TEXT))
    }.getOrNull()
}

/**
 * VOICE_COMMAND entry of the 3-Macaw Speech digital assistant (roadmap D60, D78). Like the share entry it only opens a
 * reviewable draft, never a task: into the floating card when the ball is on screen (D62, D72), otherwise the workbench.
 * It runs in the :agent process next to the ball and never creates the agent runtime just to look for it.
 */
class VoiceCommandActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = VoiceDraftText.read(intent)
        // This entry has no window of its own and finishes at once; only a toast can outlive it.
        if (text == null) Toast.makeText(this, R.string.entry_invalid, Toast.LENGTH_LONG).show()
        else if (AgentRuntime.peek()?.voiceDraft(text) != true) runCatching { startActivity(TaskEntries.intent(this, TaskEntry(text))) }
            .onFailure { Toast.makeText(this, R.string.entry_invalid, Toast.LENGTH_LONG).show() }
        finish()
    }
}
