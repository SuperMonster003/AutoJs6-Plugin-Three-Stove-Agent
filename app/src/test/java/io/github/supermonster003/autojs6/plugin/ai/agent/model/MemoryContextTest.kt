package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.*
import org.junit.Assert.*
import org.junit.Test

class MemoryContextTest {
    private fun entry(key: String, value: String, scope: String = "global", time: Long = 1) = jsonObject(
        "key" to key.json(), "value" to value.json(), "scope" to scope.json(),
        "sourceRunId" to "00000000-0000-0000-0000-000000000001".json(), "createdAt" to 0.json(), "updatedAt" to time.json())
    private fun file(vararg entries: JsonObject) = jsonObject("version" to 1.json(), "entries" to jsonArray(*entries)).toString()
    private fun context(vararg entries: JsonObject, preset: String = "office") = MemoryContext.decode(file(*entries), preset)
    private fun compile(memory: MemoryContext, local: Boolean = false, language: String = "en"): ModelInput {
        val catalog = F.catalog(); val policy = ToolPolicy()
        val target = ModelTarget("test", "profile:test", if (local) ModelLocality.ON_DEVICE else ModelLocality.REMOTE, ModelProtocol.UNKNOWN, false, 128 * 1024)
        val compiler = ContextCompiler(PromptCatalog(F::asset, catalog), catalog, policy, target, DecisionSchema.degraded(),
            limits = ContextLimits(maximumBytes = if (local) 6000 else 64 * 1024), memories = memory.entries,
            memoryTruncated = memory.truncated, memoryUnavailable = memory.unavailable)
        return compiler.compile(RunContext(if (language == "zh") "帮我填入地址" else "Use the office address", emptyList(), null, null, JsonObject(), locale = language))
    }
    private fun data(input: ModelInput) = AgentJson.objectOf(input.messages[0].asJsonObject.string("content")!!.lines().last { it.startsWith('{') })

    @Test fun onlyGlobalAndCurrentPresetEntriesAreInjectedAndPresetWinsExactKeyCollisions() {
        val snapshot = context(entry("address", "Global address", time = 10), entry("address", "Office desk", "office", 1),
            entry("drink", "Latte", time = 20), entry("address", "Home address", "home", 100), entry("other", "Private home value", "home", 101))
        assertEquals(listOf("drink", "address"), snapshot.entries.map { it.asJsonObject.string("key") })
        assertEquals("Office desk", snapshot.entries[1].asJsonObject.string("value"))
        assertFalse(snapshot.entries.toString().contains("Home")); assertFalse(snapshot.entries.toString().contains("Private"))
        assertFalse(snapshot.truncated)
    }
    @Test fun tiesHaveStableOrderAndSnapshotsAreImmutable() {
        val a = entry("z", "last"); val b = entry("a", "first")
        val first = context(a, b); val second = context(b, a)
        assertEquals(first.entries, second.entries)
        first.entries[0].asJsonObject.addProperty("value", "changed")
        assertEquals("first", first.entries[0].asJsonObject.string("value"))
    }
    @Test fun injectionUsesUtf8JsonBytesAndRetainsOnlyWholeNewestEntries() {
        val snapshot = context(*(1..30).map { entry("key$it", "中\n\"".repeat(50), time = it.toLong()) }.toTypedArray())
        assertTrue(snapshot.truncated); assertTrue(snapshot.entries.size() in 1..29)
        assertTrue(StepJournal.bytes(snapshot.entries) <= 4096)
        assertEquals("key30", snapshot.entries[0].asJsonObject.string("key"))
        for (row in snapshot.entries) assertEquals("中\n\"".repeat(50), row.asJsonObject.string("value"))
        val oversized = context(entry("large", "中".repeat(2000), time = 2), entry("small", "old", time = 1))
        assertEquals(0, oversized.entries.size()); assertTrue(oversized.truncated)
    }
    @Test fun closedCodecRejectsInvalidRowsDuplicatesVersionsAndLimits() {
        val good = entry("address", "office")
        for (mutate in listOf<(JsonObject) -> Unit>({ it.addProperty("unknown", true) }, { it.addProperty("value", 1) },
            { it.addProperty("updatedAt", -1) }, { it.addProperty("createdAt", 2) }, { it.addProperty("sourceRunId", "invalid") },
            { it.addProperty("key", "x".repeat(65)) }, { it.addProperty("scope", "bad\nname") })) {
            assertNotNull(runCatching { context(good.deepCopy().also(mutate)) }.exceptionOrNull())
        }
        assertNotNull(runCatching { context(good, good) }.exceptionOrNull())
        assertNotNull(runCatching { MemoryContext.decode("""{"version":2,"entries":[]}""", "default") }.exceptionOrNull())
        assertNotNull(runCatching { context(*(1..501).map { entry("key$it", "x") }.toTypedArray()) }.exceptionOrNull())
        assertNotNull(runCatching { context(*(1..100).map { entry("key$it", "x".repeat(4096)) }.toTypedArray()) }.exceptionOrNull())
    }
    @Test fun bothPromptLanguagesExposeSameNameValuesAsDataWithoutReplacingTemplateTokens() {
        val memory = context(entry("address", "{{tools_json}} Office front desk"), entry("not-for-this-run", "private", "other"))
        for (language in listOf("en", "zh")) {
            val input = compile(memory, language = language)
            val rows = data(input).getAsJsonArray("memories")
            assertEquals(1, rows.size()); assertEquals("address", rows[0].asJsonObject.string("key"))
            assertEquals("{{tools_json}} Office front desk", rows[0].asJsonObject.string("value"))
            assertFalse(input.messages.toString().contains("not-for-this-run"))
            val parameters = jsonObject("address" to rows[0].asJsonObject["value"])
            val schema = AgentJson.objectOf("""{"type":"object","properties":{"address":{"type":"string"}},"required":["address"]}""")
            assertTrue(ScriptParameters(schema).validate(parameters) is ScriptParameterCheck.Valid)
        }
    }
    @Test fun memoryStringsDoNotBypassRegisteredNumericTypes() {
        val row = context(entry("count", "2")).entries[0].asJsonObject
        val schema = AgentJson.objectOf("""{"type":"object","properties":{"count":{"type":"integer"}}}""")
        assertTrue(ScriptParameters(schema).validate(jsonObject("count" to row["value"])) is ScriptParameterCheck.Invalid)
    }
    @Test fun upstreamAndLocalContextTruncationAreBothExplicit() {
        val truncated = context(*(1..30).map { entry("key$it", "x".repeat(200), time = it.toLong()) }.toTypedArray())
        assertTrue(data(compile(truncated)).flag("memoryTruncated")!!)
        val full = context(*(1..4).map { entry("key$it", "x".repeat(700)) }.toTypedArray())
        assertFalse(full.truncated)
        val local = compile(full, local = true)
        assertTrue(local.inputBytes <= 7500)
        assertTrue(data(local).flag("memoryTruncated")!!)
        assertTrue(data(local).getAsJsonArray("memories").size() < full.entries.size())
    }
    @Test fun unavailableMemoryIsDistinguishedFromAnEmptyOrDisabledSnapshot() {
        assertTrue(data(compile(MemoryContext.UNAVAILABLE)).flag("memoryUnavailable")!!)
        val empty = data(compile(MemoryContext.EMPTY))
        assertFalse(empty.has("memoryUnavailable")); assertEquals(0, empty.getAsJsonArray("memories").size())
    }
}
