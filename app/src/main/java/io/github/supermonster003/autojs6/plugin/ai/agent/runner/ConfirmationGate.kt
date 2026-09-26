package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

enum class ConfirmationMode { DEFAULT, CAUTIOUS, FULL_ACCESS }
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
    val memoryScope: String? = null,
) {
    init { require(scriptTimeoutMs == null || scriptTimeoutMs in 1..RunLimits.TOOL_TIMEOUT_MS) }
}

data class ConfirmationAssessment(val tool: String, val risk: RiskLevel, val required: Boolean, val allowRunScope: Boolean,
                                  val payment: Boolean = false)

/** One gate per run; grants never cross a run, a tool name, a risk level or the payment category.
 * FULL_ACCESS comes only from the user's private settings and skips every confirmation of the run. */
class ConfirmationGate(private val policy: ToolPolicy, private val mode: ConfirmationMode) {
    private data class Grant(val tool: String, val risk: RiskLevel, val payment: Boolean)
    private val allowed = mutableSetOf<Grant>()
    fun assess(spec: ToolSpec, metadata: ToolMetadata): ConfirmationAssessment {
        val payment = !spec.readOnlyHint && (metadata.payment || policy.isPayment(metadata.context))
        val risk = maxOf(policy.risk(spec, metadata.context), if (payment) RiskLevel.SENSITIVE else RiskLevel.READ_ONLY)
        val explicitConfirmation = payment || metadata.forceConfirmation || spec.name in setOf("memory_propose", "script_run_source")
        val needsConfirmation = mode != ConfirmationMode.FULL_ACCESS &&
            (explicitConfirmation || risk == RiskLevel.SENSITIVE || (mode == ConfirmationMode.CAUTIOUS && risk != RiskLevel.READ_ONLY))
        return ConfirmationAssessment(spec.name, risk, needsConfirmation && Grant(spec.name, risk, payment) !in allowed, true, payment)
    }
    fun allow(assessment: ConfirmationAssessment, scope: ConfirmationScope): Boolean {
        if (scope == ConfirmationScope.RUN) {
            if (!assessment.allowRunScope) return false
            allowed += Grant(assessment.tool, assessment.risk, assessment.payment)
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
        metadata.memoryScope?.let { addProperty("scope", it) }
        if (metadata.passwordField && has("text")) addProperty("text", "***")
    }
}
