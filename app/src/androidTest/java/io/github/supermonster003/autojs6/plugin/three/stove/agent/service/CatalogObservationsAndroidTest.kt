package io.github.supermonster003.autojs6.plugin.three.stove.agent.service

import android.os.*
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.autojs.plugin.host.capability.api.*
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference

/** The P13 observe additions cross the real capability Binder: envelope shape, granted permissions and bounded replies. */
class CatalogObservationsAndroidTest {
    private class Exchange(val envelope: JsonObject?, val result: PortResult<ToolReply>?)

    private fun roundTrip(name: String, arguments: JsonObject, methods: Set<String>, permissions: Set<String>, respond: (JsonObject) -> JsonElement): Exchange {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = ToolCatalog(context.assets.open("catalog/tools.json").bufferedReader().use { it.readText() })
        val seen = AtomicReference<JsonObject?>()
        val broker = object : IHostCapabilityBroker.Stub() {
            override fun getBrokerInfo() = Bundle()
            override fun destroy(reason: Bundle?) = Unit
            override fun dispatch(request: Bundle, callback: IHostCapabilityCallback) {
                val envelope = AgentJson.objectOf(request.getString(H.KEY_BRIDGE_REQUEST_JSON)!!)
                seen.set(envelope)
                callback.onResponse(Bundle().apply {
                    putBoolean(H.KEY_BRIDGE_RESPONSE_OK, true)
                    putString(H.KEY_BRIDGE_RESPONSE_JSON, jsonObject("id" to checkNotNull(envelope.string("id")).json(), "ok" to true.json(), "result" to respond(envelope)).toString())
                })
            }
        }
        LinkWorkers().use { workers -> SerialRunScheduler().use { scheduler ->
            val tools = BinderRunTools(broker, Process.myUid(), workers, scheduler, catalog, { true }, methods, permissions, 131072, 30000)
            val plan = ToolHandlers(catalog).prepare(name, arguments, ToolPolicy())
            val result = AtomicReference<PortResult<ToolReply>?>()
            val done = CountDownLatch(1)
            val handle = tools.execute(PreparedTool(ToolInvocation(name, arguments, plan), ToolMetadata()), 5000) { result.set(it); done.countDown() }
            try { assertTrue("$name did not complete", done.await(5, TimeUnit.SECONDS)) } finally { handle.cancel() }
            return Exchange(seen.get(), result.get())
        } }
    }

    private fun app(index: Int) = jsonObject("packageName" to "com.example.app$index".json(), "appName" to "App $index".json(), "enabled" to true.json(), "system" to (index % 2 == 0).json())

    @Test fun appListCarriesTheQueryAndBoundsTheHostReply() {
        val exchange = roundTrip("app_list", jsonObject("query" to "app".json(), "limit" to 200.json()), setOf("package_manager.listApps"), setOf("package_manager")) {
            jsonObject("schema" to "autojs6-bridge-android-apps-v1".json(), "query" to "app".json(), "count" to 250.json(), "truncated" to false.json(),
                "visibility" to "visible_to_host".json(), "apps" to JsonArray().apply { repeat(250) { add(app(it)) } })
        }
        val envelope = checkNotNull(exchange.envelope)
        assertEquals("package_manager", envelope.string("module")); assertEquals("listApps", envelope.string("method"))
        assertEquals(jsonArray(jsonObject("query" to "app".json())), envelope.getAsJsonArray("args"))
        assertEquals(jsonArray("package_manager".json()), envelope.getAsJsonArray("permissions"))
        val value = (exchange.result as PortResult.Success).value.result.asJsonObject
        assertEquals(200, value.getAsJsonArray("apps").size()); assertEquals(250L, value.number("count")); assertTrue(value.flag("truncated")!!)
        assertEquals("com.example.app199", value.getAsJsonArray("apps")[199].asJsonObject.string("packageName"))
    }

    @Test fun installationAndRunningExecutionsRoundTripAsReadOnlyQueries() {
        val installed = roundTrip("app_installed", jsonObject("packageName" to "com.example.missing".json()), setOf("app.isInstalled"), setOf("app.query")) { false.json() }
        val query = checkNotNull(installed.envelope)
        assertEquals("app", query.string("module")); assertEquals("isInstalled", query.string("method"))
        assertEquals(jsonArray("com.example.missing".json()), query.getAsJsonArray("args")); assertEquals(jsonArray("app.query".json()), query.getAsJsonArray("permissions"))
        assertEquals(jsonObject("installed" to false.json()), (installed.result as PortResult.Success).value.result)
        val running = roundTrip("script_list", JsonObject(), setOf("engines.list"), setOf("engines")) {
            jsonObject("schema" to "autojs6-bridge-engines-list-v1".json(), "scope" to "host".json(), "count" to 2.json(), "executions" to jsonArray(
                jsonObject("id" to 7.json(), "engineName" to "rhino".json(), "sourceName" to "coffee.js".json(), "sourcePath" to "/sdcard/scripts/coffee.js".json(),
                    "workingDirectory" to "/sdcard/scripts".json(), "state" to "running".json(), "startedAt" to 1000.json(), "uptimeMs" to 250.json()),
                jsonObject("id" to 9.json(), "engineName" to "rhino".json(), "sourceName" to "token=abc.js".json(), "sourcePath" to "/sdcard/scripts/other.js".json(),
                    "workingDirectory" to "/sdcard/scripts".json(), "state" to "running".json(), "startedAt" to 2000.json(), "uptimeMs" to JsonNull.INSTANCE)))
        }
        val listing = checkNotNull(running.envelope)
        assertEquals("engines", listing.string("module")); assertEquals("list", listing.string("method")); assertEquals(0, listing.getAsJsonArray("args").size())
        val value = (running.result as PortResult.Success).value.result.asJsonObject
        assertEquals(2, value.getAsJsonArray("executions").size()); assertEquals(2L, value.number("returned")); assertFalse(value.flag("truncated")!!)
        assertEquals(7L, value.getAsJsonArray("executions")[0].asJsonObject.number("id"))
        assertEquals("token=***", value.getAsJsonArray("executions")[1].asJsonObject.string("sourceName"))
    }

    @Test fun ungrantedMethodsNeverReachTheBroker() {
        val exchange = roundTrip("app_list", JsonObject(), setOf("device.info"), setOf("device")) { fail("dispatched without a grant"); JsonNull.INSTANCE }
        assertNull(exchange.envelope); assertTrue(exchange.result is PortResult.Failure)
    }
}
