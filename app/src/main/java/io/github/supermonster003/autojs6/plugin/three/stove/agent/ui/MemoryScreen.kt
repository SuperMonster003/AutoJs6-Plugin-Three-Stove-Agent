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

/** Memory screen construction (roadmap P11): the list, entry cards, editor and import review; MemoryActivity keeps state and events. */

internal fun MemoryActivity.showList() {
    reset(bottom = false)
    kit.caption(page, getString(R.string.memory_note))
    page.addView(LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(kit.tonalButton(getString(R.string.memory_import), "memory-import") {
            runCatching { importDocument.launch(arrayOf("application/json")) }.onFailure { showError() }
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
        addView(kit.textButton(getString(R.string.memory_export), "memory-export") {
            prompt = kit.confirmDialog(getString(R.string.memory_export), getString(R.string.memory_export_notice), getString(R.string.memory_export)) {
                runCatching { exportDocument.launch("three-stove-agent-memory.json") }.onFailure { showError() }
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

internal fun MemoryActivity.entryCard(row: MemoryEntry): View = kit.card(interactive = true).apply {
    tag = "memory-entry-${row.scope}:${row.key}"
    addView(LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(kit.text(row.key, Ui.TEXT_ITEM, medium = true).apply { textAlignment = View.TEXT_ALIGNMENT_VIEW_START }, LinearLayout.LayoutParams(0, -2, 1f))
        // A scope is an unbounded preset name. Measured after the weighted key, an unbounded badge would take the
        // whole row on a 360 dp screen and push the key out, so it ellipsizes and carries the full name itself.
        addView(kit.badge(row.scope, if (row.scope == "global") Tone.ACCENT else Tone.NEUTRAL).apply {
            Ui.truncatable(this); maxLines = 1; ellipsize = TextUtils.TruncateAt.END; maxWidth = kit.dp(160); contentDescription = row.scope
        }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = kit.dp(Ui.SPACE_SM) })
    })
    addView(kit.text(AgentJson.truncate(row.value, 160), Ui.TEXT_SECONDARY, palette.muted).apply {
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, kit.dp(Ui.SPACE_XS), 0, 0)
    })
    contentDescription = getString(R.string.memory_item, row.key, row.scope, AgentJson.truncate(row.value, 160))
    setOnClickListener { selected = row; draft = row.value; showEditor() }
}

internal fun MemoryActivity.describe(row: MemoryEntry) {
    page.addView(kit.card().apply {
        addView(kit.text(row.key, Ui.TEXT_TITLE, medium = true).apply { setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START })
        addView(kit.badge(row.scope, if (row.scope == "global") Tone.ACCENT else Tone.NEUTRAL), LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
        kit.caption(this, getString(R.string.memory_source, row.sourceRunId))
        kit.caption(this, getString(R.string.memory_created, Formats.date(context, row.createdAt)))
        kit.caption(this, getString(R.string.memory_updated, Formats.date(context, row.updatedAt)))
    }, kit.cardParams(topDp = Ui.SPACE_SM))
}

internal fun MemoryActivity.showEditor() {
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

internal fun MemoryActivity.showImport() {
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
