package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.AgentPalette
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.Kit
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.ThemedPopupMenu
import org.junit.Assert.*
import org.junit.Test

class ThemedPopupMenuTest {
    private fun checkbox(root: View): CompoundButton? {
        if (root is CompoundButton) return root
        if (root is ViewGroup) for (index in 0 until root.childCount) checkbox(root.getChildAt(index))?.let { return it }
        return null
    }

    @Test fun checkedMenuItemUsesRuntimeAccentForLightAndDarkPalettes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            for (dark in listOf(false, true)) {
                var popup: ThemedPopupMenu? = null
                var accent = 0
                var clicked = false
                scenario.onActivity { activity ->
                    val palette = AgentPalette.resolve(activity, HostAppearance("en", dark, 0xff7b1fa2.toInt(), 0xff7b1fa2.toInt()))
                    accent = palette.accent
                    // A full-screen anchor leaves no vertical space for popup rows on some APIs.
                    popup = ThemedPopupMenu(Kit(activity, palette), activity.scaffold.toolbar).apply {
                        menu.add(0, R.id.workbench_floating, 0, R.string.settings_floating).setCheckable(true).isChecked = true
                        setOnMenuItemClickListener { clicked = it.itemId == R.id.workbench_floating; true }
                        show()
                    }
                }
                instrumentation.waitForIdleSync()
                val deadline = SystemClock.elapsedRealtime() + 5000
                var rowsReady = false
                while (!rowsReady && SystemClock.elapsedRealtime() < deadline) {
                    scenario.onActivity { rowsReady = popup!!.listView?.let(::checkbox) != null }
                    if (!rowsReady) SystemClock.sleep(16)
                }
                scenario.onActivity {
                    val control = checkbox(checkNotNull(popup!!.listView))
                    assertNotNull("Menu contains a checkbox after layout (children=${popup!!.listView!!.childCount})", control)
                    assertTrue(control!!.isChecked)
                    val checked = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked)
                    assertEquals(accent, control.buttonTintList!!.getColorForState(checked, 0))
                    popup!!.menu.performIdentifierAction(R.id.workbench_floating, 0)
                    assertTrue(clicked)
                    popup!!.dismiss()
                }
            }
        }
    }
}
