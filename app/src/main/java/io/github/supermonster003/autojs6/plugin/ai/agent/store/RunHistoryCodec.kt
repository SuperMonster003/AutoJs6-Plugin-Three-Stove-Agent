package io.github.supermonster003.autojs6.plugin.ai.agent.store

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import java.util.UUID

/** Private disk format, deliberately independent of the public task-query projection. */
internal object RunHistoryCodec {
    const val VERSION = 1
    const val MAX_BYTES = RunLimits.JOURNAL_BYTES + 32 * 1024
    val states = RunState.entries.map { it.wire }.toSet()
    val terminal = RunState.entries.filter { it.terminal }.map { it.wire }.toSet()
    data class Record(val run: JsonObject, val accessedAt: Long)
    fun id(value: String): String = value.also { require(UUID.fromString(it).toString() == it) }
    fun decode(text: String): Record {
        val root = AgentJson.objectOf(text, MAX_BYTES, 131_072)
        require(root.keySet() == setOf("version", "accessedAt", "run"))
        require(root.number("version") == VERSION.toLong())
        val accessed = requireNotNull(root.number("accessedAt")).also { require(it >= 0) }
        require(root["run"]?.isJsonObject == true)
        return Record(validate(root.getAsJsonObject("run")), accessed)
    }
    fun encode(run: JsonObject, accessedAt: Long): String {
        require(accessedAt >= 0)
        val text = jsonObject("version" to VERSION.json(), "accessedAt" to accessedAt.json(), "run" to validate(run)).toString()
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        // Apply the same structural limit in both directions; never persist an unreadable record.
        AgentJson.objectOf(text, MAX_BYTES, 131_072)
        return text
    }
    fun validate(run: JsonObject): JsonObject {
        try {
            id(requireNotNull(run.string("runId")))
            require(run.string("state") in states)
            require(requireNotNull(run.number("startedAt")) >= 0)
            require(requireNotNull(run.string("goal")).toByteArray(Charsets.UTF_8).size <= 4096)
            require(run.string("preset")?.let { it.isNotBlank() && it.length <= 128 } == true)
            for (key in listOf("detached", "truncated")) if (run.has(key)) require(run.flag(key) != null)
            for (key in listOf("step", "sequence")) if (run.has(key)) require((run.number(key) ?: -1) >= 0)
            for (key in listOf("budget", "remainingBudget", "pending")) if (run.has(key)) require(run[key].isJsonObject)
            run.getAsJsonObject("budget")?.let { budget ->
                for (key in listOf("maxSteps", "maxModelCalls", "maxDurationMs", "maxTotalTokens"))
                    if (budget.has(key)) require((budget.number(key) ?: -1) >= 0)
            }
            for (key in listOf("interaction", "progress")) if (run.has(key)) require(run.string(key) != null)
            require(run["steps"]?.isJsonArray == true)
            val steps = run.getAsJsonArray("steps")
            require(steps.size() <= RunLimits.STEPS)
            var previous = 0L
            steps.forEach { item ->
                val step = item.asJsonObject
                val index = requireNotNull(step.number("index"))
                require(index in (previous + 1)..RunLimits.STEPS.toLong()); previous = index
                require(step.string("kind") != null && (step.number("elapsedMs") ?: -1) >= 0)
                require(step["decision"].isJsonObject)
                val decision = step.getAsJsonObject("decision")
                if (decision.has("reasoning")) require(decision.string("reasoning") != null)
                for ((branch, field) in listOf("ask" to "question", "done" to "summary")) decision[branch]?.let { child ->
                    require(child.isJsonObject)
                    if (child.asJsonObject.has(field)) require(child.asJsonObject.string(field) != null)
                }
                for (key in listOf("tool", "confirmation", "observation", "error")) if (step.has(key)) require(step.string(key) != null)
                for (key in listOf("arguments", "usage")) if (step.has(key)) require(step[key].isJsonObject)
            }
            run["result"]?.let { value ->
                val result = value.asJsonObject
                require(result.string("status") in terminal)
                if (result.has("summary")) require(result.string("summary") != null)
                for (key in listOf("evidence", "unfinished")) result[key]?.let { array ->
                    require(array.isJsonArray && array.asJsonArray.all { it.isJsonPrimitive && it.asJsonPrimitive.isString })
                }
                for (key in listOf("script", "usage")) if (result.has(key)) require(result[key].isJsonObject)
                for (key in listOf("durationMs", "steps", "toolCalls")) if (result.has(key)) require((result.number(key) ?: -1) >= 0)
            }
            return run
        } catch (_: Exception) { throw IllegalArgumentException("Invalid history record") }
    }
}
