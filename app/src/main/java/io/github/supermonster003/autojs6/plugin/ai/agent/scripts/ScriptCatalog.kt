package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.StepJournal
import org.autojs.plugin.ai.agent.api.AiAgentContract

/** A host-validated registration snapshot, never source code or a filesystem handle. */
class RegisteredScript private constructor(private val value: JsonObject) {
    val id: String get() = value.string("id")!!
    val path: String get() = value.string("path")!!
    val description: String get() = value.string("description")!!
    val risk: String get() = value.string("risk")!!
    val confirm: String get() = value.string("confirm")!!
    val parameters: JsonObject get() = value.getAsJsonObject("parameters").deepCopy()
    val examples: List<String> get() = value.getAsJsonArray("examples").map { it.asString }
    val tags: List<String> get() = value.getAsJsonArray("tags").map { it.asString }
    fun snapshot(): JsonObject = value.deepCopy()
    override fun toString() = "RegisteredScript(metadataBytes=${StepJournal.bytes(value)})"

    companion object {
        internal fun parse(value: JsonElement): RegisteredScript {
            require(value.isJsonObject)
            val row = value.asJsonObject
            for (key in listOf("id", "path", "description")) require(!row.string(key).isNullOrBlank())
            require(row.string("path")!!.startsWith('/') && '\u0000' !in row.string("path")!!)
            require(row.string("kind") in setOf("project", "file"))
            require(row.string("risk") in setOf("readonly", "normal", "sensitive"))
            require(row.string("confirm") in setOf("never", "before-run"))
            require((row.number("timeoutMs") ?: 0) in 1..AiAgentContract.MAX_TOOL_TIMEOUT_MS && (row.number("updatedAt") ?: -1) >= 0)
            val schema = row["parameters"]?.takeIf { it.isJsonObject }?.asJsonObject ?: error("Invalid parameters")
            require(schema.string("type") == "object" && schema["properties"]?.isJsonObject == true)
            require(schema["required"]?.isJsonArray == true && schema.getAsJsonArray("required").all { it.isJsonPrimitive && it.asJsonPrimitive.isString })
            require(schema.getAsJsonObject("properties").entrySet().all { it.value.isJsonObject })
            for (key in listOf("examples", "tags")) {
                require(row[key]?.isJsonArray == true && row.getAsJsonArray(key).all { it.isJsonPrimitive && it.asJsonPrimitive.isString && it.asString.isNotBlank() })
            }
            return RegisteredScript(row.deepCopy())
        }
    }
}

class ScriptCatalogSnapshot private constructor(private val rows: List<RegisteredScript>, val total: Int, val ambiguous: Int) {
    val entries: List<RegisteredScript> get() = rows.toList()
    override fun toString() = "ScriptCatalogSnapshot(entries=${entries.size}, ambiguous=$ambiguous)"
    companion object {
        const val MAX_ENTRIES = AiAgentContract.MAX_SCRIPT_CATALOG_ENTRIES
        const val MAX_BYTES = AiAgentContract.MAX_SCRIPT_CATALOG_BYTES
        fun parse(value: JsonElement): ScriptCatalogSnapshot {
            val copy = AgentJson.parse(value.toString(), MAX_BYTES)
            require(copy.isJsonArray && copy.asJsonArray.size() <= MAX_ENTRIES)
            val entries = copy.asJsonArray.map(RegisteredScript::parse)
            require(entries.map { it.path }.distinct().size == entries.size)
            // A model can select only by ID. Do not pick an arbitrary path for a duplicate ID.
            val duplicates = entries.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
            return ScriptCatalogSnapshot(entries.filter { it.id !in duplicates }, entries.size, duplicates.size)
        }
    }
}
