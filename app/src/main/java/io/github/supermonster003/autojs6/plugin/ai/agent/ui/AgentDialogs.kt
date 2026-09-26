package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.widget.CheckedTextView
import android.widget.TextView

/** Keep platform dialog behavior while sharing the app's surfaces and accent. */
internal fun AlertDialog.showStyled(): AlertDialog {
    show()
    val colors = AgentUi.palette(context)
    window?.setBackgroundDrawable(AgentUi.shape(context, colors.surface, 24))
    findViewById<TextView>(android.R.id.message)?.setTextColor(colors.text)
    for (which in listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL)) {
        getButton(which)?.apply {
            isAllCaps = false
            minHeight = AgentUi.dp(context, 48)
            minWidth = AgentUi.dp(context, 48)
            setTextColor(colors.accent)
            background = RippleDrawable(ColorStateList.valueOf(colors.soft), null,
                AgentUi.shape(context, android.graphics.Color.WHITE, 12))
        }
    }
    listView?.apply {
        fun tintItems() {
            for (index in 0 until childCount) (getChildAt(index) as? CheckedTextView)?.apply {
                setTextColor(colors.text)
                checkMarkTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(colors.accent, colors.muted))
                minHeight = AgentUi.dp(context, 48)
            }
        }
        viewTreeObserver.addOnGlobalLayoutListener { tintItems() }
        setOnScrollListener(object : android.widget.AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: android.widget.AbsListView?, scrollState: Int) = Unit
            override fun onScroll(view: android.widget.AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) { tintItems() }
        })
    }
    return this
}

internal fun AlertDialog.Builder.showStyled(): AlertDialog = create().showStyled()
