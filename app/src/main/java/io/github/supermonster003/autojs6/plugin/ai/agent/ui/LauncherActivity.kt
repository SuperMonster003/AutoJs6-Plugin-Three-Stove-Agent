package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.Activity
import android.app.PendingIntent
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import io.github.supermonster003.autojs6.plugin.ai.agent.AiAgentPlugin
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.service.AgentLocalService
import org.autojs.plugin.ai.agent.api.*
import com.google.gson.JsonParser
import java.util.UUID
import java.util.concurrent.Executors
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptRoots

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
    private var dialog: AlertDialog? = null
    private var shownRequest: String? = null
    private val scriptRoots by lazy { ScriptRootSettings(this) }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) { link = IAiAgentLink.Stub.asInterface(binder); poll() }
        override fun onServiceDisconnected(name: ComponentName?) { link = null; show(R.string.launcher_link_timeout) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        findViewById<TextView>(R.id.launcher_host_status).text = hostStatusText()
        findViewById<Button>(R.id.launcher_script_roots).setOnClickListener { startActivity(Intent(this, ScriptRootsActivity::class.java)) }
        findViewById<Button>(R.id.launcher_connect).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            requestAttachment()
        }
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
        dialog?.dismiss(); dialog = null; shownRequest = null
        if (bound) unbindService(connection)
        bound = false; link = null
        super.onStop()
    }
    override fun onDestroy() { worker.shutdown(); super.onDestroy() }

    private fun identify(intent: Intent) {
        val identity = PendingIntent.getActivity(this, 0, Intent(this, LauncherActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        intent.putExtra(AiAgentActions.EXTRA_ATTACH_IDENTITY, identity)
        intent.putExtra("requestId", requestId ?: UUID.randomUUID().toString())
        if (scriptRoots.configured) intent.putExtra(AiAgentContract.KEY_LINK_CONFIG_JSON, ScriptRoots.configuration(scriptRoots.read()))
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
            val runId = status?.get("runningRunId")?.takeUnless { it.isJsonNull }?.asString
            val pending = if (runId == null) null else runCatching {
                val reference = Bundle().apply { putInt(AiAgentContract.KEY_CONTRACT_VERSION, AiAgentContract.CONTRACT_VERSION)
                    putString(AiAgentContract.KEY_RUN_REF_JSON, com.google.gson.JsonObject().apply { addProperty("runId", runId); addProperty("limit", 1) }.toString()) }
                JsonParser.parseString(current.getRun(reference).getString(AiAgentContract.KEY_RUN_RESPONSE_JSON)).asJsonObject.getAsJsonObject("pending")
            }.getOrNull()
            main.post {
                if (!visible || current !== link || expectedGeneration != generation) return@post
                polling = false
                val acceptedRoots = runCatching { status?.getAsJsonArray("scriptRoots")?.map { it.asString }?.toSet() }.getOrNull()
                val settingsAccepted = !scriptRoots.configured || acceptedRoots == scriptRoots.read()
                if (state == AiAgentContract.LINK_STATE_ATTACHED && settingsAccepted) { deadline = 0; requested = false; show(R.string.launcher_link_attached) }
                else if (!requested) requestAttachment()
                else if (deadline > 0 && SystemClock.elapsedRealtime() >= deadline) {
                    deadline = 0; show(if (state == AiAgentContract.LINK_STATE_ATTACHED && !settingsAccepted) R.string.script_roots_rejected else R.string.launcher_link_timeout)
                }
                showPending(current, runId, pending)
                if (visible) main.postDelayed({ poll() }, 500)
            }
        }
    }

    private fun showPending(current: IAiAgentLink, runId: String?, pending: com.google.gson.JsonObject?) {
        val request = pending?.get("requestId")?.asString
        if (request == shownRequest) return
        dialog?.dismiss(); dialog = null; shownRequest = request
        if (pending == null || request == null || runId == null || pending.get("submitted")?.asBoolean == true) return
        fun reply(value: com.google.gson.JsonElement?, allowed: Boolean? = null) {
            val body = com.google.gson.JsonObject().apply {
                addProperty("runId", runId); addProperty("requestId", request)
                if (allowed != null) { addProperty("allowed", allowed); addProperty("scope", "once") } else add("value", value)
            }
            worker.execute { runCatching { current.respond(Bundle().apply {
                putInt(AiAgentContract.KEY_CONTRACT_VERSION, AiAgentContract.CONTRACT_VERSION)
                putString(AiAgentContract.KEY_RUN_RESPONSE_JSON, body.toString())
            }) } }
        }
        val builder = AlertDialog.Builder(this).setTitle(R.string.task_reply)
        if (pending.get("type").asString == "confirmation") {
            if (pending.get("tool")?.asString == "script_run" && pending.getAsJsonObject("arguments")?.get("parameters")?.isJsonObject == true)
                builder.setView(ScriptConfirmationView.create(this, pending))
            else builder.setMessage(pending.get("description").asString + "\n" + pending.get("arguments").toString())
            builder.setPositiveButton(R.string.task_allow) { _, _ -> reply(null, true) }
                .setNegativeButton(R.string.task_deny) { _, _ -> reply(null, false) }
        } else {
            when (pending.get("kind").asString) {
                "choice" -> builder.setTitle(pending.get("question").asString).setItems(pending.getAsJsonArray("choices").map { it.asString }.toTypedArray()) { _, index ->
                    reply(pending.getAsJsonArray("choices")[index])
                }
                "confirm" -> builder.setMessage(pending.get("question").asString)
                    .setPositiveButton(R.string.task_allow) { _, _ -> reply(com.google.gson.JsonPrimitive(true)) }
                    .setNegativeButton(R.string.task_deny) { _, _ -> reply(com.google.gson.JsonPrimitive(false)) }
                else -> {
                    val field = EditText(this).apply { maxLines = 4; filters = arrayOf(android.text.InputFilter.LengthFilter(1000)) }
                    builder.setMessage(pending.get("question").asString).setView(field)
                        .setPositiveButton(R.string.task_reply) { _, _ ->
                            if (field.text.isNotBlank()) reply(com.google.gson.JsonPrimitive(field.text.toString())) else shownRequest = null
                        }
                }
            }
        }
        dialog = builder.setOnCancelListener { shownRequest = null }.show()
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
