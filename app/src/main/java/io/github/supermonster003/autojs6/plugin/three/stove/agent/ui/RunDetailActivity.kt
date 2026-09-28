package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.scripts.DynamicScriptRegistration
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.ModelRef
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.RunHistoryCodec
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import java.io.OutputStream
import java.util.concurrent.Executors

/** One task: summary, result and a step timeline with parameters, observations and generated scripts. */
class RunDetailActivity : HostAppearanceActivity() {
    private lateinit var history: HistoryConnection
    private lateinit var scaffold: Scaffold
    private lateinit var message: TextView
    internal lateinit var summary: LinearLayout
    internal lateinit var result: LinearLayout
    private lateinit var timeline: RunTimeline
    private lateinit var truncated: TextView
    private val main = Handler(Looper.getMainLooper())
    private val files = Executors.newSingleThreadExecutor()
    internal val expanded = linkedSetOf<Int>()
    internal val expandedSources = linkedSetOf<Int>()
    internal val saveScriptButtons = mutableListOf<MaterialButton>()
    /** The last opened dialog, exposed for instrumentation. */
    internal var prompt: AlertDialog? = null
    private var previous = ""
    private var row: JsonObject? = null
    private var visible = false
    private var touch = true
    internal var writing = false
    private var destination: Uri? = null
    private var scriptDestination: Uri? = null
    internal var scriptStep: Int? = null
    private var savedScroll = 0
    /** Follows new steps of a running task while the reader stays at the end of the page (roadmap P16). */
    private var follow: AutoScroll? = null
    private var id = ""
    private val poll = Runnable { refresh() }
    private val exportDocument = registerForActivityResult(SaveDocument { exportDocumentIntent(it) }) { uri ->
        destination = uri; if (uri != null) saveDestination()
    }
    internal val scriptDocument = registerForActivityResult(SaveDocument { scriptDocumentIntent(it) }) { uri ->
        scriptDestination = uri; if (uri == null) scriptStep = null else saveScriptDestination()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = runCatching { RunHistoryCodec.id(requireNotNull(intent.getStringExtra("runId"))) }.getOrDefault("")
        expanded.addAll(savedInstanceState?.getIntArray("expanded")?.toList().orEmpty())
        expandedSources.addAll(savedInstanceState?.getIntArray("expandedSources")?.toList().orEmpty())
        savedScroll = savedInstanceState?.getInt("scroll") ?: 0
        destination = savedInstanceState?.getString("destination")?.let(Uri::parse)
        scriptDestination = savedInstanceState?.getString("scriptDestination")?.let(Uri::parse)
        scriptStep = savedInstanceState?.getInt("scriptStep", -1)?.takeIf { it >= 0 }
        scaffold = buildScaffold(getString(R.string.workbench_details), contentPadding = ContentPadding.SCREEN)
        val page = scaffold.content
        message = kit.text("", Ui.TEXT_BODY, palette.danger).apply {
            visibility = View.GONE; tag = "detail-message"; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        page.addView(message, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = kit.dp(Ui.SPACE_SM) })
        summary = kit.card().also { page.addView(it, kit.cardParams()) }
        result = kit.card().apply { visibility = View.GONE }.also { page.addView(it, kit.cardParams()) }
        page.addView(kit.sectionHeader(getString(R.string.history_timeline)).apply { setPaddingRelative(kit.dp(Ui.SPACE_XS), kit.dp(Ui.SPACE_MD), 0, kit.dp(Ui.SPACE_XS)) })
        timeline = RunTimeline(kit, details = this::stepDetails)
        page.addView(kit.card().apply { addView(timeline.view, LinearLayout.LayoutParams(-1, -2)) }, kit.cardParams())
        truncated = caption(getString(R.string.history_truncated)).apply { visibility = View.GONE }
        page.addView(truncated)
        setContentView(scaffold.root)
        follow = scaffold.scroll?.let { AutoScroll(it) }
        history = HistoryConnection(this) { refresh() }
        if (id.isEmpty()) finish()
    }
    override fun onStart() { super.onStart(); visible = true; history.start() }
    override fun onStop() { visible = false; main.removeCallbacks(poll); history.stop(); writing = false; prompt?.dismiss(); prompt = null; super.onStop() }
    override fun onDestroy() { history.close(); files.shutdown(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray("expanded", expanded.toIntArray()); outState.putInt("scroll", scaffold.scroll?.scrollY ?: 0)
        outState.putIntArray("expandedSources", expandedSources.toIntArray())
        scriptDestination?.let { outState.putString("scriptDestination", it.toString()) }
        scriptStep?.let { outState.putInt("scriptStep", it) }
        destination?.let { outState.putString("destination", it.toString()) }; super.onSaveInstanceState(outState)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, R.id.detail_use_model, 0, R.string.history_use_model).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(0, R.id.detail_share, 1, R.string.history_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(0, R.id.detail_export, 2, R.string.history_export).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(0, R.id.detail_delete, 3, R.string.history_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val value = row
        menu.findItem(R.id.detail_use_model)?.isVisible = value?.getAsJsonObject("model") != null
        menu.findItem(R.id.detail_share)?.isEnabled = value?.let { ShareSummary.text(this, it) } != null
        menu.findItem(R.id.detail_export)?.isEnabled = value != null && !writing
        menu.findItem(R.id.detail_delete)?.isEnabled = value != null && !WorkbenchText.active(value)
        return super.onPrepareOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.detail_use_model -> useModel()
            R.id.detail_share -> share()
            R.id.detail_export -> prompt = kit.confirmDialog(getString(R.string.history_export), getString(R.string.history_export_note),
                getString(R.string.history_export)) { runCatching { exportDocument.launch("agent-$id.json") }.onFailure { showError() } }
            R.id.detail_delete -> prompt = kit.confirmDialog(getString(R.string.history_delete), getString(R.string.history_delete_confirm),
                getString(R.string.history_delete), destructive = true) { history.query("delete", id) { it.onSuccess { finish() }.onFailure { showError() } } }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun refresh() {
        if (!visible || id.isEmpty()) return
        history.query("get", id, touch) { result ->
            result.onSuccess { value -> touch = false; render(value); saveDestination(); saveScriptDestination() }.onFailure { showError() }
            if (visible) main.postDelayed(poll, if (row?.let(WorkbenchText::active) == true) 500 else 2000)
        }
    }
    private fun render(value: JsonObject) {
        row = value
        val key = value.toString()
        if (key == previous) return
        previous = key
        renderSummary(value); renderResult(value)
        timeline.render(value.getAsJsonArray("steps"))
        truncated.visibility = if (value.flag("truncated") == true) View.VISIBLE else View.GONE
        invalidateOptionsMenu()
        if (savedScroll > 0) { val position = savedScroll; savedScroll = 0; scaffold.scroll?.post { scaffold.scroll?.scrollTo(0, position) } }
        // A running task keeps the newest step in view unless the reader scrolled up to read earlier steps.
        else if (WorkbenchText.active(value)) follow?.contentChanged()
    }

    /** Prefills the workbench with this task's goal and preset; the model stays the shared current choice. */
    internal fun rerun(value: JsonObject) {
        startActivity(Intent(this, LauncherActivity::class.java).putExtra("rerunGoal", value.string("goal")).putExtra("rerunPreset", value.string("preset")))
    }
    /** The system share sheet receives only the redacted result text; a missing result leaves the item disabled. */
    internal fun share(): Boolean {
        val text = row?.let { ShareSummary.text(this, it) } ?: return false
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text).putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
        return runCatching { startActivity(Intent.createChooser(send, getString(R.string.history_share))) }.onFailure { showError() }.isSuccess
    }
    private fun useModel() {
        val model = row?.getAsJsonObject("model") ?: return
        val ref = runCatching { ModelRef(requireNotNull(model.string("targetId")), requireNotNull(model.string("name"))) }.getOrNull() ?: return
        runCatching { ModelSelection.choose(this, ref) }
            .onSuccess { kit.snackbar(scaffold.root, getString(R.string.history_model_selected, ref.name)) }.onFailure { showError() }
    }

    private fun saveDestination() {
        val uri = destination ?: return
        if (writing) return
        writing = true; invalidateOptionsMenu()
        history.query("export", id) { result ->
            result.onFailure { writing = false; destination = null; showError(); invalidateOptionsMenu() }.onSuccess { data ->
                destination = null
                files.execute {
                    val success = runCatching { requireNotNull(contentResolver.openOutputStream(uri, "wt")).use { writeExport(it, data) } }.isSuccess
                    main.post { writing = false; if (!isDestroyed) {
                        invalidateOptionsMenu(); notify(if (success) R.string.history_exported else R.string.workbench_request_failed, success)
                    } }
                }
            }
        }
    }
    private fun saveScriptDestination() {
        val uri = scriptDestination ?: return
        if (writing) return
        val step = row?.getAsJsonArray("steps")?.firstOrNull { it.asJsonObject.number("index") == scriptStep?.toLong() }?.asJsonObject
        val registration = step?.let(DynamicScriptRegistration::fromStep)
        scriptDestination = null; scriptStep = null
        if (registration == null) { showError(); return }
        writing = true; invalidateOptionsMenu(); saveScriptButtons.forEach { it.isEnabled = false }
        files.execute {
            val success = runCatching {
                requireNotNull(contentResolver.openOutputStream(uri, "w")).use {
                    it.write(registration.text.toByteArray(Charsets.UTF_8)); it.flush()
                }
            }.isSuccess
            main.post {
                writing = false
                if (!isDestroyed) {
                    invalidateOptionsMenu(); saveScriptButtons.forEach { it.isEnabled = true }
                    notify(if (success) R.string.script_dynamic_saved else R.string.workbench_request_failed, success)
                }
            }
        }
    }
    private fun notify(text: Int, success: Boolean) {
        if (success) { message.visibility = View.GONE; kit.snackbar(scaffold.root, getString(text)) }
        else { message.setText(text); message.visibility = View.VISIBLE }
    }
    internal fun showError() { message.setText(R.string.history_unavailable); message.visibility = View.VISIBLE }

    /** A system document picker for a new file; only content URIs are accepted. */
    private class SaveDocument(private val intent: (String) -> Intent) : ActivityResultContract<String, Uri?>() {
        override fun createIntent(context: Context, input: String) = intent(input)
        override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
            intent?.data?.takeIf { resultCode == Activity.RESULT_OK && it.scheme == "content" }
    }
    companion object {
        internal fun exportDocumentIntent(fileName: String) = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE, fileName)
        internal fun scriptDocumentIntent(fileName: String) = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE).setType("text/javascript").putExtra(Intent.EXTRA_TITLE, fileName)
        internal fun writeExport(output: OutputStream, redacted: JsonObject) {
            require(redacted.flag("redacted") == true)
            output.write(Formats.pretty(redacted).toByteArray(Charsets.UTF_8)); output.flush()
        }
    }
}
