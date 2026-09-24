package io.github.supermonster003.autojs6.plugin.ai.agent.store

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PresetStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private val groups = setOf("observe", "act", "memory")
    private val roots = setOf("/sdcard/scripts")
    @Test fun createEditCopyDefaultDeleteAndReload() {
        val file = File(folder.root, "presets.json"); val store = PresetStore(file)
        assertEquals("default", store.open().defaultName)
        val first = Preset("office", toolGroups = setOf("observe"), context = "Before")
        store.save(first, true, groups, roots)
        val changed = first.copy(context = "After", confirmPolicy = "cautious")
        store.save(changed, false, groups, roots)
        store.save(changed.copy(name = "copy"), true, groups, roots)
        assertEquals("copy", store.setDefault("copy").defaultName)
        val reloaded = PresetStore(file).open()
        assertEquals(changed.copy(name = "copy"), reloaded.resolve())
        assertEquals("default", store.delete("copy").defaultName)
        assertEquals(changed, PresetStore(file).open().resolve("office"))
        assertEquals(1, store.delete("office").presets.size)
    }
    @Test fun invalidMutationsLeaveMemoryAndDiskUntouched() {
        val file = File(folder.root, "presets.json"); val store = PresetStore(file); store.open(); store.setDefault("default")
        val bytes = file.readBytes()
        val bad = listOf<() -> Unit>({ store.delete("default") }, { store.delete("missing") }, { store.setDefault("missing") },
            { store.save(Preset("default"), true, groups, roots) }, { store.save(Preset("missing"), false, groups, roots) },
            { store.save(Preset("bad", toolGroups = setOf("shell")), true, groups, roots) },
            { store.save(Preset("bad", scriptRoots = setOf("/sdcard/unapproved")), true, groups, roots) })
        bad.forEach { operation -> assertThrows(IllegalArgumentException::class.java, operation); assertArrayEquals(bytes, file.readBytes()) }
        assertEquals("default", store.setDefault("default").defaultName)
    }
    @Test fun failedWriteCannotPublishNewDefaultOrDestroyPreviousData() {
        val file = File(folder.root, "presets.json"); val store = PresetStore(file); store.open(); store.setDefault("default")
        val bytes = file.readBytes()
        File(file.path + ".new").apply { mkdir(); File(this, "block").writeText("fixture") }
        assertNotNull(runCatching { store.save(Preset("office"), true, groups, roots) }.exceptionOrNull())
        assertArrayEquals(bytes, file.readBytes()); assertEquals(1, PresetStore(file).open().presets.size)
    }
    @Test fun recoversInterruptedAtomicReplacementFromValidBackup() {
        val file = File(folder.root, "presets.json")
        File(file.path + ".bak").writeText(PresetCodec.encode(PresetSnapshot.INITIAL)); file.writeText("partial")
        assertEquals("default", PresetStore(file).open().defaultName)
        assertEquals("default", PresetCodec.decode(file.readText()).defaultName)
        assertFalse(File(file.path + ".bak").exists())
    }
    @Test fun corruptOrUnknownDataIsPreservedAndNeverReset() {
        for (content in listOf("{", """{"version":2,"defaultName":"default","presets":[]}""")) {
            val file = folder.newFile(); file.writeText(content)
            val store = PresetStore(file)
            assertNotNull(runCatching { store.open() }.exceptionOrNull())
            assertNotNull(runCatching { store.setDefault("default") }.exceptionOrNull())
            assertEquals(content, file.readText())
        }
        val file = folder.newFile(); file.writeBytes(byteArrayOf(0xc3.toByte(), 0x28))
        assertNotNull(runCatching { PresetStore(file).open() }.exceptionOrNull())
    }
    @Test fun fullStoreRejectsExtraPresetAndKeepsDefault() {
        val file = File(folder.root, "presets.json"); val store = PresetStore(file); store.open()
        for (i in 1 until PresetCodec.MAX_COUNT) store.save(Preset("p$i"), true, groups, roots)
        assertThrows(IllegalArgumentException::class.java) { store.save(Preset("overflow"), true, groups, roots) }
        assertEquals(PresetCodec.MAX_COUNT, PresetStore(file).open().presets.size)
    }
}
