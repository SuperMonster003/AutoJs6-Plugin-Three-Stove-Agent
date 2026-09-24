package io.github.supermonster003.autojs6.plugin.ai.agent.service

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import java.util.concurrent.atomic.AtomicBoolean

/** Preparation is read-only; execute is reachable only after the runner's per-proposal confirmation. */
internal class MemoryTools(private val repository: MemoryRepository, private val preset: String, private val scope: String,
                           private val runId: () -> String, private val alive: () -> Boolean, private val delegate: RunTools) : RunTools {
    private class Change(val key: String, val value: String, val scope: String, val before: MemoryEntry?) { val used = AtomicBoolean() }
    private fun allowed(value: String) = (value == "global" && scope in setOf("global", "global_and_preset")) ||
        (value == preset && scope in setOf("preset", "global_and_preset"))
    override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
        if (invocation.name !in NAMES) return delegate.prepare(invocation, timeoutMs, callback)
        if (!alive()) { callback(PortResult.Failure(RunError.HOST_UNAVAILABLE)); return Cancellation.NONE }
        if (scope == "none") { callback(PortResult.Failure(RunError.TOOL_DISABLED)); return Cancellation.NONE }
        return repository.query({ store ->
            check(alive())
            val args = (invocation.plan as ToolPlan.Local).arguments
            if (invocation.name == "memory_get") {
                args.getAsJsonArray("keys")?.forEach { MemoryCodec.key(it.asString) }
                PreparedTool(invocation, ToolMetadata())
            } else {
                val key = requireNotNull(args.string("key")); val value = requireNotNull(args.string("value"))
                val requested = MemoryCodec.scope(requireNotNull(args.string("scope")))
                require(allowed(requested)); MemoryCodec.preference(key, value)
                PreparedTool(invocation, ToolMetadata(forceConfirmation = true, memoryScope = requested),
                    Change(key, value, requested, store.snapshot().find { it.identity == requested to key }))
            }
        }) { result -> callback(result.fold({ PortResult.Success(it) }, { PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID) })) }
    }
    override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
        if (prepared.invocation.name !in NAMES) return delegate.execute(prepared, timeoutMs, callback)
        return repository.query({ store ->
            check(alive())
            if (prepared.invocation.name == "memory_get") {
                val args = (prepared.invocation.plan as ToolPlan.Local).arguments
                val keys = args.getAsJsonArray("keys")?.map { MemoryCodec.key(it.asString) }?.toSet()
                val selected = MemoryContext.select(store.snapshot(), preset, allowed("global"), allowed(preset), keys, 24 * 1024)
                ToolReply(jsonObject("entries" to selected.entries, "truncated" to selected.truncated.json()))
            } else {
                val change = requireNotNull(prepared.opaqueContext as? Change)
                require(allowed(change.scope) && change.scope == prepared.metadata.memoryScope && change.used.compareAndSet(false, true))
                val now = maxOf(System.currentTimeMillis(), change.before?.updatedAt ?: 0)
                val row = MemoryEntry(change.key, change.value, change.scope, runId(), change.before?.createdAt ?: now, now)
                store.put(row, change.before)
                ToolReply(jsonObject("saved" to true.json(), "key" to change.key.json(), "scope" to change.scope.json()))
            }
        }) { result -> callback(result.fold({ PortResult.Success(it) }, { PortResult.Failure(RunError.INVALID_REQUEST) })) }
    }
    companion object { val NAMES = setOf("memory_get", "memory_propose") }
}
