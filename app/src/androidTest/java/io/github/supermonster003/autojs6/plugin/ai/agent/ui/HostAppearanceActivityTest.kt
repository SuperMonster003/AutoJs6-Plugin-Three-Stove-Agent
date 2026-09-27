package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class HostAppearanceActivityTest {
    @Test fun launcherInitializesDecorBeforeApplyingSystemBars() {
        ActivityScenario.launch(LauncherActivity::class.java).use { scenario ->
            repeat(2) {
                scenario.onActivity { activity ->
                    assertNotNull(activity.window.peekDecorView())
                    assertTrue(activity.window.decorView.isAttachedToWindow)
                    if (Build.VERSION.SDK_INT >= 30) assertNotNull(activity.window.insetsController)
                }
                if (it == 0) scenario.recreate()
            }
        }
    }

    @Test fun appCompatBaseKeepsExplicitLocaleAndNightModeAcrossRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = AppearancePreferences.read(context)
        try {
            for ((language, mode, dark) in listOf(Triple("ar", "dark", true), Triple("ja", "light", false))) {
                AppearancePreferences(language = language, darkMode = mode, color = original.color).save(context)
                ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
                    repeat(2) { round ->
                        scenario.onActivity { activity ->
                            assertTrue(activity is AppCompatActivity)
                            val configuration = activity.resources.configuration
                            assertEquals(language, configuration.locales[0].language)
                            assertEquals(dark, configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
                            assertEquals(if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO, activity.delegate.localNightMode)
                            assertEquals(dark, activity.palette.isDark)
                        }
                        if (round == 0) scenario.recreate()
                    }
                }
            }
        } finally { original.save(context) }
    }
}
