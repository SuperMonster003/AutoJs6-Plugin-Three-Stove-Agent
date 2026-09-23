package io.github.supermonster003.autojs6.plugin.ai.agent.service

import com.google.gson.JsonArray
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.StepJournal
import org.junit.Assert.*
import org.junit.Test

class RunSnapshotTest {
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
