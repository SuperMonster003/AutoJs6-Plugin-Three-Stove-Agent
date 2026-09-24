package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptPresentation

data class ContextLimits(
    val maximumBytes: Int = 64 * 1024,
    val grantMaximumBytes: Int = 128 * 1024,
    val recentPairs: Int = 8,
    val localInputTokens: Int = 3000,
) {
    init {
        require(maximumBytes in 1..128 * 1024 && grantMaximumBytes in 1..2 * 1024 * 1024)
        require(recentPairs in 0..32 && localInputTokens in 1..3000)
    }
}

class ContextLimitExceeded : IllegalArgumentException("Context cannot retain the mandatory task, rules and observation within the input budget")

/** Deterministic byte packing. The goal and rules are never silently shortened to make a request fit. */
class ContextCompiler(
    private val prompts: PromptCatalog, private val catalog: ToolCatalog, private val policy: ToolPolicy,
    private val target: ModelTarget, private val initialFormat: DecisionFormat,
    private val limits: ContextLimits = ContextLimits(),
    private val fixedContext: String = "", memories: JsonArray = JsonArray(), private val scripts: ScriptPresentation? = null,
    private val memoryTruncated: Boolean = false, private val memoryUnavailable: Boolean = false,
    private val memoryScopes: List<String>? = null,
) : RunContextCompiler {
    private val memories = AgentJson.parse(memories.toString(), 4096).asJsonArray
    private val local = target.locality == ModelLocality.ON_DEVICE
    val maximumBytes = minOf(limits.maximumBytes, limits.grantMaximumBytes, target.maximumContextBytes,
        if (local) limits.localInputTokens * 5 / 2 else Int.MAX_VALUE)
    init { require(fixedContext.toByteArray(Charsets.UTF_8).size <= 8192); AgentJson.checkUnicode(fixedContext) }

    override fun observe(tool: String, result: JsonElement): String {
        val envelope = jsonObject("ok" to true.json(), "result" to result)
        return ObservationCompactor.compact(envelope, ToolObservation.DEFAULT_MAX_BYTES, local && tool == "ui_dump").toString()
    }

    override fun compile(context: RunContext): ModelInput {
        val format = context.format ?: initialFormat
        val schemaBytes = format.responseSchemaJson?.toByteArray(Charsets.UTF_8)?.size ?: 0
        val language = language(context.goal, context.locale)
        val goal = prompts.goal(language, context.goal)
        require(context.history.size <= RunLimits.STEPS)
        val history = context.history.map { AgentJson.objectOf(it.toString(), 12 * 1024) }
        val current = context.observation?.let { AgentJson.parse(it, ToolObservation.DEFAULT_MAX_BYTES) }
            ?: jsonObject("initial" to true.json())
        val repair = context.repair?.let { prompts.repair(language, it) }
        val budget = AgentJson.objectOf(context.remainingBudget.toString(), 4096)
        var observationBytes = minOf(ToolObservation.DEFAULT_MAX_BYTES, maximumBytes.coerceAtLeast(128))
        var retained = minOf(limits.recentPairs, history.size)
        var summaryCount = (history.size - retained).coerceAtMost(32)
        var contextBytes = fixedContext.toByteArray(Charsets.UTF_8).size
        var memoryCount = memories.size()
        var compact = local
        var scriptCount = scripts?.size ?: 0

        fun message(role: String, content: String) = jsonObject("role" to role.json(), "content" to content.json())
        fun summary(record: JsonObject): String = jsonObject("index" to (record["index"] ?: JsonNull.INSTANCE),
            "kind" to (record["kind"] ?: JsonNull.INSTANCE), "tool" to (record["tool"] ?: JsonNull.INSTANCE),
            "confirmation" to (record["confirmation"] ?: JsonNull.INSTANCE), "error" to (record["error"] ?: JsonNull.INSTANCE),
            "observation" to AgentJson.truncate(record.string("observation").orEmpty(), 120).json()).toString()
        fun build(): JsonArray {
            val older = history.dropLast(retained).takeLast(summaryCount)
            val memory = JsonArray().apply { memories.take(memoryCount).forEach { add(it.deepCopy()) } }
            val messages = jsonArray(message("system", prompts.system(language, policy, format,
                AgentJson.truncate(fixedContext, contextBytes), memory, memoryTruncated || memoryCount != memories.size(), compact,
                contextBytes < fixedContext.toByteArray(Charsets.UTF_8).size, scripts?.render(limit = scriptCount), memoryUnavailable, context.guidance, memoryScopes)), message("user", goal))
            if (older.isNotEmpty()) messages.add(message("user", prompts.context(language, "summary", JsonArray().apply { older.forEach { add(summary(it)) } })))
            for (record in history.takeLast(retained)) {
                val decision = record.getAsJsonObject("decision")?.deepCopy()
                if (decision?.string("kind") in listOf("tool", "ask", "done") && record.flag("truncated") != true) {
                    decision!!.remove("parseMode"); decision.remove("repairs"); decision.remove("degraded"); decision.remove("rejections")
                    if (decision.string("kind") == "tool" && format.argumentsEncoding == ArgumentsEncoding.JSON_STRING) {
                        decision["arguments"]?.let { decision.addProperty("arguments", it.toString()) }
                    }
                    messages.add(message("assistant", decision.toString()))
                    messages.add(message("user", prompts.context(language, "observation", jsonObject(
                        "index" to (record["index"] ?: JsonNull.INSTANCE), "observation" to record.string("observation").orEmpty().json(),
                        "confirmation" to (record["confirmation"] ?: JsonNull.INSTANCE)))))
                } else messages.add(message("user", prompts.context(language, "summary", summary(record).json())))
            }
            messages.add(message("user", prompts.context(language, "observation", ObservationCompactor.compact(current, observationBytes, local))))
            repair?.let { messages.add(message("user", it)) }
            messages.add(message("user", prompts.context(language, "budget", budget)))
            return messages
        }
        // Drop historical pairs first, then historical summaries. Keep recent pairs whole.
        while (true) {
            val messages = build()
            val size = StepJournal.bytes(messages).toLong() + schemaBytes
            if (size <= maximumBytes) {
                val outputLimit = if (local) (4096 - Budget.estimate(size.toInt())).toInt().coerceAtLeast(1) else null
                return ModelInput(messages, schemaBytes, format, outputLimit)
            }
            when {
                retained > 0 -> { retained--; summaryCount = minOf(summaryCount + 1, 32) }
                summaryCount > 0 -> summaryCount--
                !compact -> compact = true
                scriptCount > 0 -> scriptCount--
                observationBytes > 128 -> observationBytes = maxOf(128, observationBytes / 2)
                memoryCount > 0 -> memoryCount--
                contextBytes > 0 -> contextBytes /= 2
                else -> throw ContextLimitExceeded()
            }
        }
    }

    companion object {
        /** Han goals use Chinese unless Kana/Hangul identifies a different language; otherwise locale breaks ties. */
        fun language(goal: String, locale: String = "en"): String {
            if (goal.any { it in '\u3040'..'\u30ff' || it in '\uac00'..'\ud7af' }) return "en"
            if (goal.any { it in '\u3400'..'\u9fff' }) return "zh"
            if (goal.any { it in 'a'..'z' || it in 'A'..'Z' }) return "en"
            return PromptCatalog.language(locale)
        }
    }
}
