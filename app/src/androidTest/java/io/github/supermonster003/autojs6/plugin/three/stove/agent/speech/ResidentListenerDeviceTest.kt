package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.Instrumentation
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.Lifecycle
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
 * Roadmap P17 (D79). E2: the switch is the component state, the settings alias, the resume notification, the draft route
 * without the ball, the VOICE_COMMAND draft on the floating card and another app's resident listener (D73). E3 (opt-in,
 * `-e agent.resident.acoustic true`, `.python/device_resident_check.py`): a real phone with 3-Macaw Speech, its wake word
 * and recognition models and the microphone granted hears a test recording through its own speaker (media volume as set,
 * at least 40 % or `-e agent.resident.minVolume <percent>`) and must turn the sentence after the wake word into a draft, once from the background without the ball
 * and once on the floating card. Tests that need the overlay permission skip without it, unless
 * `-e agent.resident.overlay appops` lets them allow it for their duration (FloatingBallFixture). The joint run with
 * 3-Macaw Speech (`-e agent.resident.macawTakeover sheet|assistant`) opens its voice input sheet or its digital assistant
 * while the listener waits and checks that the wake session is silenced and resumed (its roadmap D57). P17.7 (D81, D82):
 * the listening time and "only while charging" rows, the deadline with and without a running service (D67 coexistence)
 * and, with a ready 3-Macaw Speech, the deadline and the pause and resume across power changes, which `dumpsys battery`
 * simulates (always reset afterwards). Logs carry numbers only.
 */
class ResidentListenerDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val arguments = InstrumentationRegistry.getArguments()
    private val acoustic = arguments.getString("agent.resident.acoustic") == "true"
    private val grantOverlay = arguments.getString("agent.resident.overlay") == "appops"
    /** The joint run with 3-Macaw Speech: `sheet` (its voice input) or `assistant` (its digital assistant). */
    private val macawTakeover = arguments.getString("agent.resident.macawTakeover")
    /** The lowest media volume the acoustic cases play at; a phone whose microphone needs more is given it explicitly. */
    private val minVolume = arguments.getString("agent.resident.minVolume")?.toIntOrNull()?.coerceIn(40, 100) ?: 40
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val store = ResidentSettingsStore(context)
    private var savedSettings: ResidentSettings? = null
    private var wasEnabled = false
    /** The settings screen [listening] keeps open. */
    private var screen: ActivityScenario<SettingsActivity>? = null
    private var batterySimulated = false

    @Before fun remember() {
        savedSettings = store.read(); wasEnabled = ResidentListener.enabled(context)
        if (wasEnabled) ResidentListener.turnOff(context)
        // The tests start from the D81 defaults: a stored listening time would turn the switch off under them.
        store.write(ResidentSettings(savedSettings!!.keywords, savedSettings!!.sensitivity))
    }

    @After fun restore() {
        if (batterySimulated) battery("reset")
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
                Log.i(TAG, "case=settings availability=$probe switchEnabled=${activity.rows.getValue("resident").view.isEnabled}")
                assertEquals(probe == ResidentAvailability.READY, activity.rows.getValue("resident").view.isEnabled)
            }
        }
    }

    @Test fun rebootAndUpdateOnlyRemindAnEnabledListener() {
        requireNotifications()
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
        // The notification manager withdraws asynchronously (seen on API 24).
        waitFor("Turning off withdraws the reminder", 5_000) { active(555) == null }
    }

    @Test fun aDraftWithoutTheBallOpensTheWorkbenchOrANotification() = FloatingBallFixture.withOverlay<Unit>(grantOverlay) {
        // The ball lives in :agent; this process has no runtime, so the card is never shown here.
        val monitor = instrumentation.addMonitor(LauncherActivity::class.java.name, null, false)
        try {
            var route: VoiceDraftRoute? = null
            instrumentation.runOnMainSync { route = VoiceDrafts.deliver(context, "Resident draft fixture") }
            Log.i(TAG, "case=draft route=$route overlay=${Settings.canDrawOverlays(context)}")
            assertEquals(expectedRoute(), route)
            if (route == VoiceDraftRoute.NOTIFICATION) requireNotifications()
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

    /** D62 / D72 / D78: with the ball on screen the assistant's text fills the card, replacing an earlier draft; nothing runs. */
    @Test fun voiceCommandFillsTheFloatingCardAndReplacesItsDraft() = FloatingBallFixture.run<Unit>(grantOverlay) { ball ->
        val monitor = instrumentation.addMonitor(LauncherActivity::class.java.name, null, false)
        try {
            home()
            voiceCommand("Card_draft_first")
            waitFor("The card holds the first draft", 15_000) { ball.cardText() == "Card_draft_first" }
            assertTrue("The card window is on screen", ball.cardShown())
            voiceCommand("Card_draft_second")
            waitFor("The second draft replaced the first", 15_000) { ball.cardText() == "Card_draft_second" }
            SystemClock.sleep(500)
            assertEquals("The workbench stays closed", 0, monitor.hits)
            assertEquals("A draft only: no model call", 0, ball.modelCalls.get())
            Log.i(TAG, "case=voiceCommand route=CARD replaced=true workbench=0 modelCalls=${ball.modelCalls.get()}")
        } finally { instrumentation.removeMonitor(monitor); finishLaunchers() }
    }

    /** D73: another app's enabled resident listener keeps this one off, with a way to that app's settings. */
    @Test fun anotherResidentListenerKeepsThisOneOff() {
        requireNotifications()
        val testPackage = instrumentation.context.packageName
        val label = instrumentation.context.applicationInfo.loadLabel(context.packageManager).toString()
        fun rival(enabled: Boolean) = context.sendBroadcast(Intent().setComponent(ComponentName(testPackage, ResidentRivalService.Switch::class.java.name))
            .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES).putExtra(ResidentRivalService.EXTRA_ENABLED, enabled))
        rival(true)
        try {
            waitFor("The rival answers the query", 15_000) { ResidentListener.others(context).any { it.packageName == testPackage } }
            val found = ResidentListener.others(context).single { it.packageName == testPackage }
            assertEquals(label, found.label)
            assertEquals(ResidentRivalSettingsActivity::class.java.name, found.settingsClass)
            ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
                val availability = probe(scenario, "rival")
                var dialog = false
                var opened = false
                // The switch reaches the conflict only while 3-Macaw Speech is ready; otherwise it is disabled (D71).
                if (availability == ResidentAvailability.READY) {
                    scenario.onActivity {
                        assertTrue("The row names the rival", it.rows.getValue("resident").summary.text.contains(label))
                        it.rows.getValue("resident").view.performClick()
                    }
                    waitFor("Conflict dialog", 10_000) { var shown = false; scenario.onActivity { shown = it.resident.dialog?.isShowing == true }; shown }
                    scenario.onActivity {
                        val shown = it.resident.dialog!!
                        assertTrue(shown.findViewById<TextView>(android.R.id.message)?.text?.contains(label) == true)
                        assertEquals(it.getString(R.string.resident_conflict_open, label), shown.getButton(DialogInterface.BUTTON_POSITIVE).text.toString())
                        assertFalse("The switch stays off", it.rows.getValue("resident").switch!!.isChecked)
                        shown.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                    }
                    dialog = true
                    waitFor("The rival's settings opened", 10_000) { resumed().contains(ResidentRivalSettingsActivity::class.java.name) }
                    opened = true
                    waitFor("Back to the settings screen", 15_000) { scenario.state == Lifecycle.State.RESUMED }
                }
                assertFalse(ResidentListener.enabled(context))
                // A visible screen does not resume a listener beside the rival; started anyway, the service stops and says why.
                ResidentListener.setEnabled(context, true)
                ResidentListener.resume(context)
                SystemClock.sleep(1500)
                assertNull("No wake session beside the rival", active(ResidentNotifications.LISTENING_ID))
                assertTrue(ResidentListener.start(context))
                val stopped = waitForNotification(554)
                assertTrue(stopped.extras.getCharSequence(Notification.EXTRA_TEXT)?.contains(label) == true)
                waitFor("The service is gone", 10_000) { active(ResidentNotifications.LISTENING_ID) == null }
                Log.i(TAG, "case=rival availability=$availability others=${ResidentListener.others(context).size} dialog=$dialog opened=$opened stopped=CONFLICT")
            }
        } finally {
            rival(false)
            waitFor("The rival is off again", 15_000) { ResidentListener.others(context).none { it.packageName == testPackage } }
        }
    }

    /** D81 (P17.7): the two rows right below the switch, their texts, and that they save at once. */
    @Test fun theListeningTimeAndChargingRowsSaveAtOnce() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            waitFor("Resident rows", 15_000) { var built = false; scenario.onActivity { built = it.rows.containsKey("resident-charging") }; built }
            scenario.onActivity { activity ->
                val duration = activity.rows.getValue("resident-duration"); val charging = activity.rows.getValue("resident-charging")
                assertEquals(activity.getString(R.string.settings_resident_duration), duration.title.text.toString())
                assertEquals(activity.getString(R.string.resident_duration_always), duration.summary.text.toString())
                assertEquals(activity.getString(R.string.settings_resident_charging), charging.title.text.toString())
                assertEquals(activity.getString(R.string.settings_resident_charging_note), charging.summary.text.toString())
                assertFalse(charging.switch!!.isChecked)
                val page = duration.view.parent as ViewGroup
                val order = listOf("resident", "resident-duration", "resident-charging", "resident-keywords").map { page.indexOfChild(activity.rows.getValue(it).view) }
                assertEquals("Right below the switch, above the wake words", order.sorted(), order)
                activity.resident.duration.choose(0)
                charging.view.performClick()
            }
            val saved = store.read()
            assertEquals(ListeningDuration.MINUTES_10, saved.duration)
            assertTrue(saved.chargingOnly)
            assertNull("Off: the listening time starts when the switch turns on", saved.deadline)
            scenario.onActivity { activity ->
                assertEquals(activity.getString(R.string.resident_duration_10), activity.rows.getValue("resident-duration").summary.text.toString())
                assertTrue(activity.rows.getValue("resident-charging").switch!!.isChecked)
                Log.i(TAG, "case=limitRows duration=${saved.duration.id} chargingOnly=${saved.chargingOnly} rowsBelowSwitch=true")
            }
        }
    }

    /** D82 with D67: a listening time that ran out while nothing ran turns the switch off instead of resuming or reminding. */
    @Test fun anExpiredListeningTimeTurnsOffInsteadOfResumingOrReminding() {
        store.write(store.read().copy(duration = ListeningDuration.MINUTES_10, deadline = System.currentTimeMillis() - 1000))
        ResidentListener.setEnabled(context, true)
        ResidentResumeReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertFalse("After a reboot past the deadline the switch is off", ResidentListener.enabled(context))
        assertNull("... and nothing reminds", active(555))
        ResidentListener.setEnabled(context, true)
        ResidentListener.resume(context)
        assertFalse("A visible screen past the deadline turns it off", ResidentListener.enabled(context))
        // A start that comes anyway turns the switch off at once and leaves no notification.
        ResidentListener.setEnabled(context, true)
        assertTrue(ResidentListener.start(context))
        waitFor("The service turned the switch off", 10_000) { !ResidentListener.enabled(context) }
        waitFor("No ongoing notification", 10_000) { active(ResidentNotifications.LISTENING_ID) == null }
        assertNull(active(554)); assertNull(active(555))
        // Within the listening time the D67 reminder still comes.
        var reminded = "skipped"
        if (notifications.areNotificationsEnabled()) {
            store.write(store.read().copy(deadline = System.currentTimeMillis() + 10 * 60_000L))
            ResidentListener.setEnabled(context, true)
            ResidentResumeReceiver().onReceive(context, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))
            waitForNotification(555)
            assertTrue(ResidentListener.enabled(context))
            reminded = "true"
        }
        Log.i(TAG, "case=expiredResume boot=off visible=off service=off reminderWithinTime=$reminded")
    }

    /**
     * D81 with a ready 3-Macaw Speech: turned on with 10 minutes, the ongoing notification and the switch row say when
     * it ends; with the deadline moved four seconds ahead (as if ten minutes had passed) the switch turns off and the
     * ongoing notification goes, with no stop or resume notification.
     */
    @Test fun theListeningTimeTurnsTheListenerOffAtItsDeadline() {
        requireNotifications()
        assumeTrue("Another app is listening resident", ResidentListener.others(context).isEmpty())
        listening("deadline", ResidentSettings(listOf(KEYWORD), WakeSensitivity.MEDIUM, ListeningDuration.MINUTES_10)) { _, _ ->
            val deadline = store.read().deadline!!
            val left = deadline - System.currentTimeMillis()
            assertTrue("Turning on set ten minutes: $left", left in 9 * 60_000L..10 * 60_000L)
            val until = ResidentNotifications.until(context, deadline)
            waitFor("The ongoing notification says when it turns off", 10_000) { subText() == until }
            screen!!.moveToState(Lifecycle.State.STARTED); screen!!.moveToState(Lifecycle.State.RESUMED)
            // The screen follows the language of AutoJs6, which may differ from the system's.
            waitFor("The switch row says it too", 10_000) {
                var shown = false
                screen!!.onActivity { shown = it.rows.getValue("resident").summary.text.endsWith(ResidentNotifications.until(it, deadline)) }
                shown
            }
            val soon = System.currentTimeMillis() + 4000
            store.update { it.copy(deadline = soon) }
            ResidentListener.refresh(context)
            waitFor("The new turn-off time", 10_000) { subText() == ResidentNotifications.until(context, soon) }
            waitFor("The switch turned off at the deadline", 20_000) { !ResidentListener.enabled(context) }
            val offMs = System.currentTimeMillis() - soon
            waitFor("The ongoing notification is gone", 10_000) { active(ResidentNotifications.LISTENING_ID) == null }
            SystemClock.sleep(1000)
            assertNull("No stop notification", active(554)); assertNull("No resume reminder", active(555))
            assertFalse("The service is gone", listenerRunning())
            Log.i(TAG, "case=deadline minutesSet=10 offAfterDeadlineMs=$offMs ongoing=gone stopNotice=none")
        }
    }

    /**
     * D81 / D82 with a ready 3-Macaw Speech: "only while charging" pauses when unplugged (no wake session, 3-Macaw Speech
     * stops recording) and listens again when plugged in. Plugged in with the home screen in front, the session opens
     * from the background, where Android 11+ may give 3-Macaw Speech no microphone: the listener then says "tap to
     * continue" and the tap (the notification's settings screen) continues it. Plugged in while a Stove screen is visible,
     * it continues at once. The power changes are `dumpsys battery` simulations, reset at the end.
     */
    @Test fun onlyWhileChargingPausesUnpluggedAndContinuesPluggedIn() = FloatingBallFixture.withOverlay<Unit>(grantOverlay) {
        requireNotifications()
        assumeTrue("Another app is listening resident", ResidentListener.others(context).isEmpty())
        val paused = context.getString(R.string.resident_paused); val blocked = context.getString(R.string.resident_blocked)
        battery("set ac 1")
        listening("charging", ResidentSettings(listOf(KEYWORD), WakeSensitivity.MEDIUM, chargingOnly = true)) { waiting, workbench ->
            home()
            // 1. Unplugged in the background: paused, and 3-Macaw Speech stops recording.
            var started = SystemClock.elapsedRealtime()
            battery("unplug")
            waitFor("Paused when unplugged", 10_000) { listeningText() == paused }
            val pauseMs = SystemClock.elapsedRealtime() - started
            waitFor("3-Macaw Speech stopped recording", 10_000) { macawRecording() == "none" }
            val stopMs = SystemClock.elapsedRealtime() - started
            assertTrue("The listener stays on while paused", ResidentListener.enabled(context))
            // 2. Plugged in with the home screen in front: the session opens from the background.
            started = SystemClock.elapsedRealtime()
            battery("set ac 1")
            waitFor("Listening again or waiting for a screen", 30_000) { listeningText() == waiting || listeningText() == blocked }
            // A refused or silenced microphone is reported within seconds; let it come before judging.
            SystemClock.sleep(8000)
            val background = when (listeningText()) { waiting -> "waiting"; blocked -> "blocked"; else -> "other" }
            val backgroundMs = SystemClock.elapsedRealtime() - started - 8000
            val backgroundRecording = macawRecording()
            Log.i(TAG, "case=chargingBackground api=${Build.VERSION.SDK_INT} overlay=${Settings.canDrawOverlays(context)} pauseMs=$pauseMs macawStoppedMs=$stopMs backgroundResume=$background " +
                "backgroundMs=$backgroundMs backgroundRecording=$backgroundRecording")
            var heardInBackground = "notTried"
            if (background == "waiting" && acoustic) Speaker().use { speaker ->
                heardInBackground = runCatching { hearKeyword(speaker, context.getString(R.string.resident_listening)).first.toString() }.getOrDefault("missed")
                waitFor("The round after the keyword ends", 20_000) { listeningText() == waiting }
            }
            // 3. Waiting for a screen: no recording; tapping the notification opens the settings, which continue it.
            var tapMs = -1L
            if (background == "blocked") {
                assertEquals("No recording while it waits for a screen", "none", macawRecording())
                started = SystemClock.elapsedRealtime()
                active(ResidentNotifications.LISTENING_ID)!!.contentIntent.send()
                waitFor("The tap continued listening", 30_000) { listeningText() == waiting }
                tapMs = SystemClock.elapsedRealtime() - started
            } else {
                assertEquals("The background open listens", "waiting", background)
                context.startActivity(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                waitFor("A Stove screen in front", 10_000) {
                    var front = false
                    instrumentation.runOnMainSync { front = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).any { it is SettingsActivity } }
                    front
                }
            }
            waitFor("Recording again", 10_000) { macawRecording() != "none" }
            // 4. Unplugged and plugged in while a Stove screen is visible: it continues at once.
            battery("unplug")
            waitFor("Paused again", 10_000) { listeningText() == paused }
            started = SystemClock.elapsedRealtime()
            battery("set ac 1")
            waitFor("Listening again with the screen visible", 30_000) { listeningText() == waiting }
            SystemClock.sleep(8000)
            val visibleResume = if (listeningText() == waiting) "waiting" else "other:${listeningText()?.length}"
            Log.i(TAG, "case=charging heardInBackground=$heardInBackground tapMs=$tapMs visibleResume=$visibleResume " +
                "visibleMs=${SystemClock.elapsedRealtime() - started - 8000} recording=${macawRecording()}")
            assertEquals("Plugged in with a visible screen continues at once", "waiting", visibleResume)
            assertNull("No draft notification", active(553))
            assertEquals("No workbench", 0, workbench.hits)
            finishSettings()
        }
    }

    /**
     * E3: wake word "zhu li nan" of the sherpa-onnx keyword test recording, then the bilingual command recording. The
     * listener starts while the settings screen is visible (3-Macaw Speech needs that for its microphone on API 34+,
     * D74); the phone then shows the home screen, so the draft takes the background route of D77: the workbench with the
     * overlay permission, otherwise a notification.
     */
    @Test fun theWakeWordAndTheNextSentenceBecomeADraft() = FloatingBallFixture.withOverlay<Unit>(grantOverlay) {
        assumeTrue("Requires -e agent.resident.acoustic true", acoustic)
        requireNotifications()
        assumeTrue("Another app is listening resident", ResidentListener.others(context).isEmpty())
        assumeTrue("The floating ball is on screen; theFloatingCardListensThenRestoresOrTakesTheDraft covers that route", !ballOnScreen())
        listening("acoustic") { waiting, _ ->
            home()
            val expected = expectedRoute()
            Speaker().use { speaker ->
                speaker.play(recording())
                val heard = speaker.waitForHit()
                var result: String? = null
                val end = speaker.started + 60_000
                while (SystemClock.elapsedRealtime() < end && result == null) {
                    result = launcherGoal()?.takeIf { it.isNotBlank() }?.let { "workbench:${it.length}" }
                        ?: active(553)?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.let { "notification:${it.length}" }
                    SystemClock.sleep(100)
                }
                Log.i(TAG, "case=acoustic volumePercent=${speaker.percent} hitMs=$heard expected=$expected draft=$result totalMs=${SystemClock.elapsedRealtime() - speaker.started}")
                assertNotNull("No draft after the sentence", result)
                assertEquals("The draft took the background route", expected.name.lowercase(), result!!.substringBefore(':'))
                waitFor("The listener waits again", 10_000) { listeningText() == waiting }
            }
        }
    }

    /**
     * E3 on the floating card (D62, D72, D77): over the home screen the card holds the assistant's draft; the keyword alone
     * makes it say "Listening..." and, after a round without speech, brings that draft back; the keyword and the sentence
     * replace it with the dictated text. Nothing is started.
     */
    @Test fun theFloatingCardListensThenRestoresOrTakesTheDraft() {
        assumeTrue("Requires -e agent.resident.acoustic true", acoustic)
        requireNotifications()
        assumeTrue("Another app is listening resident", ResidentListener.others(context).isEmpty())
        FloatingBallFixture.run(grantOverlay) { ball ->
            val cardListening = ball.text(R.string.resident_listening)
            listening("card") { waiting, workbench ->
                home()
                voiceCommand(EARLIER_DRAFT)
                waitFor("The assistant's draft on the card", 15_000) { ball.cardText() == EARLIER_DRAFT }
                Speaker().use { speaker ->
                    // 1. The keyword alone (cut before the speech that follows it in the recording), then silence.
                    speaker.play(keywordAlone())
                    val aloneMs = speaker.waitForHit()
                    val roundStarted = SystemClock.elapsedRealtime()
                    var listeningCard = false
                    while (!listeningCard && listeningText() != waiting && SystemClock.elapsedRealtime() < roundStarted + 15_000) {
                        listeningCard = ball.cardShows(cardListening); SystemClock.sleep(100)
                    }
                    assertTrue("The card says it listens", listeningCard)
                    waitFor("The empty round ends", 20_000) { listeningText() == waiting }
                    val emptyRoundMs = SystemClock.elapsedRealtime() - roundStarted
                    // Restored, or (in a noisy room) a draft of what the round heard instead; only its length is logged.
                    var after: String? = null
                    val settled = SystemClock.elapsedRealtime() + 10_000
                    while (SystemClock.elapsedRealtime() < settled && (after == null || after != EARLIER_DRAFT)) {
                        after = ball.cardText()?.takeIf { !ball.cardShows(cardListening) }; SystemClock.sleep(150)
                    }
                    val emptyRound = when (after) { EARLIER_DRAFT -> "restored"; null -> "unknown"; else -> "draft:${after.length}" }
                    Log.i(TAG, "case=card keywordAloneHitMs=$aloneMs listeningCard=$listeningCard emptyRoundMs=$emptyRoundMs emptyRound=$emptyRound")
                    assertEquals("The earlier draft is back after a round without speech", "restored", emptyRound)
                    assertNull("An empty round leaves no draft notification", active(553))
                    // 2. The keyword and the sentence, after the 1.5 s minimum interval between hits.
                    SystemClock.sleep(2000)
                    speaker.play(recording())
                    val heard = speaker.waitForHit()
                    var listeningAgain = false
                    var text: String? = null
                    val end = speaker.started + 60_000
                    while (SystemClock.elapsedRealtime() < end && text == null) {
                        if (!listeningAgain) listeningAgain = ball.cardShows(cardListening)
                        text = ball.cardText()?.takeIf { listeningText() == waiting && it.isNotBlank() && it != EARLIER_DRAFT && !ball.cardShows(cardListening) }
                        SystemClock.sleep(100)
                    }
                    Log.i(TAG, "case=card volumePercent=${speaker.percent} hitMs=$heard listeningCard=$listeningAgain draft=card:${text?.length} totalMs=${SystemClock.elapsedRealtime() - speaker.started}")
                    assertNotNull("No dictated draft on the card", text)
                    assertNull("The card route sends no notification", active(553))
                    assertEquals("The card route keeps the workbench closed", 0, workbench.hits)
                    assertEquals("A draft only: no model call", 0, ball.modelCalls.get())
                }
            }
        }
    }

    /**
     * Joint run with 3-Macaw Speech (its roadmap D57; opt-in, `-e agent.resident.macawTakeover sheet`): while this listener
     * holds the wake session, 3-Macaw Speech's RECOGNIZE_SPEECH sheet takes the microphone over and hears the wake word
     * and a sentence; the listener must stay silent meanwhile, and once the sheet is gone the wake word wakes it again.
     * No digital assistant role needed.
     */
    @Test fun macawVoiceInputSilencesTheListenerUntilItCloses() {
        assumeTrue("Requires -e agent.resident.macawTakeover sheet", macawTakeover == "sheet")
        val sheet = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).setPackage(MacawSpeech.PACKAGE)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        assumeTrue("The 3-Macaw Speech voice input is off", sheet.resolveActivity(context.packageManager) != null)
        // The sheet finishes with its result. zh-CN: 弹层随结果关闭.
        macawTakeover("macawTakeover", VOICE_RESULT, open = { context.startActivity(sheet) }, close = {})
    }

    /**
     * The same with 3-Macaw Speech's digital assistant (opt-in, `-e agent.resident.macawTakeover assistant`), which the
     * maintainer chose as the default assistant: the assist key opens its sheet, whose dictation silences the wake session;
     * the sheet then shows the text until the back key closes it, and the wake word must wake the listener again.
     */
    @Test fun macawAssistantSilencesTheListenerUntilItCloses() {
        assumeTrue("Requires -e agent.resident.macawTakeover assistant", macawTakeover == "assistant")
        assumeTrue("3-Macaw Speech is the default digital assistant", shell("settings get secure voice_interaction_service").startsWith("${MacawSpeech.PACKAGE}/"))
        macawTakeover("macawAssistant", ASSISTANT_RESULT, open = { shell("input keyevent KEYCODE_ASSIST") }, close = {
            if (ASSISTANT_SHEET in resumed()) shell("input keyevent KEYCODE_BACK")
            waitFor("The assistant sheet closes", 10_000) { ASSISTANT_SHEET !in resumed() }
        })
    }

    /**
     * The keyword alone is played before and after the takeover, so those rounds end without a draft. [open] starts the
     * screen that takes over, [result] matches its log line of the finished dictation (the result code as the first group
     * when there is one, the characters as the last) and [close] closes what stays on screen afterwards.
     */
    private fun macawTakeover(label: String, result: Regex, open: () -> Unit, close: () -> Unit) {
        requireNotifications()
        assumeTrue("Another app is listening resident", ResidentListener.others(context).isEmpty())
        listening(label) { waiting, workbench ->
            val listening = context.getString(R.string.resident_listening)
            Speaker().use { speaker ->
                // 1. Before: the keyword alone wakes the listener; the round after it hears nothing.
                val before = hearKeyword(speaker, listening)
                waitFor("The empty round before the takeover ends", 20_000) { listeningText() == waiting }
                SystemClock.sleep(2000)
                // 2. During: the dictation silences the wake session (D57) and hears the wake word and the sentence.
                val since = System.currentTimeMillis()
                val opened = SystemClock.elapsedRealtime()
                open()
                val yieldMs = waitForMacaw(since, YIELDS, 15_000)
                SystemClock.sleep(1000) // The sheet loads its recognition model, then records.
                speaker.play(recording())
                var duringHits = 0
                var wasListening = false
                var done: MatchResult? = null
                var lastRead = 0L
                val end = SystemClock.elapsedRealtime() + 60_000
                while (done == null && SystemClock.elapsedRealtime() < end) {
                    val now = listeningText() == listening
                    if (now && !wasListening) duringHits++
                    wasListening = now
                    if (SystemClock.elapsedRealtime() - lastRead >= 500) { done = result.find(macawLog(since)); lastRead = SystemClock.elapsedRealtime() }
                    SystemClock.sleep(100)
                }
                val dictationMs = SystemClock.elapsedRealtime() - opened
                assertNotNull("The dictation gave no result", done)
                val resumeMs = waitForMacaw(since, RESUMES, 10_000) - dictationMs
                if (listeningText() == listening && !wasListening) duringHits++
                close()
                val code = done!!.groupValues.drop(1).dropLast(1).firstOrNull()?.toInt()
                val chars = done.groupValues.last().toInt()
                waitFor("The listener waits after the takeover", 10_000) { listeningText() == waiting }
                SystemClock.sleep(2000)
                // 3. After: the wake word wakes the listener again.
                val after = hearKeyword(speaker, listening)
                waitFor("The empty round after the takeover ends", 20_000) { listeningText() == waiting }
                Log.i(TAG, "case=$label volumePercent=${speaker.percent} beforeHitMs=${before.first} beforePlays=${before.second} " +
                    "yieldMs=$yieldMs duringHits=$duringHits resultCode=$code resultChars=$chars dictationMs=$dictationMs resumeMs=$resumeMs " +
                    "afterHitMs=${after.first} afterPlays=${after.second} yields=${YIELDS.findAll(macawLog(since)).count()} resumes=${RESUMES.findAll(macawLog(since)).count()}")
                assertEquals("The listener stayed silent while 3-Macaw Speech dictated (D57)", 0, duringHits)
                if (code != null) assertEquals("The sheet returned RESULT_OK", Activity.RESULT_OK, code)
                assertTrue("The dictation heard the sentence", chars > 0)
                assertNull("No draft notification", active(553))
                assertEquals("No workbench", 0, workbench.hits)
            }
        }
    }

    /**
     * Plays the keyword alone until the listener says it listens, at most three times (the keyword of kws-zh-6 is missed
     * in about one play of thirty); answers the milliseconds from the start of the last play and the number of plays.
     */
    private fun hearKeyword(speaker: Speaker, listening: String): Pair<Long, Int> {
        for (plays in 1..3) {
            speaker.play(keywordAlone())
            while (SystemClock.elapsedRealtime() < speaker.started + 6_000) {
                if (listeningText() == listening) return (SystemClock.elapsedRealtime() - speaker.started) to plays
                SystemClock.sleep(100)
            }
        }
        throw AssertionError("The wake word was not heard in three plays (volumePercent=${speaker.percent})")
    }

    /** 3-Macaw Speech's session, voice input and assistant lines (numbers only) since [since], in epoch milliseconds. */
    private fun macawLog(since: Long) = shell("logcat -d -v brief -T ${since / 1000}.${"%03d".format(since % 1000)} -s MacawSessions:I MacawVoiceInput:I MacawAssistant:I")

    /** Waits for [pattern] in 3-Macaw Speech's log; answers the milliseconds since [since]. */
    private fun waitForMacaw(since: Long, pattern: Regex, timeoutMs: Long): Long {
        waitFor("3-Macaw Speech logged ${pattern.pattern}", timeoutMs) { pattern.containsMatchIn(macawLog(since)) }
        return System.currentTimeMillis() - since
    }

    /**
     * Opens the settings screen, waits for a ready 3-Macaw Speech, turns the listener on with the keyword of the test
     * recording and runs [block] once the wake session waits, with a monitor of the workbench; turns it off and closes
     * any workbench afterwards.
     */
    private fun listening(label: String, settings: ResidentSettings = ResidentSettings(listOf(KEYWORD), WakeSensitivity.MEDIUM),
                          block: (waiting: String, workbench: Instrumentation.ActivityMonitor) -> Unit) {
        store.write(settings)
        val workbench = instrumentation.addMonitor(LauncherActivity::class.java.name, null, false)
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            screen = scenario
            try {
                val availability = probe(scenario, label)
                val allowActivityStarts = MacawSpeech.bindFlags() and Context.BIND_ALLOW_ACTIVITY_STARTS != 0
                Log.i(TAG, "case=$label api=${Build.VERSION.SDK_INT} availability=$availability allowActivityStarts=$allowActivityStarts")
                assumeTrue("3-Macaw Speech is not ready: $availability", availability == ResidentAvailability.READY)
                assertEquals("Stove lets 3-Macaw Speech start its microphone activity on API 34+ (D74)", Build.VERSION.SDK_INT >= 34, allowActivityStarts)
                // A visible Stove screen lets 3-Macaw Speech start its microphone service (API 34+, roadmap D74).
                assertTrue(ResidentListener.turnOn(context))
                val started = SystemClock.elapsedRealtime()
                val waiting = context.getString(R.string.resident_waiting, KEYWORD)
                waitFor("Wake session ready", 30_000) { listeningText() == waiting }
                Log.i(TAG, "case=$label readyMs=${SystemClock.elapsedRealtime() - started}")
                block(waiting, workbench)
            } finally { ResidentListener.turnOff(context); instrumentation.removeMonitor(workbench); finishLaunchers(); screen = null }
        }
    }

    /**
     * The settings screen's probe of 3-Macaw Speech (D74). A probe that ran out of its 5 s (for example while the phone
     * still optimizes the freshly installed APKs) is repeated once by resuming the screen again, as returning to it would.
     */
    private fun probe(scenario: ActivityScenario<SettingsActivity>, label: String): ResidentAvailability {
        fun read(): ResidentAvailability? { var value: ResidentAvailability? = null; scenario.onActivity { value = it.resident.availability }; return value }
        waitFor("3-Macaw Speech probed", 15_000) { read() != null }
        if (read() != ResidentAvailability.MACAW_UNAVAILABLE) return read()!!
        scenario.moveToState(Lifecycle.State.STARTED); scenario.moveToState(Lifecycle.State.RESUMED)
        SystemClock.sleep(6000) // The probe's timeout plus a margin: the new result replaces the old one.
        Log.i(TAG, "case=$label probeRetries=1 availability=${read()}")
        return read()!!
    }

    /** Plays recordings through the phone's own speaker at the device media volume, raised to [minVolume] only when lower. */
    private inner class Speaker : AutoCloseable {
        private val audio = context.getSystemService(AudioManager::class.java)
        private val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        private var player: MediaPlayer? = null
        val percent: Int
        var started = 0L; private set
        init {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (volume * 100 < max * minVolume) audio.setStreamVolume(AudioManager.STREAM_MUSIC, (max * minVolume + 99) / 100, 0)
            percent = audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max
        }
        fun play(file: File) {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                setDataSource(file.absolutePath); prepare()
            }
            started = SystemClock.elapsedRealtime()
            player!!.start()
        }
        /** Milliseconds from the start of the playback to the "Listening..." state of the listener. */
        fun waitForHit(): Long {
            val listening = context.getString(R.string.resident_listening)
            waitFor("The wake word was not heard (volumePercent=$percent)", 30_000) { listeningText() == listening }
            return SystemClock.elapsedRealtime() - started
        }
        override fun close() { player?.release(); player = null; audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0) }
    }

    /**
     * The listener's state and the reminders are read from this app's notifications. Without the notification permission
     * (Android 13+, a system dialog the user answers) they are not posted, so these checks skip instead of failing.
     */
    private fun requireNotifications() = assumeTrue("Notifications are off for 3-Stove Agent (grant them on the phone)", notifications.areNotificationsEnabled())

    /** The route [VoiceDrafts.deliver] takes without the ball (D77). */
    private fun expectedRoute(): VoiceDraftRoute {
        val power = context.getSystemService(PowerManager::class.java); val keyguard = context.getSystemService(KeyguardManager::class.java)
        return VoiceDraftRoute.of(false, power.isInteractive && !keyguard.isKeyguardLocked, Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context))
    }

    /** The home screen in front: the draft then reaches a phone the user is using elsewhere. */
    private fun home() {
        assertTrue(instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME))
        SystemClock.sleep(1000)
    }

    /** The 3-Macaw Speech assistant's hand-over (D60), as `am start` sends it; no spaces, since the shell splits on them. */
    private fun voiceCommand(text: String) {
        shell("am start -a ${ThreeStoveAgentPlugin.VOICE_COMMAND_ACTION} -n ${context.packageName}/.ui.VoiceCommandActivity --es android.intent.extra.TEXT $text")
    }

    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
        .bufferedReader().use { it.readText() }

    /** Any window of the floating ball or card, for example of the user's own ball with AutoJs6 attached. */
    private fun ballOnScreen() = shell("dumpsys window windows").contains("3-Stove Agent floating")

    /** The resumed activity lines of the activity manager. */
    private fun resumed() = shell("dumpsys activity activities").lineSequence().filter { it.contains("ResumedActivity") }.joinToString("\n")

    /** kws-zh-6, 0.3 s of silence, mixed-zh-en and 1 s of silence as one 16 kHz mono PCM16 WAV in the cache. */
    private fun recording() = wav("resident-wake", pcm("kws-zh-6") + silence(300) + pcm("mixed-zh-en") + silence(1000))

    /** The first 1.35 s of kws-zh-6: the keyword and the pause after it, without the speech that follows. */
    private fun keywordAlone() = wav("resident-keyword", pcm("kws-zh-6").copyOf(16_000 * 2 * 135 / 100) + silence(1000))

    private fun silence(millis: Int) = ByteArray(16_000 * 2 * millis / 1000)

    private fun pcm(name: String): ByteArray {
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

    private fun wav(name: String, data: ByteArray): File {
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data.size); put("WAVE".toByteArray()); put("fmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(16_000); putInt(32_000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data.size)
        }.array()
        return File(context.cacheDir, "$name.wav").apply { writeBytes(header + data) }
    }

    /** Simulated power for "only while charging" (`set ac 1`, `unplug`, `reset`); the shell user may change it. */
    private fun battery(command: String) { batterySimulated = command != "reset"; shell("dumpsys battery $command") }

    /** 3-Macaw Speech's active recording in the audio service: `none`, `recording` or `silenced`. */
    private fun macawRecording(): String {
        // The active recordings come first; the event history follows ("Audio event log", "Events log" on API 35).
        val tail = shell("dumpsys audio").substringAfter("RecordActivityMonitor dump time", "")
        val active = tail.substring(0, Regex("(?i)events? log").find(tail)?.range?.first ?: tail.length)
        val line = active.lineSequence().firstOrNull { it.contains(MacawSpeech.PACKAGE) } ?: return "none"
        val silenced = line.contains("silenced:true") || (line.contains("silenced") && !line.contains("not silenced") && !line.contains("silenced:false"))
        return if (silenced) "silenced" else "recording"
    }

    private fun listenerRunning() = shell("dumpsys activity services ${context.packageName}/.speech.ResidentListenerService").contains("app=ProcessRecord")

    private fun subText(): String? = active(ResidentNotifications.LISTENING_ID)?.extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

    /** Settings screens other than the scenario's, opened from a notification. */
    private fun finishSettings() {
        val own = mutableListOf<Activity>(); screen?.onActivity { own += it }
        instrumentation.runOnMainSync {
            listOf(Stage.RESUMED, Stage.STARTED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED).flatMap {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it)
            }.filterIsInstance<SettingsActivity>().filter { it !in own }.forEach { it.finish() }
        }
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

    private fun waitFor(label: String, timeoutMs: Long, check: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) { if (check()) return; SystemClock.sleep(150) }
        fail(label)
    }

    private companion object {
        const val TAG = "StoveResidentTest"
        const val KEYWORD = "朱丽楠"
        const val EARLIER_DRAFT = "Earlier_voice_draft"
        /** 3-Macaw Speech's log lines of roadmap D57, its voice input and its assistant sheet (`SpeechSessionBinders`, `VoiceInputActivity`, `AssistantActivity`). */
        val YIELDS = Regex("yields the microphone to a takeover")
        val RESUMES = Regex("resumes after the takeover")
        val VOICE_RESULT = Regex("Voice input result (-?\\d+) \\((\\d+) chars\\)")
        val ASSISTANT_RESULT = Regex("Dictation done \\((\\d+) chars")
        const val ASSISTANT_SHEET = "${MacawSpeech.PACKAGE}/.ui.assistant.AssistantActivity"
    }
}
