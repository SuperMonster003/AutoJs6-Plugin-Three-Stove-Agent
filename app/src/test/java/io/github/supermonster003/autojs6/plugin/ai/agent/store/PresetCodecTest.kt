package io.github.supermonster003.autojs6.plugin.ai.agent.store

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class PresetCodecTest {
    @Test fun versionedRoundTripKeepsEveryFieldAndDefaultSelection() {
        val preset = Preset("办公咖啡", "profile:online", setOf("observe", "act"), mapOf("maxSteps" to 10, "maxDurationMs" to 5000),
            "cautious", "Fixed context\n中", setOf("/sdcard/scripts"), "preset")
        val copy = PresetCodec.decode(PresetCodec.encode(PresetSnapshot(preset.name, listOf(Preset("default"), preset))))
        assertEquals(preset, copy.resolve()); assertEquals(Preset("default"), copy.resolve("default"))
    }
    @Test fun inheritanceAndExplicitEmptySetsStayDistinct() {
        for (preset in listOf(Preset("default"), Preset("office", toolGroups = emptySet(), scriptRoots = emptySet(), memoryScope = "none"))) {
            assertEquals(preset, PresetCodec.decodePreset(PresetCodec.encodePreset(preset)))
        }
    }
    @Test fun contextLimitCountsUtf8BytesBeforeJsonEscaping() {
        for (text in listOf("中".repeat(2730) + "ab", "\u0000".repeat(8192))) {
            val input = PresetCodec.encode(PresetSnapshot("default", listOf(Preset("default", context = text))))
            assertEquals(text, PresetCodec.decode(input).resolve().context)
        }
        assertThrows(IllegalArgumentException::class.java) { PresetCodec.encode(PresetSnapshot("default", listOf(Preset("default", context = "中".repeat(2731))))) }
    }
    @Test fun rejectsUnknownVersionsFieldsMissingDefaultsAndDuplicateNames() {
        val good = AgentJson.objectOf(PresetCodec.encode(PresetSnapshot.INITIAL))
        for (mutate in listOf<(com.google.gson.JsonObject) -> Unit>({ it.addProperty("version", 2) }, { it.addProperty("extra", 1) },
            { it.addProperty("defaultName", "missing") }, { it.getAsJsonArray("presets").add(PresetCodec.encodePreset(Preset("default"))) },
            { it.getAsJsonArray("presets").remove(0) })) {
            assertThrows(IllegalArgumentException::class.java) { PresetCodec.decode(good.deepCopy().also(mutate).toString()) }
        }
    }
    @Test fun rejectsInvalidNamesAndTargets() {
        for (name in listOf("", " ", " global ", "global", "line\nfeed", "中".repeat(43), "\ud800"))
            assertThrows(IllegalArgumentException::class.java) { PresetCodec.name(name) }
        val longest = "a".repeat(128) + ":" + "b".repeat(127)
        assertEquals(longest, PresetCodec.target(longest))
        for (id in listOf("missing-colon", "profile:", "UPPER:test", "profile:test:extra", longest + "b"))
            assertThrows(IllegalArgumentException::class.java) { PresetCodec.target(id) }
    }
    @Test fun rejectsAuthorityFieldsUnknownScopesAndUnboundedBudgets() {
        for (extra in listOf("\"riskOverrides\":{}", "\"confirmPolicy\":\"never\"", "\"memoryScope\":\"other-preset\"",
            "\"toolGroups\":[\"unknown\"]", "\"toolGroups\":[\"act\",\"act\"]", "\"budget\":{\"maxSteps\":41}",
            "\"budget\":{\"maxSteps\":1.5}", "\"budget\":{\"maxSteps\":0}", "\"budget\":{\"maxTotalTokens\":300001}",
            "\"budget\":{\"maxModelCalls\":\"2\"}", "\"scriptRoots\":[\"/sdcard/../private\"]", "\"context\":null")) {
            assertNotNull(extra, runCatching { PresetCodec.decodePreset(AgentJson.objectOf("{\"name\":\"test\",$extra}")) }.exceptionOrNull())
        }
    }
    @Test fun countAndFileSizeAreBounded() {
        val rows = listOf(Preset("default")) + (1..32).map { Preset("p$it") }
        assertThrows(IllegalArgumentException::class.java) { PresetCodec.encode(PresetSnapshot("default", rows)) }
        val large = (listOf(Preset("default")) + (1..31).map { Preset("p$it", context = "\u0000".repeat(8192)) })
        assertThrows(IllegalArgumentException::class.java) { PresetCodec.encode(PresetSnapshot("default", large)) }
    }
}
