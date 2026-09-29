package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.threeStoveAgentPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.update.AppUpdateCoordinator
import io.github.supermonster003.autojs6.plugin.three.stove.agent.update.ReleaseInfoCodec

/** Application identity, developer and licenses; every document opens offline. */
class AboutActivity : HostAppearanceActivity() {
    internal lateinit var scaffold: Scaffold; private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scaffold = buildScaffold(getString(R.string.ui_about))
        val page = scaffold.content
        val info = threeStoveAgentPluginRuntimeInfo()
        with(kit) {
            page.addView(LinearLayout(this@AboutActivity).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                setPaddingRelative(dp(Ui.SCREEN_MARGIN), dp(Ui.SPACE_XXL), dp(Ui.SCREEN_MARGIN), dp(Ui.SPACE_XXL))
                // Keep the rounded frame, with the transparent glyph revealing the surrounding page.
                addView(ImageView(context).apply {
                    setImageResource(R.mipmap.ic_launcher); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    tag = "about-icon"
                    background = roundedFill(android.graphics.Color.TRANSPARENT, Ui.RADIUS_SHEET, palette.outline)
                    clipToOutline = true
                }, LinearLayout.LayoutParams(dp(88), dp(88)))
                addView(text(getString(R.string.app_name), Ui.TEXT_DISPLAY, medium = true).apply {
                    gravity = Gravity.CENTER; setPaddingRelative(0, dp(Ui.SPACE_LG), 0, dp(Ui.SPACE_XS))
                    if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
                }, LinearLayout.LayoutParams(-1, -2))
                addView(text(getString(R.string.plugin_description), Ui.TEXT_BODY, palette.muted).apply { gravity = Gravity.CENTER },
                    LinearLayout.LayoutParams(-1, -2))
            }, LinearLayout.LayoutParams(-1, -2))
            page.addView(hairline(0))
            fun link(title: Int, icon: Int, tag: String, action: () -> Unit) {
                page.addView(settingRow(getString(title), null, icon, tag, onClick = action).view)
            }
            link(R.string.release_history_title, R.drawable.ic_article, "about-history") { document("history") }
            link(R.string.settings_source, R.drawable.ic_code, "about-source") { AppUpdateCoordinator.openPage(this@AboutActivity, ReleaseInfoCodec.SOURCE) }
            link(R.string.about_developer_page, R.drawable.ic_person, "about-developer") { AppUpdateCoordinator.openPage(this@AboutActivity, DEVELOPER_PAGE) }
            link(R.string.settings_license, R.drawable.ic_description, "about-license") { document("license") }
            link(R.string.settings_notices, R.drawable.ic_description, "about-notices") { document("notices") }
            page.addView(hairline(0))
            page.addView(infoBlock(getString(R.string.about_version), getString(R.string.about_version_value, info.versionName, info.versionCode,
                getString(R.string.plugin_version_date)), "about-version"))
            page.addView(infoBlock(getString(R.string.about_developer), getString(R.string.plugin_author), "about-author"))
            page.addView(infoBlock(getString(R.string.settings_license), getString(R.string.about_license_name), "about-license-name"))
        }
        setContentView(scaffold.root)
    }
    private fun document(name: String) {
        startActivity(Intent(this, ReleaseHistoryActivity::class.java).putExtra("document", name))
    }
    companion object {
        const val DEVELOPER_PAGE = "https://github.com/SuperMonster003"
    }
}
