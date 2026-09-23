package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class WireFailure(val code: String) : IllegalArgumentException(code)

/** Public contract envelopes. Descriptors are owned from entry, even when decoding is rejected. */
internal object AgentWire {
    fun envelope(key: String, json: String) = Bundle().apply { putInt(C.KEY_CONTRACT_VERSION, C.CONTRACT_VERSION); putString(key, json) }
    fun error(code: String) = Bundle().apply { putInt(C.KEY_CONTRACT_VERSION, C.CONTRACT_VERSION); putString(C.KEY_ERROR_CODE, code) }
    fun reason(code: String) = envelope(H.KEY_REASON_JSON, jsonObject("code" to code.json()).toString())
    /** Controls fit 32 KiB. Even a stalled descriptor is closed within the 200 ms Binder budget. */
    fun control(bundle: Bundle?, key: String, maximum: Int = C.MAX_EVENT_JSON_BYTES): String {
        if (bundle?.containsKey(C.KEY_PAYLOAD_FD) != true) return inline(bundle, key, maximum)
        return try { take(bundle, key, C.KEY_PAYLOAD_FD, maximum, maximum).use { it.read(75) } }
        catch (e: WireFailure) { throw WireFailure(if (e.code == C.ERROR_MODEL_TIMEOUT) C.ERROR_INVALID_REQUEST else e.code) }
        catch (_: Exception) { throw WireFailure(C.ERROR_INVALID_REQUEST) }
    }
    @Suppress("DEPRECATION")
    fun inline(bundle: Bundle?, key: String, maximum: Int = C.MAX_EVENT_JSON_BYTES): String {
        try {
            require(bundle != null && bundle.get(C.KEY_CONTRACT_VERSION) == C.CONTRACT_VERSION)
            require(!bundle.hasFileDescriptors())
            val value = bundle.get(key) as? String ?: throw WireFailure(C.ERROR_INVALID_REQUEST)
            require(value.toByteArray(Charsets.UTF_8).size <= maximum)
            return value
        } catch (failure: WireFailure) { throw failure }
        catch (_: Exception) { throw WireFailure(C.ERROR_INVALID_REQUEST) }
        finally { closeDescriptors(bundle) }
    }
    @Suppress("DEPRECATION")
    fun take(bundle: Bundle?, key: String, fdKey: String, inlineMaximum: Int, maximum: Int): OwnedJson {
        var descriptor: ParcelFileDescriptor? = null
        try {
            require(bundle != null && bundle.get(C.KEY_CONTRACT_VERSION) == C.CONTRACT_VERSION)
            descriptor = bundle.get(fdKey) as? ParcelFileDescriptor
            val inline = bundle.get(key) as? String
            require((inline == null) != (descriptor == null))
            require(inline == null || inline.toByteArray(Charsets.UTF_8).size <= inlineMaximum)
            closeDescriptors(bundle, descriptor)
            return OwnedJson(inline, descriptor, maximum)
        } catch (_: Exception) { closeDescriptors(bundle); throw WireFailure(C.ERROR_INVALID_REQUEST) }
    }
    @Suppress("DEPRECATION")
    fun closeDescriptors(bundle: Bundle?, except: ParcelFileDescriptor? = null) {
        if (bundle == null) return
        val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Any, Boolean>())
        fun close(item: Any?) {
            if (item == null || !seen.add(item)) return
            when (item) {
                is ParcelFileDescriptor -> if (item !== except) runCatching { item.close() }
                is Bundle -> runCatching { item.keySet().forEach { key -> close(runCatching { item.get(key) }.getOrNull()) } }
                is Array<*> -> item.forEach(::close)
                is List<*> -> item.forEach(::close)
            }
        }
        close(bundle)
    }
}

internal class OwnedJson(private val inline: String?, private val descriptor: ParcelFileDescriptor?, private val maximum: Int) : AutoCloseable {
    @Volatile private var closed = false
    fun read(timeoutMs: Long = 15_000): String {
        check(!closed)
        if (inline != null) return inline.also { require(it.toByteArray(Charsets.UTF_8).size <= maximum); AgentJson.checkUnicode(it) }
        val fd = checkNotNull(descriptor).fileDescriptor
        val end = SystemClock.elapsedRealtime() + timeoutMs.coerceIn(1, 300_000)
        val output = ByteArrayOutputStream()
        val bytes = ByteArray(8192)
        val poll = StructPollfd().apply { this.fd = fd; events = (OsConstants.POLLIN or OsConstants.POLLHUP or OsConstants.POLLERR).toShort() }
        while (!closed && !Thread.currentThread().isInterrupted) {
            val remaining = end - SystemClock.elapsedRealtime()
            if (remaining <= 0) throw WireFailure(C.ERROR_MODEL_TIMEOUT)
            if (Os.poll(arrayOf(poll), minOf(remaining, 100).toInt()) == 0) continue
            val count = Os.read(fd, bytes, 0, bytes.size)
            if (count == 0) return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(output.toByteArray())).toString()
            if (count > maximum - output.size()) throw WireFailure(C.ERROR_LIMIT_EXCEEDED)
            output.write(bytes, 0, count)
        }
        throw WireFailure(C.ERROR_CANCELLED)
    }
    override fun close() { closed = true; runCatching { descriptor?.close() } }
}

/** Separate bounded queues keep descriptor reads and remote callbacks away from run/control threads. */
internal class LinkWorkers : AutoCloseable {
    val io = pool("ai-agent-io", 2, 32)
    val reads = pool("ai-agent-payload", 2, 32)
    val callbacks = pool("ai-agent-callback", 1, 64)
    override fun close() { listOf(io, reads, callbacks).forEach { it.shutdown() } }
    companion object {
        private fun pool(name: String, count: Int, capacity: Int) = ThreadPoolExecutor(count, count, 10, TimeUnit.SECONDS,
            ArrayBlockingQueue<Runnable>(capacity), { task -> Thread(task, name).apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy())
    }
}

internal class OrderedModelEvents(private val workers: LinkWorkers, private val consume: (String) -> Unit,
                                  private val failed: (String) -> Unit) : AutoCloseable {
    private val pending = ArrayDeque<OwnedJson>()
    private var reading: OwnedJson? = null
    private var scheduled = false
    private var closed = false
    @Synchronized fun accept(value: OwnedJson) {
        if (closed) { value.close(); return }
        if (pending.size >= 32) { value.close(); close(); failed(C.ERROR_LIMIT_EXCEEDED); return }
        pending.addLast(value)
        if (scheduled) return
        scheduled = true
        try { workers.reads.execute(::drain) } catch (_: Exception) { close(); failed(C.ERROR_HOST_UNAVAILABLE) }
    }
    private fun drain() {
        while (true) {
            val item = synchronized(this) {
                if (closed || pending.isEmpty()) { scheduled = false; return }
                pending.removeFirst().also { reading = it }
            }
            try { item.use { consume(it.read()) } }
            catch (failure: Exception) { if (!synchronized(this) { closed }) failed((failure as? WireFailure)?.code ?: C.ERROR_INVALID_REQUEST); close() }
            finally { synchronized(this) { reading = null } }
        }
    }
    @Synchronized override fun close() { closed = true; reading?.close(); pending.forEach(OwnedJson::close); pending.clear() }
}
