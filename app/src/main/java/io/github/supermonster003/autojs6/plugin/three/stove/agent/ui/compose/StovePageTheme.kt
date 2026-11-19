package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.AgentColorPolicy
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.AgentPalette

/**
 * The Material 3 theme of the plugin's Compose page (the release history, plugin-center P21.8), mapped from the
 * [AgentPalette] of the View screens so it looks like the settings page that opens it: the readable accent is
 * `primary`, the opaque accent tone is `primaryContainer` (the accent keeps 4.5:1 on it), the page and the top bar
 * take the settings background, and the night mode is the plugin's own (it follows AutoJs6 unless the user chose
 * otherwise), not the system's.
 * zh-CN: 插件 Compose 页面 (发行历史, plugin-center P21.8) 的 M3 主题, 由 View 界面的 [AgentPalette] 映射而来,
 * 与打开它的设置页一致: 可读强调色为 primary, 不透明的强调色色调为 primaryContainer (强调色在其上保持 4.5:1),
 * 页面与顶栏取设置页背景色; 夜间模式取插件自身设置 (默认跟随 AutoJs6), 不取系统设置.
 */
@Composable
internal fun StovePageTheme(palette: AgentPalette, content: @Composable () -> Unit) {
    val base = if (palette.isDark) darkColorScheme() else lightColorScheme()
    val text = Color(palette.text)
    val accent = Color(palette.accent)
    val scheme = base.copy(
        primary = accent,
        onPrimary = Color(palette.onAccent),
        primaryContainer = Color(AgentColorPolicy.blend(palette.background, palette.accent, AgentPalette.TONE_ALPHA / 255.0)),
        onPrimaryContainer = accent,
        background = Color(palette.background),
        onBackground = text,
        surface = Color(palette.background),
        onSurface = text,
        surfaceVariant = Color(palette.surfaceVariant),
        onSurfaceVariant = Color(palette.muted),
        outline = Color(palette.outline),
        outlineVariant = Color(palette.divider),
    )
    val typography = Typography().let { type ->
        type.copy(
            titleLarge = type.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
            titleMedium = type.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
            bodyMedium = type.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = type.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
            labelMedium = type.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
        )
    }
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}

/**
 * A page with the plugin's standard top bar (`AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md`, section 4): the surface
 * color like the toolbar of the settings page, the back arrow, the title with an optional subtitle below it (one line
 * each, ellipsized, direction from the content), and optional actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StoveScreen(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag("stove.topBar"),
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content),
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("stove.topBar.navigation")) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.workbench_back),
                        )
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    scrolledContainerColor = colors.surface,
                    navigationIconContentColor = colors.onSurface,
                    titleContentColor = colors.onSurface,
                    actionIconContentColor = colors.onSurface,
                ),
            )
        },
        content = content,
    )
}
