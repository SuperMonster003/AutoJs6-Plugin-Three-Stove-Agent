package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class ScriptConfirmationViewTest {
    @Test fun narrowLargeFontColumnsKeepParameterNamesReadable() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = HostAppearance("ar", true, 0xff334455.toInt(), 0xffeeddcc.toInt()).wrap(instrumentation.targetContext)
            val configured = context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply { fontScale = 2f })
            val pending = jsonObject("description" to "Layout fixture".json(), "arguments" to jsonObject("parameters" to
                jsonObject("delivery_location" to "Office reception on the first floor".json())))
            val view = ScriptConfirmationView.create(configured, pending)
            val density = configured.resources.displayMetrics.density
            view.measure(View.MeasureSpec.makeMeasureSpec((280 * density).toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            fun check(view: View) {
                if (view is TextView && view.text.toString() == "delivery_location") {
                    assertTrue("Parameter column must not collapse beside a long value", view.width - view.paddingLeft - view.paddingRight >= 80 * density)
                    assertTrue("Parameter name must wrap by words/chunks, not one character per line", view.lineCount < view.text.length / 2)
                }
                if (view is ViewGroup) for (i in 0 until view.childCount) check(view.getChildAt(i))
            }
            check(view)
        }
    }

    @Test fun tableShowsExactTextMarksLiteralsAndSortsNamesWithoutNestedScrolling() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val value = "<b>Office</b>\n\"quoted\"" + "x".repeat(5000)
            val pending = jsonObject("description" to "Prepare a coffee order".json(), "arguments" to jsonObject("parameters" to
                jsonObject("count" to 1.json(), "address" to value.json(), "enabled" to false.json(), "note" to "false".json())))
            val view = ScriptConfirmationView.create(instrumentation.targetContext, pending)
            assertFalse("The card scrolls as a whole; no nested scroll view", view is ScrollView)
            fun texts(view: View): List<TextView> = when (view) {
                is TextView -> listOf(view)
                is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
                else -> emptyList()
            }
            val labels = texts(view)
            fun label(text: String) = labels.filter { it.text.toString() == text }
            assertTrue(label("Prepare a coffee order").isNotEmpty()); assertTrue("Full, unescaped value", label(value).single().isTextSelectable)
            assertTrue(label("1").isNotEmpty())
            val (literal, text) = label("false").partition { it.typeface == android.graphics.Typeface.MONOSPACE }
            assertEquals("Boolean false is marked as a literal", 1, literal.size); assertEquals("String \"false\" is plain text", 1, text.size)
            val names = labels.map { it.text.toString() }
            assertTrue(names.indexOf("address") < names.indexOf("count"))
        }
    }
}
