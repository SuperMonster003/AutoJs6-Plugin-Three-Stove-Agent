package io.github.supermonster003.autojs6.plugin.ai.agent.model

import org.junit.Assert.*
import org.junit.Test

class ModelTargetTest {
    private fun entry() = AgentJson.objectOf("""{"targetId":"profile:openai-looking-name","displayName":"OpenAI","locality":2,"configured":true,"available":true,"capabilityIds":["structured-json"],"maximumContextBytes":65536}""")
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
}
