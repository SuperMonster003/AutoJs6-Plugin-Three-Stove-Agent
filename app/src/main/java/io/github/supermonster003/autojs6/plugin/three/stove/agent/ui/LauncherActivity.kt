package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.*
import android.net.Uri
import android.provider.Settings
import android.text.*
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ThreeStoveAgentPlugin
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.scripts.ScriptRoots
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.RunLauncher
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.SettingsCodec
import io.github.supermonster003.autojs6.plugin.three.stove.agent.threeStoveAgentPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.update.AppUpdateCoordinator
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentActions
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import java.util.UUID

/** Standalone task workbench: a task feed with a docked composer. Model/device work stays in the agent process. */
class LauncherActivity : HostAppearanceActivity(), FeedActions {
    internal lateinit var agent: AgentConnection
    internal lateinit var pending: PendingCard
    internal lateinit var views: WorkbenchViews
    internal val visibility by lazy { InteractionVisibility(this, followsRun = true) }
    internal val goal get() = views.composer.goal
    internal lateinit var models: ModelSwitcher; private set
    internal var overflowMenu: PopupMenu? = null
    internal var presetDialog: AlertDialog? = null
    internal var accessDialog: AlertDialog? = null
    internal var presetSheet: SheetHandle? = null
    internal var presetMenu: PopupMenu? = null
    internal val presetStore by lazy { PresetConnection(this) {} }
    internal var defaultPresetName = "default"
    private val settings by lazy { SettingsConnection(this) {} }
    internal var accessMode = "standard"
    internal var floatingEnabled = false
    private var awaitingOverlayPermission = false
    private lateinit var updates: AppUpdateCoordinator
    private val scriptRoots by lazy { ScriptRootSettings(this) }
    private val drafts by lazy { getSharedPreferences("workbench", MODE_PRIVATE) }
    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { onSpeechResult(it.resultCode, it.data) }
    internal var currentId: String? = null
    internal var attached = false
    private var sending = false
    private var requested = false
    private var deadline = 0L
    private var requestId: String? = null
    internal var availablePresets = emptyList<String>()
    internal var selectedPreset = "default"
    internal var followDefault = true
    /** A finished task the user dismissed with New task; the feed shows the welcome state instead. */
    internal var hiddenRunId: String? = null
    internal var revealCurrent = false
    internal var lastPendingId: String? = null
    internal var hostReader: () -> HostPackageSnapshot? = { readHostPackage() }
    internal val selectedPresetName get() = selectedPreset
    internal val presetChoices get() = (availablePresets + selectedPreset).distinct()
    internal val presetsLoaded get() = availablePresets.isNotEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        models = ModelSwitcher(this, ::updateSend)
        views = WorkbenchLayout.create(this, this, onConnect = { requested = false; requestAttachment() }, onOpenHost = ::openHost,
            onPreset = this::choosePreset, onAccess = this::chooseAccess, onVoice = ::voice, onSend = ::launchRun, onMore = this::showMenu, onJump = { scrollToEnd(true) })
        setContentView(views.root)
        updates = AppUpdateCoordinator(this, threeStoveAgentPluginRuntimeInfo().versionName)
        views.root.layoutDirection = resources.configuration.layoutDirection
        agent = AgentConnection(this, this::render)
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
    override fun onStart() { super.onStart(); requested = false; sending = false; models.start(); agent.start(); settings.start(); presetStore.start(); updates.checkAutomatically() }
    override fun onResume() {
        super.onResume(); visibility.start()
        // Returning from the overlay permission screen with the grant completes the floating ball toggle.
        if (awaitingOverlayPermission) { awaitingOverlayPermission = false; if (Settings.canDrawOverlays(this)) saveSettings { it.withFloating(true) } }
    }
    override fun onPause() { visibility.stop(); super.onPause() }
    override fun onStop() {
        drafts.edit().putString("goal", goal.text.toString()).putString("preset", selectedPreset).apply()
        overflowMenu?.dismiss(); overflowMenu = null; presetDialog?.dismiss(); presetDialog = null; accessDialog?.dismiss(); accessDialog = null
        presetMenu?.dismiss(); presetMenu = null; presetSheet?.dialog?.dismiss(); presetSheet = null
        models.stop(); updates.cancel(); agent.stop(); settings.stop(); presetStore.stop(); super.onStop()
    }
    override fun onDestroy() { models.close(); updates.close(); agent.close(); presetStore.close(); super.onDestroy() }
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
        packageManager.getLaunchIntentForPackage(ThreeStoveAgentPlugin.HOST_PACKAGE_NAME)?.let { intent ->
            intent.putExtra(ThreeStoveAgentActions.EXTRA_THREE_STOVE_AGENT_ATTACH, true); identify(intent)
            runCatching { startActivity(intent) }.onFailure { showError() }
        }
    }
    internal fun presetLabel(name: String) = if (name == "default") getString(R.string.workbench_default_preset) else name
    /** Whole-object save of the private settings, the same path as the settings screen. */
    internal fun saveSettings(change: (SettingsDraft) -> SettingsDraft) {
        settings.query(jsonObject("operation" to "get".json())) { loaded ->
            val draft = loaded.mapCatching { SettingsDraft(SettingsCodec.decode(it.getAsJsonObject("settings").toString())) }.getOrNull()
            if (draft == null) { showError(); return@query }
            settings.query(jsonObject("operation" to "save".json(), "settings" to SettingsCodec.json(change(draft).settings))) { saved ->
                if (saved.isFailure) showError() else agent.refresh()
            }
        }
    }
    /** The floating ball needs the overlay permission first; the toggle completes when the user returns with it. */
    internal fun toggleFloating(enabled: Boolean) {
        if (enabled && !Settings.canDrawOverlays(this)) {
            awaitingOverlayPermission = true
            runCatching { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
                .onFailure { awaitingOverlayPermission = false; showError() }
        } else saveSettings { it.withFloating(enabled) }
    }
    internal fun renderPreset() { views.composer.showPreset(presetLabel(selectedPreset), !attached || selectedPreset in availablePresets) }
    /** New task: clear the composer and set a finished task aside; a running task stays visible. */
    internal fun newTask() {
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
    internal fun updateSend() {
        views.composer.send.isEnabled = attached && !sending && !goal.text.isNullOrBlank() && selectedPreset in availablePresets && models.available
    }
    internal fun renderLink(status: JsonObject) {
        val host = hostReader()
        val presence = classifyHostPresence(host, ThreeStoveAgentPlugin.REQUIRED_HOST_VERSION)
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
        views.banner.render(presence, host, stage, ThreeStoveAgentPlugin.REQUIRED_HOST_VERSION)
        if (presence == HostPresence.READY && !attached && !requested) requestAttachment()
    }
    private fun identify(intent: Intent) {
        val identity = PendingIntent.getActivity(this, 0, Intent(this, LauncherActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        intent.putExtra(ThreeStoveAgentActions.EXTRA_ATTACH_IDENTITY, identity)
        intent.putExtra("requestId", requestId ?: UUID.randomUUID().toString())
        if (scriptRoots.configured) intent.putExtra(C.KEY_LINK_CONFIG_JSON, ScriptRoots.configuration(scriptRoots.read()))
    }
    private fun requestAttachment() {
        if (classifyHostPresence(hostReader(), ThreeStoveAgentPlugin.REQUIRED_HOST_VERSION) != HostPresence.READY) return
        requested = true; requestId = UUID.randomUUID().toString(); deadline = SystemClock.elapsedRealtime() + 15000
        views.banner.render(HostPresence.READY, hostReader(), LinkStage.CONNECTING, ThreeStoveAgentPlugin.REQUIRED_HOST_VERSION)
        val intent = Intent(ThreeStoveAgentActions.ACTION_ATTACH_REQUEST).setPackage(ThreeStoveAgentPlugin.HOST_PACKAGE_NAME).addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        identify(intent); sendBroadcast(intent, ThreeStoveAgentActions.PLUGIN_PERMISSION)
    }
    internal fun showError() { views.composer.error.apply { setText(R.string.workbench_request_failed); visibility = View.VISIBLE } }
    private fun readHostPackage(): HostPackageSnapshot? {
        val info = try { packageManager.getPackageInfo(ThreeStoveAgentPlugin.HOST_PACKAGE_NAME, PackageManager.MATCH_DISABLED_COMPONENTS) }
            catch (_: PackageManager.NameNotFoundException) { return null }
        val version = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        return HostPackageSnapshot(info.applicationInfo?.enabled == true, version, info.versionName.orEmpty())
    }
    companion object {
        /** Opens the model switcher on arrival, for example from the floating ball. */
        const val EXTRA_OPEN_MODELS = "openModels"
    }
}
