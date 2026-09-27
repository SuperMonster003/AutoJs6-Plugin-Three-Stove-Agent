package io.github.supermonster003.autojs6.plugin.ai.agent.model

import org.junit.Assert.*
import org.junit.Test

class AutomaticTargetTest {
    private fun pick(vararg values: Pair<String, ModelLocality>) = AutomaticTarget.pick(values.toList()) { it.second }?.first

    @Test fun prefersTheFirstOnDeviceTargetOtherwiseTheFirstListed() {
        assertEquals("local:b", pick("online:a" to ModelLocality.REMOTE, "local:b" to ModelLocality.ON_DEVICE, "local:c" to ModelLocality.ON_DEVICE))
        assertEquals("online:a", pick("online:a" to ModelLocality.REMOTE, "hybrid:b" to ModelLocality.HYBRID))
        assertNull(pick())
    }
}
