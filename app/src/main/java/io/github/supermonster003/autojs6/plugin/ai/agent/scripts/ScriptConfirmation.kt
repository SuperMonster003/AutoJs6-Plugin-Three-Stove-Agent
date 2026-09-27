package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

/** Bounded script descriptions for confirmation events; no markup is interpreted. */
object ScriptConfirmation {
    fun description(text: String): String {
        var bytes = 4096
        var result = AgentJson.truncate(text, bytes)
        // JSON escaping also counts toward the 32 KiB Binder event limit.
        while (result.json().toString().toByteArray(Charsets.UTF_8).size > 4096) {
            bytes /= 2; result = AgentJson.truncate(text, bytes)
        }
        return result + if (result != text) "..." else ""
    }
}
