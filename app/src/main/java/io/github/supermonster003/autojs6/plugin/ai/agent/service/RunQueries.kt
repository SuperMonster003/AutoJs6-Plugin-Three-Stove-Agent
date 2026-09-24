package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C

/** Read-only private history survives link loss. Both endpoints use the same bounded projections. */
internal class RunQueries(private val archive: RunArchive) {
    fun list(query: Bundle?): Bundle = answer {
        val value = AgentJson.objectOf(AgentWire.control(query, C.KEY_RUN_REQUEST_JSON))
        ControlRequests.closed(value, setOf("limit", "offset"))
        val limit = ControlRequests.number(value, "limit", 20, 50).toInt()
        val offset = if (value.has("offset")) requireNotNull(value.number("offset")).also { require(it in 0..1000) }.toInt() else 0
        archive.list(limit, offset).toString()
    }
    fun get(reference: Bundle?): Bundle = answer {
        val value = AgentJson.objectOf(AgentWire.control(reference, C.KEY_RUN_REF_JSON))
        ControlRequests.closed(value, setOf("runId", "limit"))
        val row = archive.get(ControlRequests.runId(value), ControlRequests.number(value, "limit", 50, 50).toInt())
            ?: throw WireFailure(C.ERROR_RUN_NOT_FOUND)
        row.toString()
    }
    private fun answer(body: () -> String): Bundle = try { AgentWire.envelope(C.KEY_RUN_RESPONSE_JSON, body()) }
        catch (e: Exception) { AgentWire.error((e as? WireFailure)?.code ?: C.ERROR_INVALID_REQUEST) }
}
