package io.github.supermonster003.autojs6.plugin.three.stove.agent

import org.autojs.plugin.common.api.PluginActions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreeStoveAgentPluginRuntimeInfoTest {

    @Test
    fun `runtime fields are assembled without losing the plugin identity`() {
        val info = ThreeStoveAgentPluginRuntimeInfo(
            name = "3-Stove Agent",
            description = "Runs natural-language tasks by choosing registered scripts and operating the screen step by step",
            instruction = "# 3-Stove Agent",
            versionName = "1.1.0",
            versionCode = 1L,
            versionDate = "Sep 22, 2026",
        )

        assertEquals("3-Stove Agent", info.name)
        assertEquals("Runs natural-language tasks by choosing registered scripts and operating the screen step by step", info.description)
        assertEquals("# 3-Stove Agent", info.instruction)
        assertEquals("SuperMonster003", info.author)
        assertEquals("three-stove-agent", info.id)
        assertEquals("three-stove-agent", info.engine)
        assertEquals("default", info.variant)
        assertEquals("1.1.0", info.versionName)
        assertEquals(1L, info.versionCode)
        assertEquals("Sep 22, 2026", info.versionDate)
        assertArrayEquals(emptyArray<String>(), info.supportedAbis)
        assertEquals(5298L, info.requiresHostVersion)
        assertEquals(ThreeStoveAgentPlugin.REQUIRED_HOST_VERSION, info.requiresHostVersion)
    }

    @Test
    fun `identity constants follow the host discovery contract`() {
        assertEquals("io.github.supermonster003.autojs6.plugin.three.stove.agent", ThreeStoveAgentPlugin.PACKAGE_NAME)
        assertEquals("org.autojs.autojs6", ThreeStoveAgentPlugin.HOST_PACKAGE_NAME)
        assertEquals("three-stove-agent", ThreeStoveAgentPlugin.ID)
        assertEquals(ThreeStoveAgentPlugin.ID, ThreeStoveAgentPlugin.ENGINE)
        assertEquals("default", ThreeStoveAgentPlugin.VARIANT)
        assertEquals("SuperMonster003", ThreeStoveAgentPlugin.AUTHOR)
        assertEquals("org.autojs.plugin.THREE_STOVE_AGENT", ThreeStoveAgentPlugin.SERVICE_ACTION)
        assertEquals("three-stove-agent", ThreeStoveAgentPlugin.SERVICE_CATEGORY)
        assertEquals(":agent", ThreeStoveAgentPlugin.SERVICE_PROCESS)
        assertEquals("org.autojs.plugin.INFO", ThreeStoveAgentPlugin.INFO_ACTION)
        assertEquals(PluginActions.INFO, ThreeStoveAgentPlugin.INFO_ACTION)
        assertEquals("org.autojs.plugin.three.stove.agent.api.IThreeStoveAgentPlugin", ThreeStoveAgentPlugin.SERVICE_DESCRIPTOR)
    }
}
