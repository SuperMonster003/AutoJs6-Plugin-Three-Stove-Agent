package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachReplyTest {

    @Test
    fun `an answered request is accepted or names the host reason`() {
        assertEquals(AttachReply.Accepted, AttachReply.of(true, 1, "ACCEPTED"))
        ThreeStoveAgentContract.UNAVAILABLE_REASONS.forEach { assertEquals(AttachReply.Unavailable(it), AttachReply.of(true, 1, it)) }
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
        assertEquals(R.string.script_roots_rejected, AttachReply.step("ROOTS_REJECTED"))
        assertEquals(R.string.launcher_link_step_no_response, AttachReply.step("NO_RESPONSE"))
        assertEquals(R.string.launcher_link_step_no_response, AttachReply.step("NOT_ACTIVATED"))
    }

    @Test
    fun `only a tap on Connect that did not reach AutoJs6 opens it`() {
        assertTrue(AttachReply.opensHost(AttachReply.NotReached, byUser = true))
        assertFalse(AttachReply.opensHost(AttachReply.NotReached, byUser = false))
        assertFalse(AttachReply.opensHost(AttachReply.Accepted, byUser = true))
        assertFalse(AttachReply.opensHost(AttachReply.Unavailable("NOT_TRUSTED"), byUser = true))
        assertFalse(AttachReply.opensHost(AttachReply.Unknown, byUser = true))
    }

    @Test
    fun `only the ROOTS_REJECTED reason means the script directories were turned down`() {
        assertTrue(AttachReply.rejectsRoots(AttachReply.Unavailable("ROOTS_REJECTED")))
        assertFalse(AttachReply.rejectsRoots(AttachReply.Unavailable("NOT_TRUSTED")))
        assertFalse(AttachReply.rejectsRoots(AttachReply.NotReached))
        assertFalse(AttachReply.rejectsRoots(AttachReply.Accepted))
        assertFalse(AttachReply.rejectsRoots(AttachReply.Unknown))
    }

    @Test
    fun `an opened AutoJs6 changes only the undelivered text`() {
        assertEquals(R.string.launcher_link_opened_host, AttachReply.message(AttachReply.NotReached, openedHost = true))
        assertEquals(R.string.launcher_link_not_reached, AttachReply.message(AttachReply.NotReached, openedHost = false))
        assertEquals(R.string.launcher_link_timeout_accepted, AttachReply.message(AttachReply.Accepted, openedHost = true))
        assertEquals(R.string.launcher_link_timeout, AttachReply.message(AttachReply.Unknown, openedHost = true))
    }
}
