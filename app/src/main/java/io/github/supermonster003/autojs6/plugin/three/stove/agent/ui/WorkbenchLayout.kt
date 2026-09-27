package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import com.google.android.material.chip.Chip
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*

internal class WorkbenchViews(
    val root: LinearLayout,
    val toolbar: Toolbar,
    val banner: ConnectionBanner,
    val scroll: NestedScrollView,
    val feed: WorkbenchFeed,
    val composer: Composer,
    val jump: Chip,
    val more: View,
)

/**
 * Home: a top bar with the model capsule, history and the overflow menu; the connection banner;
 * the task feed; and the composer docked above the keyboard.
 */
internal object WorkbenchLayout {
    fun create(activity: LauncherActivity, actions: FeedActions, onConnect: () -> Unit, onOpenHost: () -> Unit,
               onPreset: () -> Unit, onVoice: () -> Unit, onSend: () -> Unit, onMore: (View) -> Unit, onJump: () -> Unit): WorkbenchViews {
        val kit = activity.kit
        val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(kit.palette.background) }
        val statusBar = View(activity).apply { setBackgroundColor(kit.palette.background) }
        root.addView(statusBar, LinearLayout.LayoutParams(-1, 0))

        val toolbar = activity.createToolbar("", showBack = false)
        activity.supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setContentInsetsRelative(kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_XS))
        // End-gravity custom views are laid out from the end: the first one added sits outermost.
        val more = kit.iconButton(R.drawable.ic_more, kit.string(R.string.ui_more), "more") {}.apply { id = R.id.workbench_more }
        more.setOnClickListener { onMore(more) }
        toolbar.addView(more, Toolbar.LayoutParams(kit.dp(Ui.TOUCH_TARGET), kit.dp(Ui.TOUCH_TARGET), Gravity.END or Gravity.CENTER_VERTICAL))
        toolbar.addView(kit.iconButton(R.drawable.ic_history, kit.string(R.string.history_title), "home-history") {
            activity.startActivity(android.content.Intent(activity, HistoryActivity::class.java))
        }.apply { id = R.id.workbench_history }, Toolbar.LayoutParams(kit.dp(Ui.TOUCH_TARGET), kit.dp(Ui.TOUCH_TARGET), Gravity.END or Gravity.CENTER_VERTICAL))
        toolbar.addView(activity.models.capsule.view, Toolbar.LayoutParams(-2, -2, Gravity.START or Gravity.CENTER_VERTICAL))
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))

        val banner = ConnectionBanner(kit, onConnect, onOpenHost)
        root.addView(bounded(activity, banner.view, Ui.SPACE_LG, 0, Ui.SPACE_SM), LinearLayout.LayoutParams(-1, -2))

        val feed = WorkbenchFeed(kit, actions)
        val scroll = NestedScrollView(activity).apply {
            isFillViewport = true; clipToPadding = false
            addView(bounded(activity, feed.view, Ui.SPACE_LG, Ui.SPACE_XS, Ui.SPACE_LG), android.view.ViewGroup.LayoutParams(-1, -2))
        }
        val jump = kit.chip(kit.string(R.string.workbench_jump_latest), "jump-latest", icon = R.drawable.ic_expand) { onJump() }.apply {
            visibility = View.GONE; elevation = kit.dp(4).toFloat()
        }
        root.addView(FrameLayout(activity).apply {
            addView(scroll, FrameLayout.LayoutParams(-1, -1))
            addView(jump, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = kit.dp(Ui.SPACE_SM) })
        }, LinearLayout.LayoutParams(-1, 0, 1f))

        val composer = Composer(kit, onPreset, onVoice, onSend)
        root.addView(bounded(activity, composer.view, Ui.SPACE_MD, Ui.SPACE_XS, Ui.SPACE_MD), LinearLayout.LayoutParams(-1, -2))
        activity.applySystemBarInsets(root, statusBar)
        return WorkbenchViews(root, toolbar, banner, scroll, feed, composer, jump, more)
    }

    /** Centers [child] within the 840dp content width. Vertical space is a margin, so a hidden child takes none. */
    private fun bounded(activity: LauncherActivity, child: View, horizontal: Int, top: Int, bottom: Int): FrameLayout {
        val kit = activity.kit
        val column = BoundedColumn(activity).apply {
            setPaddingRelative(kit.dp(horizontal), 0, kit.dp(horizontal), 0)
            addView(child, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(top); bottomMargin = kit.dp(bottom) })
        }
        return FrameLayout(activity).apply { addView(column, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_HORIZONTAL)) }
    }
}
