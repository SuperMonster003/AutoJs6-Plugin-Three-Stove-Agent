package io.github.supermonster003.autojs6.plugin.ai.agent.service

import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MemoryRepositoryAndroidTest {
    private fun fixture(action: (File) -> Unit) {
        val root = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.canonicalFile
        val dir = File(root, "p64-memory-${UUID.randomUUID()}").apply { check(mkdirs()) }
        try { action(dir) } finally { assertEquals(root, dir.canonicalFile.parentFile); dir.deleteRecursively() }
    }
    private fun snapshot(repo: MemoryRepository, enabled: Boolean = true): MemoryContext {
        val latch = CountDownLatch(1); var value: MemoryContext? = null
        repo.snapshot("office", enabled, "global_and_preset") { value = it; latch.countDown() }
        assertTrue(latch.await(10, TimeUnit.SECONDS)); return checkNotNull(value)
    }
    @Test fun firstSnapshotAfterRestartWaitsForMigrationAndFiltersScope() = fixture { dir ->
        val legacy = File(dir, "agent-memory.json")
        val row = MemoryEntry("address", "Global fixture", "global", UUID.randomUUID().toString(), 1, 1)
        legacy.writeText(MemoryCodec.encode(listOf(row, row.copy(scope = "office", value = "Office fixture"), row.copy(scope = "other", value = "Private fixture"))))
        repeat(2) {
            MemoryRepository(File(dir, "memories"), legacy).use { repo ->
                val result = snapshot(repo)
                assertFalse(result.unavailable); assertEquals(1, result.entries.size())
                assertEquals("Office fixture", result.entries.single().asJsonObject.string("value"))
                assertFalse(legacy.exists()); assertEquals(0, snapshot(repo, false).entries.size())
            }
        }
    }
    @Test fun malformedOversizedAndInvalidUtf8LegacyFilesArePreservedAndUnavailable() = fixture { dir ->
        val file = File(dir, "agent-memory.json")
        for (bytes in listOf("{".toByteArray(), ByteArray(MemoryCodec.MAX_BYTES + 1) { 32 }, byteArrayOf(0xc3.toByte(), 0x28))) {
            file.writeBytes(bytes)
            MemoryRepository(File(dir, "memories"), file).use { repo ->
                assertTrue(snapshot(repo).unavailable); assertEquals(0, snapshot(repo).entries.size())
                assertFalse(snapshot(repo, false).unavailable); assertArrayEquals(bytes, file.readBytes())
            }
        }
    }
}
