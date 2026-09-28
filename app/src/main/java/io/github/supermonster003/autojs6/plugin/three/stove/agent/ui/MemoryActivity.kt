package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
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
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
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
    internal lateinit var page: LinearLayout
    internal lateinit var bar: LinearLayout
    private lateinit var message: TextView
    private val files = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    internal var rows = emptyList<MemoryEntry>()
    internal var scopes = emptyList<String>()
    internal var selected: MemoryEntry? = null
    internal var draft: String? = null
    internal var editor: TextInputEditText? = null
    internal var imports = emptyList<MemoryEntry>()
    internal var importIndex = 0
    internal var filter: String? = null
    internal var query = ""
    private var source: Uri? = null
    private var destination: Uri? = null
    internal var busy = false
    /** The last opened dialog, exposed for instrumentation. */
    internal var prompt: AlertDialog? = null
    private val enabledStates = mutableListOf<Pair<View, Boolean>>()
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

    internal fun request(operation: String, extra: JsonObject = JsonObject(), complete: (JsonObject) -> Unit) {
        extra.addProperty("operation", operation)
        connection.query(extra) { result -> result.onSuccess(complete).onFailure { busy = false; enable(true); showError() } }
    }
    internal fun refresh() {
        request("list") { data ->
            rows = data.getAsJsonArray("entries").map { MemoryCodec.decodeEntry(it.asJsonObject) }
            scopes = data.getAsJsonArray("scopes").map { it.asString }
            when { importIndex < imports.size -> showImport(); selected != null -> showEditor(); else -> showList() }
            if (source != null) readSource() else if (destination != null) writeDestination()
        }
    }
    internal fun reset(bottom: Boolean) {
        enable(true); editor = null; page.removeAllViews(); bar.removeAllViews()
        bar.visibility = if (bottom) View.VISIBLE else View.GONE
        message = kit.text("", Ui.TEXT_BODY, palette.danger).apply {
            visibility = View.GONE; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
        page.addView(message, LinearLayout.LayoutParams(-1, -2))
    }

    internal fun nextImport() {
        importIndex++
        if (importIndex >= imports.size) { imports = emptyList(); importIndex = 0 }
        refresh()
    }
    internal fun save(row: MemoryEntry, before: MemoryEntry?, imported: Boolean, complete: () -> Unit) {
        if (busy) return
        busy = true; enable(false)
        request("save", jsonObject("entry" to MemoryCodec.entry(row), "before" to (before?.let(MemoryCodec::entry) ?: JsonNull.INSTANCE), "imported" to imported.json())) {
            busy = false; complete()
        }
    }
    internal fun enable(enabled: Boolean) {
        if (enabled) { enabledStates.forEach { (view, previous) -> view.isEnabled = previous }; enabledStates.clear(); return }
        enabledStates.clear()
        fun visit(view: View) { enabledStates += view to view.isEnabled; view.isEnabled = false; if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i)) }
        visit(page); visit(bar)
    }
    internal fun showError() { message.setText(R.string.memory_error); message.visibility = View.VISIBLE; scaffold.scroll?.smoothScrollTo(0, 0) }
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
