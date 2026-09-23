package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class ScriptCatalogClientTest {
    private class Source : ScriptCatalogSource {
        val calls = mutableListOf<BridgeCall>()
        val replies = mutableListOf<(PortResult<JsonElement>) -> Unit>()
        var cancellations = 0
        override fun load(call: BridgeCall, callback: (PortResult<JsonElement>) -> Unit): Cancellation {
            calls.add(call); replies.add(callback); return Cancellation { cancellations++ }
        }
        fun reply(index: Int = replies.lastIndex, id: String = "clean-downloads") = replies[index](PortResult.Success(jsonArray(ScriptFixtures.entry(id))))
    }
    @Test fun ttlIs60SecondsFromCompletionAndTaskStartForcesRefresh() {
        var time = 0L; val client = ScriptCatalogClient { time }; val source = Source(); val results = mutableListOf<PortResult<ScriptCatalogSnapshot>>()
        client.load(emptySet(), true, 5000, source, results::add); time = 100; source.reply()
        time = 60_099; client.load(emptySet(), false, 5000, source, results::add)
        assertEquals(1, source.calls.size); assertEquals(2, results.size)
        time = 60_100; client.load(emptySet(), false, 5000, source, results::add); source.reply()
        client.load(emptySet(), true, 5000, source, results::add)
        assertEquals(3, source.calls.size)
    }
    @Test fun rootsPartitionTheCacheAndAlwaysRetainTheWorkingDirectory() {
        val client = ScriptCatalogClient { 0 }; val source = Source()
        client.load(setOf("/sdcard/b", "/sdcard/a"), false, 4000, source) {}; source.reply()
        client.load(setOf("/sdcard/a", "/sdcard/b"), false, 4000, source) {}
        client.load(emptySet(), false, 4000, source) {}
        assertEquals(2, source.calls.size)
        assertEquals("agent", source.calls[0].module); assertEquals("listScripts", source.calls[0].method)
        assertEquals("""[".","/sdcard/a","/sdcard/b"]""", source.calls[0].args[0].asJsonObject["roots"].toString())
        assertEquals("""["."]""", source.calls[1].args[0].asJsonObject["roots"].toString())
        assertEquals(500L, source.calls[0].args[0].asJsonObject.number("limit"))
    }
    @Test fun cancellationAndInvalidationFenceLateReplies() {
        val client = ScriptCatalogClient { 0 }; val source = Source(); val results = mutableListOf<PortResult<ScriptCatalogSnapshot>>()
        client.load(emptySet(), false, 5000, source, results::add).cancel(); source.reply()
        assertTrue(results.isEmpty()); assertEquals(1, source.cancellations)
        client.load(emptySet(), false, 5000, source, results::add)
        client.invalidate(); source.reply()
        assertEquals(listOf(PortResult.Failure(RunError.CANCELLED)), results)
        client.load(emptySet(), false, 5000, source, results::add)
        assertEquals(3, source.calls.size)
    }
    @Test fun failuresAreNotCachedAndDuplicateRepliesSettleOnce() {
        val client = ScriptCatalogClient { 0 }; val source = Source(); val results = mutableListOf<PortResult<ScriptCatalogSnapshot>>()
        client.load(emptySet(), false, 5000, source, results::add)
        source.replies[0](PortResult.Success(JsonObject())); source.reply(0)
        assertEquals(listOf(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID)), results)
        client.load(emptySet(), false, 5000, source, results::add); source.reply()
        assertEquals(2, results.size); assertEquals(2, source.calls.size)
    }
    @Test fun olderRefreshCannotOverwriteTheNewSnapshot() {
        val client = ScriptCatalogClient { 0 }; val source = Source(); var last: ScriptCatalogSnapshot? = null
        client.load(emptySet(), true, 5000, source) {}
        client.load(emptySet(), true, 5000, source) {}
        source.reply(1, "new"); source.reply(0, "old")
        client.load(emptySet(), false, 5000, source) { last = (it as PortResult.Success).value }
        assertEquals("new", last!!.entries.single().id)
    }
    @Test fun closingAndNewLinksNeverReuseOldCache() {
        val client = ScriptCatalogClient { 0 }; val source = Source(); val results = mutableListOf<PortResult<ScriptCatalogSnapshot>>()
        client.load(emptySet(), true, 5000, source, results::add); source.reply(); client.close()
        client.load(emptySet(), false, 5000, source, results::add)
        assertEquals(PortResult.Failure(RunError.HOST_UNAVAILABLE), results.last())
        ScriptCatalogClient { 0 }.load(emptySet(), false, 5000, source) {}
        assertEquals(2, source.calls.size)
    }
}
