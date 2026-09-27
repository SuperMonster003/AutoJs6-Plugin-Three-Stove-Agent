package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.*
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Locale
import java.util.concurrent.Executors

/** Review and per-entry import. A selected file or restored draft never grants permission to write. */
class MemoryActivity : HostAppearanceActivity() {
    private lateinit var connection: MemoryConnection
    private lateinit var scaffold: Scaffold
    private lateinit var page: LinearLayout
    private lateinit var bar: LinearLayout
    private lateinit var message: TextView
    private val files = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var rows = emptyList<MemoryEntry>()
    private var scopes = emptyList<String>()
    private var selected: MemoryEntry? = null
    private var draft: String? = null
    private var editor: TextInputEditText? = null
    private var imports = emptyList<MemoryEntry>()
    private var importIndex = 0
    private var filter: String? = null
    private var query = ""
    private var source: Uri? = null
    private var destination: Uri? = null
    private var busy = false
    /** The last opened dialog, exposed for instrumentation. */
    internal var prompt: AlertDialog? = null; private set
    private val enabledStates = mutableListOf<Pair<View, Boolean>>()
    private val importDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        source = uri?.takeIf { it.scheme == "content" }
        // A document picker can return while this activity is still started and already bound.
        if (source != null && connection.connected) refresh()
    }
    private val exportDocument = registerForActivityResult(CreateJsonDocument()) { uri ->
        destination = uri
        if (destination != null && connection.connected) refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = savedInstanceState?.getString("selected")?.let { runCatching { MemoryCodec.decodeEntry(AgentJson.objectOf(it, MemoryCodec.MAX_ROW_BYTES)) }.getOrNull() }
        draft = savedInstanceState?.getString("draft")
        imports = savedInstanceState?.getByteArray("imports")?.let { runCatching { MemoryCodec.decode(it.toString(Charsets.UTF_8)) }.getOrNull() }.orEmpty()
        importIndex = savedInstanceState?.getInt("importIndex", 0)?.coerceIn(0, imports.size) ?: 0
        filter = savedInstanceState?.getString("filter")
        query = savedInstanceState?.getString("query").orEmpty()
        source = savedInstanceState?.getString("source")?.let(Uri::parse)
        destination = savedInstanceState?.getString("destination")?.let(Uri::parse)
        scaffold = buildScaffold(getString(R.string.memory_title), contentPadding = ContentPadding.SCREEN)
        page = scaffold.content
        bar = kit.actionBar().apply { visibility = View.GONE }
        scaffold.root.addView(bar, LinearLayout.LayoutParams(-1, -2))
        setContentView(scaffold.root)
        message = kit.text("", Ui.TEXT_BODY, palette.danger)
        connection = MemoryConnection(this) { refresh() }
    }
    override fun onStart() { super.onStart(); busy = false; connection.start() }
    override fun onStop() { editor?.let { draft = it.text.toString() }; connection.stop(); prompt?.dismiss(); prompt = null; super.onStop() }
    override fun onDestroy() { connection.close(); files.shutdown(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        selected?.let { outState.putString("selected", MemoryCodec.entry(it).toString()) }
        outState.putString("draft", editor?.text?.toString() ?: draft)
        // UTF-8 bytes keep even a full 256 KiB import below the saved-state Binder limit.
        if (imports.isNotEmpty()) outState.putByteArray("imports", MemoryCodec.encode(imports).toByteArray(Charsets.UTF_8))
        outState.putInt("importIndex", importIndex); outState.putString("filter", filter); outState.putString("query", query)
        outState.putString("source", source?.toString()); outState.putString("destination", destination?.toString())
        super.onSaveInstanceState(outState)
    }
    override fun navigateBack() {
        if (busy) return
        val row = selected
        val leave = { selected = null; draft = null; imports = emptyList(); importIndex = 0; refresh() }
        when {
            row != null && editor?.text?.toString() != row.value -> prompt = kit.unsavedChanges(leave)
            row != null || imports.isNotEmpty() -> leave()
            else -> finish()
        }
    }

    private fun request(operation: String, extra: JsonObject = JsonObject(), complete: (JsonObject) -> Unit) {
        extra.addProperty("operation", operation)
        connection.query(extra) { result -> result.onSuccess(complete).onFailure { busy = false; enable(true); showError() } }
    }
    private fun refresh() {
        request("list") { data ->
            rows = data.getAsJsonArray("entries").map { MemoryCodec.decodeEntry(it.asJsonObject) }
            scopes = data.getAsJsonArray("scopes").map { it.asString }
            when { importIndex < imports.size -> showImport(); selected != null -> showEditor(); else -> showList() }
            if (source != null) readSource() else if (destination != null) writeDestination()
        }
    }
    private fun reset(bottom: Boolean) {
        enable(true); editor = null; page.removeAllViews(); bar.removeAllViews()
        bar.visibility = if (bottom) View.VISIBLE else View.GONE
        message = kit.text("", Ui.TEXT_BODY, palette.danger).apply {
            visibility = View.GONE; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
        page.addView(message, LinearLayout.LayoutParams(-1, -2))
    }

    private fun showList() {
        reset(bottom = false)
        kit.caption(page, getString(R.string.memory_note))
        page.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(kit.tonalButton(getString(R.string.memory_import), "memory-import") {
                runCatching { importDocument.launch(arrayOf("application/json")) }.onFailure { showError() }
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
            addView(kit.textButton(getString(R.string.memory_export), "memory-export") {
                prompt = kit.confirmDialog(getString(R.string.memory_export), getString(R.string.memory_export_notice), getString(R.string.memory_export)) {
                    runCatching { exportDocument.launch("ai-agent-memory.json") }.onFailure { showError() }
                }
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
            addView(kit.textButton(getString(R.string.memory_refresh), "memory-refresh") { refresh() }, LinearLayout.LayoutParams(0, -2, 1f))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun render() {
            list.removeAllViews()
            val needle = query.trim().lowercase(Locale.ROOT)
            val visible = rows.filter { (filter == null || it.scope == filter) &&
                (needle.isEmpty() || listOf(it.key, it.value, it.scope).any { text -> text.lowercase(Locale.ROOT).contains(needle) }) }
                .sortedWith(compareBy<MemoryEntry> { it.scope }.thenBy { it.key })
            if (visible.isEmpty()) list.addView(kit.emptyState(getString(R.string.memory_empty), R.drawable.ic_lightbulb))
            visible.forEach { row -> list.addView(entryCard(row), kit.cardParams(bottomDp = Ui.SPACE_SM)) }
        }
        val (searchLayout, search) = kit.textField(query, getString(R.string.memory_search), InputType.TYPE_CLASS_TEXT, 256, tag = "memory-search")
        searchLayout.startIconDrawable = kit.tintedDrawable(R.drawable.ic_search, palette.muted)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) { query = s?.toString().orEmpty(); render() }
        })
        page.addView(searchLayout, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
        val ids = listOf<String?>(null) + (scopes + rows.map { it.scope }).distinct().sorted()
        val scopeChips = mutableMapOf<String?, Chip>()
        ids.forEach { id ->
            scopeChips[id] = kit.chip(id ?: getString(R.string.history_all), "memory-scope-${id ?: "all"}", checkable = true, checked = filter == id) {
                filter = id; scopeChips.forEach { (key, chip) -> chip.isChecked = key == filter }; render()
            }.also { chips.addView(it, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = kit.dp(Ui.SPACE_SM) }) }
        }
        page.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
        page.addView(list, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
        render()
    }
    private fun entryCard(row: MemoryEntry): View = kit.card(interactive = true).apply {
        tag = "memory-entry-${row.scope}:${row.key}"
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(kit.text(row.key, Ui.TEXT_ITEM, medium = true).apply { textAlignment = View.TEXT_ALIGNMENT_VIEW_START }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(kit.badge(row.scope, if (row.scope == "global") Tone.ACCENT else Tone.NEUTRAL), LinearLayout.LayoutParams(-2, -2).apply { marginStart = kit.dp(Ui.SPACE_SM) })
        })
        addView(kit.text(AgentJson.truncate(row.value, 160), Ui.TEXT_SECONDARY, palette.muted).apply {
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, kit.dp(Ui.SPACE_XS), 0, 0)
        })
        contentDescription = getString(R.string.memory_item, row.key, row.scope, AgentJson.truncate(row.value, 160))
        setOnClickListener { selected = row; draft = row.value; showEditor() }
    }
    private fun describe(row: MemoryEntry) {
        page.addView(kit.card().apply {
            addView(kit.text(row.key, Ui.TEXT_TITLE, medium = true).apply { setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START })
            addView(kit.badge(row.scope, if (row.scope == "global") Tone.ACCENT else Tone.NEUTRAL), LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
            kit.caption(this, getString(R.string.memory_source, row.sourceRunId))
            kit.caption(this, getString(R.string.memory_created, Formats.date(context, row.createdAt)))
            kit.caption(this, getString(R.string.memory_updated, Formats.date(context, row.updatedAt)))
        }, kit.cardParams(topDp = Ui.SPACE_SM))
    }
    private fun showEditor() {
        val row = selected ?: return
        reset(bottom = true); supportActionBar?.title = getString(R.string.memory_title); describe(row)
        editor = kit.formField(page, getString(R.string.memory_value), draft ?: row.value, "memory-value", InputType.TYPE_CLASS_TEXT, 8192, multiline = true)
        bar.addView(kit.textButton(getString(R.string.memory_delete), "memory-delete", danger = true) {
            prompt = kit.confirmDialog(getString(R.string.memory_delete), getString(R.string.memory_delete_confirm), getString(R.string.memory_delete), destructive = true) {
                busy = true; enable(false)
                request("delete", jsonObject("entry" to MemoryCodec.entry(row))) { busy = false; selected = null; draft = null; refresh() }
            }
        })
        bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        bar.addView(kit.filledButton(getString(R.string.memory_save), "memory-save") {
            val changed = row.copy(value = editor!!.text.toString())
            if (runCatching { MemoryCodec.preference(changed.key, changed.value) }.isFailure) { showError(); return@filledButton }
            prompt = kit.confirmDialog(getString(R.string.memory_save), getString(R.string.memory_save_confirm), getString(R.string.memory_save)) {
                save(changed, row, false) { selected = null; draft = null; refresh() }
            }
        })
    }
    private fun showImport() {
        reset(bottom = true)
        val row = imports[importIndex]
        page.addView(kit.text(getString(R.string.memory_import_progress, importIndex + 1, imports.size), Ui.TEXT_SECTION, palette.accent, medium = true))
        describe(row)
        kit.formSection(page, getString(R.string.memory_value))
        page.addView(kit.text(row.value, Ui.TEXT_BODY).apply { setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START })
        val before = rows.find { it.identity == row.identity }
        before?.let {
            kit.formSection(page, getString(R.string.memory_replace))
            page.addView(kit.text(it.value, Ui.TEXT_BODY, palette.muted).apply { setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START })
        }
        val available = row.scope in scopes
        if (!available) page.addView(Banner(kit).apply { show(getString(R.string.memory_scope_missing), Tone.WARNING, R.drawable.ic_warning) }.view,
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
        page.addView(kit.textButton(getString(R.string.memory_cancel_import), "memory-cancel-import") { imports = emptyList(); importIndex = 0; refresh() },
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_LG) })
        // Equal shares: at large text sizes both labels wrap instead of overflowing the bar.
        bar.addView(kit.tonalButton(getString(R.string.memory_skip), "memory-skip") { nextImport() },
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
        bar.addView(kit.filledButton(getString(R.string.memory_accept), "memory-accept") { save(row, before, true) { nextImport() } }.apply { isEnabled = available },
            LinearLayout.LayoutParams(0, -2, 1f))
    }
    private fun nextImport() {
        importIndex++
        if (importIndex >= imports.size) { imports = emptyList(); importIndex = 0 }
        refresh()
    }
    private fun save(row: MemoryEntry, before: MemoryEntry?, imported: Boolean, complete: () -> Unit) {
        if (busy) return
        busy = true; enable(false)
        request("save", jsonObject("entry" to MemoryCodec.entry(row), "before" to (before?.let(MemoryCodec::entry) ?: JsonNull.INSTANCE), "imported" to imported.json())) {
            busy = false; complete()
        }
    }
    private fun enable(enabled: Boolean) {
        if (enabled) { enabledStates.forEach { (view, previous) -> view.isEnabled = previous }; enabledStates.clear(); return }
        enabledStates.clear()
        fun visit(view: View) { enabledStates += view to view.isEnabled; view.isEnabled = false; if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i)) }
        visit(page); visit(bar)
    }
    private fun showError() { message.setText(R.string.memory_error); message.visibility = View.VISIBLE; scaffold.scroll?.smoothScrollTo(0, 0) }
    private fun readSource() {
        val uri = source ?: return; if (busy) return; busy = true; enable(false)
        files.execute {
            val result = runCatching { requireNotNull(contentResolver.openInputStream(uri)).use(::readImport) }
            main.post { if (!isDestroyed) {
                source = null; busy = false; enable(true)
                result.onSuccess { beginImport(it) }.onFailure { showError() }
            } }
        }
    }
    internal fun beginImport(values: List<MemoryEntry>) {
        MemoryCodec.encode(values) // Also validates instrumentation-supplied input at this boundary.
        imports = values.toList(); importIndex = 0; selected = null; draft = null
        if (values.isEmpty()) showList() else showImport()
    }
    private fun writeDestination() {
        val uri = destination ?: return; if (busy) return; busy = true; enable(false)
        request("export") { data ->
            files.execute {
                val success = runCatching { requireNotNull(contentResolver.openOutputStream(uri, "wt")).use { writeExport(it, data) } }.isSuccess
                main.post { if (!isDestroyed) {
                    destination = null; busy = false; enable(true)
                    if (success) kit.snackbar(scaffold.root, getString(R.string.history_exported)) else showError()
                } }
            }
        }
    }
    /** A new JSON document chosen by the user; only content URIs are accepted. */
    private class CreateJsonDocument : ActivityResultContract<String, Uri?>() {
        override fun createIntent(context: Context, input: String) = Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json")
            .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, input)
        override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data?.takeIf { resultCode == Activity.RESULT_OK && it.scheme == "content" }
    }
    companion object {
        internal fun readImport(input: InputStream): List<MemoryEntry> {
            val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(4096)
            while (true) { val count = input.read(buffer); if (count < 0) break; require(output.size() + count <= MemoryCodec.MAX_BYTES); output.write(buffer, 0, count) }
            val json = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(output.toByteArray())).toString()
            return MemoryCodec.decode(json)
        }
        internal fun writeExport(output: OutputStream, data: JsonObject) {
            output.write(MemoryCodec.encode(MemoryCodec.decode(data.toString())).toByteArray(Charsets.UTF_8)); output.flush()
        }
    }
}
