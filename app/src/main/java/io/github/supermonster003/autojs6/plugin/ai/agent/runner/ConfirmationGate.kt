package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

enum class ConfirmationMode { DEFAULT, CAUTIOUS }
enum class ConfirmationScope { ONCE, RUN }

/** Supplied by the trusted tool adapter's read-only inspection, never by decision arguments. */
data class ToolMetadata(
    val context: RiskContext = RiskContext(),
    val passwordField: Boolean = false,
    val payment: Boolean = false,
    val forceConfirmation: Boolean = false,
    val scriptTimeoutMs: Long? = null,
    val script: io.github.supermonster003.autojs6.plugin.ai.agent.scripts.PreparedScript? = null,
    val actionIdentity: String? = null,
) {
    init { require(scriptTimeoutMs == null || scriptTimeoutMs in 1..RunLimits.TOOL_TIMEOUT_MS) }
}

data class ConfirmationAssessment(val tool: String, val risk: RiskLevel, val required: Boolean, val allowRunScope: Boolean)

/** One gate per run; grants never cross a run, a tool name, or a risk level. */
class ConfirmationGate(private val policy: ToolPolicy, private val mode: ConfirmationMode) {
    private val allowed = mutableSetOf<Pair<String, RiskLevel>>()
    fun assess(spec: ToolSpec, metadata: ToolMetadata): ConfirmationAssessment {
        val payment = !spec.readOnlyHint && (metadata.payment || policy.isPayment(metadata.context))
        val risk = maxOf(policy.risk(spec, metadata.context), if (payment) RiskLevel.SENSITIVE else RiskLevel.READ_ONLY)
        val everyTime = payment || metadata.forceConfirmation || spec.name == "memory_propose"
        val needsConfirmation = everyTime || risk == RiskLevel.SENSITIVE || (mode == ConfirmationMode.CAUTIOUS && risk != RiskLevel.READ_ONLY)
        return ConfirmationAssessment(spec.name, risk, needsConfirmation && (everyTime || spec.name to risk !in allowed), !everyTime)
    }
    fun allow(assessment: ConfirmationAssessment, scope: ConfirmationScope): Boolean {
        if (scope == ConfirmationScope.RUN) {
            if (!assessment.allowRunScope) return false
            allowed += assessment.tool to assessment.risk
        }
        return true
    }
    fun description(spec: ToolSpec, metadata: ToolMetadata, language: String): String {
        metadata.script?.let {
            val bounded = io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptConfirmation.description(it.registration.description)
            return spec.description(language) + " [" + it.registration.id + "]\n" + bounded
        }
        val target = if (metadata.passwordField) "***" else AgentJson.truncate(metadata.context.nodeText.ifBlank { metadata.context.nodeDescription }, 400)
        return spec.description(language) + if (target.isEmpty()) "" else " [${target.replace('\n', ' ')}]"
    }
    fun arguments(arguments: JsonObject, metadata: ToolMetadata): JsonObject = (metadata.script?.arguments() ?: arguments.deepCopy()).apply {
        if (metadata.passwordField && has("text")) addProperty("text", "***")
    }
}
