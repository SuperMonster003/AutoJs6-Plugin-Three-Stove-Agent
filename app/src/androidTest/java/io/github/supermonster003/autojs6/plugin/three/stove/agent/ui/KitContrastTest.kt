package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.button.MaterialButton
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every enabled kit button stays readable (WCAG AA) in light and dark mode, including pale custom colors. */
class KitContrastTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun enabledButtonsKeepReadableTextAcrossModesSeedsAndEnabledToggles() {
        val original = AppearancePreferences.read(context)
        try {
            for ((mode, seed) in listOf("light" to null, "dark" to null, "light" to 0xffeeddcc.toInt(), "dark" to 0xff334455.toInt(), "dark" to 0xffc86b0a.toInt())) {
                AppearancePreferences(language = "en", darkMode = mode, color = seed).save(context)
                ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
                    scenario.onActivity { activity ->
                        val kit = activity.kit; val palette = kit.palette
                        val buttons = listOf("filled" to kit.filledButton("Filled") {}, "tonal" to kit.tonalButton("Tonal") {},
                            "outlined" to kit.outlinedButton("Outlined") {}, "danger" to kit.outlinedButton("Danger", danger = true) {},
                            "text" to kit.textButton("Text") {}, "text-danger" to kit.textButton("Delete", danger = true) {})
                        for ((name, button) in buttons) {
                            for (enabled in listOf(true, false, true)) button.isEnabled = enabled
                            val ratio = contrast(button, palette.surface)
                            assertTrue("$mode/${seed?.let { "#%06X".format(it and 0xffffff) }}/$name contrast $ratio", ratio >= 4.5)
                        }
                    }
                }
            }
        } finally { original.save(context) }
    }

    /** Text color against the button fill composited over the surface it sits on. */
    private fun contrast(button: MaterialButton, surface: Int): Double {
        val state = button.drawableState
        val fill = button.backgroundTintList?.getColorForState(state, 0) ?: 0
        val background = ColorUtils.compositeColors(fill, surface)
        val text = ColorUtils.compositeColors(button.textColors.getColorForState(state, button.currentTextColor), background)
        return AgentColorPolicy.contrastRatio(text, background)
    }
}
