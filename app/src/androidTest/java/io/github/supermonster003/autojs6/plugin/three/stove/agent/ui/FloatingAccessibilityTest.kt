package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.app.AppOpsManager
import android.content.*
import android.os.*
import android.view.View
import android.widget.LinearLayout
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.AgentRuntime
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.*
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

/** Real WindowManager layout with controlled presentation snapshots; no model or device actions. */
class FloatingAccessibilityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun shell(command: String) = instrumentation.uiAutomation.executeShellCommand(command).use {
        ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
    }
    private fun field(target: Any, name: String) = target.javaClass.getDeclaredField(name).apply { isAccessible = true }
    private fun call(target: Any, name: String) = target.javaClass.getDeclaredMethod(name).apply { isAccessible = true }.invoke(target)
    private fun waitFor(check: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + 10000
        while (SystemClock.elapsedRealtime() < end) { if (check()) return; SystemClock.sleep(50) }
        fail("Floating window did not finish layout")
    }

    @Suppress("DEPRECATION")
    @Test fun floatingStatesHaveAccessibleControlsAndUnclippedText() = renderStates(false)

    @Test fun captureReadmeFloating() {
        ReadmeCapture.requireOptIn()
        renderStates(true)
    }

    private fun renderStates(capture: Boolean) {
        val audit = UiAccessibilityAudit()
        val namespace = "layout-${UUID.randomUUID()}"
        val directory = File(context.cacheDir, namespace).apply { check(mkdirs()) }
        val prefs = context.getSharedPreferences(namespace, Context.MODE_PRIVATE)
        if (capture) prefs.edit().putString("goal", "Read the Android version and show the result.").commit()
        val fixture = object : ContextWrapper(context) {
            override fun getFilesDir() = directory
            override fun getSharedPreferences(name: String?, mode: Int) = prefs
        }
        SettingsStore(File(directory, "agent-settings.json")).save(AgentSettings(floating = true))
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val oldMode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, Process.myUid(), context.packageName)
        val modes = mapOf(0 to "allow", 1 to "ignore", 2 to "deny", 3 to "default", 4 to "foreground")
        var runtime: AgentRuntime? = null
        var floating: FloatingBall? = null
        try {
            shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
            instrumentation.runOnMainSync {
                runtime = AgentRuntime(fixture)
                // Layout-only fixture: suppress automatic runtime publication, then feed the
                // production presenter snapshots. Auth/runner paths have separate IPC tests.
                field(runtime, "initialized").setBoolean(runtime, false)
            }
            waitFor { runCatching { runtime!!.settings.snapshot().floating }.getOrDefault(false) }
            instrumentation.runOnMainSync {
                floating = FloatingBall(runtime!!)
                field(floating, "appearance").set(floating, HostAppearance(audit.language, audit.dark, 0xff334455.toInt(), 0xffeeddcc.toInt()))
                field(floating, "readAppearance").setBoolean(floating, false)
            }
            val runId = UUID.randomUUID().toString()
            fun show(name: String, expanded: Boolean, run: JsonObject?, timeline: Boolean = false) {
                instrumentation.runOnMainSync {
                    call(floating!!, "removeWindow")
                    field(floating, "snapshot").set(floating, WorkbenchSnapshot(
                        // The audit keeps the danger tone of full access; the README capture shows the ordinary standard mode.
                        jsonObject("state" to C.LINK_STATE_ATTACHED.json(), "voiceEnabled" to true.json(), "accessMode" to (if (capture) "standard" else "full").json()),
                        listOfNotNull(run), run, listOf(if (capture) "default" else "A preset with a long display name"),
                        if (capture) "default" else "A preset with a long display name"))
                    field(floating, "expanded").setBoolean(floating, expanded)
                    field(floating, "timelineOpen").setBoolean(floating, timeline)
                    call(floating, "publish")
                }
                waitFor { var ready = false; instrumentation.runOnMainSync {
                    val root = field(floating!!, "root").get(floating) as? View
                    ready = root?.isLaidOut == true && root.height > 0 && !root.isLayoutRequested
                }; ready }
                instrumentation.waitForIdleSync()
                instrumentation.runOnMainSync {
                    val root = field(floating!!, "root").get(floating) as LinearLayout
                    if (capture) ReadmeCapture.save(root, "floating") else audit.inspect(root, "floating-$name")
                }
            }
            val active = jsonObject("runId" to runId.json(), "state" to "running".json(),
                "goal" to "Layout inspection with a long progress label".json(), "interaction" to "plugin".json(),
                "steps" to com.google.gson.JsonArray().apply {
                    add(jsonObject("index" to 1.json(), "tool" to "ui_dump".json(), "decision" to jsonObject("reasoning" to "Observe the current screen first".json())))
                    add(jsonObject("index" to 2.json(), "tool" to "ui_click".json(), "confirmation" to "allowed".json()))
                })
            if (capture) {
                show("entry", true, null)
                return
            }
            show("idle", false, null)
            show("running", false, active)
            instrumentation.runOnMainSync {
                val root = field(floating!!, "root").get(floating) as LinearLayout
                val step = root.findViewWithTag<android.widget.TextView>("floating-step")!!
                assertEquals(View.VISIBLE, step.visibility)
                assertTrue("The compact ball names the latest step", step.text.toString().endsWith("ui_click"))
                assertFalse("No running prefix before the goal", root.findViewWithTag<android.widget.TextView>("floating-goal-label")!!.text.startsWith("Running"))
            }
            show("timeline", false, active, timeline = true)
            instrumentation.runOnMainSync {
                val root = field(floating!!, "root").get(floating) as LinearLayout
                assertNotNull("Timeline rows are shown", root.findViewWithTag<View>("step-2"))
            }
            show("entry", true, null)
            instrumentation.runOnMainSync {
                val root = field(floating!!, "root").get(floating) as LinearLayout
                assertNotNull("Model chip sits in the options row", root.findViewWithTag<View>("floating-model"))
                assertNotNull("Access chip sits in the options row", root.findViewWithTag<View>("floating-access"))
                assertSame("History moved into the more panel", root.findViewWithTag<View>("floating-more-panel"), root.findViewWithTag<View>("floating-history")!!.parent)
                assertSame("Workbench moved into the more panel", root.findViewWithTag<View>("floating-more-panel"), root.findViewWithTag<View>("floating-workbench")!!.parent)
                val send = root.findViewWithTag<View>("floating-send")!!; val row = send.parent as View
                assertTrue("Send keeps the bottom of the input row", row.height - send.bottom <= 4 * root.resources.displayMetrics.density.toInt())
            }
            for (kind in listOf("text", "choice", "confirm", "payment", "script")) {
                val pending = jsonObject("requestId" to UUID.randomUUID().toString().json(),
                    "deadlineMs" to (java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) + 120000).json())
                if (kind == "payment" || kind == "script") {
                    pending.addProperty("type", "confirmation"); pending.addProperty("risk", "sensitive")
                    pending.addProperty("description", "Review the complete parameters before allowing this fixture action")
                    pending.addProperty("tool", if (kind == "script") "script_run" else "nodes_click")
                    pending.addProperty("allowRunScope", false)
                    pending.add("arguments", AgentJson.objectOf(if (kind == "script")
                        """{"parameters":{"delivery_location":"Office reception on the first floor","quantity":3,"note":"Do not pay for this layout fixture"}}"""
                        else """{"text":"Pay for fixture order"}""", 4096))
                } else {
                    pending.addProperty("type", "question"); pending.addProperty("kind", kind)
                    pending.addProperty("question", "Which preference should this task use?")
                    pending.addProperty("memoryKey", "layout-preference"); pending.addProperty("rememberScope", "global")
                    if (kind == "choice") pending.add("choices", com.google.gson.JsonArray().apply {
                        add("A medium hot latte with regular milk"); add("A medium hot latte with oat milk")
                    })
                }
                show(kind, true, active.deepCopy().apply { addProperty("state", "waiting_user"); add("pending", pending) })
            }
            audit.finish()
        } finally {
            instrumentation.runOnMainSync { floating?.close() }
            instrumentation.waitForIdleSync()
            runtime?.memories?.close()
            shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW ${checkNotNull(modes[oldMode])}")
            context.deleteSharedPreferences(namespace)
            directory.deleteRecursively()
        }
    }
}
