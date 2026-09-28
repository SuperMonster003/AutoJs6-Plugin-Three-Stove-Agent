package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View

/**
 * Debug-only opaque screen that instrumentation keeps beneath dialog-themed activities opening in
 * their own task, such as [ConfirmationActivity]. The system destroys a finishing translucent
 * activity only after the activity beneath it has resumed and drawn again; over the emulator's home
 * screen on API 24 that can outlast ActivityScenario's close timeout ("Activity never becomes
 * requested state [DESTROYED]"), whereas this in-process backdrop resumes at once. It lives in the
 * debug source set because ActivityScenario can only start activities of the instrumented process.
 */
class BackdropActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this).apply { setBackgroundColor(Color.DKGRAY) })
    }
}
