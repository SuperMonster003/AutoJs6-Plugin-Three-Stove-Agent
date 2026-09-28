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

/** Task details construction (roadmap P11): summary, result and step rendering; RunDetailActivity keeps state and events. */

internal fun RunDetailActivity.renderSummary(value: JsonObject) {
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
    summary.addView(metaTable(listOfNotNull(
        value.getAsJsonObject("model")?.string("name")?.let { getString(R.string.floating_model) to it },
        getString(R.string.workbench_preset) to value.string("preset")?.let { if (it == "default") getString(R.string.workbench_default_preset) else it }.orEmpty(),
        value.getAsJsonObject("result")?.number("durationMs")?.let { getString(R.string.history_elapsed_label) to getString(R.string.history_elapsed_ms, it) },
        getString(R.string.ui_budget) to WorkbenchText.budget(this, value))), LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
    val active = WorkbenchText.active(value)
    summary.addView(kit.tonalButton(getString(R.string.workbench_run_again), "rerun") { rerun(value) }.apply { isEnabled = !active },
        LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
}

internal fun RunDetailActivity.renderResult(value: JsonObject) {
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
internal fun RunDetailActivity.stepDetails(step: JsonObject, box: LinearLayout) {
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

internal fun RunDetailActivity.caption(text: String) = kit.note(text)

/** Key / value rows with aligned columns: the label column keeps its width and long values wrap beside it. */
internal fun RunDetailActivity.metaTable(rows: List<Pair<String, String>>): TableLayout = TableLayout(this).apply {
    tag = "detail-meta"
    setColumnShrinkable(1, true); setColumnStretchable(1, true)
    rows.forEach { (label, text) ->
        addView(TableRow(context).apply {
            addView(kit.text(label, Ui.TEXT_SECONDARY, palette.muted).apply {
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, kit.dp(2), kit.dp(Ui.SPACE_MD), kit.dp(2))
            })
            addView(kit.text(text, Ui.TEXT_SECONDARY).apply {
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, kit.dp(2), 0, kit.dp(2)); setTextIsSelectable(true)
            })
        })
    }
}

internal fun RunDetailActivity.body(text: String) = kit.paragraph(text)
