package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.widget.Button
import android.widget.TextView
import io.github.supermonster003.autojs6.plugin.ai.agent.AiAgentPlugin
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.service.AgentLocalService
import org.autojs.plugin.ai.agent.api.*
import com.google.gson.JsonParser
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Standalone entry of the plugin (roadmap D2 / D11). The task workbench of roadmap P6 grows out
 * of this screen. A private service reports attachment across the main / :agent process boundary.
 */
class LauncherActivity : Activity() {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var link: IAiAgentLink? = null
    private var bound = false
    private var visible = false
    private var polling = false
    private var generation = 0
    private var deadline = 0L
    private var requestId: String? = null
    private var requested = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) { link = IAiAgentLink.Stub.asInterface(binder); poll() }
        override fun onServiceDisconnected(name: ComponentName?) { link = null; show(R.string.launcher_link_timeout) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        findViewById<TextView>(R.id.launcher_host_status).text = hostStatusText()
        findViewById<Button>(R.id.launcher_connect).setOnClickListener { requestAttachment() }
        findViewById<Button>(R.id.launcher_open_host).setOnClickListener {
            packageManager.getLaunchIntentForPackage(AiAgentPlugin.HOST_PACKAGE_NAME)?.let { intent ->
                intent.putExtra(AiAgentActions.EXTRA_AI_AGENT_ATTACH, true)
                identify(intent)
                runCatching { startActivity(intent) }
            }
        }
    }

    override fun onStart() {
        super.onStart(); visible = true; requested = false; generation++
        bound = bindService(Intent(this, AgentLocalService::class.java), connection, Context.BIND_AUTO_CREATE)
    }
    override fun onStop() {
        visible = false; polling = false; generation++; main.removeCallbacksAndMessages(null)
        if (bound) unbindService(connection)
        bound = false; link = null
        super.onStop()
    }
    override fun onDestroy() { worker.shutdown(); super.onDestroy() }

    private fun identify(intent: Intent) {
        val identity = PendingIntent.getActivity(this, 0, Intent(this, LauncherActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        intent.putExtra(AiAgentActions.EXTRA_ATTACH_IDENTITY, identity)
        intent.putExtra("requestId", requestId ?: UUID.randomUUID().toString())
    }
    private fun requestAttachment() {
        if (classifyHostPresence(readHostPackage(), AiAgentPlugin.REQUIRED_HOST_VERSION) != HostPresence.READY) return
        requested = true; requestId = UUID.randomUUID().toString(); deadline = SystemClock.elapsedRealtime() + 15_000
        show(R.string.launcher_link_connecting)
        val intent = Intent(AiAgentActions.ACTION_ATTACH_REQUEST).setPackage(AiAgentPlugin.HOST_PACKAGE_NAME).addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        identify(intent)
        sendBroadcast(intent, AiAgentActions.PLUGIN_PERMISSION)
        poll()
    }
    private fun show(resource: Int) { if (visible) findViewById<TextView>(R.id.launcher_host_status).setText(resource) }
    private fun poll() {
        if (!visible || polling) return
        val current = link ?: return
        val expectedGeneration = generation
        polling = true
        worker.execute {
            val status = runCatching { JsonParser.parseString(current.status.getString(AiAgentContract.KEY_STATUS_JSON)).asJsonObject }.getOrNull()
            val state = status?.get("state")?.asString
            main.post {
                if (!visible || current !== link || expectedGeneration != generation) return@post
                polling = false
                if (state == AiAgentContract.LINK_STATE_ATTACHED) { deadline = 0; requested = false; show(R.string.launcher_link_attached) }
                else if (!requested) requestAttachment()
                else if (deadline > 0 && SystemClock.elapsedRealtime() >= deadline) { deadline = 0; show(R.string.launcher_link_timeout) }
                if (visible) main.postDelayed({ poll() }, 500)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.launcher_host_status).text = hostStatusText()
    }

    private fun hostStatusText(): CharSequence {
        val snapshot = readHostPackage()
        val required = AiAgentPlugin.REQUIRED_HOST_VERSION
        return when (classifyHostPresence(snapshot, required)) {
            HostPresence.MISSING -> getString(R.string.launcher_host_missing, required)
            HostPresence.DISABLED -> getString(R.string.launcher_host_disabled)
            HostPresence.INCOMPATIBLE -> getString(R.string.launcher_host_incompatible, snapshot!!.versionCode, required)
            HostPresence.READY -> getString(R.string.launcher_host_ready, snapshot!!.versionName, snapshot.versionCode)
        }
    }

    private fun readHostPackage(): HostPackageSnapshot? {
        val packageInfo = try {
            packageManager.getPackageInfo(AiAgentPlugin.HOST_PACKAGE_NAME, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        return HostPackageSnapshot(
            enabled = packageInfo.applicationInfo?.enabled ?: false,
            versionCode = versionCode,
            versionName = packageInfo.versionName.orEmpty(),
        )
    }
}
