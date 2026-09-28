package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.HostAppearanceActivity
import kotlin.math.max

internal class Scaffold(
    val root: LinearLayout,
    val toolbar: Toolbar,
    val scroll: NestedScrollView?,
    val content: LinearLayout,
)

internal class ContentPadding(val horizontalDp: Int, val topDp: Int, val bottomDp: Int) {
    companion object {
        /** Rows manage their own horizontal padding (settings-style screens). */
        val NONE = ContentPadding(0, Ui.SPACE_SM, Ui.SECTION_GAP)
        /** Card lists and prose screens share the standard screen margin. */
        val SCREEN = ContentPadding(Ui.SCREEN_MARGIN, Ui.SPACE_LG, Ui.SECTION_GAP)
    }
}

/** A FrameLayout child measured no wider than [Ui.MAX_CONTENT_WIDTH], centered by its parent. */
internal class BoundedColumn(context: Context) : LinearLayout(context) {
    private val maximum = (Ui.MAX_CONTENT_WIDTH * context.resources.displayMetrics.density).toInt()
    init { orientation = VERTICAL }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        val bounded = if (size > maximum) MeasureSpec.makeMeasureSpec(maximum, MeasureSpec.getMode(widthMeasureSpec)) else widthMeasureSpec
        super.onMeasure(bounded, heightMeasureSpec)
    }
}

/** The top of every screen: the background column, the status-bar spacer and the toolbar, before any content. */
internal class ScaffoldShell(val root: LinearLayout, val statusBar: View, val toolbar: Toolbar)

/**
 * Builds the shell shared by [buildScaffold] and the home screen, which adds its own banner, feed and docked
 * composer below the toolbar. The caller applies [applySystemBarInsets] once its content is attached.
 */
internal fun HostAppearanceActivity.scaffoldShell(title: CharSequence, showBack: Boolean = true, subtitle: CharSequence? = null): ScaffoldShell {
    val kit = kit
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(kit.palette.background)
    }
    val statusBar = View(this).apply { setBackgroundColor(kit.palette.background) }
    root.addView(statusBar, LinearLayout.LayoutParams(-1, 0))
    val toolbar = createToolbar(title, showBack, subtitle)
    root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
    return ScaffoldShell(root, statusBar, toolbar)
}

/** Centers [child] within the 840 dp content width; vertical space is a margin, so a hidden child takes none. */
internal fun HostAppearanceActivity.boundedRow(child: View, horizontalDp: Int, topDp: Int, bottomDp: Int): FrameLayout {
    val kit = kit
    val column = BoundedColumn(this).apply {
        setPaddingRelative(kit.dp(horizontalDp), 0, kit.dp(horizontalDp), 0)
        addView(child, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(topDp); bottomMargin = kit.dp(bottomDp) })
    }
    return FrameLayout(this).apply { addView(column, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_HORIZONTAL)) }
}

/**
 * Shared screen shell: status-bar spacer, toolbar and a scrolling content column with edge-to-edge
 * insets (system bars, cutout and keyboard). The toolbar is the support action bar so screens can
 * use the options menu; navigation always goes through [HostAppearanceActivity.navigateBack].
 */
internal fun HostAppearanceActivity.buildScaffold(
    title: CharSequence,
    showBack: Boolean = true,
    subtitle: CharSequence? = null,
    scrollable: Boolean = true,
    contentPadding: ContentPadding = ContentPadding.NONE,
): Scaffold {
    val kit = kit
    val shell = scaffoldShell(title, showBack, subtitle)
    val root = shell.root
    val content = BoundedColumn(this).apply {
        setPaddingRelative(kit.dp(contentPadding.horizontalDp), kit.dp(contentPadding.topDp),
            kit.dp(contentPadding.horizontalDp), kit.dp(contentPadding.bottomDp))
    }
    val centered = FrameLayout(this).apply { addView(content, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_HORIZONTAL)) }
    var scroll: NestedScrollView? = null
    if (scrollable) {
        scroll = NestedScrollView(this).apply {
            isFillViewport = true
            addView(centered, ViewGroup.LayoutParams(-1, -2))
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    } else {
        root.addView(centered, LinearLayout.LayoutParams(-1, 0, 1f))
    }
    applySystemBarInsets(root, shell.statusBar)
    return Scaffold(root, shell.toolbar, scroll, content)
}

internal fun HostAppearanceActivity.createToolbar(title: CharSequence, showBack: Boolean, subtitle: CharSequence? = null): Toolbar =
    Toolbar(this).apply {
        val kit = kit
        this.title = title
        this.subtitle = subtitle
        setBackgroundColor(kit.palette.background)
        setTitleTextAppearance(this@createToolbar, R.style.AppToolbarTitle)
        setSubtitleTextAppearance(this@createToolbar, R.style.AppToolbarSubtitle)
        setTitleTextColor(kit.palette.text)
        setSubtitleTextColor(kit.palette.muted)
        minimumHeight = actionBarHeight()
        setContentInsetsRelative(kit.dp(16), kit.dp(8))
        setSupportActionBar(this)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        if (showBack) {
            navigationIcon = kit.tintedDrawable(R.drawable.ic_back, kit.palette.text)
            setNavigationContentDescription(R.string.workbench_back)
            setNavigationOnClickListener { navigateBack() }
        }
        overflowIcon = overflowIcon?.let { kit.tinted(it, kit.palette.text) }
    }

private fun Activity.actionBarHeight(): Int {
    val value = TypedValue()
    return if (theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, value, true) && value.type == TypedValue.TYPE_DIMENSION)
        TypedValue.complexToDimensionPixelSize(value.data, resources.displayMetrics)
    else (56 * resources.displayMetrics.density).toInt()
}

internal data class SystemBarPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Edge-to-edge: pads [root] for system bars, cutouts and the keyboard; the spacer takes the status bar. */
internal fun Activity.applySystemBarInsets(root: View, statusBar: View? = null) {
    val base = SystemBarPadding(root.paddingLeft, root.paddingTop, root.paddingRight, root.paddingBottom)
    applySystemBarInsets(root) { bars ->
        root.setPadding(base.left + bars.left, base.top + if (statusBar == null) bars.top else 0,
            base.right + bars.right, base.bottom + bars.bottom)
        statusBar?.layoutParams?.let { params ->
            if (params.height != bars.top) { params.height = bars.top; statusBar.layoutParams = params }
        }
    }
}

@Suppress("DEPRECATION")
internal fun Activity.applySystemBarInsets(root: View, apply: (SystemBarPadding) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(false)
    } else {
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
    root.setOnApplyWindowInsetsListener { _, insets ->
        val bars = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val system = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsets.Type.ime())
            SystemBarPadding(system.left, system.top, system.right, max(system.bottom, keyboard.bottom))
        } else {
            SystemBarPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        }
        apply(bars)
        insets
    }
    root.requestApplyInsets()
    root.post(root::requestApplyInsets)
}
