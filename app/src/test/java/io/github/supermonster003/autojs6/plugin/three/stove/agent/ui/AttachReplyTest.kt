package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachReplyTest {

    @Test
    fun `the mirrored constants match AutoJs6 6_8_1`() {
        // ThreeStoveAgentActions.ATTACH_RESULT_ANSWERED / ATTACH_REPLY_ACCEPTED / META_ATTACH_REPLY and
        // ThreeStoveAgentContract.UNAVAILABLE_REASONS of host 8e3dfa3237. Change both sides together.
        assertEquals(1, AttachReply.RESULT_ANSWERED)
        assertEquals(0, AttachReply.RESULT_INITIAL)
        assertEquals("ACCEPTED", AttachReply.REPLY_ACCEPTED)
        assertEquals("org.autojs.autojs6.meta.THREE_STOVE_AGENT_ATTACH_REPLY", AttachReply.META_ATTACH_REPLY)
        assertEquals(
            setOf("NOT_INSTALLED", "NOT_TRUSTED", "DISABLED", "NOT_ACTIVATED", "INCOMPATIBLE", "NO_RESPONSE", "REQUEST_DROPPED"),
            AttachReply.REASONS,
        )
    }

    @Test
    fun `an answered request is accepted or names the host reason`() {
        assertEquals(AttachReply.Accepted, AttachReply.of(true, 1, "ACCEPTED"))
        AttachReply.REASONS.forEach { assertEquals(AttachReply.Unavailable(it), AttachReply.of(true, 1, it)) }
    }

    @Test
    fun `an unanswered request means the system did not deliver it only when the host answers requests`() {
        assertEquals(AttachReply.NotReached, AttachReply.of(true, 0, null))
        assertEquals(AttachReply.Unknown, AttachReply.of(false, 0, null))
    }

    @Test
    fun `replies this version does not know fall back to the general advice`() {
        assertEquals(AttachReply.Unknown, AttachReply.of(true, 1, "SOMETHING_NEW"))
        assertEquals(AttachReply.Unknown, AttachReply.of(true, 1, null))
        assertEquals(AttachReply.Unknown, AttachReply.of(true, 7, "ACCEPTED"))
        assertEquals(R.string.launcher_link_timeout, AttachReply.message(AttachReply.Unknown))
    }

    @Test
    fun `only a reason or an undelivered request ends the wait early`() {
        assertTrue(AttachReply.Unavailable("NOT_TRUSTED").final)
        assertTrue(AttachReply.NotReached.final)
        assertFalse(AttachReply.Accepted.final)
        assertFalse(AttachReply.Unknown.final)
    }

    @Test
    fun `every reply and reason has its own text`() {
        assertEquals(R.string.launcher_link_failed, AttachReply.message(AttachReply.Unavailable("DISABLED")))
        assertEquals(R.string.launcher_link_not_reached, AttachReply.message(AttachReply.NotReached))
        assertEquals(R.string.launcher_link_timeout_accepted, AttachReply.message(AttachReply.Accepted))
        assertEquals(R.string.launcher_link_step_not_installed, AttachReply.step("NOT_INSTALLED"))
        assertEquals(R.string.launcher_link_step_not_trusted, AttachReply.step("NOT_TRUSTED"))
        assertEquals(R.string.launcher_link_step_disabled, AttachReply.step("DISABLED"))
        assertEquals(R.string.launcher_link_step_incompatible, AttachReply.step("INCOMPATIBLE"))
        assertEquals(R.string.launcher_link_step_request_dropped, AttachReply.step("REQUEST_DROPPED"))
        assertEquals(R.string.launcher_link_step_no_response, AttachReply.step("NO_RESPONSE"))
        assertEquals(R.string.launcher_link_step_no_response, AttachReply.step("NOT_ACTIVATED"))
    }
}
