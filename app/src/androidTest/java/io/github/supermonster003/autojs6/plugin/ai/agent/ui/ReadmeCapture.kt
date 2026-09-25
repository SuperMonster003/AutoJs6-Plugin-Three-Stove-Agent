package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import java.io.File

/** Opt-in documentation capture. Use a fresh, disposable emulator with synthetic task data. */
internal object ReadmeCapture {
    fun requireOptIn() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("agent.readme.capture") == "true")
        check(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("sdk")) {
            "README captures require a disposable emulator"
        }
    }

    fun save(root: View, name: String) {
        check(root.isLaidOut && root.width > 0 && root.height > 0 && !root.isLayoutRequested)
        val directory = File(root.context.cacheDir, "readme-captures").apply { mkdirs() }
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        try {
            // Render the actual app view. FLAG_SECURE remains enabled, and no system or other
            // app content is captured. These fixtures contain no real account/task information.
            root.draw(Canvas(bitmap))
            File(directory, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
    }
}
