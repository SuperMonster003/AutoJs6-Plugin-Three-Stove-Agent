package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.*
import android.net.Uri
import android.provider.Settings
import android.text.*
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ThreeStoveAgentPlugin
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.scripts.ScriptRoots
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.RunLauncher
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.SettingsCodec
import io.github.supermonster003.autojs6.plugin.three.stove.agent.threeStoveAgentPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.update.AppUpdateCoordinator
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentActions
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import java.util.UUID

/** Home screen construction (roadmap P11): menus, sheets, dialogs and snapshot rendering; LauncherActivity keeps state and events. */

internal fun LauncherActivity.showMenu(anchor: View) {
    overflowMenu = ThemedPopupMenu(kit, anchor).apply {
        menu.add(0, R.id.workbench_new_task, 0, R.string.workbench_new_task)
        menu.add(0, R.id.workbench_presets, 1, R.string.presets_title)
        menu.add(0, R.id.workbench_memory, 2, R.string.memory_title)
        menu.add(0, R.id.launcher_script_roots, 3, R.string.script_roots_title)
        menu.add(0, R.id.workbench_mcp, 4, R.string.mcp_servers)
        menu.add(0, R.id.workbench_floating, 5, R.string.settings_floating).setCheckable(true).isChecked = floatingEnabled
        menu.add(0, R.id.workbench_settings, 6, R.string.settings_title)
        setOnMenuItemClickListener { item ->
            val screen = when (item.itemId) {
                R.id.workbench_new_task -> { newTask(); return@setOnMenuItemClickListener true }
                R.id.workbench_floating -> { toggleFloating(!floatingEnabled); return@setOnMenuItemClickListener true }
                R.id.workbench_settings -> SettingsActivity::class.java
                R.id.workbench_presets -> PresetsActivity::class.java
                R.id.workbench_memory -> MemoryActivity::class.java
                R.id.workbench_mcp -> McpServersActivity::class.java
                else -> ScriptRootsActivity::class.java
            }
            startActivity(Intent(this@showMenu, screen)); true
        }; show()
    }
}

internal fun LauncherActivity.chooseAccess() {
    val modes = AccessMode.entries
    val labels = listOf(R.string.presets_standard, R.string.presets_cautious, R.string.settings_full_access).map(::getString)
    val current = when (accessMode) { "full" -> AccessMode.FULL; "cautious" -> AccessMode.CAUTIOUS; else -> AccessMode.STANDARD }
    accessDialog = kit.singleChoiceDialog(getString(R.string.settings_access_mode), labels, modes.indexOf(current)) { index ->
        if (modes[index] != current) saveSettings { it.withAccess(modes[index]) }
    }
}

/** Preset sheet: pick a preset for the next task, or manage presets without leaving the workbench. */
internal fun LauncherActivity.choosePreset() {
    presetSheet?.dialog?.dismiss()
    val handle = kit.bottomSheet(getString(R.string.workbench_preset), minHeightFraction = 0.3f, onDismiss = { presetSheet = null })
    presetSheet = handle
    renderPresetSheet(handle)
}

internal fun LauncherActivity.renderPresetSheet(handle: SheetHandle) {
    handle.content.removeAllViews()
    presetChoices.forEach { name ->
        val selected = name == selectedPreset
        val row = kit.settingRow(presetLabel(name), if (name == defaultPresetName) getString(R.string.presets_default_badge) else null,
            if (selected) R.drawable.ic_check else R.drawable.ic_layers, "preset-choice-$name", chevron = false,
            titleColor = if (selected) palette.accent else palette.text) {
            if (name != selectedPreset) followDefault = false
            selectedPreset = name; renderPreset(); updateSend(); handle.dialog.dismiss()
        }
        val more = kit.iconButton(R.drawable.ic_more, getString(R.string.presets_actions, presetLabel(name)), "preset-actions-$name", palette.muted) {}
        more.setOnClickListener { presetActions(name, more, handle) }
        row.view.addView(more)
        handle.content.addView(row.view)
    }
    handle.content.addView(LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
        setPaddingRelative(kit.dp(Ui.SCREEN_MARGIN), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SCREEN_MARGIN), 0)
        addView(kit.tonalButton(getString(R.string.presets_new), "preset-sheet-new") { openPresets(PresetsIntents.NEW); handle.dialog.dismiss() },
            LinearLayout.LayoutParams(-2, -2).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
        addView(kit.textButton(getString(R.string.presets_manage), "preset-sheet-manage") { openPresets(null); handle.dialog.dismiss() })
    }, LinearLayout.LayoutParams(-1, -2))
}

internal fun LauncherActivity.openPresets(extra: String?, name: String = "") {
    startActivity(Intent(this, PresetsActivity::class.java).apply { extra?.let { putExtra(it, name) } })
}

internal fun LauncherActivity.presetActions(name: String, anchor: View, handle: SheetHandle) {
    presetMenu = PopupMenu(this, anchor).apply {
        menu.add(0, 1, 0, R.string.presets_edit); menu.add(0, 2, 1, R.string.presets_copy)
        if (name != defaultPresetName) menu.add(0, 3, 2, R.string.presets_set_default)
        if (name != "default") menu.add(0, 4, 3, R.string.presets_delete)
        setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { openPresets(PresetsIntents.EDIT, name); handle.dialog.dismiss() }
                2 -> { openPresets(PresetsIntents.COPY, name); handle.dialog.dismiss() }
                3 -> presetStore.query(jsonObject("operation" to "default".json(), "name" to name.json())) { if (it.isFailure) showError() else agent.refresh() }
                4 -> presetDialog = kit.confirmDialog(getString(R.string.presets_delete), getString(R.string.presets_delete_confirm, name),
                    getString(R.string.presets_delete), destructive = true) {
                    presetStore.query(jsonObject("operation" to "delete".json(), "name" to name.json())) { result ->
                        if (result.isFailure) showError() else { if (selectedPreset == name) { selectedPreset = "default"; followDefault = true }; agent.refresh() }
                    }
                }
            }
            true
        }
        show()
    }
}

internal fun LauncherActivity.render(value: WorkbenchSnapshot) {
    renderLink(value.status)
    accessMode = value.status.string("accessMode") ?: if (value.status.flag("fullAccessEnabled") == true) "full" else "standard"
    views.composer.showAccess(accessMode)
    floatingEnabled = value.status.flag("floatingEnabled") == true
    views.composer.voice.visibility = if (value.status.flag("voiceEnabled") == true && SpeechInput.available(this)) View.VISIBLE else View.GONE
    val presetsChanged = availablePresets != value.presets || defaultPresetName != value.defaultPreset
    availablePresets = value.presets; defaultPresetName = value.defaultPreset
    if (followDefault && attached && value.defaultPreset in availablePresets && selectedPreset != value.defaultPreset) selectedPreset = value.defaultPreset
    renderPreset()
    if (presetsChanged) presetSheet?.let(::renderPresetSheet)
    // Preserve a historical preset that has since disappeared. Never silently rerun with default.
    views.composer.error.apply {
        if (attached && selectedPreset !in availablePresets) { setText(R.string.history_preset_unavailable); visibility = View.VISIBLE }
        else if (text == getString(R.string.history_preset_unavailable)) visibility = View.GONE
    }
    val row = value.run?.takeIf { it.string("runId") != hiddenRunId }
    currentId = row?.string("runId")
    val wasAtEnd = atEnd()
    val before = views.feed.view.height
    views.feed.render(value.copy(run = row))
    pending.render(row)
    visibility.render(row)
    val request = row?.getAsJsonObject("pending")?.string("requestId")
    val revealQuestion = request != null && request != lastPendingId
    lastPendingId = request
    views.scroll.post {
        // Sticky scrolling follows a task; the welcome state and first load keep their position.
        when {
            row == null -> views.jump.visibility = View.GONE
            revealCurrent -> { revealCurrent = false; reveal(views.feed.current) }
            revealQuestion -> reveal(views.feed.pending)
            wasAtEnd -> scrollToEnd(false)
            before > 0 && views.feed.view.height > before && !atEnd() -> views.jump.visibility = View.VISIBLE
        }
    }
    updateSend()
}

internal fun LauncherActivity.atEnd(): Boolean {
    val content = views.scroll.getChildAt(0) ?: return true
    return content.bottom - (views.scroll.height + views.scroll.scrollY) <= kit.dp(48)
}

internal fun LauncherActivity.scrollToEnd(animated: Boolean) {
    val bottom = (views.scroll.getChildAt(0)?.height ?: 0) - views.scroll.height
    if (animated) views.scroll.smoothScrollTo(0, bottom.coerceAtLeast(0)) else views.scroll.scrollTo(0, bottom.coerceAtLeast(0))
    views.jump.visibility = View.GONE
}

/** Brings [target] to the top of the feed viewport (a new task or a new question). */
internal fun LauncherActivity.reveal(target: View) {
    val content = views.scroll.getChildAt(0) ?: return
    val rect = Rect(0, 0, target.width, target.height)
    runCatching { (content as android.view.ViewGroup).offsetDescendantRectToMyCoords(target, rect) }.onFailure { return }
    views.scroll.smoothScrollTo(0, (rect.top - kit.dp(Ui.SPACE_LG)).coerceAtLeast(0))
    views.jump.visibility = View.GONE
}
