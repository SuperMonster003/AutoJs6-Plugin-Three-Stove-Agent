package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.os.*
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.autojs.plugin.host.capability.api.*
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class ObservationPollingAndroidTest {
    private fun fixture(state: String, timeout: Long = 1000, respond: (Int) -> JsonElement,
                        check: (Cancellation, CountDownLatch, () -> PortResult<ToolReply>?, AtomicInteger) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = ToolCatalog(context.assets.open("catalog/tools.json").bufferedReader().use { it.readText() })
        val calls = AtomicInteger()
        val broker = object : IHostCapabilityBroker.Stub() {
            override fun getBrokerInfo() = Bundle()
            override fun destroy(reason: Bundle?) = Unit
            override fun dispatch(request: Bundle, callback: IHostCapabilityCallback) {
                val id = AgentJson.objectOf(request.getString(H.KEY_BRIDGE_REQUEST_JSON)!!).string("id")!!
                val result = respond(calls.incrementAndGet())
                callback.onResponse(Bundle().apply {
                    putBoolean(H.KEY_BRIDGE_RESPONSE_OK, true)
                    putString(H.KEY_BRIDGE_RESPONSE_JSON, jsonObject("id" to id.json(), "ok" to true.json(), "result" to result).toString())
                })
            }
        }
        LinkWorkers().use { workers -> SerialRunScheduler().use { scheduler ->
            val tools = BinderRunTools(broker, Process.myUid(), workers, scheduler, catalog, { true }, setOf("accessibility.findOne"), setOf("accessibility"), 131072, 30000)
            val arguments = jsonObject("selector" to jsonObject("text" to "visible".json()), "state" to state.json(), "timeoutMs" to timeout.json())
            val plan = ToolHandlers(catalog).prepare("ui_wait_for", arguments, ToolPolicy())
            val result = java.util.concurrent.atomic.AtomicReference<PortResult<ToolReply>?>()
            val done = CountDownLatch(1)
            val handle = tools.execute(PreparedTool(ToolInvocation("ui_wait_for", arguments, plan), ToolMetadata()), timeout + 1000) { result.set(it); done.countDown() }
            try { check(handle, done, result::get, calls) } finally { handle.cancel() }
        } }
    }
    private fun node() = jsonObject("text" to "visible".json(), "desc" to "".json(), "id" to "label".json(), "className" to "TextView".json(),
        "clickable" to false.json(), "enabled" to true.json(), "bounds" to jsonObject("left" to 0.json(), "top" to 0.json(), "right" to 20.json(), "bottom" to 10.json()))
    @Test fun appearingNodeWaitsForASecondObservation() = fixture("appear", respond = { if (it == 1) JsonNull.INSTANCE else node() }) { _, done, result, calls ->
        assertTrue(done.await(3, TimeUnit.SECONDS)); assertEquals(2, calls.get())
        val value = (result() as PortResult.Success).value.result.asJsonObject
        assertTrue(value.flag("matched")!!); assertEquals("visible", value.getAsJsonObject("node").string("text"))
    }
    @Test fun disappearingNodeCompletesWithAnExplicitNull() = fixture("disappear", respond = { JsonNull.INSTANCE }) { _, done, result, calls ->
        assertTrue(done.await(3, TimeUnit.SECONDS)); assertEquals(1, calls.get())
        assertTrue((result() as PortResult.Success).value.result.asJsonObject["node"].isJsonNull)
    }
    @Test fun deadlineReturnsNodeNotFoundWithoutUnboundedPolling() = fixture("appear", 300, { JsonNull.INSTANCE }) { _, done, result, calls ->
        assertTrue(done.await(3, TimeUnit.SECONDS)); assertEquals(RunError.NODE_NOT_FOUND, (result() as PortResult.Failure).error)
        assertTrue(calls.get() in 2..3)
    }
    @Test fun cancelledWaitDoesNotPollOrCompleteLater() {
        val requested = CountDownLatch(1)
        fixture("appear", respond = { requested.countDown(); JsonNull.INSTANCE }) { handle, done, result, calls ->
            assertTrue(requested.await(3, TimeUnit.SECONDS)); handle.cancel()
            assertFalse(done.await(400, TimeUnit.MILLISECONDS)); assertNull(result()); assertEquals(1, calls.get())
        }
    }
}
