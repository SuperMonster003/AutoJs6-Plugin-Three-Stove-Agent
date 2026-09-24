package io.github.supermonster003.autojs6.plugin.ai.agent.store

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

/** Diagnostic export is an allowlist, not a promise that regexes can find personal data in prose. */
internal object RunHistoryExport {
    fun redact(run: JsonObject, toolNames: Set<String>): JsonObject {
        fun counters(value: JsonObject?, names: List<String>) = JsonObject().apply {
            names.forEach { key -> value?.number(key)?.takeIf { it >= 0 }?.let { addProperty(key, it) } }
            value?.flag("estimated")?.let { addProperty("estimated", it) }
        }
        fun usage(value: JsonObject?) = counters(value, listOf("modelCalls", "inputTokens", "outputTokens", "totalTokens"))
        val result = jsonObject("version" to 1.json(), "redacted" to true.json(), "runId" to RunHistoryCodec.id(run.string("runId")!!).json(),
            "state" to run.string("state")!!.takeIf { it in RunHistoryCodec.states }!!.json(),
            "goal" to "[redacted]".json(), "preset" to "[redacted]".json(), "truncated" to (run.flag("truncated") == true).json())
        result.add("steps", JsonArray().apply { run.getAsJsonArray("steps").forEach { value ->
            val source = value.asJsonObject
            add(counters(source, listOf("index", "elapsedMs")).apply {
                source.string("kind")?.takeIf { it in setOf("tool", "ask", "done", "repair", "error") }?.let { addProperty("kind", it) }
                source.string("tool")?.takeIf { it in toolNames }?.let { addProperty("tool", it) }
                source.string("confirmation")?.takeIf { it in setOf("auto", "allowed", "denied") }?.let { addProperty("confirmation", it) }
                for (key in listOf("decision", "arguments", "observation", "error")) if (source.has(key)) addProperty(key, "[redacted]")
                add("usage", usage(source.getAsJsonObject("usage")))
            })
        } })
        run.getAsJsonObject("result")?.let { source -> result.add("result", counters(source, listOf("steps", "toolCalls", "durationMs")).apply {
            source.string("status")?.takeIf { it in RunHistoryCodec.terminal }?.let { addProperty("status", it) }
            add("usage", usage(source.getAsJsonObject("usage")))
            for (key in listOf("summary", "evidence", "unfinished", "error", "script")) if (source.has(key)) addProperty(key, "[redacted]")
        }) }
        return result
    }
}
