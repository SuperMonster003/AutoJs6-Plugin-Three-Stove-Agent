package io.github.supermonster003.autojs6.plugin.ai.agent.model

import java.security.MessageDigest
import java.util.Locale

/** Lowercase hexadecimal SHA-256: the single digest helper for fingerprints, identities and file names. */
object Digests {
    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 255) }
    fun sha256Hex(text: String): String = sha256Hex(text.toByteArray(Charsets.UTF_8))
}
