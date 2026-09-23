package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.os.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.*

class AgentWireAndroidTest {
    @Test fun boundedControlDescriptorsSupportFilesAndRejectStalledPipesPromptly() {
        val pipe = ParcelFileDescriptor.createPipe()
        ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write("{}".toByteArray()) }
        val request = Bundle().apply { putInt(C.KEY_CONTRACT_VERSION, C.CONTRACT_VERSION); putParcelable(C.KEY_PAYLOAD_FD, pipe[0]) }
        assertEquals("{}", AgentWire.control(request, C.KEY_RUN_REQUEST_JSON))
        assertFalse(pipe[0].fileDescriptor.valid())
        val stalled = ParcelFileDescriptor.createPipe()
        val before = SystemClock.elapsedRealtime()
        try { assertThrows(WireFailure::class.java) { AgentWire.control(Bundle().apply {
            putInt(C.KEY_CONTRACT_VERSION, C.CONTRACT_VERSION); putParcelable(C.KEY_PAYLOAD_FD, stalled[0])
        }, C.KEY_RUN_REQUEST_JSON) } } finally { stalled[1].close() }
        assertTrue(SystemClock.elapsedRealtime() - before < 200); assertFalse(stalled[0].fileDescriptor.valid())
    }
    @Test fun malformedAndOversizedDescriptorsAreClosedAndNeverReachAConsumer() {
        val pipe = ParcelFileDescriptor.createPipe()
        val bundle = AgentWire.envelope(C.KEY_MODEL_EVENT_JSON, "{}").apply { putParcelable(C.KEY_PAYLOAD_FD, pipe[0]) }
        try {
            assertThrows(WireFailure::class.java) { AgentWire.take(bundle, C.KEY_MODEL_EVENT_JSON, C.KEY_PAYLOAD_FD, 32, 32) }
            assertFalse(pipe[0].fileDescriptor.valid())
        } finally { pipe.forEach { runCatching { it.close() } } }
        val bytes = ParcelFileDescriptor.createPipe()
        ParcelFileDescriptor.AutoCloseOutputStream(bytes[1]).use { it.write(ByteArray(65)) }
        OwnedJson(null, bytes[0], 64).use { assertThrows(WireFailure::class.java) { it.read(500) } }
        assertFalse(bytes[0].fileDescriptor.valid())
    }
    @Test fun stalledPipeTimesOutAndCancellationClosesTheRead() {
        val pipe = ParcelFileDescriptor.createPipe()
        val input = OwnedJson(null, pipe[0], 64)
        try { assertThrows(WireFailure::class.java) { input.read(100) } } finally { input.close(); pipe[1].close() }
        val pending = ParcelFileDescriptor.createPipe()
        val owner = OwnedJson(null, pending[0], 64)
        val stopped = CountDownLatch(1)
        Thread { try { owner.read(15_000) } catch (_: Exception) { stopped.countDown() } }.start()
        owner.close()
        assertTrue(stopped.await(1, TimeUnit.SECONDS)); pending[1].close()
    }
    @Test fun descriptorEventsStayOrderedAcrossInlineTerminalAndCloseOnFinish() {
        LinkWorkers().use { workers ->
            val received = CopyOnWriteArrayList<String>()
            val done = CountDownLatch(2)
            val failure = CopyOnWriteArrayList<String>()
            OrderedModelEvents(workers, { received.add(it); done.countDown() }, { failure.add(it) }).use { events ->
                val pipe = ParcelFileDescriptor.createPipe()
                events.accept(OwnedJson(null, pipe[0], 64))
                events.accept(OwnedJson("second", null, 64))
                ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write("first".toByteArray()) }
                assertTrue(done.await(2, TimeUnit.SECONDS))
                assertEquals(listOf("first", "second"), received); assertTrue(failure.isEmpty())
            }
        }
    }
}
