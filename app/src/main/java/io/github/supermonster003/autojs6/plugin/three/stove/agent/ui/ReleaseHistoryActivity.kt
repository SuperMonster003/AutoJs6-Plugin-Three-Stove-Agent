package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.compose.ReleaseHistoryScreen
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.compose.StovePageTheme

/**
 * The release history page (plugin-center P21.8, `AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md`): the bundled changelog
 * for the current locale (English when that one is missing) as one group per released version, under the title
 * "Release history" with the plugin name as the subtitle. The only Compose screen of the plugin; it extends the same
 * [HostAppearanceActivity] as the View screens, so language, night mode, theme color and the recreation on an
 * appearance change are theirs, and [StovePageTheme] maps their palette to Material 3.
 * zh-CN: 发行历史页 (plugin-center P21.8, 发行历史页规范): 按当前语言读取内置更新日志 (缺失时回退英文), 每个版本一组,
 * 标题 "发行历史", 子标题为插件名称. 这是插件唯一的 Compose 界面; 它与 View 界面继承同一个 [HostAppearanceActivity],
 * 语言, 夜间模式, 主题色以及外观变化时的重建都与之相同, 由 [StovePageTheme] 把调色板映射为 Material 3.
 */
class ReleaseHistoryActivity : HostAppearanceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge to edge like the View screens: the top bar pads for the status bar, the list for the navigation bar.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        title = getString(R.string.release_history_title)
        val candidates = ReleaseHistoryAssetPolicy.candidatesFor(resources.configuration.locales[0])
        val entries = loadReleaseHistory(candidates) { path ->
            assets.open(path).use { stream -> stream.readBytes().toString(Charsets.UTF_8) }
        }
        val palette = palette
        setContent {
            StovePageTheme(palette) {
                ReleaseHistoryScreen(entries = entries, onBack = ::navigateBack)
            }
        }
    }

}
