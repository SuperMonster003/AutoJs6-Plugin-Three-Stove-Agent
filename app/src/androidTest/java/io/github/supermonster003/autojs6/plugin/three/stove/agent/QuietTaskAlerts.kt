package io.github.supermonster003.autojs6.plugin.three.stove.agent

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.AgentLocalService
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.IAgentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.IPresetStoreCallback
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.SettingsEndpoint
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.AgentConnection
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Silences task alerts for the duration of the instrumentation suite on emulators.
 *
 * Since build 179 a finished task raises its alerts by default: failures open a dialog activity
 * (always on API 24/25, with the overlay permission on Android 10+) plus a toast and a notification,
 * completions a toast and a notification. Fixture tasks end all through the suite and their alert
 * notifications are never cancelled; once four of them sit in the shade the system adds an auto-group
 * summary whose content intent is the app's launcher entry. The notification-shade test then taps that
 * summary instead of the interaction row, the system launches the workbench through the enabled icon
 * alias, and the confirmation never opens (remote CI build 184 and every local full run on API 24).
 * The suite therefore cancels the app's notifications, saves the plugin's settings once, stores them
 * with empty alert channels and restores the original JSON when the instrumentation finishes. Fixtures
 * that save settings of their own derive them from the current ones so the channels stay silent. Real
 * devices keep the maintainer's settings: nothing is written there, and `TaskAlertsAndroidTest` covers
 * the alert channels with its own isolated runtime.
 */
object QuietTaskAlerts {
    fun onEmulator(): Boolean = Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk", ignoreCase = true) ||
        Build.HARDWARE in setOf("ranchu", "goldfish")

    /** Returns the settings JSON that was in place, or null when the runtime could not be read. */
    fun silence(context: Context): JsonObject? = withEndpoint(context) { query ->
        val original = query(jsonObject("operation", "get")).getAsJsonObject("settings")
        val quiet = original.deepCopy().apply { add("failureAlerts", JsonArray()); add("completionAlerts", JsonArray()) }
        query(jsonObject("operation", "save").apply { add("settings", quiet) })
        original
    }

    fun restore(context: Context, original: JsonObject) {
        withEndpoint(context) { query -> query(jsonObject("operation", "save").apply { add("settings", original.deepCopy()) }) }
    }

    private fun jsonObject(key: String, value: String) = JsonObject().apply { addProperty(key, value) }

    private fun <T> withEndpoint(context: Context, block: ((JsonObject) -> JsonObject) -> T): T? {
        val connected = CountDownLatch(1)
        var endpoint: IAgentSettings? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) { endpoint = IAgentSettings.Stub.asInterface(service); connected.countDown() }
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        val intent = Intent(context, AgentLocalService::class.java).setAction(SettingsEndpoint.ACTION)
        if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) return null
        try {
            if (!connected.await(15, TimeUnit.SECONDS)) return null
            val settings = endpoint ?: return null
            return block { body ->
                val done = CountDownLatch(1)
                var response: Bundle? = null
                settings.query(AgentConnection.request(C.KEY_RUN_REQUEST_JSON, body), object : IPresetStoreCallback.Stub() {
                    override fun onResult(result: Bundle?) { response = result; done.countDown() }
                })
                check(done.await(15, TimeUnit.SECONDS)) { "Settings endpoint did not answer" }
                AgentConnection.decode(checkNotNull(response))
            }
        } finally { context.unbindService(connection) }
    }
}
