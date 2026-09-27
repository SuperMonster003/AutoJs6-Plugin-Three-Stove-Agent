package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.autojs.plugin.host.capability.api.HostCapabilityContract
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
    BUDGET_EXCEEDED, CANCELLED, INVALID_REQUEST, TOOL_FAILED;
    val hostLost: Boolean get() = this == HOST_UNAVAILABLE || this == LINK_DETACHED
}

sealed interface PortResult<out T> {
    data class Success<T>(val value: T) : PortResult<T>
    data class Failure(val error: RunError, val reason: String? = null, val usage: ModelUsage? = null, val outputBytes: Int = 0,
                       val scriptParameters: io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptParameterProblem? = null,
                       val mcpReason: String? = null) : PortResult<Nothing> {
        init { require(reason == null || reason == "REQUEST_REJECTED"); require(outputBytes >= 0)
            require(scriptParameters == null || error == RunError.TOOL_ARGUMENTS_INVALID)
            require(mcpReason == null || (error == RunError.TOOL_FAILED && mcpReason in MCP_REASONS)) }
        companion object {
            val MCP_REASONS = setOf("MCP_AUTH_REQUIRED", "MCP_PAIRING_REQUIRED", "MCP_PAIRING_DENIED", "MCP_PROTOCOL_ERROR",
                "MCP_CATALOG_CHANGED", "MCP_TIMEOUT", "MCP_UNAVAILABLE", "MCP_LIMIT_EXCEEDED")
        }
    }
}

class RunOptions(
    val goal: String,
    val format: DecisionFormat,
    val detached: Boolean = false,
    val limits: BudgetLimits = BudgetLimits.defaults(detached),
    val confirmationMode: ConfirmationMode = ConfirmationMode.DEFAULT,
    val locale: String = "en",
    val modelTimeoutMs: Long = RunLimits.TOOL_TIMEOUT_MS,
    val maximumOutputTokens: Int = 2048,
) {
    init {
        require(goal.isNotBlank() && goal.length <= 4096 && goal.utf8Size() <= 4096)
        AgentJson.checkUnicode(goal)
        limits.validateOwnership(detached)
        require(modelTimeoutMs in 1..RunLimits.TOOL_TIMEOUT_MS && maximumOutputTokens in 1..65_536)
    }
    override fun toString() = "RunOptions(goalBytes=${goal.utf8Size()}, detached=$detached)"
}

class RunContext(val goal: String, val history: List<JsonObject>, val observation: String?, val repair: JsonObject?, val remainingBudget: JsonObject,
                 val format: DecisionFormat? = null, val locale: String = "en", val guidance: JsonObject = JsonObject(),
                 val images: List<ModelImage> = emptyList()) {
    override fun toString() = "RunContext(records=${history.size}, repair=${repair != null})"
}

/** P2.4's compiler supplies the bounded message array and accounts for the response schema bytes. */
class ModelInput(messages: JsonArray, val schemaBytes: Int = 0, val format: DecisionFormat? = null, val maximumOutputTokens: Int? = null,
                 tools: JsonArray = JsonArray(), val maximumContextBytes: Int = 128 * 1024,
                 images: List<ModelImage> = emptyList(), val imageMessageIndex: Int? = null, val vision: Boolean = false) {
    val images = images.toList()
    val imageTokens: Long get() = images.sumOf { it.estimatedTokens }
    val imageRefs: JsonArray get() = imageReferences(images, messageIndex = imageMessageIndex)
    private val data = AgentJson.parse(messages.toString(), 128 * 1024).asJsonArray
    private val definitions = AgentJson.parse(tools.toString(), 128 * 1024).asJsonArray
    init {
        require(schemaBytes in 0..DecisionSchema.MAX_SCHEMA_BYTES)
        require(maximumContextBytes in 1..128 * 1024)
        require(maximumOutputTokens == null || maximumOutputTokens in 1..65_536)
        require(format == null || schemaBytes == (format.responseSchemaJson?.toByteArray(Charsets.UTF_8)?.size ?: 0))
        require(definitions.size() <= 64 && (definitions.isEmpty || format?.nativeTools == true))
        VisionLimits().validate(this.images)
        require(this.images.isEmpty() || (vision && imageMessageIndex != null && imageMessageIndex in 0 until data.size() &&
            data[imageMessageIndex].asJsonObject.string("role") == "user"))
    }
    val messages: JsonArray get() = data.deepCopy()
    val tools: JsonArray get() = definitions.deepCopy()
    val inputBytes: Int get() = data.toString().utf8Size() + schemaBytes +
        (if (definitions.isEmpty) 0 else StepJournal.bytes(definitions)) + (if (images.isEmpty()) 0 else StepJournal.bytes(imageRefs))
    override fun toString() = "ModelInput(bytes=$inputBytes)"
}
fun interface RunContextCompiler {
    fun compile(context: RunContext): ModelInput
    fun observe(tool: String, result: JsonElement): String = ToolObservation.success(result)
    fun observeFailure(tool: String, result: JsonElement, error: RunError): String =
        jsonObject("ok" to false.json(), "error" to error.name.json(), "result" to
            ObservationCompactor.compact(result, ToolObservation.DEFAULT_MAX_BYTES - 256, false)).toString()
}
class ModelReply(val text: String, val usage: ModelUsage? = null, val nativeTurn: NativeToolTurn? = null,
                 val outputBytes: Int = text.utf8Size()) {
    init { require(outputBytes >= 0) }
    override fun toString() = "ModelReply(bytes=${text.utf8Size()})"
}
/** Captures already observed usage when the runner's own deadline or stop wins the callback race. */
interface ModelCallCancellation : Cancellation { fun progress(): PortResult.Failure }
class RunComponents(val compiler: RunContextCompiler, val model: RunModel, val tools: RunTools, val maximumTokens: Long? = null, val policy: ToolPolicy? = null,
                    val catalog: ToolCatalog? = null, val cleanup: Cancellation = Cancellation.NONE)
/** The Binder layer resolves public model metadata on a worker before the first decision. */
fun interface RunPreparation { fun prepare(callback: (PortResult<RunComponents>) -> Unit): Cancellation }
interface RunModel {
    fun initialFormat(proposed: DecisionFormat): DecisionFormat = proposed
    fun fallbackFormat(previous: DecisionFormat, failure: PortResult.Failure): DecisionFormat? = null
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
        is ToolPlan.DynamicScript -> plan.copy()
        is ToolPlan.Local -> plan.copy(arguments = plan.arguments.deepCopy())
        is ToolPlan.External -> plan.copy(arguments = plan.arguments.deepCopy())
    }
}
class PreparedTool(val invocation: ToolInvocation, val metadata: ToolMetadata, val opaqueContext: Any? = null) {
    override fun toString() = "PreparedTool(name=${invocation.name})"
}
class ToolReply(result: JsonElement, val script: io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptOutcome? = null,
                images: List<ModelImage> = emptyList(), val error: RunError? = null) {
    val images = images.toList().also { require(it.size <= 1) }
    private val data = AgentJson.parse(result.toString(), HostCapabilityContract.MAX_BRIDGE_INLINE_JSON_BYTES)
    val result: JsonElement get() = data.deepCopy()
    override fun toString() = "ToolReply(bytes=${StepJournal.bytes(data)})"
}
/** Both entry points return promptly; blocking bridge work belongs in the asynchronous adapter. */
interface RunTools {
    /** Prepare host accessibility when needed, then resolve node identity or script registration before risk admission.
     * P3/P4 adapters must bind inspection and execution to the same target, or reject stale targets.
     * Apart from host accessibility startup, no action, script start or memory write is permitted here. */
    fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation
    /** Executes only the prepared invocation. P3 supplies script/local adapters; P4 supplies UI flows. */
    fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation
}

enum class ReplyStatus { ACCEPTED, NOT_WAITING, INVALID }
class RunEvent(val runId: String, val sequence: Long, val type: String, data: JsonObject,
               internal val interactionDeadlineMs: Long? = null) {
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
    fun rule(name: String): String = rows.getAsJsonObject(key).string(name) ?: error("Missing rule text")
}
