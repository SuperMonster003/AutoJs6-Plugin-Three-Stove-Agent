package io.github.supermonster003.autojs6.plugin.ai.agent.service

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MemoryToolsTest {
    @get:Rule val temp = TemporaryFolder()
    private val id = "00000000-0000-0000-0000-000000000001"
    private val row = MemoryEntry("drink", "Latte", "global", id, 1, 2)
    private val delegate = object : RunTools {
        override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation = error("Unexpected delegate")
        override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation = error("Unexpected delegate")
    }
    private fun tools(repo: MemoryRepository, scope: String = "global_and_preset", alive: () -> Boolean = { true }) = MemoryTools(repo, "office", scope, { id }, alive, delegate)
    private fun invocation(name: String = "memory_propose", args: String = """{"key":"drink","value":"Tea"}""") = AgentJson.objectOf(args).let {
        ToolInvocation(name, it, ToolHandlers(F.catalog()).prepare(name, it, ToolPolicy()))
    }
    private fun <T> await(start: ((T) -> Unit) -> Unit): T {
        val latch = CountDownLatch(1); var result: T? = null
        start { result = it; latch.countDown() }; assertTrue(latch.await(5, TimeUnit.SECONDS)); return checkNotNull(result)
    }
    private fun <T> query(repo: MemoryRepository, work: (MemoryStore) -> T): T = await<Result<T>> { repo.query(work, it) }.getOrThrow()
    private fun prepare(tools: MemoryTools, input: ToolInvocation = invocation()) = await<PortResult<PreparedTool>> { tools.prepare(input, 5000, it) }
    private fun execute(tools: MemoryTools, value: PreparedTool) = await<PortResult<ToolReply>> { tools.execute(value, 5000, it) }
    @Test fun preparationHasNoWriteAndConfirmedExecutionUsesEffectiveScopeAndActualRunId() = MemoryRepository(temp.newFolder()).use { repo ->
        val tools = tools(repo); val ready = (prepare(tools) as PortResult.Success).value
        assertTrue(query(repo) { it.snapshot() }.isEmpty())
        val gate = ConfirmationGate(ToolPolicy(), ConfirmationMode.DEFAULT)
        val decision = gate.assess(F.catalog()["memory_propose"]!!, ready.metadata)
        assertTrue(decision.required); assertFalse(decision.allowRunScope); assertFalse(gate.allow(decision, ConfirmationScope.RUN))
        assertEquals("global", gate.arguments(ready.invocation.arguments, ready.metadata).string("scope"))
        assertTrue(gate.allow(decision, ConfirmationScope.ONCE)); assertTrue(execute(tools, ready) is PortResult.Success)
        val saved = query(repo) { it.snapshot().single() }
        assertEquals("Tea", saved.value); assertEquals("global", saved.scope); assertEquals(id, saved.sourceRunId)
        assertTrue(saved.updatedAt >= saved.createdAt)
        assertTrue(execute(tools, ready) is PortResult.Failure)
    }
    @Test fun readScopesKeysAndPresetOverrideCannotExposeOtherPresets() = MemoryRepository(temp.newFolder()).use { repo ->
        query(repo) { store -> for (entry in listOf(row, row.copy(value = "Office", scope = "office"), row.copy(key = "hidden", scope = "home"))) store.put(entry, null) }
        fun get(scope: String, args: String = "{}"): JsonObject {
            val tools = tools(repo, scope); val ready = (prepare(tools, invocation("memory_get", args)) as PortResult.Success).value
            return (execute(tools, ready) as PortResult.Success).value.result.asJsonObject
        }
        assertEquals("Office", get("global_and_preset").getAsJsonArray("entries").single().asJsonObject.string("value"))
        assertEquals("Latte", get("global").getAsJsonArray("entries").single().asJsonObject.string("value"))
        assertEquals(0, get("preset", """{"keys":["hidden"]}""").getAsJsonArray("entries").size())
        assertEquals(0, get("global", """{"keys":[]}""").getAsJsonArray("entries").size())
        assertTrue(prepare(tools(repo, "none"), invocation("memory_get", "{}")) is PortResult.Failure)
    }
    @Test fun scopeDefaultsDoNotAuthorizeWritingGlobalFromPresetOnlyTask() = MemoryRepository(temp.newFolder()).use { repo ->
        for (input in listOf(invocation(), invocation(args = """{"key":"drink","value":"Tea","scope":"home"}"""),
            invocation(args = """{"key":"password","value":"fixture","scope":"office"}""")))
            assertTrue(prepare(tools(repo, "preset"), input) is PortResult.Failure)
        assertTrue(prepare(tools(repo, "preset"), invocation(args = """{"key":"drink","value":"Tea","scope":"office"}""")) is PortResult.Success)
        assertTrue(query(repo) { it.snapshot() }.isEmpty())
    }
    @Test fun staleProposalAndDeadHostCannotCommit() = MemoryRepository(temp.newFolder()).use { repo ->
        query(repo) { it.put(row, null) }; var alive = true
        val tools = tools(repo, alive = { alive }); val ready = (prepare(tools) as PortResult.Success).value
        val edit = row.copy(value = "Manual edit", updatedAt = 3); query(repo) { it.put(edit, row) }
        assertTrue(execute(tools, ready) is PortResult.Failure)
        val fresh = (prepare(tools) as PortResult.Success).value; alive = false
        assertTrue(execute(tools, fresh) is PortResult.Failure); assertEquals(listOf(edit), query(repo) { it.snapshot() })
    }
    @Test fun cancelledQueuedMutationIsSkippedAndDisablingInjectionDoesNotEraseMemory() = MemoryRepository(temp.newFolder()).use { repo ->
        query(repo) { it.put(row, null) }
        val release = CountDownLatch(1); val entered = CountDownLatch(1)
        repo.query({ entered.countDown(); check(release.await(5, TimeUnit.SECONDS)) }) { }
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        var callback = false
        repo.query({ it.put(row.copy(value = "Cancelled"), row) }) { callback = true }.cancel()
        release.countDown(); assertEquals(listOf(row), query(repo) { it.snapshot() }); assertFalse(callback)
        assertEquals(0, repo.snapshot("office", false, "global_and_preset").entries.size())
        assertEquals(1, repo.snapshot("office", true, "global_and_preset").entries.size())
    }
    @Test fun explicitlyRequestedOlderMemoryCanBeReadBeyondAutomaticInjectionLimit() = MemoryRepository(temp.newFolder()).use { repo ->
        query(repo) { store -> for (index in 1..8) store.put(row.copy(key = "key$index", value = "中".repeat(1000), updatedAt = index.toLong()), null) }
        assertTrue(repo.snapshot("office", true, "global").truncated)
        val tools = tools(repo); val ready = (prepare(tools, invocation("memory_get", """{"keys":["key1"]}""")) as PortResult.Success).value
        val result = (execute(tools, ready) as PortResult.Success).value.result.asJsonObject
        assertEquals("key1", result.getAsJsonArray("entries").single().asJsonObject.string("key")); assertFalse(result.flag("truncated")!!)
    }
}
