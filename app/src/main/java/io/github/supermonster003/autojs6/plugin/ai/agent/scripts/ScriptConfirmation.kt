package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.JsonObject

/** JSON scalar rendering preserves quotes, line breaks and types; no markup is interpreted. */
object ScriptConfirmation {
    fun rows(arguments: JsonObject): List<Pair<String, String>> = arguments.getAsJsonObject("parameters")
        .entrySet().sortedBy { it.key }.map { it.key to it.value.toString() }
}
