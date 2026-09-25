package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.BridgeCall
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.autojs.plugin.host.capability.api.IHostCapabilityBroker
import org.autojs.plugin.host.capability.api.IHostCapabilityCallback
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

/** The host captures; only bounded conversion and forwarding run inside the Agent. */
internal class ScreenCaptureTransport(private val context: Context, private val broker: IHostCapabilityBroker,
                                      private val ownerUid: Int, private val workers: LinkWorkers, private val alive: () -> Boolean) {
    fun execute(call: BridgeCall, maximumRequestBytes: Int, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
        if (Build.VERSION.SDK_INT < 30) { callback(PortResult.Failure(RunError.TARGET_UNSUPPORTED)); return Cancellation.NONE }
        val id = "capture-${UUID.randomUUID()}"
        val closed = AtomicBoolean()
        val claimed = AtomicBoolean()
        val payload = AtomicReference<ScreenPayload?>()
        val end = SystemClock.elapsedRealtime() + call.timeoutMs
        fun finish(value: PortResult<ToolReply>) { if (closed.compareAndSet(false, true)) callback(value) }
        if (!unlocked()) { finish(PortResult.Failure(RunError.SCREEN_LOCKED)); return Cancellation.NONE }
        val json = call.envelope(id).toString()
        if (json.toByteArray().size > maximumRequestBytes) { finish(PortResult.Failure(RunError.LIMIT_EXCEEDED)); return Cancellation.NONE }
        val remote = object : IHostCapabilityCallback.Stub() {
            @Suppress("DEPRECATION")
            override fun onResponse(response: Bundle?) {
                if (Binder.getCallingUid() != ownerUid || closed.get() || !claimed.compareAndSet(false, true)) {
                    AgentWire.closeDescriptors(response); return
                }
                var owned: ScreenPayload? = null
                try {
                    require(response != null && (!response.containsKey(H.KEY_CONTRACT_VERSION) || response.get(H.KEY_CONTRACT_VERSION) == H.CONTRACT_VERSION))
                    val text = response.get(H.KEY_BRIDGE_RESPONSE_JSON) as? String ?: error("Missing screenshot response")
                    val ok = response.get(H.KEY_BRIDGE_RESPONSE_OK) as? Boolean ?: error("Missing screenshot status")
                    val envelope = AgentJson.objectOf(text, H.MAX_BRIDGE_INLINE_JSON_BYTES)
                    require(envelope.string("id") == id && envelope.flag("ok") == ok)
                    if (!ok) {
                        AgentWire.closeDescriptors(response)
                        finish(PortResult.Failure(BinderRunTools.bridgeError(envelope.getAsJsonObject("error")))); return
                    }
                    val result = requireNotNull(envelope.getAsJsonObject("result"))
                    val fd = response.get(H.KEY_BRIDGE_PAYLOAD_FD) as? ParcelFileDescriptor ?: error("Missing screenshot descriptor")
                    val count = response.get(H.KEY_BRIDGE_PAYLOAD_BYTES) as? Long ?: error("Missing screenshot size")
                    val mime = response.getString(H.KEY_BRIDGE_PAYLOAD_MIME)
                    val marker = requireNotNull(result.getAsJsonObject("payload"))
                    require(count in 1..H.MAX_BRIDGE_PAYLOAD_BYTES && mime in setOf("image/png", "image/jpeg"))
                    require(result.string("schema") == "autojs6-bridge-accessibility-screenshot-v1" && result.string("mime") == mime && result.number("bytes") == count)
                    require(marker.string("kind") == "descriptor" && marker.string("mime") == mime && marker.number("bytes") == count && marker.flag("oneShot") == true)
                    val width = requireNotNull(result.number("width")).also { require(it in 1..4096) }.toInt()
                    val height = requireNotNull(result.number("height")).also { require(it in 1..4096) }.toInt()
                    owned = ScreenPayload(fd, count.toInt()); payload.set(owned)
                    AgentWire.closeDescriptors(response, fd)
                    val data = owned
                    workers.reads.execute {
                        var bytes: ByteArray? = null
                        try {
                            if (closed.get()) return@execute
                            bytes = data.read(end)
                            if (closed.get()) return@execute
                            val image = ScreenImageCodec.convert(bytes, checkNotNull(mime), width, height)
                            if (!unlocked()) { finish(PortResult.Failure(RunError.SCREEN_LOCKED)); return@execute }
                            if (SystemClock.elapsedRealtime() >= end) throw WireFailure("BUDGET_EXCEEDED")
                            finish(PortResult.Success(ToolReply(jsonObject("captured" to true.json(), "mimeType" to image.mimeType.json(),
                                "width" to image.width.json(), "height" to image.height.json(), "byteLength" to image.byteCount.json(),
                                "screenWidth" to width.json(), "screenHeight" to height.json(), "quality" to ScreenImageCodec.QUALITY.json()), images = listOf(image))))
                        } catch (failure: Exception) {
                            finish(PortResult.Failure(if ((failure as? WireFailure)?.code == "BUDGET_EXCEEDED") RunError.BUDGET_EXCEEDED else RunError.TOOL_ARGUMENTS_INVALID))
                        } finally { bytes?.fill(0); data.close(); payload.compareAndSet(data, null) }
                    }
                } catch (_: Exception) { owned?.close(); AgentWire.closeDescriptors(response); finish(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID)) }
            }
        }
        try { workers.io.execute {
            if (closed.get()) return@execute
            if (!alive()) { finish(PortResult.Failure(RunError.HOST_UNAVAILABLE)); return@execute }
            if (!unlocked()) { finish(PortResult.Failure(RunError.SCREEN_LOCKED)); return@execute }
            try { broker.dispatch(AgentWire.envelope(H.KEY_BRIDGE_REQUEST_JSON, json), remote) }
            catch (_: Exception) { finish(PortResult.Failure(RunError.HOST_UNAVAILABLE)) }
        } } catch (_: Exception) { finish(PortResult.Failure(RunError.HOST_UNAVAILABLE)) }
        return Cancellation { closed.set(true); payload.getAndSet(null)?.close() }
    }

    private fun unlocked(): Boolean = context.getSystemService(KeyguardManager::class.java)?.let { !it.isKeyguardLocked && !it.isDeviceLocked } == true &&
        context.getSystemService(PowerManager::class.java)?.isInteractive == true
}

internal object ScreenImageCodec {
    const val LONGEST_EDGE = 1280
    const val QUALITY = 70
    fun convert(bytes: ByteArray, mime: String, width: Int, height: Int): ModelImage {
        require(bytes.size in 1..H.MAX_BRIDGE_PAYLOAD_BYTES && width in 1..4096 && height in 1..4096)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(mime in setOf("image/png", "image/jpeg") && bounds.outMimeType == mime && bounds.outWidth == width && bounds.outHeight == height)
        val decoded = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply {
            inSampleSize = maxOf(1, maxOf(width, height) / LONGEST_EDGE)
        }))
        var scaled: Bitmap? = null
        try {
            val factor = minOf(1.0, LONGEST_EDGE.toDouble() / maxOf(width, height))
            val w = (width * factor).roundToInt().coerceIn(1, LONGEST_EDGE)
            val h = (height * factor).roundToInt().coerceIn(1, LONGEST_EDGE)
            val bitmap = if (decoded.width == w && decoded.height == h) decoded else Bitmap.createScaledBitmap(decoded, w, h, true).also { scaled = it }
            val encoded = ByteArrayOutputStream().also { check(bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it)) }.toByteArray()
            return try { ModelImage(encoded, w, h) } finally { encoded.fill(0) }
        } finally { scaled?.recycle(); decoded.recycle() }
    }
}

/** A worker-owned duplicate remains open until exit, including concurrent cancellation. */
internal class ScreenPayload(private val source: ParcelFileDescriptor, private val count: Int) : AutoCloseable {
    init { require(count in 1..H.MAX_BRIDGE_PAYLOAD_BYTES) }
    private val closed = AtomicBoolean()
    fun read(end: Long): ByteArray {
        if (Build.VERSION.SDK_INT < 30) throw WireFailure("TARGET_UNSUPPORTED")
        val fd = synchronized(this) {
            check(!closed.get())
            // Reopening /proc/self/fd checks the foreign app's inode permissions again.
            // dup preserves the Binder-granted access and current offset. This one-shot
            // payload has one consumer, so sharing O_NONBLOCK with the sender is safe.
            Os.dup(source.fileDescriptor).also { copy ->
                try {
                    Os.fcntlInt(copy, OsConstants.F_SETFL, Os.fcntlInt(copy, OsConstants.F_GETFL, 0) or OsConstants.O_NONBLOCK)
                    Os.fcntlInt(copy, OsConstants.F_SETFD, OsConstants.FD_CLOEXEC)
                }
                catch (failure: Exception) { Os.close(copy); throw failure }
            }
        }
        val bytes = ByteArray(count)
        try {
            var position = 0
            val extra = ByteArray(1)
            val poll = StructPollfd().apply { this.fd = fd; events = OsConstants.POLLIN.toShort() }
            while (true) {
                check(!closed.get() && !Thread.currentThread().isInterrupted)
                val remaining = end - SystemClock.elapsedRealtime()
                if (remaining <= 0) throw WireFailure("BUDGET_EXCEEDED")
                val n = try {
                    if (Os.poll(arrayOf(poll), minOf(remaining, 100).toInt()) == 0) continue
                    if (position < bytes.size) Os.read(fd, bytes, position, bytes.size - position) else Os.read(fd, extra, 0, 1)
                } catch (failure: ErrnoException) {
                    if (failure.errno == OsConstants.EAGAIN || failure.errno == OsConstants.EINTR) continue
                    throw failure
                }
                if (n == 0) break
                require(position < bytes.size)
                position += n
            }
            require(position == bytes.size)
            source.checkError()
            return bytes
        } catch (failure: Exception) { bytes.fill(0); throw failure }
        finally { Os.close(fd); close() }
    }
    @Synchronized override fun close() { if (closed.compareAndSet(false, true)) runCatching { source.close() } }
}
