package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.*
import android.text.*
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.AiAgentPlugin
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptRoots
import io.github.supermonster003.autojs6.plugin.ai.agent.service.RunLauncher
import io.github.supermonster003.autojs6.plugin.ai.agent.aiAgentPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.*
import io.github.supermonster003.autojs6.plugin.ai.agent.update.AppUpdateCoordinator
import org.autojs.plugin.ai.agent.api.AiAgentActions
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import java.util.UUID

/** Standalone task workbench: a task feed with a docked composer. Model/device work stays in the agent process. */
class LauncherActivity : HostAppearanceActivity(), FeedActions {
    private lateinit var agent: AgentConnection
    private lateinit var pending: PendingCard
    private lateinit var views: WorkbenchViews
    private val visibility by lazy { InteractionVisibility(this, followsRun = true) }
    private val goal get() = views.composer.goal
    internal lateinit var models: ModelSwitcher; private set
    internal var overflowMenu: PopupMenu? = null; private set
    internal var presetDialog: AlertDialog? = null; private set
    private lateinit var updates: AppUpdateCoordinator
    private val scriptRoots by lazy { ScriptRootSettings(this) }
    private val drafts by lazy { getSharedPreferences("workbench", MODE_PRIVATE) }
    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { onSpeechResult(it.resultCode, it.data) }
    private var currentId: String? = null
    private var attached = false
    private var sending = false
    private var requested = false
    private var deadline = 0L
    private var requestId: String? = null
    private var availablePresets = emptyList<String>()
    private var selectedPreset = "default"
    private var followDefault = true
    /** A finished task the user dismissed with New task; the feed shows the welcome state instead. */
    private var hiddenRunId: String? = null
    private var revealCurrent = false
    private var lastPendingId: String? = null
    internal var hostReader: () -> HostPackageSnapshot? = { readHostPackage() }
    internal val selectedPresetName get() = selectedPreset
    internal val presetChoices get() = (availablePresets + selectedPreset).distinct()
    internal val presetsLoaded get() = availablePresets.isNotEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        models = ModelSwitcher(this, ::updateSend)
        views = WorkbenchLayout.create(this, this, onConnect = { requested = false; requestAttachment() }, onOpenHost = ::openHost,
            onPreset = ::choosePreset, onVoice = ::voice, onSend = ::launchRun, onMore = ::showMenu, onJump = { scrollToEnd(true) })
        setContentView(views.root)
        updates = AppUpdateCoordinator(this, aiAgentPluginRuntimeInfo().versionName)
        views.root.layoutDirection = resources.configuration.layoutDirection
        agent = AgentConnection(this, ::render)
        agent.selectedId = savedInstanceState?.getString("selectedId")
        hiddenRunId = savedInstanceState?.getString("hiddenRunId")
        val entry = TaskEntries.read(intent)
        // The model is the shared choice, independent of presets; entries never reset it.
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_OPEN_MODELS, false)) window.decorView.post { models.open() }
        if (savedInstanceState == null && intent.action == TaskEntries.PRESET_TASK && entry != null) TaskEntries.opened(this, entry)
        selectedPreset = savedInstanceState?.getString("preset") ?: entry?.preset ?: drafts.getString("preset", "default")!!
        goal.setText(savedInstanceState?.getString("goal") ?: entry?.goal ?: drafts.getString("goal", ""))
        followDefault = savedInstanceState?.getBoolean("followDefault") ?: (entry?.preset == null && (entry != null || goal.text.isNullOrBlank()))
        goal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateSend() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        pending = PendingCard(views.feed.pending, kit) { body, complete ->
            agent.command({ it.respond(AgentConnection.request(C.KEY_RUN_RESPONSE_JSON, body)) }) { result ->
                complete(result.isSuccess); if (result.isFailure) showError()
            }
        }
        pending.restore(savedInstanceState)
        views.scroll.setOnScrollChangeListener { _: androidx.core.widget.NestedScrollView, _: Int, _: Int, _: Int, _: Int -> if (atEnd()) views.jump.visibility = View.GONE }
        renderPreset(); updateSend()
    }
    override fun onStart() { super.onStart(); requested = false; sending = false; models.start(); agent.start(); updates.checkAutomatically() }
    override fun onResume() { super.onResume(); visibility.start() }
    override fun onPause() { visibility.stop(); super.onPause() }
    override fun onStop() {
        drafts.edit().putString("goal", goal.text.toString()).putString("preset", selectedPreset).apply()
        overflowMenu?.dismiss(); overflowMenu = null; presetDialog?.dismiss(); presetDialog = null
        models.stop(); updates.cancel(); agent.stop(); super.onStop()
    }
    override fun onDestroy() { models.close(); updates.close(); agent.close(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("goal", goal.text.toString()); outState.putString("preset", selectedPreset)
        outState.putString("selectedId", agent.selectedId); outState.putString("hiddenRunId", hiddenRunId)
        outState.putBoolean("followDefault", followDefault)
        pending.save(outState)
        super.onSaveInstanceState(outState)
    }

    /** Voice results only fill the goal; they never start a task. */
    internal fun onSpeechResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) SpeechInput.result(data)?.let { goal.setText(it); goal.setSelection(goal.length()) }
    }
    private fun voice() { runCatching { speech.launch(SpeechInput.intent(this)) }.onFailure { showError() } }
    private fun openHost() {
        packageManager.getLaunchIntentForPackage(AiAgentPlugin.HOST_PACKAGE_NAME)?.let { intent ->
            intent.putExtra(AiAgentActions.EXTRA_AI_AGENT_ATTACH, true); identify(intent)
            runCatching { startActivity(intent) }.onFailure { showError() }
        }
    }
    private fun showMenu(anchor: View) {
        overflowMenu = PopupMenu(this, anchor).apply {
            menu.add(0, R.id.workbench_new_task, 0, R.string.workbench_new_task)
            menu.add(0, R.id.workbench_presets, 1, R.string.presets_title)
            menu.add(0, R.id.workbench_memory, 2, R.string.memory_title)
            menu.add(0, R.id.launcher_script_roots, 3, R.string.script_roots_title)
            menu.add(0, R.id.workbench_mcp, 4, R.string.mcp_servers)
            menu.add(0, R.id.workbench_settings, 5, R.string.settings_title)
            setOnMenuItemClickListener { item ->
                val screen = when (item.itemId) {
                    R.id.workbench_new_task -> { newTask(); return@setOnMenuItemClickListener true }
                    R.id.workbench_settings -> SettingsActivity::class.java
                    R.id.workbench_presets -> PresetsActivity::class.java
                    R.id.workbench_memory -> MemoryActivity::class.java
                    R.id.workbench_mcp -> McpServersActivity::class.java
                    else -> ScriptRootsActivity::class.java
                }
                startActivity(Intent(this@LauncherActivity, screen)); true
            }; show()
        }
    }
    private fun presetLabel(name: String) = if (name == "default") getString(R.string.workbench_default_preset) else name
    private fun renderPreset() { views.composer.showPreset(presetLabel(selectedPreset), !attached || selectedPreset in availablePresets) }
    private fun choosePreset() {
        val names = presetChoices
        presetDialog = kit.singleChoiceDialog(getString(R.string.workbench_preset), names.map(::presetLabel), names.indexOf(selectedPreset)) { index ->
            names.getOrNull(index)?.let { if (it != selectedPreset) followDefault = false; selectedPreset = it; renderPreset(); updateSend() }
        }
    }
    /** New task: clear the composer and set a finished task aside; a running task stays visible. */
    private fun newTask() {
        currentId?.takeIf { views.feed.stopButton.visibility != View.VISIBLE }?.let { hiddenRunId = it }
        goal.setText(""); views.composer.error.visibility = View.GONE
        agent.refresh(); focusComposer()
    }
    override fun prefill(goal: String, preset: String?, chooseModel: Boolean) {
        this.goal.setText(goal); this.goal.setSelection(this.goal.length())
        preset?.let { selectedPreset = it; followDefault = false; renderPreset() }
        updateSend(); focusComposer()
        if (chooseModel) models.open()
    }
    override fun openDetail(runId: String) { startActivity(Intent(this, RunDetailActivity::class.java).putExtra("runId", runId)) }
    override fun stop(runId: String) {
        agent.command({ link ->
            link.cancelRun(AgentConnection.request(C.KEY_RUN_REF_JSON, jsonObject("runId" to runId.json())))
            AgentConnection.request(C.KEY_RUN_RESPONSE_JSON)
        }) { if (it.isFailure) showError() }
    }
    private fun focusComposer() {
        goal.requestFocus()
        getSystemService(InputMethodManager::class.java).showSoftInput(goal, 0)
    }

    private fun launchRun() {
        if (!attached || sending || selectedPreset !in availablePresets || !models.available) return
        val text = goal.text.toString().trim()
        val request = runCatching { RunLauncher.uiRequest(text, selectedPreset, resources.configuration.locales[0].toLanguageTag(), models.targetId) }.getOrNull()
        if (request == null) { views.composer.error.apply { setText(R.string.workbench_goal_invalid); visibility = View.VISIBLE }; return }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        sending = true; updateSend()
        agent.command({ it.startRun(Bundle().apply {
            putInt(C.KEY_CONTRACT_VERSION, C.CONTRACT_VERSION); putString(C.KEY_RUN_REQUEST_JSON, request)
        }, null) }) { result ->
            sending = false
            result.onSuccess {
                agent.selectedId = it.string("runId"); hiddenRunId = null; revealCurrent = true
                goal.clearFocus()
                getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(goal.windowToken, 0)
                if (goal.text.toString().trim() == text) goal.setText("")
                drafts.edit().putString("goal", goal.text.toString()).apply()
                views.composer.error.visibility = View.GONE
            }.onFailure { showError() }
            updateSend()
        }
    }
    private fun updateSend() {
        views.composer.send.isEnabled = attached && !sending && !goal.text.isNullOrBlank() && selectedPreset in availablePresets && models.available
    }
    private fun render(value: WorkbenchSnapshot) {
        renderLink(value.status)
        views.composer.fullAccess.visibility = if (value.status.flag("fullAccessEnabled") == true) View.VISIBLE else View.GONE
        views.composer.voice.visibility = if (value.status.flag("voiceEnabled") == true && SpeechInput.available(this)) View.VISIBLE else View.GONE
        availablePresets = value.presets
        if (followDefault && attached && value.defaultPreset in availablePresets && selectedPreset != value.defaultPreset) selectedPreset = value.defaultPreset
        renderPreset()
        // Preserve a historical preset that has since disappeared. Never silently rerun with default.
        views.composer.error.apply {
            if (attached && selectedPreset !in availablePresets) { setText(R.string.history_preset_unavailable); visibility = View.VISIBLE }
            else if (text == getString(R.string.history_preset_unavailable)) visibility = View.GONE
        }
        val row = value.run?.takeIf { it.string("runId") != hiddenRunId }
        currentId = row?.string("runId")
        val wasAtEnd = atEnd()
        val before = views.feed.view.height
        views.feed.render(value.copy(run = row))
        pending.render(row)
        visibility.render(row)
        val request = row?.getAsJsonObject("pending")?.string("requestId")
        val revealQuestion = request != null && request != lastPendingId
        lastPendingId = request
        views.scroll.post {
            // Sticky scrolling follows a task; the welcome state and first load keep their position.
            when {
                row == null -> views.jump.visibility = View.GONE
                revealCurrent -> { revealCurrent = false; reveal(views.feed.current) }
                revealQuestion -> reveal(views.feed.pending)
                wasAtEnd -> scrollToEnd(false)
                before > 0 && views.feed.view.height > before && !atEnd() -> views.jump.visibility = View.VISIBLE
            }
        }
        updateSend()
    }
    private fun atEnd(): Boolean {
        val content = views.scroll.getChildAt(0) ?: return true
        return content.bottom - (views.scroll.height + views.scroll.scrollY) <= kit.dp(48)
    }
    private fun scrollToEnd(animated: Boolean) {
        val bottom = (views.scroll.getChildAt(0)?.height ?: 0) - views.scroll.height
        if (animated) views.scroll.smoothScrollTo(0, bottom.coerceAtLeast(0)) else views.scroll.scrollTo(0, bottom.coerceAtLeast(0))
        views.jump.visibility = View.GONE
    }
    /** Brings [target] to the top of the feed viewport (a new task or a new question). */
    private fun reveal(target: View) {
        val content = views.scroll.getChildAt(0) ?: return
        val rect = Rect(0, 0, target.width, target.height)
        runCatching { (content as android.view.ViewGroup).offsetDescendantRectToMyCoords(target, rect) }.onFailure { return }
        views.scroll.smoothScrollTo(0, (rect.top - kit.dp(Ui.SPACE_LG)).coerceAtLeast(0))
        views.jump.visibility = View.GONE
    }
    private fun renderLink(status: JsonObject) {
        val host = hostReader()
        val presence = classifyHostPresence(host, AiAgentPlugin.REQUIRED_HOST_VERSION)
        val rootsAccepted = !scriptRoots.configured || runCatching {
            status.getAsJsonArray("scriptRoots").map { it.asString }.toSet() == scriptRoots.read()
        }.getOrDefault(false)
        attached = presence == HostPresence.READY && status.string("state") == C.LINK_STATE_ATTACHED && rootsAccepted
        models.attached(attached)
        val stage = when {
            attached -> { deadline = 0; LinkStage.ATTACHED }
            deadline > SystemClock.elapsedRealtime() -> LinkStage.CONNECTING
            requested -> if (status.string("state") == C.LINK_STATE_ATTACHED && !rootsAccepted) LinkStage.ROOTS_REJECTED else LinkStage.TIMED_OUT
            else -> LinkStage.WAITING
        }
        views.banner.render(presence, host, stage, AiAgentPlugin.REQUIRED_HOST_VERSION)
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
        views.banner.render(HostPresence.READY, hostReader(), LinkStage.CONNECTING, AiAgentPlugin.REQUIRED_HOST_VERSION)
        val intent = Intent(AiAgentActions.ACTION_ATTACH_REQUEST).setPackage(AiAgentPlugin.HOST_PACKAGE_NAME).addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        identify(intent); sendBroadcast(intent, AiAgentActions.PLUGIN_PERMISSION)
    }
    private fun showError() { views.composer.error.apply { setText(R.string.workbench_request_failed); visibility = View.VISIBLE } }
    private fun readHostPackage(): HostPackageSnapshot? {
        val info = try { packageManager.getPackageInfo(AiAgentPlugin.HOST_PACKAGE_NAME, PackageManager.MATCH_DISABLED_COMPONENTS) }
            catch (_: PackageManager.NameNotFoundException) { return null }
        val version = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        return HostPackageSnapshot(info.applicationInfo?.enabled == true, version, info.versionName.orEmpty())
    }
    companion object {
        /** Opens the model switcher on arrival, for example from the floating ball. */
        const val EXTRA_OPEN_MODELS = "openModels"
    }
}
