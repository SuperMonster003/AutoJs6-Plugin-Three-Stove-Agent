package io.github.supermonster003.autojs6.plugin.ai.agent.model

import io.github.supermonster003.autojs6.plugin.ai.agent.runner.StepJournal
import org.junit.Assert.*
import org.junit.Test

class ObservationCompactorTest {
    @Test fun localCompressionPreservesQuotedTextAndPrioritizesClickableEditableAndTextRows() {
        val source = "window: test/.Window bounds=[0,0][200,200] nodes=105\n" +
            (1..100).joinToString("\n") { "#n$it ViewGroup id=layout$it [0,0][200,200]" } +
            "\n#n101 TextView \"Readable c=(1,2) bounds=[1,2][3,4]\" c=(3,4)" +
            "\n#n102 Button clickable \"Go\" c=(2,3)\n#n103 EditText editable \"Input\" c=(4,5)"
        val packed = ObservationCompactor.compact(jsonObject("text" to source.json(), "snapshotId" to "same-id".json()), 1024, true).asJsonObject
        val text = packed.string("text")!!
        assertTrue(text.contains("#n101")); assertTrue(text.contains("#n102")); assertTrue(text.contains("#n103"))
        assertTrue(text.contains("Readable c=(1,2) bounds=[1,2][3,4]"))
        assertFalse(text.contains("c=(4,5)")); assertFalse(text.contains("ViewGroup"))
        assertEquals("same-id", packed.string("snapshotId")); assertTrue(StepJournal.bytes(packed) <= 1024)
    }
    @Test fun localSnapshotNeverKeepsMoreThanSeventyRows() {
        val source = "window: test nodes=100\n" + (1..100).joinToString("\n") { "#n$it Button clickable \"Button$it\" c=(1,2)" }
        val packed = ObservationCompactor.compact(jsonObject("text" to source.json()), 16 * 1024, true).asJsonObject
        assertEquals(70, packed.string("text")!!.lines().count { it.startsWith("#n") })
        assertTrue(packed.flag("truncated")!!)
    }
    @Test fun remoteTruncationSelectsActionableTailInsteadOfContainerPrefix() {
        val source = "window: test nodes=101\n" + (1..100).joinToString("\n") { "#n$it ViewGroup [0,0][100,100]" } +
            "\n#n101 Button clickable \"Final button\" c=(1,2)"
        val packed = ObservationCompactor.compact(jsonObject("text" to source.json()), 400, false)
        assertTrue(packed.toString().contains("#n101")); assertTrue(StepJournal.bytes(packed) <= 400)
    }
    @Test fun genericObservationsRemainValidUnicodeAndSmallJsonIsUnchanged() {
        val small = jsonObject("error" to "NODE_REF_STALE".json())
        assertEquals(small, ObservationCompactor.compact(small, 128, true))
        val packed = ObservationCompactor.compact(jsonObject("value" to "😀\n\"".repeat(300).json()), 256, false)
        assertTrue(StepJournal.bytes(packed) <= 256)
        assertEquals(packed, AgentJson.parse(packed.toString()))
        assertTrue(packed.asJsonObject.flag("truncated")!!)
    }
}
