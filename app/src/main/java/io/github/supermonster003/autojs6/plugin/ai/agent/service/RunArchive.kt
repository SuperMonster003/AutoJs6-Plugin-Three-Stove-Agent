package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.util.AtomicFile
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import java.io.File
import java.util.concurrent.Executors

/** Bounded private crash journal and full history.
 * No disk IO occurs on a Binder thread. Restarted tasks are historical blocked records only. */
internal class RunArchive(directory: File, private val legacyDirectory: File? = null) {
    private val store = RunHistoryStore(directory)
    private val disk = Executors.newSingleThreadExecutor { r -> Thread(r, "ai-agent-history").apply { isDaemon = true } }
    private val records = linkedMapOf<String, JsonObject>()
    private val dirty = linkedSetOf<String>()
    private var scheduled = false
    @Volatile var ready = false; private set
    @Volatile var storageFailed = false; private set
    init { disk.execute {
        try {
            val loaded = store.open().associateBy { it.string("runId")!! }.toMutableMap()
            // The old writer retained at most 29 files. Remove a legacy file only after durable migration.
            legacyDirectory?.listFiles { f -> f.name.endsWith(".json") }?.forEach { file ->
                val value = runCatching {
                    require(file.length() <= MAX_RECORD_BYTES)
                    AgentJson.objectOf(AtomicFile(file).openRead().use { it.bufferedReader().readText() }, MAX_RECORD_BYTES, 131_072)
                        .apply { if (!has("preset")) addProperty("preset", "default") }
                        .also { RunHistoryCodec.validate(it); require(file.name == "${it.string("runId")}.json") }
                }.getOrNull() ?: return@forEach
                val id = value.string("runId")!!
                if (id !in loaded) {
                    store.save(value).forEach(loaded::remove)
                    if (store.contains(id)) loaded[id] = value
                }
                AtomicFile(file).delete()
            }
            for ((id, value) in loaded) {
                val state = RunState.entries.firstOrNull { it.wire == value.string("state") } ?: error("Invalid state")
                if (!state.terminal) {
                    value.addProperty("state", RunState.BLOCKED.wire)
                    value.remove("pending")
                    value.add("result", jsonObject("status" to "blocked".json(), "error" to RunError.HOST_UNAVAILABLE.name.json()))
                }
                synchronized(this) { if (!records.containsKey(id)) { records[id] = value; markDirty(id) } }
            }
        } catch (_: Exception) { storageFailed = true }
        finally { ready = true }
    } }
    @Synchronized fun admit(run: AgentRunner, request: StartRequest) {
        records[run.id] = jsonObject("runId" to run.id.json(), "goal" to request.options.goal.json(),
            "state" to run.state.wire.json(), "startedAt" to System.currentTimeMillis().json(),
            "detached" to request.options.detached.json(), "interaction" to request.interaction.json(), "preset" to request.preset.json(), "steps" to JsonArray(),
            "budget" to request.options.limits.let { jsonObject("maxSteps" to it.maxSteps.json(), "maxModelCalls" to it.maxModelCalls.json(),
                "maxDurationMs" to it.maxDurationMs.json(), "maxTotalTokens" to it.maxTotalTokens.json()) })
        markDirty(run.id)
    }
    @Synchronized fun event(event: RunEvent) {
        val row = records[event.runId] ?: return
        when (event.type) {
            "state" -> { row.addProperty("state", event.payload.string("to")); row.remove("pending") }
            "input", "confirmation" -> row.add("pending", event.payload.apply { addProperty("type", event.type) })
            "done", "error" -> row.remove("pending")
            "step" -> row.add("step", event.payload["index"] ?: 0.json())
            "progress" -> { row.addProperty("progress", AgentJson.truncate(event.payload.string("message").orEmpty(), 160))
                event.payload["budget"]?.let { row.add("remainingBudget", it.deepCopy()) } }
        }
        row.addProperty("sequence", event.sequence)
        markDirty(event.runId)
    }
    @Synchronized fun journal(id: String, snapshot: JsonObject) {
        val row = records[id] ?: return
        row.add("steps", snapshot["steps"] ?: JsonArray())
        row.addProperty("truncated", snapshot.flag("truncated") == true)
        snapshot["result"]?.let { row.add("result", it) }
        markDirty(id)
    }
    @Synchronized fun pending(id: String): JsonObject? = records[id]?.getAsJsonObject("pending")?.deepCopy()
    @Synchronized fun interaction(id: String): String? = records[id]?.string("interaction")
    @Synchronized fun summary(id: String): JsonObject? = records[id]?.let { row -> JsonObject().apply {
        for (key in listOf("runId", "goal", "state", "step", "progress")) row[key]?.let { add(key, it.deepCopy()) }
    } }
    @Synchronized fun claimPending(id: String, requestId: String): Boolean {
        val pending = records[id]?.getAsJsonObject("pending") ?: return false
        if (pending.string("requestId") != requestId || pending.flag("submitted") == true) return false
        pending.addProperty("submitted", true)
        return true
    }
    @Synchronized fun get(id: String, stepLimit: Int = 50): JsonObject? = records[id]?.let { project(it, stepLimit) }
    @Synchronized fun full(id: String): JsonObject? = records[id]?.deepCopy()
    /** All private history commands and file access run on the same worker as journal writes. */
    fun history(action: () -> JsonObject, complete: (Result<JsonObject>) -> Unit) {
        disk.execute { complete(runCatching { check(ready && !storageFailed); action() }) }
    }
    fun touch(id: String) {
        val removed = store.touch(id)
        synchronized(this) { removed.forEach { records.remove(it); dirty.remove(it) } }
    }
    fun remove(id: String?) {
        val ids = synchronized(this) {
            if (id != null) {
                val row = records[id] ?: error("Missing run")
                require(row.string("state") in TERMINAL) { "Active run" }
                setOf(id)
            } else records.filterValues { it.string("state") in TERMINAL }.keys.toSet()
        }
        store.delete(ids)
        synchronized(this) { ids.forEach { records.remove(it); dirty.remove(it) } }
    }
    @Synchronized fun list(limit: Int, offset: Int): JsonObject {
        val summaries = records.values.sortedByDescending { it.number("startedAt") ?: 0 }.drop(offset).take(limit).map { row ->
            jsonObject("runId" to row["runId"], "goal" to AgentJson.truncate(row.string("goal").orEmpty(), 256).json(),
                "state" to row["state"], "startedAt" to row["startedAt"], "detached" to row["detached"], "preset" to (row["preset"] ?: "default".json()))
        }
        return jsonObject("runs" to JsonArray().apply { summaries.forEach(::add) }, "total" to records.size.json(), "ready" to ready.json())
    }
    private fun markDirty(id: String) {
        dirty.add(id)
        if (scheduled) return
        scheduled = true
        disk.execute {
            while (true) {
                val next = synchronized(this) {
                    val nextId = dirty.firstOrNull() ?: run { scheduled = false; return@execute }
                    dirty.remove(nextId)
                    nextId to records[nextId]?.deepCopy()
                }
                if (!storageFailed) try {
                    val removed = next.second?.let(store::save).orEmpty()
                    synchronized(this) { removed.forEach { records.remove(it); dirty.remove(it) } }
                } catch (_: Exception) { storageFailed = true }
            }
        }
    }
    companion object {
        private const val MAX_RECORD_BYTES = RunHistoryCodec.MAX_BYTES
        private val TERMINAL = RunState.entries.filter { it.terminal }.map { it.wire }.toSet()
        /** Copy only the records that fit. Never repeatedly serialize the entire 1 MiB journal. */
        internal fun project(source: JsonObject, stepLimit: Int): JsonObject {
            require(stepLimit in 1..50)
            val result = JsonObject().apply { source.entrySet().filter { it.key != "steps" }.forEach { add(it.key, it.value.deepCopy()) } }
            var remaining = 31 * 1024 - StepJournal.bytes(result) - 32
            val all = source.getAsJsonArray("steps")
            val kept = ArrayDeque<JsonElement>()
            for (index in all.size() - 1 downTo maxOf(0, all.size() - stepLimit)) {
                val value = all[index]
                val bytes = StepJournal.bytes(value) + 1
                if (bytes > remaining) break
                kept.addFirst(value.deepCopy()); remaining -= bytes
            }
            result.add("steps", JsonArray().apply { kept.forEach(::add) })
            if (kept.size < all.size()) result.addProperty("truncated", true)
            check(StepJournal.bytes(result) <= 32 * 1024)
            return result
        }
    }
}
