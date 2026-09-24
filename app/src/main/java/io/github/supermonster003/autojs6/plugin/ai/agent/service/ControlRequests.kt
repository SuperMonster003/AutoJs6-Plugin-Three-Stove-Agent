package io.github.supermonster003.autojs6.plugin.ai.agent.service

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptRoots
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*

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
    fun availableGroups(): Set<String> = if (source.getAsJsonObject("grantSummary")?.has("toolGroups") == true) groups
        else ToolGroup.entries.map { it.id }.toSet()
    fun withSettings(settings: AgentSettings) = LinkConfiguration(locale, roots, methods, permissions,
        availableGroups().intersect(settings.toolGroups), maxInput, maxTokens, source)
    fun narrows(previous: LinkConfiguration, hostValidatedRoots: Boolean = false): Boolean = (hostValidatedRoots || previous.roots.containsAll(roots)) && previous.groups.containsAll(groups) &&
        previous.availableGroups().containsAll(availableGroups()) &&
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
            require(groups.all { id -> ToolGroup.entries.any { it.id == id } })
            LinkConfiguration(text(value, "locale", "en", 64)!!, roots,
                if (grant.has("methods")) strings(grant, "methods", max = 256) else null,
                if (grant.has("permissions")) strings(grant, "permissions", max = 128) else null, groups,
                number(grant, "maxInputBytesPerRequest", 128 * 1024, 128 * 1024).toInt(),
                number(grant, "maxTotalTokens", RunLimits.TOKENS, RunLimits.TOKENS), value.deepCopy())
        }
    }
}

internal class StartRequest(val options: RunOptions, val target: String?, val groups: Set<String>, val context: String, val interaction: String, val scriptRoots: Set<String>,
                            val preset: String, val memory: Boolean, val memoryScope: String = "global_and_preset") {
    companion object {
        fun parse(json: String, config: LinkConfiguration, presets: PresetSnapshot = PresetSnapshot.INITIAL,
                  settings: AgentSettings? = null): StartRequest = with(ControlRequests) {
            val value = AgentJson.objectOf(json, 32 * 1024)
            closed(value, setOf("goal", "options", "origin"))
            require(text(value, "origin", "script", 16) in setOf("script", "ui"))
            val opts = obj(value, "options")
            closed(opts, setOf("preset", "target", "tools", "budget", "confirm", "interaction", "detached", "context", "parameters", "memory", "scriptRoots", "locale"))
            val preset = presets.resolve(text(opts, "preset"))
            val detached = flag(opts, "detached", false)
            val memory = flag(opts, "memory", true)
            val allowedRoots = preset.scriptRoots?.intersect(config.roots) ?: config.roots
            val root = ScriptRoots.validate(strings(opts, "scriptRoots", allowedRoots))
            require(allowedRoots.containsAll(root))
            val allowedGroups = preset.groups(if (settings == null) config.groups else config.withSettings(settings).groups)
            val groups = when {
                !opts.has("tools") -> allowedGroups
                opts["tools"].isJsonArray -> strings(opts, "tools")
                else -> {
                    val tools = obj(opts, "tools"); closed(tools, setOf("enable", "disable"))
                    strings(tools, "enable", allowedGroups) - strings(tools, "disable").also { require(it.all { id -> ToolGroup.entries.any { it.id == id } }) }
                }
            }
            require(allowedGroups.containsAll(groups))
            val inheritedDefaults = BudgetLimits.defaults(detached)
            val defaults = inheritedDefaults.copy(
                maxSteps = settings?.budget?.get("maxSteps")?.toInt() ?: inheritedDefaults.maxSteps,
                maxModelCalls = settings?.budget?.get("maxModelCalls")?.toInt() ?: inheritedDefaults.maxModelCalls,
                maxDurationMs = minOf(settings?.budget?.get("maxDurationMs") ?: inheritedDefaults.maxDurationMs,
                    if (detached) RunLimits.DETACHED_DURATION_MS else RunLimits.DURATION_MS),
                maxTotalTokens = settings?.budget?.get("maxTotalTokens") ?: inheritedDefaults.maxTotalTokens)
            val budget = obj(opts, "budget")
            closed(budget, setOf("maxSteps", "maxModelCalls", "maxDurationMs", "maxTotalTokens"))
            fun limit(key: String, ceiling: Long): Long {
                val inherited = minOf(ceiling, preset.budget[key] ?: ceiling)
                return number(budget, key, inherited, inherited)
            }
            val limits = defaults.copy(
                maxSteps = limit("maxSteps", defaults.maxSteps.toLong()).toInt(),
                maxModelCalls = limit("maxModelCalls", defaults.maxModelCalls.toLong()).toInt(),
                maxDurationMs = limit("maxDurationMs", defaults.maxDurationMs),
                maxTotalTokens = limit("maxTotalTokens", minOf(defaults.maxTotalTokens, config.maxTokens)))
            val cautious = preset.confirmPolicy == "cautious" || settings?.cautious == true
            val confirm = text(opts, "confirm", if (cautious) "cautious" else "default", 16).also {
                require(it in setOf("default", "cautious") && (!cautious || it == "cautious"))
            }
            val interaction = text(opts, "interaction", "plugin", 16).also { require(it in setOf("plugin", "script")) }!!
            val target = text(opts, "target", preset.targetId)?.let(PresetCodec::target)
            val additional = if (opts.has("context")) requireNotNull(opts.string("context")) else ""
            val fixed = listOf(preset.context, additional).filter { it.isNotEmpty() }.joinToString("\n\n")
            require(fixed.toByteArray(Charsets.UTF_8).size <= 8192)
            val parameters = obj(opts, "parameters")
            require(parameters.toString().toByteArray(Charsets.UTF_8).size <= 16 * 1024)
            val context = if (parameters.size() == 0) fixed else jsonObject("context" to fixed.json(), "parameters" to parameters).toString()
            require(context.toByteArray(Charsets.UTF_8).size <= 8192)
            StartRequest(RunOptions(requireNotNull(text(value, "goal", maximum = 4096)), DecisionSchema.degraded(), detached, limits,
                if (confirm == "cautious") ConfirmationMode.CAUTIOUS else ConfirmationMode.DEFAULT, text(opts, "locale", config.locale, 64)!!), target, groups, context, interaction, root,
                preset.name, memory && "memory" in groups && preset.memoryScope != "none", preset.memoryScope)
        }
    }
}
