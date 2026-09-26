package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.*
import android.widget.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.DynamicScriptRegistration
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
    private val expandedSources = linkedSetOf<Int>()
    private val saveScriptButtons = mutableListOf<Button>()
    private var previous = ""
    private var row: JsonObject? = null
    private var visible = false
    private var touch = true
    private var writing = false
    private var destination: Uri? = null
    private var scriptDestination: Uri? = null
    private var scriptStep: Int? = null
    private var savedScroll = 0
    private var id = ""
    private val poll = Runnable { refresh() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = runCatching { RunHistoryCodec.id(requireNotNull(intent.getStringExtra("runId"))) }.getOrDefault("")
        expanded.addAll(savedInstanceState?.getIntArray("expanded")?.toList().orEmpty())
        expandedSources.addAll(savedInstanceState?.getIntArray("expandedSources")?.toList().orEmpty())
        savedScroll = savedInstanceState?.getInt("scroll") ?: 0
        destination = savedInstanceState?.getString("destination")?.let(Uri::parse)
        scriptDestination = savedInstanceState?.getString("scriptDestination")?.let(Uri::parse)
        scriptStep = savedInstanceState?.getInt("scriptStep", -1)?.takeIf { it >= 0 }
        val root = AgentUi.column(this, 0).apply { setPadding(0, 0, 0, 0); layoutDirection = resources.configuration.layoutDirection }
        error = HistoryViews.label(root, "")
        body = HistoryViews.column(this)
        scroll = ScrollView(this).apply { addView(body) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(AgentUi.screen(this, getString(R.string.workbench_details), root, scroll = false))
        history = HistoryConnection(this) { refresh() }
        if (id.isEmpty()) finish()
        tint(root)
    }
    override fun onStart() { super.onStart(); visible = true; history.start() }
    override fun onStop() { visible = false; main.removeCallbacks(poll); history.stop(); writing = false; super.onStop() }
    override fun onDestroy() { history.close(); files.shutdown(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray("expanded", expanded.toIntArray()); outState.putInt("scroll", scroll.scrollY)
        outState.putIntArray("expandedSources", expandedSources.toIntArray())
        scriptDestination?.let { outState.putString("scriptDestination", it.toString()) }
        scriptStep?.let { outState.putInt("scriptStep", it) }
        destination?.let { outState.putString("destination", it.toString()) }; super.onSaveInstanceState(outState)
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
        val position = maxOf(savedScroll, scroll.scrollY); savedScroll = 0
        body.removeAllViews(); saveScriptButtons.clear()
        var section = AgentUi.card(body)
        fun label(text: String, title: Boolean = false) = HistoryViews.label(section, text, title)
        label(value.string("goal").orEmpty(), true)
        label(WorkbenchText.state(this, value)); label(HistoryViews.date(this, value.number("startedAt") ?: 0))
        label(getString(R.string.history_preset_value, value.string("preset").orEmpty()))
        if (value.flag("fullAccess") == true) label(getString(R.string.settings_full_access)).apply {
            tag = "full-access"; setTextColor(AgentUi.palette(this@RunDetailActivity).danger)
        }
        label(WorkbenchText.budget(this, value))
        val actions = AgentUi.disclosure(body, R.string.ui_task_options)
        HistoryViews.button(actions, R.string.history_rerun, "rerun") {
            startActivity(Intent(this, LauncherActivity::class.java).putExtra("rerunGoal", value.string("goal")).putExtra("rerunPreset", value.string("preset")))
        }.isEnabled = !WorkbenchText.active(value)
        export = HistoryViews.button(actions, R.string.history_export, "export") {
            AlertDialog.Builder(this).setMessage(R.string.history_export_note).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.history_export) { _, _ ->
                    runCatching { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/json").putExtra(Intent.EXTRA_TITLE, "agent-$id.json"), EXPORT) }.onFailure { showError() }
                }.showStyled()
        }.apply { isEnabled = !writing }
        HistoryViews.button(actions, R.string.history_delete, "delete") {
            AlertDialog.Builder(this).setMessage(R.string.history_delete_confirm).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.history_delete) { _, _ ->
                    history.query("delete", id) { it.onSuccess { finish() }.onFailure { showError() } }
                }.showStyled()
        }.isEnabled = !WorkbenchText.active(value)
        value.getAsJsonObject("result")?.let { result ->
            section = AgentUi.card(body)
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
        AgentUi.section(body, R.string.history_timeline)
        value.getAsJsonArray("steps").forEach { item ->
            section = AgentUi.card(body)
            val step = item.asJsonObject
            val index = step.number("index")!!.toInt()
            label(getString(R.string.task_running, index) + " - " + (step.string("tool") ?: step.string("kind").orEmpty()), true)
            val decision = step.getAsJsonObject("decision")
            val summary = decision.string("reasoning") ?: decision.getAsJsonObject("ask")?.string("question")
                ?: decision.getAsJsonObject("done")?.string("summary") ?: step.string("tool").orEmpty()
            label(getString(R.string.history_decision, summary))
            if (decision.flag("degraded") == true) label(getString(R.string.history_degraded))
            decision.getAsJsonArray("rejections")?.let { codes -> label(getString(R.string.history_rejections, codes.joinToString { it.asString })) }
            val registration = DynamicScriptRegistration.fromStep(step)
            if (registration != null) {
                section.addView(DynamicScriptConfirmationView.create(this, step.getAsJsonObject("arguments"),
                    index in expandedSources, confirmation = false) { if (it) expandedSources.add(index) else expandedSources.remove(index) })
                saveScriptButtons += HistoryViews.button(section, R.string.script_dynamic_save, "save-script-$index") {
                    AlertDialog.Builder(this).setMessage(R.string.script_dynamic_save_note).setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.script_dynamic_save) { _, _ ->
                            scriptStep = index
                            runCatching { startActivityForResult(scriptDocumentIntent(registration.fileName), SAVE_SCRIPT) }
                                .onFailure { scriptStep = null; showError() }
                        }.showStyled()
                }.apply { isEnabled = !writing }
            } else {
                if (step.flag("sourceRedacted") == true) label(getString(R.string.script_dynamic_redacted))
                step["arguments"]?.let { label(getString(R.string.history_arguments, HistoryViews.pretty(it))) }
            }
            step.string("confirmation")?.let { confirmation -> label(getString(R.string.history_confirmation, getString(when (confirmation) {
                "allowed" -> R.string.history_allowed; "denied" -> R.string.history_denied; else -> R.string.history_auto
            }))) }
            label(getString(R.string.history_elapsed, step.number("elapsedMs") ?: 0))
            step["usage"]?.let { label(getString(R.string.history_usage, HistoryViews.pretty(it))) }
            step.string("error")?.let { label(getString(R.string.history_error) + ": " + it) }
            step.string("observation")?.let { observation ->
                val text = label(if (index in expanded) observation else AgentJson.truncate(observation, 240))
                if (observation.toByteArray(Charsets.UTF_8).size > 240) HistoryViews.button(section,
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
        if (requestCode == SAVE_SCRIPT) {
            scriptDestination = if (resultCode == RESULT_OK) data?.data?.takeIf { it.scheme == "content" } else null
            if (scriptDestination == null) scriptStep = null
        }
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
    private fun saveScriptDestination() {
        val uri = scriptDestination ?: return
        if (writing) return
        val step = row?.getAsJsonArray("steps")?.firstOrNull { it.asJsonObject.number("index") == scriptStep?.toLong() }?.asJsonObject
        val registration = step?.let(DynamicScriptRegistration::fromStep)
        scriptDestination = null; scriptStep = null
        if (registration == null) { showError(); return }
        writing = true; export.isEnabled = false; saveScriptButtons.forEach { it.isEnabled = false }
        files.execute {
            val success = runCatching {
                requireNotNull(contentResolver.openOutputStream(uri, "w")).use {
                    it.write(registration.text.toByteArray(Charsets.UTF_8)); it.flush()
                }
            }.isSuccess
            main.post {
                writing = false
                if (!isDestroyed) {
                    export.isEnabled = true; saveScriptButtons.forEach { it.isEnabled = true }
                    error.setText(if (success) R.string.script_dynamic_saved else R.string.workbench_request_failed)
                }
            }
        }
    }
    private fun showError() { error.setText(R.string.history_unavailable) }
    companion object {
        private const val EXPORT = 20
        private const val SAVE_SCRIPT = 21
        internal fun scriptDocumentIntent(fileName: String) = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE).setType("text/javascript").putExtra(Intent.EXTRA_TITLE, fileName)
        internal fun writeExport(output: OutputStream, redacted: JsonObject) {
            require(redacted.flag("redacted") == true)
            output.write(HistoryViews.pretty(redacted).toByteArray(Charsets.UTF_8)); output.flush()
        }
    }
}
