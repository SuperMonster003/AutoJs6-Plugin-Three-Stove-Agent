package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class DynamicScriptConfirmationViewTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val source = (1..32).joinToString("\n") { "console.log(\"<b>plain $it</b>\");" }
    private fun arguments() = jsonObject("source" to source.json(), "timeoutMs" to 12345.json())
    private fun children(view: View): List<View> = listOf(view) + if (view is ViewGroup)
        (0 until view.childCount).flatMap { children(view.getChildAt(it)) } else emptyList()

    @Test fun fullSourceIsExactSelectableAndUsableInNarrowLargeFontRtl() {
        instrumentation.runOnMainSync {
            val appearance = HostAppearance("ar", true, 0xff334455.toInt(), 0xffeeddcc.toInt()).wrap(instrumentation.targetContext)
            val context = appearance.createConfigurationContext(Configuration(appearance.resources.configuration).apply { fontScale = 2f })
            var expanded = false
            val view = DynamicScriptConfirmationView.create(context, arguments()) { expanded = it }
            val code = view.findViewById<TextView>(R.id.script_dynamic_source)
            assertNotEquals(source, code.text.toString())
            assertTrue(code.text.toString().endsWith("..."))
            assertTrue(code.isTextSelectable)
            assertEquals(View.TEXT_DIRECTION_LTR, code.textDirection)
            val button = view.findViewById<Button>(R.id.script_dynamic_expand)
            button.performClick()
            assertTrue(expanded); assertEquals(source, code.text.toString())
            assertEquals(context.getString(R.string.script_dynamic_collapse), button.text)
            val density = context.resources.displayMetrics.density
            view.measure(View.MeasureSpec.makeMeasureSpec((280 * density).toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            assertTrue(button.height >= 48 * density)
            assertTrue(code.width >= 200 * density)
            // setTextIsSelectable(true) stores a Spannable buffer. Compare visible text,
            // not CharSequence implementation equality against a String.
            val warning = children(view).filterIsInstance<TextView>().first()
            assertEquals(context.getString(R.string.script_dynamic_warning), warning.text.toString())
            assertTrue("The leading permissions warning must remain selectable", warning.isTextSelectable)
            button.performClick(); assertFalse(expanded); assertNotEquals(source, code.text.toString())
        }
    }

    @Test fun pendingCardRestoresExpansionAndOffersSessionApproval() {
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            fun pending(id: String) = jsonObject("runId" to "dynamic-ui".json(), "interaction" to "plugin".json(),
                "pending" to jsonObject("requestId" to id.json(), "type" to "confirmation".json(), "risk" to "sensitive".json(),
                    "tool" to "script_run_source".json(), "description" to "Fixture".json(), "allowRunScope" to true.json(), "arguments" to arguments()))
            val firstContainer = LinearLayout(context)
            val first = PendingCard(firstContainer) { _, _ -> fail("Expansion must not execute a script") }
            first.render(pending("first"))
            firstContainer.findViewById<Button>(R.id.script_dynamic_expand).performClick()
            val state = Bundle().also(first::save)
            val recreatedContainer = LinearLayout(context)
            var response: JsonObject? = null
            val recreated = PendingCard(recreatedContainer) { body, complete -> response = body; complete(true) }
            recreated.restore(state); recreated.render(pending("first"))
            assertEquals(source, recreatedContainer.findViewById<TextView>(R.id.script_dynamic_source).text.toString())
            val buttons = children(recreatedContainer).filterIsInstance<Button>()
            assertTrue(buttons.any { it.text == context.getString(R.string.interaction_allow_run) })
            buttons.single { it.text == context.getString(R.string.interaction_allow_run) }.performClick()
            assertEquals(true, response!!.flag("allowed")); assertEquals("run", response!!.string("scope"))
            recreated.render(pending("next"))
            assertNotEquals(source, recreatedContainer.findViewById<TextView>(R.id.script_dynamic_source).text.toString())
        }
    }

    @Test fun savingRequestsANewUserChosenDocumentWithoutSourceInTheExternalIntent() {
        val intent = RunDetailActivity.scriptDocumentIntent("agent-generated-1234.js")
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent.action)
        assertTrue(intent.categories.contains(Intent.CATEGORY_OPENABLE))
        assertEquals("text/javascript", intent.type)
        assertEquals("agent-generated-1234.js", intent.getStringExtra(Intent.EXTRA_TITLE))
        assertEquals(setOf(Intent.EXTRA_TITLE), intent.extras!!.keySet())
        assertNull(intent.data)
    }
}
