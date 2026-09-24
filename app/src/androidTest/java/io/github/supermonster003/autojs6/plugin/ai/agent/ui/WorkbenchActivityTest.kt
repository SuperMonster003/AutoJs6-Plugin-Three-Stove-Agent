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
    private fun withFixture(model: Model = Model(), action: (IAiAgentLink, Model) -> Unit) {
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
            link = plugin!!.attach(bundle(C.KEY_LINK_CONFIG_JSON, """{"grantSummary":{"toolGroups":["observe"]}}"""), model, capabilities,
                object : IAiAgentLinkCallback.Stub() { override fun onStatus(status: Bundle?) = Unit; override fun onEvent(event: Bundle?) = Unit })
            action(link, model)
        } finally {
            link?.detach(bundle(H.KEY_REASON_JSON, """{"reason":"workbench-test-finished"}"""))
            context.unbindService(connection)
            prefs.edit().putString("goal", oldGoal).putString("preset", oldPreset).commit()
        }
    }
    private fun waitFor(message: String, predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 20000
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
            for (step in 1..12) {
                waitFor("Model call $step") { model.calls.get() == step && model.held != null }
                model.finish("""{"kind":"ask","ask":{"kind":"text","question":"History question $step?"}}""")
                var pending: com.google.gson.JsonObject? = null
                waitFor("Question $step") {
                    pending = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))).getAsJsonObject("pending")
                    pending?.flag("submitted") != true && pending?.string("question") == "History question $step?"
                }
                AgentConnection.decode(link.respond(bundle(C.KEY_RUN_RESPONSE_JSON, jsonObject("runId" to id.json(), "requestId" to pending!!.string("requestId")!!.json(), "value" to privateText.json()).toString())))
            }
            waitFor("Final model call") { model.calls.get() == 13 && model.held != null }; model.finish(completed)
            waitFor("Settled") { AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}"""))).string("state") == "completed" }
            val full = query("get"); assertEquals(13, full.getAsJsonArray("steps").size())
            val projected = AgentConnection.decode(link.getRun(bundle(C.KEY_RUN_REF_JSON, """{"runId":"$id"}""")))
            assertTrue(projected.flag("truncated") == true)
            ActivityScenario.launch<RunDetailActivity>(Intent(context, RunDetailActivity::class.java).putExtra("runId", id)).use { detail ->
                waitFor("Full timeline rendered") { var found = false; detail.onActivity {
                    val all = texts(it.findViewById(android.R.id.content))
                    found = all.any { text -> text.contains("History question 1?") } && all.contains("Workbench fixture complete")
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
                    assertEquals(13, model.calls.get())
                } finally { instrumentation.removeMonitor(monitor) }
            }
            val file = java.io.File.createTempFile("p62-export-", ".json", context.cacheDir)
            try {
                file.outputStream().use { RunDetailActivity.writeExport(it, query("export")) }
                assertTrue(file.length() > 0)
                val text = file.readText(); assertTrue(text.contains("\"redacted\": true")); assertFalse(text.contains("13800123456"))
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
    private fun texts(view: View): List<String> = when (view) {
        is TextView -> listOf(view.text.toString())
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else -> emptyList()
    }
}
