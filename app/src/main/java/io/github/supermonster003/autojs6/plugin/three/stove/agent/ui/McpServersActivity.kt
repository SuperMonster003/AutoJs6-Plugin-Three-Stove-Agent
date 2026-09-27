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

/** Credentials stay write-only and out of screenshots, saved instance state and autofill. */
class McpServersActivity : HostAppearanceActivity() {
    private lateinit var connection: McpConnection
    private lateinit var scaffold: Scaffold
    private lateinit var body: LinearLayout
    private lateinit var message: TextView
    private var profiles = JsonArray()
    private var revision = 0L
    private var editingRevision = 0L
    private var draft: JsonObject? = null
    /** The editor as first shown, to detect unsaved changes. */
    private var pristine: JsonObject? = null
    private var editing: String? = null
    private var editorVisible = false
    private var choices = JsonArray()
    private var busy = false
    private var blocked = false
    private var probeId: String? = null
    private val fields = linkedMapOf<String, TextInputEditText>()
    private val selections = linkedMapOf<String, MaterialCheckBox>()
    private val frozen = mutableListOf<Pair<View, Boolean>>()
    private lateinit var enabled: SettingRow
    internal lateinit var risk: ChoiceRow; private set
    private lateinit var token: TextInputEditText
    private lateinit var clearToken: MaterialCheckBox
    internal var prompt: AlertDialog? = null; private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        savedInstanceState?.let {
            editing = it.getString("editing"); editorVisible = it.getBoolean("editor")
            editingRevision = it.getLong("revision")
            draft = it.getString("draft")?.let { text -> runCatching { AgentJson.objectOf(text, 16 * 1024) }.getOrNull() }
            choices = it.getString("choices")?.let { text -> runCatching { AgentJson.parse(text, McpEndpoint.MAX_RESPONSE_BYTES).asJsonArray }.getOrNull() } ?: JsonArray()
        }
        scaffold = buildScaffold(getString(R.string.mcp_servers), contentPadding = ContentPadding.SCREEN)
        body = scaffold.content
        setContentView(scaffold.root)
        message = kit.text(getString(R.string.interaction_loading), Ui.TEXT_BODY, palette.muted).also { body.addView(it) }
        connection = McpConnection(this, ::refresh)
    }
    override fun onStart() { super.onStart(); busy = false; connection.start() }
    override fun onStop() {
        if (busy && probeId == null) {
            // A write may commit after this screen stops. Reload its acknowledged metadata on return.
            draft = null; editing = null; editorVisible = false; choices = JsonArray()
        } else if (editorVisible && fields.isNotEmpty()) draft = readDraft()
        if (::token.isInitialized) token.text?.clear()
        probeId = null; connection.stop(); prompt?.dismiss(); prompt = null
        super.onStop()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        val saving = busy && probeId == null
        outState.putBoolean("editor", editorVisible && !saving); outState.putString("editing", if (saving) null else editing)
        outState.putLong("revision", editingRevision)
        outState.putString("draft", (if (saving) null else if (editorVisible && fields.isNotEmpty()) readDraft() else draft)?.toString())
        outState.putString("choices", if (saving) "[]" else choices.toString())
        super.onSaveInstanceState(outState)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCAL_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
            body.findViewWithTag<View>("mcp-local-permission")?.visibility = View.GONE
    }
    override fun navigateBack() {
        if (busy) return
        if (!editorVisible) { finish(); return }
        val leave = { draft = null; editing = null; editorVisible = false; choices = JsonArray(); refresh() }
        if (readDraft() == pristine && token.text.isNullOrEmpty()) leave() else prompt = kit.unsavedChanges(leave)
    }

    private fun request(operation: String, parameters: JsonObject = JsonObject(), complete: (Result<JsonObject>) -> Unit) {
        parameters.addProperty("operation", operation); connection.query(parameters, complete)
    }
    private fun refresh() = request("get") { result -> result.onSuccess {
        profiles = it.getAsJsonArray("profiles"); revision = requireNotNull(it.number("revision")); blocked = it.flag("busy") == true
        if (editorVisible && draft != null) editor(draft!!) else list()
    }.onFailure { error(R.string.mcp_error) } }
    private fun header(title: CharSequence) {
        frozen.clear(); body.removeAllViews(); fields.clear(); selections.clear()
        supportActionBar?.title = title
        message = kit.text("", Ui.TEXT_BODY, palette.danger).apply {
            tag = "mcp-message"; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
        body.addView(message, LinearLayout.LayoutParams(-1, -2))
        if (blocked) message.setText(R.string.mcp_busy)
        if (Build.VERSION.SDK_INT >= 37 && checkSelfPermission(LOCAL_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            body.addView(Banner(kit).apply {
                show(getString(R.string.mcp_local_permission_note), Tone.WARNING, R.drawable.ic_warning)
                actions.addView(kit.textButton(getString(R.string.mcp_local_permission)) { requestPermissions(arrayOf(LOCAL_PERMISSION), LOCAL_REQUEST) })
                view.tag = "mcp-local-permission"
            }.view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
        }
    }
    private fun list() {
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
    private fun input(key: String, label: Int, value: String, maximum: Int, inputType: Int = InputType.TYPE_CLASS_TEXT, helper: Int? = null): TextInputEditText =
        kit.formField(body, getString(label), value, "mcp-$key", inputType, maximum, helper = helper?.let(::getString)).apply {
            isSaveEnabled = false; setSaveFromParentEnabled(false); fields[key] = this
        }
    private fun editor(value: JsonObject) {
        draft = value.deepCopy(); editorVisible = true; header(value.string("name")?.takeIf { it.isNotBlank() } ?: getString(R.string.mcp_add))
        input("id", R.string.mcp_id, value.string("id").orEmpty(), 12).isEnabled = editing == null
        input("name", R.string.mcp_name, value.string("name").orEmpty(), 80)
        input("endpoint", R.string.mcp_endpoint, value.string("endpoint").orEmpty(), 2048, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
            R.string.mcp_endpoint_note).textDirection = View.TEXT_DIRECTION_LTR
        enabled = kit.switchRow(getString(R.string.mcp_enabled), null, R.drawable.ic_hub, value.flag("enabled") == true, "mcp-enabled") {}.also {
            it.view.setPaddingRelative(0, it.view.paddingTop, 0, it.view.paddingBottom); body.addView(it.view, LinearLayout.LayoutParams(-1, -2))
        }
        risk = kit.choiceRow(getString(R.string.mcp_risk), listOf(getString(R.string.interaction_risk_sensitive), getString(R.string.interaction_risk_normal),
            getString(R.string.interaction_risk_read_only)), RISKS.indexOf(value.string("risk")).coerceAtLeast(0), R.drawable.ic_shield, "mcp-risk") {}.also {
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
        body.addView(kit.filledButton(getString(R.string.settings_save), "mcp-save", ::save).apply { isEnabled = !blocked && !busy },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SECTION_GAP) })
        if (editing != null) body.addView(kit.outlinedButton(getString(R.string.mcp_delete), "mcp-delete", danger = true) {
            prompt = kit.confirmDialog(getString(R.string.mcp_delete), getString(R.string.mcp_delete_confirm), getString(R.string.mcp_delete), destructive = true) {
                mutate("delete", jsonObject("revision" to editingRevision.json(), "id" to editing!!.json()))
            }
        }.apply { isEnabled = !blocked && !busy }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
        pristine = readDraft()
        if (busy) freeze()
    }
    private fun readDraft() = jsonObject("id" to fields.getValue("id").text.toString().json(), "name" to fields.getValue("name").text.toString().json(),
        "endpoint" to fields.getValue("endpoint").text.toString().json(), "enabled" to (enabled.switch?.isChecked == true).json(),
        "risk" to RISKS[risk.selectedIndex.coerceAtLeast(0)].json(), "selectedTools" to JsonArray().apply { selections.filterValues { it.isChecked }.keys.forEach(::add) },
        "hasBearerToken" to (draft?.flag("hasBearerToken") == true).json(), "clearToken" to clearToken.isChecked.json())
    private fun save() {
        if (busy || blocked) return
        val profile = readDraft(); val clear = clearToken.isChecked; val secret = token.text.toString()
        profile.remove("hasBearerToken"); profile.remove("clearToken")
        mutate("save", jsonObject("revision" to editingRevision.json(), "profile" to profile, "create" to (editing == null).json(),
            "token" to secret.json(), "clearToken" to clear.json()))
    }
    private fun mutate(operation: String, value: JsonObject) {
        if (busy || blocked) return
        busy = true; freeze()
        request(operation, value) { result ->
            busy = false; unfreeze()
            result.onSuccess { token.text?.clear(); editorVisible = false; editing = null; draft = null; choices = JsonArray(); refresh() }
                .onFailure { error(R.string.mcp_error) }
        }
    }
    private fun freeze() {
        if (frozen.isNotEmpty()) return
        fun visit(view: View) {
            if (view.tag == "mcp-cancel-probe" && probeId != null) return
            frozen += view to view.isEnabled; view.isEnabled = false
            if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
        }
        for (index in 0 until body.childCount) visit(body.getChildAt(index))
    }
    private fun unfreeze() { frozen.forEach { (view, enabled) -> view.isEnabled = enabled }; frozen.clear() }
    private fun probe() {
        if (busy || editing == null) return
        val saved = profiles.firstOrNull { it.asJsonObject.string("id") == editing }?.asJsonObject ?: return
        if (token.text?.isNotEmpty() == true || clearToken.isChecked || fields.getValue("endpoint").text.toString() != saved.string("endpoint")) {
            error(R.string.mcp_save_before_probe); return
        }
        draft = readDraft(); busy = true; probeId = UUID.randomUUID().toString()
        val id = probeId!!; editor(draft!!)
        request("probe", jsonObject("id" to editing!!.json(), "revision" to editingRevision.json(), "probeId" to id.json())) { result ->
            if (probeId != id) return@request
            probeId = null; busy = false
            result.onSuccess {
                if (it.flag("probeFailed") == true) {
                    editor(draft!!); error(when (it.string("code")) {
                        "MCP_AUTH_REQUIRED" -> R.string.mcp_auth_required
                        "MCP_PAIRING_REQUIRED" -> R.string.mcp_pairing_required
                        "MCP_PAIRING_DENIED" -> R.string.mcp_pairing_denied
                        else -> R.string.mcp_probe_error
                    })
                } else { choices = it.getAsJsonArray("tools"); editor(draft!!) }
            }.onFailure { editor(draft!!); error(R.string.mcp_probe_error) }
        }
    }
    private fun error(label: Int) { message.setText(label); scaffold.scroll?.smoothScrollTo(0, 0) }
    private companion object {
        val RISKS = listOf("sensitive", "normal", "read_only")
        const val LOCAL_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val LOCAL_REQUEST = 71
    }
}
