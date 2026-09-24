package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.*
import android.widget.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.RunHistoryCodec
import java.io.OutputStream
import java.util.concurrent.Executors

class RunDetailActivity : HostAppearanceActivity() {
    private lateinit var history: HistoryConnection
    private lateinit var body: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var error: TextView
    private lateinit var export: Button
    private val main = Handler(Looper.getMainLooper())
    private val files = Executors.newSingleThreadExecutor()
    private val expanded = linkedSetOf<Int>()
    private var previous = ""
    private var row: JsonObject? = null
    private var visible = false
    private var touch = true
    private var writing = false
    private var destination: Uri? = null
    private var savedScroll = 0
    private var id = ""
    private val poll = Runnable { refresh() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = runCatching { RunHistoryCodec.id(requireNotNull(intent.getStringExtra("runId"))) }.getOrDefault("")
        expanded.addAll(savedInstanceState?.getIntArray("expanded")?.toList().orEmpty())
        savedScroll = savedInstanceState?.getInt("scroll") ?: 0
        destination = savedInstanceState?.getString("destination")?.let(Uri::parse)
        val root = HistoryViews.column(this).apply { fitsSystemWindows = true; layoutDirection = resources.configuration.layoutDirection }
        HistoryViews.button(root, R.string.workbench_back, "back") { finish() }
        error = HistoryViews.label(root, "")
        body = HistoryViews.column(this)
        scroll = ScrollView(this).apply { addView(body) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        history = HistoryConnection(this) { refresh() }
        if (id.isEmpty()) finish()
        tint(root)
    }
    override fun onStart() { super.onStart(); visible = true; history.start() }
    override fun onStop() { visible = false; main.removeCallbacks(poll); history.stop(); writing = false; super.onStop() }
    override fun onDestroy() { history.close(); files.shutdown(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray("expanded", expanded.toIntArray()); outState.putInt("scroll", scroll.scrollY)
        destination?.let { outState.putString("destination", it.toString()) }; super.onSaveInstanceState(outState)
    }
    private fun refresh() {
        if (!visible || id.isEmpty()) return
        history.query("get", id, touch) { result ->
            result.onSuccess { value -> touch = false; render(value); saveDestination() }.onFailure { showError() }
            if (visible) main.postDelayed(poll, if (row?.let(WorkbenchText::active) == true) 500 else 2000)
        }
    }
    private fun render(value: JsonObject) {
        row = value
        val key = value.toString()
        if (key == previous) return
        previous = key
        val position = maxOf(savedScroll, scroll.scrollY); savedScroll = 0
        body.removeAllViews()
        fun label(text: String, title: Boolean = false) = HistoryViews.label(body, text, title)
        label(value.string("goal").orEmpty(), true)
        label(WorkbenchText.state(this, value)); label(HistoryViews.date(this, value.number("startedAt") ?: 0))
        label(getString(R.string.history_preset_value, value.string("preset").orEmpty()))
        label(WorkbenchText.budget(this, value))
        HistoryViews.button(body, R.string.history_rerun, "rerun") {
            startActivity(Intent(this, LauncherActivity::class.java).putExtra("rerunGoal", value.string("goal")).putExtra("rerunPreset", value.string("preset")))
        }.isEnabled = !WorkbenchText.active(value)
        export = HistoryViews.button(body, R.string.history_export, "export") {
            AlertDialog.Builder(this).setMessage(R.string.history_export_note).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.history_export) { _, _ ->
                    runCatching { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/json").putExtra(Intent.EXTRA_TITLE, "agent-$id.json"), EXPORT) }.onFailure { showError() }
                }.show()
        }.apply { isEnabled = !writing }
        HistoryViews.button(body, R.string.history_delete, "delete") {
            AlertDialog.Builder(this).setMessage(R.string.history_delete_confirm).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.history_delete) { _, _ ->
                    history.query("delete", id) { it.onSuccess { finish() }.onFailure { showError() } }
                }.show()
        }.isEnabled = !WorkbenchText.active(value)
        value.getAsJsonObject("result")?.let { result ->
            label(getString(R.string.history_result), true)
            label(WorkbenchText.state(this, jsonObject("state" to result["status"])))
            label(result.string("summary").orEmpty())
            for ((field, title) in listOf("evidence" to R.string.history_evidence, "unfinished" to R.string.history_unfinished,
                "script" to R.string.history_script_result, "error" to R.string.history_error)) {
                result[field]?.let { item ->
                    label(getString(title), true)
                    when {
                        item.isJsonArray -> item.asJsonArray.forEach { label(it.asString) }
                        field == "script" -> label(HistoryViews.pretty(item.asJsonObject["result"] ?: JsonNull.INSTANCE))
                        else -> label(HistoryViews.pretty(item))
                    }
                }
            }
            result.number("durationMs")?.let { label(getString(R.string.history_elapsed, it)) }
            result["usage"]?.let { label(getString(R.string.history_usage, HistoryViews.pretty(it))) }
        }
        label(getString(R.string.history_timeline), true)
        value.getAsJsonArray("steps").forEach { item ->
            val step = item.asJsonObject
            val index = step.number("index")!!.toInt()
            label(getString(R.string.task_running, index) + " - " + (step.string("tool") ?: step.string("kind").orEmpty()), true)
            val decision = step.getAsJsonObject("decision")
            val summary = decision.string("reasoning") ?: decision.getAsJsonObject("ask")?.string("question")
                ?: decision.getAsJsonObject("done")?.string("summary") ?: step.string("tool").orEmpty()
            label(getString(R.string.history_decision, summary))
            if (decision.flag("degraded") == true) label(getString(R.string.history_degraded))
            step["arguments"]?.let { label(getString(R.string.history_arguments, HistoryViews.pretty(it))) }
            step.string("confirmation")?.let { confirmation -> label(getString(R.string.history_confirmation, getString(when (confirmation) {
                "allowed" -> R.string.history_allowed; "denied" -> R.string.history_denied; else -> R.string.history_auto
            }))) }
            label(getString(R.string.history_elapsed, step.number("elapsedMs") ?: 0))
            step["usage"]?.let { label(getString(R.string.history_usage, HistoryViews.pretty(it))) }
            step.string("error")?.let { label(getString(R.string.history_error) + ": " + it) }
            step.string("observation")?.let { observation ->
                val text = label(if (index in expanded) observation else AgentJson.truncate(observation, 240))
                if (observation.toByteArray(Charsets.UTF_8).size > 240) HistoryViews.button(body,
                    if (index in expanded) R.string.history_collapse else R.string.history_expand, "observation-$index") {
                    if (!expanded.add(index)) expanded.remove(index)
                    text.text = if (index in expanded) observation else AgentJson.truncate(observation, 240)
                    body.findViewWithTag<Button>("observation-$index").setText(if (index in expanded) R.string.history_collapse else R.string.history_expand)
                }
            }
        }
        if (value.flag("truncated") == true) label(getString(R.string.history_truncated))
        tint(body); scroll.post { scroll.scrollTo(0, position) }
    }
    @Deprecated("Platform document result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EXPORT && resultCode == RESULT_OK) destination = data?.data?.takeIf { it.scheme == "content" }
    }
    private fun saveDestination() {
        val uri = destination ?: return
        if (writing) return
        writing = true; export.isEnabled = false
        history.query("export", id) { result ->
            result.onFailure { writing = false; destination = null; showError(); export.isEnabled = true }.onSuccess { data ->
                destination = null
                files.execute {
                    val success = runCatching { requireNotNull(contentResolver.openOutputStream(uri, "wt")).use { writeExport(it, data) } }.isSuccess
                    main.post { writing = false; if (!isDestroyed) {
                        export.isEnabled = true
                        error.setText(if (success) R.string.history_exported else R.string.workbench_request_failed)
                    } }
                }
            }
        }
    }
    private fun showError() { error.setText(R.string.history_unavailable) }
    companion object {
        private const val EXPORT = 20
        internal fun writeExport(output: OutputStream, redacted: JsonObject) {
            require(redacted.flag("redacted") == true)
            output.write(HistoryViews.pretty(redacted).toByteArray(Charsets.UTF_8)); output.flush()
        }
    }
}
