package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.StepJournal
import org.junit.Assert.*
import org.junit.Test

class OcrScreenObservationTest {
    private fun part(text: String, x: Int = 0, y: Int = 0, confidence: Double = .9) = jsonObject("text" to text.json(),
        "bounds" to jsonObject("left" to x.json(), "top" to y.json(), "right" to (x + 20).json(), "bottom" to (y + 10).json()), "confidence" to JsonPrimitive(confidence))
    @Test fun adjacentWordsAreOrderedAndMergedWithUnionBoundsAndConservativeConfidence() {
        val result = OcrScreenObservation.normalize(jsonArray(part("world", 25, confidence = .8), part("Hello")))
        val line = result.getAsJsonArray("lines").single().asJsonObject
        assertEquals("Hello world", line.string("text")); assertEquals(45L, line.getAsJsonObject("bounds").number("right")); assertEquals(.8, line["confidence"].asDouble, 0.0)
    }
    @Test fun columnsDifferentRowsAndOverlappingBoxesAreNotMerged() {
        val result = OcrScreenObservation.normalize(jsonArray(part("a"), part("b", 200), part("c", 0, 30), part("d", 0, 0)))
        assertEquals(4, result.getAsJsonArray("lines").size())
    }
    @Test fun multilineSourcePreservesSharedBlockBoundsWithoutInventedLineCoordinates() {
        val result = OcrScreenObservation.normalize(jsonArray(part("line1\nline2")))
        val lines = result.getAsJsonArray("lines")
        assertEquals(2, lines.size()); assertEquals("source-block", lines[0].asJsonObject.string("boundsScope"))
        assertEquals(lines[0].asJsonObject["bounds"], lines[1].asJsonObject["bounds"])
    }
    @Test fun absentBoundsAndConfidenceAreKeptUnknownAndCredentialsAreRedacted() {
        val result = OcrScreenObservation.normalize(jsonArray(jsonObject("text" to "token=private".json(), "bounds" to JsonNull.INSTANCE, "confidence" to JsonNull.INSTANCE)))
        val line = result.getAsJsonArray("lines").single().asJsonObject
        assertEquals("token=***", line.string("text")); assertTrue(line["bounds"].isJsonNull); assertTrue(line["confidence"].isJsonNull)
    }
    @Test fun malformedAndOversizedHostResultsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { OcrScreenObservation.normalize(jsonArray(part("a").apply { getAsJsonObject("bounds").addProperty("right", -1) })) }
        assertThrows(IllegalArgumentException::class.java) { OcrScreenObservation.normalize(JsonArray().apply { repeat(401) { add(part("a")) } }) }
    }
    @Test fun largeObservationsRetainWholeLinesWithinJsonBudget() {
        val result = OcrScreenObservation.normalize(JsonArray().apply { repeat(70) { add(part("😀".repeat(100), 0, it * 30)) } })
        assertTrue(result.flag("truncated")!!); assertTrue(StepJournal.bytes(result) <= ObservationTools.MAX_BYTES)
        assertTrue(result.getAsJsonArray("lines").size() in 1..69); AgentJson.parse(result.toString())
    }
    @Test fun availabilityRequiresHostDiscoveryAndEveryGrantWithoutOverridingUserGroups() {
        val methods = setOf("accessibility.readScreenText"); val permissions = setOf("accessibility", "screen_capture", "ocr")
        assertTrue(ObservationCapabilities.ocrAvailable(methods, methods, permissions))
        assertFalse(ObservationCapabilities.ocrAvailable(emptySet(), methods, permissions))
        assertFalse(ObservationCapabilities.ocrAvailable(methods, emptySet(), permissions))
        permissions.forEach { assertFalse(ObservationCapabilities.ocrAvailable(methods, methods, permissions - it)) }
        val spec = F.catalog()["ocr_screen"]!!
        assertTrue(ToolPolicy().withOcrAvailability(true).isEnabled(spec))
        assertFalse(ToolPolicy(mapOf(ToolGroup.OCR to false)).withOcrAvailability(true).isEnabled(spec))
        assertFalse(ToolPolicy(availableTools = emptySet()).withOcrAvailability(true).isEnabled(spec))
        assertFalse(ToolPolicy(ocrAvailable = true).withOcrAvailability(false).isEnabled(spec))
    }
}
