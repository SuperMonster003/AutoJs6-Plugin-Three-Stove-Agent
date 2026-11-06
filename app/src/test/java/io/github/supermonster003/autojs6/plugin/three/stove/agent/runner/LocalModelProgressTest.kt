package io.github.supermonster003.autojs6.plugin.three.stove.agent.runner

import io.github.supermonster003.autojs6.plugin.three.stove.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.RunArchive
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.StartRequest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Roadmap P24 (host issue #589): the wait for an on-device model is visible while it lasts, and its timeout names the likely causes. */
class LocalModelProgressTest {
    @get:Rule val temp = TemporaryFolder()
    private val en = AgentJson.objectOf(F.asset("runner/texts.json")).getAsJsonObject("en")

    private fun progress(f: RunnerFixture) = f.events.filter { it.type == "progress" }.map { it.payload }

    @Test fun anOnDeviceModelReportsItsWaitAndThenItsReply() {
        val f = RunnerFixture(); f.model.onDevice = true
        val run = f.start()
        val waiting = progress(f).single()
        assertEquals("model", waiting.string("phase"))
        assertEquals(en.string("model_waiting_local"), waiting.string("message"))
        f.reply(RunnerFixture.done())
        val replied = progress(f).last().string("message")!!
        assertNull(progress(f).last().string("phase"))
        assertTrue(replied, replied.startsWith(en.string("model_replied_local")!! + " (") && replied.endsWith(" s)"))
        assertEquals("completed", run.result!!.string("status"))
    }

    @Test fun aRemoteModelEmitsNoModelProgress() {
        val f = RunnerFixture(); f.enqueue(RunnerFixture.done()); f.start()
        assertTrue(progress(f).isEmpty())
    }

    @Test fun anOnDeviceTimeoutExplainsTheLikelyCauses() {
        val f = RunnerFixture(); f.model.onDevice = true
        val run = f.start()
        f.model.calls.last().fail(RunError.MODEL_TIMEOUT); f.scheduler.drain()
        val result = run.result!!
        assertEquals("MODEL_TIMEOUT", result.getAsJsonObject("error").string("code"))
        val summary = result.string("summary")!!
        assertTrue(summary, summary.contains("[MODEL_TIMEOUT: " + en.string("model_timeout_local") + "]") && !summary.contains("key:"))
    }

    @Test fun aRemoteTimeoutKeepsTheBareCode() {
        val f = RunnerFixture(); val run = f.start()
        f.model.calls.last().fail(RunError.MODEL_TIMEOUT); f.scheduler.drain()
        val summary = run.result!!.string("summary")!!
        assertTrue(summary, summary.endsWith("[MODEL_TIMEOUT]"))
    }

    @Test fun anUnreachableProviderIsExplainedWithTheHostReasonAsNote() {
        val f = RunnerFixture(); f.model.onDevice = true
        val run = f.start()
        f.model.calls.last().callback(PortResult.Failure(RunError.TARGET_UNAVAILABLE, "PROVIDER_UNREACHABLE")); f.scheduler.drain()
        val summary = run.result!!.string("summary")!!
        assertTrue(summary, summary.contains("[TARGET_UNAVAILABLE: " + en.string("provider_unreachable") + " (PROVIDER_UNREACHABLE)]"))
        // Any other host reason stays a bare code, as before.
        val g = RunnerFixture(); val other = g.start()
        g.model.calls.last().callback(PortResult.Failure(RunError.TARGET_UNAVAILABLE, "TARGET_NOT_FOUND")); g.scheduler.drain()
        assertTrue(other.result!!.string("summary")!!.endsWith("[TARGET_UNAVAILABLE: TARGET_NOT_FOUND]"))
    }

    @Test fun theArchiveKeepsTheWaitStartOnlyWhileTheModelPhaseLasts() {
        val archive = RunArchive(temp.newFolder())
        val f = RunnerFixture(); f.model.onDevice = true
        val options = f.options()
        lateinit var run: AgentRunner
        f.onEvent = { event -> archive.record(event, run) }
        run = f.submit(options)
        archive.admit(run, StartRequest(options, null, emptySet(), "", "plugin", emptySet(), "default", false))
        f.scheduler.drain()
        val waiting = checkNotNull(archive.get(run.id))
        assertEquals("model", waiting.string("progressPhase"))
        assertTrue((waiting.number("progressAt")?.toLong() ?: 0L) > 0L)
        f.reply(RunnerFixture.done())
        val replied = checkNotNull(archive.get(run.id))
        assertFalse(replied.has("progressPhase"))
        assertFalse(replied.has("progressAt"))
        assertTrue(replied.string("progress")!!.startsWith(en.string("model_replied_local")!!))
    }
}
