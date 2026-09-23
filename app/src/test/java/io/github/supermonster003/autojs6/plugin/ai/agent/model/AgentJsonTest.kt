package io.github.supermonster003.autojs6.plugin.ai.agent.model

import org.junit.Assert.*
import org.junit.Test

class AgentJsonTest {
    @Test fun strictDecoderRejectsAmbiguousOrNonJsonInput() {
        for (input in listOf("{\"x\":1,\"x\":2}", "{\"x\":1}{}", "{x:1}", "{\"x\":NaN}", "{\"x\":1,}", "/*x*/{}", "{\"x\":\"\\uD800\"}", "{\"x\":1e99999}"))
            assertThrows(input, Exception::class.java) { AgentJson.parse(input) }
    }
    @Test fun nestedDuplicateKeysAndStructuralLimitAreRejected() {
        assertThrows(Exception::class.java) { AgentJson.parse("{\"x\":{\"y\":1,\"y\":2}}") }
        assertThrows(Exception::class.java) { AgentJson.parse("[".repeat(34) + "0" + "]".repeat(34)) }
    }
    @Test fun unicodeLimitsUseUtf8BytesAndKeepCodePointsWhole() {
        val input = "{\"x\":\"中文😀\"}"
        assertEquals("中文😀", AgentJson.objectOf(input).string("x"))
        assertThrows(Exception::class.java) { AgentJson.parse(input, input.length) }
        assertEquals("中", AgentJson.truncate("中😀", 6))
        assertEquals("中😀", AgentJson.truncate("中😀", 7))
    }
}
