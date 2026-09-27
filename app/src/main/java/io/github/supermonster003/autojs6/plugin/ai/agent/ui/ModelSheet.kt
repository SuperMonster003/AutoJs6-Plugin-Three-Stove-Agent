package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.ModelLocality
import io.github.supermonster003.autojs6.plugin.ai.agent.store.ModelRef
import io.github.supermonster003.autojs6.plugin.ai.agent.store.ModelSelectionState
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.*

/**
 * Bottom sheet for choosing the model of new tasks: search, Automatic (with its current pick),
 * pinned and recent models, then the host catalog grouped by locality with capability badges.
 * Choosing never edits a preset or a running task.
 */
internal class ModelSheet(
    private val kit: Kit,
    private val catalog: ModelCatalog,
    private val selection: () -> ModelSelectionState,
    private val update: ((ModelSelectionState) -> ModelSelectionState) -> Unit,
) {
    internal var handle: SheetHandle? = null; private set
    private var search: TextInputEditText? = null
    private var status: TextView? = null
    private var list: LinearLayout? = null
    private val context get() = kit.context

    fun show() {
        handle?.dialog?.dismiss()
        val (field, edit) = kit.textField(null, kit.string(R.string.ui_model_search), InputType.TYPE_CLASS_TEXT, 256)
        field.startIconDrawable = kit.tintedDrawable(R.drawable.ic_search, kit.palette.muted)
        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) { renderList() }
        })
        val statusView = kit.text("", Ui.TEXT_SECONDARY, kit.palette.muted).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(kit.dp(Ui.SPACE_XXL), 0, kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_XS))
            addView(field, LinearLayout.LayoutParams(-1, -2).apply { marginEnd = kit.dp(Ui.SPACE_MD) })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(statusView, LinearLayout.LayoutParams(0, -2, 1f))
                addView(kit.iconButton(R.drawable.ic_restart, kit.string(R.string.presets_refresh_models), "refresh-models", kit.palette.muted) {
                    catalog.refresh()
                })
            }, LinearLayout.LayoutParams(-1, -2))
        }
        lateinit var sheet: SheetHandle
        // Dismiss callbacks arrive later; an old sheet must never clear a newer one.
        sheet = kit.bottomSheet(kit.string(R.string.ui_choose_model), header = header, minHeightFraction = 0.6f,
            onDismiss = { if (handle === sheet) clear() })
        search = edit; status = statusView; list = sheet.content; handle = sheet
        render()
    }

    fun dismiss() { val open = handle; clear(); open?.dialog?.dismiss() }

    private fun clear() { handle = null; search = null; status = null; list = null }

    /** Re-renders after a catalog or selection change; a closed sheet ignores it. */
    fun render() {
        val statusView = status ?: return
        statusView.text = kit.string(when (catalog.status) {
            ModelCatalog.Status.LOADING -> R.string.interaction_loading
            ModelCatalog.Status.READY -> if (catalog.entries.isEmpty()) R.string.ui_models_empty else R.string.ui_model_note
            else -> R.string.presets_models_unavailable
        })
        renderList()
    }

    private fun renderList() {
        val parent = list ?: return
        parent.removeAllViews()
        val state = selection()
        val ready = catalog.status == ModelCatalog.Status.READY
        val query = search?.text?.toString()?.trim().orEmpty()
        fun matches(vararg values: String?) = query.isEmpty() || values.any { it?.contains(query, ignoreCase = true) == true }
        var shown = 0

        if (query.isEmpty()) {
            val pick = catalog.automatic
            row(parent, kit.string(R.string.model_automatic), listOfNotNull(pick?.let { kit.string(R.string.model_automatic_current, it.name) },
                kit.string(R.string.model_automatic_note)).joinToString("\n"), emptyList(), "model-automatic", state.current == null) { choose(null) }
            val current = state.current
            if (ready && current != null && catalog.find(current.targetId) == null) {
                row(parent, current.name, kit.string(R.string.model_unavailable_note), listOf(kit.string(R.string.model_unavailable) to Tone.DANGER),
                    "model-unavailable", selected = true, onClick = null)
            }
        }

        fun saved(title: Int, models: List<ModelRef>, prefix: String, pinned: Boolean) {
            val visible = models.filter { model -> catalog.find(model.targetId).let { matches(model.name, model.targetId, it?.providerId) } }
            if (visible.isEmpty()) return
            parent.addView(kit.sectionHeader(kit.string(title)))
            visible.forEach { model ->
                val entry = catalog.find(model.targetId)
                val missing = ready && entry == null
                shown++
                row(parent, entry?.name ?: model.name, entry?.providerId ?: model.targetId.substringBefore(':'),
                    if (missing) listOf(kit.string(R.string.model_unavailable) to Tone.DANGER) else badges(entry),
                    "$prefix-${model.targetId}", state.current?.targetId == model.targetId,
                    pin = if (pinned) pinButton(model, "unpin-${model.targetId}") else null,
                    onClick = if (missing) null else ({ choose(entry?.ref ?: model) }))
            }
        }
        saved(R.string.model_section_pinned, state.pinned, "pinned", pinned = true)
        saved(R.string.model_section_recent, state.recents.filterNot { state.isPinned(it.targetId) }, "recent", pinned = false)

        for ((locality, title) in listOf(ModelLocality.REMOTE to R.string.presets_remote, ModelLocality.ON_DEVICE to R.string.presets_local,
            ModelLocality.HYBRID to R.string.presets_hybrid)) {
            val models = catalog.entries.filter { it.locality == locality && matches(it.name, it.targetId, it.providerId) }
            if (models.isEmpty()) continue
            parent.addView(kit.sectionHeader(kit.string(title)))
            models.forEach { entry ->
                shown++
                row(parent, entry.name, entry.providerId, badges(entry), "model-${entry.targetId}", state.current?.targetId == entry.targetId,
                    pin = pinButton(entry.ref, "pin-${entry.targetId}")) { choose(entry.ref) }
            }
        }
        if (query.isNotEmpty() && shown == 0) parent.addView(kit.text(kit.string(R.string.ui_models_no_match), Ui.TEXT_BODY, kit.palette.muted).apply {
            setPaddingRelative(kit.dp(Ui.SPACE_XXL), kit.dp(Ui.SPACE_LG), kit.dp(Ui.SPACE_XXL), kit.dp(Ui.SPACE_LG))
        })
    }

    private fun badges(entry: ModelEntry?): List<Pair<String, Tone>> = listOfNotNull(
        entry?.takeIf { it.tools }?.let { kit.string(R.string.ui_model_tools) to Tone.ACCENT },
        entry?.takeIf { it.vision }?.let { kit.string(R.string.ui_model_vision) to Tone.ACCENT })

    private fun choose(model: ModelRef?) {
        update { it.choose(model) }
        dismiss()
    }

    private fun pinButton(model: ModelRef, tag: String): View {
        val pinned = selection().isPinned(model.targetId)
        return kit.iconButton(if (pinned) R.drawable.ic_pin else R.drawable.ic_pin_outline,
            kit.string(if (pinned) R.string.model_unpin else R.string.model_pin, model.name), tag,
            if (pinned) kit.palette.accent else kit.palette.muted) {
            val before = selection()
            if (!before.isPinned(model.targetId) && before.pinned.size >= ModelSelectionState.MAX_PINNED)
                Toast.makeText(context, kit.string(R.string.model_pins_full), Toast.LENGTH_SHORT).show()
            else update { it.togglePin(model) }
        }
    }

    private fun row(parent: LinearLayout, title: String, summary: String?, badges: List<Pair<String, Tone>>, tag: String,
                    selected: Boolean, pin: View? = null, onClick: (() -> Unit)?) {
        val shell = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = kit.dp(64)
            setPaddingRelative(kit.dp(Ui.SPACE_XXL), kit.dp(Ui.SPACE_SM), kit.dp(if (pin == null) Ui.SPACE_XXL else Ui.SPACE_SM), kit.dp(Ui.SPACE_SM))
            this.tag = tag
            isSelected = selected
            if (selected) background = kit.roundedFill(kit.palette.accentTone, 0)
        }
        shell.addView(ImageView(context).apply {
            setImageDrawable(kit.tintedDrawable(R.drawable.ic_check, kit.palette.accent))
            visibility = if (selected) View.VISIBLE else View.INVISIBLE
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(kit.dp(Ui.ICON_SIZE), kit.dp(Ui.ICON_SIZE)).apply { marginEnd = kit.dp(Ui.SPACE_LG) })
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        column.addView(kit.text(title, Ui.TEXT_ITEM, if (selected) kit.palette.accent else kit.palette.text, medium = selected).apply {
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        })
        if (!summary.isNullOrEmpty()) column.addView(kit.text(summary, Ui.TEXT_SECONDARY, kit.palette.muted).apply {
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setPaddingRelative(0, kit.dp(2), 0, 0)
        })
        if (badges.isNotEmpty()) column.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            badges.forEach { (label, tone) ->
                addView(kit.badge(label, tone).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
                    LinearLayout.LayoutParams(-2, -2).apply { marginEnd = kit.dp(Ui.SPACE_XS) })
            }
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
        shell.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
        pin?.let { shell.addView(it) }
        shell.contentDescription = (listOf(title) + listOfNotNull(summary) + badges.map { it.first }).joinToString(", ")
        if (onClick == null) shell.alpha = if (selected) 1f else Ui.DISABLED_ALPHA else {
            shell.isClickable = true; shell.isFocusable = true
            kit.selectableBackground(shell)
            if (selected) shell.background = kit.roundedRippleFill(kit.palette.accentTone, 0)
            shell.setOnClickListener { onClick() }
            shell.accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = android.widget.RadioButton::class.java.name
                    info.isCheckable = true; info.isChecked = selected
                }
            }
        }
        parent.addView(shell, LinearLayout.LayoutParams(-1, -2))
    }
}
