package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DecisionSchemaTest {
    private val catalog = F.catalog()
    private val schema = DecisionSchema(catalog)
    private val minimal = ToolPolicy(ToolGroup.entries.associateWith { it == ToolGroup.USER })

    @Test fun providerSchemasMatchReviewedSnapshots() {
        val snapshots = JsonObject()
        for (protocol in ModelProtocol.entries) {
            for ((label, policy) in listOf("default" to ToolPolicy(), "minimal" to minimal)) {
                val format = schema.generate(protocol, policy)
                snapshots.add("${protocol.name}-$label", jsonObject("encoding" to format.argumentsEncoding.name.json(),
                    "reason" to format.reason.json(), "schema" to (format.responseSchemaJson?.let(AgentJson::parse) ?: JsonNull.INSTANCE)))
            }
        }
        checkSnapshot("decision-schemas.snapshot.json", snapshots)
    }

    @Test fun dynamicScriptParametersUseStringOnlyForOnlineProtocols() {
        for (protocol in listOf(ModelProtocol.OPENAI, ModelProtocol.ANTHROPIC, ModelProtocol.GEMINI)) {
            val format = schema.generate(protocol, ToolPolicy())
            assertEquals(ArgumentsEncoding.JSON_STRING, format.argumentsEncoding)
            assertEquals("DYNAMIC_PARAMETERS", format.reason)
        }
        assertEquals(ArgumentsEncoding.OBJECT, schema.generate(ModelProtocol.LOCAL, ToolPolicy(), forceString = true).argumentsEncoding)
        for (protocol in listOf(ModelProtocol.OPENAI, ModelProtocol.ANTHROPIC, ModelProtocol.GEMINI)) {
            assertEquals(ArgumentsEncoding.OBJECT, schema.generate(protocol, minimal).argumentsEncoding)
        }
    }

    @Test fun allVariantsRespectWireLimitsAndProtocolKeywords() {
        val forbidden = setOf("maxLength", "minLength", "minItems", "maxItems", "minimum", "maximum", "default")
        for (protocol in ModelProtocol.entries.filter { it != ModelProtocol.UNKNOWN }) {
            for (policy in listOf(F.policy(), minimal, ToolPolicy(ToolGroup.entries.associateWith { it != ToolGroup.SCRIPT }))) {
                val format = schema.generate(protocol, policy)
                assertTrue(format.responseSchemaJson!!.toByteArray(Charsets.UTF_8).size <= 16 * 1024)
                visit(AgentJson.objectOf(format.responseSchemaJson)) { node ->
                    assertFalse(node.keySet().any { it in forbidden })
                    if (protocol == ModelProtocol.GEMINI) {
                        assertFalse(node.has("additionalProperties"))
                        assertFalse(node["type"]?.isJsonArray == true)
                    } else if (node.string("type") == "object" && protocol != ModelProtocol.LOCAL) {
                        assertEquals(false.json(), node["additionalProperties"])
                        if (protocol == ModelProtocol.OPENAI) assertEquals(node.getAsJsonObject("properties").keySet(), node.getAsJsonArray("required").map { it.asString }.toSet())
                    }
                }
            }
        }
    }

    @Test fun complexAnthropicObjectSchemasFallBackWithinItsOptionalParameterBudget() {
        val policy = ToolPolicy(ToolGroup.entries.associateWith { it == ToolGroup.ACT })
        val format = schema.generate(ModelProtocol.ANTHROPIC, policy)
        assertEquals("SCHEMA_COMPLEXITY", format.reason)
        assertEquals(ArgumentsEncoding.JSON_STRING, format.argumentsEncoding)
        var optionalCount = 0
        visit(AgentJson.objectOf(format.responseSchemaJson!!)) { node ->
            node.getAsJsonObject("properties")?.let { optionalCount += it.size() - node.getAsJsonArray("required").size() }
        }
        assertTrue(optionalCount <= 24)
    }

    @Test fun disabledToolsDisappearAndEmptyCatalogStillAllowsQuestionsAndCompletion() {
        val root = AgentJson.objectOf(schema.generate(ModelProtocol.LOCAL, minimal).responseSchemaJson!!)
        assertEquals(listOf("report_progress"), root.getAsJsonObject("properties").getAsJsonObject("tool").getAsJsonArray("enum").map { it.asString })
        val empty = ToolPolicy(ToolGroup.entries.associateWith { false })
        val noTools = AgentJson.objectOf(schema.generate(ModelProtocol.OPENAI, empty).responseSchemaJson!!)
        assertEquals(listOf("ask", "done"), noTools.getAsJsonObject("properties").getAsJsonObject("kind").getAsJsonArray("enum").map { it.asString })
    }

    @Test fun onlyExplicitOnlineRequestRejectionTriggersOneRememberedFallback() {
        val fallback = SchemaFallbacks(schema)
        val target = SchemaTarget("provider", "opaque-profile", ModelProtocol.OPENAI, true)
        val initial = fallback.select(target, minimal)
        for (reason in listOf("PROVIDER_FAILED", "MODEL_FAILED", "TIMEOUT", "RATE_LIMITED", "CANCELLED")) {
            assertNull(fallback.onRejected(target, initial, reason, minimal))
        }
        assertEquals(ArgumentsEncoding.OBJECT, fallback.select(target, minimal).argumentsEncoding)
        assertEquals(ArgumentsEncoding.JSON_STRING, fallback.onRejected(target, initial, "REQUEST_REJECTED", minimal)!!.argumentsEncoding)
        assertNull(fallback.onRejected(target, initial, "REQUEST_REJECTED", minimal))
        assertEquals(ArgumentsEncoding.JSON_STRING, fallback.select(target, minimal).argumentsEncoding)
        assertEquals(ArgumentsEncoding.OBJECT, fallback.select(target.copy(targetId = "other"), minimal).argumentsEncoding)
        assertEquals(ArgumentsEncoding.OBJECT, fallback.select(target.copy(providerId = "different-provider"), minimal).argumentsEncoding)
        fallback.clear()
        assertEquals(ArgumentsEncoding.OBJECT, fallback.select(target, minimal).argumentsEncoding)
    }

    @Test fun unknownProtocolNeverGuessesFromTargetOrProviderName() {
        val fallback = SchemaFallbacks(schema)
        val unknown = fallback.select(SchemaTarget("openai", "profile:gpt-test", ModelProtocol.UNKNOWN, true), minimal)
        assertTrue(unknown.degraded)
        assertEquals("PROTOCOL_UNKNOWN", unknown.reason)
        val plain = fallback.select(SchemaTarget("p", "t", ModelProtocol.OPENAI, false), minimal)
        assertTrue(plain.degraded)
        assertEquals("STRUCTURED_JSON_UNAVAILABLE", plain.reason)
        val local = SchemaTarget("p", "t", ModelProtocol.LOCAL, true)
        assertNull(fallback.onRejected(local, fallback.select(local, minimal), "REQUEST_REJECTED", minimal))
    }

    @Test fun fallbackMemoryHasABoundedLinkLifetime() {
        val fallback = SchemaFallbacks(schema)
        repeat(33) {
            val target = SchemaTarget("p", "t$it", ModelProtocol.GEMINI, true)
            assertNotNull(fallback.onRejected(target, fallback.select(target, minimal), "REQUEST_REJECTED", minimal))
        }
        assertEquals(ArgumentsEncoding.OBJECT, fallback.select(SchemaTarget("p", "t0", ModelProtocol.GEMINI, true), minimal).argumentsEncoding)
        assertEquals(ArgumentsEncoding.JSON_STRING, fallback.select(SchemaTarget("p", "t32", ModelProtocol.GEMINI, true), minimal).argumentsEncoding)
    }

    private fun visit(node: JsonObject, action: (JsonObject) -> Unit) {
        action(node)
        node.getAsJsonObject("properties")?.entrySet()?.forEach { visit(it.value.asJsonObject, action) }
        node.getAsJsonObject("items")?.let { visit(it, action) }
        node.getAsJsonArray("anyOf")?.forEach { visit(it.asJsonObject, action) }
    }

    companion object {
        fun checkSnapshot(name: String, actual: JsonElement) {
            val expectedFile = File(F.root, "app/src/test/resources/$name")
            val actualText = GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping().create().toJson(actual) + "\n"
            // Write review candidates only to ignored build output; tests never update the approved fixture.
            if (!expectedFile.exists() || AgentJson.parse(expectedFile.readText(), 512 * 1024) != actual) {
                File(F.root, "build/$name.actual").writeText(actualText)
                fail("Snapshot differs: $name; review build/$name.actual")
            }
        }
    }
}
