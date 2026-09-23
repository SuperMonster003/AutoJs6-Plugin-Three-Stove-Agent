package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ToolHandlersTest(private val name: String, private val input: String, private val method: String) {
    @Test fun eachToolCompilesToItsApprovedBridgeOrLocalFlow() {
        val catalog = F.catalog()
        val plan = ToolHandlers(catalog).prepare(name, AgentJson.objectOf(input), F.policy())
        val request = when (plan) {
            is ToolPlan.Call -> plan.request
            is ToolPlan.Poll -> plan.request.also { assertEquals(10_000L, plan.deadlineMs); assertEquals("appear", plan.state) }
            is ToolPlan.Repeat -> plan.request.also { assertEquals(2, plan.times) }
            is ToolPlan.RegisteredScript -> plan.execution.also { assertEquals("readManifest", plan.manifest.method) }
            is ToolPlan.Local -> { assertEquals("local.$name", method); assertEquals(name, plan.name); return }
            is ToolPlan.AppendText -> error("Covered separately")
        }
        assertEquals(method, "${request.module}.${request.method}")
        assertTrue(method in catalog[name]!!.bridgeMapping)
        assertTrue(request.permissions.isNotEmpty())
        assertEquals("test", request.envelope("test").string("id"))
        when (name) {
            "ui_set_text" -> assertEquals("hello", request.args[1].asString)
            "ui_click" -> assertEquals("#n1", request.args[0].asJsonObject.string("nodeRef"))
            "ui_click_xy" -> assertEquals("[1,2,1,2,100]", request.args.toString())
            "ui_gesture" -> assertEquals("[500,[[1,2],[3,4]]]", request.args.toString())
            "files_write" -> assertFalse(request.args[2].asJsonObject.flag("overwrite")!!)
            "shell_exec" -> { assertFalse(request.args[1].asJsonObject.flag("root")!!); assertEquals(listOf("shell"), request.permissions) }
            "console_tail" -> assertEquals(40L, request.args[0].asJsonObject.number("lines"))
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases(): List<Array<String>> = listOf(
            arrayOf("ui_dump", "{}", "accessibility.dump"), arrayOf("ui_find", "{\"selector\":{\"text\":\"ok\"}}", "accessibility.findAll"),
            arrayOf("ui_wait_for", "{\"selector\":{\"text\":\"ok\"},\"state\":\"appear\"}", "accessibility.findOne"),
            arrayOf("app_current", "{}", "app.currentWindow"), arrayOf("screen_state", "{}", "device.isScreenOn"), arrayOf("device_info", "{}", "device.info"),
            arrayOf("console_tail", "{}", "console.tail"), arrayOf("ocr_screen", "{}", "accessibility.readScreenText"),
            arrayOf("ui_click", "{\"nodeRef\":\"#n1\"}", "accessibility.click"), arrayOf("ui_long_click", "{\"selector\":{\"text\":\"ok\"}}", "accessibility.longClick"),
            arrayOf("ui_set_text", "{\"nodeRef\":\"#n1\",\"text\":\"hello\"}", "accessibility.setText"),
            arrayOf("ui_scroll", "{\"nodeRef\":\"#n1\",\"direction\":\"forward\",\"times\":2}", "accessibility.scrollForward"),
            arrayOf("ui_press_key", "{\"key\":\"notifications\"}", "keys.notifications"), arrayOf("app_launch", "{\"packageName\":\"test.app\"}", "app.launchPackage"),
            arrayOf("clipboard_get", "{}", "clipboard.getText"), arrayOf("clipboard_set", "{\"text\":\"hello\"}", "clipboard.setText"),
            arrayOf("ui_click_xy", "{\"x\":1,\"y\":2}", "accessibility.swipe"),
            arrayOf("ui_swipe", "{\"x1\":1,\"y1\":2,\"x2\":3,\"y2\":4}", "accessibility.swipe"),
            arrayOf("ui_gesture", "{\"durationMs\":500,\"points\":[[1,2],[3,4]]}", "accessibility.gesture"),
            arrayOf("script_catalog", "{}", "agent.listScripts"), arrayOf("script_run", "{\"id\":\"example\",\"parameters\":{\"count\":1}}", "agent.execRegistered"),
            arrayOf("script_stop", "{\"executionId\":42}", "engines.stop"), arrayOf("files_list", "{\"path\":\".\"}", "files.list"),
            arrayOf("files_stat", "{\"path\":\"a.txt\"}", "files.stat"), arrayOf("files_read", "{\"path\":\"a.txt\"}", "files.read"),
            arrayOf("files_write", "{\"path\":\"a.txt\",\"content\":\"hello\"}", "files.write"), arrayOf("shell_exec", "{\"cmd\":\"pwd\"}", "shell.exec"),
            arrayOf("memory_get", "{}", "local.memory_get"), arrayOf("memory_propose", "{\"key\":\"drink\",\"value\":\"latte\"}", "local.memory_propose"),
            arrayOf("report_progress", "{\"message\":\"working\"}", "local.report_progress"),
        )
    }
}
