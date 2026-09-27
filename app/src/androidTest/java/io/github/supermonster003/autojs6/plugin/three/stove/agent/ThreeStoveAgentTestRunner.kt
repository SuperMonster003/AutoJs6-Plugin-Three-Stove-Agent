package io.github.supermonster003.autojs6.plugin.three.stove.agent

import android.os.Build
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

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
 */
class ThreeStoveAgentTestRunner : AndroidJUnitRunner() {
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

    private companion object {
        const val NATIVE_CUSHION_BYTES = 64 shl 20
    }
}
