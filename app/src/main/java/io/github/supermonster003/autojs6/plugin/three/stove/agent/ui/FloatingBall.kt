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
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.*
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/** Event-driven window owned by the attached runtime, never by an idle foreground service. */
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
    private var sending = false
    private var snapshot: WorkbenchSnapshot? = null
    private var root: LinearLayout? = null
    private var layout: WindowManager.LayoutParams? = null
    private var goalField: EditText? = null
    private var fullAccessLabel: TextView? = null
    private var pending: PendingCard? = null
    private var pendingDraft = Bundle()
    private var statusLabel: TextView? = null
    private var stopButton: View? = null
    private var sendButton: Button? = null
    private var presetRow: SettingRow? = null
    private var presetPanel: LinearLayout? = null
    private var presetPanelOpen = false
    private var morePanel: LinearLayout? = null
    private var morePanelOpen = false
    private var modelRow: SettingRow? = null
    /** The shared model choice (null means Automatic), read off the UI thread with each snapshot. */
    private var modelName: String? = null
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
                Intent.ACTION_SCREEN_OFF -> { wakeGeneration++; unlocked = false; expanded = false; removeWindow() }
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
        worker.execute {
            // No idle timer, model call or provider binding. Archive access stays off the UI thread.
            val nextAppearance = if (refreshAppearance) AppearancePreferences.resolve(app, HostAppearance.read(app)) else appearance
            val value = runCatching {
                val link = runtime.current
                val status = link?.let { AgentConnection.decode(it.status(), C.KEY_STATUS_JSON) } ?: JsonObject()
                val active = link?.liveRuns().orEmpty()
                val id = status.string("runningRunId") ?: active.firstOrNull()?.string("runId")
                val run = id?.let { runtime.archive.get(it, 1, presentation = true) }
                val presets = runtime.presets.snapshot()
                runtime.settings.snapshot().let { status.addProperty("voiceEnabled", it.voice); status.addProperty("fullAccessEnabled", it.fullAccess) }
                WorkbenchSnapshot(status, active, run, presets.presets.map { it.name }, presets.defaultName)
            }.getOrNull()
            val model = runCatching { ModelSelection.read(app).current?.name }.getOrNull()
            main.post {
                queued.set(false)
                if (closed) return@post
                if (appearance != nextAppearance) { appearance = nextAppearance; removeWindow(); context = themed() }
                snapshot = value; modelName = model
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
            expanded = false; removeWindow(); return
        }
        if (root == null) createWindow()
        val value = snapshot ?: return
        val run = value.run
        statusLabel?.visibility = if (!expanded && run == null) View.GONE else View.VISIBLE
        statusLabel?.text = if (run == null) context.getString(R.string.app_name) else
            WorkbenchText.state(context, run) + " " + (run.string("progress") ?: run.string("goal").orEmpty())
        statusLabel?.contentDescription = statusLabel?.text // The single-line label may ellipsize; readers get the full text.
        stopButton?.visibility = if (run != null) View.VISIBLE else View.GONE
        if (expanded) {
            if (selectedPreset == null) selectedPreset = value.defaultPreset
            val names = (value.presets + requireNotNull(selectedPreset)).distinct()
            if (names != presetNames) { presetNames = names; renderPresets() }
            modelRow?.setSummary(modelName ?: context.getString(R.string.model_automatic))
            voiceButton?.visibility = if (value.status.flag("voiceEnabled") == true && SpeechInput.available(context)) View.VISIBLE else View.GONE
            fullAccessLabel?.visibility = if (value.status.flag("fullAccessEnabled") == true) View.VISIBLE else View.GONE
            pending?.render(run)
            if (selectedPreset !in value.presets) message?.setText(R.string.history_preset_unavailable)
            val request = run?.getAsJsonObject("pending")?.takeIf { run.string("interaction") == "plugin" && it.flag("submitted") != true }
            if (visibleRequest != request?.string("requestId")) { visibleRequest = request?.string("requestId"); cardScroll?.scrollTo(0, 0) }
            runtime.interactions.present(visibilityOwner, run?.string("runId").takeIf { request != null }, request?.string("requestId"))
        } else runtime.interactions.present(visibilityOwner, null, null)
        updateSend(); reposition()
    }
    private fun presetLabel(name: String) = if (name == "default") context.getString(R.string.workbench_default_preset) else name
    /** Inline choices: an overlay window cannot host popups, dialogs or bottom sheets. */
    private fun renderPresets() {
        presetRow?.setSummary(selectedPreset?.let(::presetLabel))
        val panel = presetPanel ?: return
        panel.removeAllViews()
        presetNames.forEach { name ->
            val selected = name == selectedPreset
            panel.addView(kit.settingRow(presetLabel(name), null, if (selected) R.drawable.ic_check else null, "floating-preset-$name", chevron = false,
                titleColor = if (selected) kit.palette.accent else kit.palette.text) {
                selectedPreset = name; presetPanelOpen = false; panel.visibility = View.GONE; renderPresets(); updateSend(); saveDraft()
            }.view.apply { setPaddingRelative(kit.dp(Ui.SPACE_SM), paddingTop, kit.dp(Ui.SPACE_SM), paddingBottom); minimumHeight = kit.dp(52) })
        }
        panel.visibility = if (presetPanelOpen) View.VISIBLE else View.GONE
    }
    private fun pickerRow(parent: LinearLayout, title: Int, icon: Int, tag: String, onClick: () -> Unit): SettingRow =
        kit.settingRow(context.getString(title), null, icon, tag, onClick = onClick).also { row ->
            row.view.setPaddingRelative(kit.dp(Ui.SPACE_XS), kit.dp(Ui.SPACE_XS), 0, kit.dp(Ui.SPACE_XS)); row.view.minimumHeight = kit.dp(56)
            parent.addView(row.view, LinearLayout.LayoutParams(-1, -2))
        }
    // RtlHardcoded: x/y are physical display coordinates; content still follows RTL.
    // ClickableViewAccessibility: the body's touch listener only consumes ACTION_OUTSIDE (a tap beyond the window), never a click on the view.
    @android.annotation.SuppressLint("RtlHardcoded", "ClickableViewAccessibility")
    @Suppress("DEPRECATION")
    private fun createWindow(reuse: LinearLayout? = null) {
        context = themed()
        kit = Kit(context, AgentPalette.resolve(context, appearance))
        val palette = kit.palette
        val compact = !expanded && snapshot?.run == null
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
        statusLabel = TextView(context).apply {
            tag = "floating-step"; isSingleLine = true; ellipsize = TextUtils.TruncateAt.END; Ui.truncatable(this)
            textSize = Ui.TEXT_BODY; setTextColor(palette.text); textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setPaddingRelative(kit.dp(Ui.SPACE_MD), 0, kit.dp(Ui.SPACE_XS), 0)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            header.addView(this, LinearLayout.LayoutParams(0, -2, 1f))
        }
        stopButton = kit.iconButton(R.drawable.ic_stop, context.getString(R.string.task_stop), "floating-stop", palette.danger) {
            snapshot?.run?.string("runId")?.let { runtime.current?.cancelLocal(it) }
        }.also { header.addView(it, LinearLayout.LayoutParams(kit.dp(Ui.TOUCH_TARGET), kit.dp(Ui.TOUCH_TARGET))) }
        if (expanded) {
            // More: an inline panel (an overlay cannot host popup menus) with Minimize and Turn off.
            header.addView(kit.iconButton(R.drawable.ic_more, context.getString(R.string.floating_more), "floating-more", palette.muted) {
                morePanelOpen = !morePanelOpen; morePanel?.visibility = if (morePanelOpen) View.VISIBLE else View.GONE
            }, LinearLayout.LayoutParams(kit.dp(Ui.TOUCH_TARGET), kit.dp(Ui.TOUCH_TARGET)))
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
                orientation = LinearLayout.VERTICAL; tag = "floating-more-panel"; visibility = if (morePanelOpen) View.VISIBLE else View.GONE
                background = kit.roundedFill(palette.surfaceVariant, Ui.RADIUS_CONTROL)
                addView(kit.settingRow(context.getString(R.string.floating_minimize), null, R.drawable.ic_expand, "floating-minimize", chevron = false) {
                    morePanelOpen = false; expanded = false; rebuildWindow(); publish()
                }.view, LinearLayout.LayoutParams(-1, -2))
                addView(kit.settingRow(context.getString(R.string.floating_exit), null, R.drawable.ic_block, "floating-exit", chevron = false,
                    titleColor = palette.danger) {
                    // Turning the ball off is the same private setting as the settings screen switch; the runtime closes the window.
                    morePanelOpen = false
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
            // The goal field sits directly under the header so it is reachable without scrolling.
            val (goalLayout, goal) = kit.textField(draft, context.getString(R.string.workbench_goal_hint),
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 4096, singleLine = false, tag = "floating-goal")
            goalField = goal.apply {
                id = R.id.workbench_goal; maxLines = 5; minLines = 2
                if (Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { draft = s.toString(); updateSend() }
                    override fun afterTextChanged(s: Editable?) = Unit
                })
            }
            card.addView(goalLayout, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
            modelRow = pickerRow(card, R.string.floating_model, R.drawable.ic_spark, "floating-model") { open(LauncherActivity::class.java, models = true) }
            presetRow = pickerRow(card, R.string.workbench_preset, R.drawable.ic_layers, "floating-preset") {
                presetPanelOpen = !presetPanelOpen; renderPresets()
            }
            presetPanel = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; tag = "floating-presets"; visibility = View.GONE
                background = kit.roundedFill(palette.surfaceVariant, Ui.RADIUS_CONTROL)
                card.addView(this, LinearLayout.LayoutParams(-1, -2))
            }
            renderPresets()
            fullAccessLabel = kit.badge(context.getString(R.string.settings_full_access), Tone.DANGER).apply {
                tag = "floating-full-access"; visibility = View.GONE
                card.addView(this, LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
            }
            message = kit.text("", Ui.TEXT_SECONDARY, palette.danger).apply {
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                card.addView(this, LinearLayout.LayoutParams(-1, -2))
            }
            val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            card.addView(actions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
            voiceButton = kit.iconButton(R.drawable.ic_mic, context.getString(R.string.workbench_voice), "floating-voice", palette.accent, ::voice)
                .also { actions.addView(it, LinearLayout.LayoutParams(kit.dp(Ui.TOUCH_TARGET), kit.dp(Ui.TOUCH_TARGET)).apply { marginEnd = kit.dp(Ui.SPACE_SM) }) }
            sendButton = kit.filledButton(context.getString(R.string.workbench_send), "floating-send", ::send)
                .also { actions.addView(it, LinearLayout.LayoutParams(0, -2, 1f)) }
            val links = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            card.addView(links, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
            links.addView(kit.textButton(context.getString(R.string.history_title), "floating-history") { open(HistoryActivity::class.java) },
                LinearLayout.LayoutParams(0, -2, 1f))
            links.addView(kit.textButton(context.getString(R.string.floating_workbench), "floating-workbench") { open(LauncherActivity::class.java) },
                LinearLayout.LayoutParams(0, -2, 1f))
        }
        val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            (if (expanded) WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        layout = WindowManager.LayoutParams(-2, -2, type, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            title = if (expanded) "3-Stove Agent floating card" else "3-Stove Agent floating ball"
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
    private fun toggle() { expanded = !expanded; readAppearance = true; rebuildWindow(); publish(); changed() }
    private fun open(type: Class<*>, models: Boolean = false) {
        saveDraft()
        runCatching {
            val intent = if (type == LauncherActivity::class.java) TaskEntries.intent(app, TaskEntry(draft, selectedPreset))
                .putExtra(LauncherActivity.EXTRA_OPEN_MODELS, models) else Intent(app, type)
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
        params.width = (if (expanded) kit.dp(360) else if (active) kit.dp(280) else kit.dp(64)).coerceAtMost(bounds.width())
        params.height = if (expanded) {
            // Size the card to its content up to 72% of the usable height; taller content scrolls inside.
            val cap = (bounds.height() * 0.72f).toInt()
            val content = cardScroll?.getChildAt(0)?.also {
                it.measure(View.MeasureSpec.makeMeasureSpec((params.width - kit.dp(8)).coerceAtLeast(1), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            }?.measuredHeight ?: cap
            (content + kit.dp(56 + 8 + 8)).coerceIn(kit.dp(160), cap)
        } else {
            // A fixed 64dp window clips the stop label when the system font is enlarged.
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
        goalField = null; pending = null; statusLabel = null; stopButton = null; cardScroll = null; visibleRequest = null
        sendButton = null; presetRow = null; presetPanel = null; modelRow = null; voiceButton = null; fullAccessLabel = null; message = null; morePanel = null
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
