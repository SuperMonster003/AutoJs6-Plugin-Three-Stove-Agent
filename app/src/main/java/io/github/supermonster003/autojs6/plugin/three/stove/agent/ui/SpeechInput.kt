package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.speech.RecognizerIntent
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.AgentJson

internal object SpeechInput {
    const val REQUEST = 12
    /** 3-Macaw Speech answers RECOGNIZE_SPEECH with an offline bottom sheet; its own setting disables that component. */
    const val MACAW_PACKAGE = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
    /** Instrumentation only: stands in for resolving the 3-Macaw Speech sheet. */
    @Volatile internal var macawOverride: Boolean? = null
    private fun recognition(context: Context) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, context.resources.configuration.locales[0].toLanguageTag())
    /** Pinned to 3-Macaw Speech while preferred and resolvable, so several recognizers no longer bring up the system chooser. */
    fun intent(context: Context): Intent = recognition(context).apply {
        target(SpeechPreferences(context).preferMacaw, macawAvailable(context))?.let(::setPackage)
    }
    fun available(context: Context) = recognition(context).resolveActivity(context.packageManager) != null
    fun macawAvailable(context: Context) = macawOverride
        ?: (recognition(context).setPackage(MACAW_PACKAGE).resolveActivity(context.packageManager) != null)
    /** The package to pin, or null to let the system choose (maintainer decision 2026-10-17: prefer 3-Macaw Speech by default). */
    fun target(preferMacaw: Boolean, macawAvailable: Boolean) = MACAW_PACKAGE.takeIf { preferMacaw && macawAvailable }
    fun result(data: Intent?): String? = runCatching {
        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { AgentJson.truncate(it, 4096) }
    }.getOrNull()
}

/** UI-process preference, outside the agent settings: only the microphone in this process reads it. */
internal class SpeechPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    var preferMacaw: Boolean
        get() = preferences.getBoolean(PREFER_MACAW, true)
        set(value) { preferences.edit().putBoolean(PREFER_MACAW, value).apply() }
    companion object { const val NAME = "speech"; const val PREFER_MACAW = "preferMacaw" }
}

/** Private activity result bridge for the overlay. No microphone permission and no automatic send. */
class VoiceInputActivity : HostAppearanceActivity() {
    override val dialogTheme = true
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        val recognition = SpeechInput.intent(this)
        intent.getStringExtra("language")?.takeIf { it.length in 2..80 && it.matches(Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*")) }
            ?.let { recognition.putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
        if (savedInstanceState == null) runCatching { startActivityForResult(recognition, SpeechInput.REQUEST) }
            .onFailure { finish() }
    }
    @Deprecated("Platform speech result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SpeechInput.REQUEST) {
            @Suppress("DEPRECATION")
            val receiver = intent.getParcelableExtra<ResultReceiver>("receiver")
            receiver?.send(resultCode, Bundle().apply {
                if (resultCode == RESULT_OK) SpeechInput.result(data)?.let { putString("text", it) }
            })
            finish()
        }
    }
}
