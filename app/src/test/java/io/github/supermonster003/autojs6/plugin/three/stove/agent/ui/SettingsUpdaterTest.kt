package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import org.junit.Assert.*
import org.junit.Test

class SettingsUpdaterTest {
    private class Fixture {
        val saves = mutableListOf<Pair<AgentSettings, (Boolean) -> Unit>>()
        val rendered = mutableListOf<SettingsDraft>()
        var failures = 0
        val updater = SettingsUpdater({ value, done -> saves += value to done }, { rendered += it }, { failures++ })
    }

    @Test fun changesRenderImmediatelyAndSaveOneWholeObjectAtATime() {
        val f = Fixture(); f.updater.load(AgentSettings())
        assertTrue(f.updater.apply { it.withVoice(false) })
        assertTrue(f.updater.apply { it.withGroup("shell", true) })
        assertFalse(f.rendered.last().settings.voice); assertTrue("shell" in f.rendered.last().settings.toolGroups)
        assertEquals(1, f.saves.size); assertFalse(f.saves[0].first.voice); assertFalse("shell" in f.saves[0].first.toolGroups)
        f.saves[0].second(true)
        assertEquals(2, f.saves.size)
        assertFalse(f.saves[1].first.voice); assertTrue("shell" in f.saves[1].first.toolGroups)
        f.saves[1].second(true)
        assertFalse(f.updater.busy); assertEquals(f.saves[1].first, f.updater.confirmed!!.settings)
    }

    @Test fun storageRefreshWaitsForPendingWritesAndInvalidChangesAreRejected() {
        val f = Fixture(); f.updater.load(AgentSettings())
        f.updater.apply { it.withVoice(false) }
        f.updater.load(AgentSettings(voice = true, floating = true))
        assertFalse(f.updater.current!!.settings.floating)
        assertFalse(f.updater.apply { it.withLimit("maxSteps", 999) })
        assertEquals(1, f.saves.size)
    }

    @Test fun failedSaveRollsBackToConfirmedAndDropsQueuedChanges() {
        val f = Fixture(); f.updater.load(AgentSettings())
        f.updater.apply { it.withAccess(AccessMode.FULL) }
        f.updater.apply { it.withVoice(false) }
        f.saves[0].second(false)
        assertEquals(1, f.failures); assertFalse(f.updater.busy)
        assertEquals(AgentSettings(), f.rendered.last().settings); assertEquals(1, f.saves.size)
    }
}
