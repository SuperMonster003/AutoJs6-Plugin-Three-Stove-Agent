package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.autojs.plugin.ai.agent.api.AiAgentContract
import java.security.MessageDigest

/** User-initiated export adds only a reviewed registration; it never runs or grants a script. */
internal class DynamicScriptRegistration private constructor(val fileName: String, val text: String) {
    override fun toString() = "DynamicScriptRegistration(bytes=${text.toByteArray(Charsets.UTF_8).size})"

    companion object {
        fun fromStep(step: JsonObject): DynamicScriptRegistration? = runCatching {
            require(step.string("tool") == "script_run_source")
            require(step.flag("sourceRedacted") != true)
            val arguments = requireNotNull(step.getAsJsonObject("arguments"))
            val source = DynamicScriptSource.validate(requireNotNull(arguments.string("source")))
            val timeout = if (arguments.has("timeoutMs")) requireNotNull(arguments.number("timeoutMs")) else 60_000L
            require(timeout in 1..AiAgentContract.MAX_TOOL_TIMEOUT_MS)
            val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
                .take(8).joinToString("") { "%02x".format(it.toInt() and 0xff) }
            val header = "/**\n * @agent\n * @description Generated JavaScript $digest\n" +
                " * @risk sensitive\n * @confirm before-run\n * @timeout $timeout\n */\n"
            DynamicScriptRegistration("agent-generated-$digest.js", header + source)
        }.getOrNull()
    }
}
