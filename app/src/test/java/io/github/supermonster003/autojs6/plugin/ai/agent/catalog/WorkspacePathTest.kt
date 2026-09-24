package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class WorkspacePathTest {
    private val handlers = ToolHandlers(F.catalog())
    private val tools = listOf("files_list", "files_stat", "files_read", "files_write")
    private fun arguments(tool: String, path: String) = jsonObject("path" to path.json()).apply {
        if (tool == "files_write") addProperty("content", "canary")
    }

    @Test fun everyFileToolRejectsTraversalAbsolutePathsAndControlCharactersBeforeDispatch() {
        val paths = listOf("..", "../private", "folder/../../private", "folder/../file", "folder/./file", "folder//file",
            "/private/file", "//server/share", "C:/private", "C:private", "folder\\..\\private", "folder/\u0000file",
            "folder/\nfile", "folder/\u0085file", "././file", "中".repeat(1366))
        for (tool in tools) for (path in paths) {
            val failure = assertThrows(ToolFailure::class.java) { handlers.prepare(tool, arguments(tool, path), F.policy()) }
            assertEquals("TOOL_ARGUMENTS_INVALID", failure.code)
            assertFalse(failure.hint.contains("private"))
        }
    }

    @Test fun acceptedNamesRemainLiteralAndTheHostOwnsFilesystemResolution() {
        for (tool in tools) for (path in listOf(".", "./", "a", "folder/file.txt", "./folder/file.txt", "folder/",
            "folder/a..b", "目录/文件.txt", "folder/%2e%2e", "folder/..%2fprivate", "a".repeat(4096))) {
            val plan = handlers.prepare(tool, arguments(tool, path), F.policy()) as ToolPlan.Call
            assertEquals(path, plan.request.args[0].asString)
        }
    }
}
