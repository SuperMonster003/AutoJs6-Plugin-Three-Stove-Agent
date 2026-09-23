package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class ScriptConfirmationViewTest {
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
