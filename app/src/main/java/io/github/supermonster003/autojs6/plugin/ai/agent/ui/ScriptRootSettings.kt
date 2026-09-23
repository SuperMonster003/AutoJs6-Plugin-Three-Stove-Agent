package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Context
import io.github.supermonster003.autojs6.plugin.ai.agent.model.AgentJson
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptRoots

/** Accessed only in the UI process. Accepted configuration reaches :agent through host attach/update. */
internal class ScriptRootSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("script-roots", Context.MODE_PRIVATE)
    val configured: Boolean get() = preferences.contains("configuration")
    fun read(): Set<String> = runCatching {
        ScriptRoots.validate(AgentJson.objectOf(preferences.getString("configuration", "{\"scriptRoots\":[]}")!!, 8192)
            .getAsJsonArray("scriptRoots").map { require(it.isJsonPrimitive && it.asJsonPrimitive.isString); it.asString })
    }.getOrDefault(emptySet())
    fun save(roots: Set<String>) { preferences.edit().putString("configuration", ScriptRoots.configuration(ScriptRoots.validate(roots))).apply() }
}
