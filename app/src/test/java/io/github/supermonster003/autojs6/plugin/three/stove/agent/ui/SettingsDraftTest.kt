package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.SettingsCodec
import org.junit.Assert.*
import org.junit.Test

class SettingsDraftTest {
    private fun reject(block: () -> Unit) { assertThrows(IllegalArgumentException::class.java, block) }

    @Test fun accessModesStayMutuallyExclusive() {
        var draft = SettingsDraft(AgentSettings())
        assertEquals(AccessMode.STANDARD, draft.accessMode)
        draft = draft.withAccess(AccessMode.FULL)
        assertTrue(draft.settings.fullAccess); assertFalse(draft.settings.cautious); assertEquals(AccessMode.FULL, draft.accessMode)
        draft = draft.withAccess(AccessMode.CAUTIOUS)
        assertTrue(draft.settings.cautious); assertFalse(draft.settings.fullAccess)
        assertEquals(draft.settings, SettingsCodec.decode(SettingsCodec.json(draft.settings).toString()))
    }

    @Test fun groupsAndLimitsAreValidatedAndAutomaticRemovesTheKey() {
        val draft = SettingsDraft(AgentSettings()).withGroup("shell", true).withLimit("maxSteps", 7)
        assertTrue("shell" in draft.settings.toolGroups); assertEquals(7L, draft.limit("maxSteps"))
        assertFalse("shell" in draft.withGroup("shell", false).settings.toolGroups)
        assertNull(draft.withLimit("maxSteps", null).limit("maxSteps"))
        reject { draft.withGroup("unknown", true) }
        reject { draft.withLimit("maxSteps", 201) }
        reject { draft.withLimit("maxSteps", 0) }
        reject { draft.withLimit("unknown", 1) }
    }

    @Test fun durationsAreEditedInMinutesAndStoredInMilliseconds() {
        val draft = SettingsDraft(AgentSettings()).withDurationMinutes(12)
        assertEquals(720_000L, draft.limit(SettingsDraft.DURATION)); assertEquals(12L, draft.durationMinutes())
        assertEquals(60L, SettingsDraft.MAX_DURATION_MINUTES)
        // Legacy millisecond values round up for display and are never rewritten unless edited.
        val legacy = SettingsDraft(AgentSettings(budget = mapOf(SettingsDraft.DURATION to 90_001L)))
        assertEquals(2L, legacy.durationMinutes()); assertEquals(90_001L, legacy.withVoice(false).limit(SettingsDraft.DURATION))
        reject { draft.withDurationMinutes(0) }
        reject { draft.withDurationMinutes(61) }
        assertNull(draft.withDurationMinutes(null).durationMinutes())
    }
}
