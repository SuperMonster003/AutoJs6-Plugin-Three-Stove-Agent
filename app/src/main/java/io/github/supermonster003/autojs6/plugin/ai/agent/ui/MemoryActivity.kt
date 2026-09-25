package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.*
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.Executors

/** Review and per-entry import. A selected file or restored draft never grants permission to write. */
class MemoryActivity : HostAppearanceActivity() {
    private lateinit var connection: MemoryConnection
    private lateinit var body: LinearLayout
    private lateinit var error: TextView
    private val files = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var rows = emptyList<MemoryEntry>()
    private var scopes = emptyList<String>()
    private var selected: MemoryEntry? = null
    private var draft: String? = null
    private var editor: EditText? = null
    private var imports = emptyList<MemoryEntry>()
    private var importIndex = 0
    private var filter: String? = null
    private var source: Uri? = null
    private var destination: Uri? = null
    private var busy = false
    private val enabledStates = mutableListOf<Pair<View, Boolean>>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = savedInstanceState?.getString("selected")?.let { runCatching { MemoryCodec.decodeEntry(AgentJson.objectOf(it, MemoryCodec.MAX_ROW_BYTES)) }.getOrNull() }
        draft = savedInstanceState?.getString("draft")
        imports = savedInstanceState?.getByteArray("imports")?.let { runCatching { MemoryCodec.decode(it.toString(Charsets.UTF_8)) }.getOrNull() }.orEmpty()
        importIndex = savedInstanceState?.getInt("importIndex", 0)?.coerceIn(0, imports.size) ?: 0
        filter = savedInstanceState?.getString("filter")
        source = savedInstanceState?.getString("source")?.let(Uri::parse)
        destination = savedInstanceState?.getString("destination")?.let(Uri::parse)
        body = HistoryViews.column(this).apply { layoutDirection = resources.configuration.layoutDirection }
        setContentView(ScrollView(this).apply { fitsSystemWindows = true; addView(body) })
        error = HistoryViews.label(body, "")
        connection = MemoryConnection(this) { refresh() }
    }
    override fun onStart() { super.onStart(); busy = false; connection.start() }
    override fun onStop() { editor?.let { draft = it.text.toString() }; connection.stop(); super.onStop() }
    override fun onDestroy() { connection.close(); files.shutdown(); super.onDestroy() }
    override fun onSaveInstanceState(out: Bundle) {
        selected?.let { out.putString("selected", MemoryCodec.entry(it).toString()) }
        out.putString("draft", editor?.text?.toString() ?: draft)
        // UTF-8 bytes keep even a full 256 KiB import below the saved-state Binder limit.
        if (imports.isNotEmpty()) out.putByteArray("imports", MemoryCodec.encode(imports).toByteArray(Charsets.UTF_8))
        out.putInt("importIndex", importIndex); out.putString("filter", filter)
        out.putString("source", source?.toString()); out.putString("destination", destination?.toString())
        super.onSaveInstanceState(out)
    }
    private fun query(operation: String, extra: JsonObject = JsonObject(), complete: (JsonObject) -> Unit) {
        extra.addProperty("operation", operation)
        connection.query(extra) { result -> result.onSuccess(complete).onFailure { busy = false; enable(true); showError() } }
    }
    private fun refresh() {
        query("list") { data ->
            rows = data.getAsJsonArray("entries").map { MemoryCodec.decodeEntry(it.asJsonObject) }
            scopes = data.getAsJsonArray("scopes").map { it.asString }
            when { importIndex < imports.size -> showImport(); selected != null -> showEditor(); else -> showList() }
            if (source != null) readSource() else if (destination != null) writeDestination()
        }
    }
    private fun header() {
        enable(true); editor = null; body.removeAllViews()
        HistoryViews.label(body, getString(R.string.memory_title), true)
        HistoryViews.button(body, R.string.workbench_back, "back") {
            if (selected != null || imports.isNotEmpty()) { selected = null; draft = null; imports = emptyList(); importIndex = 0; refresh() }
            else finish()
        }
        error = HistoryViews.label(body, "").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE }
        HistoryViews.label(body, getString(R.string.memory_note))
    }
    private fun showList() {
        header()
        HistoryViews.button(body, R.string.memory_refresh, "memory-refresh") { refresh() }
        HistoryViews.button(body, R.string.memory_export, "memory-export") {
            AlertDialog.Builder(this).setMessage(R.string.memory_export_notice).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.memory_export) { _, _ -> runCatching {
                    startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_TITLE, "ai-agent-memory.json"), EXPORT)
                }.onFailure { showError() } }.show()
        }
        HistoryViews.button(body, R.string.memory_import, "memory-import") {
            runCatching { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE), IMPORT) }
                .onFailure { showError() }
        }
        HistoryViews.label(body, getString(R.string.memory_scope))
        val ids = listOf(null) + (scopes + rows.map { it.scope }).distinct().sorted()
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val spinner = Spinner(this).apply {
            contentDescription = getString(R.string.memory_scope)
            adapter = ArrayAdapter(this@MemoryActivity, R.layout.item_spinner_choice, ids.map { it ?: getString(R.string.history_all) })
            setSelection(ids.indexOf(filter).coerceAtLeast(0))
        }
        body.addView(spinner, LinearLayout.LayoutParams(-1, -2)); body.addView(list, LinearLayout.LayoutParams(-1, -2))
        fun render() {
            list.removeAllViews()
            val visible = rows.filter { filter == null || it.scope == filter }.sortedWith(compareBy<MemoryEntry> { it.scope }.thenBy { it.key })
            if (visible.isEmpty()) HistoryViews.label(list, getString(R.string.memory_empty))
            visible.forEach { row -> list.addView(Button(this).apply {
                tag = "memory-entry-${row.scope}:${row.key}"; isAllCaps = false
                text = getString(R.string.memory_item, row.key, row.scope, AgentJson.truncate(row.value, 160))
                setOnClickListener { selected = row; draft = row.value; showEditor() }
            }, LinearLayout.LayoutParams(-1, -2)) }
            tint(list)
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { filter = ids[position]; render() }
        }
        render(); tint(body)
    }
    private fun describe(row: MemoryEntry) {
        HistoryViews.label(body, getString(R.string.memory_identity, row.key, row.scope), true)
        HistoryViews.label(body, getString(R.string.memory_source, row.sourceRunId))
        HistoryViews.label(body, getString(R.string.memory_created, HistoryViews.date(this, row.createdAt)))
        HistoryViews.label(body, getString(R.string.memory_updated, HistoryViews.date(this, row.updatedAt)))
    }
    private fun showEditor() {
        val row = selected ?: return
        header(); describe(row)
        val caption = HistoryViews.label(body, getString(R.string.memory_value))
        editor = EditText(this).apply {
            id = View.generateViewId(); tag = "memory-value"; caption.labelFor = id
            minLines = 3; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            filters = arrayOf(InputFilter.LengthFilter(8192)); setText(draft ?: row.value)
            if (Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            body.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        HistoryViews.button(body, R.string.memory_save, "memory-save") {
            val changed = row.copy(value = editor!!.text.toString())
            if (runCatching { MemoryCodec.preference(changed.key, changed.value) }.isFailure) { showError(); return@button }
            AlertDialog.Builder(this).setMessage(R.string.memory_save_confirm).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.memory_save) { _, _ -> save(changed, row, false) { selected = null; draft = null; refresh() } }.show()
        }
        HistoryViews.button(body, R.string.memory_delete, "memory-delete") {
            AlertDialog.Builder(this).setMessage(R.string.memory_delete_confirm).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.memory_delete) { _, _ ->
                    busy = true; enable(false)
                    query("delete", jsonObject("entry" to MemoryCodec.entry(row))) { busy = false; selected = null; draft = null; refresh() }
                }.show()
        }
        tint(body)
    }
    private fun showImport() {
        header()
        val row = imports[importIndex]
        HistoryViews.label(body, getString(R.string.memory_import_progress, importIndex + 1, imports.size), true)
        describe(row); HistoryViews.label(body, getString(R.string.memory_value)); HistoryViews.label(body, row.value)
        val before = rows.find { it.identity == row.identity }
        before?.let { HistoryViews.label(body, getString(R.string.memory_replace)); HistoryViews.label(body, it.value) }
        val available = row.scope in scopes
        if (!available) HistoryViews.label(body, getString(R.string.memory_scope_missing))
        HistoryViews.button(body, R.string.memory_accept, "memory-accept") { save(row, before, true) { nextImport() } }.isEnabled = available
        HistoryViews.button(body, R.string.memory_skip, "memory-skip") { nextImport() }
        HistoryViews.button(body, R.string.memory_cancel_import, "memory-cancel-import") { imports = emptyList(); importIndex = 0; refresh() }
        tint(body)
    }
    private fun nextImport() {
        importIndex++
        if (importIndex >= imports.size) { imports = emptyList(); importIndex = 0 }
        refresh()
    }
    private fun save(row: MemoryEntry, before: MemoryEntry?, imported: Boolean, complete: () -> Unit) {
        if (busy) return
        busy = true; enable(false)
        query("save", jsonObject("entry" to MemoryCodec.entry(row), "before" to (before?.let(MemoryCodec::entry) ?: JsonNull.INSTANCE), "imported" to imported.json())) {
            busy = false; complete()
        }
    }
    private fun enable(enabled: Boolean) {
        if (enabled) { enabledStates.forEach { (view, previous) -> view.isEnabled = previous }; enabledStates.clear(); return }
        enabledStates.clear()
        fun visit(view: View) { enabledStates += view to view.isEnabled; view.isEnabled = false; if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i)) }
        visit(body)
    }
    private fun showError() { error.setText(R.string.memory_error); (body.parent as? ScrollView)?.smoothScrollTo(0, 0) }
    @Deprecated("Platform document result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data?.takeIf { it.scheme == "content" } ?: return
        if (requestCode == IMPORT) source = uri else if (requestCode == EXPORT) destination = uri
        // A document picker can return while this activity is still started and already bound.
        if (connection.connected) refresh()
    }
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
        query("export") { data ->
            files.execute {
                val success = runCatching { requireNotNull(contentResolver.openOutputStream(uri, "wt")).use { writeExport(it, data) } }.isSuccess
                main.post { if (!isDestroyed) { destination = null; busy = false; enable(true); error.setText(if (success) R.string.history_exported else R.string.memory_error) } }
            }
        }
    }
    companion object {
        private const val IMPORT = 30
        private const val EXPORT = 31
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
