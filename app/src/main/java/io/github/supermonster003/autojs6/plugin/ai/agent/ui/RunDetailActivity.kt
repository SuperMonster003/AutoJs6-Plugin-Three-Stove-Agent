package io.github.supermonster003.autojs6.plugin.ai.agent.ui

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
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.DynamicScriptRegistration
import io.github.supermonster003.autojs6.plugin.ai.agent.store.ModelRef
import io.github.supermonster003.autojs6.plugin.ai.agent.store.RunHistoryCodec
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.*
import java.io.OutputStream
import java.util.concurrent.Executors

/** One task: summary, result and a step timeline with parameters, observations and generated scripts. */
class RunDetailActivity : HostAppearanceActivity() {
    private lateinit var history: HistoryConnection
    private lateinit var scaffold: Scaffold
    private lateinit var message: TextView
    private lateinit var summary: LinearLayout
    private lateinit var result: LinearLayout
    private lateinit var timeline: RunTimeline
    private lateinit var truncated: TextView
    private val main = Handler(Looper.getMainLooper())
    private val files = Executors.newSingleThreadExecutor()
    private val expanded = linkedSetOf<Int>()
    private val expandedSources = linkedSetOf<Int>()
    private val saveScriptButtons = mutableListOf<MaterialButton>()
    /** The last opened dialog, exposed for instrumentation. */
    internal var prompt: AlertDialog? = null; private set
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
    private val exportDocument = registerForActivityResult(SaveDocument { exportDocumentIntent(it) }) { uri ->
        destination = uri; if (uri != null) saveDestination()
    }
    private val scriptDocument = registerForActivityResult(SaveDocument { scriptDocumentIntent(it) }) { uri ->
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
        timeline = RunTimeline(kit, details = ::stepDetails)
        page.addView(kit.card().apply { addView(timeline.view, LinearLayout.LayoutParams(-1, -2)) }, kit.cardParams())
        truncated = caption(getString(R.string.history_truncated)).apply { visibility = View.GONE }
        page.addView(truncated)
        setContentView(scaffold.root)
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
        menu.add(0, R.id.detail_export, 1, R.string.history_export).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(0, R.id.detail_delete, 2, R.string.history_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val value = row
        menu.findItem(R.id.detail_use_model)?.isVisible = value?.getAsJsonObject("model") != null
        menu.findItem(R.id.detail_export)?.isEnabled = value != null && !writing
        menu.findItem(R.id.detail_delete)?.isEnabled = value != null && !WorkbenchText.active(value)
        return super.onPrepareOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.detail_use_model -> useModel()
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
    }
    private fun renderSummary(value: JsonObject) {
        summary.removeAllViews()
        val (tone, _) = WorkbenchText.tone(value)
        summary.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(kit.badge(WorkbenchText.state(context, value), tone))
            if (value.flag("fullAccess") == true) addView(kit.badge(getString(R.string.settings_full_access), Tone.DANGER).apply { tag = "full-access" },
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = kit.dp(Ui.SPACE_XS) })
            addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
            addView(kit.text(Formats.date(context, value.number("startedAt") ?: 0), Ui.TEXT_CAPTION, palette.muted))
        })
        summary.addView(kit.text(value.string("goal").orEmpty(), Ui.TEXT_TITLE, medium = true).apply {
            setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, kit.dp(Ui.SPACE_MD), 0, kit.dp(Ui.SPACE_XS))
        })
        value.getAsJsonObject("model")?.string("name")?.let { summary.addView(caption(getString(R.string.history_model_value, it))) }
        summary.addView(caption(getString(R.string.history_preset_value, value.string("preset")?.let {
            if (it == "default") getString(R.string.workbench_default_preset) else it
        }.orEmpty())))
        value.getAsJsonObject("result")?.number("durationMs")?.let { summary.addView(caption(getString(R.string.history_elapsed, it))) }
        summary.addView(caption(WorkbenchText.budget(this, value)))
        val active = WorkbenchText.active(value)
        summary.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(kit.tonalButton(getString(R.string.workbench_run_again), "rerun") { rerun(value, false) }.apply { isEnabled = !active },
                LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
            addView(kit.textButton(getString(R.string.workbench_retry_model), "retry-model") { rerun(value, true) }.apply { isEnabled = !active },
                LinearLayout.LayoutParams(0, -2, 1f))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
    }
    private fun renderResult(value: JsonObject) {
        val data = value.getAsJsonObject("result")
        result.visibility = if (data == null) View.GONE else View.VISIBLE
        result.removeAllViews()
        data ?: return
        result.addView(kit.text(getString(R.string.history_result), Ui.TEXT_SECTION, palette.accent, medium = true))
        result.addView(kit.text(data.string("summary").orEmpty(), Ui.TEXT_BODY).apply {
            setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, kit.dp(Ui.SPACE_XS), 0, 0)
        })
        for ((field, title) in listOf("evidence" to R.string.history_evidence, "unfinished" to R.string.history_unfinished,
            "script" to R.string.history_script_result, "error" to R.string.history_error)) {
            val item = data[field]?.takeUnless { it.isJsonNull || it.isJsonArray && it.asJsonArray.isEmpty } ?: continue
            result.addView(kit.text(getString(title), Ui.TEXT_SECTION, palette.muted, medium = true).apply { setPaddingRelative(0, kit.dp(Ui.SPACE_MD), 0, 0) })
            when {
                item.isJsonArray -> item.asJsonArray.forEach { result.addView(body("- " + it.asString)) }
                field == "script" -> result.addView(body(Formats.pretty(item.asJsonObject["result"] ?: JsonNull.INSTANCE)).apply { typeface = Ui.monospace })
                else -> result.addView(body(if (item.isJsonPrimitive) item.asString else Formats.pretty(item)))
            }
        }
        WorkbenchText.usage(this, data.getAsJsonObject("usage"))?.let { result.addView(caption(it)) }
    }

    /** Details under one timeline step; rebuilt only when that step changes. */
    private fun stepDetails(step: JsonObject, box: LinearLayout) {
        val index = step.number("index")?.toInt() ?: return
        val decision = step.getAsJsonObject("decision")
        if (decision?.flag("degraded") == true) box.addView(caption(getString(R.string.history_degraded)))
        decision?.getAsJsonArray("rejections")?.let { codes -> box.addView(caption(getString(R.string.history_rejections, codes.joinToString { it.asString }))) }
        val registration = DynamicScriptRegistration.fromStep(step)
        if (registration != null) {
            box.addView(DynamicScriptConfirmationView.create(this, step.getAsJsonObject("arguments"), index in expandedSources, confirmation = false, kit = kit) {
                if (it) expandedSources.add(index) else expandedSources.remove(index)
            })
            saveScriptButtons += kit.tonalButton(getString(R.string.script_dynamic_save), "save-script-$index") {
                prompt = kit.confirmDialog(getString(R.string.script_dynamic_save), getString(R.string.script_dynamic_save_note), getString(R.string.script_dynamic_save)) {
                    scriptStep = index
                    runCatching { scriptDocument.launch(registration.fileName) }.onFailure { scriptStep = null; showError() }
                }
            }.apply { isEnabled = !writing }.also { box.addView(it, LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) }) }
        } else {
            if (step.flag("sourceRedacted") == true) box.addView(caption(getString(R.string.script_dynamic_redacted)))
            step["arguments"]?.takeIf { it.isJsonObject && it.asJsonObject.size() > 0 }?.let {
                box.addView(kit.parameterTable(ArgumentRows.rows(it)), LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
            }
        }
        val facts = listOfNotNull(step.string("confirmation")?.let { getString(R.string.history_confirmation, getString(when (it) {
            "allowed" -> R.string.history_allowed; "denied" -> R.string.history_denied; else -> R.string.history_auto
        })) }, getString(R.string.history_elapsed, step.number("elapsedMs") ?: 0), WorkbenchText.usage(this, step.getAsJsonObject("usage")))
        box.addView(caption(facts.joinToString(" · ")))
        step.string("error")?.let { box.addView(caption(getString(R.string.history_error) + ": " + it).apply { setTextColor(palette.danger) }) }
        step.string("observation")?.let { observation ->
            val long = observation.utf8Size() > 240
            val text = body(if (index in expanded || !long) observation else AgentJson.truncate(observation, 240)).apply {
                background = kit.roundedFill(palette.surfaceVariant, Ui.RADIUS_CONTROL)
                setPaddingRelative(kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_SM), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_SM))
            }
            box.addView(text, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
            if (long) {
                lateinit var toggle: MaterialButton
                toggle = kit.textButton(getString(if (index in expanded) R.string.history_collapse else R.string.history_expand), "observation-$index") {
                    if (!expanded.add(index)) expanded.remove(index)
                    text.text = if (index in expanded) observation else AgentJson.truncate(observation, 240)
                    toggle.setText(if (index in expanded) R.string.history_collapse else R.string.history_expand)
                }
                box.addView(toggle, LinearLayout.LayoutParams(-2, -2))
            }
        }
    }

    private fun rerun(value: JsonObject, chooseModel: Boolean) {
        startActivity(Intent(this, LauncherActivity::class.java).putExtra("rerunGoal", value.string("goal")).putExtra("rerunPreset", value.string("preset"))
            .putExtra(LauncherActivity.EXTRA_OPEN_MODELS, chooseModel))
    }
    private fun useModel() {
        val model = row?.getAsJsonObject("model") ?: return
        val ref = runCatching { ModelRef(requireNotNull(model.string("targetId")), requireNotNull(model.string("name"))) }.getOrNull() ?: return
        runCatching { ModelSelection.choose(this, ref) }
            .onSuccess { kit.snackbar(scaffold.root, getString(R.string.history_model_selected, ref.name)) }.onFailure { showError() }
    }
    private fun caption(text: String) = kit.note(text)
    private fun body(text: String) = kit.paragraph(text)

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
    private fun showError() { message.setText(R.string.history_unavailable); message.visibility = View.VISIBLE }

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
