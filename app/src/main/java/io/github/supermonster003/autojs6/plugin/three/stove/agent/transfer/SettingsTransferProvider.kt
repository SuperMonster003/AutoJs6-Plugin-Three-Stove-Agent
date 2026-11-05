package io.github.supermonster003.autojs6.plugin.three.stove.agent.transfer

import android.content.Context
import io.github.supermonster003.autojs6.plugin.three.stove.agent.mcp.McpServerProfile
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.AgentRuntime
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.AppearancePreferences
import org.autojs.plugin.common.api.PluginSettingsTransferProvider
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The settings transfer provider of AutoJs6 (contract v1, authority `<package>.autojs6.settings`, behind the PLUGIN
 * permission; roadmap P20, host settings roadmap P18.3). The document is [StoveSettingsTransfer]'s; it holds the MCP
 * bearer tokens, so an export with one is sensitive.
 *
 * The provider lives in the `:agent` process, where [AgentRuntime] owns the settings and the MCP repositories: reads
 * and writes go through them on their disk threads (this binder thread waits for the answer), so a running task, the
 * floating ball and the settings pages see an import at once, as after a save on the settings page. Like that page, the
 * MCP profiles are not replaced while a task runs. The appearance file is read on every screen start, so an import
 * shows the next time a screen of the plugin comes to the foreground. Nothing needs a restart.
 */
class SettingsTransferProvider : PluginSettingsTransferProvider() {

    override fun exportSettings(request: Request): Exported = StoveSettingsTransfer(RuntimeStorage(appContext())).exportSettings()

    override fun importSettings(settings: JSONObject, source: Source, request: Request): Imported {
        val counts = StoveSettingsTransfer(RuntimeStorage(appContext())).importSettings(settings)
        return Imported(counts.written, counts.skipped, restartNeeded = false)
    }

    private fun appContext(): Context = checkNotNull(context) { "The provider is not attached" }.applicationContext

    /** The stores of the agent runtime; one instance serves one export or import. */
    private class RuntimeStorage(private val context: Context) : StoveSettingsStorage {

        private val runtime = AgentRuntime.get(context)

        /** The whole export or import stays within the contract's 10 s call budget. */
        private val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(BUDGET_SECONDS)

        /** The MCP revision of the last [mcpProfiles], read on the repository's disk thread with the profiles. */
        private var mcpRevision: Long? = null

        override fun settings(): AgentSettings? = runCatching { await<AgentSettings> { done -> runtime.settings.query(complete = done) } }.getOrNull()

        override fun writeSettings(settings: AgentSettings) {
            await<AgentSettings> { done -> runtime.settings.query(settings, done) }
        }

        override fun appearance(): AppearancePreferences = AppearancePreferences.read(context)

        override fun writeAppearance(appearance: AppearancePreferences) = appearance.save(context)

        override fun mcpProfiles(): List<McpServerProfile>? = runCatching {
            await<Pair<List<McpServerProfile>, Long>> { done -> runtime.mcp.query { result -> done(result.map { it to runtime.mcp.revision }) } }
        }.getOrNull()?.let { (profiles, revision) ->
            mcpRevision = revision
            profiles
        }

        override fun writeMcpProfiles(profiles: List<McpServerProfile>): Boolean {
            val expected = checkNotNull(mcpRevision) { "MCP profiles not read" }
            if (!runtime.beginMaintenance()) return false
            try {
                if (runtime.mcp.revision != expected) return false
                await<List<McpServerProfile>> { done -> runtime.mcp.mutate(expected, { profiles }, done) }
                return true
            } finally {
                runtime.endMaintenance()
            }
        }

        /** Starts [action] and waits for the result it reports; throws its failure or when the budget runs out. */
        private fun <T> await(action: (done: (Result<T>) -> Unit) -> Unit): T {
            val result = AtomicReference<Result<T>>()
            val latch = CountDownLatch(1)
            action { value ->
                result.compareAndSet(null, value)
                latch.countDown()
            }
            check(latch.await(deadline - System.nanoTime(), TimeUnit.NANOSECONDS)) { "Settings store timed out" }
            return result.get().getOrThrow()
        }
    }

    private companion object {
        /** Below the contract's 10 s call budget, which leaves AutoJs6 time to receive the answer. */
        const val BUDGET_SECONDS = 8L
    }
}
