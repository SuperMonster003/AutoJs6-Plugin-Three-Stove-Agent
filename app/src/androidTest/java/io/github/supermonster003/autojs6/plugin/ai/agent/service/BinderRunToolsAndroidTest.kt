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

class BinderRunToolsAndroidTest {
    private fun call(reply: (String) -> Bundle): PortResult<ToolReply> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = ToolCatalog(context.assets.open("catalog/tools.json").bufferedReader().use { it.readText() })
        val broker = object : IHostCapabilityBroker.Stub() {
            override fun getBrokerInfo() = Bundle()
            override fun destroy(reason: Bundle?) = Unit
            override fun dispatch(request: Bundle, callback: IHostCapabilityCallback) {
                val id = AgentJson.objectOf(request.getString(H.KEY_BRIDGE_REQUEST_JSON)!!).string("id")!!
                callback.onResponse(reply(id))
            }
        }
        LinkWorkers().use { workers -> SerialRunScheduler().use { scheduler ->
            val tools = BinderRunTools(broker, Process.myUid(), workers, scheduler, catalog, { true }, setOf("device.info"), setOf("device"), 131072, 30000)
            val latch = CountDownLatch(1); var result: PortResult<ToolReply>? = null
            val plan = ToolPlan.Call(BridgeCall("device", "info", JsonArray(), listOf("device")))
            tools.execute(PreparedTool(ToolInvocation("device_info", JsonObject(), plan), ToolMetadata()), 3000) { result = it; latch.countDown() }
            assertTrue(latch.await(5, TimeUnit.SECONDS)); return checkNotNull(result)
        } }
    }
    @Test fun sharedBridgeReplyWithoutRepeatedVersionIsAcceptedButWrongVersionIsRejected() {
        fun reply(id: String) = Bundle().apply {
            putBoolean(H.KEY_BRIDGE_RESPONSE_OK, true)
            putString(H.KEY_BRIDGE_RESPONSE_JSON, jsonObject("id" to id.json(), "ok" to true.json(), "result" to jsonObject("sdkInt" to 37.json())).toString())
        }
        val result = call(::reply) as PortResult.Success
        assertEquals(37L, result.value.result.asJsonObject.number("sdkInt"))
        assertEquals(RunError.TOOL_ARGUMENTS_INVALID, (call { reply(it).apply { putInt(H.KEY_CONTRACT_VERSION, 2) } } as PortResult.Failure).error)
    }
    @Test fun descriptorContainsOnlyResultAndIsClosedAfterReading() {
        var received: ParcelFileDescriptor? = null
        val result = call { id ->
            val body = """{"sdkInt":37}""".toByteArray()
            val pipe = ParcelFileDescriptor.createPipe(); received = pipe[0]
            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write(body) }
            Bundle().apply {
                putBoolean(H.KEY_BRIDGE_RESPONSE_OK, true)
                putString(H.KEY_BRIDGE_RESPONSE_JSON, jsonObject("id" to id.json(), "ok" to true.json(), "result" to jsonObject("payload" to
                    jsonObject("kind" to "descriptor".json(), "mime" to "application/json".json(), "bytes" to body.size.json()))).toString())
                putParcelable(H.KEY_BRIDGE_PAYLOAD_FD, pipe[0]); putLong(H.KEY_BRIDGE_PAYLOAD_BYTES, body.size.toLong()); putString(H.KEY_BRIDGE_PAYLOAD_MIME, "application/json")
            }
        } as PortResult.Success
        assertEquals(37L, result.value.result.asJsonObject.number("sdkInt")); assertFalse(received!!.fileDescriptor.valid())
    }
}
