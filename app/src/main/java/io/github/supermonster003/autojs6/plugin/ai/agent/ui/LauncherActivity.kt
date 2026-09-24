package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognizerIntent
import android.text.*
import android.view.View
import android.widget.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.AiAgentPlugin
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptRoots
import io.github.supermonster003.autojs6.plugin.ai.agent.service.RunLauncher
import org.autojs.plugin.ai.agent.api.AiAgentActions
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import java.util.UUID

/** Standalone task workbench. Model/device work stays in the attached agent process. */
class LauncherActivity : HostAppearanceActivity() {
    private lateinit var agent: AgentConnection
    private lateinit var pending: PendingCard
    private lateinit var goal: EditText
    private lateinit var preset: Spinner
    private val scriptRoots by lazy { ScriptRootSettings(this) }
    private val drafts by lazy { getSharedPreferences("workbench", MODE_PRIVATE) }
    private var currentId: String? = null
    private var attached = false
    private var sending = false
    private var requested = false
    private var deadline = 0L
    private var requestId: String? = null
    private var presetIds = emptyList<String>()
    private var selectedPreset = "default"
    private var recentKey = ""
    internal var hostReader: () -> HostPackageSnapshot? = { readHostPackage() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        findViewById<View>(android.R.id.content).layoutDirection = resources.configuration.layoutDirection
        agent = AgentConnection(this, ::render)
        agent.selectedId = savedInstanceState?.getString("selectedId")
        selectedPreset = savedInstanceState?.getString("preset") ?: drafts.getString("preset", "default")!!
        goal = findViewById(R.id.workbench_goal)
        goal.setText(savedInstanceState?.getString("goal") ?: drafts.getString("goal", ""))
        goal.filters = arrayOf(InputFilter.LengthFilter(4096))
        goal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateSend() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        preset = findViewById(R.id.workbench_preset)
        preset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                presetIds.getOrNull(position)?.let { selectedPreset = it }
            }
        }
        pending = PendingCard(findViewById(R.id.workbench_pending)) { body, complete ->
            agent.command({ it.respond(AgentConnection.request(C.KEY_RUN_RESPONSE_JSON, body)) }) { result ->
                complete(result.isSuccess); if (result.isFailure) showError()
            }
        }
        pending.restore(savedInstanceState)
        findViewById<Button>(R.id.workbench_send).setOnClickListener { launchRun() }
        findViewById<Button>(R.id.workbench_stop).setOnClickListener {
            currentId?.let { id -> agent.command({ link ->
                link.cancelRun(AgentConnection.request(C.KEY_RUN_REF_JSON, jsonObject("runId" to id.json())))
                AgentConnection.request(C.KEY_RUN_RESPONSE_JSON)
            }) { if (it.isFailure) showError() } }
        }
        findViewById<Button>(R.id.workbench_details).setOnClickListener { currentId?.let(::openDetail) }
        findViewById<Button>(R.id.launcher_script_roots).setOnClickListener { startActivity(Intent(this, ScriptRootsActivity::class.java)) }
        findViewById<Button>(R.id.launcher_connect).setOnClickListener { requested = false; requestAttachment() }
        findViewById<Button>(R.id.launcher_open_host).setOnClickListener {
            packageManager.getLaunchIntentForPackage(AiAgentPlugin.HOST_PACKAGE_NAME)?.let { intent ->
                intent.putExtra(AiAgentActions.EXTRA_AI_AGENT_ATTACH, true); identify(intent)
                runCatching { startActivity(intent) }.onFailure { showError() }
            }
        }
        findViewById<Button>(R.id.workbench_voice).apply {
            visibility = if (speechIntent().resolveActivity(packageManager) == null) View.GONE else View.VISIBLE
            setOnClickListener { runCatching { startActivityForResult(speechIntent(), VOICE_REQUEST) }.onFailure { showError() } }
        }
        updateSend(); tint(findViewById(android.R.id.content))
    }
    override fun onStart() { super.onStart(); requested = false; sending = false; agent.start() }
    override fun onStop() {
        drafts.edit().putString("goal", goal.text.toString()).putString("preset", selectedPreset).apply()
        agent.stop(); super.onStop()
    }
    override fun onDestroy() { agent.close(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("goal", goal.text.toString()); outState.putString("preset", selectedPreset)
        outState.putString("selectedId", agent.selectedId)
        pending.save(outState)
        super.onSaveInstanceState(outState)
    }
    @Deprecated("Platform speech result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_REQUEST && resultCode == RESULT_OK) data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()?.let { goal.setText(AgentJson.truncate(it, 4096)); goal.setSelection(goal.length()) }
    }
    private fun speechIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, resources.configuration.locales[0].toLanguageTag())

    private fun launchRun() {
        if (!attached || sending) return
        val text = goal.text.toString().trim()
        val request = runCatching { RunLauncher.uiRequest(text, selectedPreset, resources.configuration.locales[0].toLanguageTag()) }.getOrNull()
        if (request == null) { goal.error = getString(R.string.workbench_goal_invalid); return }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        sending = true; updateSend()
        agent.command({ it.startRun(Bundle().apply {
            putInt(C.KEY_CONTRACT_VERSION, C.CONTRACT_VERSION); putString(C.KEY_RUN_REQUEST_JSON, request)
        }, null) }) { result ->
            sending = false
            result.onSuccess {
                agent.selectedId = it.string("runId")
                if (goal.text.toString().trim() == text) goal.setText("")
                drafts.edit().putString("goal", goal.text.toString()).apply()
                findViewById<TextView>(R.id.workbench_error).visibility = View.GONE
            }.onFailure { showError() }
            updateSend()
        }
    }
    private fun updateSend() {
        findViewById<Button>(R.id.workbench_send).isEnabled = attached && !sending && goal.text.isNotBlank()
    }
    private fun render(value: WorkbenchSnapshot) {
        renderLink(value.status)
        if (value.presets != presetIds) {
            presetIds = value.presets
            preset.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
                presetIds.map { if (it == "default") getString(R.string.workbench_default_preset) else it })
            preset.setSelection(presetIds.indexOf(selectedPreset).coerceAtLeast(0))
        }
        preset.isEnabled = attached && presetIds.isNotEmpty()
        val row = value.run
        currentId = row?.string("runId")
        findViewById<View>(R.id.workbench_current).visibility = if (row == null) View.GONE else View.VISIBLE
        if (row != null) {
            findViewById<TextView>(R.id.workbench_current_goal).text = row.string("goal")
            findViewById<TextView>(R.id.workbench_state).text = WorkbenchText.state(this, row)
            findViewById<TextView>(R.id.workbench_step).text = if (!WorkbenchText.active(row)) WorkbenchText.summary(row) else
                row.string("progress") ?: row.getAsJsonArray("steps")?.lastOrNull()?.asJsonObject?.let {
                    it.getAsJsonObject("decision")?.string("reasoning") ?: it.string("tool")
                }.orEmpty()
            findViewById<TextView>(R.id.workbench_budget).text = WorkbenchText.budget(this, row)
            findViewById<ProgressBar>(R.id.workbench_progress).apply {
                max = row.getAsJsonObject("budget")?.number("maxSteps")?.toInt() ?: 40
                progress = (row.number("step") ?: 0).toInt()
                contentDescription = WorkbenchText.budget(this@LauncherActivity, row)
            }
            findViewById<Button>(R.id.workbench_stop).visibility = if (WorkbenchText.active(row)) View.VISIBLE else View.GONE
        }
        pending.render(row)
        val key = value.runs.toString()
        if (key != recentKey) {
            recentKey = key
            findViewById<LinearLayout>(R.id.workbench_recent).apply {
                removeAllViews()
                if (value.runs.isEmpty()) addView(TextView(context).apply { setText(R.string.workbench_no_runs) })
                else value.runs.take(20).forEach { recent -> addView(Button(context).apply {
                    isAllCaps = false; text = getString(R.string.workbench_recent_item, recent.string("goal"), WorkbenchText.state(context, recent))
                    setOnClickListener { recent.string("runId")?.let(::openDetail) }
                }) }
            }
            tint(findViewById(R.id.workbench_recent))
        }
        tint(findViewById(R.id.workbench_pending)); updateSend()
    }
    private fun renderLink(status: JsonObject) {
        val host = hostReader()
        val presence = classifyHostPresence(host, AiAgentPlugin.REQUIRED_HOST_VERSION)
        val rootsAccepted = !scriptRoots.configured || runCatching {
            status.getAsJsonArray("scriptRoots").map { it.asString }.toSet() == scriptRoots.read()
        }.getOrDefault(false)
        attached = presence == HostPresence.READY && status.string("state") == C.LINK_STATE_ATTACHED && rootsAccepted
        val label = when (presence) {
            HostPresence.MISSING -> getString(R.string.launcher_host_missing, AiAgentPlugin.REQUIRED_HOST_VERSION)
            HostPresence.DISABLED -> getString(R.string.launcher_host_disabled)
            HostPresence.INCOMPATIBLE -> getString(R.string.launcher_host_incompatible, host!!.versionCode, AiAgentPlugin.REQUIRED_HOST_VERSION)
            HostPresence.READY -> when {
                attached -> { deadline = 0; getString(R.string.workbench_connected, status.string("modelName") ?: getString(R.string.workbench_auto_model)) }
                deadline > SystemClock.elapsedRealtime() -> getString(R.string.launcher_link_connecting)
                requested -> getString(if (status.string("state") == C.LINK_STATE_ATTACHED && !rootsAccepted) R.string.script_roots_rejected else R.string.launcher_link_timeout)
                else -> getString(R.string.launcher_host_ready, host!!.versionName, host.versionCode)
            }
        }
        findViewById<TextView>(R.id.launcher_host_status).text = label
        findViewById<Button>(R.id.launcher_connect).apply { isEnabled = presence == HostPresence.READY; visibility = if (attached) View.GONE else View.VISIBLE }
        findViewById<Button>(R.id.launcher_open_host).apply { isEnabled = presence == HostPresence.READY; visibility = if (attached) View.GONE else View.VISIBLE }
        if (presence == HostPresence.READY && !attached && !requested) requestAttachment()
    }
    private fun identify(intent: Intent) {
        val identity = PendingIntent.getActivity(this, 0, Intent(this, LauncherActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        intent.putExtra(AiAgentActions.EXTRA_ATTACH_IDENTITY, identity)
        intent.putExtra("requestId", requestId ?: UUID.randomUUID().toString())
        if (scriptRoots.configured) intent.putExtra(C.KEY_LINK_CONFIG_JSON, ScriptRoots.configuration(scriptRoots.read()))
    }
    private fun requestAttachment() {
        if (classifyHostPresence(hostReader(), AiAgentPlugin.REQUIRED_HOST_VERSION) != HostPresence.READY) return
        requested = true; requestId = UUID.randomUUID().toString(); deadline = SystemClock.elapsedRealtime() + 15000
        findViewById<TextView>(R.id.launcher_host_status).setText(R.string.launcher_link_connecting)
        val intent = Intent(AiAgentActions.ACTION_ATTACH_REQUEST).setPackage(AiAgentPlugin.HOST_PACKAGE_NAME).addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        identify(intent); sendBroadcast(intent, AiAgentActions.PLUGIN_PERMISSION)
    }
    private fun showError() { findViewById<TextView>(R.id.workbench_error).apply { setText(R.string.workbench_request_failed); visibility = View.VISIBLE } }
    private fun openDetail(id: String) { startActivity(Intent(this, RunDetailActivity::class.java).putExtra("runId", id)) }
    private fun readHostPackage(): HostPackageSnapshot? {
        val info = try { packageManager.getPackageInfo(AiAgentPlugin.HOST_PACKAGE_NAME, PackageManager.MATCH_DISABLED_COMPONENTS) }
            catch (_: PackageManager.NameNotFoundException) { return null }
        val version = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        return HostPackageSnapshot(info.applicationInfo?.enabled == true, version, info.versionName.orEmpty())
    }
    private companion object { const val VOICE_REQUEST = 12 }
}
