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

    @Test fun tablePreservesTypesMarkupAndLongValuesInsideAScrollView() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val value = "<b>Office</b>\n\"quoted\"" + "x".repeat(5000)
            val pending = jsonObject("description" to "Prepare a coffee order".json(), "arguments" to jsonObject("parameters" to
                jsonObject("count" to 1.json(), "address" to value.json(), "enabled" to false.json())))
            val view = ScriptConfirmationView.create(instrumentation.targetContext, pending)
            assertTrue(view is ScrollView)
            fun texts(view: View): List<String> = when (view) {
                is TextView -> listOf(view.text.toString())
                is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
                else -> emptyList()
            }
            val labels = texts(view)
            assertTrue(labels.contains("Prepare a coffee order")); assertTrue(labels.contains(value.json().toString()))
            assertTrue(labels.contains("1")); assertTrue(labels.contains("false"))
            assertTrue(labels.indexOf("address") < labels.indexOf("count"))
        }
    }
}
