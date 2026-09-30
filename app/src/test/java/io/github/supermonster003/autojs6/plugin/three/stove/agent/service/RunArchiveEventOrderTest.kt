package io.github.supermonster003.autojs6.plugin.three.stove.agent.service

import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** A run query must never show a terminal state without its result (remote CI build 178, API 24 conformance). */
class RunArchiveEventOrderTest {
    @get:Rule val temp = TemporaryFolder()
    private val terminal = RunState.entries.filter { it.terminal }.map { it.wire }.toSet()

    @Test fun aTerminalStateBecomesVisibleTogetherWithItsResult() {
        val archive = RunArchive(temp.newFolder())
        val fixture = RunnerFixture()
        val options = fixture.options()
        val seen = mutableListOf<String>()
        lateinit var run: AgentRunner
        fixture.onEvent = { event ->
            archive.record(event, run)
            val row = checkNotNull(archive.get(event.runId))
            seen += "${event.type}:${row.string("state")}:${row.has("result")}"
        }
        run = fixture.submit(options)
        archive.admit(run, StartRequest(options, null, emptySet(), "", "plugin", emptySet(), "default", false))
        fixture.scheduler.drain()
        fixture.reply(RunnerFixture.done())

        val row = checkNotNull(archive.get(run.id))
        assertEquals(row.toString(), "completed", row.string("state"))
        assertEquals("completed", row.getAsJsonObject("result").string("status"))
        assertEquals(seen.toString(), emptyList<String>(), seen.filter { it.split(":")[1] in terminal && it.endsWith(":false") })
        assertTrue(seen.toString(), "state:completed:true" in seen && "done:completed:true" in seen)
    }

    @Test fun eventsWithoutAnOwningRunnerStillUpdateTheRow() {
        val archive = RunArchive(temp.newFolder())
        val fixture = RunnerFixture()
        val options = fixture.options()
        val run = fixture.submit(options)
        archive.admit(run, StartRequest(options, null, emptySet(), "", "plugin", emptySet(), "default", false))
        archive.record(RunEvent(run.id, 1, "progress", jsonObject("message" to "Working".json())), null)
        assertEquals("Working", checkNotNull(archive.get(run.id)).string("progress"))
    }
}
