package io.github.supermonster003.autojs6.plugin.ai.agent.service

import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import org.junit.Assert.*
import org.junit.Test

class RunLauncherTest {
    private val config = LinkConfiguration.parse("{}")
    @Test fun uiAndHostShareAdmissionLimitsAndPresetValidation() {
        val request = RunLauncher.uiRequest("Read the screen", "default", "zh-Hans")
        val admitted = RunLauncher.start(C.LINK_STATE_ATTACHED, config, request) { it }
        assertEquals("plugin", admitted.interaction)
        assertEquals("zh-Hans", admitted.options.locale)
        assertEquals(40, admitted.options.limits.maxSteps)
        assertThrows(IllegalArgumentException::class.java) {
            RunLauncher.start(C.LINK_STATE_ATTACHED, config, RunLauncher.uiRequest("Read", "missing", "en")) { fail("Invalid preset admitted") }
        }
        assertThrows(IllegalArgumentException::class.java) { RunLauncher.uiRequest("界".repeat(1366), "default", "en") }
        assertThrows(IllegalArgumentException::class.java) { RunLauncher.uiRequest("   ", "default", "en") }
    }
    @Test fun detachedOrLostHostNeverEnqueues() {
        for ((state, error) in listOf(C.LINK_STATE_DETACHED to C.ERROR_LINK_DETACHED, C.LINK_STATE_HOST_UNAVAILABLE to C.ERROR_HOST_UNAVAILABLE)) {
            assertEquals(error, assertThrows(WireFailure::class.java) {
                RunLauncher.start(state, config, RunLauncher.uiRequest("Read", "default", "en")) { fail("Detached request admitted") }
            }.code)
        }
    }
}
