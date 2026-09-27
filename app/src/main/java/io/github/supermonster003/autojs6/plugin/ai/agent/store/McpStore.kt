package io.github.supermonster003.autojs6.plugin.ai.agent.store

import com.google.gson.JsonArray
import io.github.supermonster003.autojs6.plugin.ai.agent.mcp.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal interface McpEncryption {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray): ByteArray
}

/** A single worker owns this encrypted atomic file; plaintext never reaches a temporary file. */
internal class McpStore(private val file: File, private val encryption: McpEncryption) {
    fun open(): List<McpServerProfile> {
        val backup = File(file.path + ".bak")
        val source = if (backup.exists()) backup else file
        if (!source.exists()) return emptyList()
        require(source.length() in 1..MAX_CIPHER_BYTES.toLong())
        val encrypted = source.inputStream().use { input ->
            val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(4096)
            while (true) { val count = input.read(buffer); if (count < 0) break
                require(output.size() + count <= MAX_CIPHER_BYTES); output.write(buffer, 0, count) }
            output.toByteArray()
        }
        require(encrypted.size <= MAX_CIPHER_BYTES)
        val plaintext = encryption.decrypt(encrypted)
        val profiles = try {
            require(plaintext.size <= MAX_BYTES)
            val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(plaintext)).toString()
            decode(text)
        } finally { plaintext.fill(0) }
        // Do not recover or replace any file until authentication and the whole document validate.
        if (backup.exists()) { check(!file.exists() || file.delete()); check(backup.renameTo(file)) }
        return profiles
    }
    fun save(profiles: List<McpServerProfile>) {
        val plaintext = encode(profiles).toByteArray(Charsets.UTF_8)
        val encrypted = try { encryption.encrypt(plaintext) } finally { plaintext.fill(0) }
        require(encrypted.size in 1..MAX_CIPHER_BYTES)
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        val temporary = File(file.path + ".new"); val backup = File(file.path + ".bak")
        try {
            FileOutputStream(temporary).use { it.write(encrypted); it.fd.sync() }
            check(!backup.exists())
            if (file.exists()) check(file.renameTo(backup))
            if (!temporary.renameTo(file)) { if (backup.exists()) check(backup.renameTo(file)); error("MCP store unavailable") }
            if (backup.exists() && !backup.delete()) { check(file.delete()); check(backup.renameTo(file)); error("MCP store unavailable") }
        } finally { temporary.delete() }
    }
    companion object {
        const val MAX_PROFILES = 8
        const val MAX_SELECTED_TOOLS = 32
        const val MAX_BYTES = 128 * 1024
        const val MAX_CIPHER_BYTES = MAX_BYTES + 1024
        fun validate(profiles: List<McpServerProfile>) {
            require(profiles.size <= MAX_PROFILES && profiles.map { it.id }.distinct().size == profiles.size)
            require(profiles.sumOf { it.selectedTools.size } <= MAX_SELECTED_TOOLS)
            profiles.forEach { McpProfileCodec.decode(McpProfileCodec.encode(it, includeSecret = true)) }
        }
        fun encode(profiles: List<McpServerProfile>): String {
            validate(profiles)
            return jsonObject("version" to 1.json(), "profiles" to JsonArray().apply {
                profiles.forEach { add(McpProfileCodec.encode(it, includeSecret = true)) }
            }).toString().also { require(it.utf8Size() <= MAX_BYTES) }
        }
        fun decode(text: String): List<McpServerProfile> {
            val value = AgentJson.objectOf(text, MAX_BYTES)
            require(value.keySet() == setOf("version", "profiles") && value.number("version") == 1L)
            val rows = requireNotNull(value["profiles"]?.takeIf { it.isJsonArray }?.asJsonArray)
            return rows.map { McpProfileCodec.decode(it.asJsonObject) }.also(::validate)
        }
    }
}
