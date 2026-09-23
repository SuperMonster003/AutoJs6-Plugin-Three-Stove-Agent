package io.github.supermonster003.autojs6.plugin.ai.agent.model

import org.junit.Assert.*
import org.junit.Test

class ModelTargetTest {
    private fun entry() = AgentJson.objectOf("""{"targetId":"profile:openai-looking-name","displayName":"OpenAI","locality":2,"configured":true,"available":true,"capabilityIds":["structured-json"],"supportedControls":["response-json-schema","maximum-output-tokens"],"maximumContextBytes":65536}""")
    @Test fun protocolIsNotInferredFromProviderOrTargetNames() {
        val target = ModelTarget.fromCatalog("three-stone-ai", entry())
        assertEquals(ModelProtocol.UNKNOWN, target.protocol); assertTrue(target.structuredJson)
        assertEquals(ModelLocality.REMOTE, target.locality)
    }
    @Test fun localAndHybridMetadataAreHandledConservatively() {
        assertEquals(ModelProtocol.LOCAL, ModelTarget.fromCatalog("provider", entry().apply { addProperty("locality", 1); addProperty("targetId", "local:test") }).protocol)
        assertEquals(ModelProtocol.UNKNOWN, ModelTarget.fromCatalog("provider", entry().apply { addProperty("locality", 3) }).protocol)
    }
    @Test fun unavailableMalformedOrUnboundedTargetsAreRejected() {
        for (entry in listOf(entry().apply { addProperty("available", false) }, entry().apply { addProperty("configured", false) },
            entry().apply { addProperty("maximumContextBytes", Long.MAX_VALUE) }, entry().apply { addProperty("locality", 99) },
            entry().apply { addProperty("targetId", "../../invalid") })) {
            assertThrows(IllegalArgumentException::class.java) { ModelTarget.fromCatalog("provider", entry) }
        }
    }
    @Test fun idsMatchThePublicAiCommonGrammar() {
        for (id in listOf("local:UPPER", "local:a:b", "local:" + "x".repeat(129))) {
            assertThrows(IllegalArgumentException::class.java) { ModelTarget.fromCatalog("provider", entry().apply { addProperty("targetId", id) }) }
        }
        assertEquals("custom-protocol:valid_target", ModelTarget.fromCatalog("provider", entry().apply { addProperty("targetId", "custom-protocol:valid_target") }).targetId)
    }
    @Test fun schemaStreamingAndOutputControlsAreNegotiatedIndependently() {
        val metadata = entry().apply { add("supportedControls", jsonArray()) }
        val target = ModelTarget.fromCatalog("provider", metadata)
        assertFalse(target.structuredJson); assertFalse(target.supportsOutputLimit); assertFalse(target.supportsStreaming)
        metadata.getAsJsonArray("capabilityIds").add("streaming")
        assertTrue(ModelTarget.fromCatalog("provider", metadata).supportsStreaming)
    }
}
