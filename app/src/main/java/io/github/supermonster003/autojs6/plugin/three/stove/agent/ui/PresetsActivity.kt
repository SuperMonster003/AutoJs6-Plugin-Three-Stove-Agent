package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.net.Uri
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolGroup
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors

/**
 * Private preset management: cards with a row menu, a full-page editor whose draft is kept until
 * the agent process acknowledges an atomic save, and a portable JSON export with a row-by-row
 * import review (roadmap I.3). Presets carry tools, limits, confirmation policy, context, script
 * directories and memory scope; the model is chosen on the home screen (roadmap D46) and never
 * travels in a file.
 */
class PresetsActivity : HostAppearanceActivity() {
    private lateinit var connection: PresetConnection
    private lateinit var scaffold: Scaffold
    internal lateinit var page: LinearLayout
    internal lateinit var bar: LinearLayout
    private lateinit var message: TextView
    private val files = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    internal var configuration = JsonObject()
    internal var draft: JsonObject? = null
    /** The editor's starting point, to detect unsaved changes. */
    internal var initial: JsonObject? = null
    internal var editing: String? = null
    /** A legacy preset model is kept for scripts only; the plugin UI uses the shared model choice. */
    internal var targetId: String? = null
    internal lateinit var name: TextInputEditText
    internal lateinit var fixedContext: TextInputEditText
    internal lateinit var inheritGroups: MaterialCheckBox
    internal lateinit var inheritRoots: MaterialCheckBox
    internal lateinit var confirmation: ChoiceRow
    internal lateinit var planMode: MaterialCheckBox
    internal lateinit var scope: ChoiceRow
    internal val groups = linkedMapOf<String, MaterialCheckBox>()
    internal val roots = linkedMapOf<String, MaterialCheckBox>()
    internal val budgets = linkedMapOf<String, TextInputEditText>()
    /** Rows read from a chosen file and reviewed one at a time; nothing is written until a row is accepted. */
    internal var imports = emptyList<Preset>()
    internal var importIndex = 0
    private var source: Uri? = null
    private var destination: Uri? = null
    /** The last opened dialog or menu, exposed for instrumentation. */
    internal var prompt: AlertDialog? = null
    internal var menu: PopupMenu? = null; private set
    internal var editorVisible = false
    private var busy = false
    private val enabledStates = mutableListOf<Pair<View, Boolean>>()
    /** An editor requested by the launching intent: name to edit or copy, or "" for a new preset. */
    private var pendingOpen: Pair<String, Boolean>? = null
    internal val importDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        source = uri?.takeIf { it.scheme == "content" }
        // A document picker can return while this activity is still started and already bound.
        if (source != null && connection.connected) refresh()
    }
    internal val exportDocument = registerForActivityResult(CreateJsonDocument()) { uri ->
        destination = uri
        if (destination != null && connection.connected) refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editing = savedInstanceState?.getString("editing")
        draft = savedInstanceState?.getString("draft")?.let { runCatching { AgentJson.objectOf(it, PresetCodec.MAX_ROW_BYTES) }.getOrNull() }
        initial = savedInstanceState?.getString("initial")?.let { runCatching { AgentJson.objectOf(it, PresetCodec.MAX_ROW_BYTES) }.getOrNull() }
        imports = savedInstanceState?.getByteArray("imports")?.let { runCatching { PresetCodec.decodeExport(it.toString(Charsets.UTF_8)) }.getOrNull() }.orEmpty()
        importIndex = savedInstanceState?.getInt("importIndex", 0)?.coerceIn(0, imports.size) ?: 0
        source = savedInstanceState?.getString("source")?.let(Uri::parse)
        destination = savedInstanceState?.getString("destination")?.let(Uri::parse)
        scaffold = buildScaffold(getString(R.string.presets_title), contentPadding = ContentPadding.SCREEN)
        page = scaffold.content
        bar = kit.actionBar().apply { visibility = View.GONE }
        scaffold.root.addView(bar, LinearLayout.LayoutParams(-1, -2))
        setContentView(scaffold.root)
        message = kit.text("", Ui.TEXT_BODY, palette.danger)
        connection = PresetConnection(this) { refresh() }
        if (savedInstanceState == null) pendingOpen = when {
            intent.hasExtra(PresetsIntents.NEW) -> "" to false
            intent.hasExtra(PresetsIntents.EDIT) -> intent.getStringExtra(PresetsIntents.EDIT).orEmpty() to false
            intent.hasExtra(PresetsIntents.COPY) -> intent.getStringExtra(PresetsIntents.COPY).orEmpty() to true
            else -> null
        }
    }
    override fun onStart() { super.onStart(); busy = false; connection.start() }
    override fun onStop() { if (editorVisible) draft = readDraft(); connection.stop(); prompt?.dismiss(); prompt = null; menu?.dismiss(); menu = null; super.onStop() }
    override fun onDestroy() { connection.close(); files.shutdown(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("editing", editing)
        outState.putString("draft", (if (editorVisible) readDraft() else draft)?.toString())
        outState.putString("initial", initial?.toString())
        // UTF-8 bytes keep even a full 256 KiB import below the saved-state Binder limit.
        if (imports.isNotEmpty()) outState.putByteArray("imports", PresetCodec.encodeExport(imports).toByteArray(Charsets.UTF_8))
        outState.putInt("importIndex", importIndex)
        outState.putString("source", source?.toString()); outState.putString("destination", destination?.toString())
        super.onSaveInstanceState(outState)
    }
    override fun navigateBack() {
        if (busy) return
        if (imports.isNotEmpty()) { cancelImport(); return }
        if (!editorVisible) { finish(); return }
        val leave = { draft = null; initial = null; editing = null; editorVisible = false; refresh() }
        if (readDraft() == initial) leave() else prompt = kit.unsavedChanges(leave)
    }

    private fun request(operation: String, extra: JsonObject = JsonObject(), complete: (JsonObject) -> Unit) {
        extra.addProperty("operation", operation)
        connection.query(extra) { result -> result.onSuccess(complete).onFailure { busy = false; enable(true); showError() } }
    }
    private fun refresh() {
        request("list") { value ->
            configuration = value
            val saved = draft
            val open = pendingOpen; pendingOpen = null
            when {
                importIndex < imports.size -> showImport()
                saved != null -> showEditor(saved, initial ?: saved)
                open == null -> showList()
                open.first.isEmpty() -> { editing = null; val row = PresetCodec.encodePreset(Preset("")); showEditor(row, row) }
                else -> edit(open.first, copy = open.second)
            }
            if (source != null) readSource() else if (destination != null) writeDestination()
        }
    }
    internal fun reset(title: Int) {
        enable(true); page.removeAllViews(); groups.clear(); roots.clear(); budgets.clear()
        supportActionBar?.title = getString(title)
        message = kit.text("", Ui.TEXT_BODY, palette.danger).apply {
            visibility = View.GONE; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
        page.addView(message, LinearLayout.LayoutParams(-1, -2))
    }
    /** Freezes the page and the action bar while a write or a file operation is in flight, then restores each control's own state. */
    internal fun enable(enabled: Boolean) {
        if (enabled) { enabledStates.forEach { (view, previous) -> view.isEnabled = previous }; enabledStates.clear(); return }
        enabledStates.clear()
        fun visit(view: View) { enabledStates += view to view.isEnabled; view.isEnabled = false; if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index)) }
        visit(page); visit(bar)
    }
    internal fun presetLabel(name: String) = if (name == "default") getString(R.string.workbench_default_preset) else name
    internal fun groupLabel(id: String): String = ToolGroup.entries.firstOrNull { it.id == id }?.let { ToolPresentation.groupLabel(this, it) } ?: id

    internal fun edit(key: String, copy: Boolean) = request("get", jsonObject("name" to key.json())) { row ->
        editing = key.takeUnless { copy }
        if (copy) row.addProperty("name", "")
        showEditor(row, row.deepCopy())
    }
    internal fun actions(key: String, anchor: View) {
        menu = PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, R.string.presets_edit); menu.add(0, 2, 1, R.string.presets_copy)
            if (configuration.string("defaultName") != key) menu.add(0, 3, 2, R.string.presets_set_default)
            if (TaskEntries.canPin(this@PresetsActivity)) menu.add(0, 4, 3, R.string.shortcut_pin)
            if (key != "default") menu.add(0, 5, 4, R.string.presets_delete)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> edit(key, copy = false)
                    2 -> edit(key, copy = true)
                    3 -> request("default", jsonObject("name" to key.json())) { refresh() }
                    4 -> pin(key)
                    5 -> prompt = kit.confirmDialog(getString(R.string.presets_delete), getString(R.string.presets_delete_confirm, key),
                        getString(R.string.presets_delete), destructive = true) { request("delete", jsonObject("name" to key.json())) { refresh() } }
                }
                true
            }
            show()
        }
    }
    private fun pin(key: String) {
        prompt = kit.inputDialog(getString(R.string.shortcut_pin), null, getString(R.string.shortcut_review), getString(R.string.shortcut_goal),
            maxLength = 4096, positive = getString(R.string.shortcut_pin),
            validate = { goal -> if (runCatching { TaskEntry(goal.trim(), key) }.isSuccess) null else getString(R.string.entry_invalid) },
        ) { goal ->
            if (!runCatching { TaskEntries.pin(this, TaskEntry(goal.trim(), key)) }.getOrDefault(false)) kit.snackbar(scaffold.root, getString(R.string.entry_invalid))
        }
    }

    private fun readDraft(): JsonObject = jsonObject("name" to name.text.toString().json(), "context" to fixedContext.text.toString().json(),
        "confirmPolicy" to (if (confirmation.selectedIndex == 1) "cautious" else "default").json(),
        "memoryScope" to PresetCodec.scopes[scope.selectedIndex.coerceAtLeast(0)].json(),
        "budget" to JsonObject().apply { budgets.forEach { (key, input) ->
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) addProperty(key, if (key == DURATION) text.toLongOrNull()?.let { (it * 60_000).toString() } ?: text else text)
        } }).apply {
        targetId?.let { addProperty("targetId", it) }
        if (planMode.isChecked) addProperty("planMode", true)
        if (!inheritGroups.isChecked) add("toolGroups", JsonArray().apply { groups.filterValues { it.isChecked }.keys.forEach(::add) })
        if (!inheritRoots.isChecked) add("scriptRoots", JsonArray().apply { roots.filterValues { it.isChecked }.keys.forEach(::add) })
    }
    internal fun save() {
        if (busy || !editorVisible) return
        val row = runCatching {
            val value = readDraft()
            val budget = value.getAsJsonObject("budget")
            budget.keySet().toList().forEach { key -> budget.addProperty(key, budget[key].asString.toLong()) }
            val parsed = PresetCodec.decodePreset(value)
            require(parsed.toolGroups?.all { it in configuration.getAsJsonArray("toolGroups").map { value -> value.asString } } != false)
            require(parsed.scriptRoots?.all { it in configuration.getAsJsonArray("scriptRoots").map { value -> value.asString } } != false)
            require(PresetCodec.encodePreset(parsed).toString().utf8Size() <= PresetCodec.MAX_ROW_BYTES)
            value
        }.getOrElse { showError(); return }
        busy = true
        // Keep the acknowledged draft stable, including Back, while its write is in flight.
        enable(false)
        connection.query(jsonObject("operation" to "save".json(), "preset" to row, "create" to (editing == null).json())) { result ->
            busy = false; enable(true)
            result.onSuccess { draft = null; initial = null; editing = null; editorVisible = false; refresh() }.onFailure { showError() }
        }
    }

    private fun readSource() {
        val uri = source ?: return
        if (busy) return
        busy = true; enable(false)
        files.execute {
            val result = runCatching { requireNotNull(contentResolver.openInputStream(uri)).use(::readImport) }
            main.post { if (!isDestroyed) {
                source = null; busy = false; enable(true)
                result.onSuccess { beginImport(it) }.onFailure { showError() }
            } }
        }
    }
    /** Starts the review of validated rows; the first row is shown at once, later ones after each decision. */
    internal fun beginImport(values: List<Preset>) {
        PresetCodec.encodeExport(values) // Also validates instrumentation-supplied input at this boundary.
        imports = values.toList(); importIndex = 0; draft = null; initial = null; editing = null
        showImport()
    }
    internal fun cancelImport() { imports = emptyList(); importIndex = 0; refresh() }
    internal fun nextImport() {
        importIndex++
        if (importIndex >= imports.size) { imports = emptyList(); importIndex = 0 }
        refresh()
    }
    /** Writes one accepted row exactly as the review showed it (see [importable]). */
    internal fun importPreset(row: Preset, create: Boolean) {
        if (busy) return
        busy = true; enable(false)
        request("save", jsonObject("preset" to PresetCodec.encodePreset(row), "create" to create.json())) { busy = false; nextImport() }
    }
    /** The row as this device can store it: tool groups and script directories it does not offer are dropped and named. */
    internal fun importable(row: Preset): Pair<Preset, List<String>> {
        val allowedGroups = configuration.getAsJsonArray("toolGroups").map { it.asString }.toSet()
        val allowedRoots = configuration.getAsJsonArray("scriptRoots").map { it.asString }.toSet()
        val dropped = row.toolGroups.orEmpty().filter { it !in allowedGroups }.sorted().map(::groupLabel) + row.scriptRoots.orEmpty().filter { it !in allowedRoots }.sorted()
        return row.copy(toolGroups = row.toolGroups?.intersect(allowedGroups), scriptRoots = row.scriptRoots?.intersect(allowedRoots)) to dropped
    }
    private fun writeDestination() {
        val uri = destination ?: return
        if (busy) return
        busy = true; enable(false)
        connection.query(jsonObject("operation" to "export".json())) { result ->
            result.onFailure { destination = null; busy = false; enable(true); showError() }.onSuccess { data ->
                files.execute {
                    val success = runCatching { requireNotNull(contentResolver.openOutputStream(uri, "wt")).use { writeExport(it, data) } }.isSuccess
                    main.post { if (!isDestroyed) {
                        destination = null; busy = false; enable(true)
                        if (success) kit.snackbar(scaffold.root, getString(R.string.history_exported)) else showError()
                    } }
                }
            }
        }
    }
    internal fun showError() {
        message.setText(R.string.presets_error); message.visibility = View.VISIBLE
        scaffold.scroll?.smoothScrollTo(0, 0)
    }
    internal companion object {
        const val DURATION = "maxDurationMs"
        val BUDGET_LABELS = linkedMapOf("maxSteps" to R.string.presets_steps, "maxModelCalls" to R.string.presets_calls,
            DURATION to R.string.settings_duration_minutes, "maxTotalTokens" to R.string.presets_tokens)
        val SCOPE_LABELS = listOf(R.string.presets_memory_both, R.string.presets_memory_global, R.string.presets_memory_preset, R.string.presets_memory_none)
        internal fun readImport(input: InputStream): List<Preset> = PresetCodec.decodeExport(readJsonDocument(input, PresetCodec.MAX_EXPORT_BYTES))
        internal fun writeExport(output: OutputStream, data: JsonObject) {
            output.write(PresetCodec.encodeExport(PresetCodec.decodeExport(data.toString())).toByteArray(Charsets.UTF_8)); output.flush()
        }
    }
}

/** Intent extras that open the presets screen directly in its editor (value: the preset name, or "" for a new one). */
internal object PresetsIntents {
    const val EDIT = "edit"
    const val COPY = "copy"
    const val NEW = "new"
}
