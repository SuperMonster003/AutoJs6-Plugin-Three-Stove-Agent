package io.github.supermonster003.autojs6.plugin.ai.agent.core

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.io.File

object CoreFixtures {
    val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "app/src/main/assets").isDirectory }
    fun asset(path: String) = File(root, "app/src/main/assets/$path").readText()
    fun snapshot(path: String) = File(root, "app/src/test/resources/$path").readText()
    fun catalog() = ToolCatalog(asset("catalog/tools.json"))
    fun policy() = ToolPolicy(ToolGroup.entries.associateWith { true }, ocrAvailable = true)
    fun fails(code: String, action: () -> Unit) {
        try { action(); throw AssertionError("Expected $code") } catch (failure: ToolFailure) { org.junit.Assert.assertEquals(code, failure.code) }
    }
}
