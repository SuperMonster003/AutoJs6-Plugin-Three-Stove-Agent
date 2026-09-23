package io.github.supermonster003.autojs6.plugin.ai.agent.service

import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class ControlRequestsTest {
    @Test fun invalidOptionsCannotWidenGrantsOrBudgets() {
        val config = LinkConfiguration.parse("""{"grantSummary":{"toolGroups":["observe"],"maxTotalTokens":1000}}""")
        for (options in listOf("""{"tools":["act"]}""", """{"budget":{"maxTotalTokens":1001}}""",
            """{"budget":{"maxSteps":41}}""", """{"scriptRoots":["/sdcard"]}""", """{"target":"invalid"}""",
            """{"confirm":"never"}""", """{"detached":"true"}""", """{"preset":"unknown"}""", """{"unknown":1}""")) {
            assertThrows(options, IllegalArgumentException::class.java) { StartRequest.parse("""{"goal":"test","options":$options}""", config) }
        }
        val request = StartRequest.parse("""{"goal":"test","options":{"tools":[],"budget":{"maxTotalTokens":500}}}""", config)
        assertEquals(500, request.options.limits.maxTotalTokens); assertTrue(request.groups.isEmpty())
    }
    @Test fun configUpdatesMustNarrowEveryAuthorityDimension() {
        val previous = LinkConfiguration.parse("""{"scriptRoots":["/sdcard/scripts"],"grantSummary":{"methods":["app.launch"],"permissions":["accessibility"],"toolGroups":["observe","act"],"maxTotalTokens":1000}}""")
        assertTrue(LinkConfiguration.parse("""{"grantSummary":{"methods":[],"permissions":[],"toolGroups":["observe"],"maxTotalTokens":100}}""").narrows(previous))
        assertFalse(LinkConfiguration.parse("{}").narrows(previous))
        assertThrows(IllegalArgumentException::class.java) { LinkConfiguration.parse("""{"grantSummary":{"toolGroups":["shell"]}}""") }
        assertThrows(IllegalArgumentException::class.java) { LinkConfiguration.parse("""{"grantSummary":{"modelCallsPerMinute":1}}""") }
    }
    @Test fun goalAndContextUseUtf8LimitsAndClosedSchemas() {
        val config = LinkConfiguration.parse("{}")
        assertThrows(IllegalArgumentException::class.java) { StartRequest.parse("""{"goal":"${"中".repeat(1366)}"}""", config) }
        assertThrows(IllegalArgumentException::class.java) { StartRequest.parse("""{"goal":"x","options":{"tools":["observe","observe"]}}""", config) }
        assertThrows(IllegalArgumentException::class.java) { StartRequest.parse("""{"goal":"x","options":{"budget":{"maxSteps":1.5}}}""", config) }
        assertEquals(ConfirmationMode.CAUTIOUS, StartRequest.parse("""{"goal":"x","options":{"confirm":"cautious"}}""", config).options.confirmationMode)
    }
}
