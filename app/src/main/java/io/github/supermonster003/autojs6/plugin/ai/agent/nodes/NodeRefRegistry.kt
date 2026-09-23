package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import com.google.gson.JsonArray
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.autojs.plugin.ai.agent.api.AiAgentContract

/** Run-owned display snapshots. Host snapshot IDs remain authoritative for every real action. */
class NodeRefRegistry(private val capacity: Int = AiAgentContract.MAX_SNAPSHOTS_PER_LINK) : AutoCloseable {
    init { require(capacity in 1..AiAgentContract.MAX_SNAPSHOTS_PER_LINK) }
    data class Reference(val snapshotId: String, val node: CompactNodeText.Node)
    class Stale : IllegalArgumentException("NODE_REF_STALE")
    private val snapshots = linkedMapOf<String, CompactNodeText.Snapshot>()
    private var latest: CompactNodeText.Snapshot? = null
    private var closed = false

    @Synchronized fun record(snapshot: CompactNodeText.Snapshot): com.google.gson.JsonObject {
        check(!closed)
        val previous = latest
        require(!snapshots.containsKey(snapshot.id))
        val windowChanged = previous != null && previous.window != snapshot.window
        if (windowChanged) snapshots.clear()
        snapshots[snapshot.id] = snapshot; latest = snapshot
        while (snapshots.size > capacity) snapshots.remove(snapshots.keys.first())
        fun labels(nodes: List<CompactNodeText.Node>) = nodes.mapNotNull { node ->
            (node.text.ifEmpty { node.description }).takeIf { it.isNotEmpty() && it != "[password]" }
        }
        fun difference(first: List<String>, second: List<String>): List<String> {
            val remaining = second.groupingBy { it }.eachCount().toMutableMap()
            return first.filter { label -> val count = remaining[label] ?: 0; if (count > 0) remaining[label] = count - 1; count == 0 }
        }
        val before = labels(previous?.nodes.orEmpty()); val after = labels(snapshot.nodes)
        val added = if (previous == null) emptyList() else difference(after, before)
        val removed = if (previous == null) emptyList() else difference(before, after)
        fun bounded(values: List<String>) = JsonArray().apply { values.take(16).forEach { add(AgentJson.truncate(it, 80)) } }
        fun state(s: CompactNodeText.Snapshot) = s.nodes.map { listOf(it.fingerprint(s.window), it.flags.sorted(), it.bounds) }
        return jsonObject("baseline" to (previous == null).json(), "windowChanged" to windowChanged.json(),
            "changed" to (previous != null && (windowChanged || state(previous) != state(snapshot))).json(),
            "previousSnapshotId" to (previous?.id?.json() ?: com.google.gson.JsonNull.INSTANCE),
            "addedText" to bounded(added), "removedText" to bounded(removed), "addedCount" to added.size.json(), "removedCount" to removed.size.json(),
            "partial" to (snapshot.truncated || previous?.truncated == true || added.size > 16 || removed.size > 16 || (added + removed).any { it.toByteArray().size > 80 }).json())
    }
    @Synchronized fun resolve(ref: String, snapshotId: String? = null): Reference {
        if (closed) throw Stale()
        val snapshot = snapshots[snapshotId ?: latest?.id] ?: throw Stale()
        return Reference(snapshot.id, snapshot.nodes.singleOrNull { it.ref == ref } ?: throw Stale())
    }
    /** A conservative display match only. P4 actions must still ask the host to validate the returned ID. */
    @Synchronized fun relocate(ref: String, snapshotId: String): Reference {
        val old = resolve(ref, snapshotId)
        val before = snapshots[snapshotId] ?: throw Stale()
        val current = latest ?: throw Stale()
        if (before.window != current.window || !old.node.relocatable) throw Stale()
        val fingerprint = old.node.fingerprint(before.window)
        val candidate = current.nodes.filter { it.relocatable && it.fingerprint(current.window) == fingerprint && old.node.bounds.permits(it.bounds) }.singleOrNull() ?: throw Stale()
        return Reference(current.id, candidate)
    }
    @Synchronized fun clear() { snapshots.clear(); latest = null }
    @Synchronized override fun close() { clear(); closed = true }
}
