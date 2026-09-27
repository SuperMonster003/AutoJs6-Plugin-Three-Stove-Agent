package io.github.supermonster003.autojs6.plugin.three.stove.agent.scripts

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class DynamicScriptRegistrationTest {
    private fun step(source: String, timeoutMs: Long? = null) = jsonObject("tool" to "script_run_source".json(),
        "arguments" to jsonObject("source" to source.json()).apply { timeoutMs?.let { addProperty("timeoutMs", it) } })

    @Test fun exportedRegistrationRequiresConfirmationAndPreservesEverySourceCharacter() {
        val source = "\"ui\";\r\n/** @agent */\nconst value = \"😀 <b>plain</b>\";\nconsole.log(value);\n"
        val saved = checkNotNull(DynamicScriptRegistration.fromStep(step(source, 12345)))
        assertTrue(saved.fileName.matches(Regex("agent-generated-[a-f0-9]{16}\\.js")))
        assertTrue(saved.text.startsWith("/**\n * @agent\n * @description Generated JavaScript "))
        assertTrue(saved.text.contains("\n * @risk sensitive\n * @confirm before-run\n * @timeout 12345\n */\n"))
        assertEquals(source, saved.text.substringAfter(" */\n"))
        assertEquals(saved.fileName, DynamicScriptRegistration.fromStep(step(source, 12345))!!.fileName)
        assertNotEquals(saved.fileName, DynamicScriptRegistration.fromStep(step(source + " "))!!.fileName)
        assertFalse(saved.toString().contains("const value"))
    }

    @Test fun incompleteTruncatedInvalidAndUnrelatedJournalStepsCannotBeSavedAsRegisteredScripts() {
        assertNull(DynamicScriptRegistration.fromStep(JsonObject()))
        assertNull(DynamicScriptRegistration.fromStep(step("console.log(1)").apply { addProperty("tool", "script_run") }))
        assertNull(DynamicScriptRegistration.fromStep(step("console.log('***')").apply { addProperty("sourceRedacted", true) }))
        assertNull(DynamicScriptRegistration.fromStep(step(" ")))
        assertNull(DynamicScriptRegistration.fromStep(step("x".repeat(8193))))
        assertNull(DynamicScriptRegistration.fromStep(step("\n".repeat(4100) + "x")))
        assertNull(DynamicScriptRegistration.fromStep(step("bad\u0000source")))
        assertNull(DynamicScriptRegistration.fromStep(step("bad\uD800source")))
        assertNull(DynamicScriptRegistration.fromStep(step("console.log(1)", 0)))
        assertNull(DynamicScriptRegistration.fromStep(step("console.log(1)", 300001)))
        assertNull(DynamicScriptRegistration.fromStep(step("console.log(1)").apply {
            getAsJsonObject("arguments").addProperty("timeoutMs", "not-a-number")
        }))
        assertTrue(DynamicScriptRegistration.fromStep(step("console.log(1)"))!!.text.contains(" * @timeout 60000\n"))
    }
}
