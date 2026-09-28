package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.util.TypedValue
import android.view.View
import androidx.test.core.app.ActivityScenario
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.Ui
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The host connection banner on a 360 dp wide screen at twice the text size (a Redmi Note 12 or an
 * Xperia XZ1 Compact with the largest font): its two actions stack instead of squeezing the second
 * button into one character per line.
 */
class ConnectionBannerTest {
    @Test fun bannerActionsStackInsteadOfSqueezingAtLargeText() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val kit = activity.kit
                val banner = ConnectionBanner(kit, {}, {})
                banner.render(HostPresence.READY, HostPackageSnapshot(true, 5298, "6.8.0"), LinkStage.WAITING, 5298)
                for (button in listOf(banner.openHost, banner.connect)) button.setTextSize(TypedValue.COMPLEX_UNIT_PX, button.textSize * 2)
                banner.message.setTextSize(TypedValue.COMPLEX_UNIT_PX, banner.message.textSize * 2)
                val width = kit.dp(360 - 2 * Ui.SCREEN_MARGIN)
                for (pass in 0 until 2) {
                    banner.view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                    banner.view.layout(0, 0, width, banner.view.measuredHeight)
                }
                for (button in listOf(banner.openHost, banner.connect)) {
                    // A long label may wrap once on its own row; it must never be squeezed into a few characters per line.
                    assertTrue("${button.text} wraps ${button.lineCount} times", button.lineCount <= 2)
                    val layout = button.layout
                    for (line in 0 until layout.lineCount - 1) {
                        assertTrue("${button.text} line $line is squeezed", layout.getLineEnd(line) - layout.getLineStart(line) >= 6)
                    }
                    assertTrue("${button.text} inside the banner", button.right <= width && button.left >= 0)
                }
                assertTrue("actions stacked", banner.connect.top >= banner.openHost.bottom)
                assertEquals("open host label on one line", 1, banner.openHost.lineCount)
            }
        }
    }
}
