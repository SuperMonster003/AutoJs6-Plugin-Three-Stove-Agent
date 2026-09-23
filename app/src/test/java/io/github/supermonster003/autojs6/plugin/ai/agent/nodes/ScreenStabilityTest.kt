package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import org.junit.Assert.*
import org.junit.Test

class ScreenStabilityTest {
    private fun sample(id: String = "one", text: String = "Go") = CompactNodeText.parse(dump(id, rows = listOf("#n1 Button clickable \"$text\" c=(10,20)")))
    @Test fun changingSnapshotIdsDoNotResetTheQuietInterval() {
        val tracker = ScreenStability(0)
        assertEquals(ScreenStability.State.WAITING, tracker.sample(sample(), 0))
        assertEquals(ScreenStability.State.WAITING, tracker.sample(sample("two"), 499))
        assertEquals(ScreenStability.State.STABLE, tracker.sample(sample("three"), 500))
    }
    @Test fun contentChangeRestartsTheQuietInterval() {
        val tracker = ScreenStability(0)
        tracker.sample(sample(), 0)
        assertEquals(ScreenStability.State.WAITING, tracker.sample(sample("two", "Loading"), 400))
        assertEquals(ScreenStability.State.WAITING, tracker.sample(sample("three", "Loading"), 899))
        assertEquals(ScreenStability.State.STABLE, tracker.sample(sample("four", "Loading"), 900))
    }
    @Test fun continuousChangesEndAtTheMaximumWait() {
        val tracker = ScreenStability(0)
        for (now in 0L..2750L step 250) assertEquals(ScreenStability.State.WAITING, tracker.sample(sample(text = "$now"), now))
        assertEquals(ScreenStability.State.TIMED_OUT, tracker.sample(sample(text = "3000"), 3000))
    }
    @Test fun laterObservationUsesTheActionBaselineRatherThanAnIntermediateDump() {
        val observations = ObservationTools()
        observations.actionBaseline(sample("before", "Before"))
        val changes = observations.sinceAction(sample("after", "After"))!!
        assertEquals("before", changes["previousSnapshotId"].asString)
        assertEquals("Before", changes.getAsJsonArray("removedText").single().asString)
        observations.actionBaseline(null)
        assertNull(observations.sinceAction(sample()))
    }
}
