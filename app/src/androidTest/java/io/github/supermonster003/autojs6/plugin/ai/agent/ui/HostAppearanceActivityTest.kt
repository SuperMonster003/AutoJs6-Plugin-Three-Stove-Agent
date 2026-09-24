package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.os.Build
import androidx.test.core.app.ActivityScenario
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
}
