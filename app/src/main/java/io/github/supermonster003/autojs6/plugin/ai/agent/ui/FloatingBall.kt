package io.github.supermonster003.autojs6.plugin.ai.agent.ui

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
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.service.*
import org.autojs.plugin.ai.agent.api.AiAgentContract as C
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/** Event-driven window owned by the attached runtime, never by an idle foreground service. */
internal class FloatingBall(private val runtime: AgentRuntime) : AutoCloseable {
    private val app = runtime.context
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "ai-agent-floating").apply { isDaemon = true } }
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
    private var closed = false
    private var unlocked = power.isInteractive && !keyguard.isKeyguardLocked
    private var wakeGeneration = 0
    private var expanded = false
    private var sending = false
    private var snapshot: WorkbenchSnapshot? = null
    private var root: LinearLayout? = null
    private var layout: WindowManager.LayoutParams? = null
    private var goalField: EditText? = null
    private var pending: PendingCard? = null
    private var pendingDraft = Bundle()
    private var statusLabel: TextView? = null
    private var stopButton: Button? = null
    private var sendButton: Button? = null
    private var presetSpinner: Spinner? = null
    private var voiceButton: Button? = null
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
            val nextAppearance = if (refreshAppearance) HostAppearance.read(app) else appearance
            val value = runCatching {
                val link = runtime.current
                val status = link?.let { AgentConnection.decode(it.status(), C.KEY_STATUS_JSON) } ?: JsonObject()
                val active = link?.liveRuns().orEmpty()
                val id = status.string("runningRunId") ?: active.firstOrNull()?.string("runId")
                val run = id?.let { runtime.archive.get(it, 1, presentation = true) }
                val presets = runtime.presets.snapshot()
                status.addProperty("voiceEnabled", runtime.settings.snapshot().voice)
                WorkbenchSnapshot(status, active, run, presets.presets.map { it.name }, presets.defaultName)
            }.getOrNull()
            main.post {
                queued.set(false)
                if (closed) return@post
                if (appearance != nextAppearance) { appearance = nextAppearance; removeWindow(); context = themed() }
                snapshot = value
                publish()
                if (revision != expected) changed()
            }
        }
    }
    private fun themed(): Context {
        val base = appearance?.wrap(windowContext) ?: windowContext
        val dark = appearance?.dark ?: (base.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        return ContextThemeWrapper(base, if (dark) R.style.Theme_AiAgent_Dark else R.style.Theme_AiAgent_Light)
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
        stopButton?.visibility = if (run != null) View.VISIBLE else View.GONE
        if (expanded) {
            if (selectedPreset == null) selectedPreset = value.defaultPreset
            val names = (value.presets + requireNotNull(selectedPreset)).distinct()
            if (names != presetNames) {
                presetNames = names
                presetSpinner?.adapter = ArrayAdapter(context, R.layout.item_spinner_choice, names)
                presetSpinner?.setSelection(names.indexOf(selectedPreset).coerceAtLeast(0))
            }
            voiceButton?.visibility = if (value.status.flag("voiceEnabled") == true && SpeechInput.available(context)) View.VISIBLE else View.GONE
            pending?.render(run)
            if (selectedPreset !in value.presets) message?.setText(R.string.history_preset_unavailable)
            val request = run?.getAsJsonObject("pending")?.takeIf { run.string("interaction") == "plugin" && it.flag("submitted") != true }
            if (visibleRequest != request?.string("requestId")) { visibleRequest = request?.string("requestId"); cardScroll?.scrollTo(0, 0) }
            runtime.interactions.present(visibilityOwner, run?.string("runId").takeIf { request != null }, request?.string("requestId"))
        } else runtime.interactions.present(visibilityOwner, null, null)
        root?.let { styleHostControls(it, appearance) }
        updateSend(); reposition()
    }
    private fun button(parent: LinearLayout, resource: Int, tag: String, action: () -> Unit): Button = Button(context).apply {
        text = context.getString(resource); contentDescription = text; this.tag = tag; isAllCaps = false
        minimumHeight = dp(48); setOnClickListener { action() }; parent.addView(this)
    }
    @android.annotation.SuppressLint("RtlHardcoded") // x/y are physical display coordinates; content still follows RTL.
    @Suppress("DEPRECATION")
    private fun createWindow(reuse: LinearLayout? = null) {
        context = themed()
        val body = (reuse ?: LinearLayout(context)).apply {
            orientation = LinearLayout.VERTICAL; layoutDirection = context.resources.configuration.layoutDirection
            val background = android.util.TypedValue().also { context.theme.resolveAttribute(android.R.attr.colorBackground, it, true) }
            this.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(if (background.resourceId != 0) context.getColor(background.resourceId) else background.data)
                cornerRadius = dp(if (!expanded && snapshot?.run == null) 32 else 8).toFloat()
            }
            clipToOutline = true
            elevation = dp(8).toFloat(); setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        root = body
        val header = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; body.addView(this) }
        val handle = button(header, if (expanded) R.string.floating_collapse else R.string.floating_open, "floating-toggle") { toggle() }
        handle.setText(R.string.floating_monogram)
        handle.background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(appearance?.accent ?: 0xff607d8b.toInt()) }
        handle.minWidth = dp(48); handle.minimumWidth = dp(48)
        handle.layoutParams = LinearLayout.LayoutParams(dp(56), -2)
        drag(handle)
        statusLabel = TextView(context).apply {
            tag = "floating-step"; isSingleLine = true; ellipsize = TextUtils.TruncateAt.END
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            header.addView(this, LinearLayout.LayoutParams(0, -2, 1f))
        }
        stopButton = button(header, R.string.task_stop, "floating-stop") {
            snapshot?.run?.string("runId")?.let { runtime.current?.cancelLocal(it) }
        }
        if (expanded) {
            val scroll = ScrollView(context)
            cardScroll = scroll
            val card = HistoryViews.column(context)
            scroll.addView(card); body.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            val pendingColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; card.addView(this) }
            pending = PendingCard(pendingColumn) { request, complete ->
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
            HistoryViews.label(card, context.getString(R.string.workbench_goal_hint)).apply {
                labelFor = R.id.workbench_goal
            }
            goalField = EditText(context).apply {
                id = R.id.workbench_goal; tag = "floating-goal"; setText(draft)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; maxLines = 5; minLines = 2
                filters = arrayOf(InputFilter.LengthFilter(4096))
                if (Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { draft = s.toString(); updateSend() }
                    override fun afterTextChanged(s: Editable?) = Unit
                }); card.addView(this)
            }
            presetSpinner = Spinner(context).apply {
                tag = "floating-preset"; contentDescription = context.getString(R.string.presets_title); minimumHeight = dp(48)
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        presetNames.getOrNull(position)?.let { selectedPreset = it; updateSend() }
                    }
                }; card.addView(this)
            }
            message = HistoryViews.label(card, "").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE }
            voiceButton = button(card, R.string.workbench_voice, "floating-voice", ::voice)
            sendButton = button(card, R.string.workbench_send, "floating-send", ::send)
            button(card, R.string.history_title, "floating-history") { open(HistoryActivity::class.java) }
            button(card, R.string.floating_workbench, "floating-workbench") { open(LauncherActivity::class.java) }
        }
        val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            (if (expanded) WindowManager.LayoutParams.FLAG_SECURE else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        layout = WindowManager.LayoutParams(-2, -2, type, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            title = if (expanded) "AI Agent floating card" else "AI Agent floating ball"
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
        styleHostControls(body, appearance)
    }
    private fun updateSend() {
        sendButton?.isEnabled = !sending && draft.isNotBlank() && selectedPreset in snapshot?.presets.orEmpty() && permitted()
    }
    private fun send() {
        val link = runtime.current ?: return
        if (sending || !permitted()) return
        val text = draft.trim()
        val request = runCatching { RunLauncher.uiRequest(text, requireNotNull(selectedPreset), context.resources.configuration.locales[0].toLanguageTag()) }.getOrNull()
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
        params.width = (if (expanded) dp(360) else if (active) dp(280) else dp(64)).coerceAtMost(bounds.width())
        params.height = if (expanded) (bounds.height() * 0.72f).toInt() else {
            // A fixed 64dp window clips the stop label when the system font is enlarged.
            root?.measure(View.MeasureSpec.makeMeasureSpec(params.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            maxOf(dp(64), root?.measuredHeight ?: 0).coerceAtMost(bounds.height())
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
        sendButton = null; presetSpinner = null; voiceButton = null; message = null; presetNames = emptyList(); dimensions = null
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
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
    override fun close() {
        if (closed) return
        closed = true; removeWindow(); main.removeCallbacksAndMessages(null)
        app.unregisterReceiver(events); appOps.stopWatchingMode(permissions); worker.shutdown()
    }
}
