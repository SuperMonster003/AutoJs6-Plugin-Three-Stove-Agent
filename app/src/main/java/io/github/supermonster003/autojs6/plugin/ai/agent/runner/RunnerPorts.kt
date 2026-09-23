package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.util.Locale

enum class RunState {
    QUEUED, RUNNING, WAITING_INPUT, WAITING_CONFIRMATION, CANCELLING, COMPLETED, PARTIAL, FAILED, BLOCKED, CANCELLED;
    val wire: String get() = name.lowercase(Locale.ROOT)
    val terminal: Boolean get() = this in setOf(COMPLETED, PARTIAL, FAILED, BLOCKED, CANCELLED)
}

enum class RunError {
    LINK_DETACHED, HOST_UNAVAILABLE, QUEUE_FULL, TOOL_DISABLED, TOOL_ARGUMENTS_INVALID, CAPABILITY_DENIED,
    QUOTA_EXCEEDED, RATE_LIMITED, LIMIT_EXCEEDED, TARGET_UNSUPPORTED, TARGET_UNAVAILABLE, MODEL_FAILED, MODEL_TIMEOUT,
    DECISION_UNPARSABLE, A11Y_SERVICE_NOT_RUNNING, NODE_REF_STALE, NODE_NOT_FOUND, SCREEN_LOCKED,
    SCRIPT_NOT_REGISTERED, SCRIPT_TIMEOUT, SCRIPT_FAILED, OCR_PLUGIN_REQUIRED, USER_DENIED, USER_TIMEOUT,
    BUDGET_EXCEEDED, CANCELLED, INVALID_REQUEST;
    val hostLost: Boolean get() = this == HOST_UNAVAILABLE || this == LINK_DETACHED
}

sealed interface PortResult<out T> {
    data class Success<T>(val value: T) : PortResult<T>
    data class Failure(val error: RunError) : PortResult<Nothing>
}

class RunOptions(
    val goal: String,
    val format: DecisionFormat,
    val detached: Boolean = false,
    val limits: BudgetLimits = BudgetLimits.defaults(detached),
    val confirmationMode: ConfirmationMode = ConfirmationMode.DEFAULT,
    val locale: String = "en",
    val modelTimeoutMs: Long = 300_000,
    val maximumOutputTokens: Int = 2048,
) {
    init {
        require(goal.isNotBlank() && goal.length <= 4096 && goal.toByteArray(Charsets.UTF_8).size <= 4096)
        AgentJson.checkUnicode(goal)
        limits.validateOwnership(detached)
        require(modelTimeoutMs in 1..RunLimits.TOOL_TIMEOUT_MS && maximumOutputTokens in 1..65_536)
    }
    override fun toString() = "RunOptions(goalBytes=${goal.toByteArray(Charsets.UTF_8).size}, detached=$detached)"
}

class RunContext(val goal: String, val history: List<JsonObject>, val observation: String?, val repair: JsonObject?, val remainingBudget: JsonObject,
                 val format: DecisionFormat? = null, val locale: String = "en") {
    override fun toString() = "RunContext(records=${history.size}, repair=${repair != null})"
}

/** P2.4's compiler supplies the bounded message array and accounts for the response schema bytes. */
class ModelInput(messages: JsonArray, val schemaBytes: Int = 0, val format: DecisionFormat? = null, val maximumOutputTokens: Int? = null) {
    private val data = AgentJson.parse(messages.toString(), 128 * 1024).asJsonArray
    init {
        require(schemaBytes in 0..DecisionSchema.MAX_SCHEMA_BYTES)
        require(maximumOutputTokens == null || maximumOutputTokens in 1..65_536)
        require(format == null || schemaBytes == (format.responseSchemaJson?.toByteArray(Charsets.UTF_8)?.size ?: 0))
    }
    val messages: JsonArray get() = data.deepCopy()
    val inputBytes: Int get() = data.toString().toByteArray(Charsets.UTF_8).size + schemaBytes
    override fun toString() = "ModelInput(bytes=$inputBytes)"
}
fun interface RunContextCompiler {
    fun compile(context: RunContext): ModelInput
    fun observe(tool: String, result: JsonElement): String = ToolObservation.success(result)
}
class ModelReply(val text: String, val usage: ModelUsage? = null) {
    override fun toString() = "ModelReply(bytes=${text.toByteArray(Charsets.UTF_8).size})"
}
interface RunModel {
    /** Must return promptly; callbacks may be synchronous, duplicated or late. */
    fun generate(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long, callback: (PortResult<ModelReply>) -> Unit): Cancellation
}

class ToolInvocation(val name: String, arguments: JsonObject, plan: ToolPlan) {
    private val data = arguments.deepCopy()
    private val preparedPlan = copyPlan(plan)
    val arguments: JsonObject get() = data.deepCopy()
    val plan: ToolPlan get() = copyPlan(preparedPlan)
    override fun toString() = "ToolInvocation(name=$name)"
    private fun copyCall(call: BridgeCall) = call.copy(args = call.args.deepCopy(), permissions = call.permissions.toList())
    private fun copyPlan(plan: ToolPlan): ToolPlan = when (plan) {
        is ToolPlan.Call -> plan.copy(request = copyCall(plan.request))
        is ToolPlan.Poll -> plan.copy(request = copyCall(plan.request))
        is ToolPlan.Repeat -> plan.copy(request = copyCall(plan.request))
        is ToolPlan.AppendText -> plan.copy(target = plan.target.deepCopy())
        is ToolPlan.RegisteredScript -> plan.copy(manifest = copyCall(plan.manifest), execution = copyCall(plan.execution))
        is ToolPlan.Local -> plan.copy(arguments = plan.arguments.deepCopy())
    }
}
class PreparedTool(val invocation: ToolInvocation, val metadata: ToolMetadata, val opaqueContext: Any? = null) {
    override fun toString() = "PreparedTool(name=${invocation.name})"
}
class ToolReply(result: JsonElement) {
    private val data = AgentJson.parse(result.toString(), 512 * 1024)
    val result: JsonElement get() = data.deepCopy()
    override fun toString() = "ToolReply(bytes=${StepJournal.bytes(data)})"
}
/** Both entry points return promptly; blocking bridge work belongs in the asynchronous adapter. */
interface RunTools {
    /** Read-only preparation: resolve node identity or script registration before risk admission.
     * P3/P4 adapters must bind inspection and execution to the same target, or reject stale targets.
     * No action, script start, memory write or other side effect is permitted here. */
    fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation
    /** Executes only the prepared invocation. P3 supplies script/local adapters; P4 supplies UI flows. */
    fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation
}

enum class ReplyStatus { ACCEPTED, NOT_WAITING, INVALID }
class RunEvent(val runId: String, val sequence: Long, val type: String, data: JsonObject) {
    private val snapshot = data.deepCopy()
    val payload: JsonObject get() = snapshot.deepCopy()
    override fun toString() = "RunEvent(runId=$runId, sequence=$sequence, type=$type)"
}

/** Fixed user-facing terminal text, injected from the ten-language packaged catalog. */
class RunnerText(json: String, locale: String) {
    private val rows = AgentJson.objectOf(json)
    private val key = when {
        rows.has(locale) -> locale
        locale.startsWith("zh", true) -> if (locale.contains("Hant", true) || locale.contains("TW", true) || locale.contains("HK", true)) "zh-Hant-TW" else "zh-Hans"
        rows.has(locale.substringBefore('-').lowercase(Locale.ROOT)) -> locale.substringBefore('-').lowercase(Locale.ROOT)
        else -> "en"
    }
    fun terminal(error: RunError, budgetDimension: String? = null): String {
        val strings = rows.getAsJsonObject(key)
        val message = strings.string(when {
            error == RunError.CANCELLED -> "cancelled"
            error.hostLost -> "blocked"
            error == RunError.BUDGET_EXCEEDED -> "budget"
            error == RunError.DECISION_UNPARSABLE -> "decision"
            else -> "failed"
        }) ?: error("Missing runner text")
        val dimension = if (error == RunError.BUDGET_EXCEEDED) strings.string("budget_$budgetDimension") else null
        return message + if (dimension == null) "" else " [$dimension]"
    }
}
