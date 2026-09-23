package io.github.supermonster003.autojs6.plugin.ai.agent

import org.autojs.plugin.common.api.PluginActions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AiAgentPluginRuntimeInfoTest {

    @Test
    fun `runtime fields are assembled without losing the plugin identity`() {
        val info = AiAgentPluginRuntimeInfo(
            name = "AI Agent",
            description = "Runs natural-language tasks by choosing registered scripts and operating the screen step by step",
            instruction = "# AI Agent",
            versionName = "1.0.0",
            versionCode = 1L,
            versionDate = "Sep 22, 2026",
        )

        assertEquals("AI Agent", info.name)
        assertEquals("Runs natural-language tasks by choosing registered scripts and operating the screen step by step", info.description)
        assertEquals("# AI Agent", info.instruction)
        assertEquals("SuperMonster003", info.author)
        assertEquals("ai-agent", info.id)
        assertEquals("ai-agent", info.engine)
        assertEquals("default", info.variant)
        assertEquals("1.0.0", info.versionName)
        assertEquals(1L, info.versionCode)
        assertEquals("Sep 22, 2026", info.versionDate)
        assertArrayEquals(emptyArray<String>(), info.supportedAbis)
        assertEquals(5289L, info.requiresHostVersion)
        assertEquals(AiAgentPlugin.REQUIRED_HOST_VERSION, info.requiresHostVersion)
    }

    @Test
    fun `identity constants follow the host discovery contract`() {
        assertEquals("io.github.supermonster003.autojs6.plugin.ai.agent", AiAgentPlugin.PACKAGE_NAME)
        assertEquals("org.autojs.autojs6", AiAgentPlugin.HOST_PACKAGE_NAME)
        assertEquals("ai-agent", AiAgentPlugin.ID)
        assertEquals(AiAgentPlugin.ID, AiAgentPlugin.ENGINE)
        assertEquals("default", AiAgentPlugin.VARIANT)
        assertEquals("SuperMonster003", AiAgentPlugin.AUTHOR)
        assertEquals("org.autojs.plugin.AI_AGENT", AiAgentPlugin.SERVICE_ACTION)
        assertEquals("ai-agent", AiAgentPlugin.SERVICE_CATEGORY)
        assertEquals(":agent", AiAgentPlugin.SERVICE_PROCESS)
        assertEquals("org.autojs.plugin.INFO", AiAgentPlugin.INFO_ACTION)
        assertEquals(PluginActions.INFO, AiAgentPlugin.INFO_ACTION)
        assertEquals("org.autojs.plugin.ai.agent.api.IAiAgentPlugin", AiAgentPlugin.SERVICE_DESCRIPTOR)
    }
}
