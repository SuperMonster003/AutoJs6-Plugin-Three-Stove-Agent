package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class NodeRefRegistryTest {
    private fun snapshot(id: String, rows: List<String> = listOf("#n1 Button clickable \"Go\" id=go c=(10,20)"), pkg: String = "test", truncated: Boolean = false) =
        CompactNodeText.parse(dump(id, rows, truncated, pkg))
    @Test fun reorderedNodeRelocatesToNewHostReferenceWithoutChangingFingerprint() {
        val registry = NodeRefRegistry()
        val old = snapshot("s1"); registry.record(old)
        val next = snapshot("s2", listOf("#n1 View c=(0,0)", "#n2 Button clickable \"Go\" id=go c=(15,22)")); registry.record(next)
        val match = registry.relocate("#n1", "s1")
        assertEquals("s2", match.snapshotId); assertEquals("#n2", match.node.ref)
        assertEquals(old.nodes[0].fingerprint(old.window), match.node.fingerprint(next.window))
    }
    @Test fun ambiguousMovedAndClippedNodesRequireFreshObservation() {
        for (rows in listOf(listOf("#n1 Button \"Go\" id=go c=(200,20)"),
            listOf("#n1 Button \"Go\" id=go c=(10,20)", "#n2 Button \"Go\" id=go c=(10,20)"))) {
            val registry = NodeRefRegistry(); registry.record(snapshot("s1")); registry.record(snapshot("s2", rows))
            assertThrows(NodeRefRegistry.Stale::class.java) { registry.relocate("#n1", "s1") }
        }
        val registry = NodeRefRegistry(); registry.record(snapshot("s1", listOf("#n1 Button \"Go...\" c=(1,1)")))
        assertThrows(NodeRefRegistry.Stale::class.java) { registry.relocate("#n1", "s1") }
    }
    @Test fun windowChangeEvictionClearAndCloseInvalidateOldReferences() {
        val registry = NodeRefRegistry(1); registry.record(snapshot("s1")); registry.record(snapshot("s2"))
        assertThrows(NodeRefRegistry.Stale::class.java) { registry.resolve("#n1", "s1") }
        assertTrue(registry.record(snapshot("s3", pkg = "other")).flag("windowChanged")!!)
        assertThrows(NodeRefRegistry.Stale::class.java) { registry.resolve("#n1", "s2") }
        registry.clear(); assertThrows(NodeRefRegistry.Stale::class.java) { registry.resolve("#n1") }
        registry.close(); assertThrows(IllegalStateException::class.java) { registry.record(snapshot("s4")) }
    }
    @Test fun anonymousContainerRelocationDoesNotAliasSmallerDescendants() {
        val registry = NodeRefRegistry()
        registry.record(snapshot("s1", listOf("#n1 FrameLayout [0,746][1800,991]")))
        registry.record(snapshot("s2", listOf("#n1 FrameLayout [0,746][1800,991]", "#n2 FrameLayout [183,771][623,825]")))
        assertEquals("#n1", registry.relocate("#n1", "s1").node.ref)
    }
    @Test fun aSmallerReplacementInsideTheOldBoundsCannotRelocate() {
        val registry = NodeRefRegistry()
        registry.record(snapshot("s1", listOf("#n1 FrameLayout [0,0][100,100]")))
        registry.record(snapshot("s2", listOf("#n1 FrameLayout [49,49][51,51]")))
        assertThrows(NodeRefRegistry.Stale::class.java) { registry.relocate("#n1", "s1") }
    }
    @Test fun textDiffCountsDuplicatesAndFlagsPartialSnapshots() {
        val registry = NodeRefRegistry()
        registry.record(snapshot("s1", listOf("#n1 TextView \"Same\" c=(0,0)", "#n2 TextView \"Same\" c=(1,1)")))
        val diff = registry.record(snapshot("s2", listOf("#n1 TextView \"Same\" c=(0,0)"), truncated = true))
        assertEquals(1L, diff.number("removedCount")); assertEquals(jsonArray("Same".json()), diff["removedText"]); assertTrue(diff.flag("partial")!!)
    }
    @Test fun stateChangesAreVisibleEvenWhenTextDoesNotChange() {
        val registry = NodeRefRegistry(); registry.record(snapshot("s1"))
        assertFalse(registry.record(snapshot("s2")).flag("changed")!!)
        val diff = registry.record(snapshot("s3", listOf("#n1 Button clickable selected \"Go\" id=go c=(10,20)")))
        assertTrue(diff.flag("changed")!!); assertEquals(0L, diff.number("addedCount"))
    }
    @Test fun summariesAreBoundedAndRegistriesDoNotShareReferences() {
        val registry = NodeRefRegistry(); registry.record(snapshot("s1", emptyList()))
        val diff = registry.record(snapshot("s2", (1..100).map { "#n$it TextView \"item$it\" c=(0,0)" }))
        assertEquals(100L, diff.number("addedCount")); assertEquals(16, diff.getAsJsonArray("addedText").size()); assertTrue(diff.flag("partial")!!)
        assertThrows(NodeRefRegistry.Stale::class.java) { NodeRefRegistry().resolve("#n1", "s2") }
    }
}
