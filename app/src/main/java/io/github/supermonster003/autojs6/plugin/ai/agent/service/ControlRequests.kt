package io.github.supermonster003.autojs6.plugin.ai.agent.service

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptRoots

/** Pure validation of the public control JSON. No model-supplied field grants authority. */
internal object ControlRequests {
    fun closed(value: JsonObject, fields: Set<String>) { require(value.keySet().all { it in fields }) }
    fun text(value: JsonObject, key: String, default: String? = null, maximum: Int = 256): String? {
        if (!value.has(key)) return default
        return requireNotNull(value.string(key)).also { require(it.isNotBlank() && it.toByteArray(Charsets.UTF_8).size <= maximum) }
    }
    fun flag(value: JsonObject, key: String, default: Boolean) = if (value.has(key)) requireNotNull(value.flag(key)) else default
    fun number(value: JsonObject, key: String, default: Long, ceiling: Long): Long = try {
        (if (value.has(key)) requireNotNull(value.number(key)) else default).also { require(it in 1..ceiling) }
    } catch (_: ArithmeticException) { throw IllegalArgumentException("Invalid integer") }
    fun strings(value: JsonObject, key: String, default: Set<String> = emptySet(), max: Int = 32): Set<String> {
        if (!value.has(key)) return default
        val items = requireNotNull(value[key]?.takeIf { it.isJsonArray }?.asJsonArray)
        require(items.size() <= max)
        return items.map { require(it.isJsonPrimitive && it.asJsonPrimitive.isString); it.asString.also { s -> require(s.isNotBlank() && s.toByteArray(Charsets.UTF_8).size <= 1024) } }.toSet()
            .also { require(it.size == items.size()) }
    }
    fun obj(value: JsonObject, key: String) = if (!value.has(key)) JsonObject() else requireNotNull(value[key]?.takeIf { it.isJsonObject }?.asJsonObject)
    fun runId(value: JsonObject): String = requireNotNull(text(value, "runId", maximum = 36)).also { require(it.matches(Regex("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}"))) }
}

internal class LinkConfiguration private constructor(val locale: String, val roots: Set<String>, val methods: Set<String>?,
                                                    val permissions: Set<String>?, val groups: Set<String>, val maxInput: Int, val maxTokens: Long,
                                                    val source: JsonObject) {
    fun narrows(previous: LinkConfiguration, hostValidatedRoots: Boolean = false): Boolean = (hostValidatedRoots || previous.roots.containsAll(roots)) && previous.groups.containsAll(groups) &&
        (previous.methods == null || methods != null && previous.methods.containsAll(methods)) &&
        (previous.permissions == null || permissions != null && previous.permissions.containsAll(permissions)) && maxInput <= previous.maxInput && maxTokens <= previous.maxTokens
    companion object {
        fun parse(json: String): LinkConfiguration = with(ControlRequests) {
            val value = AgentJson.objectOf(json, 8192)
            closed(value, setOf("hostLabel", "locale", "scriptRoots", "grantSummary"))
            text(value, "hostLabel")
            val roots = ScriptRoots.validate(strings(value, "scriptRoots"))
            val grant = obj(value, "grantSummary")
            closed(grant, setOf("methods", "permissions", "toolGroups", "maxInputBytesPerRequest", "maxTotalTokens"))
            val groups = strings(grant, "toolGroups", ToolGroup.entries.filter { it.defaultEnabled }.map { it.id }.toSet())
            // Only the authenticated host can widen its initial grant. Run options still only narrow it.
            require(groups.all { id -> ToolGroup.entries.any { it.id == id && (it.defaultEnabled || it == ToolGroup.GESTURE) } })
            LinkConfiguration(text(value, "locale", "en", 64)!!, roots,
                if (grant.has("methods")) strings(grant, "methods", max = 256) else null,
                if (grant.has("permissions")) strings(grant, "permissions", max = 128) else null, groups,
                number(grant, "maxInputBytesPerRequest", 128 * 1024, 128 * 1024).toInt(),
                number(grant, "maxTotalTokens", RunLimits.TOKENS, RunLimits.TOKENS), value.deepCopy())
        }
    }
}

internal class StartRequest(val options: RunOptions, val target: String?, val groups: Set<String>, val context: String, val interaction: String, val scriptRoots: Set<String>,
                            val preset: String, val memory: Boolean) {
    companion object {
        fun parse(json: String, config: LinkConfiguration): StartRequest = with(ControlRequests) {
            val value = AgentJson.objectOf(json, 32 * 1024)
            closed(value, setOf("goal", "options", "origin"))
            require(text(value, "origin", "script", 16) in setOf("script", "ui"))
            val opts = obj(value, "options")
            closed(opts, setOf("preset", "target", "tools", "budget", "confirm", "interaction", "detached", "context", "parameters", "memory", "scriptRoots", "locale"))
            val preset = text(opts, "preset", "default")!!
            require(preset == "default") // Named presets arrive in P6.
            val detached = flag(opts, "detached", false)
            val memory = flag(opts, "memory", true)
            val root = ScriptRoots.validate(strings(opts, "scriptRoots", config.roots))
            require(config.roots.containsAll(root))
            val groups = when {
                !opts.has("tools") -> config.groups
                opts["tools"].isJsonArray -> strings(opts, "tools")
                else -> {
                    val tools = obj(opts, "tools"); closed(tools, setOf("enable", "disable"))
                    strings(tools, "enable", config.groups) - strings(tools, "disable").also { require(it.all { id -> ToolGroup.entries.any { it.id == id } }) }
                }
            }
            require(config.groups.containsAll(groups))
            val defaults = BudgetLimits.defaults(detached)
            val budget = obj(opts, "budget")
            closed(budget, setOf("maxSteps", "maxModelCalls", "maxDurationMs", "maxTotalTokens"))
            val limits = defaults.copy(
                maxSteps = number(budget, "maxSteps", defaults.maxSteps.toLong(), defaults.maxSteps.toLong()).toInt(),
                maxModelCalls = number(budget, "maxModelCalls", defaults.maxModelCalls.toLong(), defaults.maxModelCalls.toLong()).toInt(),
                maxDurationMs = number(budget, "maxDurationMs", defaults.maxDurationMs, defaults.maxDurationMs),
                maxTotalTokens = number(budget, "maxTotalTokens", minOf(defaults.maxTotalTokens, config.maxTokens), minOf(defaults.maxTotalTokens, config.maxTokens)))
            val confirm = text(opts, "confirm", "default", 16).also { require(it in setOf("default", "cautious")) }
            val interaction = text(opts, "interaction", "plugin", 16).also { require(it in setOf("plugin", "script")) }!!
            val target = text(opts, "target")?.also { require(it.length <= 256 && it.matches(Regex("[a-z0-9][a-z0-9._-]{0,127}:[a-z0-9][a-z0-9._-]{0,127}"))) }
            val fixed = if (opts.has("context")) requireNotNull(opts.string("context")) else ""
            require(fixed.toByteArray(Charsets.UTF_8).size <= 8192)
            val parameters = obj(opts, "parameters")
            require(parameters.toString().toByteArray(Charsets.UTF_8).size <= 16 * 1024)
            val context = if (parameters.size() == 0) fixed else jsonObject("context" to fixed.json(), "parameters" to parameters).toString()
            require(context.toByteArray(Charsets.UTF_8).size <= 8192)
            StartRequest(RunOptions(requireNotNull(text(value, "goal", maximum = 4096)), DecisionSchema.degraded(), detached, limits,
                if (confirm == "cautious") ConfirmationMode.CAUTIOUS else ConfirmationMode.DEFAULT, text(opts, "locale", config.locale, 64)!!), target, groups, context, interaction, root, preset, memory && "memory" in groups)
        }
    }
}
