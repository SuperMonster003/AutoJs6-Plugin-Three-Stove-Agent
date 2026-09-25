package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.AlertDialog
import android.content.*
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.service.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import io.github.supermonster003.autojs6.plugin.ai.agent.update.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Actual settings UI and serial stores in an isolated directory; personal histories are never cleared. */
class SettingsActivityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun waitFor(label: String, check: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < end) { if (check()) return; SystemClock.sleep(60) }
        fail(label)
    }
    private fun ui(scenario: ActivityScenario<SettingsActivity>, label: String, check: (SettingsActivity) -> Boolean) = waitFor(label) {
        var result = false; scenario.onActivity { result = check(it) }; result
    }
    private fun <T : View> SettingsActivity.view(tag: String): T? = findViewById<View>(android.R.id.content).findViewWithTag(tag)
    private fun query(endpoint: IAgentSettings, operation: String, fields: JsonObject = JsonObject()): JsonObject {
        val latch = CountDownLatch(1); var response: Bundle? = null
        fields.addProperty("operation", operation)
        endpoint.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, fields), object : IPresetStoreCallback.Stub() {
            override fun onResult(result: Bundle?) { response = result; latch.countDown() }
        })
        assertTrue("Settings response", latch.await(15, TimeUnit.SECONDS))
        assertNull(response!!.getString(C.KEY_ERROR_CODE)); return AgentConnection.decode(response!!)
    }
    private fun isolated(action: (AgentRuntime, IAgentSettings, File) -> Unit) {
        val directory = File(context.cacheDir, "p66-${UUID.randomUUID()}").apply { check(mkdirs()) }
        val fixture = object : ContextWrapper(context) { override fun getFilesDir() = directory }
        val preset = PresetStore(File(directory, "agent-presets.json")); preset.open()
        preset.save(Preset("fixture"), true, emptySet(), emptySet()); preset.setDefault("fixture")
        val id = UUID.randomUUID().toString()
        val memory = MemoryStore(File(directory, "memories")); memory.open()
        memory.put(MemoryEntry("fixture-preference", "Office fixture", "global", id, 1, 1), null)
        val history = RunHistoryStore(File(directory, "runs")); history.open()
        history.save(jsonObject("runId" to id.json(), "goal" to "Settings fixture".json(), "state" to "completed".json(),
            "startedAt" to 1.json(), "preset" to "default".json(), "steps" to JsonArray()))
        val runtime = AgentRuntime(fixture); val endpoint = SettingsEndpoint(runtime)
        SettingsConnection.endpointOverride = endpoint
        try { query(endpoint, "get"); action(runtime, endpoint, directory) }
        finally { SettingsConnection.endpointOverride = null; runtime.memories.close(); directory.deleteRecursively() }
    }
    @Test fun settingsDraftAndSavedLimitsSurviveRecreationAndDiskReload() = isolated { _, endpoint, directory ->
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            ui(scenario, "Settings form") { it.view<Button>("save") != null }
            scenario.onActivity {
                assertFalse(it.view<CheckBox>("group-gesture")!!.isChecked)
                assertFalse(it.view<CheckBox>("group-files")!!.isChecked)
                assertFalse(it.view<CheckBox>("group-shell")!!.isChecked)
                it.view<CheckBox>("group-shell")!!.isChecked = true
                it.view<CheckBox>("cautious")!!.isChecked = true
                it.view<CheckBox>("voice")!!.isChecked = false
                it.view<EditText>("maxSteps")!!.setText("7")
            }
            scenario.recreate()
            ui(scenario, "Draft retained") { it.view<EditText>("maxSteps")?.text?.toString() == "7" && it.view<CheckBox>("group-shell")?.isChecked == true }
            scenario.onActivity { it.view<Button>("save")!!.performClick() }
            waitFor("Settings stored") { runCatching { SettingsStore(File(directory, "agent-settings.json")).open().budget["maxSteps"] == 7L }.getOrDefault(false) }
            val saved = SettingsCodec.decode(query(endpoint, "get").getAsJsonObject("settings").toString())
            assertTrue(saved.cautious); assertFalse(saved.voice); assertTrue("shell" in saved.toolGroups)
            scenario.recreate()
            ui(scenario, "Saved state retained") { it.view<CheckBox>("cautious")?.isChecked == true && it.view<CheckBox>("voice")?.isChecked == false }
        }
    }
    @Test fun defaultSelectionAndConfirmedCategoryClearsReachRealStores() = isolated { _, endpoint, directory ->
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            ui(scenario, "Data rows") { it.view<Button>("clear-memory") != null }
            scenario.onActivity {
                it.view<Button>("default")!!.performClick()
                val list = it.prompt!!.listView; list.performItemClick(list.getChildAt(0), 0, list.adapter.getItemId(0))
            }
            waitFor("Default changed") { query(endpoint, "get").string("defaultName") == "default" }
            for ((store, key, expected) in listOf(Triple("memory", "memoryData", 0L), Triple("history", "historyData", 0L), Triple("presets", "presetData", 1L))) {
                ui(scenario, "Clear enabled") { it.view<Button>("clear-$store")?.isEnabled == true }
                scenario.onActivity {
                    it.view<Button>("clear-$store")!!.performClick()
                    it.prompt!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
                }
                assertTrue(query(endpoint, "get").getAsJsonObject(key).number("count")!! > expected)
                scenario.onActivity {
                    it.view<Button>("clear-$store")!!.performClick()
                    it.prompt!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                }
                waitFor("Category cleared") { query(endpoint, "get").getAsJsonObject(key).number("count") == expected }
            }
            assertTrue(MemoryStore(File(directory, "memories")).open().isEmpty())
            assertTrue(RunHistoryStore(File(directory, "runs")).open().isEmpty())
            assertEquals(listOf("default"), PresetStore(File(directory, "agent-presets.json")).open().presets.map { it.name })
        }
    }
    @Test fun competingMaintenanceCannotClearData() = isolated { runtime, endpoint, _ ->
        assertTrue(runtime.beginMaintenance())
        val latch = CountDownLatch(1); var error: String? = null
        try {
            endpoint.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, jsonObject("operation" to "clear".json(), "store" to "memory".json())),
                object : IPresetStoreCallback.Stub() { override fun onResult(result: Bundle?) { error = result?.getString(C.KEY_ERROR_CODE); latch.countDown() } })
            assertTrue(latch.await(5, TimeUnit.SECONDS)); assertEquals(C.ERROR_INVALID_REQUEST, error)
            assertTrue(runtime.maintenance)
        } finally { runtime.endMaintenance() }
        assertEquals(1L, query(endpoint, "get").getAsJsonObject("memoryData").number("count"))
    }
    @Test fun bundledReleaseHistoryLicenseAndNoticesOpenWithoutNetwork() {
        for ((document, expected) in listOf("history" to "v1.0.0", "license" to "Mozilla Public License", "notices" to "Gson")) {
            ActivityScenario.launch<ReleaseHistoryActivity>(Intent(context, ReleaseHistoryActivity::class.java).putExtra("document", document)).use { scenario ->
                waitFor("Bundled $document") { var ready = false; scenario.onActivity {
                    ready = it.findViewById<View>(android.R.id.content).findViewWithTag<TextView>("document")?.text?.contains(expected, ignoreCase = true) == true
                }; ready }
            }
        }
    }
    @Test fun privateSettingsBinderPersistsAcrossConnectionsAndRestoresUserPreferences() {
        fun withEndpoint(action: (IAgentSettings) -> Unit) {
            val ready = CountDownLatch(1); var binder: IBinder? = null
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) { binder = service; ready.countDown() }
                override fun onServiceDisconnected(name: ComponentName?) = Unit
            }
            assertTrue(context.bindService(Intent(context, AgentLocalService::class.java).setAction(SettingsEndpoint.ACTION), connection, Context.BIND_AUTO_CREATE))
            try {
                assertTrue(ready.await(15, TimeUnit.SECONDS))
                assertEquals(IAgentSettings.DESCRIPTOR, binder!!.interfaceDescriptor)
                assertNull("Must cross the :agent process boundary", binder!!.queryLocalInterface(IAgentSettings.DESCRIPTOR))
                action(IAgentSettings.Stub.asInterface(binder))
            } finally { context.unbindService(connection) }
        }
        var original: JsonObject? = null
        try {
            withEndpoint { endpoint ->
                original = query(endpoint, "get").getAsJsonObject("settings")
                val changed = original!!.deepCopy().apply { addProperty("voice", original!!.flag("voice") != true) }
                query(endpoint, "save", jsonObject("settings" to changed))
            }
            withEndpoint { endpoint ->
                val saved = query(endpoint, "get").getAsJsonObject("settings")
                assertEquals(original!!.flag("voice") != true, saved.flag("voice"))
                assertEquals(saved, SettingsCodec.json(SettingsStore(File(context.filesDir, "agent-settings.json")).open()))
            }
        } finally { original?.let { value -> withEndpoint { query(it, "save", jsonObject("settings" to value)) } } }
    }
    @Test fun settingsAndHistoryUseArabicNightAppearanceWithinScrollableWidth() = isolated { _, _, _ ->
        val original = HostAppearance.cached
        val release = CountDownLatch(1); val entered = CountDownLatch(1)
        HostAppearance.worker.execute { entered.countDown(); release.await(45, TimeUnit.SECONDS) }
        assertTrue(entered.await(15, TimeUnit.SECONDS))
        HostAppearance.cached = HostAppearance("ar", true, 0xff334455.toInt(), 0xffeeddcc.toInt())
        fun inspect(activity: HostAppearanceActivity, name: String) {
            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            assertEquals("ar", activity.resources.configuration.locales[0].language)
            assertEquals(android.content.res.Configuration.UI_MODE_NIGHT_YES,
                activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)
            assertEquals(View.LAYOUT_DIRECTION_RTL, (root.getChildAt(0) as ScrollView).getChildAt(0).layoutDirection)
            fun widths(view: View) {
                if (view.visibility != View.GONE) assertTrue("${view.tag} fits", view.width in 1..root.width)
                if (view is ViewGroup) for (index in 0 until view.childCount) widths(view.getChildAt(index))
            }
            widths(root)
            val bitmap = android.graphics.Bitmap.createBitmap(root.width, root.height, android.graphics.Bitmap.Config.ARGB_8888)
            root.draw(android.graphics.Canvas(bitmap))
            File(context.cacheDir, "p66-$name-ar.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        try {
            ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
                ui(scenario, "RTL settings layout") { it.view<Button>("update")?.isLaidOut == true }
                scenario.onActivity { inspect(it, "settings") }
            }
            ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
                waitFor("RTL release history layout") { var ready = false; scenario.onActivity {
                    ready = it.findViewById<View>(android.R.id.content).findViewWithTag<TextView>("document")?.let { view ->
                        view.isLaidOut && view.text.contains("v1.0.0") } == true
                }; ready }
                scenario.onActivity { inspect(it, "history") }
            }
        } finally { HostAppearance.cached = original; release.countDown() }
    }
    @Test fun settingsDocumentsAndDialogsHaveAccessibleControlsAndUnclippedText() = isolated { _, _, _ -> withUpdatePreferences {
        val audit = UiAccessibilityAudit()
        audit.themed {
            ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
                ui(scenario, "Audit settings ready") { it.view<Button>("update")?.isLaidOut == true }
                scenario.onActivity { audit.inspect(it, "settings"); it.view<Button>("clear-history")!!.performClick() }
                instrumentation.waitForIdleSync()
                scenario.onActivity { audit.inspect(it.prompt!!.window!!.decorView, "clear-dialog"); it.prompt!!.dismiss() }
                scenario.onActivity { it.view<Button>("default")!!.performClick() }
                instrumentation.waitForIdleSync()
                scenario.onActivity { audit.inspect(it.prompt!!.window!!.decorView, "default-dialog"); it.prompt!!.dismiss() }
                AppUpdateCoordinator.sourceOverride = UpdateSource { UpdateResult.Success(
                    ReleaseInfo("v2.0.0", "${ReleaseInfoCodec.SOURCE}/releases/tag/v2.0.0", "Controlled update fixture")) }
                scenario.onActivity { it.view<Button>("update")!!.performClick() }
                ui(scenario, "Audit update dialog ready") { it.updates.dialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.isLaidOut == true }
                scenario.onActivity { audit.inspect(it.updates.dialog!!.window!!.decorView, "update-dialog"); it.updates.dialog!!.dismiss() }
            }
            for (document in listOf("history", "license", "notices")) {
                ActivityScenario.launch<ReleaseHistoryActivity>(Intent(context, ReleaseHistoryActivity::class.java).putExtra("document", document)).use { scenario ->
                    waitFor("Audit document ready") { var ready = false; scenario.onActivity {
                        ready = it.findViewById<View>(android.R.id.content).findViewWithTag<TextView>("document")?.let { text ->
                            text.isLaidOut && text.text.length > 100 } == true
                    }; ready }
                    instrumentation.waitForIdleSync()
                    scenario.onActivity { audit.inspect(it, "document-$document") }
                }
            }
            audit.finish()
        }
    } }

    private fun withUpdatePreferences(action: (android.content.SharedPreferences) -> Unit) {
        val preferences = context.getSharedPreferences("updates", Context.MODE_PRIVATE); val saved = preferences.all.toMap()
        preferences.edit().clear().commit()
        try { action(preferences) } finally {
            AppUpdateCoordinator.sourceOverride = null
            preferences.edit().clear().apply {
                saved.forEach { (key, value) -> when (value) { is String -> putString(key, value); is Long -> putLong(key, value); is Boolean -> putBoolean(key, value) } }
            }.commit()
        }
    }
    @Test fun manualUpdateCacheIgnoreAndBothNavigationButtonsWork() = isolated { _, _, _ -> withUpdatePreferences { preferences ->
        val calls = AtomicInteger(); val release = ReleaseInfo("v2.0.0", "${ReleaseInfoCodec.SOURCE}/releases/tag/v2.0.0", "Controlled update fixture")
        AppUpdateCoordinator.sourceOverride = UpdateSource { calls.incrementAndGet(); UpdateResult.Success(release) }
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            ui(scenario, "Update entry") { it.view<Button>("update") != null }
            scenario.onActivity { it.view<Button>("update")!!.performClick() }
            ui(scenario, "New version dialog") { it.updates.dialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.visibility == View.VISIBLE }
            scenario.onActivity { it.updates.dialog!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick() }
            waitFor("Version ignored") { preferences.getString("ignored", null) == "v2.0.0" }; assertEquals(1, calls.get())
            scenario.onActivity { it.view<Button>("update")!!.performClick() }
            ui(scenario, "Cached ignored version") { it.updates.dialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.text == it.getString(R.string.update_unignore) }
            scenario.onActivity { it.updates.dialog!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick() }
            waitFor("Ignore removed") { preferences.getString("ignored", null) == null }; assertEquals(1, calls.get())
            val monitor = instrumentation.addMonitor(IntentFilter(Intent.ACTION_VIEW).apply {
                addDataScheme("https"); addDataAuthority("github.com", null)
            }, null, true)
            try {
                scenario.onActivity { it.view<Button>("update")!!.performClick(); it.updates.dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick() }
                waitFor("Release page navigation") { monitor.hits == 1 }
            } finally { instrumentation.removeMonitor(monitor) }
            val historyMonitor = instrumentation.addMonitor(ReleaseHistoryActivity::class.java.name, null, false)
            try {
                scenario.onActivity { it.view<Button>("update")!!.performClick(); it.updates.dialog!!.getButton(AlertDialog.BUTTON_NEUTRAL).performClick() }
                val history = historyMonitor.waitForActivityWithTimeout(10000)
                assertNotNull(history); instrumentation.runOnMainSync { history.finish() }
            } finally { instrumentation.removeMonitor(historyMonitor) }
        }
    } }
    @Test fun failedAndCancelledUpdateChecksDoNotReplaceCachedState() = isolated { _, _, _ -> withUpdatePreferences { preferences ->
        preferences.edit().putLong("checked", 1).putString("release", "").commit()
        AppUpdateCoordinator.sourceOverride = UpdateSource { UpdateResult.Failure(UpdateFailure.HTTP) }
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            ui(scenario, "Update entry") { it.view<Button>("update") != null }
            scenario.onActivity { it.view<Button>("update")!!.performClick() }
            ui(scenario, "Failure dismissed") { it.updates.dialog == null }
            assertEquals(1L, preferences.getLong("checked", -1))
            val started = CountDownLatch(1); val returned = CountDownLatch(1)
            AppUpdateCoordinator.sourceOverride = UpdateSource { token ->
                started.countDown()
                while (!token.cancelled) SystemClock.sleep(10)
                returned.countDown(); UpdateResult.Success(null)
            }
            scenario.onActivity { it.view<Button>("update")!!.performClick() }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            scenario.onActivity { it.updates.dialog!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick() }
            assertTrue(returned.await(5, TimeUnit.SECONDS)); instrumentation.waitForIdleSync()
            assertEquals(1L, preferences.getLong("checked", -1))
        }
    } }
}
