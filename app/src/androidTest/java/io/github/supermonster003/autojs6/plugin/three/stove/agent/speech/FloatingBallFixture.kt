package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.json
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.jsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.AgentLocalService
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.AgentWire
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.IAgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.IPresetStoreCallback
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.SettingsEndpoint
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.AgentConnection
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.HostAppearance
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import org.autojs.plugin.host.capability.api.IHostCapabilityBroker
import org.autojs.plugin.host.capability.api.IHostCapabilityCallback
import org.autojs.plugin.three.stove.agent.api.IThreeStoveAgentLink
import org.autojs.plugin.three.stove.agent.api.IThreeStoveAgentLinkCallback
import org.autojs.plugin.three.stove.agent.api.IThreeStoveAgentModelBroker
import org.autojs.plugin.three.stove.agent.api.IThreeStoveAgentModelCallback
import org.autojs.plugin.three.stove.agent.api.IThreeStoveAgentPlugin
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * The real floating ball of the :agent process for the voice draft tests (roadmap D62, D72, D77): a fixture link attached
 * through the debug-only `WorkbenchFixtureService` (no host, no model call, no device action), the floating setting on and
 * the overlay permission. The permission is used as granted; only with `grantOverlay` (`-e agent.resident.overlay appops`)
 * an ungranted one is allowed through appops and restored afterwards, as the floating tests of `WorkbenchActivityTest` do.
 * The card is read through accessibility. Every change is restored, and :agent is ended at the end so that it rereads
 * the ball's draft file instead of keeping the fixture drafts in memory.
 */
internal class FloatingBallFixture private constructor(private val agentPid: Int) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val automation = instrumentation.uiAutomation
    /** Calls that would start a task; the voice entries only open drafts. */
    val modelCalls = AtomicInteger()

    /** The language of the ball's own window ([AppearancePreferences], as the ball resolves it). */
    fun text(id: Int): String = AppearancePreferences.resolve(context, HostAppearance.read(context)).wrap(context).getString(id)

    fun ballShown() = window(BALL) != null
    fun cardShown() = window(CARD) != null
    /**
     * The text of the expanded card's field (never its hint), null while the card is not on screen. Below API 26 the
     * Material text field announces "text, hint", or the hint alone when empty.
     */
    fun cardText(): String? = overlay()?.findAccessibilityNodeInfosByViewId("${context.packageName}:id/workbench_goal")?.firstOrNull()?.let { node ->
        if (Build.VERSION.SDK_INT >= 26) return@let if (node.isShowingHintText) "" else node.text?.toString()
        val hint = text(R.string.workbench_goal_hint)
        node.text?.toString()?.let { if (it == hint) "" else it.removeSuffix(", $hint") }
    }
    fun cardShows(text: String) = overlay()?.findAccessibilityNodeInfosByText(text)?.isNotEmpty() == true

    /** The one overlay window of this app (ball or card), matched like the floating tests of `WorkbenchActivityTest`. */
    private fun overlay(): AccessibilityNodeInfo? = automation.windows.firstOrNull {
        // TYPE_PHONE windows are reported as application windows on API 24/25.
        it.root?.packageName == context.packageName && (it.title == CARD || it.type == AccessibilityWindowInfo.TYPE_SYSTEM)
    }?.root ?: automation.rootInActiveWindow?.takeIf { it.packageName == context.packageName }

    /** The WindowManager entry of a drawn and visible overlay window, null otherwise (the checks of `WorkbenchActivityTest`). */
    private fun window(title: String): String? = shell("dumpsys window windows").lineSequence()
        .dropWhile { !it.contains("Window #") || !it.contains(title) }.drop(1).takeWhile { !it.contains("Window #") }.joinToString("\n")
        .takeIf {
            (it.contains("isOnScreen=true") && it.contains("isVisible=true") ||
                Build.VERSION.SDK_INT <= 25 && it.contains("isReadyForDisplay()=true") && it.contains("Surface: shown=true")) && it.contains("HAS_DRAWN")
        }

    private inner class Model : IThreeStoveAgentModelBroker.Stub() {
        override fun getBrokerInfo() = AgentWire.envelope(C.KEY_MODEL_BROKER_INFO_JSON,
            """{"available":true,"providerId":"voice-fixture","maximumInputBytes":131072,"maximumOutputBytes":65536,"maximumResponseSchemaBytes":16384}""").apply {
            putString(H.KEY_GRANT_JSON, """{"maxInputBytesPerRequest":131072,"maxTotalTokens":1000000,"consumedTokens":0}""")
        }
        override fun listTargets(request: Bundle, callback: IThreeStoveAgentModelCallback) {
            val id = JSONObject(request.getString(C.KEY_MODEL_REQUEST_JSON)!!).getString("requestId")
            fun emit(type: String, sequence: Int, data: JSONObject = JSONObject()) =
                callback.onEvent(AgentWire.envelope(C.KEY_MODEL_EVENT_JSON, data.put("requestId", id).put("type", type).put("sequence", sequence).toString()))
            emit("started", 1)
            emit("completed", 2, JSONObject().put("targets", org.json.JSONArray("""[{"targetId":"voice:fixture","displayName":"Voice fixture model","locality":2,"configured":true,"available":true,"maximumContextBytes":131072,"capabilityIds":[],"supportedControls":[]}]""")))
        }
        override fun generate(request: Bundle?, callback: IThreeStoveAgentModelCallback?) { modelCalls.incrementAndGet() }
        override fun cancel(reference: Bundle?) = Unit
        override fun submitToolResults(request: Bundle?) = Unit
        override fun destroy(reason: Bundle?) = Unit
    }

    private val capabilities = object : IHostCapabilityBroker.Stub() {
        override fun getBrokerInfo() = Bundle().apply {
            putInt(H.KEY_CONTRACT_VERSION, H.CONTRACT_VERSION)
            putStringArray(H.KEY_GRANT_METHODS, emptyArray()); putStringArray(H.KEY_GRANT_PERMISSIONS, emptyArray())
            putInt(H.KEY_GRANT_MAX_REQUEST_BYTES, 32768); putLong(H.KEY_GRANT_MAX_TIMEOUT_MS, 30000)
        }
        override fun dispatch(request: Bundle?, callback: IHostCapabilityCallback?) { error("The voice fixture never operates the device") }
        override fun destroy(reason: Bundle?) = Unit
    }

    companion object {
        private const val BALL = "3-Stove Agent floating ball"
        private const val CARD = "3-Stove Agent floating card"

        private fun shell(command: String) = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
            .let(ParcelFileDescriptor::AutoCloseInputStream).bufferedReader().use { it.readText() }

        /** Runs [block] with the overlay permission allowed through appops when [grant] and it is missing, then restores the mode. */
        fun <T> withOverlay(grant: Boolean, block: () -> T): T {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            if (!grant || Settings.canDrawOverlays(context)) return block()
            val mode = Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(shell("appops get ${context.packageName} SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
            shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
            try { return block() } finally { shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW $mode") }
        }

        /** Runs [block] with the ball on screen; skips (assumption) when the overlay permission is missing or the screen is locked. */
        fun <T> run(grantOverlay: Boolean, block: (FloatingBallFixture) -> T): T = withOverlay(grantOverlay) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val automation = instrumentation.uiAutomation
            val power = context.getSystemService(PowerManager::class.java); val keyguard = context.getSystemService(KeyguardManager::class.java)
            assumeTrue("The screen is off or locked", power.isInteractive && !keyguard.isKeyguardLocked)
            assumeTrue("The overlay permission is not granted (pass --grant-overlay to allow it for the test)", Settings.canDrawOverlays(context))
            val connections = ArrayList<ServiceConnection>()
            fun bind(intent: Intent): IBinder {
                val latch = CountDownLatch(1); var binder: IBinder? = null
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) { binder = service; latch.countDown() }
                    override fun onServiceDisconnected(name: ComponentName?) = Unit
                }
                assertTrue(context.bindService(intent, connection, Context.BIND_AUTO_CREATE)); connections += connection
                assertTrue("Bound ${intent.component?.shortClassName}", latch.await(15, TimeUnit.SECONDS))
                return checkNotNull(binder)
            }
            val settings = IAgentSettings.Stub.asInterface(bind(Intent(context, AgentLocalService::class.java).setAction(SettingsEndpoint.ACTION)))
            fun query(body: JsonObject): JsonObject {
                val latch = CountDownLatch(1); var response: Bundle? = null
                settings.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, body), object : IPresetStoreCallback.Stub() {
                    override fun onResult(result: Bundle?) { response = result; latch.countDown() }
                })
                assertTrue(latch.await(15, TimeUnit.SECONDS)); return AgentConnection.decode(response!!)
            }
            val prefs = context.getSharedPreferences("floating", Context.MODE_PRIVATE)
            val oldDraft = prefs.all
            var saved: JsonObject? = null
            var link: IThreeStoveAgentLink? = null
            var fixture: FloatingBallFixture? = null
            val flags = automation.serviceInfo.flags
            try {
                val plugin = IThreeStoveAgentPlugin.Stub.asInterface(bind(Intent().setClassName(context, context.packageName + ".service.WorkbenchFixtureService")))
                fixture = FloatingBallFixture(plugin.capabilities.getInt("fixture.pid"))
                link = plugin.attach(AgentWire.envelope(C.KEY_LINK_CONFIG_JSON, """{"grantSummary":{"toolGroups":["observe"]}}"""), fixture.Model(), fixture.capabilities,
                    object : IThreeStoveAgentLinkCallback.Stub() { override fun onStatus(status: Bundle?) = Unit; override fun onEvent(event: Bundle?) = Unit })
                saved = query(jsonObject("operation" to "get".json())).getAsJsonObject("settings").deepCopy()
                prefs.edit().clear().commit()
                query(jsonObject("operation" to "save".json(), "settings" to saved.deepCopy().apply { addProperty("floating", true) }))
                automation.serviceInfo = automation.serviceInfo.apply {
                    this.flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                }
                val end = SystemClock.elapsedRealtime() + 15_000
                while (!fixture.ballShown() && !fixture.cardShown() && SystemClock.elapsedRealtime() < end) SystemClock.sleep(150)
                assertTrue("The floating ball did not appear", fixture.ballShown() || fixture.cardShown())
                block(fixture)
            } finally {
                automation.serviceInfo = automation.serviceInfo.apply { this.flags = flags }
                runCatching { link?.detach(AgentWire.envelope(H.KEY_REASON_JSON, """{"reason":"voice-fixture-finished"}""")) }
                saved?.let { runCatching { query(jsonObject("operation" to "save".json(), "settings" to it)) } }
                connections.forEach { runCatching { context.unbindService(it) } }
                val editor = prefs.edit().clear()
                oldDraft.forEach { (key, value) -> when (value) { is String -> editor.putString(key, value); is Float -> editor.putFloat(key, value) } }
                editor.commit()
                fixture?.agentPid?.takeIf { it > 0 && it != Process.myPid() }?.let(Process::killProcess)
            }
        }
    }
}
