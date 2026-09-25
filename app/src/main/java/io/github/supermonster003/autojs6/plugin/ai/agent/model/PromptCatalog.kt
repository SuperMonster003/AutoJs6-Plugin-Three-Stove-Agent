package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import java.util.Locale

/** Assets supply rules; user/screen/script/memory content is inserted exactly once as JSON data. */
class PromptCatalog(private val readAsset: (String) -> String, private val catalog: ToolCatalog) {
    private val templates = listOf("en", "zh").associateWith { language ->
        listOf("system", "compact_system", "goal", "observation", "repair", "context", "scripts").associateWith { name ->
            readAsset("prompts/$language/$name.md").replace("\r\n", "\n").replace('\r', '\n')
                .also { require(it.toByteArray(Charsets.UTF_8).size <= 16 * 1024) }
        }
    }

    fun system(language: String, policy: ToolPolicy, format: DecisionFormat, fixedContext: String = "",
               memories: JsonArray = JsonArray(), memoryTruncated: Boolean = false,
               compact: Boolean = false, contextTruncated: Boolean = false, registeredScripts: JsonObject? = null, memoryUnavailable: Boolean = false,
               guidance: JsonObject = JsonObject(), memoryScopes: List<String>? = null): String {
        bounded(fixedContext, 8 * 1024)
        // P3.2 supplies global + current preset entries, already sorted/trimmed to 4 KiB.
        val memory = memories.toString().also { bounded(it, 4 * 1024); AgentJson.parse(it) }
        val system = render(language, if (compact) "compact_system" else "system", mapOf(
            "tools_json" to if (compact) CompactToolDescriptions.render(catalog, policy) else catalog.render(policy, language(language)),
            "format_json" to if (compact) compactContract(format) else DecisionSchema.promptContract(format),
            "verification_json" to AgentJson.objectOf(guidance.toString(), 2048).toString(),
            "context_json" to jsonObject("fixedContext" to fixedContext.json(), "memories" to AgentJson.parse(memory),
                "memoryTruncated" to memoryTruncated.json()).apply {
                    if (contextTruncated) addProperty("contextTruncated", true)
                    if (memoryUnavailable) addProperty("memoryUnavailable", true)
                    memoryScopes?.let { scopes ->
                        require(scopes.size <= 2); scopes.forEach { io.github.supermonster003.autojs6.plugin.ai.agent.store.MemoryCodec.scope(it) }
                        add("memoryScopes", JsonArray().apply { scopes.forEach(::add) })
                    }
                }.toString(),
        ))
        return if (registeredScripts == null) system else {
            val data = registeredScripts.toString().also { bounded(it, 12 * 1024) }
            system + "\n" + render(language, "scripts", mapOf("scripts_json" to data))
        }
    }

    fun goal(language: String, goal: String): String {
        require(goal.isNotBlank())
        bounded(goal, 4 * 1024)
        return render(language, "goal", mapOf("goal_json" to jsonObject("goal" to goal.json()).toString()))
    }

    fun observation(language: String, step: Int, tool: String, elapsedMs: Long, result: String, budget: JsonObject): String {
        require(step in 1..200 && catalog[tool] != null && elapsedMs in 0..3_600_000)
        val observation = AgentJson.objectOf(result, 24 * 1024)
        val budgetCopy = AgentJson.objectOf(budget.toString(), 4 * 1024)
        val data = jsonObject("step" to step.json(), "tool" to tool.json(), "elapsedMs" to elapsedMs.json(),
            "observation" to observation, "remaining_budget" to budgetCopy)
        return render(language, "observation", mapOf("observation_json" to data.toString()))
    }

    fun repair(language: String, repair: DecisionAttempt.Repair): String = repair(language, repair.observation)
    fun repair(language: String, repair: JsonObject): String = render(language, "repair",
        mapOf("repair_json" to AgentJson.objectOf(repair.toString(), 1024).toString()))

    fun context(language: String, section: String, value: JsonElement): String = render(language, "context",
        mapOf("context_json" to jsonObject("section" to section.json(), "data" to value).toString()))

    private fun compactContract(format: DecisionFormat) = """
        {kind:tool|ask|done,tool?:enabled name,arguments?:${if (format.argumentsEncoding == ArgumentsEncoding.JSON_STRING) "JSON-encoded object string" else "object"},ask?:{question:string<=500,kind?:text|choice|confirm,choices?:string[1..8]<=200 each,memoryKey?:string<=64},done?:{status:completed|partial|failed|blocked,summary:string<=1000,evidence?:string[0..8]<=200 each,unfinished?:string[0..8]<=200 each,orderStatus?:none|cart|pending_payment|submitted|paid}}
        Only the selected branch. ${if (format.nullableOptionals) "Unused optional fields must be null." else "Omit unused fields."} Choice needs distinct choices; text/confirm have none. Degraded=${format.degraded}: output one JSON object even without a schema.
    """.trimIndent()

    private fun render(language: String, name: String, values: Map<String, String>): String {
        val template = templates.getValue(language(language)).getValue(name)
        val placeholders = PLACEHOLDER.findAll(template).map { it.groupValues[1] }.toSet()
        require(placeholders == values.keys) { "Prompt placeholders do not match supplied fields" }
        // Replacement text is not scanned a second time; '$', backslashes and {{...}} remain literal.
        return PLACEHOLDER.replace(template) { values.getValue(it.groupValues[1]) }.also { bounded(it, 128 * 1024) }
    }

    private fun bounded(text: String, maxBytes: Int) {
        require(text.length <= maxBytes && text.toByteArray(Charsets.UTF_8).size <= maxBytes) { "Prompt field exceeds byte limit" }
        AgentJson.checkUnicode(text)
    }

    companion object {
        private val PLACEHOLDER = Regex("\\{\\{([a-z_]+)\\}\\}")
        fun language(tag: String) = if (tag.lowercase(Locale.ROOT).startsWith("zh")) "zh" else "en"
    }
}
