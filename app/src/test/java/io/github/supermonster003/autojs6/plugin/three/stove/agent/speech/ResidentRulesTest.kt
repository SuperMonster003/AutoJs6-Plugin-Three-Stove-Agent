package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.json
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.jsonObject
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap D65, D71, D73-D77: the pure rules of the resident listener. */
class ResidentRulesTest {
    private val ready = MacawProbe(installed = true, bound = true, contractVersion = 1, features = setOf("models", "asr", "asrStreaming", "vad", "kws"),
        microphoneGranted = true, wakeModels = 1, recognitionModels = 2)

    @Test
    fun defaultKeywordsAndSensitivityFollowTheMaintainerChoice() {
        assertEquals(listOf("小炉小炉", "Hey Stove"), ResidentKeywords.DEFAULT)
        assertEquals(ResidentSettings(ResidentKeywords.DEFAULT, WakeSensitivity.MEDIUM), ResidentSettings())
        assertNull(WakeSensitivity.MEDIUM.threshold)
        assertTrue(WakeSensitivity.HIGH.threshold!! < 0.25f && WakeSensitivity.LOW.threshold!! > 0.25f)
        assertEquals(WakeSensitivity.LOW, WakeSensitivity.of("low"))
        assertEquals(WakeSensitivity.MEDIUM, WakeSensitivity.of("loud"))
        assertEquals(WakeSensitivity.MEDIUM, WakeSensitivity.of(null))
    }

    @Test
    fun keywordsAreOnePerLineOneToFiveDistinctAndShort() {
        assertEquals(listOf("小炉小炉", "Hey Stove"), ResidentKeywords.parse(" 小炉小炉 \n\n  Hey Stove\n"))
        assertNull(ResidentKeywords.parse(""))
        assertNull(ResidentKeywords.parse(" \n "))
        assertNull(ResidentKeywords.parse((1..6).joinToString("\n") { "word$it" }))
        assertEquals(5, ResidentKeywords.parse((1..5).joinToString("\n") { "word$it" })!!.size)
        assertNull(ResidentKeywords.parse("Hey Stove\nhey stove"))
        assertNull(ResidentKeywords.parse("a".repeat(33)))
        assertEquals(listOf("a".repeat(32)), ResidentKeywords.parse("a".repeat(32)))
        assertFalse(ResidentKeywords.valid(listOf("hey" + 7.toChar() + "stove")))
        assertFalse(ResidentKeywords.valid(listOf(" padded")))
        assertFalse(ResidentKeywords.valid(listOf("x" + 0xD800.toChar())))
    }

    @Test
    fun settingsFileRoundTripsAndFallsBackToDefaults() {
        val value = ResidentSettings(listOf("Hey Stove", "小炉小炉"), WakeSensitivity.HIGH)
        val encoded = ResidentSettings.encode(value)
        assertEquals("""{"version":1,"keywords":["Hey Stove","小炉小炉"],"sensitivity":"high"}""", encoded.toString())
        assertEquals(value, ResidentSettings.decode(JsonParser.parseString(encoded.toString()).asJsonObject))
        assertEquals(ResidentSettings(), ResidentSettings.decode(null))
        assertEquals(ResidentSettings(), ResidentSettings.decode(jsonObject("version" to 2.json())))
        assertEquals(ResidentSettings(), ResidentSettings.decode(JsonParser.parseString("""{"version":1,"keywords":[],"sensitivity":"low"}""").asJsonObject))
        assertEquals(WakeSensitivity.MEDIUM, ResidentSettings.decode(JsonParser.parseString("""{"version":1,"keywords":["Hey Stove"],"sensitivity":"?"}""").asJsonObject).sensitivity)
    }

    @Test
    fun availabilityNamesTheFirstMissingPiece() {
        assertEquals(ResidentAvailability.READY, ResidentAvailability.of(ready))
        assertEquals(ResidentAvailability.MACAW_MISSING, ResidentAvailability.of(MacawProbe(installed = false)))
        assertEquals(ResidentAvailability.MACAW_UNAVAILABLE, ResidentAvailability.of(MacawProbe(installed = true)))
        assertEquals(ResidentAvailability.MACAW_INCOMPATIBLE, ResidentAvailability.of(ready.copy(contractVersion = null)))
        assertEquals(ResidentAvailability.MACAW_INCOMPATIBLE, ResidentAvailability.of(ready.copy(contractVersion = SpeechContract.MAX_CONTRACT_VERSION + 1)))
        assertEquals(ResidentAvailability.MACAW_INCOMPATIBLE, ResidentAvailability.of(ready.copy(features = setOf("asr"))))
        assertEquals(ResidentAvailability.MACAW_INCOMPATIBLE, ResidentAvailability.of(ready.copy(features = setOf("kws"))))
        assertEquals(ResidentAvailability.NO_WAKE_MODEL, ResidentAvailability.of(ready.copy(wakeModels = 0, recognitionModels = 0, microphoneGranted = false)))
        assertEquals(ResidentAvailability.NO_RECOGNITION_MODEL, ResidentAvailability.of(ready.copy(recognitionModels = 0, microphoneGranted = false)))
        assertEquals(ResidentAvailability.MICROPHONE_DENIED, ResidentAvailability.of(ready.copy(microphoneGranted = false)))
    }

    @Test
    fun stopReasonsComeFromTheSpeechErrorCodes() {
        assertEquals(ResidentStop.MICROPHONE, ResidentStop.of(SpeechErrorCodes.PERMISSION_DENIED))
        assertEquals(ResidentStop.MODEL, ResidentStop.of(SpeechErrorCodes.MODEL_MISSING))
        assertEquals(ResidentStop.KEYWORDS, ResidentStop.of(SpeechErrorCodes.INVALID_OPTIONS))
        assertEquals(ResidentStop.BUSY, ResidentStop.of(SpeechErrorCodes.SESSION_BUSY))
        assertEquals(ResidentStop.INCOMPATIBLE, ResidentStop.of(SpeechErrorCodes.PLUGIN_INCOMPATIBLE))
        assertEquals(ResidentStop.UNAVAILABLE, ResidentStop.of(SpeechErrorCodes.PLUGIN_UNAVAILABLE))
        assertEquals(ResidentStop.ENDED, ResidentStop.of(SpeechErrorCodes.ENGINE_FAILED))
        assertEquals(ResidentStop.ENDED, ResidentStop.of(null))
        assertEquals(SpeechErrorCodes.SESSION_BUSY, ResidentStop.code(IllegalStateException("SESSION_BUSY: the microphone is held")))
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, ResidentStop.code(IllegalArgumentException("INVALID_OPTIONS: the English word 'STOVE' is not in the model's lexicon")))
        assertEquals(SpeechErrorCodes.PLUGIN_UNAVAILABLE, ResidentStop.code(SecurityException("Not allowed to bind")))
        assertNull(ResidentStop.code(IllegalStateException("boom")))
    }

    @Test
    fun draftsGoToTheCardThenTheWorkbenchThenANotification() {
        for (unlocked in listOf(true, false)) for (background in listOf(true, false))
            assertEquals(VoiceDraftRoute.CARD, VoiceDraftRoute.of(true, unlocked, background))
        assertEquals(VoiceDraftRoute.WORKBENCH, VoiceDraftRoute.of(false, unlocked = true, backgroundStarts = true))
        assertEquals(VoiceDraftRoute.NOTIFICATION, VoiceDraftRoute.of(false, unlocked = true, backgroundStarts = false))
        assertEquals(VoiceDraftRoute.NOTIFICATION, VoiceDraftRoute.of(false, unlocked = false, backgroundStarts = true))
    }

    @Test
    fun theNotificationShowsFortyCodePoints() {
        assertEquals("打开设置 then Wi-Fi", VoiceDraftRoute.preview("打开设置\nthen Wi-Fi"))
        val forty = "字".repeat(40)
        assertEquals(forty, VoiceDraftRoute.preview(forty))
        assertEquals(forty + "...", VoiceDraftRoute.preview(forty + "多"))
        val smile = String(Character.toChars(0x1F600))
        assertEquals(smile.repeat(40) + "...", VoiceDraftRoute.preview(smile.repeat(41)))
        assertEquals("a".repeat(39) + "...", VoiceDraftRoute.preview("a".repeat(39) + "  b"))
    }

    @Test
    fun otherListenersExcludeThisAppAndResolveTheirSettings() {
        val own = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
        val found = listOf(
            ResidentListenerApp("b.stone", "3-Stone AI", ".ui.SettingsActivity"),
            ResidentListenerApp(own, "3-Stove Agent", "$own.speech.ResidentListenerSettings"),
            ResidentListenerApp("a.macaw", "3-Macaw Speech", "a.macaw.ui.settings.SettingsActivity"),
            ResidentListenerApp("b.stone", "3-Stone AI", null),
            ResidentListenerApp("c.other", "other", "not a class"),
        )
        assertEquals(listOf(
            ResidentListenerApp("a.macaw", "3-Macaw Speech", "a.macaw.ui.settings.SettingsActivity"),
            ResidentListenerApp("b.stone", "3-Stone AI", "b.stone.ui.SettingsActivity"),
            ResidentListenerApp("c.other", "other", null),
        ), ResidentListenerApps.others(found, own))
        assertTrue(ResidentListenerApps.others(found.filter { it.packageName == own }, own).isEmpty())
    }

    @Test
    fun roundTextJoinsPiecesAndSkipsFailedRounds() {
        assertEquals("Open the calculator", ResidentRound.join("Open the", " calculator "))
        assertEquals("打开设置", ResidentRound.join("打开", "设置"))
        assertEquals("Hello. World", ResidentRound.join("Hello.", "World"))
        assertEquals("打开 Wi-Fi", ResidentRound.join("打开 ", "Wi-Fi"))
        assertEquals("abc", ResidentRound.join("", "abc"))
        assertEquals("abc", ResidentRound.join("abc", null))
        assertEquals("abc", ResidentRound.join("abc", "  "))
        assertEquals("打开设置", ResidentRound.draft(SpeechContract.REASON_STOPPED, "打开设置", "ignored"))
        assertEquals("finals", ResidentRound.draft(SpeechContract.REASON_MAX_DURATION, " ", "finals"))
        assertNull(ResidentRound.draft(SpeechContract.REASON_TIMEOUT, null, ""))
        assertNull(ResidentRound.draft(SpeechContract.REASON_CANCELLED, "text", "text"))
        assertNull(ResidentRound.draft(SpeechContract.REASON_ERROR, "text", "text"))
        assertTrue(ResidentRound.TIMEOUT_MILLIS < ResidentRound.MAX_DURATION_MILLIS)
    }
}
