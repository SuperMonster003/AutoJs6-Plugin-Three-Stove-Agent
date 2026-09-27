package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.*
import java.util.concurrent.Executors

/** Offline bundled documents (version history, license, notices). No WebView, scripts or remote resources. */
class ReleaseHistoryActivity : HostAppearanceActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    internal lateinit var scaffold: Scaffold; private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val document = intent.getStringExtra("document")
        val titleId = when (document) { "license" -> R.string.settings_license; "notices" -> R.string.settings_notices; else -> R.string.release_history_title }
        scaffold = buildScaffold(getString(titleId), contentPadding = ContentPadding.SCREEN)
        val progress = LinearProgressIndicator(this).apply { isIndeterminate = true; kit.applyThemeToControls(this) }
        val content = TextView(this).apply {
            tag = "document"; textSize = Ui.TEXT_BODY; setTextColor(palette.text); setTextIsSelectable(true)
            setLineSpacing(0f, 1.25f); text = getString(R.string.interaction_loading)
            textDirection = View.TEXT_DIRECTION_LOCALE
        }
        scaffold.content.addView(progress, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = kit.dp(Ui.SPACE_LG) })
        scaffold.content.addView(content, LinearLayout.LayoutParams(-1, -2))
        setContentView(scaffold.root)
        val locale = resources.configuration.locales[0]
        worker.execute {
            fun read(path: String): String = assets.open(path).use {
                val bytes = it.readBytes(); require(bytes.size <= 1024 * 1024); bytes.toString(Charsets.UTF_8)
            }
            val text = runCatching { when (document) {
                "license" -> read("legal/LICENSE")
                "notices" -> read("legal/THIRD_PARTY_NOTICES.md")
                else -> ReleaseHistory.load(locale, ::read)
            } }.getOrNull()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                progress.visibility = View.GONE
                if (text == null) {
                    content.visibility = View.GONE
                    scaffold.content.addView(kit.emptyState(getString(R.string.release_history_error), null, R.drawable.ic_warning))
                } else content.text = if (document == "license") text else DocumentText.render(text, palette)
            }
        }
    }
    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }
}
