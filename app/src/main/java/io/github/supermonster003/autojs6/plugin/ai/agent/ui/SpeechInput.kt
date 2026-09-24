package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.speech.RecognizerIntent
import io.github.supermonster003.autojs6.plugin.ai.agent.model.AgentJson

internal object SpeechInput {
    const val REQUEST = 12
    fun intent(context: Context) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, context.resources.configuration.locales[0].toLanguageTag())
    fun available(context: Context) = intent(context).resolveActivity(context.packageManager) != null
    fun result(data: Intent?): String? = runCatching {
        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { AgentJson.truncate(it, 4096) }
    }.getOrNull()
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
