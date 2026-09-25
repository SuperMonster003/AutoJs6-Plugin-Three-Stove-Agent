package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Inspects laid-out production views, including controls below the current scroll viewport. */
internal class UiAccessibilityAudit {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val failures = mutableListOf<String>()
    private val reports = mutableListOf<String>()
    private val options = InstrumentationRegistry.getArguments()
    val language: String = options.getString("agent.ui.locale", "ar")
    val dark: Boolean = options.getString("agent.ui.dark", "true").toBoolean()

    fun themed(action: () -> Unit) {
        val previous = HostAppearance.cached
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        HostAppearance.worker.execute { entered.countDown(); release.await(180, TimeUnit.SECONDS) }
        assertTrue("Appearance worker ready", entered.await(15, TimeUnit.SECONDS))
        HostAppearance.cached = HostAppearance(language, dark, 0xff334455.toInt(), 0xffeeddcc.toInt())
        try { action() } finally { HostAppearance.cached = previous; release.countDown() }
    }

    fun inspect(activity: Activity, name: String) {
        val configuration = activity.resources.configuration
        assertEquals(language, configuration.locales[0].language)
        assertEquals(if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO,
            configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        options.getString("agent.ui.font")?.toFloat()?.let { assertEquals(it, configuration.fontScale, 0.02f) }
        inspect(activity.findViewById(android.R.id.content), name)
    }

    fun inspect(root: View, name: String) {
        val before = failures.size
        val views = mutableListOf<View>()
        fun visit(view: View) {
            if (view.visibility != View.VISIBLE) return
            views += view
            if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
        visit(root)
        val minimum = 48 * root.resources.displayMetrics.density
        var controls = 0
        fun identity(view: View) = view.javaClass.simpleName + ":" +
            (runCatching { view.resources.getResourceEntryName(view.id) }.getOrNull() ?: "generated")
        fun check(ok: Boolean, view: View, issue: String) { if (!ok) failures += "$name/${identity(view)}: $issue" }
        for (view in views) {
            val control = view is Button || view is EditText || view is Spinner
            if (control) {
                controls++
                val caption = views.filterIsInstance<TextView>().any {
                    view.id != View.NO_ID && it.labelFor == view.id && it.text.isNotBlank()
                }
                val named = !view.contentDescription.isNullOrBlank() || caption || when (view) {
                    is EditText -> !view.hint.isNullOrBlank()
                    is TextView -> view.text.isNotBlank()
                    else -> false
                }
                check(named, view, "missing accessible label")
                check(view.width + 1 >= minimum && view.height + 1 >= minimum, view,
                    "touch target ${view.width}x${view.height}, minimum ${minimum.toInt()}")
            }
            if (view.width > 0 && view.parent is ViewGroup && view !== root) {
                val parent = view.parent as ViewGroup
                check(view.left >= -1 && view.right <= parent.width + 1, view, "outside parent horizontal bounds")
            }
            if (view is TextView && view !is EditText && view.text.isNotBlank() && view.tag != "floating-step") {
                view.layout?.let { layout ->
                    check(layout.height <= view.height - view.compoundPaddingTop - view.compoundPaddingBottom + 2,
                        view, "text vertically clipped")
                    check((0 until layout.lineCount).none { layout.getEllipsisCount(it) > 0 }, view, "label ellipsized")
                }
            }
        }
        val scale = root.resources.configuration.fontScale
        reports += "$name views=${views.size} controls=$controls font=$scale issues=${failures.size - before}"
        if (root.width > 0 && root.height > 0) {
            val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            try {
                val colors = root.context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
                bitmap.eraseColor(colors.getColor(0, android.graphics.Color.BLACK)); colors.recycle()
                root.draw(Canvas(bitmap))
                File(root.context.cacheDir, "p7-ui-$name-$language-$dark-$scale.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } finally { bitmap.recycle() }
        }
    }

    fun finish() {
        instrumentation.sendStatus(0, Bundle().apply { putString("stream", reports.joinToString("\n", "P7_UI\n", "\n")) })
        assertTrue(failures.take(60).joinToString("\n"), failures.isEmpty())
    }
}
