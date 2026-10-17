package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
 */
class ResidentListenerService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "three-stove-agent-resident").apply { isDaemon = true } }
    private lateinit var runtime: AgentRuntime
    private lateinit var store: ResidentSettingsStore
    private var connection: ServiceConnection? = null
    private var plugin: ISpeechPlugin? = null
    private var session: ISpeechSession? = null
    /** Bumped for every wake session and on stop, so that late events of an earlier session are dropped. */
    private var generation = 0
    private var keyword = ResidentKeywords.DEFAULT.first()
    private var state = ResidentNotifications.State.CONNECTING
    private var sessionError: String? = null
    private var stopping = false
    private val death = IBinder.DeathRecipient { main.post { stop(ResidentStop.UNAVAILABLE) } }

    override fun onCreate() { super.onCreate(); runtime = AgentRuntime.get(this); store = ResidentSettingsStore(this) }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService needs startForeground first, even when the service stops right away.
        if (!promote()) { stopSelf(); return START_NOT_STICKY }
        when (intent?.action) {
            ACTION_TURN_OFF -> { ResidentListener.setEnabled(this, false); ResidentNotifications.cancelReminders(this); stop(null) }
            ACTION_RESTART -> if (plugin != null) open() else start()
            else -> start()
        }
        // A restart by the system would come from the background, where 3-Macaw Speech cannot open the microphone (D76).
        return START_NOT_STICKY
    }

    private fun promote(): Boolean = runCatching {
        val notification = ResidentNotifications.listening(this, state, keyword)
        if (Build.VERSION.SDK_INT >= 34) startForeground(ResidentNotifications.LISTENING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(ResidentNotifications.LISTENING_ID, notification)
    }.isSuccess

    private fun start() {
        if (connection != null || stopping) return
        if (!ResidentListener.enabled(this)) { stop(null); return }
        ResidentListener.others(this).firstOrNull()?.let { stop(ResidentStop.CONFLICT, it.label); return }
        ResidentNotifications.cancelReminders(this)
        val binding = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val bound = service?.let(ISpeechPlugin.Stub::asInterface) ?: return stop(ResidentStop.UNAVAILABLE)
                runCatching { service.linkToDeath(death, 0) }
                plugin = bound; open()
            }
            override fun onServiceDisconnected(name: ComponentName?) = stop(ResidentStop.UNAVAILABLE)
            override fun onBindingDied(name: ComponentName?) = stop(ResidentStop.UNAVAILABLE)
            override fun onNullBinding(name: ComponentName?) = stop(ResidentStop.UNAVAILABLE)
        }
        connection = binding
        if (!runCatching { bindService(MacawSpeech.intent(), binding, MacawSpeech.bindFlags()) }.getOrDefault(false)) stop(ResidentStop.UNAVAILABLE)
    }

    /** Opens (or reopens, after a settings change) the wake session with the stored keywords. */
    private fun open() {
        val bound = plugin ?: return
        val gen = ++generation
        val previous = session; session = null
        runCatching { previous?.cancel() }
        val settings = store.read()
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

    private fun notifyState() { if (!stopping) ResidentNotifications.updateListening(this, state, keyword) }

    private fun stop(reason: ResidentStop?, other: String? = null) {
        if (stopping) return
        stopping = true; generation++
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
        override fun onWarning(warning: Bundle?) { Log.i(TAG, "Wake warning: ${warning?.getString(SpeechContract.KEY_ERROR_CODE)}") }
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
        const val ACTION_TURN_OFF = "io.github.supermonster003.autojs6.plugin.three.stove.agent.RESIDENT_TURN_OFF"
        const val ACTION_RESTART = "io.github.supermonster003.autojs6.plugin.three.stove.agent.RESIDENT_RESTART"
    }
}
