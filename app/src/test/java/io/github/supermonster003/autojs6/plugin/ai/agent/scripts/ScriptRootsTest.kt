package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import io.github.supermonster003.autojs6.plugin.ai.agent.service.*
import org.junit.Assert.*
import org.junit.Test

class ScriptRootsTest {
    @Test fun settingsValidateSyntaxBeforeForwardingToHost() {
        assertEquals(setOf("/sdcard/a", "/sdcard/b"), ScriptRoots.parseLines(" /sdcard/b/ \n\n/sdcard/a"))
        for (input in listOf("relative", "/", "/sdcard/../data", "/sdcard/.hidden/..", "/sdcard/a\n/sdcard/a/", "/sdcard/a\u0000", "/sdcard\\a")) {
            assertThrows(input, IllegalArgumentException::class.java) { ScriptRoots.parseLines(input) }
        }
        assertThrows(IllegalArgumentException::class.java) { ScriptRoots.validate((1..33).map { "/sdcard/$it" }) }
        assertThrows(IllegalArgumentException::class.java) { ScriptRoots.validate((1..20).map { "/sdcard/$it-" + "x".repeat(600) }) }
    }
    @Test fun taskRootsCanOnlyNarrowTheHostApprovedSet() {
        val config = LinkConfiguration.parse("""{"scriptRoots":["/sdcard/a","/sdcard/b"]}""")
        fun request(options: String) = StartRequest.parse("""{"goal":"test","options":$options}""", config)
        assertEquals(config.roots, request("{}").scriptRoots)
        assertEquals(setOf("/sdcard/a"), request("""{"scriptRoots":["/sdcard/a"]}""").scriptRoots)
        assertTrue(request("""{"scriptRoots":[]}""").scriptRoots.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { request("""{"scriptRoots":["/sdcard/c"]}""") }
    }
    @Test fun onlyHostConfigurationMayReplaceRootsAndItStillCannotWidenGrants() {
        val old = LinkConfiguration.parse("""{"grantSummary":{"toolGroups":["observe","script"]}}""")
        val next = LinkConfiguration.parse("""{"scriptRoots":["/sdcard/a"],"grantSummary":{"toolGroups":["observe","script"]}}""")
        assertFalse(next.narrows(old)); assertTrue(next.narrows(old, hostValidatedRoots = true))
        assertFalse(LinkConfiguration.parse("""{"scriptRoots":["/sdcard/a"]}""").narrows(old, hostValidatedRoots = true))
    }
}
