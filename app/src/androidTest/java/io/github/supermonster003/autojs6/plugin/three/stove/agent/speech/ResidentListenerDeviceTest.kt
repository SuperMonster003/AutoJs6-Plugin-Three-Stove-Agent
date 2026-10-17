package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ThreeStoveAgentPlugin
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.LauncherActivity
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.SettingsActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Roadmap P17 (D79). E2: the switch is the component state, the settings alias, the resume notification and the draft
 * route without 3-Macaw Speech. E3 (opt-in, `-e agent.resident.acoustic true`, `.python/device_resident_check.py`): a real
 * phone with 3-Macaw Speech, its wake word and recognition models and the microphone granted hears a test recording
 * through its own speaker (media volume as set, at least 40 %) and must turn the sentence after the wake word into a
 * draft. Logs carry numbers only.
 */
class ResidentListenerDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val store = ResidentSettingsStore(context)
    private var savedSettings: ResidentSettings? = null
    private var wasEnabled = false

    @Before fun remember() {
        savedSettings = store.read(); wasEnabled = ResidentListener.enabled(context)
        if (wasEnabled) ResidentListener.turnOff(context)
    }

    @After fun restore() {
        ResidentListener.turnOff(context)
        savedSettings?.let(store::write)
        notifications.cancel(553); notifications.cancel(554); notifications.cancel(555)
        // A listener the maintainer had on stays on; it resumes when Stove is next visible.
        if (wasEnabled) ResidentListener.setEnabled(context, true)
    }

    @Test fun theSwitchIsTheComponentStateThatOtherAppsQuery() {
        val query = Intent(ThreeStoveAgentPlugin.RESIDENT_LISTENER_ACTION).setPackage(context.packageName)
        assertFalse(ResidentListener.enabled(context))
        assertTrue("A disabled component answers no query", context.packageManager.queryIntentServices(query, 0).isEmpty())
        ResidentListener.setEnabled(context, true)
        assertTrue(ResidentListener.enabled(context))
        val found = context.packageManager.queryIntentServices(query, PackageManager.GET_META_DATA).single().serviceInfo
        assertEquals(ResidentListenerService::class.java.name, found.name)
        assertEquals(context.packageName + ".speech.ResidentListenerSettings", found.metaData.getString(ThreeStoveAgentPlugin.RESIDENT_LISTENER_SETTINGS))
        assertEquals("org.autojs.permission.PLUGIN", found.permission)
        assertTrue("This app never counts as a rival", ResidentListener.others(context).none { it.packageName == context.packageName })
        ResidentListener.turnOff(context)
        assertFalse(ResidentListener.enabled(context))
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, context.packageManager.getComponentEnabledSetting(ResidentListener.component(context)))
    }

    @Test fun theSettingsAliasOpensTheResidentRows() {
        val alias = ComponentName(context, context.packageName + ".speech.ResidentListenerSettings")
        assertEquals(SettingsActivity::class.java.name, context.packageManager.getActivityInfo(alias, 0).targetActivity)
        ActivityScenario.launch<SettingsActivity>(Intent().setComponent(alias)).use { scenario ->
            waitFor("Resident rows", 15_000) {
                var ready = false
                scenario.onActivity { activity ->
                    val keywords = activity.rows["resident-keywords"]?.summary?.text?.toString()
                    ready = activity.rows.containsKey("resident") && activity.rows.containsKey("resident-sensitivity") &&
                        keywords == store.read().keywords.joinToString(", ") && activity.resident.availability != null
                }
                ready
            }
            scenario.onActivity { activity ->
                val probe = activity.resident.availability!!
                Log.i(TAG, "settings availability=$probe switchEnabled=${activity.rows.getValue("resident").view.isEnabled}")
                assertEquals(probe == ResidentAvailability.READY, activity.rows.getValue("resident").view.isEnabled)
            }
        }
    }

    @Test fun rebootAndUpdateOnlyRemindAnEnabledListener() {
        val receiver = ResidentResumeReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertNull("Off: no reminder", active(555))
        ResidentListener.setEnabled(context, true)
        receiver.onReceive(context, Intent(Intent.ACTION_SCREEN_ON))
        assertNull("Other broadcasts are ignored", active(555))
        receiver.onReceive(context, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))
        val reminder = waitForNotification(555)
        assertEquals(context.getString(R.string.resident_resume_text), reminder.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
        ResidentListener.turnOff(context)
        assertNull("Turning off withdraws the reminder", active(555))
    }

    @Test fun aDraftWithoutTheBallOpensTheWorkbenchOrANotification() {
        // The ball lives in :agent; this process has no runtime, so the card is never shown here.
        val monitor = instrumentation.addMonitor(LauncherActivity::class.java.name, null, false)
        try {
            var route: VoiceDraftRoute? = null
            instrumentation.runOnMainSync { route = VoiceDrafts.deliver(context, "Resident draft fixture") }
            Log.i(TAG, "draft route=$route overlay=${Settings.canDrawOverlays(context)}")
            when (route) {
                VoiceDraftRoute.WORKBENCH -> {
                    instrumentation.waitForMonitorWithTimeout(monitor, 15_000)
                    waitFor("Workbench draft", 15_000) { launcherGoal() == "Resident draft fixture" }
                }
                VoiceDraftRoute.NOTIFICATION -> {
                    val draft = waitForNotification(553)
                    assertEquals(context.getString(R.string.voice_draft_ready), draft.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
                    assertEquals("Resident draft fixture", draft.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
                    assertEquals(Notification.VISIBILITY_PRIVATE, draft.visibility)
                    assertNotNull(draft.publicVersion)
                }
                else -> fail("Unexpected route $route")
            }
        } finally { instrumentation.removeMonitor(monitor); finishLaunchers() }
    }

    /** E3: wake word "zhu li nan" of the sherpa-onnx keyword test recording, then the bilingual command recording. */
    @Test fun theWakeWordAndTheNextSentenceBecomeADraft() {
        assumeTrue("Requires -e agent.resident.acoustic true", InstrumentationRegistry.getArguments().getString("agent.resident.acoustic") == "true")
        assumeTrue("Another app is listening resident", ResidentListener.others(context).isEmpty())
        val keyword = "朱丽楠"
        store.write(ResidentSettings(listOf(keyword), WakeSensitivity.MEDIUM))
        val audio = context.getSystemService(AudioManager::class.java)
        val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val monitor = instrumentation.addMonitor(LauncherActivity::class.java.name, null, false)
        // A visible Stove screen lets 3-Macaw Speech start its microphone service (API 34+, roadmap D74).
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            try {
                waitFor("3-Macaw Speech probed", 15_000) { var done = false; scenario.onActivity { done = it.resident.availability != null }; done }
                var availability: ResidentAvailability? = null
                scenario.onActivity { availability = it.resident.availability }
                Log.i(TAG, "acoustic availability=$availability")
                assumeTrue("3-Macaw Speech is not ready: $availability", availability == ResidentAvailability.READY)
                ResidentListener.setEnabled(context, true)
                assertTrue(ResidentListener.start(context))
                val started = SystemClock.elapsedRealtime()
                val waiting = context.getString(R.string.resident_waiting, keyword)
                waitFor("Wake session ready", 30_000) { listeningText() == waiting }
                val ready = SystemClock.elapsedRealtime() - started
                // The device's own media volume, raised to 40 % only when it is lower (3-Macaw Speech's wake check plays the same way).
                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (volume * 100 < max * 40) audio.setStreamVolume(AudioManager.STREAM_MUSIC, (max * 40 + 99) / 100, 0)
                val percent = audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max
                val player = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    setDataSource(recording().absolutePath); prepare()
                }
                val played = SystemClock.elapsedRealtime()
                player.start()
                var heard = -1L
                var result: String? = null
                val end = played + 60_000
                while (SystemClock.elapsedRealtime() < end && result == null) {
                    if (heard < 0 && listeningText() == context.getString(R.string.resident_listening)) heard = SystemClock.elapsedRealtime() - played
                    result = launcherGoal()?.takeIf { it.isNotBlank() }?.let { "workbench:${it.length}" }
                        ?: active(553)?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.let { "notification:${it.length}" }
                        ?: "card".takeIf { floatingCard() && listeningText() == waiting && heard >= 0 }
                    SystemClock.sleep(100)
                }
                player.release()
                Log.i(TAG, "acoustic volumePercent=$percent readyMs=$ready hitMs=$heard draft=$result totalMs=${SystemClock.elapsedRealtime() - played}")
                assertTrue("The wake word was not heard", heard >= 0)
                assertNotNull("No draft after the sentence", result)
                assertEquals("The listener waits again", waiting, listeningText())
            } finally {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                instrumentation.removeMonitor(monitor); finishLaunchers()
            }
        }
    }

    /** kws-zh-6, 0.3 s of silence, mixed-zh-en and 1 s of silence as one 16 kHz mono PCM16 WAV in the cache. */
    private fun recording(): File {
        fun pcm(name: String): ByteArray {
            val bytes = instrumentation.context.assets.open("test-audio/$name.wav").use { it.readBytes() }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            var offset = 12
            while (offset + 8 <= bytes.size) {
                val id = String(bytes, offset, 4, Charsets.US_ASCII); val size = buffer.getInt(offset + 4)
                if (id == "data") return bytes.copyOfRange(offset + 8, minOf(bytes.size, offset + 8 + size))
                offset += 8 + size + (size and 1)
            }
            error("No data chunk in $name")
        }
        val data = pcm("kws-zh-6") + ByteArray(16_000 * 2 * 3 / 10) + pcm("mixed-zh-en") + ByteArray(16_000 * 2)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data.size); put("WAVE".toByteArray()); put("fmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(16_000); putInt(32_000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data.size)
        }.array()
        return File(context.cacheDir, "resident-wake.wav").apply { writeBytes(header + data) }
    }

    private fun active(id: Int): Notification? = notifications.activeNotifications.firstOrNull { it.id == id }?.notification
    private fun listeningText(): String? = active(ResidentNotifications.LISTENING_ID)?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
    private fun waitForNotification(id: Int): Notification { var found: Notification? = null; waitFor("Notification $id", 10_000) { found = active(id); found != null }; return found!! }

    private fun launcherGoal(): String? {
        var goal: String? = null
        instrumentation.runOnMainSync {
            goal = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<LauncherActivity>()
                .firstOrNull()?.findViewById<EditText>(R.id.workbench_goal)?.text?.toString()
        }
        return goal
    }

    private fun finishLaunchers() = instrumentation.runOnMainSync {
        listOf(Stage.RESUMED, Stage.STARTED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED).flatMap {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it)
        }.filterIsInstance<LauncherActivity>().forEach { it.finish() }
    }

    private fun floatingCard(): Boolean = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("dumpsys window windows"))
        .bufferedReader().use { it.readText() }.contains("3-Stove Agent floating card")

    private fun waitFor(label: String, timeoutMs: Long, check: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) { if (check()) return; SystemClock.sleep(150) }
        fail(label)
    }

    private companion object { const val TAG = "StoveResidentTest" }
}
