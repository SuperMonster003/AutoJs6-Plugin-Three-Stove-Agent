package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AgentUiAppearanceTest {
    @Test fun primaryButtonRemainsReadableWhenRestyledAndEnabledStateChanges() {
        ActivityScenario.launch(LauncherActivity::class.java).use { scenario ->
            lateinit var button: Button
            scenario.onActivity { activity ->
                val content = LinearLayout(activity)
                button = AgentUi.action(content, android.R.string.ok, "contrast-check", primary = true) {}
                activity.addContentView(content, android.view.ViewGroup.LayoutParams(-1, -2))
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                for (enabled in listOf(true, false, true)) {
                    button.isEnabled = enabled
                    repeat(3) {
                        AgentUi.style(button, activity.appearance)
                        val drawable = button.background
                        drawable.setBounds(0, 0, 128, 64)
                        val bitmap = Bitmap.createBitmap(128, 64, Bitmap.Config.ARGB_8888)
                        try {
                            drawable.draw(Canvas(bitmap))
                            val fill = Color.luminance(bitmap.getPixel(64, 32))
                            val text = Color.luminance(button.currentTextColor)
                            val contrast = (maxOf(fill, text) + .05) / (minOf(fill, text) + .05)
                            assertTrue("Primary button contrast after restyle, enabled=$enabled: $contrast", contrast >= 4.5)
                        } finally { bitmap.recycle() }
                    }
                }
            }
        }
    }
}
