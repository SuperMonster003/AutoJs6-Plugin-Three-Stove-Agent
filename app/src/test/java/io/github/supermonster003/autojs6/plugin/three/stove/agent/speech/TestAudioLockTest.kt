package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.Properties

/** Roadmap D79: the recordings of the acoustic device test are exactly the pinned upstream files, and nothing else is staged. */
class TestAudioLockTest {
    @Test
    fun recordingsMatchTheLock() {
        val root = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.isDirectory(it.resolve("app/src/main")) }
        val lock = Properties().apply { Files.newBufferedReader(root.resolve("locks/test-audio.lock")).use(::load) }
        assertEquals("1", lock.getProperty("format"))
        val names = lock.stringPropertyNames().filter { it.endsWith(".path") }.map { it.removeSuffix(".path") }.sorted()
        assertEquals(listOf("kws-zh-6", "mixed-zh-en"), names)
        val staged = Files.list(root.resolve("app/src/androidTest/assets/test-audio")).use { files -> files.map { it.fileName.toString() }.toList() }.sorted()
        assertEquals(names.map { "$it.wav" }, staged)
        for (name in names) {
            val file: Path = root.resolve(lock.getProperty("$name.path"))
            val digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)).joinToString("") { "%02x".format(it) }
            assertEquals(name, lock.getProperty("$name.sha256"), digest)
            assertEquals("Apache-2.0", lock.getProperty("$name.license"))
        }
    }
}
