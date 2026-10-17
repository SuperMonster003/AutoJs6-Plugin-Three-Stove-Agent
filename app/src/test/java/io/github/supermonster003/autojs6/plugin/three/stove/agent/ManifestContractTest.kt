package io.github.supermonster003.autojs6.plugin.three.stove.agent

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
 * Keeps `AndroidManifest.xml` and [ThreeStoveAgentPlugin] from drifting apart: the host discovers the
 * plugin through the manifest, while the services and tests use the Kotlin constants.
 */
class ManifestContractTest {

    private val manifest: Element by lazy {
        val path = findProjectRoot().resolve("app/src/main/AndroidManifest.xml")
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        factory.newDocumentBuilder().parse(path.toFile()).documentElement
    }

    @Test
    fun `manifest declares the D28 permissions plus boot and queries the host package and the speech actions`() {
        val permissions = manifest.children("uses-permission").map { it.androidAttribute("name") }
        assertEquals(listOf(PLUGIN_PERMISSION, "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_SPECIAL_USE", "android.permission.POST_NOTIFICATIONS", "android.permission.INTERNET",
            "android.permission.ACCESS_LOCAL_NETWORK", "android.permission.SYSTEM_ALERT_WINDOW",
            // Only posts "tap to resume" for an enabled resident listener after boot (roadmap D67).
            "android.permission.RECEIVE_BOOT_COMPLETED"), permissions)

        val queried = manifest.child("queries").children("package").map { it.androidAttribute("name") }
        assertEquals(listOf(ThreeStoveAgentPlugin.HOST_PACKAGE_NAME), queried)
        val intents = manifest.child("queries").children("intent").map { it.child("action").androidAttribute("name") }
        // The speech service of 3-Macaw Speech (roadmap D74) and the other resident listeners (D73) join the recognizers.
        assertEquals(listOf("android.speech.action.RECOGNIZE_SPEECH", "org.autojs.plugin.SPEECH", ThreeStoveAgentPlugin.RESIDENT_LISTENER_ACTION), intents)
        // Bare actions make every matching app visible, including the 3-Macaw Speech sheet that the microphone pins with setPackage.
        for (speech in manifest.child("queries").children("intent"))
            assertEquals(listOf("action"), (0 until speech.childNodes.length).map { speech.childNodes.item(it) }.filterIsInstance<Element>().map { it.tagName })
    }

    @Test
    fun `application metadata points at the wake activity and the author string`() {
        val application = manifest.child("application")
        assertEquals("false", application.androidAttribute("allowBackup"))
        assertEquals("@string/app_name", application.androidAttribute("label"))
        assertEquals("@mipmap/ic_icon_studio_application", application.androidAttribute("icon"))
        assertEquals("@mipmap/ic_icon_studio_application", application.androidAttribute("roundIcon"))
        assertEquals("@style/Theme.ThreeStoveAgent", application.androidAttribute("theme"))
        assertEquals("@xml/data_extraction_rules", application.androidAttribute("dataExtractionRules"))
        assertEquals("@xml/locales_config", application.androidAttribute("localeConfig"))
        assertEquals("@xml/network_security_config", application.androidAttribute("networkSecurityConfig"))

        val metaData = application.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(".WakeActivity", metaData["org.autojs.plugin.WAKE_ACTIVITY"])
        assertEquals("@string/plugin_author", metaData["org.autojs.plugin.info.AUTHOR"])
        assertEquals("0", metaData["org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT"])
    }

    @Test
    fun `wake and launcher are exported while script settings remain private`() {
        val activities = manifest.child("application").children("activity")
        assertEquals(listOf(".WakeActivity", ".ui.LauncherActivity", ".ui.ShareTargetActivity", ".ui.VoiceCommandActivity", ".ui.VoiceInputActivity", ".ui.ScriptRootsActivity", ".ui.RunDetailActivity", ".ui.HistoryActivity", ".ui.PresetsActivity", ".ui.MemoryActivity", ".ui.SettingsActivity", ".ui.McpServersActivity", ".ui.RiskRecognitionActivity", ".ui.ReleaseHistoryActivity", ".ui.AboutActivity", ".ui.ConfirmationActivity", ".ui.FailureAlertActivity"), activities.map { it.androidAttribute("name") })
        assertEquals("true", activities.last().androidAttribute("excludeFromRecents"))
        assertEquals("@style/Theme.ThreeStoveAgent.Dialog.Light", activities.last().androidAttribute("theme"))

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
        assertTrue(launcher.children("intent-filter").isEmpty())
        val allAliases = manifest.child("application").children("activity-alias")
        // The resident listener settings entry for 3-Stone AI and 3-Macaw Speech (D73): signature protected, no filter.
        val residentSettings = allAliases.single { it.androidAttribute("name") == ".speech.ResidentListenerSettings" }
        assertEquals("true", residentSettings.androidAttribute("exported"))
        assertEquals(PLUGIN_PERMISSION, residentSettings.androidAttribute("permission"))
        assertEquals(".ui.SettingsActivity", residentSettings.androidAttribute("targetActivity"))
        assertTrue(residentSettings.children("intent-filter").isEmpty())
        val aliases = allAliases - residentSettings
        assertEquals(listOf("AdaptiveLight", "AdaptiveDark", "AdaptiveAuto", "Transparent").map { ".launcher.${it}IconAlias" },
            aliases.map { it.androidAttribute("name") })
        assertEquals(listOf("@mipmap/ic_launcher_system_light", "@mipmap/ic_launcher_system", "@mipmap/ic_launcher_system_auto", "@mipmap/ic_launcher"),
            aliases.map { it.androidAttribute("icon") })
        for ((index, alias) in aliases.withIndex()) {
            assertEquals((index == 2).toString(), alias.androidAttribute("enabled"))
            assertEquals("true", alias.androidAttribute("exported"))
            assertEquals(".ui.LauncherActivity", alias.androidAttribute("targetActivity"))
            assertEquals("@xml/shortcuts", alias.child("meta-data").androidAttribute("resource"))
            val launcherFilter = alias.child("intent-filter")
            assertEquals(listOf("android.intent.action.MAIN"), launcherFilter.children("action").map { it.androidAttribute("name") })
            assertEquals(listOf("android.intent.category.LAUNCHER"), launcherFilter.children("category").map { it.androidAttribute("name") })
        }

        val share = activities[2]
        assertEquals("true", share.androidAttribute("exported"))
        assertNull(share.androidAttributeOrNull("permission"))
        val filter = share.child("intent-filter")
        assertEquals(listOf("android.intent.action.SEND"), filter.children("action").map { it.androidAttribute("name") })
        assertEquals("text/plain", filter.child("data").androidAttribute("mimeType"))

        // The 3-Macaw Speech assistant target (D60, D78): no permission, no window, next to the floating ball, and no
        // label or icon of its own, so the assistant button shows the application's.
        val voice = activities[3]
        assertEquals(".ui.VoiceCommandActivity", voice.androidAttribute("name"))
        assertEquals("true", voice.androidAttribute("exported"))
        assertNull(voice.androidAttributeOrNull("permission"))
        assertNull(voice.androidAttributeOrNull("label"))
        assertNull(voice.androidAttributeOrNull("icon"))
        assertEquals(ThreeStoveAgentPlugin.SERVICE_PROCESS, voice.androidAttribute("process"))
        assertEquals("@android:style/Theme.NoDisplay", voice.androidAttribute("theme"))
        assertEquals("true", voice.androidAttribute("excludeFromRecents"))
        assertEquals("true", voice.androidAttribute("noHistory"))
        val voiceFilter = voice.child("intent-filter")
        assertEquals(listOf(ThreeStoveAgentPlugin.VOICE_COMMAND_ACTION), voiceFilter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.category.DEFAULT"), voiceFilter.children("category").map { it.androidAttribute("name") })
        assertTrue(voiceFilter.children("data").isEmpty())
        for (settings in activities.drop(4)) {
        assertEquals("false", settings.androidAttribute("exported"))
        assertNull(settings.androidAttributeOrNull("process"))
        assertTrue(settings.children("intent-filter").isEmpty())
        }

        val receivers = manifest.child("application").children("receiver")
        assertEquals(listOf(".ui.LauncherIconUpdateReceiver", ".speech.ResidentResumeReceiver"), receivers.map { it.androidAttribute("name") })
        for (receiver in receivers) assertEquals("false", receiver.androidAttribute("exported"))
        assertEquals(listOf("android.intent.action.MY_PACKAGE_REPLACED"), receivers[0].child("intent-filter").children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.action.BOOT_COMPLETED", "android.intent.action.MY_PACKAGE_REPLACED"),
            receivers[1].child("intent-filter").children("action").map { it.androidAttribute("name") })
        assertTrue(manifest.child("application").children("provider").isEmpty())
    }

    @Test
    fun `info service and agent service match the identity constants`() {
        val services = manifest.child("application").children("service").associateBy { it.androidAttribute("name") }
        assertEquals(setOf(".ThreeStoveAgentPluginInfoService", ".ThreeStoveAgentPluginService", ".service.AgentLocalService",
            ".ThreeStoveAgentTaskForegroundService", ".speech.ResidentListenerService"), services.keys)
        for (name in listOf(".service.AgentLocalService", ".ThreeStoveAgentTaskForegroundService")) {
            assertEquals("false", services.getValue(name).androidAttribute("exported"))
            assertEquals(":agent", services.getValue(name).androidAttribute("process"))
        }
        assertEquals("specialUse", services.getValue(".ThreeStoveAgentTaskForegroundService").androidAttribute("foregroundServiceType"))

        val info = services.getValue(".ThreeStoveAgentPluginInfoService")
        assertDiscoveryContract(info, ThreeStoveAgentPlugin.INFO_ACTION)
        assertNull(info.androidAttributeOrNull("process"))

        val agent = services.getValue(".ThreeStoveAgentPluginService")
        assertDiscoveryContract(agent, ThreeStoveAgentPlugin.SERVICE_ACTION)
        assertEquals(ThreeStoveAgentPlugin.SERVICE_PROCESS, agent.androidAttribute("process"))

        // Resident listener (D66, D73): the component state is the switch, so the Manifest keeps it disabled.
        val resident = services.getValue(".speech.ResidentListenerService")
        assertEquals("false", resident.androidAttribute("enabled"))
        assertEquals("true", resident.androidAttribute("exported"))
        assertEquals(PLUGIN_PERMISSION, resident.androidAttribute("permission"))
        assertEquals(ThreeStoveAgentPlugin.SERVICE_PROCESS, resident.androidAttribute("process"))
        assertEquals("specialUse", resident.androidAttribute("foregroundServiceType"))
        val residentFilter = resident.child("intent-filter")
        assertEquals(listOf(ThreeStoveAgentPlugin.RESIDENT_LISTENER_ACTION), residentFilter.children("action").map { it.androidAttribute("name") })
        assertTrue(residentFilter.children("category").isEmpty())
        val residentMeta = resident.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(ThreeStoveAgentPlugin.PACKAGE_NAME + ".speech.ResidentListenerSettings", residentMeta[ThreeStoveAgentPlugin.RESIDENT_LISTENER_SETTINGS])
        assertTrue(resident.children("property").single().androidAttribute("value").isNotBlank())
    }

    private fun assertDiscoveryContract(service: Element, action: String) {
        assertEquals("true", service.androidAttribute("exported"))
        assertEquals("true", service.androidAttribute("enabled"))
        assertEquals(PLUGIN_PERMISSION, service.androidAttribute("permission"))
        val filter = service.child("intent-filter")
        assertEquals(listOf(action), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf(ThreeStoveAgentPlugin.SERVICE_CATEGORY), filter.children("category").map { it.androidAttribute("name") })
        val metaData = service.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(ThreeStoveAgentPlugin.REQUIRED_HOST_VERSION.toString(), metaData["requiresHostVersion"])
    }

    @Test
    fun `only discovery activation and draft entry components are exported`() {
        val expected = mapOf(".WakeActivity" to PLUGIN_PERMISSION, ".ui.LauncherActivity" to null,
            ".ui.ShareTargetActivity" to null, ".ui.VoiceCommandActivity" to null, ".ThreeStoveAgentPluginInfoService" to PLUGIN_PERMISSION,
            ".speech.ResidentListenerSettings" to PLUGIN_PERMISSION, ".speech.ResidentListenerService" to PLUGIN_PERMISSION,
            ".ThreeStoveAgentPluginService" to PLUGIN_PERMISSION) +
            listOf("AdaptiveLight", "AdaptiveDark", "AdaptiveAuto", "Transparent").associate { ".launcher.${it}IconAlias" to null }
        val components = listOf("activity", "activity-alias", "service", "receiver", "provider")
            .flatMap { manifest.child("application").children(it) }
        val exported = components.filter { it.androidAttribute("exported") == "true" }
        assertEquals(expected, exported.associate { it.androidAttribute("name") to it.androidAttributeOrNull("permission") })
        components.filterNot { it in exported }.forEach { assertEquals("false", it.androidAttribute("exported")) }
        assertTrue(manifest.child("application").children("receiver").all { it.androidAttribute("exported") == "false" })
        assertTrue(manifest.child("application").children("provider").isEmpty())
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
