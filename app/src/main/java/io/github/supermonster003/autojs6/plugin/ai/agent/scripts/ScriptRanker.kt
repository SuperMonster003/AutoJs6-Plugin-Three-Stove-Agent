package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.StepJournal
import java.text.Normalizer
import java.util.Locale

/** Bounded lexical ranking, independent of locale, catalog traversal order and any model. */
object ScriptRanker {
    const val MAX_CANDIDATES = 24
    private val WORDS = Regex("[\\p{L}\\p{N}]+")
    fun select(catalog: ScriptCatalogSnapshot, query: String, filter: Boolean = false): ScriptPresentation {
        require(query.toByteArray(Charsets.UTF_8).size <= 4096)
        val words = tokens(query)
        fun overlap(text: String) = tokens(text).count { it in words }
        val scored = catalog.entries.map { entry ->
            entry to (6 * overlap(entry.id) + 3 * overlap(entry.description) +
                8 * overlap(entry.tags.joinToString(" ")) + 4 * overlap(entry.examples.joinToString(" ")))
        }.filter { !filter || words.isEmpty() || it.second > 0 }
            .sortedWith(compareByDescending<Pair<RegisteredScript, Int>> { it.second }.thenBy { it.first.id }.thenBy { it.first.path })
        return ScriptPresentation(scored.take(MAX_CANDIDATES).map { summarize(it.first) }, catalog.total, scored.size, catalog.ambiguous)
    }

    private fun tokens(text: String): Set<String> {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
        return buildSet {
            WORDS.findAll(normalized).forEach { match ->
                val word = match.value
                if (word.any { it in '\u3040'..'\u30ff' || it in '\u3400'..'\u9fff' || it in '\uac00'..'\ud7af' }) {
                    if (word.length == 1) add(word) else word.windowed(2).forEach(::add)
                } else add(word)
            }
        }
    }

    private fun summarize(entry: RegisteredScript): JsonObject {
        var truncated = false
        fun shorten(text: String, bytes: Int): String = AgentJson.truncate(text, bytes).also { if (it != text) truncated = true }
        val schema = entry.parameters
        val required = schema.getAsJsonArray("required").map { it.asString }.toSet()
        val parameters = JsonObject()
        val properties = schema.getAsJsonObject("properties").entrySet().sortedWith(
            compareByDescending<Map.Entry<String, JsonElement>> { it.key in required }.thenBy { it.key })
        for ((name, value) in properties.take(12)) {
            if (name.toByteArray(Charsets.UTF_8).size > 128) { truncated = true; continue }
            val source = value.asJsonObject
            val target = jsonObject("type" to (source["type"] ?: JsonNull.INSTANCE).deepCopy(), "required" to (name in required).json())
            source.string("description")?.let { target.addProperty("description", shorten(it, 96)) }
            for (key in listOf("default", "enum", "minimum", "maximum", "minLength", "maxLength")) source[key]?.let {
                if (StepJournal.bytes(it) <= 128) target.add(key, it.deepCopy())
                else { target.addProperty("${key}Omitted", true); truncated = true }
            }
            parameters.add(name, target)
        }
        if (parameters.size() != properties.size) truncated = true
        val result = jsonObject("id" to entry.id.json(), "description" to shorten(entry.description, 384).json(),
            "parameters" to parameters, "risk" to entry.risk.json(), "confirm" to entry.confirm.json(),
            "examples" to JsonArray().apply { entry.examples.take(2).forEach { add(shorten(it, 160)) } })
        if (entry.examples.size > 2) truncated = true
        while (StepJournal.bytes(result) > 2048 && parameters.size() > 0) {
            parameters.remove(parameters.keySet().last()); truncated = true
        }
        if (truncated) result.addProperty("truncated", true)
        return result
    }
}

/** Complete candidate records only; truncation never creates a different ID or parameter value. */
class ScriptPresentation internal constructor(items: List<JsonObject>, private val total: Int, private val matched: Int,
                                              private val ambiguous: Int, private val error: String? = null) {
    private val items = items.map { it.deepCopy() }
    val size: Int get() = items.size
    fun render(maximumBytes: Int = 12 * 1024, limit: Int = ScriptRanker.MAX_CANDIDATES): JsonObject {
        require(maximumBytes in 128..24 * 1024 && limit in 0..ScriptRanker.MAX_CANDIDATES)
        val kept = JsonArray()
        fun value() = jsonObject("scripts" to kept, "total" to total.json(), "matched" to matched.json(),
            "omitted" to (matched - kept.size()).json(), "ambiguousIds" to ambiguous.json(),
            "truncated" to (kept.size() < matched || kept.any { it.asJsonObject.flag("truncated") == true }).json())
            .apply { error?.let { addProperty("error", it) } }
        for (item in items.take(limit)) {
            kept.add(item.deepCopy())
            if (StepJournal.bytes(value()) > maximumBytes) { kept.remove(kept.size() - 1); break }
        }
        return value().also { require(StepJournal.bytes(it) <= maximumBytes) }
    }
    companion object {
        fun unavailable(error: String) = ScriptPresentation(emptyList(), 0, 0, 0, error)
    }
}
