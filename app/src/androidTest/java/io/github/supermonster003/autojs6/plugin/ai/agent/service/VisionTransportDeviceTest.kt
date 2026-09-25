package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.graphics.*
import android.os.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.BridgeCall
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.autojs.plugin.ai.agent.api.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import org.autojs.plugin.host.capability.api.*
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference

/** Synthetic pixels only. No network or physical-device screenshot is needed. */
class VisionTransportDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun png(w: Int = 160, h: Int = 240): ByteArray {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(60, 140, 200))
        return try { ByteArrayOutputStream().also { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }.toByteArray() }
        finally { bitmap.recycle() }
    }
    private fun file(bytes: ByteArray): ParcelFileDescriptor {
        val file = File.createTempFile("vision-fixture-", ".bin", context.cacheDir)
        return try { file.writeBytes(bytes); ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) } finally { file.delete() }
    }
    @Test fun portraitLandscapeAndSmallImagesUseActualJpegAndOriginalAspectRatio() {
        for ((width, height) in listOf(1920 to 1080, 1080 to 1920, 160 to 240)) {
            val image = ScreenImageCodec.convert(png(width, height), "image/png", width, height)
            assertEquals("image/jpeg", image.mimeType); assertTrue(maxOf(image.width, image.height) <= 1280)
            assertEquals(width.toDouble() / height, image.width.toDouble() / image.height, .002)
            val bytes = ByteArrayOutputStream().also(image::writeTo).toByteArray()
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            assertEquals("image/jpeg", options.outMimeType); assertEquals(image.width, options.outWidth); assertEquals(image.height, options.outHeight)
        }
    }
    @Test fun advertisedImageMetadataMustMatchDecodedContent() {
        val bytes = png()
        assertThrows(IllegalArgumentException::class.java) { ScreenImageCodec.convert(bytes, "image/jpeg", 160, 240) }
        assertThrows(IllegalArgumentException::class.java) { ScreenImageCodec.convert(bytes, "image/png", 161, 240) }
        assertThrows(IllegalArgumentException::class.java) { ScreenImageCodec.convert(ByteArray(100), "image/png", 160, 240) }
        assertThrows(IllegalArgumentException::class.java) { ScreenImageCodec.convert(bytes, "image/png", 5000, 240) }
    }
    @SdkSuppress(minSdkVersion = 30)
    @Test fun payloadLengthAndCurrentOffsetAreEnforcedAndSourceCloses() {
        val bytes = png()
        val valid = file(byteArrayOf(1, 2, 3) + bytes)
        android.system.Os.lseek(valid.fileDescriptor, 3, android.system.OsConstants.SEEK_SET)
        assertArrayEquals(bytes, ScreenPayload(valid, bytes.size).read(SystemClock.elapsedRealtime() + 3000))
        assertFalse(valid.fileDescriptor.valid())
        for (size in listOf(bytes.size - 1, bytes.size + 1)) {
            val fd = file(bytes)
            assertThrows(IllegalArgumentException::class.java) { ScreenPayload(fd, size).read(SystemClock.elapsedRealtime() + 3000) }
            assertFalse(fd.fileDescriptor.valid())
        }
    }
    @SdkSuppress(minSdkVersion = 30)
    @Test fun stalledPipeCancellationEndsWorkerWithoutWaitingForWriter() {
        val pipe = ParcelFileDescriptor.createReliablePipe()
        val payload = ScreenPayload(pipe[0], 1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val entered = CountDownLatch(1)
            val future = executor.submit<Throwable?> { entered.countDown(); runCatching { payload.read(SystemClock.elapsedRealtime() + 20_000) }.exceptionOrNull() }
            assertTrue(entered.await(1, TimeUnit.SECONDS)); payload.close()
            assertNotNull(future.get(2, TimeUnit.SECONDS)); assertFalse(pipe[0].fileDescriptor.valid())
        } finally { payload.close(); pipe[1].close(); executor.shutdownNow() }
    }
    @SdkSuppress(minSdkVersion = 30)
    @Test fun stalledPipeDeadlineAndReliableProducerErrorCannotDeliverAnImage() {
        val stalled = ParcelFileDescriptor.createReliablePipe()
        try {
            val failure = assertThrows(WireFailure::class.java) { ScreenPayload(stalled[0], 1).read(SystemClock.elapsedRealtime() + 150) }
            assertEquals("BUDGET_EXCEEDED", failure.code); assertFalse(stalled[0].fileDescriptor.valid())
        } finally { stalled[0].close(); stalled[1].close() }
        val broken = ParcelFileDescriptor.createReliablePipe()
        ParcelFileDescriptor.AutoCloseOutputStream(ParcelFileDescriptor.dup(broken[1].fileDescriptor)).use { it.write(1) }
        broken[1].closeWithError("Synthetic producer failure")
        assertThrows(IOException::class.java) { ScreenPayload(broken[0], 1).read(SystemClock.elapsedRealtime() + 2000) }
        assertFalse(broken[0].fileDescriptor.valid())
    }
    private fun response(id: String, fd: ParcelFileDescriptor, bytes: Int, width: Int = 160): Bundle = Bundle().apply {
        putBoolean(H.KEY_BRIDGE_RESPONSE_OK, true); putLong(H.KEY_BRIDGE_PAYLOAD_BYTES, bytes.toLong())
        putString(H.KEY_BRIDGE_PAYLOAD_MIME, "image/png"); putParcelable(H.KEY_BRIDGE_PAYLOAD_FD, fd)
        putString(H.KEY_BRIDGE_RESPONSE_JSON, jsonObject("id" to id.json(), "ok" to true.json(), "result" to jsonObject(
            "schema" to "autojs6-bridge-accessibility-screenshot-v1".json(), "mime" to "image/png".json(), "bytes" to bytes.json(),
            "width" to width.json(), "height" to 240.json(), "payload" to jsonObject("kind" to "descriptor".json(), "mime" to "image/png".json(),
                "bytes" to bytes.json(), "oneShot" to true.json()))).toString())
    }
    private fun broker(answer: (String, IHostCapabilityCallback) -> Unit) = object : IHostCapabilityBroker.Stub() {
        override fun getBrokerInfo() = Bundle()
        override fun dispatch(request: Bundle, callback: IHostCapabilityCallback) { answer(AgentJson.objectOf(request.getString(H.KEY_BRIDGE_REQUEST_JSON)!!).string("id")!!, callback) }
        override fun destroy(reason: Bundle?) = Unit
    }
    private val call get() = BridgeCall("accessibility", "screenshot", jsonArray(jsonObject("format" to "png".json())), listOf("accessibility", "screen_capture"), 3000)
    @SdkSuppress(minSdkVersion = 30)
    @Test fun captureDeliveryContainsMetadataAndOneImageAndClosesDuplicates() {
        val bytes = png(); val first = file(bytes); val duplicate = file(bytes)
        val result = AtomicReference<PortResult<ToolReply>>(); val done = CountDownLatch(1)
        LinkWorkers().use { workers ->
            val adapter = ScreenCaptureTransport(context, broker { id, cb -> cb.onResponse(response(id, first, bytes.size)); cb.onResponse(response(id, duplicate, bytes.size)) }, Process.myUid(), workers) { true }
            adapter.execute(call, 32768) { result.set(it); done.countDown() }
            assertTrue(done.await(5, TimeUnit.SECONDS))
            val reply = (result.get() as PortResult.Success).value
            assertEquals(1, reply.images.size); assertEquals(160L, reply.result.asJsonObject.number("screenWidth"))
            assertEquals(240L, reply.result.asJsonObject.number("screenHeight")); assertEquals(70L, reply.result.asJsonObject.number("quality"))
            assertFalse(reply.result.toString().contains("sha256")); workers.reads.shutdown(); assertTrue(workers.reads.awaitTermination(2, TimeUnit.SECONDS))
            assertFalse(first.fileDescriptor.valid()); assertFalse(duplicate.fileDescriptor.valid())
        }
    }
    @SdkSuppress(minSdkVersion = 30)
    @Test fun invalidCaptureMetadataClosesDescriptorWithoutDeliveringImage() {
        val bytes = png(); val fd = file(bytes); val result = AtomicReference<PortResult<ToolReply>>(); val done = CountDownLatch(1)
        LinkWorkers().use { workers ->
            ScreenCaptureTransport(context, broker { id, cb -> cb.onResponse(response(id, fd, bytes.size, 161)) }, Process.myUid(), workers) { true }
                .execute(call, 32768) { result.set(it); done.countDown() }
            assertTrue(done.await(5, TimeUnit.SECONDS)); assertTrue(result.get() is PortResult.Failure)
            workers.reads.shutdown(); assertTrue(workers.reads.awaitTermination(2, TimeUnit.SECONDS)); assertFalse(fd.fileDescriptor.valid())
        }
    }
    @SdkSuppress(minSdkVersion = 30)
    @Test fun lateCancelledAndWrongUidResponsesCloseTheirDescriptors() {
        for (wrongUid in listOf(false, true)) {
            val callback = AtomicReference<Pair<String, IHostCapabilityCallback>>(); val called = CountDownLatch(1)
            val done = CountDownLatch(1); val bytes = png(); val fd = file(bytes)
            LinkWorkers().use { workers ->
                val adapter = ScreenCaptureTransport(context, broker { id, cb -> callback.set(id to cb); called.countDown() },
                    if (wrongUid) Process.myUid() + 1 else Process.myUid(), workers) { true }
                val cancellation = adapter.execute(call, 32768) { done.countDown() }
                assertTrue(called.await(3, TimeUnit.SECONDS))
                if (!wrongUid) cancellation.cancel()
                val (id, cb) = callback.get(); cb.onResponse(response(id, fd, bytes.size))
                assertFalse(fd.fileDescriptor.valid()); assertEquals(1L, done.count); cancellation.cancel()
            }
        }
    }
    @Test fun modelInitialAndContinuationDescriptorsAreReadOnlyUnlinkedAndClosedAfterSend() {
        val image = ScreenImageCodec.convert(png(), "image/png", 160, 240)
        val descriptors = mutableListOf<ParcelFileDescriptor>(); val requests = LinkedBlockingQueue<String>()
        val failures = LinkedBlockingQueue<RunError>()
        val remote = object : IAiAgentModelBroker.Stub() {
            override fun getBrokerInfo() = Bundle()
            override fun listTargets(request: Bundle, callback: IAiAgentModelCallback) = Unit
            @Suppress("DEPRECATION") private fun inspect(request: Bundle) {
                val fd = request.getParcelableArray(C.KEY_MODEL_IMAGE_FDS)!!.single() as ParcelFileDescriptor
                descriptors += fd
                assertTrue(android.system.Os.readlink("/proc/self/fd/${fd.fd}").endsWith("(deleted)"))
                assertThrows(android.system.ErrnoException::class.java) { android.system.Os.write(fd.fileDescriptor, byteArrayOf(0), 0, 1) }
                val bytes = ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(fd.fileDescriptor)).use { it.readBytes() }
                assertEquals(image.byteCount, bytes.size.toLong())
                val text = request.getString(C.KEY_MODEL_REQUEST_JSON) ?: ParcelFileDescriptor.AutoCloseInputStream(
                    ParcelFileDescriptor.dup(requireNotNull(request.getParcelable<ParcelFileDescriptor>(C.KEY_PAYLOAD_FD)).fileDescriptor)).bufferedReader().use { it.readText() }
                requests.add(AgentJson.objectOf(text, 2 * 1024 * 1024).string("requestId")!!)
            }
            override fun generate(request: Bundle, callback: IAiAgentModelCallback) = inspect(request)
            override fun submitToolResults(request: Bundle) = inspect(request)
            override fun cancel(reference: Bundle?) = Unit
            override fun destroy(reason: Bundle?) = Unit
        }
        LinkWorkers().use { workers -> BinderModelBroker(context, remote, Process.myUid(), workers).use { adapter ->
            adapter.generate(jsonObject("requestId" to "vision-test".json(), "padding" to "x".repeat(C.MAX_MODEL_REQUEST_INLINE_BYTES).json()).toString(), listOf(image), {}, failures::add)
            assertEquals("vision-test", requests.poll(5, TimeUnit.SECONDS))
            adapter.submitToolResults("{\"requestId\":\"vision-test\",\"round\":1,\"results\":[]}", listOf(image), failures::add)
            assertEquals("vision-test", requests.poll(5, TimeUnit.SECONDS))
            workers.io.shutdown(); assertTrue(workers.io.awaitTermination(2, TimeUnit.SECONDS))
            assertTrue(failures.isEmpty()); assertEquals(2, descriptors.size); assertTrue(descriptors.all { !it.fileDescriptor.valid() })
            assertTrue(context.cacheDir.listFiles()!!.none { it.name.startsWith("agent-image-") || it.name.startsWith("agent-model-") })
        } }
    }
}
