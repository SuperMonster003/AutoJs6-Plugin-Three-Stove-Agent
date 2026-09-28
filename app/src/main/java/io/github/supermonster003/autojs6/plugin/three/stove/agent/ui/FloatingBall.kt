package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.app.*
import android.content.*
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.*
import android.provider.Settings
import android.text.*
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.android.material.chip.Chip
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.Cancellation
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.PortResult
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.ModelRef
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.ModelSelectionState
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * Event-driven window owned by the attached runtime, never by an idle foreground service.
 *
 * Three shapes (roadmap P16): the compact ball (with the task and its latest step beside the handle
 * while a task runs), the step timeline card opened by tapping that text, and the control card with
 * the same two-row composer as the workbench (preset, model and access chips above the goal field,
 * voice and start). Every choice is inline: an overlay cannot host dialogs, menus or sheets.
 */
internal class FloatingBall(private val runtime: AgentRuntime) : AutoCloseable {
    private val app = runtime.context
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "three-stove-agent-floating").apply { isDaemon = true } }
    private val queued = AtomicBoolean()
    private var revision = 0
    private val prefs = app.getSharedPreferences("floating", Context.MODE_PRIVATE)
    private val visibilityOwner = Binder()
    private val appOps = app.getSystemService(AppOpsManager::class.java)
    private val power = app.getSystemService(PowerManager::class.java)
    private val keyguard = app.getSystemService(KeyguardManager::class.java)
    @Suppress("DEPRECATION")
    private val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
    private val windowContext = if (Build.VERSION.SDK_INT >= 30) app.createDisplayContext(
        app.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)).createWindowContext(type, null) else app
    private val manager = windowContext.getSystemService(WindowManager::class.java)
    private var appearance: HostAppearance? = null
    private var readAppearance = true
    private var context: Context = themed()
    private var kit: Kit = Kit(context, AgentPalette.resolve(context, appearance))
    private var closed = false
    private var unlocked = power.isInteractive && !keyguard.isKeyguardLocked
    private var wakeGeneration = 0
    private var expanded = false
    /** The step timeline card over the compact ball; never combined with the control card. */
    private var timelineOpen = false
    private enum class Panel { NONE, MORE, PRESETS, MODELS, ACCESS }
    private var panel = Panel.NONE
    private var sending = false
    private var snapshot: WorkbenchSnapshot? = null
    private var root: LinearLayout? = null
    private var layout: WindowManager.LayoutParams? = null
    private var goalField: EditText? = null
    private var pending: PendingCard? = null
    private var pendingDraft = Bundle()
    private var goalLabel: TextView? = null
    private var stepLabel: TextView? = null
    private var textColumn: LinearLayout? = null
    private var stopButton: View? = null
    private var sendButton: Button? = null
    private var presetChip: Chip? = null
    private var modelChip: Chip? = null
    private var accessChip: Chip? = null
    private var presetPanel: LinearLayout? = null
    private var modelPanel: LinearLayout? = null
    private var accessPanel: LinearLayout? = null
    private var morePanel: LinearLayout? = null
    private var timeline: RunTimeline? = null
    private var timelineFollow: AutoScroll? = null
    /** The shared model choice, read off the UI thread with each snapshot (null current means Automatic). */
    private var selection: ModelSelectionState = ModelSelectionState.AUTOMATIC
    private var models: List<ModelEntry> = emptyList()
    private var modelsStatus = ModelCatalog.Status.IDLE
    private var modelsLoad: Cancellation? = null
    private var voiceButton: View? = null
    private var message: TextView? = null
    private var cardScroll: ScrollView? = null
    private var visibleRequest: String? = null
    private var draft = prefs.getString("goal", "").orEmpty()
    private var selectedPreset = prefs.getString("preset", null)
    private var presetNames = emptyList<String>()
    private var xFraction = prefs.getFloat("x", 1f)
    private var yFraction = prefs.getFloat("y", 0.35f)
    private var dimensions: Pair<Int, Int>? = null
    private val events = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> { wakeGeneration++; unlocked = false; expanded = false; timelineOpen = false; removeWindow() }
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> reconcileWake(++wakeGeneration, 20)
                Intent.ACTION_CONFIGURATION_CHANGED -> { readAppearance = true; removeWindow(); changed() }
            }
        }
    }
    private fun reconcileWake(generation: Int, remaining: Int) {
        if (closed || generation != wakeGeneration) return
        // Power/keyguard/display state may settle after the broadcast, especially without a
        // secure keyguard. Reconcile for two seconds after this event, never poll while idle.
        val ready = power.isInteractive && !keyguard.isKeyguardLocked
        if (ready != unlocked || remaining == 20 || remaining == 0) { unlocked = ready; changed() }
        if (remaining > 0) main.postDelayed({ reconcileWake(generation, remaining - 1) }, 100)
    }
    private val permissions = AppOpsManager.OnOpChangedListener { _, packageName ->
        if (packageName == app.packageName) main.post { if (!closed) changed() }
    }
    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(events, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("UnspecifiedRegisterReceiverFlag") app.registerReceiver(events, filter)
        appOps.startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, app.packageName, permissions)
    }
    fun changed() {
        revision++
        if (closed || !queued.compareAndSet(false, true)) return
        val expected = revision
        val refreshAppearance = readAppearance; readAppearance = false
        val steps = if (timelineOpen) 50 else 1
        worker.execute {
            // No idle timer, model call or provider binding. Archive access stays off the UI thread.
            val nextAppearance = if (refreshAppearance) AppearancePreferences.resolve(app, HostAppearance.read(app)) else appearance
            val value = runCatching {
                val link = runtime.current
                val status = link?.let { AgentConnection.decode(it.status(), C.KEY_STATUS_JSON) } ?: JsonObject()
                val active = link?.liveRuns().orEmpty()
                val id = status.string("runningRunId") ?: active.firstOrNull()?.string("runId")
                val run = id?.let { runtime.archive.get(it, steps, presentation = true) }
                val presets = runtime.presets.snapshot()
                runtime.settings.snapshot().let {
                    status.addProperty("voiceEnabled", it.voice); status.addProperty("fullAccessEnabled", it.fullAccess); status.addProperty("accessMode", it.accessMode)
                }
                WorkbenchSnapshot(status, active, run, presets.presets.map { it.name }, presets.defaultName)
            }.getOrNull()
            val chosen = runCatching { ModelSelection.read(app) }.getOrDefault(ModelSelectionState.AUTOMATIC)
            main.post {
                queued.set(false)
                if (closed) return@post
                if (appearance != nextAppearance) { appearance = nextAppearance; removeWindow(); context = themed() }
                snapshot = value; selection = chosen
                publish()
                if (revision != expected) changed()
            }
        }
    }
    private fun themed(): Context {
        val base = appearance?.wrap(windowContext) ?: windowContext
        val dark = appearance?.dark ?: (base.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        return ContextThemeWrapper(base, if (dark) R.style.Theme_ThreeStoveAgent_Dark else R.style.Theme_ThreeStoveAgent_Light)
    }
    private fun permitted() = !closed && unlocked && power.isInteractive && !keyguard.isKeyguardLocked &&
        Settings.canDrawOverlays(app) && runCatching { runtime.settings.snapshot().floating }.getOrDefault(false)
    private fun publish() {
        if (!permitted() || snapshot?.status?.string("state") != C.LINK_STATE_ATTACHED) {
            expanded = false; timelineOpen = false; removeWindow(); return
        }
        val value = snapshot ?: return
        val run = value.run
        // The timeline card has nothing to show once the task left the live set.
        if (timelineOpen && run == null) { timelineOpen = false; rebuildWindow() }
        if (root == null) createWindow()
        textColumn?.visibility = if (!expanded && run == null) View.GONE else View.VISIBLE
        goalLabel?.text = run?.string("goal")?.takeIf { it.isNotBlank() } ?: context.getString(R.string.app_name)
        val step = run?.let(::stepText)
        stepLabel?.text = step.orEmpty()
        stepLabel?.visibility = if (step.isNullOrEmpty()) View.GONE else View.VISIBLE
        // The single-line labels may ellipsize; readers get the full text plus what a tap does.
        textColumn?.contentDescription = listOfNotNull(goalLabel?.text, step, if (run == null) null else
            context.getString(if (timelineOpen) R.string.floating_timeline_close else R.string.floating_timeline_open)).joinToString(", ")
        stopButton?.visibility = if (run != null) View.VISIBLE else View.GONE
        if (timelineOpen && run != null) { timeline?.render(run.getAsJsonArray("steps")); timelineFollow?.contentChanged() }
        if (expanded) {
            if (selectedPreset == null) selectedPreset = value.defaultPreset
            val names = (value.presets + requireNotNull(selectedPreset)).distinct()
            if (names != presetNames) { presetNames = names; renderPresets() }
            renderModelChip(); renderAccess(value.status.string("accessMode") ?: if (value.status.flag("fullAccessEnabled") == true) "full" else "standard")
            voiceButton?.visibility = if (value.status.flag("voiceEnabled") == true && SpeechInput.available(context)) View.VISIBLE else View.GONE
            pending?.render(run)
            if (selectedPreset !in value.presets) message?.setText(R.string.history_preset_unavailable)
            val request = run?.getAsJsonObject("pending")?.takeIf { run.string("interaction") == "plugin" && it.flag("submitted") != true }
            if (visibleRequest != request?.string("requestId")) { visibleRequest = request?.string("requestId"); cardScroll?.scrollTo(0, 0) }
            runtime.interactions.present(visibilityOwner, run?.string("runId").takeIf { request != null }, request?.string("requestId"))
        } else runtime.interactions.present(visibilityOwner, null, null)
        updateSend(); reposition()
    }
    /** The latest step as the timeline names it: "Screen observation · ui_dump", a question, the finish. */
    private fun stepText(run: JsonObject): String? {
        val last = run.getAsJsonArray("steps")?.lastOrNull { it.isJsonObject }?.asJsonObject ?: return run.string("progress")
        return when (last.string("kind")) {
            "ask" -> context.getString(R.string.step_ask)
            "done" -> context.getString(R.string.step_done)
            "repair" -> context.getString(R.string.step_repair)
            "error" -> context.getString(R.string.step_error)
            else -> last.string("tool")?.let { ToolPresentation.label(context, it) } ?: run.string("progress")
        }
    }
    private fun presetLabel(name: String) = if (name == "default") context.getString(R.string.workbench_default_preset) else name
    private fun accessLabel(mode: String) = context.getString(when (mode) {
        "full" -> R.string.settings_full_access; "cautious" -> R.string.access_cautious_short; else -> R.string.access_standard_short
    })
    private fun showPanel(next: Panel) {
        panel = if (panel == next) Panel.NONE else next
        morePanel?.visibility = if (panel == Panel.MORE) View.VISIBLE else View.GONE
        presetPanel?.visibility = if (panel == Panel.PRESETS) View.VISIBLE else View.GONE
        modelPanel?.visibility = if (panel == Panel.MODELS) View.VISIBLE else View.GONE
        accessPanel?.visibility = if (panel == Panel.ACCESS) View.VISIBLE else View.GONE
        if (panel == Panel.MODELS && modelsStatus != ModelCatalog.Status.LOADING) loadModels()
        reposition()
    }
    /** Inline choices: an overlay window cannot host popups, dialogs or bottom sheets. */
    private fun renderPresets() {
        val chip = presetChip
        if (chip != null) { chip.text = selectedPreset?.let(::presetLabel).orEmpty(); chip.contentDescription = context.getString(R.string.workbench_preset) + ", " + chip.text }
        val list = presetPanel ?: return
        list.removeAllViews()
        presetNames.forEach { name ->
            val selected = name == selectedPreset
            list.addView(choiceRow(presetLabel(name), null, selected, "floating-preset-$name") {
                selectedPreset = name; showPanel(Panel.NONE); renderPresets(); updateSend(); saveDraft()
            })
        }
    }
    private fun renderModelChip() {
        val chip = modelChip ?: return
        val current = selection.current
        val automatic = AutomaticTarget.pick(models) { it.locality }
        chip.text = current?.name ?: automatic?.let { context.getString(R.string.model_automatic_with, it.name) } ?: context.getString(R.string.model_automatic)
        chip.contentDescription = context.getString(R.string.floating_model) + ", " + chip.text
    }
    private fun loadModels() {
        val link = runtime.current ?: run { modelsStatus = ModelCatalog.Status.FAILED; renderModels(); return }
        modelsLoad?.cancel()
        modelsStatus = ModelCatalog.Status.LOADING; renderModels()
        modelsLoad = link.targets { result ->
            main.post {
                if (closed) return@post
                when (result) {
                    is PortResult.Success -> {
                        models = result.value.map { ModelEntry(it.target.targetId, it.displayName.take(ModelRef.MAX_NAME), it.target.providerId,
                            it.target.locality, it.target.nativeTools != null, it.target.vision != null) }.distinctBy { it.targetId }
                        modelsStatus = ModelCatalog.Status.READY
                    }
                    is PortResult.Failure -> modelsStatus = ModelCatalog.Status.FAILED
                }
                renderModelChip(); renderModels()
            }
        }
    }
    private fun renderModels() {
        val list = modelPanel ?: return
        list.removeAllViews()
        val current = selection.current
        val ready = modelsStatus == ModelCatalog.Status.READY
        val note = kit.text(context.getString(when (modelsStatus) {
            ModelCatalog.Status.LOADING -> R.string.interaction_loading
            ModelCatalog.Status.READY -> if (models.isEmpty()) R.string.ui_models_empty else R.string.ui_model_note
            else -> R.string.presets_models_unavailable
        }), Ui.TEXT_CAPTION, kit.palette.muted).apply {
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setPaddingRelative(kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_SM), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_XS))
        }
        list.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(note, LinearLayout.LayoutParams(0, -2, 1f))
            addView(kit.iconButton(R.drawable.ic_restart, context.getString(R.string.presets_refresh_models), "floating-refresh-models", kit.palette.muted) { loadModels() },
                LinearLayout.LayoutParams(kit.dp(Ui.TOUCH_TARGET), kit.dp(Ui.TOUCH_TARGET)))
        }, LinearLayout.LayoutParams(-1, -2))
        val automatic = AutomaticTarget.pick(models) { it.locality }
        list.addView(choiceRow(context.getString(R.string.model_automatic), automatic?.let { context.getString(R.string.model_automatic_current, it.name) },
            current == null, "floating-model-automatic") { chooseModel(null) })
        if (ready && current != null && models.none { it.targetId == current.targetId }) {
            list.addView(choiceRow(current.name, context.getString(R.string.model_unavailable), true, "floating-model-unavailable", enabled = false) {})
        }
        for ((locality, title) in listOf(ModelLocality.REMOTE to R.string.presets_remote, ModelLocality.ON_DEVICE to R.string.presets_local, ModelLocality.HYBRID to R.string.presets_hybrid)) {
            val group = models.filter { it.locality == locality }
            if (group.isEmpty()) continue
            list.addView(kit.text(context.getString(title), Ui.TEXT_CAPTION, kit.palette.accent, medium = true).apply {
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_SM), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_XS))
            })
            group.forEach { entry ->
                list.addView(choiceRow(entry.name, entry.providerId.takeIf { it.isNotBlank() }, current?.targetId == entry.targetId, "floating-model-${entry.targetId}") { chooseModel(entry.ref) })
            }
        }
    }
    /** Writes the shared choice; the workbench reads the same file when it next starts. */
    private fun chooseModel(model: ModelRef?) {
        runCatching { ModelSelection.update(app) { it.choose(model) } }.onSuccess { selection = it }.onFailure { showError() }
        showPanel(Panel.NONE); renderModelChip()
    }
    private fun renderAccess(mode: String) {
        val chip = accessChip ?: return
        val danger = mode == "full"
        chip.text = accessLabel(mode)
        chip.contentDescription = context.getString(R.string.workbench_access, chip.text) + if (danger) ". " + context.getString(R.string.settings_full_access_note) else ""
        val (fill, foreground) = if (danger) kit.toneColors(Tone.DANGER) else kit.palette.surface to kit.palette.text
        chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(fill)
        chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(if (danger) foreground else kit.palette.outline)
        chip.setTextColor(foreground)
        chip.chipIcon = kit.tintedDrawable(R.drawable.ic_shield, if (danger) foreground else kit.palette.muted)
        val list = accessPanel ?: return
        list.removeAllViews()
        for ((value, label) in listOf("standard" to R.string.presets_standard, "cautious" to R.string.presets_cautious, "full" to R.string.settings_full_access)) {
            list.addView(choiceRow(context.getString(label), if (value == "full") context.getString(R.string.settings_full_access_note) else null, mode == value, "floating-access-$value") {
                showPanel(Panel.NONE)
                runCatching { runtime.settings.query(runtime.settings.snapshot().copy(cautious = value == "cautious", fullAccess = value == "full")) { changed() } }
                    .onFailure { showError() }
            })
        }
    }
    private fun choiceRow(title: String, summary: String?, selected: Boolean, tag: String, enabled: Boolean = true, onClick: () -> Unit): View =
        kit.settingRow(title, summary, if (selected) R.drawable.ic_check else null, tag, chevron = false,
            titleColor = if (!enabled) kit.palette.danger else if (selected) kit.palette.accent else kit.palette.text, onClick = if (enabled) onClick else null).view.apply {
            setPaddingRelative(kit.dp(Ui.SPACE_SM), paddingTop, kit.dp(Ui.SPACE_SM), paddingBottom); minimumHeight = kit.dp(52)
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
    private fun inlinePanel(parent: LinearLayout, tag: String): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; this.tag = tag; visibility = View.GONE
        background = kit.roundedFill(kit.palette.surfaceVariant, Ui.RADIUS_CONTROL)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
    }
    // RtlHardcoded: x/y are physical display coordinates; content still follows RTL.
    // ClickableViewAccessibility: the body's touch listener only consumes ACTION_OUTSIDE (a tap beyond the window), never a click on the view.
    @android.annotation.SuppressLint("RtlHardcoded", "ClickableViewAccessibility")
    @Suppress("DEPRECATION")
    private fun createWindow(reuse: LinearLayout? = null) {
        context = themed()
        kit = Kit(context, AgentPalette.resolve(context, appearance))
        val palette = kit.palette
        val active = snapshot?.run != null
        val compact = !expanded && !timelineOpen && !active
        val body = (reuse ?: LinearLayout(context)).apply {
            orientation = LinearLayout.VERTICAL; layoutDirection = context.resources.configuration.layoutDirection
            background = kit.roundedFill(palette.surface, if (compact) Ui.RADIUS_PILL else Ui.RADIUS_BUBBLE + 2, palette.outline)
            clipToOutline = true
            elevation = kit.dp(8).toFloat(); setPadding(kit.dp(4), kit.dp(4), kit.dp(4), kit.dp(4))
        }
        root = body
        val header = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; body.addView(this) }
        // The handle stays the first header child: it toggles the card and drags the window.
        val handle = Button(context).apply {
            tag = "floating-toggle"; setText(R.string.floating_monogram); isAllCaps = false
            textSize = Ui.TEXT_ITEM; typeface = Ui.medium; setTextColor(palette.onPrimary); stateListAnimator = null
            contentDescription = context.getString(if (expanded) R.string.floating_collapse else R.string.floating_open)
            background = android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(palette.accentRipple),
                android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(palette.primary) }, null)
            minWidth = kit.dp(48); minimumWidth = kit.dp(48); minHeight = kit.dp(48); minimumHeight = kit.dp(48); setPadding(0, 0, 0, 0)
            setOnClickListener { toggle() }
        }
        header.addView(handle, LinearLayout.LayoutParams(kit.dp(56), kit.dp(56)))
        drag(handle)
        // Task and latest step beside the handle; tapping the text opens or closes the step timeline.
        goalLabel = TextView(context).apply {
            tag = "floating-goal-label"; isSingleLine = true; ellipsize = TextUtils.TruncateAt.END; Ui.truncatable(this)
            textSize = Ui.TEXT_BODY; setTextColor(palette.text); textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        stepLabel = TextView(context).apply {
            tag = "floating-step"; isSingleLine = true; ellipsize = TextUtils.TruncateAt.END; Ui.truncatable(this)
            textSize = Ui.TEXT_SECONDARY; setTextColor(palette.muted); textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setPaddingRelative(0, kit.dp(1), 0, 0); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        textColumn = LinearLayout(context).apply {
            tag = "floating-text"; orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; minimumHeight = kit.dp(Ui.TOUCH_TARGET)
            setPaddingRelative(kit.dp(Ui.SPACE_MD), 0, kit.dp(Ui.SPACE_XS), 0)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            addView(goalLabel, LinearLayout.LayoutParams(-1, -2)); addView(stepLabel, LinearLayout.LayoutParams(-1, -2))
            if (!expanded) {
                isClickable = true; isFocusable = true; kit.selectableBackground(this)
                setOnClickListener { toggleTimeline() }
                accessibilityDelegate = object : View.AccessibilityDelegate() {
                    override fun onInitializeAccessibilityNodeInfo(host: View, info: android.view.accessibility.AccessibilityNodeInfo) {
                        super.onInitializeAccessibilityNodeInfo(host, info); info.className = Button::class.java.name
                    }
                }
            }
            header.addView(this, LinearLayout.LayoutParams(0, -2, 1f))
        }
        stopButton = kit.iconButton(R.drawable.ic_stop, context.getString(R.string.task_stop), "floating-stop", palette.danger) {
            snapshot?.run?.string("runId")?.let { runtime.current?.cancelLocal(it) }
        }.also { header.addView(it, LinearLayout.LayoutParams(kit.dp(Ui.TOUCH_TARGET), kit.dp(Ui.TOUCH_TARGET))) }
        if (timelineOpen && !expanded) {
            // Any tap outside the card returns to the compact ball.
            body.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_OUTSIDE && timelineOpen) { timelineOpen = false; rebuildWindow(); publish(); true } else false
            }
            val scroll = ScrollView(context).apply { tag = "floating-timeline"; isVerticalScrollBarEnabled = true }
            val list = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPaddingRelative(kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_XS), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_MD))
            }
            list.addView(kit.text(context.getString(R.string.history_timeline), Ui.TEXT_CAPTION, palette.accent, medium = true).apply {
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START; if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            }, LinearLayout.LayoutParams(-1, -2))
            timeline = RunTimeline(kit).also { list.addView(it.view, LinearLayout.LayoutParams(-1, -2)) }
            scroll.addView(list); body.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            cardScroll = scroll; timelineFollow = AutoScroll(scroll)
        }
        if (expanded) {
            // More: an inline panel (an overlay cannot host popup menus) with history, workbench, minimize and turn off.
            header.addView(kit.iconButton(R.drawable.ic_more, context.getString(R.string.floating_more), "floating-more", palette.muted) { showPanel(Panel.MORE) },
                LinearLayout.LayoutParams(kit.dp(Ui.TOUCH_TARGET), kit.dp(Ui.TOUCH_TARGET)))
            // Any tap outside the card minimizes it, like dismissing a sheet.
            body.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_OUTSIDE && expanded) { expanded = false; rebuildWindow(); publish(); true } else false
            }
            val scroll = ScrollView(context)
            cardScroll = scroll
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPaddingRelative(kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_SM), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_MD))
            }
            scroll.addView(card); body.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            morePanel = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; tag = "floating-more-panel"; visibility = if (panel == Panel.MORE) View.VISIBLE else View.GONE
                background = kit.roundedFill(palette.surfaceVariant, Ui.RADIUS_CONTROL)
                addView(kit.settingRow(context.getString(R.string.history_title), null, R.drawable.ic_history, "floating-history", chevron = false) {
                    showPanel(Panel.NONE); open(HistoryActivity::class.java)
                }.view, LinearLayout.LayoutParams(-1, -2))
                addView(kit.settingRow(context.getString(R.string.floating_workbench), null, R.drawable.ic_task, "floating-workbench", chevron = false) {
                    showPanel(Panel.NONE); open(LauncherActivity::class.java)
                }.view, LinearLayout.LayoutParams(-1, -2))
                addView(kit.settingRow(context.getString(R.string.floating_minimize), null, R.drawable.ic_expand, "floating-minimize", chevron = false) {
                    panel = Panel.NONE; expanded = false; rebuildWindow(); publish()
                }.view, LinearLayout.LayoutParams(-1, -2))
                addView(kit.settingRow(context.getString(R.string.floating_exit), null, R.drawable.ic_block, "floating-exit", chevron = false,
                    titleColor = palette.danger) {
                    // Turning the ball off is the same private setting as the settings screen switch; the runtime closes the window.
                    panel = Panel.NONE
                    runCatching { runtime.settings.query(runtime.settings.snapshot().copy(floating = false)) {} }.onFailure { showError() }
                }.view, LinearLayout.LayoutParams(-1, -2))
                card.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = kit.dp(Ui.SPACE_XS) })
            }
            val pendingColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; card.addView(this) }
            pending = PendingCard(pendingColumn, kit, framed = false) { request, complete ->
                val link = runtime.current
                if (link == null) complete(false) else {
                    // Release focus before the confirmed action or the next observation resumes.
                    expanded = false; rebuildWindow(); publish()
                    command({ link.local.respond(AgentConnection.request(C.KEY_RUN_RESPONSE_JSON, request)) }) {
                        complete(it.isSuccess)
                        if (it.isFailure) { expanded = true; rebuildWindow(); publish(); showError() }
                    }
                }
            }.apply { restore(pendingDraft) }
            message = kit.text("", Ui.TEXT_SECONDARY, palette.danger).apply {
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                card.addView(this, LinearLayout.LayoutParams(-1, -2))
            }
            // Options row, like the workbench composer: preset (may use the spare width), model, access.
            presetChip = kit.chip("", "floating-preset", icon = R.drawable.ic_layers) { showPanel(Panel.PRESETS) }.apply { Ui.truncatable(this) }
            modelChip = kit.chip("", "floating-model", icon = R.drawable.ic_spark) { showPanel(Panel.MODELS) }.apply {
                Ui.truncatable(this); ellipsize = TextUtils.TruncateAt.END; maxWidth = kit.dp(150)
            }
            accessChip = kit.chip("", "floating-access", icon = R.drawable.ic_shield) { showPanel(Panel.ACCESS) }.apply {
                contentDescription = context.getString(R.string.settings_access_mode)
            }
            card.addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(FrameLayout(context).apply {
                    addView(presetChip, FrameLayout.LayoutParams(-2, -2, Gravity.START or Gravity.CENTER_VERTICAL))
                }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_XS) })
                addView(modelChip, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = kit.dp(Ui.SPACE_XS) })
                addView(accessChip, LinearLayout.LayoutParams(-2, -2))
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
            presetPanel = inlinePanel(card, "floating-presets")
            modelPanel = inlinePanel(card, "floating-models")
            accessPanel = inlinePanel(card, "floating-access-panel")
            renderPresets(); renderModels()
            // Input row: the goal field grows to four lines; voice and start keep their targets at the bottom.
            val (goalLayout, goal) = kit.textField(draft, context.getString(R.string.workbench_goal_hint),
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 4096, singleLine = false, tag = "floating-goal")
            goalField = goal.apply {
                id = R.id.workbench_goal; minLines = 1; maxLines = 4; gravity = Gravity.CENTER_VERTICAL or Gravity.START
                if (Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { draft = s.toString(); updateSend() }
                    override fun afterTextChanged(s: Editable?) = Unit
                })
            }
            voiceButton = kit.iconButton(R.drawable.ic_mic, context.getString(R.string.workbench_voice), "floating-voice", palette.accent, ::voice)
            sendButton = kit.filledButton("", "floating-send", ::send).apply {
                contentDescription = context.getString(R.string.workbench_send)
                setIconResource(R.drawable.ic_send); iconPadding = 0; iconSize = kit.dp(22)
                iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
                setPaddingRelative(0, 0, 0, 0); minWidth = kit.dp(Ui.TOUCH_TARGET); minimumWidth = kit.dp(Ui.TOUCH_TARGET)
            }
            card.addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM; isBaselineAligned = false
                addView(goalLayout, LinearLayout.LayoutParams(0, -2, 1f))
                addView(voiceButton, LinearLayout.LayoutParams(kit.dp(Ui.TOUCH_TARGET), kit.dp(Ui.TOUCH_TARGET)).apply { marginStart = kit.dp(Ui.SPACE_XS); bottomMargin = kit.dp(4) })
                addView(sendButton, LinearLayout.LayoutParams(kit.dp(52), kit.dp(52)).apply { marginStart = kit.dp(Ui.SPACE_XS); bottomMargin = kit.dp(2) })
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
        }
        val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or when {
            expanded -> WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            timelineOpen -> WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            else -> WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        layout = WindowManager.LayoutParams(-2, -2, type, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            title = when { expanded -> "3-Stove Agent floating card"; timelineOpen -> "3-Stove Agent floating timeline"; else -> "3-Stove Agent floating ball" }
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        measureWindow()
        runCatching { if (reuse == null) manager.addView(body, layout) else manager.updateViewLayout(body, layout) }
            .onFailure { removeWindow() }
        if (reuse == null) body.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val bounds = usableBounds()
            val next = bounds.width() to bounds.height()
            if (dimensions != next) { dimensions = next; main.post { if (root === body) reposition() } }
        }
    }
    private fun updateSend() {
        sendButton?.isEnabled = !sending && draft.isNotBlank() && selectedPreset in snapshot?.presets.orEmpty() && permitted()
    }
    private fun send() {
        val link = runtime.current ?: return
        if (sending || !permitted()) return
        val text = draft.trim()
        // The same stored model choice as the workbench; a missing or unreadable file means Automatic.
        val target = ModelSelection.read(app).current?.targetId
        val request = runCatching { RunLauncher.uiRequest(text, requireNotNull(selectedPreset), context.resources.configuration.locales[0].toLanguageTag(), target) }.getOrNull()
        if (request == null) { goalField?.error = context.getString(R.string.workbench_goal_invalid); return }
        sending = true; updateSend()
        command({ link.local.startRun(AgentWire.envelope(C.KEY_RUN_REQUEST_JSON, request), null) }) { result ->
            sending = false
            if (result.isSuccess) {
                if (draft.trim() == text) { draft = ""; goalField?.setText("") }
                // Retain a visible compact overlay while the admitted task promotes its foreground service.
                expanded = false; rebuildWindow(); publish()
            } else showError()
            updateSend()
        }
    }
    private fun command(action: () -> Bundle, complete: (Result<JsonObject>) -> Unit) {
        if (closed) return
        worker.execute {
            val result = runCatching { AgentConnection.decode(action()) }
            main.post { if (!closed) { complete(result); changed() } }
        }
    }
    private fun showError() { message?.setText(R.string.workbench_request_failed) }
    private fun toggle() { expanded = !expanded; timelineOpen = false; panel = Panel.NONE; readAppearance = true; rebuildWindow(); publish(); changed() }
    private fun toggleTimeline() {
        if (expanded) return
        timelineOpen = !timelineOpen; rebuildWindow(); publish()
        if (timelineOpen) changed() // The compact snapshot carries one step; the card needs the window of steps.
    }
    private fun open(type: Class<*>) {
        saveDraft()
        runCatching {
            val intent = if (type == LauncherActivity::class.java) TaskEntries.intent(app, TaskEntry(draft, selectedPreset)) else Intent(app, type)
            app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onSuccess { expanded = false; rebuildWindow(); publish() }.onFailure { showError() }
    }
    private fun voice() {
        val oldDraft = draft
        val receiver = object : ResultReceiver(main) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (closed || resultCode != Activity.RESULT_OK || draft != oldDraft || !runtime.settings.snapshot().voice) return
                val text = resultData?.getString("text") ?: return
                draft = text; expanded = true; rebuildWindow(); publish(); saveDraft()
            }
        }
        runCatching { app.startActivity(Intent(app, VoiceInputActivity::class.java).putExtra("receiver", receiver)
            .putExtra("language", context.resources.configuration.locales[0].toLanguageTag()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { showError() }
        expanded = false; rebuildWindow(); publish()
    }
    private fun drag(view: View) {
        var startX = 0f; var startY = 0f; var originalX = 0; var originalY = 0; var moved = false
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        view.setOnTouchListener { target, event ->
            val params = layout ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startX = event.rawX; startY = event.rawY; originalX = params.x; originalY = params.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX; val dy = event.rawY - startY
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) {
                        val bounds = usableBounds()
                        xFraction = FloatingPosition.fraction(originalX + dx.toInt(), bounds.left, bounds.right, params.width)
                        yFraction = FloatingPosition.fraction(originalY + dy.toInt(), bounds.top, bounds.bottom, params.height)
                        reposition()
                    }; true
                }
                MotionEvent.ACTION_UP -> { if (moved) saveDraft() else target.performClick(); true }
                MotionEvent.ACTION_CANCEL -> { saveDraft(); true }
                else -> false
            }
        }
    }
    @Suppress("DEPRECATION")
    private fun usableBounds(): Rect {
        if (Build.VERSION.SDK_INT >= 30) {
            val metrics = manager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val bounds = metrics.bounds
            // Overlay gravity coordinates start inside the system's available frame, not at physical y=0.
            return Rect(0, 0, (bounds.width() - insets.left - insets.right).coerceAtLeast(1),
                (bounds.height() - insets.top - insets.bottom).coerceAtLeast(1))
        }
        val size = android.graphics.Point(); manager.defaultDisplay.getSize(size)
        val top = root?.rootWindowInsets?.systemWindowInsetTop ?: 0
        // Legacy WindowManager fits TYPE_PHONE/TYPE_APPLICATION_OVERLAY inside the system bars.
        return Rect(0, 0, size.x, (size.y - top).coerceAtLeast(1))
    }
    private fun measureWindow() {
        val params = layout ?: return
        val bounds = usableBounds()
        val active = snapshot?.run != null
        params.width = (when { expanded || timelineOpen -> kit.dp(360); active -> kit.dp(320); else -> kit.dp(64) }).coerceAtMost(bounds.width())
        params.height = if (expanded || timelineOpen) {
            // Size the card to its content up to a share of the usable height; taller content scrolls inside.
            val cap = (bounds.height() * (if (expanded) 0.72f else 0.6f)).toInt()
            val content = cardScroll?.getChildAt(0)?.also {
                it.measure(View.MeasureSpec.makeMeasureSpec((params.width - kit.dp(8)).coerceAtLeast(1), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            }?.measuredHeight ?: cap
            (content + kit.dp(56 + 8 + 8)).coerceIn(kit.dp(160), cap)
        } else {
            // A fixed 64dp window clips the labels when the system font is enlarged.
            root?.measure(View.MeasureSpec.makeMeasureSpec(params.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            maxOf(kit.dp(64), root?.measuredHeight ?: 0).coerceAtMost(bounds.height())
        }
        params.x = FloatingPosition.coordinate(xFraction, bounds.left, bounds.right, params.width)
        params.y = FloatingPosition.coordinate(yFraction, bounds.top, bounds.bottom, params.height)
    }
    private fun reposition() {
        measureWindow()
        root?.takeIf { it.isAttachedToWindow }?.let { runCatching { manager.updateViewLayout(it, layout) }.onFailure { removeWindow() } }
    }
    private fun saveDraft() { prefs.edit().putString("goal", draft).putString("preset", selectedPreset).putFloat("x", xFraction).putFloat("y", yFraction).apply() }
    private fun clearVisibility() { runtime.interactions.present(visibilityOwner, null, null) }
    private fun clearCard() {
        goalField = null; pending = null; goalLabel = null; stepLabel = null; textColumn = null; stopButton = null; cardScroll = null; visibleRequest = null
        sendButton = null; presetChip = null; modelChip = null; accessChip = null; presetPanel = null; modelPanel = null; accessPanel = null; morePanel = null
        voiceButton = null; message = null; timeline = null; timelineFollow = null
        modelsLoad?.cancel(); modelsLoad = null; if (modelsStatus == ModelCatalog.Status.LOADING) modelsStatus = ModelCatalog.Status.IDLE
        presetNames = emptyList(); dimensions = null
    }
    private fun rebuildWindow() {
        val body = root ?: return
        pending?.save(pendingDraft)
        context.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(body.windowToken, 0)
        clearCard(); body.removeAllViews(); createWindow(body); saveDraft()
    }
    private fun removeWindow() {
        pending?.save(pendingDraft)
        root?.let { view ->
            context.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(view.windowToken, 0)
            runCatching { manager.removeViewImmediate(view) }
        }
        root = null; layout = null; clearCard()
        saveDraft(); clearVisibility()
    }
    override fun close() {
        if (closed) return
        closed = true; removeWindow(); main.removeCallbacksAndMessages(null)
        app.unregisterReceiver(events); appOps.stopWatchingMode(permissions); worker.shutdown()
    }
}
