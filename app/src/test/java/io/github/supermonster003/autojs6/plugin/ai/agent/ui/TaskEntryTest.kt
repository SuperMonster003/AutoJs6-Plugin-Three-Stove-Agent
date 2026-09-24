package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import org.junit.Assert.*
import org.junit.Test

class TaskEntryTest {
    @Test fun unicodeGoalUsesByteLimitAndPresetNamesKeepTheirValidation() {
        assertEquals("default", TaskEntry("中".repeat(1365), "default").preset)
        assertThrows(IllegalArgumentException::class.java) { TaskEntry("中".repeat(1366)) }
        assertThrows(IllegalArgumentException::class.java) { TaskEntry("bad\u0000goal") }
        assertThrows(IllegalArgumentException::class.java) { TaskEntry("goal", "") }
        assertEquals("", TaskEntry("").goal)
    }
    @Test fun floatingCoordinatesStayUsableAfterRotationOversizeAndInvalidSavedPositions() {
        assertEquals(936, FloatingPosition.coordinate(1f, 0, 1000, 64))
        assertEquals(250, FloatingPosition.coordinate(0.5f, 10, 850, 360))
        assertEquals(10, FloatingPosition.coordinate(-1f, 10, 100, 64))
        assertEquals(36, FloatingPosition.coordinate(2f, 10, 100, 64))
        assertEquals(0, FloatingPosition.coordinate(Float.NaN, 0, 100, 500))
        val fraction = FloatingPosition.fraction(180, 0, 360, 60)
        assertEquals(0.6f, fraction, 0.001f)
        assertEquals(540, FloatingPosition.coordinate(fraction, 0, 960, 60))
        assertEquals(0f, FloatingPosition.fraction(-10, 0, 100, 64), 0f)
        assertEquals(1f, FloatingPosition.fraction(400, 0, 100, 64), 0f)
    }
}
