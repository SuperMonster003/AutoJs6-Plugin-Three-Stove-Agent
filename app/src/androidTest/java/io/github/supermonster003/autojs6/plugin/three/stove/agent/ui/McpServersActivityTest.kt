package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.os.*
import android.view.*
import android.widget.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.*
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** UI fixtures return metadata only and never access the personal encrypted repository. */
class McpServersActivityTest {
    private class Endpoint : IAgentSettings.Stub() {
        val requests = java.util.concurrent.CopyOnWriteArrayList<JsonObject>()
        @Volatile var holdProbe = false
        @Volatile var holdSave = false
        override fun query(request: Bundle?, callback: IPresetStoreCallback?) {
            val body = AgentJson.objectOf(AgentWire.inline(request, C.KEY_RUN_REQUEST_JSON, McpEndpoint.MAX_REQUEST_BYTES))
            requests += body
            val response = when (body.string("operation")) {
                "get" -> jsonObject("revision" to 3.json(), "busy" to false.json(), "profiles" to JsonArray().apply {
                    add(jsonObject("id" to "local".json(), "name" to "Fixture".json(), "endpoint" to "http://127.0.0.1:9637/mcp".json(),
                        "enabled" to false.json(), "risk" to "sensitive".json(), "selectedTools" to JsonArray(), "hasBearerToken" to true.json()))
                })
                "probe" -> {
                    if (holdProbe) return
                    jsonObject("tools" to JsonArray().apply {
                        add(jsonObject("name" to "supported".json(), "supported" to true.json()))
                        add(jsonObject("name" to "unsupported".json(), "supported" to false.json(), "reason" to "MCP_TASKS_REQUIRED".json()))
                    })
                }
                "save" -> { if (holdSave) return; JsonObject() }
                else -> JsonObject()
            }
            callback?.onResult(AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, response.toString()))
        }
    }
    private fun <T : View> McpServersActivity.view(tag: String): T? = findViewById<View>(android.R.id.content).findViewWithTag(tag)
    private fun waitFor(label: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < deadline) { if (condition()) return; SystemClock.sleep(50) }
        fail(label)
    }
    private fun ui(scenario: ActivityScenario<McpServersActivity>, label: String, condition: (McpServersActivity) -> Boolean) = waitFor(label) {
        var result = false; scenario.onActivity { result = condition(it) }; result
    }
    private fun open(scenario: ActivityScenario<McpServersActivity>) {
        ui(scenario, "Saved server") { it.view<View>("mcp-server-local") != null }
        scenario.onActivity { it.view<View>("mcp-server-local")!!.performClick() }
        ui(scenario, "Credential editor") { it.view<EditText>("mcp-token") != null }
    }
    private fun isolated(action: (Endpoint) -> Unit) {
        val endpoint = Endpoint(); McpConnection.endpointOverride = endpoint
        try { action(endpoint) } finally { McpConnection.endpointOverride = null }
    }
    @Test fun savedTokenNeverAppearsAndUnsavedCredentialIsNotRestoredAfterRecreation() = isolated {
        ActivityScenario.launch(McpServersActivity::class.java).use { scenario ->
            open(scenario)
            scenario.onActivity { activity ->
                assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
                val token = activity.view<EditText>("mcp-token")!!
                assertEquals("", token.text.toString()); assertFalse(token.isSaveEnabled)
                assertFalse(activity.view<ViewGroup>("mcp-enabled")!!.findSwitch()!!.isChecked)
                assertEquals(0, activity.risk.selectedIndex)
                activity.view<EditText>("mcp-name")!!.setText("Retained metadata")
                token.setText("unsaved-private-token")
            }
            scenario.recreate()
            ui(scenario, "Metadata retained") { it.view<EditText>("mcp-name")?.text?.toString() == "Retained metadata" }
            scenario.onActivity { assertEquals("", it.view<EditText>("mcp-token")!!.text.toString()) }
        }
    }
    @Test fun discoveryDoesNotEnableToolsAndUnsupportedSchemasCannotBeSelected() = isolated { endpoint ->
        ActivityScenario.launch(McpServersActivity::class.java).use { scenario ->
            open(scenario); scenario.onActivity { it.view<Button>("mcp-probe")!!.performClick() }
            ui(scenario, "Tool discovery") { it.view<CheckBox>("mcp-tool-supported") != null }
            scenario.onActivity {
                assertFalse(it.view<CheckBox>("mcp-tool-supported")!!.isChecked)
                assertFalse(it.view<CheckBox>("mcp-tool-unsupported")!!.isEnabled)
                assertTrue(it.view<CheckBox>("mcp-tool-unsupported")!!.text.contains("MCP_TASKS_REQUIRED"))
                it.view<CheckBox>("mcp-tool-supported")!!.isChecked = true
                it.view<Button>("mcp-save")!!.performClick()
            }
            waitFor("Explicit selection saved") { endpoint.requests.any { it.string("operation") == "save" } }
            val saved = endpoint.requests.single { it.string("operation") == "save" }
            assertEquals(listOf("supported"), saved.getAsJsonObject("profile").getAsJsonArray("selectedTools").map { it.asString })
            assertEquals("", saved.string("token")); assertFalse(saved.flag("clearToken")!!)
            assertFalse(saved.getAsJsonObject("profile").flag("enabled")!!)
        }
    }
    @Test fun leavingScreenCancelsPendingDiscovery() = isolated { endpoint ->
        endpoint.holdProbe = true
        ActivityScenario.launch(McpServersActivity::class.java).use { scenario ->
            open(scenario); scenario.onActivity { it.view<Button>("mcp-probe")!!.performClick() }
            waitFor("Discovery requested") { endpoint.requests.any { it.string("operation") == "probe" } }
            scenario.onActivity {
                assertFalse(it.view<EditText>("mcp-endpoint")!!.isEnabled)
                assertFalse(it.view<EditText>("mcp-name")!!.isEnabled)
                assertTrue(it.view<Button>("mcp-cancel-probe")!!.isEnabled)
            }
        }
        waitFor("Discovery cancelled") { endpoint.requests.any { it.string("operation") == "cancel" } }
        assertEquals(endpoint.requests.single { it.string("operation") == "probe" }.string("probeId"),
            endpoint.requests.single { it.string("operation") == "cancel" }.string("probeId"))
    }
    @Test fun recreationDuringSaveReloadsServerListInsteadOfReusingAnUnacknowledgedDraft() = isolated { endpoint ->
        endpoint.holdSave = true
        ActivityScenario.launch(McpServersActivity::class.java).use { scenario ->
            open(scenario)
            scenario.onActivity {
                it.view<EditText>("mcp-name")!!.setText("Unacknowledged metadata")
                it.view<EditText>("mcp-token")!!.setText("unsaved-token")
                it.view<Button>("mcp-save")!!.performClick()
                assertFalse(it.view<EditText>("mcp-name")!!.isEnabled)
            }
            waitFor("Write submitted") { endpoint.requests.any { it.string("operation") == "save" } }
            scenario.recreate()
            ui(scenario, "Server metadata reloaded") { it.view<View>("mcp-server-local") != null && it.view<EditText>("mcp-token") == null }
        }
    }
    @Test fun arabicDarkEditorWrapsWithinNarrowWidthAndMaintainsTouchTargets() = isolated { endpoint ->
        endpoint.holdProbe = true
        val previous = HostAppearance.cached
        val release = CountDownLatch(1); val entered = CountDownLatch(1)
        HostAppearance.worker.execute { entered.countDown(); release.await(45, TimeUnit.SECONDS) }
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        HostAppearance.cached = HostAppearance("ar", true, 0xff334455.toInt(), 0xffeeddcc.toInt())
        try {
            ActivityScenario.launch(McpServersActivity::class.java).use { scenario ->
                open(scenario)
                scenario.onActivity { activity ->
                    assertEquals("ar", activity.resources.configuration.locales[0].language)
                    val root = activity.findViewById<ViewGroup>(android.R.id.content)
                    val column = activity.view<View>("mcp-message")!!.parent as LinearLayout
                    assertEquals(View.LAYOUT_DIRECTION_RTL, column.layoutDirection)
                    val width = (280 * activity.resources.displayMetrics.density).toInt()
                    val touch = kotlin.math.ceil(48 * activity.resources.displayMetrics.density).toInt()
                    fun inspectVisibleControls(state: String) {
                        // Exercise large text without changing device-wide font settings.
                        for (index in 0 until column.childCount) (column.getChildAt(index) as? TextView)?.textSize = 30f
                        column.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                        column.layout(0, 0, width, column.measuredHeight)
                        for (index in 0 until column.childCount) {
                            val view = column.getChildAt(index)
                            // GONE controls have no layout or touch target. Their visible state is checked below.
                            if (view.visibility == View.GONE) continue
                            val label = "$state: ${view.tag ?: view.javaClass.simpleName}"
                            assertTrue("$label fits width", view.left >= 0 && view.right <= width)
                            if (view is Button || view is EditText || view is Spinner)
                                assertTrue("$label height ${view.measuredHeight} must be at least $touch", view.measuredHeight >= touch)
                        }
                    }
                    assertEquals(View.GONE, activity.view<Button>("mcp-cancel-probe")!!.visibility)
                    inspectVisibleControls("idle")
                    activity.view<Button>("mcp-probe")!!.performClick()
                    assertEquals(View.VISIBLE, activity.view<Button>("mcp-cancel-probe")!!.visibility)
                    assertTrue(activity.view<Button>("mcp-cancel-probe")!!.isEnabled)
                    inspectVisibleControls("discovering")
                }
            }
        } finally { HostAppearance.cached = previous; release.countDown() }
    }
}
