package io.github.supermonster003.autojs6.plugin.ai.agent

import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class AgentCoreDeviceTest {
    @Test fun packagedCatalogAndKeywordDataLoadOnAndroid() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val catalog = ToolCatalog(assets.open("catalog/tools.json").bufferedReader().use { it.readText() })
        val keywords = ToolPolicy.readKeywords(assets.open("catalog/sensitive-keywords.json").bufferedReader().use { it.readText() })
        val policy = ToolPolicy(keywords = keywords)
        assertEquals(30, catalog.tools.size)
        assertEquals(RiskLevel.SENSITIVE, policy.risk(catalog["ui_click"]!!, RiskContext(nodeText = "确认订单")))
        val request = (ToolHandlers(catalog).prepare("ui_dump", AgentJson.objectOf("{}"), policy) as ToolPlan.Call).request
        assertEquals("compact", request.args[0].asJsonObject.string("format"))
    }

    @Test fun strictTreeParsingAndUnicodeObservationMatchJvmBehavior() {
        assertThrows(Exception::class.java) { AgentJson.parse("{\"x\":1,\"x\":2}") }
        assertEquals("中😀", AgentJson.objectOf("{\"text\":\"中😀\"}").string("text"))
        val text = ToolObservation.success(jsonObject("text" to "😀".repeat(2000).json()), 1024)
        assertTrue(text.toByteArray(Charsets.UTF_8).size <= 1024)
        assertTrue(AgentJson.objectOf(text).flag("truncated")!!)
    }
}
