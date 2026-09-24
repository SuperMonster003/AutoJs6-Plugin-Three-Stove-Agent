package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import android.widget.ScrollView
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import java.util.concurrent.Executors

/** Offline bundled documents. No WebView, script execution or remote resource loading. */
class ReleaseHistoryActivity : HostAppearanceActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val document = intent.getStringExtra("document")
        val titleId = when (document) { "license" -> R.string.settings_license; "notices" -> R.string.settings_notices; else -> R.string.release_history_title }
        setTitle(titleId)
        val column = HistoryViews.column(this).apply { layoutDirection = resources.configuration.layoutDirection }
        HistoryViews.label(column, getString(titleId), true)
        HistoryViews.button(column, R.string.workbench_back, "back") { finish() }
        val content = HistoryViews.label(column, getString(R.string.interaction_loading)).apply { tag = "document" }
        setContentView(ScrollView(this).apply { fitsSystemWindows = true; addView(column) }); tint(column)
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
            runOnUiThread { if (!isFinishing && !isDestroyed) content.text = text?.let {
                if (document == "license") it else markdown(it)
            } ?: getString(R.string.release_history_error) }
        }
    }
    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }
    private fun markdown(text: String): CharSequence = SpannableStringBuilder().apply {
        text.lineSequence().forEach { line ->
            if (line.matches(Regex("[ *-]{3,}"))) return@forEach
            val heading = line.takeWhile { it == '#' }.length
            val start = length
            append(if (heading > 0) line.drop(heading).trimStart() else line.removePrefix("* ").let { if (line.startsWith("* ")) "- $it" else it })
            append('\n')
            if (heading > 0) {
                setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(RelativeSizeSpan(if (heading == 1) 1.35f else 1.1f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
