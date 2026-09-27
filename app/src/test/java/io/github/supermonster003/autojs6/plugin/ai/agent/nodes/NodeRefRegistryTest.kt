package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class NodeRefRegistryTest {
    private fun snapshot(id: String, rows: List<String> = listOf("#n1 Button clickable \"Go\" id=go c=(10,20)"), pkg: String = "test", truncated: Boolean = false) =
        CompactNodeText.parse(dump(id, rows, truncated, pkg))
    @Test fun fingerprintIgnoresReferenceAndPositionButKeepsIdentity() {
        val old = snapshot("s1")
        val next = snapshot("s2", listOf("#n1 View c=(0,0)", "#n2 Button clickable \"Go\" id=go c=(15,22)"))
        assertEquals(old.nodes[0].fingerprint(old.window), next.nodes[1].fingerprint(next.window))
        assertNotEquals(old.nodes[0].fingerprint(old.window), next.nodes[0].fingerprint(next.window))
        assertNotEquals(old.nodes[0].fingerprint("test"), old.nodes[0].fingerprint("other"))
        val registry = NodeRefRegistry(); registry.record(old); registry.record(next)
        assertEquals("#n2", registry.resolve("#n2").node.ref); assertEquals("#n1", registry.resolve("#n1", "s1").node.ref)
        assertThrows(NodeRefRegistry.Stale::class.java) { registry.resolve("#n3") }
    }
    @Test fun windowChangeEvictionClearAndCloseInvalidateOldReferences() {
        val registry = NodeRefRegistry(1); registry.record(snapshot("s1")); registry.record(snapshot("s2"))
        assertThrows(NodeRefRegistry.Stale::class.java) { registry.resolve("#n1", "s1") }
        assertTrue(registry.record(snapshot("s3", pkg = "other")).flag("windowChanged")!!)
        assertThrows(NodeRefRegistry.Stale::class.java) { registry.resolve("#n1", "s2") }
        registry.clear(); assertThrows(NodeRefRegistry.Stale::class.java) { registry.resolve("#n1") }
        registry.close(); assertThrows(IllegalStateException::class.java) { registry.record(snapshot("s4")) }
    }
    @Test fun changedCapabilitiesChangeTheFingerprint() {
        val plain = snapshot("s1", listOf("#n1 ViewGroup [0,0][100,100]")).nodes.single()
        for (flag in listOf("clickable", "long_clickable", "checkable", "scrollable", "editable")) {
            val capable = snapshot("s2", listOf("#n1 ViewGroup $flag [0,0][100,100]")).nodes.single()
            assertNotEquals(flag, plain.fingerprint("test"), capable.fingerprint("test"))
        }
    }
    @Test fun transientSelectionStateDoesNotChangeTheFingerprint() {
        val plain = snapshot("s1").nodes.single()
        val selected = snapshot("s2", listOf("#n1 Button clickable checked focused selected \"Go\" id=go c=(10,20)")).nodes.single()
        assertEquals(plain.fingerprint("test"), selected.fingerprint("test"))
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
