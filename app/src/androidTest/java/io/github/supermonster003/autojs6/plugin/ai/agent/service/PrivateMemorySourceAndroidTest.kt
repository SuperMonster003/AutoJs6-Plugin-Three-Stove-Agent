package io.github.supermonster003.autojs6.plugin.ai.agent.service

import androidx.test.platform.app.InstrumentationRegistry
import android.util.AtomicFile
import com.google.gson.JsonArray
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class PrivateMemorySourceAndroidTest {
    private fun fixture(action: (File, PrivateMemorySource) -> Unit) {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "p32-memory-${UUID.randomUUID()}.json")
        try { action(file, PrivateMemorySource(file)) } finally { AtomicFile(file).delete() }
    }
    @Test fun privateSnapshotFiltersScopeAndDisablingMemoryDoesNotReadOrWriteIt() = fixture { file, source ->
        assertFalse(source.snapshot("default", true).unavailable); assertFalse(file.exists())
        fun entry(scope: String, value: String) = jsonObject("key" to "address".json(), "value" to value.json(), "scope" to scope.json(),
            "sourceRunId" to "00000000-0000-0000-0000-000000000001".json(), "createdAt" to 1.json(), "updatedAt" to 1.json())
        val bytes = jsonObject("version" to 1.json(), "entries" to jsonArray(entry("global", "Shared"), entry("default", "Office"), entry("other", "Private"))).toString().toByteArray()
        val atomic = AtomicFile(file); val stream = atomic.startWrite(); stream.write(bytes); atomic.finishWrite(stream)
        val enabled = source.snapshot("default", true)
        assertEquals(1, enabled.entries.size()); assertEquals("Office", enabled.entries[0].asJsonObject.string("value"))
        assertEquals(JsonArray(), source.snapshot("default", false).entries); assertArrayEquals(bytes, file.readBytes())
    }
    @Test fun malformedOversizedAndInvalidUtf8FilesAreUnavailableAndNeverPartiallyInjected() = fixture { file, source ->
        for (bytes in listOf("{".toByteArray(), ByteArray(MemoryContext.MAX_FILE_BYTES + 1) { 32 }, byteArrayOf(0xc3.toByte(), 0x28))) {
            file.writeBytes(bytes)
            assertTrue(source.snapshot("default", true).unavailable)
            assertEquals(JsonArray(), source.snapshot("default", true).entries)
            assertFalse(source.snapshot("default", false).unavailable)
            assertArrayEquals(bytes, file.readBytes())
        }
    }
}
