package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView

/**
 * Keeps a growing list pinned to its end while the reader stays there (roadmap P16): new content
 * scrolls into view; a reader who scrolls up keeps their place; scrolling back to the end resumes
 * following. Works for the plain [ScrollView] of the floating window and the [NestedScrollView] of
 * the screens. Programmatic scrolls never change the following state.
 */
internal class AutoScroll(private val scroll: ViewGroup) {
    var following = true
        private set
    private var programmatic = false

    init {
        when (scroll) {
            is NestedScrollView -> scroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ -> changed() })
            else -> scroll.setOnScrollChangeListener { _, _, _, _, _ -> changed() }
        }
    }

    private fun changed() { if (!programmatic) following = atEnd() }

    /** True when the last content pixel is within a few pixels of the viewport bottom. */
    fun atEnd(): Boolean {
        val end = contentEnd()
        return end <= scroll.height || scroll.scrollY + scroll.height >= end - SLOP
    }

    /** Call after the content changed: scrolls to the end only while the reader was already there. */
    fun contentChanged() {
        if (!following) return
        scroll.post {
            if (!following) return@post
            programmatic = true
            try { scroll.scrollTo(0, maxOf(0, contentEnd() - scroll.height)) } finally { programmatic = false }
        }
    }

    private fun contentEnd(): Int {
        val child: View = scroll.getChildAt(0) ?: return 0
        return child.bottom + scroll.paddingBottom
    }

    companion object { private const val SLOP = 8 }
}
