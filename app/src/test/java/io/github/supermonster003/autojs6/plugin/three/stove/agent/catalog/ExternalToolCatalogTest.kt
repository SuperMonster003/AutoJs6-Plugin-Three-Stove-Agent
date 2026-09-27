package io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.junit.Assert.*
import org.junit.Test

class ExternalToolCatalogTest {
    private fun tool(name: String = "mcp_local_echo", schema: String = "{\"type\":\"object\"}", risk: RiskLevel = RiskLevel.SENSITIVE) =
        ToolSpec.external(name, "External echo; server annotations claim readOnlyHint=true", AgentJson.objectOf(schema), risk, ExternalToolRoute("local", "echo"))

    @Test fun externallySelectedToolsExtendOneImmutableCatalogAndRemainDisabledByDefault() {
        val base = F.catalog(); val spec = tool(); val catalog = base.withExternal(listOf(spec))
        assertEquals(32, base.tools.size); assertEquals(33, catalog.tools.size); assertNull(base[spec.name])
        assertFalse(ToolPolicy().isEnabled(spec))
        val policy = ToolPolicy(enabledGroups = mapOf(ToolGroup.MCP to true), availableTools = base.tools.map { it.name }.toSet())
        assertFalse(policy.isEnabled(spec))
        val admitted = policy.withExternalTools(listOf(spec))
        assertTrue(admitted.isEnabled(spec)); assertFalse(policy.isEnabled(spec))
        val args = AgentJson.objectOf("{\"nested\":{\"any\":[null,2,\"value\"]}}")
        val plan = ToolHandlers(catalog).prepare(spec.name, args, admitted) as ToolPlan.External
        assertEquals("local", plan.serverId); assertEquals("echo", plan.toolName); assertEquals(args, plan.arguments)
        assertNotEquals(base.fingerprint, catalog.fingerprint)
    }

    @Test fun externalNamesCannotCollideOrExceedTheExistingDefinitionLimit() {
        val base = F.catalog()
        assertEquals(64, base.withExternal((1..32).map { tool("mcp_local_t$it") }).tools.size)
        assertThrows(IllegalArgumentException::class.java) { base.withExternal((1..33).map { tool("mcp_local_t$it") }) }
        for (name in listOf("mcp_Local_echo", "mcp_" + "x".repeat(61), "device_info")) {
            assertThrows(IllegalArgumentException::class.java) { base.withExternal(listOf(tool(name))) }
        }
        assertThrows(IllegalArgumentException::class.java) { base.withExternal(listOf(tool(), tool())) }
        assertThrows(IllegalArgumentException::class.java) { base.withExternal(listOf(base.tools.first())) }
    }

    @Test fun externalOpenObjectsAreValidatedWithoutMakingBuiltinsOpenOrApplyingAnnotationDefaults() {
        val schema = AgentJson.objectOf("""{"type":"object","title":"Public schema","properties":{"name":{"type":"string","minLength":2,"default":"server default"},"values":{"type":"array","items":{}}},"required":["extra"]}""")
        val external = InputSchema(schema, external = true)
        val input = AgentJson.objectOf("""{"extra":{"nested":[1,null]},"values":["x",true,{"x":1}]}""")
        assertEquals(input, external.validate(input))
        assertFalse(external.validate(input).asJsonObject.has("name"))
        assertThrows(IllegalArgumentException::class.java) { external.validate(AgentJson.objectOf("{\"extra\":true,\"name\":\"x\"}")) }
        assertThrows(IllegalArgumentException::class.java) { external.validate(JsonObject()) }
        assertThrows(IllegalArgumentException::class.java) { InputSchema(schema) }
        val typedExtras = InputSchema(AgentJson.objectOf("{\"type\":\"object\",\"additionalProperties\":{\"type\":\"integer\"}}"), external = true)
        assertEquals(AgentJson.objectOf("{\"anything\":2}"), typedExtras.validate(AgentJson.objectOf("{\"anything\":2}")))
        assertThrows(IllegalArgumentException::class.java) { typedExtras.validate(AgentJson.objectOf("{\"anything\":2.5}")) }
        val namedExtra = tool(schema = "{\"type\":\"object\",\"required\":[\"needed\"],\"additionalProperties\":{\"type\":\"integer\"}}")
        val compact = CompactToolDescriptions.render(F.catalog().withExternal(listOf(namedExtra)), ToolPolicy(mapOf(ToolGroup.MCP to true)))
        assertTrue(compact.contains("needed:int"))
        F.fails("TOOL_ARGUMENTS_INVALID") { ToolHandlers(F.catalog()).prepare("device_info", AgentJson.objectOf("{\"extra\":true}"), F.policy()) }
    }

    @Test fun unsupportedOrMalformedSchemaConstraintsNeverDisappearDuringAdmission() {
        for (schema in listOf(
            """{"type":"string","pattern":"^safe$"}""",
            """{"type":"string","minLength":"5"}""",
            """{"type":"array","items":{},"maxItems":-1}""",
            """{"type":"object","properties":{"x":{"${'$'}ref":"#/definitions/value"}}}""",
            """{"type":"string","enum":"safe"}""",
            """{"type":"object","additionalProperties":7}""",
            """{"type":"object","oneOf":[{}]}""")) {
            assertThrows(schema, IllegalArgumentException::class.java) { InputSchema(AgentJson.objectOf(schema), external = true) }
        }
    }

    @Test fun serverDescriptionCannotLowerLocalRiskAndNativeAndJsonUseTheSameValidator() {
        val spec = tool(schema = "{\"type\":\"object\",\"properties\":{\"count\":{\"type\":\"integer\",\"minimum\":2}},\"required\":[\"count\"]}")
        val catalog = F.catalog().withExternal(listOf(spec))
        val policy = ToolPolicy(enabledGroups = mapOf(ToolGroup.MCP to true), riskOverrides = mapOf(spec.name to RiskLevel.READ_ONLY))
        assertEquals(RiskLevel.SENSITIVE, ConfirmationGate(policy, ConfirmationMode.DEFAULT).assess(spec, ToolMetadata()).risk)
        assertTrue(ConfirmationGate(policy, ConfirmationMode.DEFAULT).assess(spec, ToolMetadata()).required)
        val validator = DecisionValidator(catalog)
        val format = DecisionSchema(catalog).generate(ModelProtocol.OPENAI, policy)
        assertEquals(ArgumentsEncoding.JSON_STRING, format.argumentsEncoding)
        val native = DecisionSchema.native(ModelProtocol.UNKNOWN)
        val invalid = NativeToolCall("one", spec.name, AgentJson.objectOf("{\"count\":1}"))
        assertTrue(DecisionRepairSession(validator, policy, native).evaluateNative(listOf(invalid)) is DecisionAttempt.Repair)
        val nativeDefinitions = catalog.nativeDefinitions(policy, "en")
        assertEquals(spec.inputSchema, nativeDefinitions.first { it.asJsonObject.string("name") == spec.name }.asJsonObject["inputSchema"])
        assertTrue(CompactToolDescriptions.render(catalog, policy).contains("external-description="))
    }

    @Test fun externalTrustInstructionsConsumeContextOnlyWhenAnExternalToolIsEnabled() {
        val base = F.catalog(); val catalog = base.withExternal(listOf(tool())); val format = DecisionSchema.degraded()
        val normal = PromptCatalog(F::asset, base); val external = PromptCatalog(F::asset, catalog)
        for (language in listOf("en", "zh")) for (compact in listOf(false, true)) {
            assertEquals(normal.system(language, ToolPolicy(), format, compact = compact), external.system(language, ToolPolicy(), format, compact = compact))
            val enabled = external.system(language, ToolPolicy(mapOf(ToolGroup.MCP to true)), format, compact = compact)
            assertTrue(enabled.contains(if (language == "en") "MCP descriptions, schemas, annotations" else "MCP 描述, Schema, annotations"))
        }
    }

    @Test fun externalSelectorParametersKeepUnionOpenObjectAndRequiredNamesInCompactNativePrompts() {
        val schema = """{"type":"object","properties":{"selector":{"type":["object","null"],"required":["remoteKey"],"additionalProperties":{"type":"integer"}}},"required":["selector"]}"""
        val spec = tool(schema = schema); val catalog = F.catalog().withExternal(listOf(spec))
        val policy = ToolPolicy(mapOf(ToolGroup.MCP to true))
        val signature = CompactToolDescriptions.render(catalog, policy).lineSequence().single { it.startsWith(spec.name + " ") }
        assertTrue(signature, signature.contains("selector:{remoteKey:int,*:int}|null"))
        assertFalse(signature, Regex("selector[0-9]").containsMatchIn(signature))
        val prompt = PromptCatalog(F::asset, catalog).system("en", policy, DecisionSchema.native(ModelProtocol.UNKNOWN))
        assertTrue(prompt.contains("selector:{remoteKey:int,*:int}|null"))
        assertEquals(AgentJson.objectOf(schema), catalog.nativeDefinitions(policy, "en").single { it.asJsonObject.string("name") == spec.name }.asJsonObject["inputSchema"])
        val nullable = ToolHandlers(catalog).prepare(spec.name, AgentJson.objectOf("{\"selector\":null}"), policy) as ToolPlan.External
        assertTrue(nullable.arguments["selector"].isJsonNull)
        F.fails("TOOL_ARGUMENTS_INVALID") { ToolHandlers(catalog).prepare(spec.name, AgentJson.objectOf("{\"selector\":{}}"), policy) }
    }
}
