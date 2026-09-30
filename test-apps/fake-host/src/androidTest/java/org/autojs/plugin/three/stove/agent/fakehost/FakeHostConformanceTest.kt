package org.autojs.plugin.three.stove.agent.fakehost

import android.content.*
import android.os.*
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.three.stove.agent.api.*
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.autojs.plugin.three.stove.agent.fakehost.FakeHostService.Companion.envelope

/** Install only with the fake host in a disposable emulator data directory. */
class FakeHostConformanceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun waitFor(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(30)
        assertTrue(message, condition())
    }
    private inner class Fixture(mode: String) : Closeable {
        lateinit var driver: IFakeHostDriver
        val link: IThreeStoveAgentLink
        private val connection: ServiceConnection
        init {
            assertTrue("Disposable AVD only", Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk"))
            assertEquals("org.autojs.autojs6", context.packageName)
            assertEquals("conformance", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
            val ready = CountDownLatch(1)
            connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    driver = IFakeHostDriver.Stub.asInterface(service); ready.countDown()
                }
                override fun onServiceDisconnected(name: ComponentName) = Unit
            }
            assertTrue(context.bindService(Intent(context, FakeHostService::class.java), connection, Context.BIND_AUTO_CREATE))
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            val attached = driver.attach(envelope(C.KEY_LINK_CONFIG_JSON, """{"grantSummary":{"toolGroups":["observe"]}}"""), mode)
            assertEquals(Process.myUid(), attached.getInt("uid")); assertNotEquals(Process.myPid(), attached.getInt("pid"))
            link = IThreeStoveAgentLink.Stub.asInterface(attached.getBinder("link"))
            assertNull(link.asBinder().queryLocalInterface("org.autojs.plugin.three.stove.agent.api.IThreeStoveAgentLink"))
            waitFor("Callback came from the real plugin UID") { driver.stats().getInt("pluginUid") == context.packageManager.getApplicationInfo(FakeHostService.PLUGIN, 0).uid }
        }
        override fun close() { runCatching { driver.detach() }; context.unbindService(connection) }
    }
    private fun start(link: IThreeStoveAgentLink): String {
        val reply = link.startRun(envelope(C.KEY_RUN_REQUEST_JSON, """{"goal":"Fake host conformance","options":{"interaction":"script"}}"""), null)
        assertNull(reply.getString(C.KEY_ERROR_CODE)); return JSONObject(reply.getString(C.KEY_RUN_RESPONSE_JSON)!!).getString("runId")
    }
    private fun row(link: IThreeStoveAgentLink, id: String): JSONObject {
        val reply = link.getRun(envelope(C.KEY_RUN_REF_JSON, JSONObject().put("runId", id).toString()))
        assertNull(reply.getString(C.KEY_ERROR_CODE)); return JSONObject(reply.getString(C.KEY_RUN_RESPONSE_JSON)!!)
    }
    @Test fun realAttachRunCancelDetachAndConfigurationCannotWidenGrant() {
        Fixture("hold").use { fixture ->
            assertEquals("attached", JSONObject(fixture.link.status.getString(C.KEY_STATUS_JSON)!!).getString("state"))
            val widening = runCatching { fixture.link.updateConfig(envelope(C.KEY_LINK_CONFIG_JSON, """{"grantSummary":{"toolGroups":["observe","shell"]}}""")) }.exceptionOrNull()
            assertTrue(widening.toString(), widening is IllegalArgumentException)
            val id = start(fixture.link)
            waitFor("Model active") { fixture.driver.stats().getInt("models") == 1 }
            fixture.link.cancelRun(envelope(C.KEY_RUN_REF_JSON, JSONObject().put("runId", id).toString()))
            waitFor("Cancelled") { row(fixture.link, id).getString("state") == "cancelled" }
            fixture.driver.detach()
            assertEquals("detached", JSONObject(fixture.link.status.getString(C.KEY_STATUS_JSON)!!).getString("state"))
            assertEquals(C.ERROR_LINK_DETACHED, fixture.link.startRun(envelope(C.KEY_RUN_REQUEST_JSON, """{"goal":"late"}"""), null).getString(C.KEY_ERROR_CODE))
        }
    }
    @Test fun disabledToolNeverReachesCapabilityBroker() = denial("disabled-tool", 0)
    @Test fun trustedHostCannotBindPrivateMcpCredentialsOrSettings() {
        assertTrue("Disposable AVD only", Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk"))
        assertEquals("conformance", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) { fail("Private MCP endpoint was exposed") }
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        val intent = Intent("io.github.supermonster003.autojs6.plugin.three.stove.agent.MCP_SETTINGS")
            .setComponent(ComponentName(FakeHostService.PLUGIN, "${FakeHostService.PLUGIN}.service.AgentLocalService"))
        var bound = false
        try {
            val failure = runCatching { bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE) }.exceptionOrNull()
            assertTrue("Even the trusted host UID must not access MCP settings", failure is SecurityException || !bound)
        } finally { if (bound) context.unbindService(connection) }
    }
    @Test fun jsonVisionTransportsSyntheticCaptureAcrossRealBinderUids() = visionRoundTrip("vision-json")
    @Test fun nativeVisionTransportsCaptureAsTheMatchingToolResult() = visionRoundTrip("native-vision")
    private fun visionRoundTrip(mode: String) {
        org.junit.Assume.assumeTrue(Build.VERSION.SDK_INT >= 30)
        Fixture(mode).use { fixture ->
            val id = start(fixture.link)
            try {
                waitFor("Image round trip completed") { row(fixture.link, id).getString("state") in setOf("completed", "failed", "blocked") }
            } catch (failure: AssertionError) {
                val stats = fixture.driver.stats()
                throw AssertionError("$mode: ${row(fixture.link, id)}, models=${stats.getInt("models")}, tools=${stats.getInt("tools")}, images=${stats.getInt("imagesReceived")}", failure)
            }
            val result = row(fixture.link, id)
            assertEquals(result.toString(), "completed", result.getString("state"))
            val stats = fixture.driver.stats()
            assertEquals(1, stats.getInt("imagesReceived")); assertEquals(1, stats.getInt("tools"))
            assertEquals(if (mode.startsWith("native")) 1 else 2, stats.getInt("models"))
            assertEquals(if (mode.startsWith("native")) 1 else 0, stats.getInt("continuations"))
        }
    }
    @Test fun oldAndroidKeepsTextModeEvenWhenHostAndTargetAdvertiseVision() {
        org.junit.Assume.assumeTrue(Build.VERSION.SDK_INT < 30)
        Fixture("vision-json").use { fixture ->
            val id = start(fixture.link)
            waitFor("Text round trip completed") { row(fixture.link, id).getString("state") in setOf("completed", "failed", "blocked") }
            assertEquals("completed", row(fixture.link, id).getString("state"))
            assertEquals(0, fixture.driver.stats().getInt("imagesReceived")); assertEquals(0, fixture.driver.stats().getInt("tools"))
        }
    }
    @Test fun nativeBatchUsesRealCrossUidContinuationAndCumulativeUsage() = nativeRoundTrip("native-denied", 2)
    @Test fun nativeInvalidBatchIsRepairedWithoutAnyDeviceOperation() = nativeRoundTrip("native-repair", 0)
    private fun nativeRoundTrip(mode: String, expectedTools: Int) {
        Fixture(mode).use { fixture ->
            val id = start(fixture.link)
            waitFor("Native round trip completed") { row(fixture.link, id).getString("state") in setOf("completed", "failed", "blocked") }
            val result = row(fixture.link, id)
            assertEquals(result.toString(), "completed", result.getString("state"))
            assertTrue("A terminal row is visible only together with its result: $result", result.has("result"))
            assertEquals(1, fixture.driver.stats().getInt("models")); assertEquals(1, fixture.driver.stats().getInt("continuations"))
            assertEquals(expectedTools, fixture.driver.stats().getInt("tools")); assertTrue(fixture.driver.stats().getBoolean("observedDenial"))
            val usage = result.getJSONObject("result").getJSONObject("usage")
            assertEquals(2, usage.getInt("modelCalls")); assertEquals(34, usage.getInt("totalTokens"))
        }
    }
    @Test fun targetToolsCapabilityOnOldHostKeepsJsonPath() {
        Fixture("legacy-tools").use { fixture ->
            val id = start(fixture.link)
            waitFor("Old broker completed") { row(fixture.link, id).getString("state") == "completed" }
            assertEquals(1, fixture.driver.stats().getInt("models")); assertEquals(0, fixture.driver.stats().getInt("continuations"))
        }
    }
    @Test fun cancelDuringNativeToolClosesPausedModelWithoutContinuing() {
        Fixture("native-hold").use { fixture ->
            val id = start(fixture.link)
            waitFor("First native tool waiting") { fixture.driver.stats().getInt("tools") == 1 }
            val before = fixture.driver.stats().getInt("cancellations")
            fixture.link.cancelRun(envelope(C.KEY_RUN_REF_JSON, JSONObject().put("runId", id).toString()))
            waitFor("Paused model cancelled") { row(fixture.link, id).getString("state") == "cancelled" && fixture.driver.stats().getInt("cancellations") > before }
            assertEquals(1, fixture.driver.stats().getInt("tools")); assertEquals(0, fixture.driver.stats().getInt("continuations"))
        }
    }
    @Test fun capabilityGrantRejectionReturnsAnObservationAndCanFinish() = denial("denied", 1)
    private fun denial(mode: String, expectedTools: Int) {
        Fixture(mode).use { fixture ->
            val id = start(fixture.link)
            waitFor("Denial handled") { row(fixture.link, id).getString("state") in setOf("completed", "failed", "blocked") }
            assertEquals("completed", row(fixture.link, id).getString("state"))
            assertEquals(2, fixture.driver.stats().getInt("models")); assertEquals(expectedTools, fixture.driver.stats().getInt("tools"))
            assertTrue("Denied result reaches next model request", fixture.driver.stats().getBoolean("observedDenial"))
        }
    }
    @Test fun actualBrokerProcessDeathBlocksRunningAndQueuedTasksAndFreshAttachDoesNotReplay() {
        val fixture = Fixture("hold")
        val ids = (0..2).map { start(fixture.link) }
        waitFor("One model active") { fixture.driver.stats().getInt("models") == 1 }
        val died = CountDownLatch(1); fixture.driver.asBinder().linkToDeath({ died.countDown() }, 0)
        fixture.driver.die(); assertTrue(died.await(5, TimeUnit.SECONDS))
        try {
            waitFor("Every admitted task is blocked") { ids.all { row(fixture.link, it).getString("state") == "blocked" } }
            ids.forEach { assertEquals("HOST_UNAVAILABLE", row(fixture.link, it).getJSONObject("result").getJSONObject("error").getString("code")) }
        } finally { fixture.close() }
        Fixture("done").use { fresh ->
            assertEquals(0, fresh.driver.stats().getInt("models"))
            ids.forEach { assertEquals("blocked", row(fresh.link, it).getString("state")) }
            val id = start(fresh.link)
            waitFor("Explicit run after fresh attach") { row(fresh.link, id).getString("state") == "completed" }
            assertEquals(1, fresh.driver.stats().getInt("models"))
        }
    }
}
