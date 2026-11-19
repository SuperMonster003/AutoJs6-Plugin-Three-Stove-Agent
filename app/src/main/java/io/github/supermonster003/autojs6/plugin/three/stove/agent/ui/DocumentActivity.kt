package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import java.util.concurrent.Executors

/**
 * Offline bundled legal documents (license, third-party notices). No WebView, scripts or remote resources. The release
 * history has its own Compose page since plugin-center P21.8 ([ReleaseHistoryActivity]); an unknown document key
 * closes this screen instead of showing a default.
 * zh-CN: 离线内置的法律文档 (许可证, 第三方声明), 不用 WebView, 脚本或远程资源. 发行历史自 plugin-center P21.8 起是
 * 单独的 Compose 页面 ([ReleaseHistoryActivity]); 未知的文档键直接关闭本界面, 不显示默认文档.
 */
class DocumentActivity : HostAppearanceActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    internal lateinit var scaffold: Scaffold; private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val document = intent.getStringExtra(EXTRA_DOCUMENT)
        val (titleId, asset) = when (document) {
            LICENSE -> R.string.settings_license to "legal/LICENSE"
            NOTICES -> R.string.settings_notices to "legal/THIRD_PARTY_NOTICES.md"
            else -> { finish(); return }
        }
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
        worker.execute {
            val text = runCatching {
                assets.open(asset).use {
                    val bytes = it.readBytes(); require(bytes.size <= 1024 * 1024); bytes.toString(Charsets.UTF_8)
                }
            }.getOrNull()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                progress.visibility = View.GONE
                if (text == null) {
                    content.visibility = View.GONE
                    scaffold.content.addView(kit.emptyState(getString(R.string.document_load_error), R.drawable.ic_warning))
                } else content.text = if (document == LICENSE) text else DocumentText.render(text, palette)
            }
        }
    }
    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }

    companion object {
        internal const val EXTRA_DOCUMENT = "document"
        internal const val LICENSE = "license"
        internal const val NOTICES = "notices"
        internal fun intent(context: Context, document: String): Intent =
            Intent(context, DocumentActivity::class.java).putExtra(EXTRA_DOCUMENT, document)
    }
}
