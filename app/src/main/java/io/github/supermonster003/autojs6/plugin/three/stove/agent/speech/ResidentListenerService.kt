package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.AgentRuntime
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.VoiceDraftText
import org.autojs.plugin.speech.api.ISpeechPlugin
import org.autojs.plugin.speech.api.ISpeechSession
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.ISpeechWakeCallback
import org.autojs.plugin.speech.api.SpeechCapabilityKeys
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import java.util.concurrent.Executors

/**
 * Resident listener (roadmap D61, D66, D73-D77): a `specialUse` foreground service in :agent that holds the wake session
 * of 3-Macaw Speech. 3-Macaw Speech records; Stove never does. A hit is followed by one dictation round whose text
 * becomes a reviewable draft, never a task. While the 3-Macaw Speech assistant takes the microphone the session stays
 * open and silent (its D57), which needs nothing here. The component is exported for the D31 convention only; the
 * other apps query it and never start it, and the Manifest keeps it disabled until the user turns it on.
 *
 * The listening time and "only while charging" (D81, D82): at the deadline the service turns the switch off and goes
 * away with its notification; unplugged, it keeps running without a wake session (3-Macaw Speech stops recording) and
 * opens one again when the power comes back. That open happens in the background, where Android may deny 3-Macaw Speech
 * the microphone (API 35 did, API 28 to 33 recorded); it then waits for a visible screen, as after a reboot (D67), and
 * says so.
 */
class ResidentListenerService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "three-stove-agent-resident").apply { isDaemon = true } }
    private lateinit var runtime: AgentRuntime
    private lateinit var store: ResidentSettingsStore
    private var connection: ServiceConnection? = null
    private var plugin: ISpeechPlugin? = null
    private var session: ISpeechSession? = null
    /** A wake session is being opened on the worker thread. */
    private var opening = false
    /** Bumped for every wake session, pause and stop, so that late events of an earlier session are dropped. */
    private var generation = 0
    private var keyword = ResidentKeywords.DEFAULT.first()
    private var deadline: Long? = null
    private var state = ResidentNotifications.State.CONNECTING
    private var sessionError: String? = null
    private var stopping = false
    /** Plugged into power; follows the power broadcasts while the service runs (D81). */
    private var plugged = true
    private var watchingPower = false
    /** The last command came from a visible screen (D74); a power change acts from the background. */
    private var visible = false
    /** When the current wake session was opened from the background (after the power came back), else null. */
    private var openedInBackground: Long? = null
    private val death = IBinder.DeathRecipient { main.post { stop(ResidentStop.UNAVAILABLE) } }
    private val expiry = Runnable { apply() }
    private val power = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            plugged = intent.action == Intent.ACTION_POWER_CONNECTED
            visible = false
            apply()
        }
    }

    override fun onCreate() { super.onCreate(); runtime = AgentRuntime.get(this); store = ResidentSettingsStore(this) }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService needs startForeground first, even when the service stops right away.
        if (!promote()) { stopSelf(); return START_NOT_STICKY }
        // Every command but Turn off comes from a visible screen of this app (D74).
        when (intent?.action) {
            ACTION_TURN_OFF -> { ResidentListener.setEnabled(this, false); ResidentNotifications.cancelReminders(this); stop(null) }
            ACTION_RESTART -> { visible = true; if (plugin != null) apply(reopen = true) else start() }
            else -> { visible = true; start() }
        }
        // A restart by the system would come from the background, where 3-Macaw Speech cannot open the microphone (D76).
        return START_NOT_STICKY
    }

    private fun promote(): Boolean = runCatching {
        val notification = ResidentNotifications.listening(this, state, keyword, deadline)
        if (Build.VERSION.SDK_INT >= 34) startForeground(ResidentNotifications.LISTENING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(ResidentNotifications.LISTENING_ID, notification)
    }.isSuccess

    private fun start() {
        if (stopping) return
        // Running already: a visible screen re-applies the rules, which also reopens a session that waited for it.
        if (connection != null) { apply(); return }
        if (!ResidentListener.enabled(this)) { stop(null); return }
        if (store.read().expired(System.currentTimeMillis())) { expire(); return }
        ResidentListener.others(this).firstOrNull()?.let { stop(ResidentStop.CONFLICT, it.label); return }
        ResidentNotifications.cancelReminders(this)
        watchPower()
        val binding = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val bound = service?.let(ISpeechPlugin.Stub::asInterface) ?: return stop(ResidentStop.UNAVAILABLE)
                runCatching { service.linkToDeath(death, 0) }
                plugin = bound; apply()
            }
            override fun onServiceDisconnected(name: ComponentName?) = stop(ResidentStop.UNAVAILABLE)
            override fun onBindingDied(name: ComponentName?) = stop(ResidentStop.UNAVAILABLE)
            override fun onNullBinding(name: ComponentName?) = stop(ResidentStop.UNAVAILABLE)
        }
        connection = binding
        if (!runCatching { bindService(MacawSpeech.intent(), binding, MacawSpeech.bindFlags()) }.getOrDefault(false)) stop(ResidentStop.UNAVAILABLE)
    }

    /** Power broadcasts are implicit: only a registered receiver hears them, so they are watched while the service runs. */
    private fun watchPower() {
        if (watchingPower) return
        val filter = IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED) }
        watchingPower = runCatching {
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(power, filter, RECEIVER_NOT_EXPORTED) else registerReceiver(power, filter)
        }.isSuccess
        plugged = ResidentPower.plugged(this)
    }

    /**
     * Applies the listening time and the power rule (D81, D82) to the bound service: at the deadline the switch turns off,
     * unplugged the wake session pauses, otherwise a session opens unless one runs. [reopen] reopens a running session
     * with new keywords. A session that the background could not give a microphone waits for a visible screen.
     */
    private fun apply(reopen: Boolean = false) {
        if (stopping || plugin == null) return
        val settings = store.read()
        val now = System.currentTimeMillis()
        deadline = settings.deadline
        main.removeCallbacks(expiry)
        // Wall clock: a timer that slept with the phone fires late, and the next power change or visible screen checks again.
        settings.deadline?.let { main.postDelayed(expiry, (it - now).coerceAtLeast(0)) }
        when (ResidentGate.of(settings, now, plugged)) {
            ResidentGate.EXPIRED -> expire()
            ResidentGate.PAUSED -> pause()
            ResidentGate.LISTEN -> if (reopen || (session == null && !opening && (state != ResidentNotifications.State.BLOCKED || visible))) open(settings)
                else notifyState()
        }
    }

    /** Unplugged with "only while charging": no wake session, so 3-Macaw Speech stops recording; the service stays for the power. */
    private fun pause() {
        if (state == ResidentNotifications.State.PAUSED && session == null && !opening) return
        dropSession()
        state = ResidentNotifications.State.PAUSED; notifyState()
    }

    /** A wake session opened from the background got no microphone: wait for a visible screen (D67, D82). */
    private fun block() {
        Log.i(TAG, "Wake session without a microphone in the background; waiting for a visible screen")
        dropSession()
        state = ResidentNotifications.State.BLOCKED; notifyState()
    }

    private fun dropSession() {
        generation++; opening = false
        runCatching { session?.cancel() }; session = null
        runtime.voiceIdle()
    }

    /** The listening time is over (D81): the switch turns off and the ongoing notification goes; nothing reminds. */
    private fun expire() {
        Log.i(TAG, "Resident listener turned off: the listening time is over")
        ResidentListener.setEnabled(this, false)
        ResidentNotifications.cancelReminders(this)
        stop(null)
    }

    /** Opens (or reopens, after a settings change) the wake session with the stored keywords. */
    private fun open(settings: ResidentSettings) {
        val bound = plugin ?: return
        val gen = ++generation
        val previous = session; session = null
        runCatching { previous?.cancel() }
        opening = true; openedInBackground = if (visible) null else SystemClock.elapsedRealtime()
        keyword = settings.keywords.first(); sessionError = null
        state = ResidentNotifications.State.CONNECTING; notifyState()
        worker.execute {
            val result = runCatching {
                val version = bound.capabilities?.getInt(SpeechCapabilityKeys.CONTRACT_VERSION, -1) ?: -1
                check(version in SpeechContract.MIN_CONTRACT_VERSION..SpeechContract.MAX_CONTRACT_VERSION) { SpeechErrorCodes.encode(SpeechErrorCodes.PLUGIN_INCOMPATIBLE) }
                bound.openWakeSession(options(settings), WakeEvents(gen), RoundEvents(gen))
            }
            main.post {
                val opened = result.getOrNull()
                if (gen != generation || stopping) { runCatching { opened?.cancel() }; return@post }
                opening = false
                if (opened != null) session = opened
                else {
                    val code = result.exceptionOrNull()?.let(ResidentStop::code)
                    Log.w(TAG, "Wake session refused: ${code ?: "unknown"}")
                    stop(ResidentStop.of(code))
                }
            }
        }
    }

    private fun options(settings: ResidentSettings) = Bundle().apply {
        putInt(SpeechContract.KEY_CONTRACT_VERSION, SpeechContract.CONTRACT_VERSION)
        putParcelableArrayList(SpeechOptionKeys.KEYWORDS, ArrayList(settings.keywords.map { keyword ->
            Bundle().apply { putString(SpeechOptionKeys.KEYWORD, keyword); settings.sensitivity.threshold?.let { putFloat(SpeechOptionKeys.THRESHOLD, it) } }
        }))
        putBundle(SpeechOptionKeys.THEN_LISTEN, Bundle().apply {
            putBoolean(SpeechOptionKeys.VAD, true)
            putLong(SpeechOptionKeys.SILENCE_END_MILLIS, ResidentRound.SILENCE_END_MILLIS)
            putLong(SpeechOptionKeys.MAX_DURATION_MILLIS, ResidentRound.MAX_DURATION_MILLIS)
            putLong(SpeechOptionKeys.TIMEOUT_MILLIS, ResidentRound.TIMEOUT_MILLIS)
            putBoolean(SpeechOptionKeys.PUNCTUATION, true)
        })
    }

    private fun notifyState() { if (!stopping) ResidentNotifications.updateListening(this, state, keyword, deadline) }

    private fun stop(reason: ResidentStop?, other: String? = null) {
        if (stopping) return
        stopping = true; generation++
        main.removeCallbacks(expiry)
        if (watchingPower) { runCatching { unregisterReceiver(power) }; watchingPower = false }
        reason?.let { Log.i(TAG, "Resident listener stopped: $it") }
        runCatching { session?.cancel() }; session = null
        plugin?.asBinder()?.let { runCatching { it.unlinkToDeath(death, 0) } }; plugin = null
        connection?.let { runCatching { unbindService(it) } }; connection = null
        runtime.voiceIdle()
        if (reason != null) ResidentNotifications.stopped(this, reason, other)
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    override fun onDestroy() { stop(null); worker.shutdown(); super.onDestroy() }

    /** Wake events: hits only; the dictation round after each hit reports on [RoundEvents]. */
    private inner class WakeEvents(private val gen: Int) : ISpeechWakeCallback.Stub() {
        override fun onReady(info: Bundle?) = post { state = ResidentNotifications.State.WAITING; notifyState() }
        override fun onWake(hit: Bundle?) = post {
            state = ResidentNotifications.State.LISTENING; notifyState(); runtime.voiceListening(null)
        }
        override fun onWarning(warning: Bundle?) {
            val code = warning?.getString(SpeechContract.KEY_ERROR_CODE)
            Log.i(TAG, "Wake warning: $code")
            // Refused microphone service or silenced recording. Right after an open from the background it means Android
            // gave 3-Macaw Speech no microphone there, and the session would never hear anything. Later, or after an open
            // from a visible screen, it is kept as before: another app may hold the microphone for a while (a call).
            if (code == SpeechErrorCodes.PERMISSION_DENIED) post {
                val since = openedInBackground
                if (since != null && SystemClock.elapsedRealtime() - since < BACKGROUND_CHECK_MILLIS) block()
            }
        }
        override fun onError(error: Bundle?) { val code = error?.getString(SpeechContract.KEY_ERROR_CODE); post { sessionError = code } }
        override fun onEnd(end: Bundle?) = post { stop(ResidentStop.of(sessionError)) }
        private fun post(block: () -> Unit) { main.post { if (gen == generation && !stopping) block() } }
    }

    /** The dictation round after a hit (D61): its text becomes a draft (D77); a round without text restores the card. */
    private inner class RoundEvents(private val gen: Int) : ISpeechSessionCallback.Stub() {
        /** Finals of the current round; main thread only. */
        private var finals = ""
        override fun onReady(info: Bundle?) = post { finals = "" }
        override fun onPartial(result: Bundle?) {
            val text = result?.getString(SpeechResultKeys.TEXT)
            post { runtime.voiceListening(VoiceDraftText.clean(ResidentRound.join(finals, text))) }
        }
        override fun onFinal(result: Bundle?) { val text = result?.getString(SpeechResultKeys.TEXT); post { finals = ResidentRound.join(finals, text) } }
        override fun onEndpoint() = Unit
        override fun onVoiceActivity(activity: Bundle?) = Unit
        override fun onWarning(warning: Bundle?) = Unit
        // The round's end follows an error; a failed round leaves no draft.
        override fun onError(error: Bundle?) = Unit
        override fun onEnd(end: Bundle?) {
            val reason = end?.getString(SpeechResultKeys.REASON)
            val text = end?.getString(SpeechResultKeys.TEXT)
            post {
                val draft = ResidentRound.draft(reason, text, finals)?.let(VoiceDraftText::clean)
                finals = ""
                state = ResidentNotifications.State.WAITING; notifyState()
                if (draft == null) runtime.voiceIdle() else VoiceDrafts.deliver(this@ResidentListenerService, draft)
            }
        }
        private fun post(block: () -> Unit) { main.post { if (gen == generation && !stopping) block() } }
    }

    companion object {
        private const val TAG = "StoveResidentListener"
        /** 3-Macaw Speech reports a refused or silenced microphone within a few seconds of recording (one second of zeros). */
        private const val BACKGROUND_CHECK_MILLIS = 15_000L
        const val ACTION_TURN_OFF = "io.github.supermonster003.autojs6.plugin.three.stove.agent.RESIDENT_TURN_OFF"
        const val ACTION_RESTART = "io.github.supermonster003.autojs6.plugin.three.stove.agent.RESIDENT_RESTART"
        /** The listening time or the power rule changed; handled like a visible start. */
        const val ACTION_REFRESH = "io.github.supermonster003.autojs6.plugin.three.stove.agent.RESIDENT_REFRESH"
    }
}
