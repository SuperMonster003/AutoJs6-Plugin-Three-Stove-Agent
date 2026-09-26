package io.github.supermonster003.autojs6.plugin.ai.agent.store

import io.github.supermonster003.autojs6.plugin.ai.agent.mcp.McpServerProfile
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class McpStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private val crypto = object : McpEncryption {
        private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        override fun encrypt(plaintext: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, key); iv + doFinal(plaintext)
        }
        override fun decrypt(ciphertext: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, ciphertext.copyOfRange(0, 12)))
            doFinal(ciphertext, 12, ciphertext.size - 12)
        }
    }
    private fun profile(id: String = "local", selected: List<String> = listOf("device_info")) =
        McpServerProfile(id, "Local fixture", "http://127.0.0.1:9637/mcp", selectedTools = selected, bearerToken = "private-test-credential")
    @Test fun wholeDocumentAndCredentialAreEncryptedAndRoundTrip() {
        val file = File(temporary.root, "profiles.enc"); val store = McpStore(file, crypto)
        assertTrue(store.open().isEmpty()); store.save(listOf(profile()))
        val raw = file.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(raw.contains("private-test-credential")); assertFalse(raw.contains("127.0.0.1")); assertFalse(raw.contains("Local fixture"))
        assertEquals(listOf(profile()), McpStore(file, crypto).open())
        assertFalse(File(file.path + ".new").exists()); assertFalse(File(file.path + ".bak").exists())
    }
    @Test fun tamperingAndWrongKeyDoNotRestoreAnEmptyOrPermissiveConfiguration() {
        val file = File(temporary.root, "profiles.enc"); McpStore(file, crypto).save(listOf(profile()))
        val original = file.readBytes(); val changed = original.copyOf().apply { this[lastIndex] = (last().toInt() xor 1).toByte() }
        file.writeBytes(changed)
        assertThrows(Exception::class.java) { McpStore(file, crypto).open() }
        assertArrayEquals(changed, file.readBytes())
        file.writeBytes(original)
        val wrong = object : McpEncryption {
            override fun encrypt(plaintext: ByteArray) = error("Unused")
            override fun decrypt(ciphertext: ByteArray): ByteArray = error("Keystore unavailable")
        }
        assertThrows(Exception::class.java) { McpStore(file, wrong).open() }
        assertArrayEquals(original, file.readBytes())
    }
    @Test fun validAtomicBackupRecoversButCorruptBackupNeverGetsOverwritten() {
        val file = File(temporary.root, "profiles.enc"); val store = McpStore(file, crypto)
        store.save(listOf(profile())); val backup = File(file.path + ".bak"); file.copyTo(backup)
        file.writeText("partial replacement")
        assertEquals(listOf(profile()), store.open()); assertFalse(backup.exists())
        backup.writeText("corrupt backup"); val before = file.readBytes()
        assertThrows(Exception::class.java) { store.open() }
        assertEquals("corrupt backup", backup.readText()); assertArrayEquals(before, file.readBytes())
    }
    @Test fun selectionCapacityAndDuplicateIdentityRejectBeforeReplacingCiphertext() {
        val file = File(temporary.root, "profiles.enc"); val store = McpStore(file, crypto)
        store.save(listOf(profile())); val before = file.readBytes()
        assertThrows(Exception::class.java) { store.save(listOf(profile(), profile())) }
        assertThrows(Exception::class.java) { store.save(listOf(profile("first", (1..32).map { "t$it" }), profile("second"))) }
        assertArrayEquals(before, file.readBytes())
    }
}
