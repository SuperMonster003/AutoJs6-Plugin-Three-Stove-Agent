package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.*
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.update.*
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
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
    private fun SettingsActivity.ready() = view<View>("tool-groups")?.isEnabled == true
    private fun AlertDialog.pick(index: Int) { listView.performItemClick(null, index, listView.adapter.getItemId(index)) }
    private fun query(endpoint: IAgentSettings, operation: String, fields: JsonObject = JsonObject()): JsonObject {
        val latch = CountDownLatch(1); var response: Bundle? = null
        fields.addProperty("operation", operation)
        endpoint.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, fields), object : IPresetStoreCallback.Stub() {
            override fun onResult(result: Bundle?) { response = result; latch.countDown() }
        })
        assertTrue("Settings response", latch.await(15, TimeUnit.SECONDS))
        assertNull(response!!.getString(C.KEY_ERROR_CODE)); return AgentConnection.decode(response!!)
    }
    private fun stored(directory: File) = runCatching { SettingsStore(File(directory, "agent-settings.json")).open() }.getOrNull()
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

    @Test fun changesApplyImmediatelyAndSurviveRecreationAndDiskReload() = isolated { _, endpoint, directory ->
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            ui(scenario, "Settings loaded") { it.ready() }
            scenario.onActivity { it.view<View>("tool-groups")!!.performClick() }
            ui(scenario, "Tool group sheet") { it.sheet?.content?.findViewWithTag<View>("group-shell") != null }
            scenario.onActivity {
                val shell = it.sheet!!.content.findViewWithTag<ViewGroup>("group-shell")!!
                assertFalse(shell.findSwitch()!!.isChecked)
                shell.performClick(); assertTrue(shell.findSwitch()!!.isChecked); it.sheet!!.dialog.dismiss()
            }
            waitFor("Tool group saved") { stored(directory)?.toolGroups?.contains("shell") == true }
            scenario.onActivity { it.access.choose(1) }
            waitFor("Cautious saved") { stored(directory)?.cautious == true }
            scenario.onActivity { it.view<View>("voice")!!.performClick() }
            waitFor("Voice saved") { stored(directory)?.voice == false }
            scenario.onActivity { it.editLimit("maxSteps") }
            scenario.onActivity {
                val field = (it.prompt!!.window!!.decorView as ViewGroup).findEditText()!!
                field.setText("7"); it.prompt!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            }
            waitFor("Limit saved") { stored(directory)?.budget?.get("maxSteps") == 7L }
            assertFalse("Full access is untouched", stored(directory)!!.fullAccess)
            val saved = SettingsCodec.decode(query(endpoint, "get").getAsJsonObject("settings").toString())
            assertTrue(saved.cautious); assertFalse(saved.voice); assertTrue("shell" in saved.toolGroups)
            scenario.recreate()
            ui(scenario, "Saved state retained") {
                it.ready() && it.access.selectedIndex == 1 && it.view<ViewGroup>("voice")!!.findSwitch()?.isChecked == false
            }
        }
    }

    @Test fun fullAccessShowsOnlyAnInlineWarningAndPersists() = isolated { _, endpoint, directory ->
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            ui(scenario, "Settings loaded") { it.ready() }
            scenario.onActivity { assertEquals(View.GONE, it.view<View>("full-access-note")!!.visibility); it.access.choose(2) }
            ui(scenario, "Inline full access warning") { it.view<View>("full-access-note")?.visibility == View.VISIBLE }
            scenario.onActivity { assertNull("Choosing full access must not open a dialog", it.prompt) }
            waitFor("Full access stored") { stored(directory)?.fullAccess == true }
            val saved = SettingsCodec.decode(query(endpoint, "get").getAsJsonObject("settings").toString())
            assertTrue(saved.fullAccess); assertFalse(saved.cautious)
            scenario.recreate()
            ui(scenario, "Full access retained") { it.ready() && it.access.selectedIndex == 2 && it.view<View>("full-access-note")?.visibility == View.VISIBLE }
        }
    }

    @Test fun durationIsEditedInMinutesAndAutomaticRestoresTheDefault() = isolated { _, _, directory ->
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            ui(scenario, "Settings loaded") { it.ready() }
            scenario.onActivity { it.editLimit(SettingsDraft.DURATION) }
            scenario.onActivity {
                val field = (it.prompt!!.window!!.decorView as ViewGroup).findEditText()!!
                field.setText("61"); it.prompt!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertTrue("Out of range stays open", it.prompt!!.isShowing)
                field.setText("12"); it.prompt!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            }
            waitFor("Minutes stored as milliseconds") { stored(directory)?.budget?.get("maxDurationMs") == 720_000L }
            scenario.onActivity { it.editLimit(SettingsDraft.DURATION) }
            scenario.onActivity { it.prompt!!.getButton(AlertDialog.BUTTON_NEUTRAL).performClick() }
            waitFor("Automatic removes the limit") { stored(directory)?.budget?.containsKey("maxDurationMs") == false }
        }
    }

    @Test fun appearanceChoicesApplyImmediately() = isolated { _, _, _ ->
        val original = AppearancePreferences.read(context)
        AppearancePreferences(language = "en", darkMode = "light").save(context)
        try {
            ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
                ui(scenario, "Appearance ready") { it.view<View>("appearance-language") != null }
                scenario.onActivity { it.view<View>("appearance-language")!!.performClick(); it.prompt!!.pick(2) }
                ui(scenario, "Chinese applied") { it.resources.configuration.locales[0].language == "zh" }
                scenario.onActivity { it.view<View>("appearance-dark")!!.performClick(); it.prompt!!.pick(3) }
                ui(scenario, "Independent dark preference applied") {
                    it.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
                scenario.onActivity { it.view<View>("appearance-color")!!.performClick(); it.prompt!!.pick(2) }
                ui(scenario, "Theme color applied") { it.appearance?.primary == 0xff007c8a.toInt() }
                assertEquals(AppearancePreferences("zh-Hans", "dark", 0xff007c8a.toInt()), AppearancePreferences.read(context))
            }
        } finally { original.save(context) }
    }

    @Test fun ignoredVersionsMigrateAndCanBeRestoredIndividually() = withUpdatePreferences { preferences ->
        preferences.edit().putString("ignored", "v2.0.0").commit()
        val settings = AppUpdateSettings(context)
        assertFalse(settings.automatic)
        settings.automatic = true
        settings.ignore("v3.0.0")
        assertEquals(setOf("v2.0.0", "v3.0.0"), AppUpdateSettings(context).ignored)
        settings.unignore(listOf("2.0.0"))
        assertEquals(setOf("v3.0.0"), settings.ignored)
        assertTrue(AppUpdateSettings(context).automatic)
        settings.unignore(listOf("v3.0.0"))
        assertTrue(settings.ignored.isEmpty())
    }

    @Test fun defaultSelectionAndConfirmedCategoryClearsReachRealStores() = isolated { _, endpoint, directory ->
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            ui(scenario, "Data rows") { it.view<View>("clear-memory")?.isEnabled == true }
            scenario.onActivity { it.view<View>("default")!!.performClick(); it.prompt!!.pick(0) }
            waitFor("Default changed") { query(endpoint, "get").string("defaultName") == "default" }
            for ((store, key, expected) in listOf(Triple("memory", "memoryData", 0L), Triple("history", "historyData", 0L), Triple("presets", "presetData", 1L))) {
                ui(scenario, "Clear enabled") { it.view<View>("clear-$store")?.isEnabled == true }
                scenario.onActivity {
                    it.view<View>("clear-$store")!!.performClick()
                    it.prompt!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
                }
                assertTrue(query(endpoint, "get").getAsJsonObject(key).number("count")!! > expected)
                scenario.onActivity {
                    it.view<View>("clear-$store")!!.performClick()
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
        for ((document, expected) in listOf("history" to "v1.1.0", "license" to "Mozilla Public License", "notices" to "Material Components")) {
            ActivityScenario.launch<ReleaseHistoryActivity>(Intent(context, ReleaseHistoryActivity::class.java).putExtra("document", document)).use { scenario ->
                waitFor("Bundled $document") { var ready = false; scenario.onActivity {
                    ready = it.findViewById<View>(android.R.id.content).findViewWithTag<TextView>("document")?.text?.contains(expected, ignoreCase = true) == true
                }; ready }
            }
        }
    }

    @Test fun aboutShowsIdentityAndOpensBundledDocuments() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName!!
            scenario.onActivity {
                val root = it.findViewById<View>(android.R.id.content)
                assertTrue(root.findViewWithTag<ViewGroup>("about-version")!!.let { block -> (block.getChildAt(1) as TextView).text.contains(version) })
                assertEquals(context.getString(R.string.about_license_name), (root.findViewWithTag<ViewGroup>("about-license-name")!!.getChildAt(1) as TextView).text.toString())
                for (tag in listOf("about-history", "about-source", "about-developer", "about-license", "about-notices")) assertNotNull(tag, root.findViewWithTag<View>(tag))
            }
            val monitor = instrumentation.addMonitor(ReleaseHistoryActivity::class.java.name, null, false)
            try {
                scenario.onActivity { it.findViewById<View>(android.R.id.content).findViewWithTag<View>("about-notices")!!.performClick() }
                val opened = monitor.waitForActivityWithTimeout(10000)
                assertNotNull(opened); assertEquals("notices", opened.intent.getStringExtra("document"))
                instrumentation.runOnMainSync { opened.finish() }
            } finally { instrumentation.removeMonitor(monitor) }
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
            assertEquals(View.LAYOUT_DIRECTION_RTL, root.getChildAt(0).layoutDirection)
            fun widths(view: View) {
                if (view.visibility == View.GONE) return
                // The toolbar keeps an empty menu container of zero width when a screen has no actions.
                if (view is ViewGroup && view.childCount == 0 && view.width == 0) return
                assertTrue("${view.javaClass.simpleName}:${view.tag} width ${view.width} fits", view.width in 1..root.width)
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
                ui(scenario, "RTL settings layout") { it.ready() && it.view<View>("update")?.isLaidOut == true }
                instrumentation.waitForIdleSync()
                scenario.onActivity { inspect(it, "settings") }
            }
            ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
                waitFor("RTL release history layout") { var ready = false; scenario.onActivity {
                    ready = it.findViewById<View>(android.R.id.content).findViewWithTag<TextView>("document")?.let { view ->
                        view.isLaidOut && view.text.contains("v1.1.0") } == true
                }; ready }
                scenario.onActivity { inspect(it, "history") }
            }
        } finally { HostAppearance.cached = original; release.countDown() }
    }

    @Test fun settingsDocumentsSheetsAndDialogsHaveAccessibleControlsAndUnclippedText() = isolated { _, _, _ -> withUpdatePreferences {
        val audit = UiAccessibilityAudit()
        audit.themed {
            ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
                ui(scenario, "Audit settings ready") { it.ready() && it.view<View>("update")?.isLaidOut == true }
                instrumentation.waitForIdleSync()
                scenario.onActivity { audit.inspect(it, "settings"); it.view<View>("clear-history")!!.performClick() }
                instrumentation.waitForIdleSync()
                scenario.onActivity { audit.inspect(it.prompt!!.window!!.decorView, "clear-dialog"); it.prompt!!.dismiss() }
                scenario.onActivity { it.view<View>("default")!!.performClick() }
                instrumentation.waitForIdleSync()
                scenario.onActivity { audit.inspect(it.prompt!!.window!!.decorView, "default-dialog"); it.prompt!!.dismiss() }
                for (tag in listOf("tool-groups", "limits")) {
                    scenario.onActivity { it.view<View>(tag)!!.performClick() }
                    instrumentation.waitForIdleSync(); SystemClock.sleep(400)
                    scenario.onActivity { audit.inspect(it.sheet!!.dialog.window!!.decorView, "sheet-$tag"); it.sheet!!.dialog.dismiss() }
                }
                AppUpdateCoordinator.sourceOverride = UpdateSource { UpdateResult.Success(
                    ReleaseInfo("v2.0.0", "${ReleaseInfoCodec.SOURCE}/releases/tag/v2.0.0", "Controlled update fixture")) }
                scenario.onActivity { it.view<View>("update")!!.performClick() }
                ui(scenario, "Audit update dialog ready") { it.updates.dialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.isLaidOut == true }
                scenario.onActivity { audit.inspect(it.updates.dialog!!.window!!.decorView, "update-dialog"); it.updates.dialog!!.dismiss() }
            }
            ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
                instrumentation.waitForIdleSync()
                scenario.onActivity { audit.inspect(it, "about") }
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
                saved.forEach { (key, value) -> when (value) { is String -> putString(key, value); is Long -> putLong(key, value); is Boolean -> putBoolean(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet()) } }
            }.commit()
        }
    }
    @Test fun automaticUpdatesRequireOptInRespectIgnoredVersionsAndThrottleRequests() = isolated { _, _, _ -> withUpdatePreferences { preferences ->
        val calls = AtomicInteger()
        val release = ReleaseInfo("v2.0.0", ReleaseInfoCodec.SOURCE + "/releases/tag/v2.0.0", "Controlled automatic update fixture")
        AppUpdateCoordinator.sourceOverride = UpdateSource { calls.incrementAndGet(); UpdateResult.Success(release) }
        AppUpdateSettings(context).ignore("v2.0.0")
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            ui(scenario, "Update controls ready") { it.view<View>("update") != null }
            scenario.onActivity { it.updates.checkAutomatically() }
            assertEquals(0, calls.get())
            AppUpdateSettings(context).automatic = true
            scenario.onActivity { it.updates.checkAutomatically() }
            waitFor("Automatic result cached") { preferences.contains("checked") }
            assertEquals(1, calls.get())
            scenario.onActivity {
                assertNull("Ignored automatic release stays silent", it.updates.dialog)
                it.updates.checkAutomatically()
                assertNull(it.updates.dialog)
            }
            assertEquals("Same foreground session is throttled", 1, calls.get())
        }
    } }
    @Test fun manualUpdateCacheIgnoreAndBothNavigationButtonsWork() = isolated { _, _, _ -> withUpdatePreferences { preferences ->
        val calls = AtomicInteger(); val release = ReleaseInfo("v2.0.0", "${ReleaseInfoCodec.SOURCE}/releases/tag/v2.0.0", "Controlled update fixture")
        AppUpdateCoordinator.sourceOverride = UpdateSource { calls.incrementAndGet(); UpdateResult.Success(release) }
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            ui(scenario, "Update entry") { it.view<View>("update") != null }
            scenario.onActivity { it.view<View>("update")!!.performClick() }
            ui(scenario, "New version dialog") { it.updates.dialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.visibility == View.VISIBLE }
            scenario.onActivity { it.updates.dialog!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick() }
            waitFor("Version ignored") { preferences.getString("ignored", null) == "v2.0.0" }; assertEquals(1, calls.get())
            scenario.onActivity { it.view<View>("update")!!.performClick() }
            ui(scenario, "Cached ignored version") { it.updates.dialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.text == it.getString(R.string.update_unignore) }
            scenario.onActivity { it.updates.dialog!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick() }
            waitFor("Ignore removed") { preferences.getString("ignored", null) == null }; assertEquals(1, calls.get())
            val monitor = instrumentation.addMonitor(IntentFilter(Intent.ACTION_VIEW).apply {
                addDataScheme("https"); addDataAuthority("github.com", null)
            }, null, true)
            try {
                scenario.onActivity { it.view<View>("update")!!.performClick(); it.updates.dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick() }
                waitFor("Release page navigation") { monitor.hits == 1 }
            } finally { instrumentation.removeMonitor(monitor) }
            val historyMonitor = instrumentation.addMonitor(ReleaseHistoryActivity::class.java.name, null, false)
            try {
                scenario.onActivity { it.view<View>("update")!!.performClick(); it.updates.dialog!!.getButton(AlertDialog.BUTTON_NEUTRAL).performClick() }
                val history = historyMonitor.waitForActivityWithTimeout(10000)
                assertNotNull(history); instrumentation.runOnMainSync { history.finish() }
            } finally { instrumentation.removeMonitor(historyMonitor) }
        }
    } }
    @Test fun failedAndCancelledUpdateChecksDoNotReplaceCachedState() = isolated { _, _, _ -> withUpdatePreferences { preferences ->
        preferences.edit().putLong("checked", 1).putString("release", "").commit()
        AppUpdateCoordinator.sourceOverride = UpdateSource { UpdateResult.Failure(UpdateFailure.HTTP) }
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            ui(scenario, "Update entry") { it.view<View>("update") != null }
            scenario.onActivity { it.view<View>("update")!!.performClick() }
            ui(scenario, "Failure dismissed") { it.updates.dialog == null }
            assertEquals(1L, preferences.getLong("checked", -1))
            val started = CountDownLatch(1); val returned = CountDownLatch(1)
            AppUpdateCoordinator.sourceOverride = UpdateSource { token ->
                started.countDown()
                while (!token.cancelled) SystemClock.sleep(10)
                returned.countDown(); UpdateResult.Success(null)
            }
            scenario.onActivity { it.view<View>("update")!!.performClick() }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            scenario.onActivity { it.updates.dialog!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick() }
            assertTrue(returned.await(5, TimeUnit.SECONDS)); instrumentation.waitForIdleSync()
            assertEquals(1L, preferences.getLong("checked", -1))
        }
    } }
}

internal fun ViewGroup.findEditText(): EditText? {
    for (index in 0 until childCount) {
        val child = getChildAt(index)
        if (child is EditText) return child
        if (child is ViewGroup) child.findEditText()?.let { return it }
    }
    return null
}

internal fun ViewGroup.findSwitch(): CompoundButton? {
    for (index in 0 until childCount) {
        val child = getChildAt(index)
        if (child is CompoundButton) return child
        if (child is ViewGroup) child.findSwitch()?.let { return it }
    }
    return null
}
