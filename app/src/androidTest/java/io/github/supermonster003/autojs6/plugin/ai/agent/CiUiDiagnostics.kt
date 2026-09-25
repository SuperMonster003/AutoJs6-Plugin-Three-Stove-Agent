package io.github.supermonster003.autojs6.plugin.ai.agent

import android.graphics.Bitmap
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Opt-in failure artifacts for disposable CI emulators; never part of the release APK. */
internal object CiUiDiagnostics {
    fun capture(label: String) {
        if (InstrumentationRegistry.getArguments().getString("ai.agent.ciDiagnostics") != "true" ||
            Build.HARDWARE !in setOf("ranchu", "goldfish")) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = runCatching {
            val parent = instrumentation.targetContext.getExternalFilesDir(null) ?: return
            File(parent, "ci-ui-diagnostics/${SystemClock.uptimeMillis()}-${label.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80)}")
                .takeIf { it.mkdirs() }
        }.getOrNull() ?: return
        // Capture before test cleanup removes the fixture window. A capture failure must
        // not replace the original assertion, and no screen content goes to ordinary logs.
        for ((name, command) in mapOf(
            "windows" to "dumpsys window windows",
            "input" to "dumpsys input",
            "activities" to "dumpsys activity activities",
            "power" to "dumpsys power",
            "accessibility" to "dumpsys accessibility",
        )) runCatching {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { input ->
                File(directory, "$name.txt").outputStream().use { output -> input.copyTo(output) }
            }
        }
        runCatching {
            instrumentation.uiAutomation.takeScreenshot()?.let { screenshot ->
                try { File(directory, "screen.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                finally { screenshot.recycle() }
            }
        }
    }
}
