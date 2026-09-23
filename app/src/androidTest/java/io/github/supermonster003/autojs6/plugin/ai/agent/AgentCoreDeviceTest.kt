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

    @Test fun packagedPromptTemplatesAndDecisionRepairWorkOnAndroid() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        fun read(path: String) = assets.open(path).bufferedReader().use { it.readText() }
        val catalog = ToolCatalog(read("catalog/tools.json"))
        val format = DecisionSchema(catalog).generate(ModelProtocol.LOCAL, ToolPolicy())
        val prompts = PromptCatalog(::read, catalog)
        for (language in listOf("en", "zh")) {
            val system = prompts.system(language, ToolPolicy(), format)
            assertTrue(system.contains("ui_dump"))
            assertFalse(system.contains("{{tools_json}}"))
        }
        val session = DecisionRepairSession(DecisionValidator(catalog), ToolPolicy(), DecisionSchema.degraded())
        assertTrue(session.evaluate("invalid") is DecisionAttempt.Repair)
        val accepted = session.evaluate("```json\n{\"kind\":\"tool\",\"tool\":\"ui_dump\",\"arguments\":{}}\n```") as DecisionAttempt.Accepted
        assertEquals(ParseMode.EXTRACTED, accepted.parseMode)
        assertEquals("ui_dump", (accepted.decision as AgentDecision.Tool).name)
    }

    @Test fun decisionUnicodeLimitsAndOnlineArgumentEncodingMatchJvm() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val catalog = ToolCatalog(assets.open("catalog/tools.json").bufferedReader().use { it.readText() })
        val format = DecisionSchema(catalog).generate(ModelProtocol.OPENAI, ToolPolicy())
        assertEquals(ArgumentsEncoding.JSON_STRING, format.argumentsEncoding)
        val raw = jsonObject("kind" to "tool".json(), "tool" to "ui_dump".json(),
            "arguments" to "{}".json(), "reasoning" to "😀中".repeat(400).json())
        val decision = DecisionValidator(catalog).validate(DecisionParser.parse(raw.toString()), ToolPolicy(), format)
        assertEquals("😀中".repeat(300), decision.reasoning)
        raw.add("done", jsonObject("status" to "completed".json(), "summary" to "ok".json()))
        assertThrows(DecisionFailure::class.java) { DecisionValidator(catalog).validate(DecisionParser.parse(raw.toString()), ToolPolicy(), format) }
    }
}
