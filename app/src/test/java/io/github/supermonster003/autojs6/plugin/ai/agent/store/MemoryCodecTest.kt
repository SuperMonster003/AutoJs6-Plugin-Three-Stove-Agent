package io.github.supermonster003.autojs6.plugin.ai.agent.store

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class MemoryCodecTest {
    private val row = MemoryEntry("drink", "热拿铁\n中杯", "office", "00000000-0000-0000-0000-000000000001", 1, 2)
    @Test fun backupAndEntryRoundTripsRetainProvenanceAndExactUnicode() {
        val value = row.copy(value = "\"\\\u0000😀".repeat(800))
        assertEquals(value, MemoryCodec.decodeFile(MemoryCodec.encodeFile(value)))
        assertEquals(listOf(row, value.copy(scope = "global")), MemoryCodec.decode(MemoryCodec.encode(listOf(row, value.copy(scope = "global")))))
        assertFalse(value.toString().contains(value.value))
    }
    @Test fun closedSchemaRejectsDuplicateFieldsVersionsIdentitiesAndInvalidMetadata() {
        val text = MemoryCodec.encode(listOf(row))
        for (invalid in listOf(text.replace("\"version\":1", "\"version\":2"), text.replace("\"version\":1", "\"version\":1,\"version\":1"),
            text.replace("\"entries\"", "\"unknown\":true,\"entries\""))) assertTrue(runCatching { MemoryCodec.decode(invalid) }.isFailure)
        for (invalid in listOf(row.copy(key = " x"), row.copy(key = "x\n"), row.copy(value = "\ud800"), row.copy(scope = "office\n"),
            row.copy(sourceRunId = "run"), row.copy(createdAt = -1), row.copy(updatedAt = 0)))
            assertTrue(invalid.toString(), runCatching { MemoryCodec.encode(listOf(invalid)) }.isFailure)
        assertTrue(runCatching { MemoryCodec.encode(listOf(row, row)) }.isFailure)
    }
    @Test fun recognizedCredentialsAreRejectedWithoutBlockingOrdinaryPreferenceKeys() {
        for (key in listOf("Password", "api_key", "ＡＰＩ　ＫＥＹ", "OTP", "office access-token", "登录密码", "パスワード", "비밀번호", "mot de passe"))
            assertTrue(key, runCatching { MemoryCodec.preference(key, "fixture") }.isFailure)
        for (value in listOf("Bearer synthetic_fixture_123456", "sk-synthetic_fixture_123456", "-----BEGIN PRIVATE KEY-----", "password = fixture", "api_key: fixture"))
            assertTrue(runCatching { MemoryCodec.preference("note", value) }.isFailure)
        for (key in listOf("hotpot", "drink", "front desk", "收货地址", "theme")) MemoryCodec.preference(key, "A preference, not a credential")
    }
    @Test fun entryCountUtf8ByteAndCodePointLimitsAreIndependent() {
        val rows = (1..500).map { row.copy(key = "key$it") }
        assertEquals(500, MemoryCodec.decode(MemoryCodec.encode(rows)).size)
        assertTrue(runCatching { MemoryCodec.encode(rows + row) }.isFailure)
        MemoryCodec.preference("😀".repeat(64), "😀".repeat(4096))
        assertTrue(runCatching { MemoryCodec.preference("😀".repeat(65), "x") }.isFailure)
        assertTrue(runCatching { MemoryCodec.preference("value", "😀".repeat(4097)) }.isFailure)
        assertTrue(runCatching { MemoryCodec.encode((1..30).map { row.copy(key = "key$it", value = "中".repeat(4096)) }) }.isFailure)
    }
    @Test fun disguisedCredentialAssignmentsAreRejectedInValuesAndImports() {
        for (value in listOf("ｐａｓｓｗｏｒｄ： synthetic", "api\u200b_key = synthetic", "Refresh Token: synthetic",
            "session_cookie=synthetic", "OTP: 123456", "验证码：123456", "mot de passe: synthetic",
            "{\"private_key\":\"synthetic\"}", "Ｂｅａｒｅｒ synthetic_fixture_123456", "(password=synthetic)",
            "https://example.invalid/?api_key=synthetic", "api.key: synthetic", "Keep this secret: synthetic")) {
            assertTrue("Recognized credentials must not become preferences", runCatching { MemoryCodec.preference("note", value) }.isFailure)
            val raw = MemoryCodec.entry(row.copy(key = "note", value = value))
            assertTrue("Imported entries use the same credential policy", runCatching { MemoryCodec.decodeEntry(raw) }.isFailure)
        }
        for (value in listOf("Drink: hot latte", "cookie flavour: chocolate", "Reminder: pin the shopping list", "主题：深色"))
            MemoryCodec.preference("note", value)
    }
    @Test fun invalidImportRowRejectsTheWholeFileBeforeReview() {
        val raw = AgentJson.objectOf(MemoryCodec.encode(listOf(row, row.copy(key = "other"))))
        raw.getAsJsonArray("entries")[1].asJsonObject.addProperty("key", "password")
        assertTrue(runCatching { MemoryCodec.decode(raw.toString()) }.isFailure)
    }
}
