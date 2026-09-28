package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.pm.PackageManager
import android.os.*
import android.text.InputType
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.McpEndpoint
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*
import java.util.UUID

/** MCP servers screen construction (roadmap P11): the header, list and editor form; McpServersActivity keeps state and events. */

internal fun McpServersActivity.header(title: CharSequence) {
    frozen.clear(); body.removeAllViews(); fields.clear(); selections.clear()
    bar.removeAllViews(); bar.visibility = View.GONE
    supportActionBar?.title = title
    message = kit.text("", Ui.TEXT_BODY, palette.danger).apply {
        tag = "mcp-message"; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }
    body.addView(message, LinearLayout.LayoutParams(-1, -2))
    if (blocked) message.setText(R.string.mcp_busy)
    if (Build.VERSION.SDK_INT >= 37 && checkSelfPermission(McpServersActivity.LOCAL_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
        body.addView(Banner(kit).apply {
            show(getString(R.string.mcp_local_permission_note), Tone.WARNING, R.drawable.ic_warning)
            actions.addView(kit.textButton(getString(R.string.mcp_local_permission)) { requestPermissions(arrayOf(McpServersActivity.LOCAL_PERMISSION), McpServersActivity.LOCAL_REQUEST) })
            view.tag = "mcp-local-permission"
        }.view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
    }
}

internal fun McpServersActivity.list() {
    editorVisible = false; draft = null; header(getString(R.string.mcp_servers))
    kit.caption(body, getString(R.string.mcp_setup_note))
    body.addView(kit.tonalButton(getString(R.string.mcp_add), "mcp-add") {
        if (!blocked) {
            editing = null; editingRevision = revision; choices = JsonArray()
            editor(jsonObject("id" to "".json(), "name" to "".json(), "endpoint" to "http://127.0.0.1:9637/mcp".json(),
                "enabled" to false.json(), "risk" to "sensitive".json(), "selectedTools" to JsonArray(), "hasBearerToken" to false.json()))
        }
    }.apply { isEnabled = !blocked && profiles.size() < 8 }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_MD); bottomMargin = kit.dp(Ui.SPACE_SM) })
    if (profiles.isEmpty) body.addView(kit.emptyState(getString(R.string.mcp_empty), R.drawable.ic_hub))
    profiles.forEach { value -> val profile = value.asJsonObject
        val row = kit.settingRow(getString(R.string.mcp_server_label, profile.string("name"), profile.string("id")),
            listOfNotNull(profile.string("endpoint"), getString(if (profile.flag("enabled") == true) R.string.mcp_state_on else R.string.mcp_state_off)).joinToString(" · "),
            R.drawable.ic_hub, "mcp-server-${profile.string("id")}") {
            editing = profile.string("id"); editingRevision = revision; choices = JsonArray(); editor(profile.deepCopy())
        }
        row.view.setPaddingRelative(0, row.view.paddingTop, 0, row.view.paddingBottom)
        body.addView(row.view, LinearLayout.LayoutParams(-1, -2))
    }
}

internal fun McpServersActivity.input(key: String, label: Int, value: String, maximum: Int, inputType: Int = InputType.TYPE_CLASS_TEXT, helper: Int? = null): TextInputEditText =
    kit.formField(body, getString(label), value, "mcp-$key", inputType, maximum, helper = helper?.let(::getString)).apply {
        isSaveEnabled = false; setSaveFromParentEnabled(false); fields[key] = this
    }

internal fun McpServersActivity.editor(value: JsonObject) {
    draft = value.deepCopy(); editorVisible = true; header(value.string("name")?.takeIf { it.isNotBlank() } ?: getString(R.string.mcp_add))
    input("id", R.string.mcp_id, value.string("id").orEmpty(), 12).isEnabled = editing == null
    input("name", R.string.mcp_name, value.string("name").orEmpty(), 80)
    input("endpoint", R.string.mcp_endpoint, value.string("endpoint").orEmpty(), 2048, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
        R.string.mcp_endpoint_note).textDirection = View.TEXT_DIRECTION_LTR
    enabled = kit.switchRow(getString(R.string.mcp_enabled), null, R.drawable.ic_hub, value.flag("enabled") == true, "mcp-enabled") {}.also {
        it.view.setPaddingRelative(0, it.view.paddingTop, 0, it.view.paddingBottom); body.addView(it.view, LinearLayout.LayoutParams(-1, -2))
    }
    risk = kit.choiceRow(getString(R.string.mcp_risk), listOf(getString(R.string.interaction_risk_sensitive), getString(R.string.interaction_risk_normal),
        getString(R.string.interaction_risk_read_only)), McpServersActivity.RISKS.indexOf(value.string("risk")).coerceAtLeast(0), R.drawable.ic_shield, "mcp-risk") {}.also {
        it.view.setPaddingRelative(0, it.view.paddingTop, 0, it.view.paddingBottom); body.addView(it.view, LinearLayout.LayoutParams(-1, -2))
    }
    kit.caption(body, getString(R.string.mcp_risk_note))
    kit.formSection(body, getString(R.string.mcp_token))
    kit.caption(body, getString(if (value.flag("hasBearerToken") == true) R.string.mcp_token_saved else R.string.mcp_token_empty))
    token = input("token", R.string.mcp_token, "", 4096, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, R.string.mcp_token_note).apply {
        if (Build.VERSION.SDK_INT >= 26) {
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            imeOptions = imeOptions or android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        }
        isLongClickable = false
    }
    clearToken = kit.checkRow(body, getString(R.string.mcp_clear_token), "mcp-clear-token", value.flag("clearToken") == true)

    kit.formSection(body, getString(R.string.mcp_tools))
    kit.caption(body, getString(R.string.mcp_selection_note))
    body.addView(kit.tonalButton(getString(R.string.mcp_probe), "mcp-probe", ::probe).apply { isEnabled = editing != null && !busy },
        LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
    body.addView(kit.textButton(getString(android.R.string.cancel), "mcp-cancel-probe") {
        probeId?.let(connection::cancel); probeId = null; busy = false; draft = readDraft(); editor(draft!!)
    }.apply { visibility = if (probeId == null) View.GONE else View.VISIBLE }, LinearLayout.LayoutParams(-2, -2))
    val selected = value.getAsJsonArray("selectedTools").map { it.asString }.toSet()
    val offered = choices.map { it.asJsonObject.string("name")!! }.toSet()
    val all = choices.toList() + selected.filter { it !in offered }.map { jsonObject("name" to it.json(), "supported" to true.json()) }
    if (all.isEmpty()) kit.caption(body, getString(R.string.mcp_no_tools))
    all.forEach { item -> val tool = item.asJsonObject; val name = requireNotNull(tool.string("name")); val supported = tool.flag("supported") == true
        val reason = tool.string("reason")?.takeIf { it in McpEndpoint.UNSUPPORTED_REASONS }
        selections[name] = kit.checkRow(body, if (supported) name else getString(R.string.mcp_unsupported, name) + (reason?.let { " ($it)" } ?: ""),
            "mcp-tool-$name", supported && name in selected).apply { isEnabled = supported && !blocked; textDirection = View.TEXT_DIRECTION_LTR }
        tool.string("description")?.takeIf { it.isNotBlank() }?.let { kit.caption(body, AgentJson.truncate(it, 256)) }
    }
    bar.visibility = View.VISIBLE
    if (editing != null) bar.addView(kit.textButton(getString(R.string.mcp_delete), "mcp-delete", danger = true) {
        prompt = kit.confirmDialog(getString(R.string.mcp_delete), getString(R.string.mcp_delete_confirm), getString(R.string.mcp_delete), destructive = true) {
            mutate("delete", jsonObject("revision" to editingRevision.json(), "id" to editing!!.json()))
        }
    }.apply { isEnabled = !blocked && !busy })
    bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
    bar.addView(kit.filledButton(getString(R.string.settings_save), "mcp-save", ::save).apply { isEnabled = !blocked && !busy })
    pristine = readDraft()
    if (busy) freeze()
}
