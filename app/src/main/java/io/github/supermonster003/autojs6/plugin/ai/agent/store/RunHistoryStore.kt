package io.github.supermonster003.autojs6.plugin.ai.agent.store

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Called only by RunArchive's serial disk worker. Index is rebuildable; per-run files are authoritative. */
internal class RunHistoryStore(
    private val directory: File,
    private val maxCount: Int = 200,
    private val maxBytes: Long = 32L * 1024 * 1024,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class Entry(val state: String, val started: Long, val accessed: Long, val bytes: Long)
    private val entries = linkedMapOf<String, Entry>()
    private var opened = false
    fun bytes(): Long = entries.values.sumOf { it.bytes } + File(directory, "index.json").length()
    init { require(maxCount in 1..200 && maxBytes in (INDEX_BYTES + 2048L)..(32L * 1024 * 1024)) }
    fun open(): List<JsonObject> {
        check(!opened)
        check(directory.isDirectory || directory.mkdirs())
        // Recover interrupted replacements before reading. Unknown formats never get overwritten.
        directory.listFiles()?.filter { it.name.endsWith(".bak") }?.forEach { backup ->
            val target = File(directory, backup.name.removeSuffix(".bak"))
            check(!target.exists() || target.delete()); check(backup.renameTo(target))
        }
        val index = File(directory, "index.json")
        if (index.exists()) {
            require(index.length() <= INDEX_BYTES)
            val value = runCatching { AgentJson.objectOf(read(index, INDEX_BYTES), INDEX_BYTES) }.getOrNull()
            if (value != null) require(value.number("version") == RunHistoryCodec.VERSION.toLong())
        }
        val loaded = linkedMapOf<String, JsonObject>()
        val metadata = linkedMapOf<String, Entry>()
        val files = directory.listFiles()?.filter { it.name.endsWith(".json") && it.name != "index.json" }.orEmpty()
        require(files.size <= maxCount + 9 && files.sumOf { it.length() } <= maxBytes + RunHistoryCodec.MAX_BYTES)
        files.forEach { file ->
            val id = RunHistoryCodec.id(file.name.removeSuffix(".json"))
            require(file.length() <= RunHistoryCodec.MAX_BYTES)
            val record = RunHistoryCodec.decode(read(file, RunHistoryCodec.MAX_BYTES))
            require(record.run.string("runId") == id)
            loaded[id] = record.run
            metadata[id] = Entry(record.run.string("state")!!, record.run.number("startedAt")!!, record.accessedAt, file.length())
        }
        entries.putAll(metadata); opened = true
        trim(); writeIndex()
        directory.listFiles()?.filter { it.name.endsWith(".new") }?.forEach { check(it.delete()) }
        return loaded.filterKeys { it in entries }.values.toList()
    }
    fun save(run: JsonObject): Set<String> {
        check(opened)
        val id = RunHistoryCodec.id(requireNotNull(run.string("runId")))
        val access = entries[id]?.accessed ?: (run.number("startedAt") ?: clock())
        val text = RunHistoryCodec.encode(run, access)
        write(File(directory, "$id.json"), text)
        entries[id] = Entry(run.string("state")!!, run.number("startedAt")!!, access, text.toByteArray(Charsets.UTF_8).size.toLong())
        return trim().also { writeIndex() }
    }
    fun touch(id: String): Set<String> {
        check(opened)
        val entry = entries[RunHistoryCodec.id(id)] ?: return emptySet()
        val access = maxOf(clock(), entry.accessed)
        val run = RunHistoryCodec.decode(read(File(directory, "$id.json"), RunHistoryCodec.MAX_BYTES)).run
        val text = RunHistoryCodec.encode(run, access)
        write(File(directory, "$id.json"), text)
        entries[id] = entry.copy(accessed = access, bytes = text.toByteArray(Charsets.UTF_8).size.toLong())
        return trim().also { writeIndex() }
    }
    fun delete(ids: Set<String>) {
        check(opened)
        require(ids.all { entries[it]?.state in RunHistoryCodec.terminal || it !in entries })
        ids.forEach { id ->
            RunHistoryCodec.id(id)
            val file = File(directory, "$id.json")
            check(!file.exists() || file.delete()); entries.remove(id)
        }
        writeIndex()
    }
    fun contains(id: String) = id in entries
    private fun trim(): Set<String> {
        val removed = linkedSetOf<String>()
        // Index reserve is included in the 32 MiB limit. Live tasks are never evicted.
        var bytes = entries.values.sumOf { it.bytes } + INDEX_BYTES
        val candidates = entries.entries.filter { it.value.state in RunHistoryCodec.terminal }
            .sortedWith(compareBy({ it.value.accessed }, { it.value.started }, { it.key }))
        for ((id, entry) in candidates) {
            if (entries.size <= maxCount && bytes <= maxBytes) break
            check(File(directory, "$id.json").delete()); entries.remove(id); removed += id; bytes -= entry.bytes
        }
        check(entries.size <= maxCount && bytes <= maxBytes) { "Active history exceeds capacity" }
        return removed
    }
    private fun writeIndex() = write(File(directory, "index.json"), jsonObject("version" to RunHistoryCodec.VERSION.json(),
        "runs" to JsonArray().apply { entries.forEach { (id, entry) -> add(jsonObject("runId" to id.json(),
            "accessedAt" to entry.accessed.json(), "bytes" to entry.bytes.json())) } }).toString())
    companion object {
        private const val INDEX_BYTES = 32 * 1024
        private fun read(file: File, maximum: Int): String {
            require(file.length() <= maximum)
            val bytes = file.readBytes().also { require(it.size <= maximum) }
            return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        }
        /** AtomicFile-style replacement, using APIs available on API 24 and the JVM. */
        private fun write(target: File, text: String) {
            val temp = File(target.path + ".new")
            val backup = File(target.path + ".bak")
            FileOutputStream(temp).use { it.write(text.toByteArray(Charsets.UTF_8)); it.fd.sync() }
            if (target.exists()) check(target.renameTo(backup))
            if (!temp.renameTo(target)) {
                if (backup.exists()) check(backup.renameTo(target))
                error("History write failed")
            }
            check(!backup.exists() || backup.delete())
        }
    }
}
