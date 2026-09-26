package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.content.Context
import io.github.supermonster003.autojs6.plugin.ai.agent.mcp.McpServerProfile
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import java.io.File
import java.util.Collections
import java.util.concurrent.*

/** Publication follows an authenticated read or durable encrypted write. Failed loads stay unavailable. */
internal class McpRepository internal constructor(private val store: McpStore, private val changed: () -> Unit = {}) {
    constructor(context: Context, changed: () -> Unit = {}) : this(
        McpStore(File(context.noBackupFilesDir, "mcp-profiles.enc"), AndroidMcpEncryption()), changed)
    private val disk = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(16),
        { work -> Thread(work, "ai-agent-mcp-store").apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy())
    @Volatile private var loaded: List<McpServerProfile>? = null
    // A restarted service must reject a draft acknowledged by a previous process as well.
    @Volatile var revision = java.util.UUID.randomUUID().leastSignificantBits ushr 2; private set
    init { disk.execute { runCatching { loaded = freeze(store.open()); changed() } } }
    fun snapshot(): List<McpServerProfile> = checkNotNull(loaded) { "MCP settings unavailable" }
    fun query(complete: (Result<List<McpServerProfile>>) -> Unit) = submit({ snapshot() }, complete)
    fun mutate(expectedRevision: Long, transform: (List<McpServerProfile>) -> List<McpServerProfile>,
               complete: (Result<List<McpServerProfile>>) -> Unit) = submit({
        check(revision == expectedRevision) { "MCP settings changed" }
        val next = freeze(transform(snapshot()))
        store.save(next); loaded = next; revision++; changed(); next
    }, complete)
    private fun submit(action: () -> List<McpServerProfile>, complete: (Result<List<McpServerProfile>>) -> Unit) {
        try { disk.execute { complete(runCatching(action)) } } catch (failure: Exception) { complete(Result.failure(failure)) }
    }
    private fun freeze(rows: List<McpServerProfile>) = Collections.unmodifiableList(rows.map {
        it.copy(selectedTools = Collections.unmodifiableList(it.selectedTools.toList()))
    })
    internal fun close() { disk.shutdownNow() }
}
