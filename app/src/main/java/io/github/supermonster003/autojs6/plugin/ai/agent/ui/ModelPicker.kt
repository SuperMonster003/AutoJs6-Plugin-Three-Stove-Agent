package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

/** Select a public target for the next task without mutating a preset or a running task. */
internal class ModelPicker(private val activity: HostAppearanceActivity, private val button: Button, private val changed: () -> Unit) {
    var selectedId: String? = null
    var selectedName: String? = null
    private var targets = emptyList<JsonObject>()
    private var checked = false
    private var loading = false
    private var attached = false
    private var connected = false
    private var generation = 0
    internal var dialog: AlertDialog? = null; private set
    private var list: LinearLayout? = null
    private var search: EditText? = null
    private var message: TextView? = null
    private val connection = PresetConnection(activity) { connected = true; if (attached) refresh() }
    val selectionAvailable get() = selectedId == null || !checked || targets.any { it.string("targetId") == selectedId }
    init { button.setOnClickListener { show() } }
    fun start() { checked = false; connection.start() }
    fun stop() { generation++; loading = false; connected = false; connection.stop(); dialog?.dismiss(); dialog = null; list = null; search = null; message = null }
    fun close() { connection.close() }
    fun attached(value: Boolean) {
        val wasAttached = attached; attached = value
        button.isEnabled = value
        if (value && !wasAttached && connected) refresh()
        if (!value) checked = false
        renderButton()
    }
    private fun refresh() {
        if (!connected || !attached || loading) return
        loading = true; val expected = ++generation; renderList()
        connection.query(jsonObject("operation" to "targets".json())) { result ->
            if (expected != generation) return@query
            loading = false; checked = result.isSuccess
            targets = result.getOrNull()?.getAsJsonArray("targets")?.map { it.asJsonObject }.orEmpty()
            targets.find { it.string("targetId") == selectedId }?.string("displayName")?.let { selectedName = it }
            renderButton(); renderList(); changed()
        }
    }
    fun renderButton() {
        button.text = if (selectedId == null) activity.getString(R.string.ui_choose_model) + "\n" + activity.getString(R.string.ui_model_inherit) else
            (selectedName ?: selectedId).let { if (selectionAvailable) it else activity.getString(R.string.presets_unavailable, it) }
        button.contentDescription = activity.getString(R.string.ui_choose_model) + ", " + button.text
    }
    private fun choose(id: String?, name: String?) {
        selectedId = id; selectedName = name; renderButton(); changed(); dialog?.dismiss()
    }
    @Suppress("DEPRECATION") // API 24-29 still require resize for an IME inside a platform dialog.
    private fun show() {
        val body = AgentUi.column(activity)
        AgentUi.text(body, activity.getString(R.string.ui_model_note), 14)
        search = EditText(activity).apply {
            setHint(R.string.ui_model_search); contentDescription = activity.getString(R.string.ui_model_search)
            inputType = android.text.InputType.TYPE_CLASS_TEXT; maxLines = 1
            filters = arrayOf(android.text.InputFilter.LengthFilter(256))
            body.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        message = AgentUi.text(body, "", 13).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        list = AgentUi.column(activity, 0).also { body.addView(it) }
        AgentUi.action(body, R.string.presets_refresh_models, "refresh-models") { refresh() }
        dialog = AlertDialog.Builder(activity).setTitle(R.string.ui_choose_model)
            .setView(ScrollView(activity).apply { addView(body) }).setNegativeButton(android.R.string.cancel, null).create()
        search!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderList() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        renderList(); AgentUi.style(body); dialog!!.showStyled()
        dialog!!.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        refresh()
    }
    private fun option(parent: LinearLayout, title: String, summary: String, tag: String, selected: Boolean, choose: () -> Unit) {
        AgentUi.row(parent, title, summary, tag, choose).apply {
            isSelected = selected; AgentUi.style(this)
            val mark = if (selected) activity.getDrawable(R.drawable.ic_check)!!.mutate().apply {
                setTint(AgentUi.palette(activity).accent); setBounds(0, 0, AgentUi.dp(activity, 24), AgentUi.dp(activity, 24))
            } else null
            setCompoundDrawablesRelative(null, null, mark, null)
        }
    }
    private fun renderList() {
        val parent = list ?: return
        parent.removeAllViews()
        message?.text = activity.getString(when { loading -> R.string.interaction_loading; !checked -> R.string.presets_models_unavailable
            targets.isEmpty() -> R.string.ui_models_empty; else -> R.string.ui_model_capabilities })
        option(parent, activity.getString(R.string.ui_model_inherit), activity.getString(R.string.ui_model_inherit_note), "model-inherit", selectedId == null) { choose(null, null) }
        val query = search?.text?.toString()?.trim().orEmpty()
        val matching = targets.filter { query.isBlank() || listOf(it.string("displayName"), it.string("providerId"), it.string("targetId")).any { text -> text?.contains(query, true) == true } }
        if (checked && targets.isNotEmpty() && matching.isEmpty()) AgentUi.text(parent, activity.getString(R.string.ui_models_no_match), 14)
        for ((locality, title) in listOf("REMOTE" to R.string.presets_remote, "ON_DEVICE" to R.string.presets_local, "HYBRID" to R.string.presets_hybrid)) {
            val models = matching.filter { it.string("locality") == locality }
            if (models.isEmpty()) continue
            AgentUi.section(parent, title)
            models.forEach { model ->
                val name = model.string("displayName")!!; val id = model.string("targetId")!!
                val details = listOfNotNull(model.string("providerId"),
                    if (model.flag("nativeTools") == true) activity.getString(R.string.ui_model_tools) else null,
                    if (model.flag("vision") == true) activity.getString(R.string.ui_model_vision) else null)
                option(parent, if (id == selectedId) activity.getString(R.string.ui_selected_model, name) else name,
                    details.joinToString(" / "), "model-$id", id == selectedId) { choose(id, name) }
            }
        }
    }
}
