package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R

/**
 * What AutoJs6 answered to the last attach request (feedback-680 P3.3). Hosts since 6.8.1 answer an
 * ordered attach broadcast and declare so with receiver meta-data; older hosts leave every reply [Unknown],
 * and the workbench keeps the general advice it always showed.
 */
internal sealed interface AttachReply {
    /** The host took the request; the link status tells the outcome. */
    data object Accepted : AttachReply

    /** The host cannot connect, for one of its unavailable reasons. */
    data class Unavailable(val reason: String) : AttachReply

    /** The host answers attach requests, yet this one came back unanswered: the system did not deliver it. */
    data object NotReached : AttachReply

    /** Nothing to go by: an older host, or a reply this version does not know. */
    data object Unknown : AttachReply

    /** Whether the reply settles the request, so the workbench need not wait out the 15 seconds. */
    val final: Boolean get() = this is Unavailable || this == NotReached

    companion object {
        // Mirrors of ThreeStoveAgentActions and ThreeStoveAgentContract in AutoJs6 6.8.1 (host 8e3dfa3237). The
        // bundled three-stove-agent-api.aar predates them (feedback-680 Q13); AttachReplyTest pins the values.
        const val RESULT_INITIAL = 0
        const val RESULT_ANSWERED = 1
        const val REPLY_ACCEPTED = "ACCEPTED"
        const val META_ATTACH_REPLY = "org.autojs.autojs6.meta.THREE_STOVE_AGENT_ATTACH_REPLY"
        val REASONS = setOf("NOT_INSTALLED", "NOT_TRUSTED", "DISABLED", "NOT_ACTIVATED", "INCOMPATIBLE", "NO_RESPONSE", "REQUEST_DROPPED")

        /** Reads the result of an ordered attach broadcast; [hostAnswers] is whether the host declares [META_ATTACH_REPLY]. */
        fun of(hostAnswers: Boolean, resultCode: Int, data: String?): AttachReply = when {
            resultCode == RESULT_ANSWERED && data == REPLY_ACCEPTED -> Accepted
            resultCode == RESULT_ANSWERED && data != null && data in REASONS -> Unavailable(data)
            resultCode == RESULT_INITIAL && hostAnswers -> NotReached
            else -> Unknown
        }

        /** The banner text once the request ended without a link. */
        @StringRes
        fun message(reply: AttachReply): Int = when (reply) {
            is Unavailable -> R.string.launcher_link_failed
            NotReached -> R.string.launcher_link_not_reached
            Accepted -> R.string.launcher_link_timeout_accepted
            Unknown -> R.string.launcher_link_timeout
        }

        /** The next step for a host reason, shown after it in [R.string.launcher_link_failed]. */
        @StringRes
        fun step(reason: String): Int = when (reason) {
            "NOT_INSTALLED" -> R.string.launcher_link_step_not_installed
            "NOT_TRUSTED" -> R.string.launcher_link_step_not_trusted
            "DISABLED" -> R.string.launcher_link_step_disabled
            "INCOMPATIBLE" -> R.string.launcher_link_step_incompatible
            "REQUEST_DROPPED" -> R.string.launcher_link_step_request_dropped
            else -> R.string.launcher_link_step_no_response
        }
    }
}
