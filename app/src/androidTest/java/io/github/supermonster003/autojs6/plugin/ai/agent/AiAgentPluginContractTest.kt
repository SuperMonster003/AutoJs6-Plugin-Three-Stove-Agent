package io.github.supermonster003.autojs6.plugin.ai.agent

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.LauncherActivity
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.ai.agent.api.*
import org.junit.Assert.assertThrows
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Verifies the host-facing activation and discovery contract against the installed APK:
 * the Wake Activity, the INFO service (with a real `getInfo()` round trip), the
 * `org.autojs.plugin.AI_AGENT` service living in its own process with the placeholder Binder
 * descriptor (roadmap P0.1), and the launcher entry.
 */
@RunWith(AndroidJUnit4::class)
class AiAgentPluginContractTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val packageName: String
        get() = context.packageName

    @Test
    fun wakeActivityFollowsTheHostActivationContract() {
        val applicationInfo = context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val wakeActivity = applicationInfo.metaData?.getString(WAKE_ACTIVITY_META_DATA)
        assertEquals(".WakeActivity", wakeActivity)
        assertEquals(context.getString(R.string.plugin_author), applicationInfo.metaData?.getString(AUTHOR_META_DATA))
        assertEquals(0, applicationInfo.metaData?.getInt(NATIVE_PAGE_ALIGNMENT_META_DATA, -1))

        val component = ComponentName(packageName, packageName + wakeActivity)
        val activityInfo = context.packageManager.getActivityInfo(component, 0)
        assertTrue("Wake Activity must be exported", activityInfo.exported)
        assertTrue("Wake Activity must be enabled", activityInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, activityInfo.permission)
        assertEquals(android.R.style.Theme_NoDisplay, activityInfo.theme)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH != 0)

        val wakeIntent = Intent(WAKE_ACTION).addCategory(Intent.CATEGORY_DEFAULT).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentActivities(wakeIntent, 0)
        assertEquals("The WAKE action must resolve to exactly one activity", 1, matches.size)
        assertEquals(component.className, matches.single().activityInfo.name)
    }

    @Test
    fun launcherActivityIsTheOnlyLauncherEntry() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentActivities(launcherIntent, 0)
        assertEquals("Exactly one launcher activity is expected", 1, matches.size)
        val activityInfo = matches.single().activityInfo
        assertEquals(LauncherActivity::class.java.name, activityInfo.name)
        assertTrue(activityInfo.exported)
        assertNull(activityInfo.permission)
        assertEquals(packageName, activityInfo.processName)
    }

    @Test
    fun infoServiceIsDiscoverableAndReportsPluginInfo() {
        val serviceInfo = discoverSingleService(
            AiAgentPlugin.INFO_ACTION,
            AiAgentPluginInfoService::class.java.name,
        )
        assertEquals(packageName, serviceInfo.processName)

        withBoundService(serviceInfo) { binder ->
            assertEquals(IPluginInfoProvider.DESCRIPTOR, binder.interfaceDescriptor)
            val info = IPluginInfoProvider.Stub.asInterface(binder).info
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val expectedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            assertEquals("AI Agent", info.name)
            assertEquals(context.getString(R.string.app_name), info.name)
            assertEquals(context.getString(R.string.plugin_description), info.description)
            assertTrue("instruction must be read from the raw resource", info.instruction?.isNotBlank() == true)
            assertEquals(AiAgentPlugin.AUTHOR, info.author)
            assertEquals(context.getString(R.string.plugin_author), info.author)
            assertEquals(AiAgentPlugin.ID, info.id)
            assertEquals(context.getString(R.string.plugin_id), info.id)
            assertEquals(AiAgentPlugin.ENGINE, info.engine)
            assertEquals(context.getString(R.string.plugin_engine), info.engine)
            assertEquals(AiAgentPlugin.VARIANT, info.variant)
            assertEquals(context.getString(R.string.plugin_variant), info.variant)
            assertEquals(packageInfo.versionName, info.versionName)
            assertEquals(expectedVersionCode, info.versionCode)
            assertEquals(context.getString(R.string.plugin_version_date), info.versionDate)
            assertTrue(info.versionDate?.isNotBlank() == true)
            // Explicit empty array: no ABI restriction, as opposed to a null (unspecified) value.
            assertArrayEquals(emptyArray<String>(), info.supportedAbis)
            assertCapabilities(requireNotNull(info.capabilities))
        }
    }

    @Test
    fun agentServiceExposesRealInfoAndRejectsPluginUidAsHost() {
        val serviceInfo = discoverSingleService(
            AiAgentPlugin.SERVICE_ACTION,
            AiAgentPluginService::class.java.name,
        )
        assertEquals(packageName + AiAgentPlugin.SERVICE_PROCESS, serviceInfo.processName)

        withBoundService(serviceInfo) { binder ->
            assertEquals(AiAgentPlugin.SERVICE_DESCRIPTOR, binder.interfaceDescriptor)
            assertTrue(binder.isBinderAlive)
            assertTrue(binder.pingBinder())
            val api = IAiAgentPlugin.Stub.asInterface(binder)
            assertCapabilities(api.capabilities)
            assertEquals(AiAgentPlugin.ID, api.info.id)
            assertThrows(SecurityException::class.java) { api.attach(Bundle(), null, null, null) }
            // The service lives in :agent, not the instrumentation process.
            assertNull(binder.queryLocalInterface(AiAgentPlugin.SERVICE_DESCRIPTOR))
        }
    }

    private fun assertCapabilities(capabilities: Bundle) {
        assertEquals(AiAgentPlugin.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
        assertEquals(setOf(PluginCapabilityKeys.REQUIRES_HOST_VERSION, AiAgentCapabilityKeys.CONTRACT_VERSION,
            AiAgentCapabilityKeys.TOOL_GROUPS, AiAgentCapabilityKeys.FEATURES), capabilities.keySet())
        assertEquals(AiAgentContract.CONTRACT_VERSION, capabilities.getInt(AiAgentCapabilityKeys.CONTRACT_VERSION))
        assertArrayEquals(arrayOf(AiAgentCapabilityKeys.FEATURE_STRUCTURED_JSON_LOOP), capabilities.getStringArray(AiAgentCapabilityKeys.FEATURES))
    }

    private fun discoverSingleService(action: String, expectedClassName: String): ServiceInfo {
        val discoveryIntent = Intent(action)
            .addCategory(AiAgentPlugin.SERVICE_CATEGORY)
            .setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentServices(discoveryIntent, PackageManager.GET_META_DATA)
        assertEquals("The discovery contract for $action must resolve exactly one service", 1, matches.size)

        val serviceInfo = matches.single().serviceInfo
        assertEquals(packageName, serviceInfo.packageName)
        assertEquals(expectedClassName, serviceInfo.name)
        assertTrue("$expectedClassName must be exported", serviceInfo.exported)
        assertTrue("$expectedClassName must be enabled", serviceInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, serviceInfo.permission)
        val requiresHostVersion = requireNotNull(serviceInfo.metaData) { "requiresHostVersion meta-data is missing" }
            .getInt(REQUIRES_HOST_VERSION_META_DATA)
        assertEquals(AiAgentPlugin.REQUIRED_HOST_VERSION, requiresHostVersion.toLong())
        return serviceInfo
    }

    private fun withBoundService(serviceInfo: ServiceInfo, block: (IBinder) -> Unit) {
        val binderReference = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binderReference.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit

            override fun onNullBinding(name: ComponentName) {
                connected.countDown()
            }
        }

        val explicitIntent = Intent().setComponent(ComponentName(serviceInfo.packageName, serviceInfo.name))
        assertTrue("bindService returned false", context.bindService(explicitIntent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("Timed out waiting for the Binder service", connected.await(10, TimeUnit.SECONDS))
            val binder = binderReference.get()
            assertNotNull("The service returned a null Binder", binder)
            block(binder)
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val WAKE_ACTION = "org.autojs.plugin.action.WAKE"
        const val WAKE_ACTIVITY_META_DATA = "org.autojs.plugin.WAKE_ACTIVITY"
        const val AUTHOR_META_DATA = "org.autojs.plugin.info.AUTHOR"
        const val NATIVE_PAGE_ALIGNMENT_META_DATA = "org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT"
        const val REQUIRES_HOST_VERSION_META_DATA = "requiresHostVersion"
    }
}
