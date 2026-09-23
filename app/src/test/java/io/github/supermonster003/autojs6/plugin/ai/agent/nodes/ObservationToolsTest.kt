package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

internal fun dump(id: String = "s1", rows: List<String> = listOf("#n1 Button clickable \"Go\" id=go c=(10,20)"), truncated: Boolean = false, pkg: String = "test") =
    jsonObject("format" to "compact".json(), "snapshotId" to id.json(), "nodeCount" to rows.size.json(), "truncated" to truncated.json(),
        "packageName" to pkg.json(), "activityName" to "Activity".json(), "text" to ("window: $pkg/.Activity  nodes=${rows.size}\n" + rows.joinToString("\n")).trimEnd().json())

class ObservationToolsTest {
    private fun transform(name: String, value: JsonElement, args: JsonObject = JsonObject(), tools: ObservationTools = ObservationTools()): JsonElement =
        tools.transform(ToolInvocation(name, args, ToolPlan.Local(name, args)), value)
    @Test fun hostCompactEscapesAndFlagsAreParsedWithoutTreatingQuotedTextAsMetadata() {
        val value = dump(rows = listOf("#n1  TextView \"Go \\\"clickable\\\" \\n中文\" desc=\"x\\ty\" id=label [-2,0][20,40]"))
        val node = CompactNodeText.parse(value).nodes.single()
        assertEquals(1, node.depth); assertEquals("Go \"clickable\" \n中文", node.text)
        assertEquals("x\ty", node.description); assertTrue(node.flags.isEmpty()); assertEquals(-2, node.bounds.left)
    }
    @Test fun emptyTreeAndHostTruncationFooterAreAccepted() {
        assertTrue(CompactNodeText.parse(dump(rows = emptyList())).nodes.isEmpty())
        val value = dump(truncated = true).apply { addProperty("text", string("text") + "\n(the maxNodes=1 budget cut the tree; use ui_find)") }
        assertTrue(CompactNodeText.parse(value).truncated)
    }
    @Test fun malformedReferencesCountsCoordinatesAndUnknownFlagsAreRejected() {
        for (value in listOf(dump(rows = listOf("#n2 Button c=(1,1)")), dump().apply { addProperty("nodeCount", 2) },
            dump(rows = listOf("#n1 Button [3,2][1,0]")), dump(rows = listOf("#n1 Button invented c=(1,1)")),
            dump(rows = listOf("#n1 Button \"unclosed c=(1,1)")), dump().apply { addProperty("snapshotId", "") })) {
            assertThrows(IllegalArgumentException::class.java) { CompactNodeText.parse(value) }
        }
    }
    @Test fun croppedEscapeAndEllipsisRemainDisplayOnly() {
        val value = dump(rows = listOf("#n1 TextView \"clipped\\...\" c=(1,1)"))
        val node = CompactNodeText.parse(value).nodes.single()
        assertEquals("clipped\\...", node.text); assertFalse(node.relocatable)
    }
    @Test fun dumpRegistersSnapshotAndPreservesBoundedChanges() {
        val tools = ObservationTools()
        assertTrue(transform("ui_dump", dump(), tools = tools).asJsonObject.getAsJsonObject("changes").flag("baseline")!!)
        val next = transform("ui_dump", dump("s2", listOf("#n1 Button \"Done\" c=(10,20)")), tools = tools).asJsonObject
        assertEquals("s2", next.string("snapshotId"))
        assertEquals(jsonArray("Done".json()), next.getAsJsonObject("changes")["addedText"])
        assertEquals(jsonArray("Go".json()), next.getAsJsonObject("changes")["removedText"])
        assertEquals("s2", tools.nodes.resolve("#n1").snapshotId)
    }
    @Test fun denseDumpKeepsWholeRowsAndSnapshotIdentityWithinObservationBudget() {
        val value = dump(rows = (1..400).map { "#n$it Button clickable \"${"中文".repeat(18)}\" id=button$it c=(1,1)" })
        val packed = transform("ui_dump", value).asJsonObject
        assertEquals("s1", packed.string("snapshotId")); assertTrue(packed.flag("truncated")!!)
        assertTrue(StepJournal.bytes(packed) <= ObservationTools.MAX_BYTES)
        assertTrue(packed.string("text")!!.lines().filter { it.startsWith("#n") }.all { it.endsWith("c=(1,1)") })
    }
    private fun node(text: String) = jsonObject("text" to text.json(), "desc" to "".json(), "id" to "id".json(), "className" to "TextView".json(),
        "bounds" to jsonObject("left" to 0.json(), "top" to 0.json(), "right" to 10.json(), "bottom" to 20.json()), "clickable" to false.json(), "enabled" to true.json())
    @Test fun findPreservesNodeDescriptorsAndSignalsClippedEntries() {
        val values = JsonArray().apply { repeat(12) { add(node("😀".repeat(300))) } }
        val result = transform("ui_find", values).asJsonObject
        assertEquals(10, result.getAsJsonArray("nodes").size()); assertEquals(12L, result.number("total")); assertTrue(result.flag("truncated")!!)
        assertTrue(result.getAsJsonArray("nodes")[0].asJsonObject.flag("truncated")!!)
        AgentJson.parse(result.toString())
    }
    @Test fun consoleSplitsMessagesBeforeTakingNewestLinesAndRedactsCredentials() {
        val source = jsonObject("entries" to jsonArray(jsonObject("text" to "old\nnew\ntoken=private".json())), "truncated" to false.json())
        val result = transform("console_tail", source, jsonObject("lines" to 2.json())).asJsonObject
        assertEquals(jsonArray("new".json(), "token=***".json()), result["lines"]); assertTrue(result.flag("truncated")!!)
        assertEquals("global-window", result.string("consoleCaptureMode"))
    }
    @Test fun waitResultAndScreenStateHaveExplicitMeanings() {
        val found = jsonObject("matched" to true.json(), "state" to "appear".json(), "node" to node("seen"))
        assertEquals("seen", transform("ui_wait_for", found).asJsonObject.getAsJsonObject("node").string("text"))
        assertEquals(jsonObject("screenOn" to false.json()), transform("screen_state", false.json()))
        assertThrows(IllegalArgumentException::class.java) { transform("screen_state", "false".json()) }
    }
}
