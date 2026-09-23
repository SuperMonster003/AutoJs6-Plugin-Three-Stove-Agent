package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.JsonArray
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

/** Syntax only. Canonical containment, existence and storage visibility belong to the host. */
object ScriptRoots {
    fun validate(paths: Collection<String>): Set<String> {
        require(paths.size <= 32)
        val roots = paths.map { path ->
            require(path.startsWith('/') && path.toByteArray(Charsets.UTF_8).size <= 1024)
            AgentJson.checkUnicode(path)
            require(path.none { it < ' ' || it == '\\' } && path.split('/').none { it == "." || it == ".." })
            path.trimEnd('/').also { require(it.isNotEmpty()) }
        }
        require(roots.distinct().size == roots.size)
        require(configuration(roots.toSet()).toByteArray(Charsets.UTF_8).size <= 8192)
        return roots.toSortedSet()
    }
    fun parseLines(value: String): Set<String> = validate(value.lineSequence().map(String::trim).filter(String::isNotBlank).toList())
    fun configuration(roots: Set<String>): String = jsonObject("scriptRoots" to JsonArray().apply { roots.sorted().forEach(::add) }).toString()
}
