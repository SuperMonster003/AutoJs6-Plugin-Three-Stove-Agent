package io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog

import com.google.gson.JsonElement
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*

/** Observations are bounded valid JSON, not arbitrarily cut JSON or raw exception bodies. */
object ToolObservation {
    const val DEFAULT_MAX_BYTES = 24 * 1024
    fun success(result: JsonElement, maxBytes: Int = DEFAULT_MAX_BYTES): String {
        require(maxBytes in 256..256 * 1024)
        val full = jsonObject("ok" to true.json(), "result" to result).toString()
        if (full.utf8Size() <= maxBytes) return full
        val source = if (result.isJsonObject && result.asJsonObject.string("text") != null) result.asJsonObject.string("text")!! else result.toString()
        val record = jsonObject("ok" to true.json(), "truncated" to true.json(), "text" to "".json())
        // Include envelope and escaping in the actual byte budget; never split a surrogate pair.
        return ObservationCompactor.fitText(record, "text", source, maxBytes).toString()
    }

}
