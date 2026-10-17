package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import org.autojs.plugin.speech.api.ISpeechPlugin
import org.autojs.plugin.speech.api.SpeechCapabilityKeys
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechPluginActions
import org.autojs.plugin.speech.api.SpeechPluginIds
import org.autojs.plugin.speech.api.SpeechResultKeys
import java.util.concurrent.Executors

/**
 * The speech service of 3-Macaw Speech (roadmap D74). It is protected only by the host's signature permission
 * `org.autojs.permission.PLUGIN`, so binding fails with a [SecurityException] while AutoJs6 is not installed.
 */
internal object MacawSpeech {
    const val PACKAGE = SpeechPluginIds.OFFICIAL_PACKAGE_NAME
    private const val PROBE_TIMEOUT_MILLIS = 5000L
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "three-stove-agent-macaw-probe").apply { isDaemon = true } }

    fun intent(): Intent = Intent(SpeechPluginActions.SPEECH).addCategory(SpeechPluginActions.SERVICE_CATEGORY).setPackage(PACKAGE)

    fun installed(context: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= 33) context.packageManager.getPackageInfo(PACKAGE, PackageManager.PackageInfoFlags.of(0))
        else @Suppress("DEPRECATION") context.packageManager.getPackageInfo(PACKAGE, 0)
    }.isSuccess

    /** API 34+: lets 3-Macaw Speech open its transparent activity that starts the microphone service while Stove is visible. */
    fun bindFlags(): Int = Context.BIND_AUTO_CREATE or if (Build.VERSION.SDK_INT >= 34) Context.BIND_ALLOW_ACTIVITY_STARTS else 0

    fun launchIntent(context: Context): Intent? = context.packageManager.getLaunchIntentForPackage(PACKAGE)

    /**
     * Reads capabilities and installed models with a short binding (the settings screen on every resume). [done] runs on
     * the main thread once, after [PROBE_TIMEOUT_MILLIS] at the latest; the returned action cancels it.
     */
    fun probe(context: Context, done: (MacawProbe) -> Unit): () -> Unit {
        val app = context.applicationContext
        val main = Handler(Looper.getMainLooper())
        var finished = false
        var bound = false
        lateinit var connection: ServiceConnection
        fun finish(result: MacawProbe) {
            if (finished) return
            finished = true; main.removeCallbacksAndMessages(null)
            if (bound) runCatching { app.unbindService(connection) }
            done(result)
        }
        if (!installed(app)) { main.post { finish(MacawProbe(installed = false)) }; return { finished = true } }
        connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val plugin = ISpeechPlugin.Stub.asInterface(service) ?: return
                worker.execute {
                    val result = runCatching { read(plugin) }.getOrElse { MacawProbe(installed = true) }
                    main.post { finish(result) }
                }
            }
            override fun onServiceDisconnected(name: ComponentName?) = Unit
            override fun onBindingDied(name: ComponentName?) { main.post { finish(MacawProbe(installed = true)) } }
            override fun onNullBinding(name: ComponentName?) { main.post { finish(MacawProbe(installed = true)) } }
        }
        bound = runCatching { app.bindService(intent(), connection, Context.BIND_AUTO_CREATE) }.getOrDefault(false)
        if (!bound) main.post { finish(MacawProbe(installed = true)) }
        else main.postDelayed({ finish(MacawProbe(installed = true)) }, PROBE_TIMEOUT_MILLIS)
        return { main.post { if (!finished) { finished = true; main.removeCallbacksAndMessages(null); if (bound) runCatching { app.unbindService(connection) } } } }
    }

    /** One probe over a bound plugin; Binder calls, never on the main thread. */
    fun read(plugin: ISpeechPlugin): MacawProbe {
        val capabilities = plugin.capabilities ?: Bundle()
        fun installedModels(kind: String) = plugin.listModels(Bundle().apply {
            putInt(SpeechContract.KEY_CONTRACT_VERSION, SpeechContract.CONTRACT_VERSION)
            putString(SpeechOptionKeys.KIND, kind); putBoolean(SpeechOptionKeys.INSTALLED, true)
        })?.let { answer ->
            @Suppress("DEPRECATION") answer.getParcelableArrayList<Bundle>(SpeechResultKeys.MODELS)?.size
        } ?: 0
        return MacawProbe(
            installed = true, bound = true,
            contractVersion = capabilities.getInt(SpeechCapabilityKeys.CONTRACT_VERSION, -1).takeIf { it >= 0 },
            features = capabilities.getStringArray(SpeechCapabilityKeys.FEATURES)?.toSet().orEmpty(),
            microphoneGranted = capabilities.getBoolean(SpeechCapabilityKeys.MICROPHONE_GRANTED, false),
            wakeModels = installedModels(SpeechContract.KIND_KWS),
            recognitionModels = installedModels(SpeechContract.KIND_ASR),
        )
    }
}
