package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Opt-in device check (`-e agent.speech.expectMacaw true|false`, see `.python/device_voice_check.py`): the real resolution
 * of the 3-Macaw Speech sheet under Android 11+ package visibility, and the microphone intent opening that sheet itself
 * instead of the system chooser. The sheet is cancelled with Back at once; nothing is recognized or returned.
 */
class SpeechInputDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun realRecognizersMatchTheDeclaredDevice() {
        val expected = InstrumentationRegistry.getArguments().getString("agent.speech.expectMacaw")
        assumeTrue("Requires -e agent.speech.expectMacaw true|false", expected == "true" || expected == "false")
        val macaw = expected.toBoolean()
        val preferences = context.getSharedPreferences(SpeechPreferences.NAME, Context.MODE_PRIVATE); val saved = preferences.all.toMap()
        try {
            preferences.edit().clear().commit()
            assertEquals("3-Macaw Speech sheet resolves", macaw, SpeechInput.macawAvailable(context))
            val intent = SpeechInput.intent(context)
            assertEquals(if (macaw) SpeechInput.MACAW_PACKAGE else null, intent.`package`)
            SpeechPreferences(context).preferMacaw = false
            assertNull("Switched off, the system chooses", SpeechInput.intent(context).`package`)
            if (macaw) {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                waitFor("3-Macaw Speech sheet on top") { top().contains("${SpeechInput.MACAW_PACKAGE}/.ui.voice.VoiceInputActivity") }
                instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                waitFor("Sheet cancelled") { !top().contains(SpeechInput.MACAW_PACKAGE) }
            }
        } finally {
            preferences.edit().clear().apply { saved.forEach { (key, value) -> if (value is Boolean) putBoolean(key, value) } }.commit()
        }
    }

    private fun top(): String {
        val dump = android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("dumpsys activity activities"))
            .bufferedReader().use { it.readText() }
        return dump.lineSequence().firstOrNull { "topResumedActivity" in it || "mResumedActivity" in it }.orEmpty()
    }

    private fun waitFor(label: String, check: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < end) { if (check()) return; SystemClock.sleep(200) }
        fail(label)
    }
}
