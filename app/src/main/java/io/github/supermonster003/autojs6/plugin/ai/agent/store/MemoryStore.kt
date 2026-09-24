package io.github.supermonster003.autojs6.plugin.ai.agent.store

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

/** Single-worker owner. Each atomic mutation writes one entry, not the entire memory collection. */
internal class MemoryStore(private val directory: File, private val legacy: File? = null) {
    private var rows: List<MemoryEntry>? = null
    fun snapshot(): List<MemoryEntry> = checkNotNull(rows)
    fun open(): List<MemoryEntry> {
        check(rows == null)
        check(!directory.exists() || directory.isDirectory)
        val paths = if (directory.exists()) checkNotNull(directory.listFiles()).filter { it.name.endsWith(".json") || it.name.endsWith(".json.bak") } else emptyList()
        require(paths.size <= MemoryCodec.MAX_ENTRIES * 2)
        val names = paths.map { it.name.removeSuffix(".bak") }.distinct()
        val loaded = names.map { name ->
            require(name.matches(Regex("[a-f0-9]{64}\\.json")))
            val file = File(directory, name); val backup = File(file.path + ".bak")
            MemoryCodec.decodeFile(read(if (backup.exists()) backup else file, MemoryCodec.MAX_ROW_BYTES)).also { require(fileName(it.scope, it.key) == name) }
        }
        validate(loaded)
        // Only restore backups after all current entries have passed validation.
        names.forEach { name -> val file = File(directory, name); val backup = File(file.path + ".bak")
            if (backup.exists()) { check(!file.exists() || file.delete()); check(backup.renameTo(file)) }
        }
        val old = legacy?.let { file -> File(file.path + ".bak").takeIf { it.exists() } ?: file.takeIf { it.exists() } }
        val merged = if (old == null) loaded else loaded + MemoryCodec.decode(read(old, MemoryCodec.MAX_BYTES)).filter { row -> loaded.none { it.identity == row.identity } }
        validate(merged)
        if (old != null) {
            merged.filter { row -> loaded.none { it.identity == row.identity } }.forEach { atomic(File(directory, fileName(it.scope, it.key)), MemoryCodec.encodeFile(it)) }
            // A surviving legacy file is retried on next open; existing validated entries win.
            check(!legacy.exists() || legacy.delete()); val backup = File(legacy.path + ".bak"); check(!backup.exists() || backup.delete())
        }
        rows = merged.toList(); return snapshot()
    }
    fun put(row: MemoryEntry, expected: MemoryEntry?): List<MemoryEntry> {
        val before = snapshot(); check(before.find { it.identity == row.identity } == expected) { "Memory changed" }
        val next = before.filter { it.identity != row.identity } + row; validate(next)
        atomic(File(directory, fileName(row.scope, row.key)), MemoryCodec.encodeFile(row))
        rows = next; return snapshot()
    }
    fun delete(scope: String, key: String, expected: MemoryEntry): List<MemoryEntry> {
        MemoryCodec.scope(scope); MemoryCodec.key(key)
        val before = snapshot(); require(expected.identity == scope to key); check(before.find { it.identity == scope to key } == expected)
        check(File(directory, fileName(scope, key)).delete())
        rows = before.filter { it.identity != scope to key }; return snapshot()
    }
    private fun validate(values: List<MemoryEntry>) {
        MemoryCodec.encode(values)
        require(values.sumOf { MemoryCodec.encodeFile(it).toByteArray(Charsets.UTF_8).size } <= MemoryCodec.MAX_BYTES)
    }
    private fun atomic(file: File, text: String) {
        check(directory.isDirectory || directory.mkdirs())
        val temp = File(file.path + ".new"); val backup = File(file.path + ".bak")
        try {
            FileOutputStream(temp).use { it.write(text.toByteArray(Charsets.UTF_8)); it.fd.sync() }
            check(!backup.exists())
            if (file.exists()) check(file.renameTo(backup))
            if (!temp.renameTo(file)) {
                if (backup.exists()) check(backup.renameTo(file)); error("Memory write failed")
            }
            if (backup.exists() && !backup.delete()) { check(file.delete()); check(backup.renameTo(file)); error("Memory write failed") }
        } finally { temp.delete() }
    }
    companion object {
        fun fileName(scope: String, key: String): String {
            val data = com.google.gson.JsonArray().apply { add(scope); add(key) }.toString().toByteArray(Charsets.UTF_8)
            return MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) } + ".json"
        }
        fun read(file: File, limit: Int): String = file.inputStream().use { input ->
            val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(4096)
            while (true) { val count = input.read(buffer); if (count < 0) break; require(output.size() + count <= limit); output.write(buffer, 0, count) }
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(output.toByteArray())).toString()
        }
    }
}
