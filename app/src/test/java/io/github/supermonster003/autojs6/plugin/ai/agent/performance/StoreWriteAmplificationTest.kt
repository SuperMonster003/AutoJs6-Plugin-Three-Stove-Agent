package io.github.supermonster003.autojs6.plugin.ai.agent.performance

import com.google.gson.JsonArray
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest

/** Observe real atomic replacements, including identical-content rewrites, at full store capacity. */
class StoreWriteAmplificationTest {
    @get:Rule val temp = TemporaryFolder()
    private data class Stamp(val identity: String, val modified: Long, val digest: String, val bytes: Long)
    private fun snapshot(directory: File): Map<String, Stamp> = directory.listFiles()!!.associate { file ->
        val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
        file.name to Stamp(attributes.fileKey()?.toString().orEmpty(), attributes.lastModifiedTime().toMillis(),
            MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }, file.length())
    }
    private fun mark(directory: File) { directory.listFiles()!!.forEach { check(it.setLastModified(1_000_000_000_000L)) } }
    @Test fun historyUpdateReplacesOnlyItsRecordAndSmallRebuildableIndex() {
        val directory = temp.newFolder()
        val records = (1..200).map { index -> RunHistoryCodecTest.fixture(index).apply {
            addProperty("goal", "g".repeat(1500))
            add("steps", JsonArray().apply { repeat(10) { add(RunHistoryCodecTest.step(it + 1)) } })
        } }
        records.forEach { File(directory, "${it.string("runId")}.json").writeText(RunHistoryCodec.encode(it, 1)) }
        val store = RunHistoryStore(directory)
        assertEquals(200, store.open().size)
        val target = "${records.first().string("runId")}.json"
        repeat(3) { iteration ->
            mark(directory); val before = snapshot(directory)
            val updated = records.first().deepCopy().apply { addProperty("goal", "Updated fixture $iteration") }
            store.save(updated)
            val after = snapshot(directory)
            val changed = after.keys.filter { before[it] != after[it] }.toSet()
            assertEquals(setOf(target, "index.json"), changed)
            assertEquals(before.keys, after.keys)
            assertEquals(updated, RunHistoryCodec.decode(File(directory, target).readText()).run)
            val written = changed.sumOf { after.getValue(it).bytes }
            assertTrue(written < before.values.sumOf { it.bytes } / 10)
            println("P7_STORE " + jsonObject("kind" to "history".json(), "entries" to 200.json(), "changedFiles" to changed.size.json(),
                "logicalWriteBytes" to written.json(), "collectionBytes" to before.values.sumOf { it.bytes }.json()))
        }
    }
    @Test fun memoryUpdateReplacesOneEntryAtTheFiveHundredEntryLimit() {
        val directory = temp.newFolder()
        val rows = (1..500).map { MemoryEntry("preference$it", "v".repeat(128), "global", "00000000-0000-0000-0000-000000000001", 1, 1) }
        rows.forEach { File(directory, MemoryStore.fileName(it.scope, it.key)).writeText(MemoryCodec.encodeFile(it)) }
        val store = MemoryStore(directory)
        assertEquals(500, store.open().size)
        var previous = rows.first()
        repeat(3) { iteration ->
            mark(directory); val before = snapshot(directory)
            val updated = previous.copy(value = "Updated fixture $iteration", updatedAt = 2L + iteration)
            store.put(updated, previous)
            val after = snapshot(directory)
            val changed = after.keys.filter { before[it] != after[it] }.toSet()
            assertEquals(setOf(MemoryStore.fileName(updated.scope, updated.key)), changed)
            assertEquals(before.keys, after.keys)
            assertEquals(updated, MemoryStore(directory).open().first { it.identity == updated.identity })
            val written = changed.sumOf { after.getValue(it).bytes }
            assertTrue(written < before.values.sumOf { it.bytes } / 100)
            println("P7_STORE " + jsonObject("kind" to "memory".json(), "entries" to 500.json(), "changedFiles" to changed.size.json(),
                "logicalWriteBytes" to written.json(), "collectionBytes" to before.values.sumOf { it.bytes }.json()))
            previous = updated
        }
    }
}
