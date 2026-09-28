package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.os.SystemClock
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

/** A real attached ScrollView: following resumes only when the reader returns to the end (roadmap P16). */
class AutoScrollTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun waitFor(label: String, check: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + 5000
        while (SystemClock.elapsedRealtime() < end) { if (check()) return; SystemClock.sleep(30) }
        fail(label)
    }

    @Test fun followsNewContentOnlyWhileTheReaderStaysAtTheEnd() {
        ActivityScenario.launch(BackdropActivity::class.java).use { scenario ->
            lateinit var scroll: ScrollView; lateinit var list: LinearLayout; lateinit var follow: AutoScroll
            fun rows() = list.childCount
            fun addRows(count: Int) = repeat(count) { list.addView(View(list.context), LinearLayout.LayoutParams(-1, 120)) }
            fun end() = list.height - scroll.height
            scenario.onActivity { activity ->
                scroll = ScrollView(activity); list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                scroll.addView(list); addRows(30)
                activity.setContentView(scroll, LinearLayout.LayoutParams(-1, 600))
                follow = AutoScroll(scroll)
            }
            waitFor("Laid out") { var ready = false; scenario.onActivity { ready = scroll.isLaidOut && list.height > scroll.height }; ready }
            scenario.onActivity { assertTrue("Following by default", follow.following); follow.contentChanged() }
            waitFor("Scrolled to the end") { var ok = false; scenario.onActivity { ok = scroll.scrollY == end() }; ok }
            scenario.onActivity { assertTrue(follow.following); scroll.scrollTo(0, 0) } // A reader scrolling up keeps their place.
            scenario.onActivity { assertFalse("Scrolling up stops following", follow.following); addRows(5); follow.contentChanged() }
            instrumentation.waitForIdleSync()
            scenario.onActivity { assertEquals("New rows do not move the reader", 0, scroll.scrollY); assertEquals(35, rows()) }
            scenario.onActivity { scroll.scrollTo(0, end()) } // Back at the end: following resumes.
            scenario.onActivity { assertTrue("Reaching the end resumes following", follow.following); addRows(5); follow.contentChanged() }
            waitFor("New rows scroll into view") { var ok = false; scenario.onActivity { ok = scroll.scrollY == end() && rows() == 40 }; ok }
        }
    }
}
