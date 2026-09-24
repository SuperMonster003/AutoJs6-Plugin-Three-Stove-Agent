package io.github.supermonster003.autojs6.plugin.ai.agent.store

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.RunHistoryCodecTest.Companion.fixture
import io.github.supermonster003.autojs6.plugin.ai.agent.store.RunHistoryCodecTest.Companion.rejects
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RunHistoryStoreTest {
    @get:Rule val temp = TemporaryFolder()
    private fun id(index: Int) = fixture(index).string("runId")!!
    @Test fun defaultCountCapIsTwoHundredAndReadsDoNotChangeListOrdering() {
        val dir = temp.newFolder()
        val store = RunHistoryStore(dir); store.open()
        for (index in 1..201) store.save(fixture(index))
        val rows = RunHistoryStore(dir).open()
        assertEquals(200, rows.size); assertFalse(rows.any { it.string("runId") == id(1) })
    }
    @Test fun lruTouchSurvivesRestartAndWritesDoNotTouchAccessTime() {
        val dir = temp.newFolder()
        val store = RunHistoryStore(dir, maxCount = 2, clock = { 100 })
        store.open(); store.save(fixture(1)); store.save(fixture(2)); store.touch(id(1))
        val restarted = RunHistoryStore(dir, maxCount = 2); restarted.open()
        restarted.save(fixture(2)); assertEquals(setOf(id(2)), restarted.save(fixture(3)))
        assertTrue(restarted.contains(id(1))); assertTrue(restarted.contains(id(3)))
    }
    @Test fun byteCapIncludesIndexAndEvictsLeastRecentlyUsedRecord() {
        val dir = temp.newFolder()
        val store = RunHistoryStore(dir, maxBytes = 40 * 1024L); store.open()
        for (index in 1..10) store.save(fixture(index).apply { addProperty("goal", "g".repeat(3500)) })
        assertTrue(dir.listFiles()!!.sumOf { it.length() } <= 40 * 1024L)
        assertFalse(store.contains(id(1))); assertTrue(store.contains(id(10)))
    }
    @Test fun activeTasksAreProtectedFromEvictionAndDelete() {
        val dir = temp.newFolder()
        val store = RunHistoryStore(dir, maxCount = 2); store.open()
        store.save(fixture(1, "running")); store.save(fixture(2)); store.save(fixture(3))
        assertTrue(store.contains(id(1))); assertFalse(store.contains(id(2)))
        rejects { store.delete(setOf(id(1))) }
        assertTrue(File(dir, "${id(1)}.json").exists())
    }
    @Test fun deletedRecordsStayDeletedAfterRestartAndIndexRebuild() {
        val dir = temp.newFolder()
        val store = RunHistoryStore(dir); store.open(); store.save(fixture(1)); store.save(fixture(2)); store.delete(setOf(id(1)))
        File(dir, "index.json").writeText("broken index")
        assertEquals(listOf(id(2)), RunHistoryStore(dir).open().map { it.string("runId") })
    }
    @Test fun futureIndexOrRecordVersionIsNotOverwritten() {
        val dir = temp.newFolder()
        File(dir, "index.json").writeText("{\"version\":999}")
        rejects { RunHistoryStore(dir).open() }
        assertEquals("{\"version\":999}", File(dir, "index.json").readText())
        File(dir, "index.json").delete()
        val text = RunHistoryCodec.encode(fixture(1), 1).replace("\"version\":1", "\"version\":999")
        File(dir, "${id(1)}.json").writeText(text)
        rejects { RunHistoryStore(dir).open() }
        assertEquals(text, File(dir, "${id(1)}.json").readText())
    }
    @Test fun interruptedReplacementRecoversBackupAndIgnoresUncommittedTemp() {
        val dir = temp.newFolder()
        val file = File(dir, "${id(1)}.json")
        File(file.path + ".bak").writeText(RunHistoryCodec.encode(fixture(1), 1))
        file.writeText("incomplete"); File(file.path + ".new").writeText("unfinished")
        assertEquals(id(1), RunHistoryStore(dir).open().single().string("runId"))
        assertFalse(File(file.path + ".bak").exists()); assertFalse(File(file.path + ".new").exists())
    }
    @Test fun filenameIdentityMismatchFailsClosed() {
        val dir = temp.newFolder()
        File(dir, "${id(1)}.json").writeText(RunHistoryCodec.encode(fixture(2), 1))
        rejects { RunHistoryStore(dir).open() }
    }
}
