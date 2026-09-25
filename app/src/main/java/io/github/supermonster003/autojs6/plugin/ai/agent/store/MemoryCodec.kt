package io.github.supermonster003.autojs6.plugin.ai.agent.store

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.text.Normalizer
import java.util.Locale

internal data class MemoryEntry(val key: String, val value: String, val scope: String, val sourceRunId: String,
                                val createdAt: Long, val updatedAt: Long) {
    val identity get() = scope to key
    override fun toString() = "MemoryEntry(valueBytes=${value.toByteArray(Charsets.UTF_8).size})"
}

/** Shared validation for private storage, import, tool proposals and prompt injection. */
internal object MemoryCodec {
    const val MAX_ENTRIES = 500
    const val MAX_BYTES = 256 * 1024
    const val MAX_ROW_BYTES = 32 * 1024
    private val fields = setOf("key", "value", "scope", "sourceRunId", "createdAt", "updatedAt")
    private val credentials = listOf("password", "passwd", "passphrase", "apikey", "accesstoken", "refreshtoken", "authtoken", "authorization",
        "credential", "privatekey", "secretkey", "clientsecret", "sessiontoken", "sessioncookie", "setcookie", "verificationcode", "pincode", "密码", "密碼", "口令", "密钥", "密鑰", "验证码", "驗證碼",
        "パスワード", "秘密鍵", "認証コード", "비밀번호", "인증코드", "пароль", "секретныйключ", "motdepasse", "contraseña", "contrasena", "كلمةالمرور", "رمزالتحقق")
    private val credentialNames = setOf("pwd", "pin", "otp", "token", "secret", "cookie", "sessionid")
    // Overlapping matches also inspect the credential name after an ordinary prose prefix.
    private val assignments = Regex("(?=(?<![\\p{L}\\p{N}_.-])[\"']?([\\p{L}\\p{N}_.-]+(?:[\\p{Zs}\\t]+[\\p{L}\\p{N}_.-]+){0,5})[\"']?\\s*[:=])")
    private val credentialValue = Regex("(?i)\\b(?:bearer\\s+|sk-(?:proj-)?|eyJ)[a-z0-9_./+=-]{12,}")
    private fun credentialName(value: String): Boolean {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
        return normalized in credentialNames || credentials.any(normalized::contains)
    }
    fun key(value: String) = text(value, 64).also { require(it == it.trim() && it.none(Character::isISOControl)) }
    fun scope(value: String) = if (value == "global") value else PresetCodec.name(value)
    private fun text(value: String, maximum: Int, blank: Boolean = false) = value.also {
        AgentJson.checkUnicode(it); require((blank || it.isNotBlank()) && it.codePointCount(0, it.length) <= maximum)
    }
    fun preference(key: String, value: String) {
        key(key); text(value, 4096, true)
        require(!credentialName(key))
        // Normalize only the inspection copy. Keep approved preference text byte-for-byte intact.
        val inspected = Normalizer.normalize(value, Normalizer.Form.NFKC).filterNot { Character.getType(it) == Character.FORMAT.toInt() }
        require(!inspected.lowercase(Locale.ROOT).contains("-----begin") && !credentialValue.containsMatchIn(inspected))
        // Recognizable credential assignments are rejected even under an innocent preference key.
        require(assignments.findAll(inspected).none { credentialName(it.groupValues[1]) })
    }
    fun decodeEntry(row: JsonObject): MemoryEntry {
        require(row.keySet() == fields)
        val key = requireNotNull(row.string("key")); val value = requireNotNull(row.string("value")); preference(key, value)
        val scope = scope(requireNotNull(row.string("scope")))
        val source = requireNotNull(row.string("sourceRunId")).also { require(it.matches(Regex("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}"))) }
        val created = requireNotNull(row.number("createdAt")); val updated = requireNotNull(row.number("updatedAt"))
        require(created >= 0 && updated >= created)
        return MemoryEntry(key, value, scope, source, created, updated)
    }
    fun entry(row: MemoryEntry) = jsonObject("key" to row.key.json(), "value" to row.value.json(), "scope" to row.scope.json(),
        "sourceRunId" to row.sourceRunId.json(), "createdAt" to row.createdAt.json(), "updatedAt" to row.updatedAt.json())
    fun decode(json: String): List<MemoryEntry> {
        val root = AgentJson.objectOf(json, MAX_BYTES)
        require(root.keySet() == setOf("version", "entries") && root.number("version") == 1L)
        val raw = requireNotNull(root["entries"]?.takeIf { it.isJsonArray }?.asJsonArray); require(raw.size() <= MAX_ENTRIES)
        return raw.map { decodeEntry(it.asJsonObject) }.also { require(it.map(MemoryEntry::identity).distinct().size == it.size) }
    }
    fun encode(rows: List<MemoryEntry>): String = jsonObject("version" to 1.json(), "entries" to JsonArray().apply { rows.forEach { add(entry(it)) } })
        .toString().also { decode(it) }
    fun encodeFile(row: MemoryEntry): String = jsonObject("version" to 1.json(), "entry" to entry(row)).toString().also {
        AgentJson.objectOf(it, MAX_ROW_BYTES); decodeEntry(entry(row))
    }
    fun decodeFile(json: String): MemoryEntry {
        val root = AgentJson.objectOf(json, MAX_ROW_BYTES)
        require(root.keySet() == setOf("version", "entry") && root.number("version") == 1L)
        return decodeEntry(requireNotNull(root.getAsJsonObject("entry")))
    }
}
