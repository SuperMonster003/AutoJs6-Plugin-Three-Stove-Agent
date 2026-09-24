package io.github.supermonster003.autojs6.plugin.ai.agent.store

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class RunHistoryCodecTest {
    @Test fun roundTripRetainsAllStepsAndExplicitNullScriptResult() {
        val run = fixture(1).apply {
            add("steps", JsonArray().apply { for (i in 1..200) add(step(i)) })
            add("result", jsonObject("status" to "completed".json(), "script" to jsonObject("result" to JsonNull.INSTANCE)))
        }
        val decoded = RunHistoryCodec.decode(RunHistoryCodec.encode(run, 55))
        assertEquals(run, decoded.run); assertEquals(55, decoded.accessedAt)
    }
    @Test fun unknownVersionAndDuplicateKeysFailClosed() {
        val encoded = RunHistoryCodec.encode(fixture(1), 1)
        rejects { RunHistoryCodec.decode(encoded.replace("\"version\":1", "\"version\":2")) }
        rejects { RunHistoryCodec.decode(encoded.replace("\"version\":1", "\"version\":1,\"version\":1")) }
    }
    @Test fun unsafeIdsInvalidStateAndWrongFieldTypesAreRejected() {
        for ((key, value) in listOf("runId" to "../secret".json(), "state" to "surprise".json(), "goal" to 55.json(),
            "startedAt" to (-1).json(), "preset" to JsonNull.INSTANCE, "steps" to JsonObject())) {
            rejects { RunHistoryCodec.encode(fixture(1).apply { add(key, value) }, 1) }
        }
    }
    @Test fun oversizedAndDeepRecordsAreRejected() {
        rejects { RunHistoryCodec.decode(" ".repeat(RunHistoryCodec.MAX_BYTES + 1)) }
        rejects { RunHistoryCodec.encode(fixture(1).apply { addProperty("goal", "中".repeat(2000)) }, 1) }
        val run = fixture(1).apply { add("steps", JsonArray().apply { for (i in 1..201) add(step(i)) }) }
        rejects { RunHistoryCodec.encode(run, 1) }
    }
    @Test fun nonMonotonicStepsAndMalformedResultsCannotReachReplayUi() {
        rejects { RunHistoryCodec.encode(fixture(1).apply { add("steps", jsonArray(step(2), step(1))) }, 1) }
        rejects { RunHistoryCodec.encode(fixture(1).apply { add("result", jsonObject("status" to "completed".json(), "evidence" to jsonArray(3.json()))) }, 1) }
        rejects { RunHistoryCodec.encode(fixture(1).apply { add("steps", jsonArray(step(1).apply { add("decision", jsonObject("ask" to "broken".json())) })) }, 1) }
        rejects { RunHistoryCodec.encode(fixture(1).apply { add("budget", jsonObject("maxSteps" to "broken".json())) }, 1) }
    }
    @Test fun exportDoesNotLeakProseDynamicKeysOrNumericPersonalData() {
        val secret = "13800123456 Private Road token=abc"
        val run = fixture(1).apply {
            addProperty("goal", secret); addProperty("preset", secret)
            add("pending", jsonObject("answer" to secret.json()))
            add("steps", jsonArray(step(1).apply {
                addProperty("tool", "ui_click"); addProperty("confirmation", "allowed")
                add("arguments", jsonObject(secret to 13800123456L.json()))
                addProperty("observation", secret); add("decision", jsonObject("reasoning" to secret.json()))
                add("usage", jsonObject("inputTokens" to 88.json(), secret to secret.json()))
            }))
            add("result", jsonObject("status" to "completed".json(), "summary" to secret.json(), "script" to jsonObject("result" to jsonObject(secret to secret.json()))))
        }
        val export = RunHistoryExport.redact(run, setOf("ui_click"))
        assertFalse(export.toString().contains(secret)); assertFalse(export.toString().contains("13800123456"))
        assertTrue(run.toString().contains(secret)); assertEquals("ui_click", export.getAsJsonArray("steps")[0].asJsonObject.string("tool"))
        assertEquals(88L, export.getAsJsonArray("steps")[0].asJsonObject.getAsJsonObject("usage").number("inputTokens"))
        assertFalse(export.has("pending")); assertFalse(export.has("startedAt"))
    }
    @Test fun filtersCombineStatusPresetAndInclusiveStartExclusiveEnd() {
        val filter = RunHistoryFilter("completed", "default", 10, 20)
        assertTrue(filter.matches(fixture(10))); assertTrue(filter.matches(fixture(19)))
        assertFalse(filter.matches(fixture(9))); assertFalse(filter.matches(fixture(20)))
        assertFalse(filter.matches(fixture(10).apply { addProperty("state", "failed") }))
        assertFalse(filter.matches(fixture(10).apply { addProperty("preset", "other") }))
    }
    companion object {
        fun fixture(index: Int, state: String = "completed") = jsonObject("runId" to ("00000000-0000-0000-0000-" + index.toString().padStart(12, '0')).json(),
            "goal" to "Fixture $index".json(), "state" to state.json(), "startedAt" to index.json(), "preset" to "default".json(), "steps" to JsonArray())
        fun step(index: Int) = jsonObject("index" to index.json(), "kind" to "tool".json(), "decision" to JsonObject(), "elapsedMs" to 123.json())
        fun rejects(action: () -> Unit) { try { action(); fail("Expected rejection") } catch (_: IllegalArgumentException) { } catch (_: IllegalStateException) { } }
    }
}
