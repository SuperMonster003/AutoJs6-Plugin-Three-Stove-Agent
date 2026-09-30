package io.github.supermonster003.autojs6.plugin.three.stove.agent

import android.os.Build
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.google.gson.JsonObject

/**
 * Runner for the whole instrumentation suite.
 *
 * Android 7.0 / 7.1 under-count the process-wide native allocation counter: framework classes that
 * call `VMRuntime.registerNativeFree` directly (Parcel, VectorDrawable, ImageReader) let the counter
 * drift below what NativeAllocationRegistry users registered. Once the counter is near zero, the
 * next collected registry object (a Paint registers 98 bytes on 7.0) throws
 * "Attempted to free 98 native bytes with only N native bytes registered as allocated" inside the
 * Cleaner daemon, which exits the process with status 1 ("Process crashed" after ~117 tests on the
 * API 24 emulator, locally and on CI). Reserving a cushion once keeps the long suite alive; it
 * changes nothing about the plugin under test and is a no-op on Android 8.0+.
 *
 * On emulators the runner also silences task alerts for the whole suite and restores the saved
 * settings afterwards; see [QuietTaskAlerts] for the windows they otherwise stack over UI tests.
 */
class ThreeStoveAgentTestRunner : AndroidJUnitRunner() {
    private var originalSettings: JsonObject? = null

    override fun onCreate(arguments: Bundle?) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            runCatching {
                val runtime = Class.forName("dalvik.system.VMRuntime")
                val instance = runtime.getMethod("getRuntime").invoke(null)
                runtime.getMethod("registerNativeAllocation", Int::class.javaPrimitiveType).invoke(instance, NATIVE_CUSHION_BYTES)
            }
        }
        super.onCreate(arguments)
    }

    override fun onStart() {
        // Leftover alert notifications (four or more make the system add a group summary whose content intent
        // launches the app through its launcher alias) must not survive into the notification-shade tests.
        runCatching { targetContext.getSystemService(android.app.NotificationManager::class.java).cancelAll() }
        if (QuietTaskAlerts.onEmulator()) originalSettings = runCatching { QuietTaskAlerts.silence(targetContext) }
            .onFailure { android.util.Log.e(TAG, "Task alerts were not silenced", it) }.getOrNull()
            .also { android.util.Log.i(TAG, "Task alerts silenced=${it != null}") }
        super.onStart()
    }

    override fun finish(resultCode: Int, results: Bundle?) {
        originalSettings?.let { runCatching { QuietTaskAlerts.restore(targetContext, it) }.onFailure { android.util.Log.e(TAG, "Task alerts were not restored", it) } }
        originalSettings = null
        runCatching { targetContext.getSystemService(android.app.NotificationManager::class.java).cancelAll() }
        super.finish(resultCode, results)
    }

    private companion object {
        const val TAG = "ThreeStoveAgentTestRunner"
        const val NATIVE_CUSHION_BYTES = 64 shl 20
    }
}
