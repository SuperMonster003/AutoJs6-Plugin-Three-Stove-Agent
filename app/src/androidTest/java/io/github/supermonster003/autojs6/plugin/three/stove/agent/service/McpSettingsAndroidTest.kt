package io.github.supermonster003.autojs6.plugin.three.stove.agent.service

import android.content.ContextWrapper
import android.os.*
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AndroidMcpEncryption
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.McpStore
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.AgentConnection
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class McpSettingsAndroidTest {
    private fun isolated(action: (AgentRuntime, McpEndpoint, File) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "mcp-private-${UUID.randomUUID()}").apply { check(mkdirs()) }
        val fixture = object : ContextWrapper(context) { override fun getFilesDir() = directory; override fun getNoBackupFilesDir() = directory }
        val runtime = AgentRuntime(fixture)
        val endpoint = McpEndpoint(runtime)
        try { action(runtime, endpoint, directory) }
        finally { endpoint.close(); runtime.mcp.close(); runtime.memories.close(); directory.deleteRecursively() }
    }
    private fun query(endpoint: IAgentSettings, body: JsonObject, success: Boolean = true): JsonObject {
        val latch = CountDownLatch(1); var response: Bundle? = null
        endpoint.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, body), object : IPresetStoreCallback.Stub() {
            override fun onResult(result: Bundle?) { response = result; latch.countDown() }
        })
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        if (!success) { assertNotNull(response!!.getString(C.KEY_ERROR_CODE)); return JsonObject() }
        assertNull(response!!.getString(C.KEY_ERROR_CODE)); return AgentConnection.decode(response!!)
    }
    private fun get(endpoint: IAgentSettings) = query(endpoint, jsonObject("operation" to "get".json()))
    private fun save(endpoint: IAgentSettings, revision: Long, create: Boolean, token: String = "", clear: Boolean = false,
                     endpointUrl: String = "http://127.0.0.1:9637/mcp", success: Boolean = true) = query(endpoint,
        jsonObject("operation" to "save".json(), "revision" to revision.json(), "create" to create.json(), "token" to token.json(),
            "clearToken" to clear.json(), "profile" to jsonObject("id" to "local".json(), "name" to "Private fixture".json(),
                "endpoint" to endpointUrl.json(), "enabled" to false.json(), "risk" to "sensitive".json(),
                "selectedTools" to JsonArray().apply { add("device_info") })), success)
    @Test fun credentialsAreWriteOnlyEncryptedRetainedClearedAndBoundToEndpoint() = isolated { runtime, endpoint, directory ->
        val revision = get(endpoint).number("revision")!!
        save(endpoint, revision, true, "mcp-test-secret")
        val first = get(endpoint); assertFalse(first.toString().contains("mcp-test-secret"))
        assertTrue(first.getAsJsonArray("profiles")[0].asJsonObject.flag("hasBearerToken") == true)
        assertFalse(File(directory, "mcp-profiles.enc").readBytes().toString(Charsets.ISO_8859_1).contains("mcp-test-secret"))
        assertEquals("mcp-test-secret", McpStore(File(directory, "mcp-profiles.enc"), AndroidMcpEncryption()).open().single().bearerToken)
        save(endpoint, revision + 1, false)
        assertEquals("mcp-test-secret", runtime.mcp.snapshot().single().bearerToken)
        save(endpoint, revision + 2, false, endpointUrl = "https://mcp.example.test/mcp")
        assertNull(runtime.mcp.snapshot().single().bearerToken); assertTrue(runtime.mcp.snapshot().single().selectedTools.isEmpty())
        save(endpoint, revision + 3, false, "replacement")
        save(endpoint, revision + 4, false, clear = true)
        assertNull(runtime.mcp.snapshot().single().bearerToken)
        assertFalse(get(endpoint).getAsJsonArray("profiles")[0].asJsonObject.flag("hasBearerToken")!!)
    }
    @Test fun staleRevisionAndMaintenanceRefuseMutationWithoutReleasingAnotherOwner() = isolated { runtime, endpoint, _ ->
        val revision = get(endpoint).number("revision")!!
        save(endpoint, revision, true, "secret")
        save(endpoint, revision, false, "stale", success = false)
        assertEquals("secret", runtime.mcp.snapshot().single().bearerToken)
        assertTrue(runtime.beginMaintenance())
        try {
            save(endpoint, revision + 1, false, "busy", success = false); assertTrue(runtime.maintenance)
            query(endpoint, jsonObject("operation" to "delete".json(), "revision" to (revision + 1).json(), "id" to "local".json()), false)
            assertEquals(1, runtime.mcp.snapshot().size)
        } finally { runtime.endMaintenance() }
        query(endpoint, jsonObject("operation" to "delete".json(), "revision" to (revision + 1).json(), "id" to "local".json()))
        assertTrue(runtime.mcp.snapshot().isEmpty())
    }
    private class TrackingBinder : Binder() {
        val recipients = ConcurrentHashMap.newKeySet<IBinder.DeathRecipient>()
        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) { recipients += recipient }
        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int) = recipients.remove(recipient)
    }
    private class BlockedServer : AutoCloseable {
        private val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        private val client = AtomicReference<Socket?>()
        val connected = CountDownLatch(1)
        val endpoint = "http://127.0.0.1:${server.localPort}/mcp"
        private val thread = Thread({ runCatching {
            server.accept().use { socket ->
                client.set(socket); socket.soTimeout = 20000; connected.countDown()
                // Read the request but never respond, so closure must cancel a real in-flight HTTP read.
                val input = socket.getInputStream()
                while (input.read() >= 0) { }
            }
        } }, "mcp-endpoint-lifecycle-fixture").apply { isDaemon = true; start() }
        override fun close() {
            server.close(); runCatching { client.get()?.close() }; thread.join(3000)
        }
    }
    @Test fun closingDiscoveryEndpointCancelsHttpReleasesDeathListenerAndTerminatesOwnedWorkers() = isolated { runtime, initial, _ ->
        BlockedServer().use { server ->
            val revision = get(initial).number("revision")!!
            save(initial, revision, true, endpointUrl = server.endpoint)
            val before = Thread.getAllStackTraces().keys.filter { it.name == "three-stove-agent-mcp-probe" }.toSet()
            val tracking = TrackingBinder(); val replied = CountDownLatch(1); val replies = AtomicInteger()
            var response: Bundle? = null
            initial.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, jsonObject("operation" to "probe".json(),
                "revision" to (revision + 1).json(), "id" to "local".json(), "probeId" to UUID.randomUUID().toString().json())),
                object : IPresetStoreCallback.Stub() {
                    override fun asBinder(): IBinder = tracking
                    override fun onResult(result: Bundle?) { response = result; replies.incrementAndGet(); replied.countDown() }
                })
            assertTrue("Discovery reaches the fixture", server.connected.await(8, TimeUnit.SECONDS))
            assertEquals(1, tracking.recipients.size)
            val owned = Thread.getAllStackTraces().keys.filter { it.name == "three-stove-agent-mcp-probe" && it !in before }
            assertTrue("A discovery worker was started", owned.isNotEmpty())
            initial.close(); initial.close()
            assertTrue("Closed discovery returns cancellation", replied.await(3, TimeUnit.SECONDS))
            assertNotNull(response!!.getString(C.KEY_ERROR_CODE)); assertTrue(tracking.recipients.isEmpty())
            owned.forEach { it.join(McpEndpoint.PROBE_TIMEOUT_MS + 3000) }
            assertFalse("No discovery worker survives its endpoint", owned.any { it.isAlive })
            assertEquals(1, replies.get())
            query(initial, jsonObject("operation" to "get".json()), success = false)
            McpEndpoint(runtime).use { replacement -> assertEquals(1, get(replacement).getAsJsonArray("profiles").size()) }
        }
    }
    @Test fun closingEndpointDoesNotCancelAnAcceptedRepositoryWriteOrReleaseMaintenanceEarly() = isolated { runtime, endpoint, directory ->
        val revision = get(endpoint).number("revision")!!
        val blocked = CountDownLatch(1); val release = CountDownLatch(1)
        runtime.mcp.query { blocked.countDown(); release.await(10, TimeUnit.SECONDS) }
        assertTrue(blocked.await(5, TimeUnit.SECONDS))
        val replied = CountDownLatch(1); var response: Bundle? = null
        try {
            endpoint.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, jsonObject("operation" to "save".json(),
                "revision" to revision.json(), "create" to true.json(), "token" to "retained-test-token".json(), "clearToken" to false.json(),
                "profile" to jsonObject("id" to "local".json(), "name" to "Write lifecycle fixture".json(),
                    "endpoint" to "http://127.0.0.1:9637/mcp".json(), "enabled" to false.json(), "risk" to "sensitive".json(), "selectedTools" to JsonArray()))),
                object : IPresetStoreCallback.Stub() {
                    override fun onResult(result: Bundle?) { response = result; replied.countDown() }
                })
            assertTrue(runtime.maintenance)
            endpoint.close()
            assertTrue("Closing the UI does not release an in-flight write guard", runtime.maintenance)
        } finally { release.countDown() }
        assertTrue("Accepted write finishes after UI closure", replied.await(10, TimeUnit.SECONDS))
        assertNull(response!!.getString(C.KEY_ERROR_CODE)); assertFalse(runtime.maintenance)
        assertEquals("retained-test-token", McpStore(File(directory, "mcp-profiles.enc"), AndroidMcpEncryption()).open().single().bearerToken)
    }
}
