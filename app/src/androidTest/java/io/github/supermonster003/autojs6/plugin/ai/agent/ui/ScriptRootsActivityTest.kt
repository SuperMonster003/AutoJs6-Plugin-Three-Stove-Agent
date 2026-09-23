package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import org.junit.Assert.*
import org.junit.Test

/** No filesystem permission: this screen stores proposals and leaves validation to the host. */
class ScriptRootsActivityTest {
    @Test fun settingsRejectTraversalAndPersistAnExplicitEmptySelection() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences("script-roots", 0)
        val previous = preferences.getString("configuration", null)
        preferences.edit().remove("configuration").commit()
        fun open() = instrumentation.startActivitySync(Intent(context, ScriptRootsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ScriptRootsActivity
        fun views(view: View): List<View> = listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
        var activity: ScriptRootsActivity? = null
        try {
            assertFalse(ScriptRootSettings(context).configured)
            val first = open().also { activity = it }
            instrumentation.runOnMainSync {
                val field = first.findViewById<EditText>(R.id.script_roots_paths)
                val save = views(first.window.decorView).filterIsInstance<Button>().single { it.text == context.getString(R.string.script_roots_save) }
                field.setText("/sdcard/../data")
                save.performClick()
                assertNotNull(field.error); assertFalse(first.isFinishing)
                assertFalse(ScriptRootSettings(context).configured)
                field.setText("/sdcard/AgentSamples/\n/sdcard/OtherScripts")
                save.performClick()
                assertTrue(first.isFinishing)
            }
            instrumentation.waitForIdleSync()
            assertEquals(setOf("/sdcard/AgentSamples", "/sdcard/OtherScripts"), ScriptRootSettings(context).read())
            val second = open().also { activity = it }
            instrumentation.runOnMainSync {
                val field = second.findViewById<EditText>(R.id.script_roots_paths)
                assertTrue(field.text.toString().contains("/sdcard/AgentSamples"))
                field.setText("")
                views(second.window.decorView).filterIsInstance<Button>().single { it.text == context.getString(R.string.script_roots_save) }.performClick()
            }
            instrumentation.waitForIdleSync()
            assertTrue(ScriptRootSettings(context).configured)
            assertTrue(ScriptRootSettings(context).read().isEmpty())
        } finally {
            instrumentation.runOnMainSync { activity?.finish() }
            preferences.edit().apply { if (previous == null) remove("configuration") else putString("configuration", previous) }.commit()
        }
    }
}
