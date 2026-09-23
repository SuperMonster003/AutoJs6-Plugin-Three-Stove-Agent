package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import io.github.supermonster003.autojs6.plugin.ai.agent.model.string
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*

/** The model can search metadata, but cannot supply roots or replace host approval. */
class ScriptCatalogTools(private val client: ScriptCatalogClient, roots: Set<String>, private val source: ScriptCatalogSource,
                         private val delegate: RunTools, private val allowed: Boolean) : RunTools {
    private val roots = roots.toSet()
    fun present(query: String, filter: Boolean, refresh: Boolean, timeoutMs: Long,
                callback: (PortResult<ScriptPresentation>) -> Unit): Cancellation {
        if (!allowed) { callback(PortResult.Failure(RunError.CAPABILITY_DENIED)); return Cancellation.NONE }
        return client.load(roots, refresh, timeoutMs, source) { result ->
            callback(when (result) {
                is PortResult.Failure -> result
                is PortResult.Success -> PortResult.Success(ScriptRanker.select(result.value, query, filter))
            })
        }
    }
    override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
        if (invocation.name == "script_catalog" && !allowed) {
            callback(PortResult.Failure(RunError.CAPABILITY_DENIED)); return Cancellation.NONE
        }
        return delegate.prepare(invocation, timeoutMs, callback)
    }
    override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
        if (prepared.invocation.name != "script_catalog") return delegate.execute(prepared, timeoutMs, callback)
        return present(prepared.invocation.arguments.string("query").orEmpty(), true, false, timeoutMs) { result ->
            callback(when (result) {
                is PortResult.Failure -> result
                is PortResult.Success -> PortResult.Success(ToolReply(result.value.render()))
            })
        }
    }
}
