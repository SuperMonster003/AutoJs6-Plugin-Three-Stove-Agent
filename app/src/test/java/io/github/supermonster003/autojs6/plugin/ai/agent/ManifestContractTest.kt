package io.github.supermonster003.autojs6.plugin.ai.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Keeps `AndroidManifest.xml` and [AiAgentPlugin] from drifting apart: the host discovers the
 * plugin through the manifest, while the services and tests use the Kotlin constants.
 */
class ManifestContractTest {

    private val manifest: Element by lazy {
        val path = findProjectRoot().resolve("app/src/main/AndroidManifest.xml")
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        factory.newDocumentBuilder().parse(path.toFile()).documentElement
    }

    @Test
    fun `manifest declares only task foreground and plugin permissions and queries the host package`() {
        val permissions = manifest.children("uses-permission").map { it.androidAttribute("name") }
        assertEquals(listOf(PLUGIN_PERMISSION, "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_SPECIAL_USE", "android.permission.POST_NOTIFICATIONS", "android.permission.INTERNET"), permissions)

        val queried = manifest.child("queries").children("package").map { it.androidAttribute("name") }
        assertEquals(listOf(AiAgentPlugin.HOST_PACKAGE_NAME), queried)
    }

    @Test
    fun `application metadata points at the wake activity and the author string`() {
        val application = manifest.child("application")
        assertEquals("false", application.androidAttribute("allowBackup"))
        assertEquals("@string/app_name", application.androidAttribute("label"))
        assertEquals("@mipmap/ic_launcher", application.androidAttribute("icon"))
        assertEquals("@style/Theme.AiAgent", application.androidAttribute("theme"))
        assertEquals("@xml/data_extraction_rules", application.androidAttribute("dataExtractionRules"))
        assertEquals("@xml/locales_config", application.androidAttribute("localeConfig"))

        val metaData = application.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(".WakeActivity", metaData["org.autojs.plugin.WAKE_ACTIVITY"])
        assertEquals("@string/plugin_author", metaData["org.autojs.plugin.info.AUTHOR"])
        assertEquals("0", metaData["org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT"])
    }

    @Test
    fun `wake and launcher are exported while script settings remain private`() {
        val activities = manifest.child("application").children("activity")
        assertEquals(listOf(".WakeActivity", ".ui.LauncherActivity", ".ui.ScriptRootsActivity", ".ui.RunDetailActivity", ".ui.HistoryActivity", ".ui.PresetsActivity", ".ui.MemoryActivity", ".ui.SettingsActivity", ".ui.ReleaseHistoryActivity", ".ui.ConfirmationActivity"), activities.map { it.androidAttribute("name") })
        assertEquals("true", activities.last().androidAttribute("excludeFromRecents"))
        assertEquals("@style/Theme.AiAgent.Dialog.Light", activities.last().androidAttribute("theme"))

        val wake = activities.first()
        assertEquals("true", wake.androidAttribute("exported"))
        assertEquals("true", wake.androidAttribute("excludeFromRecents"))
        assertEquals("true", wake.androidAttribute("finishOnTaskLaunch"))
        assertEquals(PLUGIN_PERMISSION, wake.androidAttribute("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.androidAttribute("theme"))
        val wakeFilter = wake.child("intent-filter")
        assertEquals(listOf("org.autojs.plugin.action.WAKE"), wakeFilter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.category.DEFAULT"), wakeFilter.children("category").map { it.androidAttribute("name") })

        val launcher = activities[1]
        assertEquals("true", launcher.androidAttribute("exported"))
        assertNull(launcher.androidAttributeOrNull("permission"))
        assertNull(launcher.androidAttributeOrNull("process"))
        val launcherFilter = launcher.child("intent-filter")
        assertEquals(listOf("android.intent.action.MAIN"), launcherFilter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.category.LAUNCHER"), launcherFilter.children("category").map { it.androidAttribute("name") })

        for (settings in activities.drop(2)) {
        assertEquals("false", settings.androidAttribute("exported"))
        assertNull(settings.androidAttributeOrNull("process"))
        assertTrue(settings.children("intent-filter").isEmpty())
        }

        assertTrue(manifest.child("application").children("receiver").isEmpty())
        assertTrue(manifest.child("application").children("provider").isEmpty())
    }

    @Test
    fun `info service and agent service match the identity constants`() {
        val services = manifest.child("application").children("service").associateBy { it.androidAttribute("name") }
        assertEquals(setOf(".AiAgentPluginInfoService", ".AiAgentPluginService", ".service.AgentLocalService", ".AiAgentTaskForegroundService"), services.keys)
        for (name in listOf(".service.AgentLocalService", ".AiAgentTaskForegroundService")) {
            assertEquals("false", services.getValue(name).androidAttribute("exported"))
            assertEquals(":agent", services.getValue(name).androidAttribute("process"))
        }
        assertEquals("specialUse", services.getValue(".AiAgentTaskForegroundService").androidAttribute("foregroundServiceType"))

        val info = services.getValue(".AiAgentPluginInfoService")
        assertDiscoveryContract(info, AiAgentPlugin.INFO_ACTION)
        assertNull(info.androidAttributeOrNull("process"))

        val agent = services.getValue(".AiAgentPluginService")
        assertDiscoveryContract(agent, AiAgentPlugin.SERVICE_ACTION)
        assertEquals(AiAgentPlugin.SERVICE_PROCESS, agent.androidAttribute("process"))
    }

    private fun assertDiscoveryContract(service: Element, action: String) {
        assertEquals("true", service.androidAttribute("exported"))
        assertEquals("true", service.androidAttribute("enabled"))
        assertEquals(PLUGIN_PERMISSION, service.androidAttribute("permission"))
        val filter = service.child("intent-filter")
        assertEquals(listOf(action), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf(AiAgentPlugin.SERVICE_CATEGORY), filter.children("category").map { it.androidAttribute("name") })
        val metaData = service.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(AiAgentPlugin.REQUIRED_HOST_VERSION.toString(), metaData["requiresHostVersion"])
    }

    private fun Element.children(tag: String): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length)
            .map { nodes.item(it) }
            .filterIsInstance<Element>()
            .filter { it.tagName == tag }
    }

    private fun Element.child(tag: String): Element = children(tag).single()

    private fun Element.androidAttribute(name: String): String =
        androidAttributeOrNull(name) ?: error("Missing android:$name on <$tagName>")

    private fun Element.androidAttributeOrNull(name: String): String? =
        if (hasAttributeNS(ANDROID_NAMESPACE, name)) getAttributeNS(ANDROID_NAMESPACE, name) else null

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
    }
}
