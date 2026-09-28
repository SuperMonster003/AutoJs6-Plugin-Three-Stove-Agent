package io.github.supermonster003.autojs6.plugin.three.stove.agent.service

import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import org.junit.Assert.*
import org.junit.Test

class AgentRuntimePolicyAndroidTest {
    @Test fun cachedRuntimePolicyLoadsAllAssetsBeforeBinderRunAdmission() {
        val runtime = AgentRuntime.get(InstrumentationRegistry.getInstrumentation().targetContext)
        val policy = runtime.policy(setOf("observe", "act"), AgentSettings())
        assertTrue(policy.isOrderGoal("Order a latte"))
        assertTrue(policy.isOrderGoal("请帮我下单星巴克拿铁"))
        assertFalse(policy.isOrderGoal("Transfer files"))
        assertTrue(policy.isPayment(RiskContext(nodeText = "Pay")))
        assertTrue(policy.isPayment(RiskContext(packageName = "com.eg.android.AlipayGphone")))
        assertTrue(policy.isEnabled(runtime.catalog["ui_click"]!!))
        assertFalse(policy.isEnabled(runtime.catalog["ui_swipe"]!!))
        // The private risk recognition lists widen the packaged tables for every admitted run.
        val widened = runtime.policy(setOf("observe", "act"), AgentSettings(riskPackages = setOf("com.example.pay"), riskKeywords = setOf("remit")))
        assertTrue(widened.isPayment(RiskContext(packageName = "com.example.pay")))
        assertEquals(RiskLevel.SENSITIVE, widened.risk(runtime.catalog["ui_click"]!!, RiskContext(nodeText = "Remit funds")))
        assertEquals(RiskLevel.NORMAL, policy.risk(runtime.catalog["ui_click"]!!, RiskContext(nodeText = "Remit funds")))
    }
}
