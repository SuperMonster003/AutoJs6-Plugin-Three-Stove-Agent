package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.*
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.service.AgentWire
import io.github.supermonster003.autojs6.plugin.ai.agent.service.IAgentSettings
import io.github.supermonster003.autojs6.plugin.ai.agent.service.SettingsEndpoint
import io.github.supermonster003.autojs6.plugin.ai.agent.service.RunArchive
import io.github.supermonster003.autojs6.plugin.ai.agent.service.IRunHistory
import io.github.supermonster003.autojs6.plugin.ai.agent.service.IRunHistoryCallback
import io.github.supermonster003.autojs6.plugin.ai.agent.service.HistoryEndpoint
import io.github.supermonster003.autojs6.plugin.ai.agent.service.AgentLocalService
import io.github.supermonster003.autojs6.plugin.ai.agent.service.IPresetStore
import io.github.supermonster003.autojs6.plugin.ai.agent.service.IPresetStoreCallback
import io.github.supermonster003.autojs6.plugin.ai.agent.service.PresetEndpoint
import io.github.supermonster003.autojs6.plugin.ai.agent.store.PresetCodec
import io.github.supermonster003.autojs6.plugin.ai.agent.store.Preset
import io.github.supermonster003.autojs6.plugin.ai.agent.store.RunHistoryCodec
import io.github.supermonster003.autojs6.plugin.ai.agent.store.MemoryCodec
import io.github.supermonster003.autojs6.plugin.ai.agent.store.MemoryEntry
import io.github.supermonster003.autojs6.plugin.ai.agent.service.IMemoryStore
import io.github.supermonster003.autojs6.plugin.ai.agent.service.IMemoryStoreCallback
import io.github.supermonster003.autojs6.plugin.ai.agent.service.MemoryEndpoint
import org.autojs.plugin.ai.agent.api.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import org.autojs.plugin.host.capability.api.*
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.autojs.plugin.common.api.AutoJs6HostSettingsContract as S
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real activities, Binder boundary, queue, foreground service and archive; only broker output is scripted. */
class WorkbenchActivityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun bundle(key: String, json: String = "{}") = AgentWire.envelope(key, json)
    private val completed = """{"kind":"done","done":{"status":"completed","summary":"Workbench fixture complete","evidence":["Fixture answer received"]}}"""
    private inner class Model(private val holdEveryCall: Boolean = false) : IAiAgentModelBroker.Stub() {
        val calls = AtomicInteger()
        val requests = java.util.concurrent.CopyOnWriteArrayList<JSONObject>()
        @Volatile var held: Pair<String, IAiAgentModelCallback>? = null
        override fun getBrokerInfo() = bundle(C.KEY_MODEL_BROKER_INFO_JSON,
            """{"available":true,"providerId":"workbench","maximumInputBytes":131072,"maximumOutputBytes":65536,"maximumResponseSchemaBytes":16384}""").apply {
            putString(H.KEY_GRANT_JSON, """{"maxInputBytesPerRequest":131072,"maxTotalTokens":1000000,"consumedTokens":0}""")
        }
        private fun emit(cb: IAiAgentModelCallback, id: String, type: String, sequence: Int, data: JSONObject = JSONObject()) {
            cb.onEvent(bundle(C.KEY_MODEL_EVENT_JSON, data.put("requestId", id).put("type", type).put("sequence", sequence).toString()))
        }
        override fun listTargets(request: Bundle, callback: IAiAgentModelCallback) {
            val id = JSONObject(request.getString(C.KEY_MODEL_REQUEST_JSON)!!).getString("requestId")
            emit(callback, id, "started", 1)
            emit(callback, id, "completed", 2, JSONObject().put("targets", JSONArray("""[{"targetId":"workbench:fixture","displayName":"Workbench fixture model","locality":2,"configured":true,"available":true,"maximumContextBytes":131072,"capabilityIds":[],"supportedControls":["maximum-output-tokens"]}]""")))
        }
        override fun generate(request: Bundle, callback: IAiAgentModelCallback) {
            val json = JSONObject(request.getString(C.KEY_MODEL_REQUEST_JSON)!!)
            requests.add(json)
            val id = json.getString("requestId")
            emit(callback, id, "started", 1); held = id to callback
            if (calls.incrementAndGet() > 1 && !holdEveryCall) finish(completed)
        }
        fun finish(text: String) { val (id, callback) = checkNotNull(held); held = null
            emit(callback, id, "usage", 2, JSONObject().put("usage", JSONObject().put("inputTokens", 1).put("outputTokens", 1).put("totalTokens", 2)))
            emit(callback, id, "completed", 3, JSONObject().put("text", text).put("targetId", "workbench:fixture").put("finishReason", 0)) }
        override fun cancel(reference: Bundle?) = Unit
        override fun destroy(reason: Bundle?) = Unit
    }
    private val capabilities = object : IHostCapabilityBroker.Stub() {
        override fun getBrokerInfo() = Bundle().apply {
            putInt(H.KEY_CONTRACT_VERSION, H.CONTRACT_VERSION)
            putStringArray(H.KEY_GRANT_METHODS, emptyArray()); putStringArray(H.KEY_GRANT_PERMISSIONS, emptyArray())
            putInt(H.KEY_GRANT_MAX_REQUEST_BYTES, 32768); putLong(H.KEY_GRANT_MAX_TIMEOUT_MS, 30000)
        }
        override fun dispatch(request: Bundle?, callback: IHostCapabilityCallback?) { error("Fixture must not operate the device") }
        override fun destroy(reason: Bundle?) = Unit
    }
    private fun withFixture(model: Model = Model(), groups: List<String> = listOf("observe"), action: (IAiAgentLink, Model) -> Unit) {
        val connected = CountDownLatch(1); var plugin: IAiAgentPlugin? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) { plugin = IAiAgentPlugin.Stub.asInterface(service); connected.countDown() }
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        val prefs = context.getSharedPreferences("workbench", Context.MODE_PRIVATE)
        val oldGoal = prefs.getString("goal", null); val oldPreset = prefs.getString("preset", null)
        check(context.bindService(Intent().setClassName(context, context.packageName + ".service.WorkbenchFixtureService"), connection, Context.BIND_AUTO_CREATE))
        var link: IAiAgentLink? = null
        try {
            assertTrue(connected.await(15, TimeUnit.SECONDS))
            link = plugin!!.attach(bundle(C.KEY_LINK_CONFIG_JSON, JSONObject().put("grantSummary", JSONObject().put("toolGroups", JSONArray(groups))).toString()), model, capabilities,
                object : IAiAgentLinkCallback.Stub() { override fun onStatus(status: Bundle?) = Unit; override fun onEvent(event: Bundle?) = Unit })
            action(link, model)
        } finally {
            link?.detach(bundle(H.KEY_REASON_JSON, """{"reason":"workbench-test-finished"}"""))
            context.unbindService(connection)
            prefs.edit().putString("goal", oldGoal).putString("preset", oldPreset).commit()
        }
    }
    private fun waitFor(message: String, timeoutMs: Long = 20000, predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) { if (predicate()) return; SystemClock.sleep(80) }
        fail(message)
    }
    private fun waitUi(scenario: ActivityScenario<LauncherActivity>, message: String, predicate: (LauncherActivity) -> Boolean) {
        waitFor(message) { var ready = false; scenario.onActivity { ready = predicate(it) }; ready }
    }
    private fun enter(scenario: ActivityScenario<LauncherActivity>, text: String = "Workbench acceptance fixture") {
        scenario.onActivity { it.findViewById<EditText>(R.id.workbench_goal).setText(text) }
        waitUi(scenario, "Send enabled after attachment") { it.findViewById<Button>(R.id.workbench_send).isEnabled }
        scenario.onActivity { it.findViewById<Button>(R.id.workbench_send).performClick() }
    }
    @Test fun inputRunningInlineReplyRecreationCompletionAndOfflineDetails() = withFixture { link, model ->
        ActivityScenario.launch(LauncherActivity::class.java).use { scenario ->
            enter(scenario)
            waitFor("Model started") { model.calls.get() == 1 }
            waitUi(scenario, "Running card") { it.findViewById<TextView>(R.id.workbench_state).text == it.getString(R.string.run_running) }
            model.finish("""{"kind":"ask","ask":{"kind":"text","question":"Fixture answer?"}}""")
            waitUi(scenario, "Inline answer") { it.findViewById<EditText>(R.id.workbench_answer) != null }
            scenario.onActivity { it.findViewById<EditText>(R.id.workbench_answer).setText("Fixture answer") }
            scenario.recreate()
            waitUi(scenario, "Answer draft survives recreation") { it.findViewById<EditText>(R.id.workbench_answer)?.text?.toString() == "Fixture answer" }
            scenario.onActivity { activity ->
                val card = activity.findViewById<LinearLayout>(R.id.workbench_pending)
                (0 until card.childCount).map { card.getChildAt(it) }.filterIsInstance<Button>().single().performClick()
            }
            waitUi(scenario, "Completed result") { it.findViewById<TextView>(R.id.workbench_step).text.toString() == "Workbench fixture complete" }
            val runs = AgentConnection.decode(link.listRuns(bundle(C.KEY_RUN_REQUEST_JSON))).getAsJsonArray("runs")
            val id = runs[0].asJsonObject.string("runId")!!
            assertEquals(2, model.calls.get())
            link.detach(bundle(H.KEY_REASON_JSON, """{"reason":"offline-details"}"""))
            ActivityScenario.launch<RunDetailActivity>(Intent(context, RunDetailActivity::class.java).putExtra("runId", id)).use { detail ->
                waitFor("Offline details readable") { var found = false; detail.onActivity {
                    found = texts(it.findViewById(android.R.id.content)).contains("Workbench fixture complete")
                }; found }
            }
        }
    }
    @Test fun stopButtonCancelsPendingModelWithoutStartingAnotherTask() = withFixture(Model(true)) { _, model ->
        ActivityScenario.launch(LauncherActivity::class.java).use { scenario ->
            enter(scenario, "Stop fixture")
            waitFor("Model started") { model.calls.get() == 1 }
            waitUi(scenario, "Stop button visible") { it.findViewById<Button>(R.id.workbench_stop).visibility == View.VISIBLE }
            scenario.onActivity { it.findViewById<Button>(R.id.workbench_stop).performClick() }
            waitUi(scenario, "Cancelled") { it.findViewById<TextView>(R.id.workbench_state).text == it.getString(R.string.run_cancelled) }
            assertEquals(1, model.calls.get())
        }
    }
    @Test fun fullHistoryCrossesBinderReplaysExportsAndRerunsWithoutExecuting() = withFixture(Model(true)) { link, model ->
        val started = AgentConnection.decode(link.startRun(bundle(C.KEY_RUN_REQUEST_JSON,
            """{"goal":"History acceptance fixture","options":{"interaction":"plugin"}}"""), null))
        val id = started.string("runId")!!
        val privateText = "Private fixture address 13800123456 " + "a".repeat(3400)
        val connected = CountDownLatch(1); var endpoint: IRunHistory? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) { endpoint = IRunHistory.Stub.asInterface(service); connected.countDown() }
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        assertTrue(context.bindService(Intent(context, AgentLocalService::class.java).setAction(HistoryEndpoint.ACTION), connection, Context.BIND_AUTO_CREATE))
        fun query(operation: String): com.google.gson.JsonObject {
            val latch = CountDownLatch(1); var value: com.google.gson.JsonObject? = null; var failure: Throwable? = null
            endpoint!!.query(bundle(C.KEY_RUN_REQUEST_JSON, jsonObject("operation" to operation.json(), "runId" to id.json()).toString()), object : IRunHistoryCallback.Stub() {
                override fun onResult(response: Bundle) {
                    try {
                        assertNull(response.getString(C.KEY_ERROR_CODE))
                        if (operation == "get") assertTrue("Large history uses a descriptor", response.containsKey(C.KEY_PAYLOAD_FD))
                        value = AgentWire.take(response, C.KEY_RUN_RESPONSE_JSON, C.KEY_PAYLOAD_FD, 32768, RunHistoryCodec.MAX_BYTES).use { AgentJson.objectOf(it.read(), RunHistoryCodec.MAX_BYTES, 131072) }
                    } catch (t: Throwable) { failure = t } finally { latch.countDown() }
                }
            })
            assertTrue(latch.await(15, TimeUnit.SECONDS)); failure?.let { throw it }; return value!!
        }
        try {
            assertTrue(connected.await(15, TimeUnit.SECONDS))
            waitFor("Initial history model call") { model.calls.get() == 1 && model.held != null }
            model.finish("private-rejected-model-body")
            for (step in 1..12) {
                waitFor("Model call $step") { model.calls.get() == step + 1 && model.held != null }
                model.finish("""{"kind":"ask","ask":{"kind":"text","question":"History question $step?"}}""")
                var pending: com.google.gson.JsonObject? = null
                waitFor("Question $step") {
                    pending = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))).getAsJsonObject("pending")
                    pending?.flag("submitted") != true && pending?.string("question") == "History question $step?"
                }
                AgentConnection.decode(link.respond(bundle(C.KEY_RUN_RESPONSE_JSON, jsonObject("runId" to id.json(), "requestId" to pending!!.string("requestId")!!.json(), "value" to privateText.json()).toString())))
            }
            waitFor("Final model call") { model.calls.get() == 14 && model.held != null }; model.finish(completed)
            waitFor("Settled") { AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))).string("state") == "completed" }
            val full = query("get"); assertEquals(13, full.getAsJsonArray("steps").size())
            assertEquals(listOf("DECISION_UNPARSABLE"), full.getAsJsonArray("steps")[0].asJsonObject
                .getAsJsonObject("decision").getAsJsonArray("rejections").map { it.asString })
            assertFalse(full.toString().contains("private-rejected-model-body"))
            val projected = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}""")))
            assertTrue(projected.flag("truncated") == true)
            ActivityScenario.launch<RunDetailActivity>(Intent(context, RunDetailActivity::class.java).putExtra("runId", id)).use { detail ->
                waitFor("Full timeline rendered") { var found = false; detail.onActivity {
                    val all = texts(it.findViewById(android.R.id.content))
                    found = all.any { text -> text.contains("History question 1?") } && all.contains("Workbench fixture complete") &&
                        all.contains(it.getString(R.string.history_rejections, "DECISION_UNPARSABLE"))
                }; found }
                detail.onActivity { it.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<Button>("observation-1").performClick() }
                detail.recreate()
                waitFor("Expanded observation survives recreation") { var found = false; detail.onActivity {
                    found = texts(it.findViewById(android.R.id.content)).any { text -> text.contains(privateText) }
                }; found }
                val monitor = instrumentation.addMonitor(LauncherActivity::class.java.name, null, false)
                try {
                    detail.onActivity { it.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<Button>("rerun").performClick() }
                    val launcher = instrumentation.waitForMonitorWithTimeout(monitor, 10000) as LauncherActivity
                    instrumentation.runOnMainSync {
                        assertEquals("History acceptance fixture", launcher.findViewById<EditText>(R.id.workbench_goal).text.toString())
                        launcher.finish()
                    }
                    assertEquals(14, model.calls.get())
                } finally { instrumentation.removeMonitor(monitor) }
            }
            val file = java.io.File.createTempFile("p62-export-", ".json", context.cacheDir)
            try {
                file.outputStream().use { RunDetailActivity.writeExport(it, query("export")) }
                assertTrue(file.length() > 0)
                val text = file.readText(); assertTrue(text.contains("\"redacted\": true")); assertFalse(text.contains("13800123456"))
                assertTrue(text.contains("DECISION_UNPARSABLE")); assertFalse(text.contains("private-rejected-model-body"))
            } finally { file.delete() }
            ActivityScenario.launch(HistoryActivity::class.java).use { history ->
                waitFor("History entry") { var found = false; history.onActivity {
                    found = texts(it.findViewById(android.R.id.content)).any { text -> text.contains("History acceptance fixture") }
                }; found }
                history.recreate()
            }
            query("delete")
            val missing = link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))
            assertEquals(C.ERROR_RUN_NOT_FOUND, missing.getString(C.KEY_ERROR_CODE))
        } finally { context.unbindService(connection) }
    }
    @Test fun missingHostGuidanceDisablesTaskAdmissionAndPreservesDraft() = withFixture { _, _ ->
        ActivityScenario.launch(LauncherActivity::class.java).use { scenario ->
            scenario.onActivity { it.hostReader = { null }; it.findViewById<EditText>(R.id.workbench_goal).setText("Draft only") }
            waitUi(scenario, "Missing host guidance") {
                // A fresh host-appearance snapshot may recreate the activity before its first poll.
                // Apply this test's package fixture to the current instance, including that replacement.
                it.hostReader = { null }
                !it.findViewById<Button>(R.id.workbench_send).isEnabled &&
                    it.findViewById<TextView>(R.id.launcher_host_status).text == it.getString(R.string.launcher_host_missing, 5289L)
            }
            scenario.recreate()
            scenario.onActivity { assertEquals("Draft only", it.findViewById<EditText>(R.id.workbench_goal).text.toString()) }
        }
    }
    @Test fun removedRerunPresetDoesNotSilentlySelectDefault() = withFixture { _, model ->
        ActivityScenario.launch<LauncherActivity>(Intent(context, LauncherActivity::class.java)
            .putExtra("rerunGoal", "Only prepare this draft").putExtra("rerunPreset", "removed-preset")).use { scenario ->
            waitUi(scenario, "Missing preset preserved") {
                it.findViewById<Spinner>(R.id.workbench_preset).selectedItem == "removed-preset" &&
                    it.findViewById<TextView>(R.id.workbench_error).text == it.getString(R.string.history_preset_unavailable)
            }
            scenario.onActivity { assertFalse(it.findViewById<Button>(R.id.workbench_send).isEnabled) }
            scenario.recreate()
            waitUi(scenario, "Preset survives recreation") { it.findViewById<Spinner>(R.id.workbench_preset).selectedItem == "removed-preset" }
            assertEquals(0, model.calls.get())
        }
    }
    @Test fun confirmationCardRepliesOnceAndCannotAnswerForScriptOwner() {
        instrumentation.runOnMainSync {
            val layout = LinearLayout(context); var replies = 0
            val card = PendingCard(layout) { body, done ->
                replies++; assertFalse(body["allowed"].asBoolean); assertEquals("once", body.string("scope")); done(true)
            }
            val row = AgentJson.objectOf("""{"runId":"test","interaction":"plugin","pending":{"requestId":"request","type":"confirmation","tool":"ui_click","description":"Fixture confirmation","arguments":{}}}""")
            card.render(row)
            (0 until layout.childCount).map { layout.getChildAt(it) }.filterIsInstance<Button>().last().performClick()
            assertEquals(1, replies)
            assertTrue((0 until layout.childCount).map { layout.getChildAt(it) }.filterIsInstance<Button>().all { !it.isEnabled })
            row.addProperty("interaction", "script"); card.render(row)
            assertEquals(1, layout.childCount); assertTrue(layout.getChildAt(0) is TextView)
        }
    }
    @Test fun appearanceSnapshotRejectsUnknownProtocolsAndSupportsRtlNight() {
        val value = Bundle().apply {
            putInt(S.KEY_PROTOCOL_VERSION, S.PROTOCOL_VERSION); putString(S.KEY_HOST_PACKAGE_NAME, S.HOST_PACKAGE_NAME)
            putString(S.KEY_RESOLVED_LANGUAGE_TAG, "ar"); putBoolean(S.KEY_DARK_MODE_ACTIVE, true)
            putInt(S.KEY_THEME_COLOR_PRIMARY, 0xff334455.toInt()); putInt(S.KEY_THEME_COLOR_ACCENT, 0xff445566.toInt())
        }
        val appearance = checkNotNull(HostAppearance.decode(value))
        val config = appearance.wrap(context).resources.configuration
        assertEquals(View.LAYOUT_DIRECTION_RTL, config.layoutDirection)
        assertEquals(android.content.res.Configuration.UI_MODE_NIGHT_YES, config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)
        value.putInt(S.KEY_PROTOCOL_VERSION, 999); assertNull(HostAppearance.decode(value))
    }
    @Test fun legacyHistoryMigratesAllRecordsAndKeepsStartTimeOrdering() {
        val directory = java.io.File(context.cacheDir, "p61-history-${java.util.UUID.randomUUID()}").apply { check(mkdirs()) }
        val stamp = System.currentTimeMillis()
        for (index in 1..21) {
            val id = "00000000-0000-0000-0000-" + index.toString().padStart(12, '0')
            java.io.File(directory, "$id.json").apply {
                writeText("""{"runId":"$id","goal":"History fixture $index","state":"completed","startedAt":$index,"detached":false,"preset":"default","steps":[]}""")
                check(setLastModified(stamp - index * 1000))
            }
        }
        val archive = RunArchive(java.io.File(directory, "runs"), directory)
        // Drain the private writer before asserting retention and deleting only this fixture's directory.
        val disk = RunArchive::class.java.getDeclaredField("disk").apply { isAccessible = true }.get(archive) as java.util.concurrent.ExecutorService
        try {
            waitFor("History loaded") { archive.ready }
            disk.submit {}.get(10, TimeUnit.SECONDS)
            val rows = archive.list(20, 0).getAsJsonArray("runs")
            assertEquals(20, rows.size())
            assertFalse(archive.storageFailed)
            assertEquals(21L, archive.list(200, 0).number("total"))
            assertEquals(setOf("runs", "total", "ready"), archive.list(20, 0).keySet())
            assertTrue(java.io.File(directory, "runs/index.json").isFile)
            assertTrue(directory.listFiles { file -> file.name.endsWith(".json") }!!.isEmpty())
            assertEquals(21L, rows.first().asJsonObject.number("startedAt"))
            assertEquals(2L, rows.last().asJsonObject.number("startedAt"))
        } finally {
            disk.shutdown(); assertTrue(disk.awaitTermination(10, TimeUnit.SECONDS))
            assertEquals(context.cacheDir.canonicalFile, directory.canonicalFile.parentFile)
            directory.deleteRecursively()
        }
    }
    @Test fun restartedTasksBecomeBlockedAndClearingDoesNotResurrectDirtyRecords() {
        val directory = java.io.File(context.cacheDir, "p62-history-${java.util.UUID.randomUUID()}").apply { check(mkdirs()) }
        val id = java.util.UUID.randomUUID().toString()
        val run = AgentJson.objectOf("""{"runId":"$id","goal":"Crash fixture","state":"waiting_input","startedAt":1,"preset":"default","steps":[],"pending":{"question":"Unanswered"}}""")
        java.io.File(directory, "$id.json").writeText(RunHistoryCodec.encode(run, 1))
        val archive = RunArchive(directory)
        val disk = RunArchive::class.java.getDeclaredField("disk").apply { isAccessible = true }.get(archive) as java.util.concurrent.ExecutorService
        try {
            waitFor("History loaded") { archive.ready }; disk.submit {}.get(10, TimeUnit.SECONDS)
            assertFalse(archive.storageFailed)
            assertEquals("blocked", archive.full(id)?.string("state")); assertFalse(archive.full(id)!!.has("pending"))
            archive.journal(id, jsonObject("steps" to com.google.gson.JsonArray()))
            val cleared = CountDownLatch(1); var success = false
            archive.history({ archive.remove(null); com.google.gson.JsonObject() }) { success = it.isSuccess; cleared.countDown() }
            assertTrue(cleared.await(10, TimeUnit.SECONDS)); assertTrue(success)
            disk.submit {}.get(10, TimeUnit.SECONDS)
            assertNull(archive.full(id)); assertFalse(java.io.File(directory, "$id.json").exists())
            assertTrue(io.github.supermonster003.autojs6.plugin.ai.agent.store.RunHistoryStore(directory).open().isEmpty())
        } finally {
            disk.shutdown(); assertTrue(disk.awaitTermination(10, TimeUnit.SECONDS))
            assertEquals(context.cacheDir.canonicalFile, directory.canonicalFile.parentFile); directory.deleteRecursively()
        }
    }
    @Test fun arabicNightLayoutKeepsLargeTextAndControlsWithinScrollableWidth() {
        instrumentation.runOnMainSync {
            val wrapped = HostAppearance("ar", true, 0xff334455.toInt(), 0xffeeddcc.toInt()).wrap(context)
            val large = wrapped.createConfigurationContext(android.content.res.Configuration(wrapped.resources.configuration).apply { fontScale = 2f })
            val themed = android.view.ContextThemeWrapper(large, R.style.Theme_AiAgent_Dark)
            // Match the activity's content root: direction follows the host context, not the process locale.
            val parent = FrameLayout(themed).apply { layoutDirection = large.resources.configuration.layoutDirection }
            val view = android.view.LayoutInflater.from(themed).inflate(R.layout.activity_launcher, parent, false)
            parent.addView(view)
            val width = (360 * context.resources.displayMetrics.density).toInt()
            parent.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY))
            parent.layout(0, 0, width, 640)
            assertEquals(View.LAYOUT_DIRECTION_RTL, view.layoutDirection)
            assertTrue(view.findViewById<EditText>(R.id.workbench_goal).textSize >= 30 * context.resources.displayMetrics.density)
            for (id in listOf(R.id.workbench_goal, R.id.workbench_send, R.id.workbench_voice, R.id.launcher_connect)) {
                val field = view.findViewById<View>(id)
                assertTrue(field.width in 1..width); assertTrue(field.height > 0)
            }
            assertTrue((view as ScrollView).getChildAt(0).height > view.height)
        }
    }
    private inner class PresetsClient : AutoCloseable {
        private var endpoint: IPresetStore? = null
        private val connected = CountDownLatch(1)
        private val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) { endpoint = IPresetStore.Stub.asInterface(service); connected.countDown() }
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        init {
            assertTrue(context.bindService(Intent(context, AgentLocalService::class.java).setAction(PresetEndpoint.ACTION), connection, Context.BIND_AUTO_CREATE))
            assertTrue(connected.await(15, TimeUnit.SECONDS))
        }
        fun query(operation: String, value: com.google.gson.JsonObject = com.google.gson.JsonObject()): Result<com.google.gson.JsonObject> {
            value.addProperty("operation", operation)
            val latch = CountDownLatch(1); var result: Result<com.google.gson.JsonObject>? = null
            endpoint!!.query(bundle(C.KEY_RUN_REQUEST_JSON, value.toString()), object : IPresetStoreCallback.Stub() {
                override fun onResult(response: Bundle) {
                    result = runCatching {
                        response.getString(C.KEY_ERROR_CODE)?.let { AgentWire.closeDescriptors(response); error(it) }
                        AgentWire.take(response, C.KEY_RUN_RESPONSE_JSON, C.KEY_PAYLOAD_FD, 32768, PresetEndpoint.MAX_RESPONSE_BYTES)
                            .use { AgentJson.objectOf(it.read(), PresetEndpoint.MAX_RESPONSE_BYTES) }
                    }
                    latch.countDown()
                }
            })
            assertTrue(latch.await(20, TimeUnit.SECONDS)); return checkNotNull(result)
        }
        fun save(preset: Preset, create: Boolean = true) = query("save", jsonObject("preset" to PresetCodec.encodePreset(preset), "create" to create.json()))
        fun named(operation: String, name: String) = query(operation, jsonObject("name" to name.json()))
        override fun close() { context.unbindService(connection) }
    }
    @Test fun presetEditorCreatesRestoresAndStartsWithSelectedTargetContextAndBudget() = withFixture(Model(true)) { link, model ->
        PresetsClient().use { client ->
            val key = "ui-${java.util.UUID.randomUUID()}"
            try {
                ActivityScenario.launch(PresetsActivity::class.java).use { scenario ->
                    fun ready(message: String, check: (PresetsActivity) -> Boolean) = waitFor(message) { var ok = false; scenario.onActivity { ok = check(it) }; ok }
                    ready("Preset list loaded") { it.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<Button>("preset-new") != null }
                    scenario.onActivity { activity ->
                        val root = activity.findViewById<ViewGroup>(android.R.id.content)
                        root.findViewWithTag<Button>("preset-new").performClick()
                        root.findViewWithTag<EditText>("preset-name").setText(key)
                        root.findViewWithTag<EditText>("preset-context").setText("P63 fixture fixed context")
                        root.findViewWithTag<EditText>("preset-maxSteps").setText("3")
                        root.findViewWithTag<EditText>("preset-maxTotalTokens").setText("10000")
                        root.findViewWithTag<CheckBox>("preset-inherit-groups").isChecked = false
                        root.findViewWithTag<Spinner>("preset-confirm").setSelection(1)
                        root.findViewWithTag<Spinner>("preset-memory").setSelection(3)
                    }
                    ready("Host model catalog shown with locality and fallback") { activity ->
                        val spinner = activity.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<Spinner>("preset-target")
                        spinner.count == 2 && spinner.getItemAtPosition(1).toString().contains(activity.getString(R.string.presets_degraded)) &&
                            spinner.getItemAtPosition(1).toString().contains(activity.getString(R.string.presets_remote))
                    }
                    scenario.onActivity { it.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<Spinner>("preset-target").setSelection(1) }
                    instrumentation.waitForIdleSync(); scenario.recreate()
                    ready("Editor draft and model selection restored") { activity ->
                        val root = activity.findViewById<ViewGroup>(android.R.id.content)
                        root.findViewWithTag<EditText>("preset-context")?.text?.toString() == "P63 fixture fixed context" &&
                            root.findViewWithTag<Spinner>("preset-target")?.selectedItem?.toString()?.contains("Workbench fixture model") == true &&
                            root.findViewWithTag<Spinner>("preset-confirm")?.selectedItemPosition == 1
                    }
                    scenario.onActivity { activity ->
                        val root = activity.findViewById<ViewGroup>(android.R.id.content)
                        assertEquals("3", root.findViewWithTag<EditText>("preset-maxSteps").text.toString())
                        assertEquals(3, root.findViewWithTag<Spinner>("preset-memory").selectedItemPosition)
                        assertFalse(root.findViewWithTag<CheckBox>("preset-group-shell").isEnabled)
                        root.findViewWithTag<Button>("preset-save").performClick()
                    }
                    ready("Saved preset appears") { it.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<Button>("preset-$key") != null }
                }
                val saved = PresetCodec.decodePreset(client.named("get", key).getOrThrow())
                assertEquals("workbench:fixture", saved.targetId); assertEquals("none", saved.memoryScope)
                assertEquals(setOf("observe"), saved.toolGroups); assertEquals("cautious", saved.confirmPolicy)
                assertTrue(AgentConnection.decode(link.listPresets(bundle(C.KEY_RUN_REQUEST_JSON))).getAsJsonArray("presets").any { it.asJsonObject.string("id") == key })
                ActivityScenario.launch<LauncherActivity>(Intent(context, LauncherActivity::class.java).putExtra("rerunPreset", key)).use { scenario ->
                    enter(scenario, "Preset UI fixture goal")
                    waitFor("Preset model started") { model.calls.get() == 1 && model.held != null }
                    assertEquals("workbench:fixture", model.requests.single().getString("targetId"))
                    assertTrue(model.requests.single().toString().contains("P63 fixture fixed context"))
                    val rows = AgentConnection.decode(link.listRuns(bundle(C.KEY_RUN_REQUEST_JSON))).getAsJsonArray("runs")
                    val id = rows.first { it.asJsonObject.string("preset") == key }.asJsonObject.string("runId")!!
                    val run = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}""")))
                    assertEquals(3, run.getAsJsonObject("budget").number("maxSteps")!!.toInt())
                    model.finish(completed)
                    waitUi(scenario, "Preset run completed") { it.findViewById<TextView>(R.id.workbench_state).text == it.getString(R.string.run_completed) }
                }
            } finally { client.named("delete", key) }
        }
    }
    @Test fun privatePresetCrudDefaultAndQueuedSnapshotSurviveEditsAndDeletion() = withFixture(Model(true)) { link, model ->
        PresetsClient().use { client ->
            val oldDefault = client.query("list").getOrThrow().string("defaultName")!!
            val first = "store-${java.util.UUID.randomUUID()}"; val second = "copy-${java.util.UUID.randomUUID()}"
            try {
                assertTrue(client.named("delete", "default").isFailure)
                assertTrue(client.save(Preset(first, toolGroups = setOf("shell"))).isFailure)
                val preset = Preset(first, toolGroups = setOf("observe"), context = "Original queued context")
                client.save(preset).getOrThrow(); assertTrue(client.save(preset).isFailure)
                val copied = PresetCodec.decodePreset(client.named("get", first).getOrThrow()).copy(name = second)
                client.save(copied).getOrThrow(); client.named("default", second).getOrThrow()
                val prefs = context.getSharedPreferences("workbench", Context.MODE_PRIVATE)
                prefs.edit().putString("goal", "").commit()
                ActivityScenario.launch(LauncherActivity::class.java).use { scenario ->
                    waitUi(scenario, "Workbench honors chosen default") { it.findViewById<Spinner>(R.id.workbench_preset).selectedItem == second }
                }
                val firstRun = AgentConnection.decode(link.startRun(bundle(C.KEY_RUN_REQUEST_JSON, """{"goal":"First preset fixture"}"""), null)).string("runId")!!
                waitFor("First run preparing") { model.calls.get() == 1 && model.held != null }
                val queued = AgentConnection.decode(link.startRun(bundle(C.KEY_RUN_REQUEST_JSON, """{"goal":"Queued preset fixture"}"""), null)).string("runId")!!
                client.save(copied.copy(context = "Edited after admission"), false).getOrThrow()
                client.named("delete", second).getOrThrow()
                assertEquals("default", client.query("list").getOrThrow().string("defaultName"))
                model.finish(completed)
                waitFor("Queued run uses admitted snapshot") { model.calls.get() == 2 && model.held != null }
                assertTrue(model.requests[1].toString().contains("Original queued context"))
                assertFalse(model.requests[1].toString().contains("Edited after admission"))
                model.finish(completed)
                for (id in listOf(firstRun, queued)) waitFor("Settled preset run") {
                    AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))).string("state") == "completed"
                }
                assertEquals(C.ERROR_INVALID_REQUEST, link.startRun(bundle(C.KEY_RUN_REQUEST_JSON,
                    """{"goal":"Deleted preset must not run","options":{"preset":"$second"}}"""), null).getString(C.KEY_ERROR_CODE))
            } finally {
                client.named("default", oldDefault); client.named("delete", second); client.named("delete", first)
            }
        }
    }
    @Test fun largePresetPayloadCrossesPrivateBinderAndMissingTargetFailsWithoutFallback() = withFixture { link, model ->
        PresetsClient().use { client ->
            val key = "payload-${java.util.UUID.randomUUID()}"
            try {
                val large = Preset(key, targetId = "profile:removed", context = "\u0000".repeat(8192))
                client.save(large).getOrThrow()
                assertEquals(large, PresetCodec.decodePreset(client.named("get", key).getOrThrow()))
                val targets = client.query("targets").getOrThrow().getAsJsonArray("targets")
                assertEquals("REMOTE", targets.single().asJsonObject.string("locality"))
                assertFalse(targets.single().asJsonObject.flag("structuredJson")!!)
                val id = AgentConnection.decode(link.startRun(bundle(C.KEY_RUN_REQUEST_JSON,
                    """{"goal":"Missing model fixture","options":{"preset":"$key"}}"""), null)).string("runId")!!
                waitFor("Missing target fails closed") {
                    val run = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}""")))
                    run.string("state") == "failed" && run.toString().contains("TARGET_UNAVAILABLE")
                }
                assertEquals(0, model.calls.get())
            } finally { client.named("delete", key) }
        }
    }
    private inner class MemoriesClient : AutoCloseable {
        private var endpoint: IMemoryStore? = null
        private val connected = CountDownLatch(1)
        var usedDescriptor = false
        private val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) { endpoint = IMemoryStore.Stub.asInterface(service); connected.countDown() }
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        init {
            assertTrue(context.bindService(Intent(context, AgentLocalService::class.java).setAction(MemoryEndpoint.ACTION), connection, Context.BIND_AUTO_CREATE))
            assertTrue(connected.await(15, TimeUnit.SECONDS))
        }
        fun query(operation: String, data: com.google.gson.JsonObject = com.google.gson.JsonObject()): Result<com.google.gson.JsonObject> {
            data.addProperty("operation", operation)
            val latch = CountDownLatch(1); var result: Result<com.google.gson.JsonObject>? = null
            endpoint!!.query(bundle(C.KEY_RUN_REQUEST_JSON, data.toString()), object : IMemoryStoreCallback.Stub() {
                override fun onResult(response: Bundle) {
                    result = runCatching {
                        response.getString(C.KEY_ERROR_CODE)?.let { AgentWire.closeDescriptors(response); error(it) }
                        usedDescriptor = response.containsKey(C.KEY_PAYLOAD_FD)
                        AgentWire.take(response, C.KEY_RUN_RESPONSE_JSON, C.KEY_PAYLOAD_FD, 32768, MemoryEndpoint.MAX_RESPONSE_BYTES)
                            .use { AgentJson.objectOf(it.read(), MemoryEndpoint.MAX_RESPONSE_BYTES) }
                    }; latch.countDown()
                }
            })
            assertTrue(latch.await(20, TimeUnit.SECONDS)); return checkNotNull(result)
        }
        fun rows() = query("list").getOrThrow().getAsJsonArray("entries").map { MemoryCodec.decodeEntry(it.asJsonObject) }
        fun save(row: MemoryEntry, before: MemoryEntry? = null, imported: Boolean = true) = query("save", jsonObject("entry" to MemoryCodec.entry(row),
            "before" to (before?.let(MemoryCodec::entry) ?: com.google.gson.JsonNull.INSTANCE), "imported" to imported.json()))
        fun delete(row: MemoryEntry) = query("delete", jsonObject("entry" to MemoryCodec.entry(row)))
        override fun close() { context.unbindService(connection) }
    }
    private fun cardButtons(card: LinearLayout) = (0 until card.childCount).map { card.getChildAt(it) }.filterIsInstance<Button>().filter { it !is CompoundButton }
    private fun interactionNotification() = context.getSystemService(android.app.NotificationManager::class.java).activeNotifications
        .firstOrNull { it.id == io.github.supermonster003.autojs6.plugin.ai.agent.service.InteractionPresentation.NOTIFICATION_ID }?.notification
    private fun drawFixture(view: View, name: String, top: Int = 0, height: Int = view.height) {
        // Capture only controlled fixture views; production confirmation windows remain FLAG_SECURE.
        val bitmap = android.graphics.Bitmap.createBitmap(view.width, height, android.graphics.Bitmap.Config.ARGB_8888)
        try {
            val colors = view.context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
            bitmap.eraseColor(colors.getColor(0, android.graphics.Color.BLACK)); colors.recycle()
            view.draw(android.graphics.Canvas(bitmap).apply { translate(0f, -top.toFloat()) })
            java.io.File(context.cacheDir, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }
    private fun openInteractionNotification(): ConfirmationActivity {
        val notification = checkNotNull(interactionNotification())
        assertEquals(android.app.Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertNotNull(notification.publicVersion); assertEquals(1, notification.actions.size)
        if (Build.VERSION.SDK_INT >= 26) assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH,
            context.getSystemService(android.app.NotificationManager::class.java).getNotificationChannel(notification.channelId).importance)
        val monitor = instrumentation.addMonitor(ConfirmationActivity::class.java.name, null, false)
        val automation = instrumentation.uiAutomation
        val originalFlags = automation.serviceInfo.flags
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = originalFlags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        var opened = false
        var diagnostics = ""
        try {
            instrumentation.waitForIdleSync()
            assertTrue(automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS))
            runCatching { automation.waitForIdle(500, 5000) }
            val title = notification.extras.getString(android.app.Notification.EXTRA_TITLE)!!
            waitFor("Notification can be opened by user tap") {
                val roots = listOfNotNull(automation.rootInActiveWindow) + automation.windows.mapNotNull { it.root }
                val queue = ArrayDeque(roots)
                val nodes = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
                var inspected = 0
                while (queue.isNotEmpty() && inspected++ < 1000) {
                    val node = queue.removeFirst()
                    if (node.text?.toString() == title) nodes += node
                    for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
                }
                diagnostics = "roots=${roots.map { it.packageName.toString() + ":" + it.childCount }}, nodes=$inspected, matches=${nodes.size}, flags=${automation.serviceInfo.flags}, capabilities=${automation.serviceInfo.capabilities}"
                nodes.any { candidate ->
                    var clickable: android.view.accessibility.AccessibilityNodeInfo? = candidate
                    while (clickable != null && !clickable.isClickable) clickable = clickable.parent
                    if (clickable?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true) return@any true
                    // Notification rows need not expose an accessibility click action on every Android version.
                    val bounds = android.graphics.Rect().also(candidate::getBoundsInScreen)
                    if (!candidate.isVisibleToUser || bounds.isEmpty) false else {
                        val now = SystemClock.uptimeMillis()
                        val down = android.view.MotionEvent.obtain(now, now, android.view.MotionEvent.ACTION_DOWN, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                        val up = android.view.MotionEvent.obtain(now, now + 80, android.view.MotionEvent.ACTION_UP, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                        down.source = android.view.InputDevice.SOURCE_TOUCHSCREEN; up.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                        try { automation.injectInputEvent(down, true) && automation.injectInputEvent(up, true) }
                        finally { down.recycle(); up.recycle() }
                    }
                }
            }
            val activity = monitor.waitForActivityWithTimeout(15000)
            assertNotNull("Notification tap opens its activity", activity)
            return (activity as ConfirmationActivity).also { opened = true }
        } catch (failure: AssertionError) {
            throw AssertionError("Notification route: $diagnostics", failure)
        } finally {
            if (!opened) automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            automation.serviceInfo = automation.serviceInfo.apply { flags = originalFlags }
            instrumentation.removeMonitor(monitor)
        }
    }
    @Test fun rememberedAnswerSurvivesRecreationAndRequiresItsOwnInlineApproval() = withFixture(Model(true), listOf("observe", "memory")) { link, model ->
        MemoriesClient().use { memory ->
            val key = "answer-${java.util.UUID.randomUUID()}"
            try { ActivityScenario.launch(LauncherActivity::class.java).use { scenario ->
                enter(scenario, "P65 answer fixture")
                waitFor("First model call") { model.held != null }
                model.finish("""{"kind":"ask","ask":{"kind":"text","question":"Fixture destination?","memoryKey":"$key"}}""")
                waitUi(scenario, "Remember checkbox") { it.findViewById<CheckBox>(R.id.interaction_remember)?.isEnabled == true }
                var countdown = ""
                scenario.onActivity {
                    it.findViewById<EditText>(R.id.workbench_answer).setText("Office fixture")
                    it.findViewById<CheckBox>(R.id.interaction_remember).isChecked = true
                    countdown = it.findViewById<TextView>(R.id.interaction_countdown).text.toString()
                }
                SystemClock.sleep(1500); scenario.recreate()
                waitUi(scenario, "Answer and remember state restored without resetting deadline") {
                    it.findViewById<EditText>(R.id.workbench_answer)?.text?.toString() == "Office fixture" &&
                        it.findViewById<CheckBox>(R.id.interaction_remember)?.isChecked == true &&
                        it.findViewById<TextView>(R.id.interaction_countdown)?.text?.toString() != countdown
                }
                scenario.onActivity { cardButtons(it.findViewById(R.id.workbench_pending)).single().performClick() }
                waitUi(scenario, "Separate memory confirmation") { cardButtons(it.findViewById(R.id.workbench_pending)).size == 2 }
                assertEquals(1, model.calls.get()); assertTrue(memory.rows().none { it.key == key })
                assertNull(interactionNotification())
                scenario.onActivity { cardButtons(it.findViewById(R.id.workbench_pending)).first().performClick() }
                waitFor("Approved preference saved") { memory.rows().any { it.key == key && it.value == "Office fixture" } }
                waitFor("Model receives answer and user proposal history") { model.calls.get() == 2 && model.held != null }
                val row = memory.rows().single { it.key == key }
                val run = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"${row.sourceRunId}"}""")))
                assertEquals("user", run.getAsJsonArray("steps")[1].asJsonObject.getAsJsonObject("decision").string("source"))
                assertTrue(model.requests[1].toString().contains("Office fixture")); model.finish(completed)
            } } finally { memory.rows().filter { it.key == key }.forEach { memory.delete(it) } }
        }
    }
    @Test fun backgroundNotificationOpensExactConfirmationAndDenialInvalidatesOldEntry() = withFixture(Model(true), listOf("memory")) { link, model ->
        MemoriesClient().use { memory -> ActivityScenario.launch(LauncherActivity::class.java).use { scenario ->
            var workbenchTask = -1; scenario.onActivity { workbenchTask = it.taskId }
            val key = "denied-${java.util.UUID.randomUUID()}"
            enter(scenario, "P65 background confirmation")
            waitFor("Model starts") { model.held != null }
            model.finish("""{"kind":"tool","tool":"memory_propose","arguments":{"key":"$key","value":"Denied fixture"}}""")
            waitUi(scenario, "Inline confirmation") { cardButtons(it.findViewById(R.id.workbench_pending)).size == 2 }
            assertNull(interactionNotification())
            val id = AgentConnection.decode(link.status, C.KEY_STATUS_JSON).string("runningRunId")!!
            val pending = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))).getAsJsonObject("pending")
            assertFalse(pending.has("deadlineMs")); assertFalse(pending.has("rememberScope"))
            val oldEntry = ConfirmationActivity.intent(context, id, pending.string("requestId")!!)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            waitFor("Background high priority notification") { interactionNotification() != null }
            val activity = openInteractionNotification()
            try {
                assertNotEquals("Confirmation must not bring the workbench task over the target app", workbenchTask, activity.taskId)
                waitFor("Notification destination renders pending confirmation") { var ready = false; instrumentation.runOnMainSync {
                    ready = activity.findViewById<LinearLayout>(R.id.workbench_pending)?.let { cardButtons(it).size == 2 } == true
                }; ready }
                waitFor("Visible dialog suppresses notification") { interactionNotification() == null }
                instrumentation.runOnMainSync {
                    assertTrue(activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
                    assertTrue("Review buttons accept user input", cardButtons(activity.findViewById(R.id.workbench_pending)).all { it.isEnabled })
                    assertEquals("Opening a notification cannot answer it", 1, model.calls.get())
                    drawFixture(activity.window.decorView, "p65-confirmation-dialog.png")
                    cardButtons(activity.findViewById(R.id.workbench_pending)).last().performClick()
                }
                waitFor("Denial returned to model") { model.calls.get() == 2 && model.held != null }
                waitFor("Answered confirmation closes its separate task") { activity.isFinishing || activity.isDestroyed }
                assertTrue(model.requests.last().toString().contains("USER_DENIED")); assertTrue(memory.rows().none { it.key == key })
                ActivityScenario.launch<ConfirmationActivity>(oldEntry).use { stale ->
                    waitFor("Old notification is non-interactive") { var ready = false; stale.onActivity {
                        ready = cardButtons(it.findViewById(R.id.workbench_pending)).isEmpty() &&
                            texts(it.findViewById(android.R.id.content)).contains(it.getString(R.string.interaction_expired))
                    }; ready }
                }
                model.finish(completed)
            } finally { instrumentation.runOnMainSync { activity.finish() } }
        } }
    }
    @Test fun confirmationCommandCompletionSurvivesConnectionStop() = withFixture { _, _ ->
        val connected = CountDownLatch(1); val entered = CountDownLatch(1)
        val release = CountDownLatch(1); val completed = CountDownLatch(1)
        var acknowledged = false
        val connection = AgentConnection(context) { if (it.status.string("state") == C.LINK_STATE_ATTACHED) connected.countDown() }
        try {
            instrumentation.runOnMainSync { connection.start() }
            assertTrue(connected.await(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                connection.command({ link ->
                    entered.countDown(); check(release.await(10, TimeUnit.SECONDS))
                    link.listRuns(bundle(C.KEY_RUN_REQUEST_JSON, """{"limit":1}"""))
                }, completeWhileStopped = true) { result -> acknowledged = result.isSuccess; completed.countDown() }
            }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { connection.stop() }
            release.countDown()
            assertTrue("Accepted command completion is not lost when a target app stops the confirmation UI", completed.await(10, TimeUnit.SECONDS))
            assertTrue(acknowledged)
        } finally {
            release.countDown()
            instrumentation.runOnMainSync { connection.stop(); connection.close() }
        }
    }
    @Test fun confirmationTimeoutWithdrawsBackgroundNotificationAndNeverSaves() = withFixture(Model(true), listOf("memory")) { link, model ->
        MemoriesClient().use { memory -> ActivityScenario.launch(LauncherActivity::class.java).use { scenario ->
            val key = "timeout-${java.util.UUID.randomUUID()}"
            enter(scenario, "P65 timeout fixture")
            waitFor("First model call") { model.held != null }
            model.finish("""{"kind":"tool","tool":"memory_propose","arguments":{"key":"$key","value":"Expired fixture"}}""")
            waitUi(scenario, "Confirmation countdown") { it.findViewById<TextView>(R.id.interaction_countdown) != null }
            val id = AgentConnection.decode(link.status, C.KEY_STATUS_JSON).string("runningRunId")!!
            val pending = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))).getAsJsonObject("pending")
            assertEquals(120000L, pending.number("timeoutMs"))
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            waitFor("Notification posted") { interactionNotification() != null }
            waitFor("Actual 120-second timeout returned to model", 130000) { model.calls.get() == 2 && model.held != null }
            assertTrue(model.requests.last().toString().contains("USER_TIMEOUT"))
            waitFor("Expired notification withdrawn") { interactionNotification() == null }
            assertTrue(memory.rows().none { it.key == key })
            val response = link.respond(bundle(C.KEY_RUN_RESPONSE_JSON,
                """{"runId":"$id","requestId":"${pending.string("requestId")}","allowed":true}"""))
            assertEquals(C.ERROR_RUN_NOT_INTERACTIVE, response.getString(C.KEY_ERROR_CODE))
            model.finish(completed)
        } }
    }
    @Test fun scriptOwnedQuestionHasNoNotificationAndPluginCannotRespond() = withFixture(Model(true)) { link, model ->
        val id = AgentConnection.decode(link.startRun(bundle(C.KEY_RUN_REQUEST_JSON,
            """{"goal":"P65 script ownership","options":{"interaction":"script"}}"""), null)).string("runId")!!
        waitFor("Script model call") { model.held != null }
        model.finish("""{"kind":"ask","ask":{"kind":"confirm","question":"Script-owned question?"}}""")
        var request = ""
        waitFor("Script input pending") {
            val pending = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))).getAsJsonObject("pending")
            request = pending?.string("requestId").orEmpty(); request.isNotEmpty()
        }
        ActivityScenario.launch<ConfirmationActivity>(ConfirmationActivity.intent(context, id, request)).use { scenario ->
            waitFor("Script ownership shown") { var ready = false; scenario.onActivity {
                ready = texts(it.findViewById(android.R.id.content)).contains(it.getString(R.string.workbench_script_interaction))
                assertTrue(cardButtons(it.findViewById(R.id.workbench_pending)).isEmpty())
            }; ready }
            assertNull(interactionNotification())
            assertEquals(C.ERROR_RUN_NOT_INTERACTIVE, link.respond(bundle(C.KEY_RUN_RESPONSE_JSON,
                """{"runId":"$id","requestId":"$request","value":true}""")).getString(C.KEY_ERROR_CODE))
        }
    }
    @Test fun questionKindsAndRunScopeCardRespectRememberAndPaymentFlags() {
        instrumentation.runOnMainSync {
            val container = LinearLayout(context)
            var response: com.google.gson.JsonObject? = null
            val card = PendingCard(container) { body, done -> response = body; done(true) }
            for (kind in listOf("text", "choice", "confirm")) {
                card.render(AgentJson.objectOf("""{"runId":"fixture","interaction":"plugin","pending":{"requestId":"$kind","type":"input","kind":"$kind","question":"Fixture?","choices":["One","Two"],"memoryKey":"office","rememberScope":"global"}}"""))
                container.findViewById<CheckBox>(R.id.interaction_remember).isChecked = true
                if (kind == "text") container.findViewById<EditText>(R.id.workbench_answer).setText("Typed fixture")
                cardButtons(container).first().performClick()
                assertEquals(true, response?.flag("remember")); assertTrue(response!!.has("value"))
                if (kind == "confirm") assertTrue(response.getAsJsonPrimitive("value").isBoolean)
            }
            for (allowed in listOf(true, false)) {
                card.render(AgentJson.objectOf("""{"runId":"fixture","interaction":"plugin","pending":{"requestId":"scope-$allowed","type":"confirmation","risk":"sensitive","description":"Fixture","arguments":{},"allowRunScope":$allowed}}"""))
                assertEquals(if (allowed) 3 else 2, cardButtons(container).size)
                if (allowed) { cardButtons(container).last().performClick(); assertEquals("run", response?.string("scope")) }
            }
        }
    }
    @Test fun backgroundQuestionFollowsSeparateMemoryReviewAcrossRecreation() = withFixture(Model(true), listOf("memory")) { link, model ->
        MemoriesClient().use { memory -> ActivityScenario.launch(LauncherActivity::class.java).use { workbench ->
            val key = "choice-${java.util.UUID.randomUUID()}"
            try {
                enter(workbench, "P65 background choice")
                waitFor("Question model call") { model.held != null }
                model.finish("""{"kind":"ask","ask":{"kind":"choice","question":"Fixture preference?","choices":["One","Two"],"memoryKey":"$key"}}""")
                waitUi(workbench, "Inline choices") { it.findViewById<CheckBox>(R.id.interaction_remember)?.isEnabled == true }
                val id = AgentConnection.decode(link.status, C.KEY_STATUS_JSON).string("runningRunId")!!
                val pending = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))).getAsJsonObject("pending")
                workbench.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                waitFor("Question notification") { interactionNotification() != null }
                // The route is the same immutable PendingIntent target already tested with a notification tap.
                ActivityScenario.launch<ConfirmationActivity>(ConfirmationActivity.intent(context, id, pending.string("requestId")!!)).use { dialog ->
                    fun ready(message: String, check: (ConfirmationActivity) -> Boolean) = waitFor(message) {
                        var found = false; dialog.onActivity { found = check(it) }; found
                    }
                    ready("Question dialog") { it.findViewById<CheckBox>(R.id.interaction_remember)?.isEnabled == true }
                    dialog.onActivity { it.findViewById<CheckBox>(R.id.interaction_remember).isChecked = true }
                    dialog.recreate()
                    ready("Choice remember state restored") { it.findViewById<CheckBox>(R.id.interaction_remember)?.isChecked == true }
                    dialog.onActivity { cardButtons(it.findViewById(R.id.workbench_pending)).last().performClick() }
                    ready("Separate review in same dialog") {
                        cardButtons(it.findViewById(R.id.workbench_pending)).firstOrNull()?.text == it.getString(R.string.task_allow)
                    }
                    assertTrue(memory.rows().none { it.key == key }); dialog.recreate()
                    ready("Followed confirmation survives recreation") {
                        cardButtons(it.findViewById(R.id.workbench_pending)).firstOrNull()?.text == it.getString(R.string.task_allow)
                    }
                    dialog.onActivity { cardButtons(it.findViewById(R.id.workbench_pending)).first().performClick() }
                    waitFor("Remembered choice saved") { memory.rows().any { it.key == key && it.value == "Two" } }
                    waitFor("Model resumes") { model.calls.get() == 2 && model.held != null }
                }
                model.finish("""{"kind":"ask","ask":{"kind":"confirm","question":"Second fixture preference?","memoryKey":"$key-next"}}""")
                var secondRequest = ""
                waitFor("Second question in same run") {
                    val next = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))).getAsJsonObject("pending")
                    secondRequest = next?.string("requestId").orEmpty()
                    next?.string("type") == "input" && secondRequest != pending.string("requestId")
                }
                ActivityScenario.launch<ConfirmationActivity>(ConfirmationActivity.intent(context, id, secondRequest)).use { dialog ->
                    fun ready(message: String, check: (ConfirmationActivity) -> Boolean) = waitFor(message) {
                        var found = false; dialog.onActivity { found = check(it) }; found
                    }
                    ready("Boolean question") { it.findViewById<CheckBox>(R.id.interaction_remember)?.isEnabled == true }
                    dialog.onActivity {
                        it.findViewById<CheckBox>(R.id.interaction_remember).isChecked = true
                        cardButtons(it.findViewById(R.id.workbench_pending)).last().performClick()
                    }
                    ready("Earlier saved proposal cannot close the new review") {
                        cardButtons(it.findViewById(R.id.workbench_pending)).firstOrNull()?.text == it.getString(R.string.task_allow)
                    }
                    assertTrue(memory.rows().none { it.key == "$key-next" })
                    dialog.onActivity { cardButtons(it.findViewById(R.id.workbench_pending)).first().performClick() }
                    waitFor("Boolean answer saved only after review") { memory.rows().any { it.key == "$key-next" && it.value == "false" } }
                    waitFor("Second answer returns to model") { model.calls.get() == 3 && model.held != null }; model.finish(completed)
                }
            } finally { memory.rows().filter { it.key in setOf(key, "$key-next") }.forEach { memory.delete(it) } }
        } }
    }
    @Test fun confirmationCardArabicNightLargeTextStaysWithinScrollableWidth() {
        instrumentation.runOnMainSync {
            val wrapped = HostAppearance("ar", true, 0xff334455.toInt(), 0xffeeddcc.toInt()).wrap(context)
            val large = wrapped.createConfigurationContext(android.content.res.Configuration(wrapped.resources.configuration).apply { fontScale = 2f })
            val themed = android.view.ContextThemeWrapper(large, R.style.Theme_AiAgent_Dialog_Dark)
            val card = LinearLayout(themed).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            val scroll = ScrollView(themed).apply { addView(card); layoutDirection = View.LAYOUT_DIRECTION_RTL }
            PendingCard(card) { _, _ -> fail("Layout must not answer") }.render(AgentJson.objectOf(
                """{"runId":"fixture","interaction":"plugin","pending":{"requestId":"rtl","type":"confirmation","risk":"sensitive","description":"${"Fixture description ".repeat(30)}","arguments":{"value":"${"Long value ".repeat(80)}"},"allowRunScope":true}}"""))
            val width = (360 * context.resources.displayMetrics.density).toInt()
            scroll.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY))
            scroll.layout(0, 0, width, 640)
            assertEquals(View.LAYOUT_DIRECTION_RTL, card.layoutDirection); assertTrue(card.height > scroll.height)
            assertEquals(android.content.res.Configuration.UI_MODE_NIGHT_YES, themed.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)
            for (button in cardButtons(card)) { assertTrue(button.width in 1..width); assertTrue(button.height > 0) }
            drawFixture(card, "p65-rtl-confirmation-top.png", height = 640)
            drawFixture(card, "p65-rtl-confirmation-bottom.png", top = (card.height - 640).coerceAtLeast(0), height = 640)
        }
    }
    @Test fun confirmedMemoryReachesNextTaskAndDeniedLargeProposalRemainsAnObservation() = withFixture(Model(true), listOf("observe", "memory")) { link, model ->
        MemoriesClient().use { memory -> PresetsClient().use { presets ->
            val name = "memory-${java.util.UUID.randomUUID()}"; val key = "drink-${java.util.UUID.randomUUID()}"
            fun run(id: String) = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}""")))
            fun proposal(value: String, entryKey: String = key) = jsonObject("kind" to "tool".json(), "tool" to "memory_propose".json(),
                "arguments" to jsonObject("key" to entryKey.json(), "value" to value.json(), "scope" to name.json())).toString()
            try {
                presets.save(Preset(name, memoryScope = "preset")).getOrThrow()
                ActivityScenario.launch<LauncherActivity>(Intent(context, LauncherActivity::class.java).putExtra("rerunPreset", name)).use { scenario ->
                    enter(scenario, "P64 memory fixture")
                    waitFor("First memory model") { model.calls.get() == 1 && model.held != null }
                    val id = AgentConnection.decode(link.listRuns(bundle(C.KEY_RUN_REQUEST_JSON))).getAsJsonArray("runs")
                        .first { it.asJsonObject.string("preset") == name }.asJsonObject.string("runId")!!
                    model.finish(proposal("Hot medium latte fixture"))
                    waitUi(scenario, "Memory confirmation shown") { it.findViewById<LinearLayout>(R.id.workbench_pending).let { card ->
                        (0 until card.childCount).map { index -> card.getChildAt(index) }.filterIsInstance<Button>().size == 2 } }
                    val pending = run(id).getAsJsonObject("pending")
                    assertEquals("memory_propose", pending.string("tool")); assertFalse(pending.flag("allowRunScope")!!)
                    assertEquals(name, pending.getAsJsonObject("arguments").string("scope")); assertTrue(memory.rows().none { it.key == key })
                    scenario.onActivity { activity -> val card = activity.findViewById<LinearLayout>(R.id.workbench_pending)
                        (0 until card.childCount).map { card.getChildAt(it) }.filterIsInstance<Button>().first().performClick() }
                    waitFor("Confirmed memory stored") { model.calls.get() == 2 && model.held != null }
                    val saved = memory.rows().single { it.key == key }; assertEquals(id, saved.sourceRunId)
                    assertEquals("Hot medium latte fixture", saved.value); model.finish(completed)
                    waitFor("First memory task settled") { run(id).string("state") == "completed" }
                    enter(scenario, "P64 use remembered preference")
                    waitFor("Next task model") { model.calls.get() == 3 && model.held != null }
                    assertTrue(model.requests[2].toString().contains("Hot medium latte fixture"))
                    val second = AgentConnection.decode(link.listRuns(bundle(C.KEY_RUN_REQUEST_JSON))).getAsJsonArray("runs")
                        .first { it.asJsonObject.string("preset") == name && it.asJsonObject.string("runId") != id }.asJsonObject.string("runId")!!
                    val large = "\u0001".repeat(4096); model.finish(proposal(large, "$key-x"))
                    waitFor("Full large memory confirmation") { run(second).getAsJsonObject("pending")?.string("tool") == "memory_propose" }
                    assertEquals(large, run(second).getAsJsonObject("pending").getAsJsonObject("arguments").string("value"))
                    waitUi(scenario, "Deny large memory") { it.findViewById<LinearLayout>(R.id.workbench_pending).let { card ->
                        (0 until card.childCount).map { index -> card.getChildAt(index) }.filterIsInstance<Button>().size == 2 } }
                    scenario.onActivity { activity -> val card = activity.findViewById<LinearLayout>(R.id.workbench_pending)
                        (0 until card.childCount).map { card.getChildAt(it) }.filterIsInstance<Button>().last().performClick() }
                    waitFor("Denial returned to model") { model.calls.get() == 4 && model.held != null }
                    assertTrue(model.requests[3].toString().contains("USER_DENIED")); assertTrue(memory.rows().none { it.key == "$key-x" })
                    model.finish(completed); waitFor("Denied proposal run settled") { run(second).string("state") == "completed" }
                }
                val disabled = AgentConnection.decode(link.startRun(bundle(C.KEY_RUN_REQUEST_JSON,
                    """{"goal":"P64 disabled injection","options":{"preset":"$name","memory":false}}"""), null)).string("runId")!!
                waitFor("Disabled injection task") { model.calls.get() == 5 && model.held != null }
                assertFalse(model.requests[4].toString().contains("Hot medium latte fixture")); model.finish(completed)
                waitFor("Disabled injection task settled") { run(disabled).string("state") == "completed" }
            } finally { memory.rows().filter { it.scope == name }.forEach { memory.delete(it) }; presets.named("delete", name) }
        } }
    }
    @Test fun memoryImportRequiresEachApprovalAndRestoresPendingReviewWithoutWriting() {
        MemoriesClient().use { client ->
            val prefix = "import-${java.util.UUID.randomUUID()}"
            val first = MemoryEntry(prefix, "Reviewed fixture", "global", java.util.UUID.randomUUID().toString(), 1, 1)
            val second = first.copy(key = "$prefix-x", value = "Skipped fixture")
            try {
                ActivityScenario.launch(MemoryActivity::class.java).use { scenario ->
                    fun ready(message: String, tag: String) = waitFor(message) { var found = false; scenario.onActivity {
                        found = it.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<View>(tag)?.isEnabled == true }; found }
                    ready("Memory loaded", "memory-import")
                    val input = MemoryCodec.encode(listOf(first, second)).byteInputStream()
                    scenario.onActivity { it.beginImport(MemoryActivity.readImport(input)) }
                    ready("First import review", "memory-accept"); assertTrue(client.rows().none { it.key.startsWith(prefix) })
                    scenario.onActivity { it.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<Button>("memory-accept").performClick() }
                    waitFor("One row saved") { client.rows().count { it.key.startsWith(prefix) } == 1 }
                    ready("Second review", "memory-accept"); instrumentation.waitForIdleSync(); scenario.recreate()
                    ready("Pending review restored", "memory-skip")
                    scenario.onActivity { activity ->
                        assertTrue(texts(activity.findViewById(android.R.id.content)).contains(second.value))
                        activity.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<Button>("memory-skip").performClick()
                    }
                    ready("Import finished", "memory-import"); assertTrue(client.rows().none { it.key == second.key })
                }
                val output = java.io.ByteArrayOutputStream(); MemoryActivity.writeExport(output, client.query("export").getOrThrow())
                assertEquals(client.rows().toSet(), MemoryActivity.readImport(output.toByteArray().inputStream()).toSet())
                assertTrue(runCatching { MemoryActivity.readImport(byteArrayOf(0xc3.toByte(), 0x28).inputStream()) }.isFailure)
            } finally { client.rows().filter { it.key.startsWith(prefix) }.forEach { client.delete(it) } }
        }
    }
    @Test fun privateMemoryLargeBackupUsesDescriptorAndRejectsStaleManagementWrites() {
        MemoriesClient().use { client ->
            val prefix = "large-${java.util.UUID.randomUUID()}"
            val first = MemoryEntry(prefix, "\u0001".repeat(4096), "global", java.util.UUID.randomUUID().toString(), 1, 1)
            try {
                client.save(first).getOrThrow(); client.save(first.copy(key = "$prefix-x")).getOrThrow()
                val rows = client.rows(); assertTrue(client.usedDescriptor)
                val saved = rows.single { it.key == prefix }
                val edited = saved.copy(value = "Updated fixture")
                client.save(edited, saved, false).getOrThrow()
                assertTrue(client.save(saved, saved, false).isFailure); assertTrue(client.delete(saved).isFailure)
                val latest = client.rows().single { it.key == prefix }
                assertEquals(saved.sourceRunId, latest.sourceRunId); assertEquals(saved.createdAt, latest.createdAt)
                assertEquals(edited.value, latest.value)
                assertTrue(client.save(first.copy(key = "$prefix-z", scope = "missing-preset-${java.util.UUID.randomUUID()}")).isFailure)
            } finally { client.rows().filter { it.key.startsWith(prefix) }.forEach { client.delete(it) } }
        }
    }
    @Test fun memoryEditorPreservesDraftAndRequiresConfirmationForEditsAndDeletion() {
        MemoriesClient().use { client ->
            val key = "edit-${java.util.UUID.randomUUID()}"
            val entry = MemoryEntry(key, "Original fixture", "global", java.util.UUID.randomUUID().toString(), 1, 1)
            fun acceptDialog() {
                val automation = instrumentation.uiAutomation; val flags = automation.serviceInfo.flags
                automation.serviceInfo = automation.serviceInfo.apply { this.flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS }
                try { waitFor("Memory confirmation dialog") {
                    val node = automation.rootInActiveWindow?.findAccessibilityNodeInfosByViewId("android:id/button1")?.firstOrNull()
                    node?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true
                } } finally { automation.serviceInfo = automation.serviceInfo.apply { this.flags = flags } }
            }
            try {
                client.save(entry).getOrThrow()
                ActivityScenario.launch(MemoryActivity::class.java).use { scenario ->
                    fun ready(message: String, tag: String) = waitFor(message) { var found = false; scenario.onActivity {
                        found = it.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<View>(tag)?.isEnabled == true }; found }
                    fun click(tag: String) = scenario.onActivity { it.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<Button>(tag).performClick() }
                    ready("Entry listed", "memory-entry-global:$key"); click("memory-entry-global:$key")
                    scenario.onActivity { it.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<EditText>("memory-value").setText("Draft fixture") }
                    scenario.recreate(); ready("Editor restored", "memory-value")
                    scenario.onActivity {
                        assertEquals("Draft fixture", it.findViewById<ViewGroup>(android.R.id.content).findViewWithTag<EditText>("memory-value").text.toString())
                    }
                    assertEquals(entry.value, client.rows().single { it.key == key }.value)
                    click("memory-save"); assertEquals(entry.value, client.rows().single { it.key == key }.value); acceptDialog()
                    ready("Saved entry listed", "memory-entry-global:$key")
                    assertEquals("Draft fixture", client.rows().single { it.key == key }.value)
                    click("memory-entry-global:$key"); click("memory-delete")
                    assertTrue(client.rows().any { it.key == key }); acceptDialog()
                    ready("Deletion completed", "memory-import"); assertTrue(client.rows().none { it.key == key })
                }
            } finally { client.rows().filter { it.key == key }.forEach { client.delete(it) } }
        }
    }
    @Test fun memoryManagementUsesArabicNightAppearanceWithinScrollableWidth() {
        val original = HostAppearance.cached
        val release = CountDownLatch(1); val entered = CountDownLatch(1)
        // Hold only the host-appearance refresh; memory IPC and rendering remain real.
        HostAppearance.worker.execute { entered.countDown(); release.await(30, TimeUnit.SECONDS) }
        assertTrue(entered.await(15, TimeUnit.SECONDS))
        HostAppearance.cached = HostAppearance("ar", true, 0xff334455.toInt(), 0xffeeddcc.toInt())
        try {
            ActivityScenario.launch(MemoryActivity::class.java).use { scenario ->
                waitFor("Arabic memory management loaded") { var loaded = false; scenario.onActivity { activity ->
                    val root = activity.findViewById<ViewGroup>(android.R.id.content)
                    loaded = root.findViewWithTag<Button>("memory-import")?.isLaidOut == true
                }; loaded }
                scenario.onActivity { activity ->
                    val root = activity.findViewById<ViewGroup>(android.R.id.content)
                    assertEquals("ar", activity.resources.configuration.locales[0].language)
                    assertEquals(android.content.res.Configuration.UI_MODE_NIGHT_YES, activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    assertEquals(View.LAYOUT_DIRECTION_RTL, (root.getChildAt(0) as ScrollView).getChildAt(0).layoutDirection)
                    for (tag in listOf("memory-refresh", "memory-export", "memory-import")) {
                        val view = root.findViewWithTag<Button>(tag)
                        assertTrue(view.width in 1..root.width); assertTrue(view.height > 0)
                    }
                }
                instrumentation.waitForIdleSync()
                // The screenshot is a visual artifact; allow the platform's entry animation to finish.
                SystemClock.sleep(500)
                instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                    java.io.File(context.cacheDir, "p64-memory-rtl.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            }
        } finally { HostAppearance.cached = original; release.countDown() }
    }
    @Test fun shareAndShortcutOpenDraftsAndEachStartExactlyOneTask() = withFixture { link, model ->
        val automation = instrumentation.uiAutomation
        val monitor = instrumentation.addMonitor(LauncherActivity::class.java.name, null, false)
        try {
            val command = "am start -a android.intent.action.SEND -t text/plain -n ${context.packageName}/.ui.ShareTargetActivity --es android.intent.extra.TEXT Share_acceptance_fixture"
            ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
            instrumentation.waitForMonitorWithTimeout(monitor, 15000) as LauncherActivity
            var launcher: LauncherActivity? = null
            assertEquals(0, model.calls.get())
            waitFor("Share draft with preset selector") { var ready = false; instrumentation.runOnMainSync {
                launcher = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filterIsInstance<LauncherActivity>().firstOrNull()
                ready = launcher?.findViewById<EditText>(R.id.workbench_goal)?.text?.toString() == "Share_acceptance_fixture" &&
                    launcher?.findViewById<Button>(R.id.workbench_send)?.isEnabled == true
            }; ready }
            instrumentation.runOnMainSync { launcher!!.findViewById<Button>(R.id.workbench_send).performClick() }
            waitFor("Share model call") { model.calls.get() == 1 && model.held != null }; model.finish(completed)
            waitFor("Share completes") { AgentConnection.decode(link.status, C.KEY_STATUS_JSON).string("runningRunId") == null }
            instrumentation.runOnMainSync { launcher!!.finish() }
            val entry = TaskEntry("Shortcut acceptance fixture", "default")
            ActivityScenario.launch<LauncherActivity>(TaskEntries.intent(context, entry)).use { scenario ->
                waitUi(scenario, "Shortcut draft") {
                    it.findViewById<EditText>(R.id.workbench_goal).text.toString() == entry.goal && it.findViewById<Button>(R.id.workbench_send).isEnabled
                }
                assertEquals(1, model.calls.get())
                scenario.onActivity { it.findViewById<Button>(R.id.workbench_send).performClick() }
                waitFor("Shortcut model call") { model.calls.get() == 2 }
                waitUi(scenario, "Shortcut completed") { it.findViewById<TextView>(R.id.workbench_state).text == it.getString(R.string.run_completed) }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }
    @Test fun entryParsingRejectsMalformedSharesAndSpeechFillsWithoutRunning() = withFixture { _, model ->
        assertNull(TaskEntries.read(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_TEXT, "bad")))
        assertNull(TaskEntries.read(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "中".repeat(1400))))
        assertNull(TaskEntries.read(Intent(TaskEntries.PRESET_TASK).putExtra("rerunGoal", "missing preset")))
        assertEquals(TaskEntry(""), TaskEntries.read(Intent(TaskEntries.NEW_TASK)))
        val shared = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, android.text.SpannableString("plain text"))
            .putExtra("allowed", true).putExtra("rerunPreset", "untrusted")
        assertEquals(TaskEntry("plain text"), TaskEntries.read(shared))
        ActivityScenario.launch<LauncherActivity>(TaskEntries.intent(context, TaskEntry("Before speech"))).use { scenario ->
            waitUi(scenario, "Attached") { it.findViewById<Button>(R.id.workbench_send).isEnabled }
            scenario.onActivity {
                assertEquals(it.resources.configuration.locales[0].toLanguageTag(), SpeechInput.intent(it).getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))
                if (!SpeechInput.available(it)) assertEquals(View.GONE, it.findViewById<View>(R.id.workbench_voice).visibility)
                LauncherActivity::class.java.getDeclaredMethod("onActivityResult", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Intent::class.java)
                    .apply { isAccessible = true }.invoke(it, SpeechInput.REQUEST, android.app.Activity.RESULT_OK,
                        Intent().putStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS, arrayListOf("Speech draft")))
                assertEquals("Speech draft", it.findViewById<EditText>(R.id.workbench_goal).text.toString())
            }
            assertEquals(0, model.calls.get())
        }
        if (Build.VERSION.SDK_INT >= 25) assertTrue(context.getSystemService(android.content.pm.ShortcutManager::class.java).manifestShortcuts.any { it.id == "new-task" })
    }
    private fun withFloatingSettings(action: () -> Unit) {
        val connected = CountDownLatch(1); var endpoint: IAgentSettings? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) { endpoint = IAgentSettings.Stub.asInterface(service); connected.countDown() }
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        fun query(body: com.google.gson.JsonObject): com.google.gson.JsonObject {
            val latch = CountDownLatch(1); var response: Bundle? = null
            endpoint!!.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, body), object : IPresetStoreCallback.Stub() {
                override fun onResult(result: Bundle?) { response = result; latch.countDown() }
            })
            assertTrue(latch.await(15, TimeUnit.SECONDS)); return AgentConnection.decode(response!!)
        }
        assertTrue(context.bindService(Intent(context, AgentLocalService::class.java).setAction(SettingsEndpoint.ACTION), connection, Context.BIND_AUTO_CREATE))
        var saved: com.google.gson.JsonObject? = null
        val original = shell("appops get ${context.packageName} SYSTEM_ALERT_WINDOW")
        val mode = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(original)?.groupValues?.get(1) ?: "default"
        val prefs = context.getSharedPreferences("floating", Context.MODE_PRIVATE)
        val oldDraft = prefs.all
        try {
            assertTrue(connected.await(15, TimeUnit.SECONDS))
            saved = query(jsonObject("operation" to "get".json())).getAsJsonObject("settings").deepCopy()
            shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
            prefs.edit().clear().commit()
            query(jsonObject("operation" to "save".json(), "settings" to saved.deepCopy().apply { addProperty("floating", true) }))
            action()
        } finally {
            saved?.let { query(jsonObject("operation" to "save".json(), "settings" to it)) }
            shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW $mode")
            val editor = prefs.edit().clear()
            oldDraft.forEach { (key, value) -> when (value) { is String -> editor.putString(key, value); is Float -> editor.putFloat(key, value) } }
            editor.commit(); context.unbindService(connection)
        }
    }
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
        .bufferedReader().use { it.readText() }
    private fun floatingFrame(card: Boolean): android.graphics.Rect? {
        val title = if (card) "AI Agent floating card" else "AI Agent floating ball"
        val dump = shell("dumpsys window windows")
        val lines = dump.lineSequence().dropWhile { !it.contains("Window #") || !it.contains(title) }.drop(1).takeWhile { !it.contains("Window #") }.joinToString("\n")
        if (!lines.contains("isOnScreen=true") || !lines.contains("isVisible=true") || !lines.contains("HAS_DRAWN")) return null
        val match = Regex("(?:mFrame|frame)=\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]").find(lines) ?: return null
        val coordinates = match.groupValues.drop(1).map(String::toInt)
        return android.graphics.Rect(coordinates[0], coordinates[1], coordinates[2], coordinates[3]).takeIf { it.width() > 0 && it.height() > 0 }
    }
    private fun tap(x: Int, y: Int) {
        shell("input tap $x $y")
    }
    @Test fun floatingWindowFramesExpandDraftSurvivesCollapseAndStopsTask() = withFixture { link, model ->
        withFloatingSettings {
            shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard"); shell("input keyevent KEYCODE_HOME")
            SystemClock.sleep(500) // Let the launcher transition finish before injecting a touch.
            waitFor("Floating ball frame") { floatingFrame(false) != null }
            val beforeDrag = checkNotNull(floatingFrame(false))
            shell("input swipe ${beforeDrag.centerX()} ${beforeDrag.centerY()} ${beforeDrag.centerX() - beforeDrag.width() * 2} ${beforeDrag.centerY() + beforeDrag.height()} 400")
            waitFor("Drag changes frame within usable screen") { floatingFrame(false)?.left?.let { it < beforeDrag.left && it >= 0 } == true }
            val ball = checkNotNull(floatingFrame(false))
            assertTrue(ball.top > 0)
            assertFalse(shell("dumpsys activity services ${context.packageName}").contains("isForeground=true"))
            tap(ball.centerX(), ball.centerY())
            waitFor("Floating card frame") { floatingFrame(true)?.width()?.let { it > ball.width() } == true }
            val automation = instrumentation.uiAutomation
            val flags = automation.serviceInfo.flags
            automation.serviceInfo = automation.serviceInfo.apply { this.flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
            fun overlayRoot() = automation.windows.firstOrNull {
                it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM && it.root?.packageName == context.packageName
            }?.root
            try {
                val fieldFrame = checkNotNull(floatingFrame(true))
                SystemClock.sleep(300) // WindowManager animates the old compact surface to the new frame.
                tap(fieldFrame.centerX(), fieldFrame.top + (ball.height() * 2))
                waitFor("Floating text field") {
                    val input = overlayRoot()?.findAccessibilityNodeInfosByViewId("${context.packageName}:id/workbench_goal")?.firstOrNull()
                    input?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                        putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Floating acceptance fixture")
                    }) == true
                }
                val card = checkNotNull(floatingFrame(true))
                tap(card.left + (ball.width() / 2), card.top + (ball.height() / 2))
                waitFor("Collapsed after editing") { floatingFrame(false) != null }
                SystemClock.sleep(300)
                val again = checkNotNull(floatingFrame(false)); tap(again.centerX(), again.centerY())
                waitFor("Reopened card") { floatingFrame(true) != null }
                waitFor("Draft retained") {
                    overlayRoot()?.findAccessibilityNodeInfosByViewId("${context.packageName}:id/workbench_goal")?.firstOrNull()?.text?.toString() == "Floating acceptance fixture"
                }
                val labels = HostAppearance.read(context)?.wrap(context) ?: context
                waitFor("Start from actual overlay") {
                    val node = overlayRoot()?.findAccessibilityNodeInfosByText(labels.getString(R.string.workbench_send))?.firstOrNull { it.isClickable && it.isEnabled }
                    node?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true
                }
                waitFor("Floating model call") { model.calls.get() == 1 }
                val id = AgentConnection.decode(link.status, C.KEY_STATUS_JSON).string("runningRunId")!!
                waitFor("Running ball resized") { floatingFrame(false)?.width()?.let { it > ball.width() } == true }
                SystemClock.sleep(300)
                val running = checkNotNull(floatingFrame(false))
                tap(running.right - ball.width() / 2, running.centerY())
                waitFor("Overlay stop cancels run") { AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))).string("state") == "cancelled" }
                link.detach(bundle(org.autojs.plugin.host.capability.api.HostCapabilityContract.KEY_REASON_JSON, """{"reason":"floating-test"}"""))
                waitFor("Detached overlay hidden") { floatingFrame(false) == null && floatingFrame(true) == null }
            } finally { automation.serviceInfo = automation.serviceInfo.apply { this.flags = flags } }
        }
    }
    @Test fun floatingConfirmationSuppressesOnlyItsVisibleRequestAndRestoresAfterUnlock() = withFixture(Model(true), listOf("memory")) { link, model ->
        withFloatingSettings {
            ActivityScenario.launch(LauncherActivity::class.java).use { scenario ->
                enter(scenario, "P67 floating confirmation fixture")
                waitFor("Model started") { model.held != null }
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                shell("input keyevent KEYCODE_HOME"); SystemClock.sleep(300)
                model.finish("""{"kind":"tool","tool":"memory_propose","arguments":{"key":"p67-denied-fixture","value":"Not stored"}}""")
                waitFor("Collapsed overlay keeps notification") { interactionNotification() != null && floatingFrame(false) != null }
                var frame = checkNotNull(floatingFrame(false)); val size = frame.height()
                tap(frame.left + size / 2, frame.centerY())
                waitFor("Visible confirmation card suppresses notification") { floatingFrame(true) != null && interactionNotification() == null }
                val automation = instrumentation.uiAutomation; val flags = automation.serviceInfo.flags
                automation.serviceInfo = automation.serviceInfo.apply { this.flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
                try {
                    val labels = HostAppearance.read(context)?.wrap(context) ?: context
                    fun root() = automation.windows.firstOrNull { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM && it.root?.packageName == context.packageName }?.root
                    waitFor("Real confirmation buttons in overlay") { root()?.findAccessibilityNodeInfosByText(labels.getString(R.string.task_deny))?.any { it.isClickable } == true }
                    assertTrue(shell("dumpsys window windows").contains("SECURE"))
                    waitFor("Collapse actual confirmation card") {
                        root()?.findAccessibilityNodeInfosByText(labels.getString(R.string.floating_collapse))?.firstOrNull { it.isClickable }
                            ?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true
                    }
                    waitFor("Collapsed card restores notification") { floatingFrame(false) != null && interactionNotification() != null }
                    // A user's PIN/pattern cannot be dismissed by the test harness. Exercise lock
                    // restoration on unsecured test devices while retaining confirmation coverage everywhere.
                    if (!context.getSystemService(android.app.KeyguardManager::class.java).isDeviceSecure) {
                        val position = checkNotNull(floatingFrame(false))
                        shell("input keyevent KEYCODE_SLEEP")
                        waitFor("Lock hides overlay") { floatingFrame(false) == null && floatingFrame(true) == null }
                        shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard")
                        waitFor("Unlock restores remembered position") { floatingFrame(false)?.let { kotlin.math.abs(it.left - position.left) < 3 && kotlin.math.abs(it.top - position.top) < 3 } == true }
                    }
                    SystemClock.sleep(300)
                    frame = checkNotNull(floatingFrame(false)); tap(frame.left + size / 2, frame.centerY())
                    waitFor("Deny in overlay") {
                        root()?.findAccessibilityNodeInfosByText(labels.getString(R.string.task_deny))?.firstOrNull { it.isClickable && it.isEnabled }
                            ?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true
                    }
                    waitFor("Denial reaches runner") { model.calls.get() == 2 && model.held != null }
                    waitFor("Reply collapses the card before the next observation") { floatingFrame(false) != null && floatingFrame(true) == null }
                    assertTrue(model.requests.last().toString().contains("USER_DENIED")); model.finish(completed)
                    waitFor("Completed") { AgentConnection.decode(link.status, C.KEY_STATUS_JSON).string("runningRunId") == null }
                } finally {
                    shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard")
                    automation.serviceInfo = automation.serviceInfo.apply { this.flags = flags }
                }
            }
        }
    }
    private fun texts(view: View): List<String> = when (view) {
        is TextView -> listOf(view.text.toString())
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else -> emptyList()
    }
}
