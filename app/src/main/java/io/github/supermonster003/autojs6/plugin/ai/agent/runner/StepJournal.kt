package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

data class StepRecord(
    val index: Int, val kind: String, val decision: JsonObject,
    val tool: String? = null, val arguments: JsonObject? = null, val confirmation: String? = null,
    val observation: String? = null, val usage: JsonObject? = null, val elapsedMs: Long = 0,
    val error: String? = null,
    val rejections: List<DecisionRejection> = emptyList(),
)

/** Private task history only. Ordinary logs must never print these records. */
class StepJournal(private val maxBytes: Int = RunLimits.JOURNAL_BYTES, private val maxSteps: Int = RunLimits.STEPS) {
    init { require(maxBytes in 2048..RunLimits.JOURNAL_BYTES && maxSteps in 1..RunLimits.STEPS) }
    private val records = ArrayDeque<JsonObject>()
    private val secrets = linkedSetOf<String>()
    private var terminal: JsonObject? = null
    var truncated = false; private set

    fun protectText(text: String) {
        if (text.isEmpty() || text in secrets) return
        require(secrets.size < RunLimits.STEPS && text.toByteArray(Charsets.UTF_8).size <= 64 * 1024)
        secrets += text
        // A value may have appeared in an earlier question or observation before field metadata was known.
        val old = records.map(::redactRecord)
        records.clear(); records.addAll(old)
        terminal = terminal?.let(::redactResult)
        trim()
    }

    fun append(record: StepRecord): JsonObject {
        check(terminal == null) { "Task journal is settled" }
        require(record.index in 1..RunLimits.STEPS && record.elapsedMs >= 0)
        require(record.rejections.size <= DecisionRepairSession.MAX_REPAIRS + 1)
        fun decision(limit: Int) = clipped(redactDecision(record.decision), limit).asJsonObject.apply {
            // Runtime diagnostics use the existing decision metadata object, including after clipping.
            if (record.rejections.isNotEmpty()) add("rejections", JsonArray().apply { record.rejections.forEach { add(it.name) } })
        }
        val entry = jsonObject("index" to record.index.json(), "kind" to record.kind.json(),
            "decision" to decision(2048), "elapsedMs" to record.elapsedMs.json())
        record.tool?.let { entry.addProperty("tool", it) }
        record.arguments?.let { entry.add("arguments", clipped(redact(it), 2048)) }
        record.confirmation?.let { entry.addProperty("confirmation", it) }
        record.observation?.let { entry.addProperty("observation", AgentJson.truncate(redactText(it), 4096)) }
        record.usage?.let { entry.add("usage", it.deepCopy()) }
        record.error?.let { entry.addProperty("error", it) }
        val limit = minOf(12 * 1024, maxBytes / 2)
        if (bytes(entry) > limit) {
            entry.add("decision", decision(256))
            entry["arguments"]?.let { entry.add("arguments", clipped(it, 256)) }
            entry.string("observation")?.let { entry.addProperty("observation", AgentJson.truncate(it, 32)) }
            entry.addProperty("truncated", true)
            truncated = true
        }
        require(bytes(entry) <= limit) { "Step metadata exceeds journal limit" }
        records.add(entry)
        trim()
        return entry.deepCopy()
    }

    fun finish(result: JsonObject): JsonObject {
        check(terminal == null) { "Task journal already has a terminal result" }
        val value = redactResult(result)
        val limit = minOf(24 * 1024, maxBytes / 2)
        // Preserve AgentResult's identity, status, counters and usage even when text is escaped or large.
        var textLimit = 1024
        while (bytes(value) > limit && textLimit >= 1) {
            for (key in listOf("summary", "evidence", "unfinished")) value[key]?.let { value.add(key, shorten(it, textLimit)) }
            value.getAsJsonObject("error")?.let { e -> e.string("message")?.let { e.addProperty("message", AgentJson.truncate(it, textLimit)) } }
            value.getAsJsonObject("script")?.let { script ->
                script["result"]?.takeIf { bytes(it) > 256 }?.let {
                    script.add("result", clipped(it, 256))
                    script.addProperty("resultTruncated", true)
                }
            }
            value.addProperty("truncated", true)
            truncated = true
            textLimit /= 2
        }
        require(bytes(value) <= limit) { "Result metadata exceeds journal limit" }
        terminal = value
        trim()
        return terminal!!.deepCopy()
    }
    fun history(): List<JsonObject> = records.map { it.deepCopy() }
    fun snapshot(): JsonObject = jsonObject("steps" to JsonArray().apply { records.forEach { add(it.deepCopy()) } },
        "truncated" to truncated.json()).apply { terminal?.let { add("result", it.deepCopy()) } }
    private fun redactRecord(value: JsonObject) = value.deepCopy().apply {
        getAsJsonObject("decision")?.let { add("decision", redactDecision(it)) }
        for (key in listOf("arguments", "observation", "preview")) get(key)?.let { add(key, redact(it)) }
    }
    private fun redactDecision(value: JsonObject) = redact(value).asJsonObject.apply {
        for (key in listOf("kind", "tool", "rejections")) value[key]?.let { add(key, it.deepCopy()) }
        for ((branch, keys) in listOf("ask" to listOf("kind"), "done" to listOf("status", "orderStatus"))) {
            value.getAsJsonObject(branch)?.let { original -> keys.forEach { key -> original[key]?.let { getAsJsonObject(branch).add(key, it.deepCopy()) } } }
        }
    }
    fun redactResult(value: JsonObject) = value.deepCopy().apply {
        for (key in listOf("summary", "evidence", "unfinished", "preview")) get(key)?.let { add(key, redact(it)) }
        getAsJsonObject("script")?.let { script -> script["result"]?.let { script.add("result", redact(it)) } }
        getAsJsonObject("error")?.get("message")?.let { getAsJsonObject("error").add("message", redact(it)) }
    }
    fun redact(value: JsonElement): JsonElement = when {
        value.isJsonObject -> JsonObject().apply { value.asJsonObject.entrySet().forEach { (key, child) -> add(key, redact(child)) } }
        value.isJsonArray -> JsonArray().apply { value.asJsonArray.forEach { add(redact(it)) } }
        value.isJsonPrimitive && value.asJsonPrimitive.isString -> redactText(value.asString).json()
        else -> value.deepCopy()
    }
    private fun redactText(value: String): String {
        var result = value
        secrets.sortedByDescending { it.length }.forEach { secret ->
            val escaped = secret.json().toString().drop(1).dropLast(1)
            result = result.replace(escaped, "***").replace(secret, "***")
        }
        return result
    }
    private fun shorten(value: JsonElement, limit: Int): JsonElement = when {
        value.isJsonArray -> JsonArray().apply { value.asJsonArray.forEach { add(shorten(it, limit)) } }
        value.isJsonPrimitive && value.asJsonPrimitive.isString -> AgentJson.truncate(value.asString, limit).json()
        else -> value.deepCopy()
    }
    private fun trim() {
        while (records.size > maxSteps || bytes(snapshot()) > maxBytes) {
            if (records.isEmpty()) error("Terminal record exceeds journal limit")
            records.removeFirst(); truncated = true
        }
    }
    companion object {
        fun bytes(value: JsonElement) = value.toString().toByteArray(Charsets.UTF_8).size
        fun clipped(value: JsonElement, maxBytes: Int): JsonElement {
            require(maxBytes >= 256)
            if (bytes(value) <= maxBytes) return value.deepCopy()
            return jsonObject("truncated" to true.json(), "preview" to AgentJson.truncate(value.toString(), (maxBytes - 64) / 6).json())
        }
        fun decision(value: AgentDecision): JsonObject = when (value) {
            is AgentDecision.Tool -> jsonObject("kind" to "tool".json(), "tool" to value.name.json(), "arguments" to value.arguments.deepCopy())
            is AgentDecision.Ask -> jsonObject("kind" to "ask".json(), "ask" to jsonObject("question" to value.question.json(), "kind" to value.kind.json(),
                "choices" to JsonArray().apply { value.choices.forEach(::add) }).apply { value.memoryKey?.let { addProperty("memoryKey", it) } })
            is AgentDecision.Done -> jsonObject("kind" to "done".json(), "done" to jsonObject("status" to value.status.json(), "summary" to value.summary.json(),
                "evidence" to JsonArray().apply { value.evidence.forEach(::add) }, "unfinished" to JsonArray().apply { value.unfinished.forEach(::add) })
                .apply { value.orderStatus?.let { addProperty("orderStatus", it) } })
        }.apply { value.reasoning?.let { addProperty("reasoning", it) } }
    }
}
