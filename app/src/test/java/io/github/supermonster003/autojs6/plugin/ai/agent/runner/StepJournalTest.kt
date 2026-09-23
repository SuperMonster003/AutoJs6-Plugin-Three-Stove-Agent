package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import com.google.gson.JsonArray
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class StepJournalTest {
    private fun record(index: Int, text: String = "ok", observation: String = text) = StepRecord(index, "tool",
        jsonObject("kind" to "tool".json(), "tool" to "ui_set_text".json(), "reasoning" to text.json()),
        "ui_set_text", jsonObject("text" to text.json()), "allowed", observation, elapsedMs = 2)
    private fun result(summary: String) = jsonObject("id" to "test".json(), "status" to "completed".json(),
        "summary" to summary.json(), "steps" to 1.json(), "toolCalls" to 1.json(), "usage" to jsonObject("modelCalls" to 1.json(), "estimated" to true.json()))
    @Test fun retainsAtMostTwoHundredRecordsAndReturnsDefensiveSnapshots() {
        val journal = StepJournal()
        repeat(200) { journal.append(record(it + 1)) }
        assertEquals(200, journal.history().size)
        journal.history()[0].addProperty("kind", "changed")
        journal.snapshot().getAsJsonArray("steps").remove(0)
        assertEquals("tool", journal.history()[0].string("kind")); assertEquals(200, journal.history().size)
        assertThrows(IllegalArgumentException::class.java) { journal.append(record(201)) }
        val short = StepJournal(maxSteps = 2)
        repeat(3) { short.append(record(it + 1)) }
        assertEquals(listOf(2L, 3L), short.history().map { it.number("index") }); assertTrue(short.truncated)
    }
    @Test fun byteBoundIncludesJsonEscapingWrapperAndTerminalResult() {
        val journal = StepJournal()
        repeat(200) { journal.append(record(it + 1, "x".repeat(1500), "a".repeat(4000))) }
        assertTrue(journal.truncated); assertEquals(200L, journal.history().last().number("index"))
        journal.finish(result("done"))
        assertTrue(StepJournal.bytes(journal.snapshot()) <= RunLimits.JOURNAL_BYTES)
        val small = StepJournal(maxBytes = 2048)
        val entry = small.append(record(1, "\u0001".repeat(2000), "\u0002".repeat(4000)))
        assertEquals(1L, entry.number("index")); assertEquals("tool", entry.string("kind"))
        val final = small.finish(result("\u0001".repeat(1000)).apply { add("evidence", JsonArray().apply { repeat(8) { add("\u0002".repeat(200)) } }) })
        assertEquals("test", final.string("id")); assertEquals("completed", final.string("status")); assertTrue(final.has("usage"))
        assertTrue(StepJournal.bytes(small.snapshot()) <= 2048)
        assertEquals(small.snapshot(), AgentJson.objectOf(small.snapshot().toString()))
    }
    @Test fun redactsPasswordAcrossArgumentsReasoningObservationsAndExistingHistory() {
        val journal = StepJournal()
        val password = "sample\n\"secret"
        journal.append(record(1, password, jsonObject("password" to password.json()).toString()))
        journal.protectText(password)
        val next = journal.append(record(2, password))
        assertEquals("***", next.getAsJsonObject("arguments").string("text"))
        assertFalse(journal.snapshot().toString().contains("secret"))
        assertEquals("***", journal.finish(result(password)).string("summary"))
        assertThrows(IllegalStateException::class.java) { journal.append(record(3)) }
        assertThrows(IllegalStateException::class.java) { journal.finish(result("again")) }
    }
    @Test fun secretsThatMatchProtocolWordsNeverCorruptStructuralFields() {
        val journal = StepJournal()
        journal.protectText("tool"); journal.protectText("completed")
        val entry = journal.append(record(1, "tool"))
        assertEquals("tool", entry.string("kind")); assertEquals("tool", entry.getAsJsonObject("decision").string("kind"))
        val final = journal.finish(result("completed"))
        assertEquals("completed", final.string("status")); assertEquals("***", final.string("summary"))
    }
}
