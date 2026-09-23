package io.github.supermonster003.autojs6.plugin.ai.agent.service

import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import org.junit.Assert.*
import org.junit.Test

class AgentRuntimePolicyAndroidTest {
    @Test fun cachedRuntimePolicyLoadsAllAssetsBeforeBinderRunAdmission() {
        val runtime = AgentRuntime.get(InstrumentationRegistry.getInstrumentation().targetContext)
        val policy = runtime.policy(setOf("observe", "act"))
        assertTrue(policy.isOrderGoal("Order a latte"))
        assertTrue(policy.isOrderGoal("请帮我下单星巴克拿铁"))
        assertFalse(policy.isOrderGoal("Transfer files"))
        assertTrue(policy.isPayment(RiskContext(nodeText = "Pay")))
        assertTrue(policy.isEnabled(runtime.catalog["ui_click"]!!))
        assertFalse(policy.isEnabled(runtime.catalog["ui_swipe"]!!))
    }
}
