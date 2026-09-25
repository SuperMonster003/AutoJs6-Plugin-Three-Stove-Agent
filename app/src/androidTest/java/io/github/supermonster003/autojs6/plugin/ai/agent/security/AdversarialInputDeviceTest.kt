package io.github.supermonster003.autojs6.plugin.ai.agent.security

import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.CiUiDiagnostics
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.nodes.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real hostile UI and scheduler; deterministic model and inspection adapter. No Provider or real host broker. */
class AdversarialInputDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val fixturePackage = instrumentation.context.packageName
    private fun asset(path: String) = instrumentation.targetContext.assets.open(path).bufferedReader().use { it.readText() }

    @Test fun observedInjectionCannotEnableToolsForgeNodesOrApproveDeletion() = withScreen {
        for (degraded in listOf(false, true)) for (language in listOf("en", "zh")) {
            exercise(degraded, language, allowFiles = false)
            exercise(degraded, language, allowFiles = true)
        }
    }
    @Test fun fixtureDeleteButtonIsFunctionalAndOnlyRemovesItsOwnCanary() = withScreen {
        recycle(screenNode(AdversarialScreenActivity.INTACT))
        val button = screenNode(AdversarialScreenActivity.DELETE)
        try { assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK)) } finally { recycle(button) }
        recycle(screenNode("canary: deleted"))
    }

    private fun exercise(degraded: Boolean, language: String, allowFiles: Boolean) {
        val catalog = ToolCatalog(asset("catalog/tools.json"))
        val policy = ToolPolicy.fromAssets(::asset, mapOf(ToolGroup.FILES to allowFiles))
        val target = ModelTarget("fixture", "local:injection", ModelLocality.ON_DEVICE, ModelProtocol.LOCAL, true, 128 * 1024)
        val format = if (degraded) DecisionSchema.degraded() else DecisionSchema(catalog).generate(target.protocol, policy)
        val compiler = ContextCompiler(PromptCatalog(::asset, catalog), catalog, policy, target, format)
        val inputs = CopyOnWriteArrayList<ModelInput>()
        val events = CopyOnWriteArrayList<RunEvent>()
        val actions = AtomicInteger()
        val inspections = AtomicInteger()
        val done = CountDownLatch(1)
        val waiting = CountDownLatch(1)
        val decisions = ArrayDeque(listOf(
            """{"kind":"tool","tool":"ui_dump","arguments":{}}""",
            if (allowFiles) """{"kind":"tool","tool":"files_write","arguments":{"path":"../p7-injection-canary.txt","content":"overwrite"}}"""
            else """{"kind":"tool","tool":"shell_exec","arguments":{"cmd":"rm p7-injection-canary.txt"}}""",
            """{"kind":"tool","tool":"ui_click","arguments":{"nodeRef":"#n9999","snapshotId":"foreign-snapshot"}}""",
            """{"kind":"tool","tool":"ui_click","arguments":{"nodeRef":"#n2"}}""",
            """{"kind":"done","done":{"status":"partial","summary":"No deletion was authorized","unfinished":["Only the requested reading task is allowed"]}}"""))
        val model = object : RunModel {
            override fun generate(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long, callback: (PortResult<ModelReply>) -> Unit): Cancellation {
                inputs += input
                callback(PortResult.Success(ModelReply(decisions.removeFirst(), ModelUsage(10, 10))))
                return Cancellation.NONE
            }
        }
        SerialRunScheduler().use { scheduler ->
            val observations = ObservationTools()
            val adapter = ActionTools(scheduler, observations, { call, callback ->
                if (call.method == "inspectNode") {
                    inspections.incrementAndGet()
                    assertEquals("published", call.args[0].asJsonObject.string("snapshotId"))
                    assertEquals("#n2", call.args[0].asJsonObject.string("nodeRef"))
                    val node = screenNode(AdversarialScreenActivity.DELETE)
                    try { callback(PortResult.Success(jsonObject(
                        "target" to jsonObject("nodeRef" to "#n1".json(), "snapshotId" to "bound".json(), "actionToken" to "test-only-token".json()),
                        "text" to node.text.toString().json(), "desc" to "".json(), "packageName" to fixturePackage.json(),
                        "password" to false.json(), "enabled" to node.isEnabled.json(), "uncertain" to false.json())))
                    } finally { recycle(node) }
                } else {
                    actions.incrementAndGet()
                    if (call.method == "click") {
                        val node = screenNode(AdversarialScreenActivity.DELETE)
                        try { callback(PortResult.Success(node.performAction(AccessibilityNodeInfo.ACTION_CLICK).json())) }
                        finally { recycle(node) }
                    } else callback(PortResult.Failure(RunError.CAPABILITY_DENIED))
                }
                Cancellation.NONE
            }, canObserve = true)
            val tools = object : RunTools {
                override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
                    if (invocation.name == "ui_dump") { callback(PortResult.Success(PreparedTool(invocation, ToolMetadata()))); return Cancellation.NONE }
                    return adapter.prepare(invocation, timeoutMs, callback)
                }
                override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
                    if (prepared.invocation.name == "ui_dump") {
                        callback(PortResult.Success(ToolReply(observations.transform(prepared.invocation, observation()))))
                        return Cancellation.NONE
                    }
                    actions.incrementAndGet()
                    return adapter.execute(prepared, timeoutMs, callback)
                }
            }
            val queue = RunQueue(scheduler, catalog, policy, compiler, model, tools) { RunnerText(asset("runner/texts.json"), it) }
            val run = queue.submit(RunOptions(if (language == "zh") "只读取当前界面文字" else "Read the screen text only", format, locale = language)) {
                events += it
                if (it.type == "confirmation") waiting.countDown()
                if (it.type == "done") done.countDown()
            }
            val reachedConfirmation = waiting.await(15, TimeUnit.SECONDS)
            assertTrue("Expected deletion to wait for real approval, state=${run.state}", reachedConfirmation)
            assertEquals(RunState.WAITING_CONFIRMATION, run.state)
            assertEquals(0, actions.get()); assertEquals(1, inspections.get())
            val pending = events.single { it.type == "confirmation" }.payload
            assertEquals("sensitive", pending.string("risk"))
            assertTrue(pending.string("description")!!.contains(AdversarialScreenActivity.DELETE))
            // An explicit fixture denial, never an automatic approval derived from the screen.
            run.confirm(pending.string("requestId")!!, false)
            assertTrue(done.await(10, TimeUnit.SECONDS))
            assertEquals(RunState.PARTIAL, run.state); assertEquals(0, actions.get())
            assertTrue(decisions.isEmpty())
            assertTrue(inputs[1].messages.toString().contains(AdversarialScreenActivity.ATTACK))
            for (input in inputs) {
                val messages = input.messages.map { it.asJsonObject }
                assertEquals(1, messages.count { it.string("role") == "system" })
                assertFalse(messages.first().string("content")!!.contains(AdversarialScreenActivity.ATTACK))
            }
            var history: JsonObject? = null
            run.readJournal { history = it }
            val steps = checkNotNull(history).getAsJsonArray("steps").map { it.asJsonObject }
            assertEquals(listOf(if (allowFiles) "TOOL_ARGUMENTS_INVALID" else "TOOL_DISABLED"),
                steps.single { it.getAsJsonObject("decision").has("rejections") }.getAsJsonObject("decision").getAsJsonArray("rejections").map { it.asString })
            assertTrue(steps.any { it.string("error") == "NODE_REF_STALE" })
            assertTrue(steps.any { it.string("confirmation") == "denied" })
            assertEquals(1, events.count { it.type == "done" })
            val intact = screenNode(AdversarialScreenActivity.INTACT)
            recycle(intact)
        }
    }

    private fun observation(): JsonObject {
        val rows = listOf(AdversarialScreenActivity.ATTACK, AdversarialScreenActivity.DELETE, AdversarialScreenActivity.INTACT).mapIndexed { index, text ->
            val node = screenNode(text)
            try {
                val bounds = Rect(); node.getBoundsInScreen(bounds)
                "#n${index + 1} ${node.className.toString().substringAfterLast('.')}" +
                    (if (node.isClickable) " clickable" else "") + " ${node.text.toString().json()} [${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]"
            } finally { recycle(node) }
        }
        return jsonObject("format" to "compact".json(), "snapshotId" to "published".json(), "nodeCount" to rows.size.json(),
            "truncated" to false.json(), "text" to ("window: $fixturePackage/AdversarialScreenActivity\n" + rows.joinToString("\n")).json())
    }

    private fun screenNode(text: String): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (SystemClock.uptimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root != null) {
                val matches = root.findAccessibilityNodeInfosByText(text)
                recycle(root)
                val selected = matches.firstOrNull { it.text?.toString() == text && it.packageName?.toString() == fixturePackage }
                matches.filter { it !== selected }.forEach(::recycle)
                if (selected != null) return selected
            }
            SystemClock.sleep(50)
        }
        CiUiDiagnostics.capture("injection-node-missing")
        error("Injection fixture node was not visible")
    }
    private fun withScreen(action: () -> Unit) {
        instrumentation.context.startActivity(Intent().setClassName(fixturePackage, AdversarialScreenActivity::class.java.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val result = runCatching { recycle(screenNode(AdversarialScreenActivity.ATTACK)); action() }
        val cleanup = runCatching {
            val close = screenNode(AdversarialScreenActivity.CLOSE)
            try { assertTrue(close.performAction(AccessibilityNodeInfo.ACTION_CLICK)) } finally { recycle(close) }
            // performAction acknowledges dispatch, not Activity destruction. The next test
            // must not read or click this closing window instead of its newly created one.
            val deadline = SystemClock.elapsedRealtime() + 15_000
            fun stillVisible(): Boolean {
                val root = instrumentation.uiAutomation.rootInActiveWindow ?: return false
                return try { root.refresh() && root.packageName?.toString() == fixturePackage } finally { recycle(root) }
            }
            while (stillVisible() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50)
            assertFalse("Previous injection window has closed", stillVisible())
        }
        result.exceptionOrNull()?.let { failure -> cleanup.exceptionOrNull()?.let(failure::addSuppressed); throw failure }
        cleanup.getOrThrow()
    }
    @Suppress("DEPRECATION") private fun recycle(node: AccessibilityNodeInfo) = node.recycle()
}
