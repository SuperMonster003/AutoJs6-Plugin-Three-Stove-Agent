package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentActions as A
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C

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
        /** The result code the ordered broadcast starts with; the host replaces it with [A.ATTACH_RESULT_ANSWERED]. */
        const val RESULT_INITIAL = 0

        /** Reads the result of an ordered attach broadcast; [hostAnswers] is whether the host declares [A.META_ATTACH_REPLY]. */
        fun of(hostAnswers: Boolean, resultCode: Int, data: String?): AttachReply = when {
            resultCode == A.ATTACH_RESULT_ANSWERED && data == A.ATTACH_REPLY_ACCEPTED -> Accepted
            resultCode == A.ATTACH_RESULT_ANSWERED && data != null && data in C.UNAVAILABLE_REASONS -> Unavailable(data)
            resultCode == RESULT_INITIAL && hostAnswers -> NotReached
            else -> Unknown
        }

        /**
         * Whether the workbench opens AutoJs6 to finish the connection (feedback-680 P3.6): only after the user
         * tapped Connect and the system did not deliver the request; the automatic request on start never leaves
         * the workbench.
         */
        fun opensHost(reply: AttachReply, byUser: Boolean): Boolean = byUser && reply == NotReached

        /** The banner text once the request ended without a link; [openedHost] is whether AutoJs6 was opened for it. */
        @StringRes
        fun message(reply: AttachReply, openedHost: Boolean = false): Int = when (reply) {
            is Unavailable -> R.string.launcher_link_failed
            NotReached -> if (openedHost) R.string.launcher_link_opened_host else R.string.launcher_link_not_reached
            Accepted -> R.string.launcher_link_timeout_accepted
            Unknown -> R.string.launcher_link_timeout
        }

        /** The next step for a host reason, shown after it in [R.string.launcher_link_failed]. */
        @StringRes
        fun step(reason: String): Int = when (reason) {
            C.UNAVAILABLE_REASON_NOT_INSTALLED -> R.string.launcher_link_step_not_installed
            C.UNAVAILABLE_REASON_NOT_TRUSTED -> R.string.launcher_link_step_not_trusted
            C.UNAVAILABLE_REASON_DISABLED -> R.string.launcher_link_step_disabled
            C.UNAVAILABLE_REASON_INCOMPATIBLE -> R.string.launcher_link_step_incompatible
            C.UNAVAILABLE_REASON_REQUEST_DROPPED -> R.string.launcher_link_step_request_dropped
            else -> R.string.launcher_link_step_no_response
        }
    }
}
