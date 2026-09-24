package io.github.supermonster003.autojs6.plugin.ai.agent.service

import com.google.gson.JsonArray
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.StepJournal
import org.junit.Assert.*
import org.junit.Test

class RunSnapshotTest {
    @Test fun processRecoveryFailsEveryUnfinishedStateAndPreservesTerminalHistory() {
        for (state in io.github.supermonster003.autojs6.plugin.ai.agent.runner.RunState.entries) {
            val source = jsonObject("state" to state.wire.json(), "goal" to "fixture".json(),
                "steps" to JsonArray(), "pending" to jsonObject("requestId" to "old".json()))
            if (state.terminal) source.add("result", jsonObject("status" to state.wire.json(), "error" to "HOST_UNAVAILABLE".json()))
            val before = source.deepCopy()
            RunArchive.recoverInterrupted(source)
            if (state.terminal) assertEquals(before, source)
            else {
                assertEquals("failed", source.string("state")); assertFalse(source.has("pending"))
                assertEquals("process-died", source.getAsJsonObject("result").string("error"))
                assertEquals("failed", source.getAsJsonObject("result").string("status"))
                assertEquals(before["goal"], source["goal"]); assertEquals(before["steps"], source["steps"])
            }
            val recovered = source.deepCopy()
            RunArchive.recoverInterrupted(source)
            assertEquals(recovered, source)
        }
    }
    @Test fun boundedRecentStepsPreserveResultAndCannotMutateThePrivateArchive() {
        val source = jsonObject("goal" to "中".repeat(1300).json(), "state" to "completed".json(),
            "result" to jsonObject("status" to "completed".json(), "summary" to "verified".json()),
            "steps" to JsonArray().apply { repeat(200) { add(jsonObject("index" to it.json(), "observation" to "x".repeat(4000).json())) } })
        val snapshot = RunArchive.project(source, 50)
        assertTrue(StepJournal.bytes(snapshot) <= 32 * 1024); assertTrue(snapshot.flag("truncated") == true)
        val steps = snapshot.getAsJsonArray("steps")
        assertEquals(199, steps.last().asJsonObject.number("index")!!.toInt())
        assertTrue(steps.size() in 1..7); assertEquals("completed", snapshot.getAsJsonObject("result").string("status"))
        steps.last().asJsonObject.addProperty("index", -1)
        assertEquals(199, source.getAsJsonArray("steps").last().asJsonObject.number("index")!!.toInt())
        assertEquals(1, RunArchive.project(source, 1).getAsJsonArray("steps").size())
    }
}
