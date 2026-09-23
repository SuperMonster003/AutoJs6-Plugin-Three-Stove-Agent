package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import com.google.gson.JsonElement
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

/** Observations are bounded valid JSON, not arbitrarily cut JSON or raw exception bodies. */
object ToolObservation {
    const val DEFAULT_MAX_BYTES = 24 * 1024
    fun success(result: JsonElement, maxBytes: Int = DEFAULT_MAX_BYTES): String {
        require(maxBytes in 256..256 * 1024)
        val full = jsonObject("ok" to true.json(), "result" to result).toString()
        if (full.toByteArray(Charsets.UTF_8).size <= maxBytes) return full
        val source = if (result.isJsonObject && result.asJsonObject.string("text") != null) result.asJsonObject.string("text")!! else result.toString()
        val record = jsonObject("ok" to true.json(), "truncated" to true.json(), "text" to "".json())
        // Include envelope and escaping in the actual byte budget; never split a surrogate pair.
        var lower = 0
        var upper = minOf(source.toByteArray(Charsets.UTF_8).size, maxBytes)
        while (lower < upper) {
            val middle = (lower + upper + 1) / 2
            record.addProperty("text", AgentJson.truncate(source, middle))
            if (record.toString().toByteArray(Charsets.UTF_8).size <= maxBytes) lower = middle else upper = middle - 1
        }
        record.addProperty("text", AgentJson.truncate(source, lower))
        return record.toString()
    }

    fun failure(category: String, stableDetail: String? = null, module: String? = null): String {
        val code = stableDetail?.takeIf { it in DETAILS } ?: when (category) {
            "process-dead" -> "LINK_DETACHED"
            "permission-denied", "capability-denied" -> "CAPABILITY_DENIED"
            "rate-limited" -> "RATE_LIMITED"
            "resource-limit" -> "LIMIT_EXCEEDED"
            "invalid-request" -> "TOOL_ARGUMENTS_INVALID"
            "timeout" -> if (module == "agent" || module == "engines") "SCRIPT_TIMEOUT" else "HOST_UNAVAILABLE"
            "runtime-error" -> if (module == "agent" || module == "engines") "SCRIPT_FAILED" else "HOST_UNAVAILABLE"
            else -> "HOST_UNAVAILABLE"
        }
        return jsonObject("error" to code.json(), "hint" to "Check the last observation and the tool prerequisites before retrying.".json()).toString()
    }
    private val DETAILS = setOf("A11Y_SERVICE_NOT_RUNNING", "NODE_REF_STALE", "NODE_NOT_FOUND", "SCREEN_LOCKED", "SCRIPT_NOT_REGISTERED", "SCRIPT_TIMEOUT", "SCRIPT_FAILED", "OCR_PLUGIN_REQUIRED", "CAPABILITY_DENIED", "QUOTA_EXCEEDED", "LIMIT_EXCEEDED", "RATE_LIMITED")
}
