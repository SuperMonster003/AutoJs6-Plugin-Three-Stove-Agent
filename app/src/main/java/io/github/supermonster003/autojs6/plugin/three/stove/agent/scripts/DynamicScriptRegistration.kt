package io.github.supermonster003.autojs6.plugin.three.stove.agent.scripts

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import org.autojs.plugin.ai.agent.api.AiAgentContract
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolNames

/** User-initiated export adds only a reviewed registration; it never runs or grants a script. */
internal class DynamicScriptRegistration private constructor(val fileName: String, val text: String) {
    override fun toString() = "DynamicScriptRegistration(bytes=${text.utf8Size()})"

    companion object {
        fun fromStep(step: JsonObject): DynamicScriptRegistration? = runCatching {
            require(step.string("tool") == ToolNames.SCRIPT_RUN_SOURCE)
            require(step.flag("sourceRedacted") != true)
            val arguments = requireNotNull(step.getAsJsonObject("arguments"))
            val source = DynamicScriptSource.validate(requireNotNull(arguments.string("source")))
            val timeout = if (arguments.has("timeoutMs")) requireNotNull(arguments.number("timeoutMs")) else 60_000L
            require(timeout in 1..AiAgentContract.MAX_TOOL_TIMEOUT_MS)
            val digest = Digests.sha256Hex(source).take(16)
            val header = "/**\n * @agent\n * @description Generated JavaScript $digest\n" +
                " * @risk sensitive\n * @confirm before-run\n * @timeout $timeout\n */\n"
            DynamicScriptRegistration("agent-generated-$digest.js", header + source)
        }.getOrNull()
    }
}
