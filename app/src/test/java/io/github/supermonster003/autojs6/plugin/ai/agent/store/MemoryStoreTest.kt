package io.github.supermonster003.autojs6.plugin.ai.agent.store

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MemoryStoreTest {
    @get:Rule val temp = TemporaryFolder()
    private val row = MemoryEntry("drink", "Latte", "global", "00000000-0000-0000-0000-000000000001", 1, 2)
    private fun file(dir: File, value: MemoryEntry = row) = File(dir, MemoryStore.fileName(value.scope, value.key))
    @Test fun editingOneEntryDoesNotRewriteOthersAndDeletionSurvivesReload() {
        val dir = temp.newFolder(); val store = MemoryStore(dir); assertTrue(store.open().isEmpty())
        store.put(row, null); val other = row.copy(scope = "office"); store.put(other, null)
        val untouched = file(dir, other); val content = untouched.readBytes(); assertTrue(untouched.setLastModified(1000))
        val changed = row.copy(value = "Tea", updatedAt = 3); store.put(changed, row)
        assertEquals(1000, untouched.lastModified()); assertArrayEquals(content, untouched.readBytes())
        assertEquals(setOf(changed, other), MemoryStore(dir).open().toSet())
        store.delete(changed.scope, changed.key, changed)
        assertEquals(listOf(other), MemoryStore(dir).open())
    }
    @Test fun staleConfirmationsCannotOverwriteOrDeleteAConcurrentEdit() {
        val dir = temp.newFolder(); val store = MemoryStore(dir); store.open(); store.put(row, null)
        val changed = row.copy(value = "Tea", updatedAt = 3); store.put(changed, row)
        assertTrue(runCatching { store.put(row.copy(value = "Coffee"), row) }.isFailure)
        assertTrue(runCatching { store.delete(row.scope, row.key, row) }.isFailure)
        assertTrue(runCatching { store.put(row, null) }.isFailure)
        assertEquals(listOf(changed), store.snapshot()); assertEquals(listOf(changed), MemoryStore(dir).open())
    }
    @Test fun failedAtomicWritePreservesPreviousBytesAndPublishedSnapshot() {
        val dir = temp.newFolder(); val store = MemoryStore(dir); store.open(); store.put(row, null)
        val bytes = file(dir).readBytes(); val blocked = File(file(dir).path + ".new"); assertTrue(blocked.mkdir())
        File(blocked, "keep").writeText("fixture")
        assertTrue(runCatching { store.put(row.copy(value = "Changed"), row) }.isFailure)
        assertArrayEquals(bytes, file(dir).readBytes()); assertEquals(listOf(row), store.snapshot())
    }
    @Test fun validBackupRecoversThePreviousConfirmedEntry() {
        val dir = temp.newFolder(); val original = file(dir)
        File(original.path + ".bak").writeText(MemoryCodec.encodeFile(row)); original.writeText("unfinished")
        assertEquals(listOf(row), MemoryStore(dir).open())
        assertEquals(row, MemoryCodec.decodeFile(original.readText())); assertFalse(File(original.path + ".bak").exists())
    }
    @Test fun invalidOrMisnamedEntryFailsClosedWithoutResettingStorage() {
        for (content in listOf("{", MemoryCodec.encodeFile(row).replace("\"version\":1", "\"version\":2"), MemoryCodec.encodeFile(row.copy(key = "other")))) {
            val dir = temp.newFolder(); val target = file(dir); target.writeText(content)
            assertTrue(runCatching { MemoryStore(dir).open() }.isFailure); assertEquals(content, target.readText())
        }
    }
    @Test fun migrationMergesMissingRowsAndKeepsAlreadyMigratedEdits() {
        val dir = temp.newFolder(); val legacy = temp.newFile(); val other = row.copy(scope = "office")
        legacy.writeText(MemoryCodec.encode(listOf(row, other)))
        val changed = row.copy(value = "Current", updatedAt = 3)
        file(dir).writeText(MemoryCodec.encodeFile(changed))
        assertEquals(setOf(changed, other), MemoryStore(dir, legacy).open().toSet()); assertFalse(legacy.exists())
        assertEquals(setOf(changed, other), MemoryStore(dir, legacy).open().toSet())
    }
    @Test fun corruptLegacyAndInvalidUtf8DoNotPublishPartialMemory() {
        for (bytes in listOf("{".toByteArray(), byteArrayOf(0xc3.toByte(), 0x28), ByteArray(MemoryCodec.MAX_BYTES + 1) { 32 })) {
            val dir = temp.newFolder(); val legacy = temp.newFile(); legacy.writeBytes(bytes)
            val store = MemoryStore(dir, legacy)
            assertTrue(runCatching { store.open() }.isFailure); assertTrue(runCatching { store.snapshot() }.isFailure)
            assertArrayEquals(bytes, legacy.readBytes()); assertTrue(dir.listFiles()!!.isEmpty())
        }
    }
    @Test fun storeLimitsRejectNewRowsWithoutChangingTheCollection() {
        val dir = temp.newFolder(); val legacy = temp.newFile()
        legacy.writeText(MemoryCodec.encode((1..500).map { row.copy(key = "key$it") }))
        val store = MemoryStore(dir, legacy); store.open()
        assertTrue(runCatching { store.put(row, null) }.isFailure); assertEquals(500, store.snapshot().size)
        val larger = MemoryStore(temp.newFolder()); larger.open()
        var inserted = 0
        for (index in 1..30) {
            if (runCatching { larger.put(row.copy(key = "large$index", value = "中".repeat(4096)), null) }.isFailure) break
            inserted++
        }
        assertTrue(inserted in 1..29); assertEquals(inserted, larger.snapshot().size)
        assertTrue(larger.snapshot().sumOf { MemoryCodec.encodeFile(it).toByteArray().size } <= MemoryCodec.MAX_BYTES)
    }
}
