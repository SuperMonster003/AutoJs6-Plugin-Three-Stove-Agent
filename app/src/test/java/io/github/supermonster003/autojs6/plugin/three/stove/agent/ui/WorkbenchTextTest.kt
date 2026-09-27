package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class WorkbenchTextTest {
    private fun row(vararg steps: com.google.gson.JsonObject) = jsonObject("steps" to jsonArray(*steps))
    private fun step(tool: String?, error: String? = null) = jsonObject("index" to 1.json(), "kind" to (if (tool == null) "ask" else "tool").json()).apply {
        tool?.let { addProperty("tool", it) }; error?.let { addProperty("error", it) }
    }
    @Test fun accessibilityFallbackFollowsTheLatestToolAttemptOnly() {
        assertFalse(WorkbenchText.accessibilityBlocked(null))
        assertFalse(WorkbenchText.accessibilityBlocked(row()))
        assertTrue(WorkbenchText.accessibilityBlocked(row(step("ui_dump", "A11Y_SERVICE_NOT_RUNNING"))))
        // The model asking the user to enable accessibility keeps the shortcut visible.
        assertTrue(WorkbenchText.accessibilityBlocked(row(step("ui_dump", "A11Y_SERVICE_NOT_RUNNING"), step(null))))
        assertFalse(WorkbenchText.accessibilityBlocked(row(step("ui_dump", "A11Y_SERVICE_NOT_RUNNING"), step("ui_dump"))))
        assertFalse(WorkbenchText.accessibilityBlocked(row(step("ui_click", "NODE_NOT_FOUND"))))
    }
}
