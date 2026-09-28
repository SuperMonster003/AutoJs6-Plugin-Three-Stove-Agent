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
    internal lateinit var connection: McpConnection
    internal lateinit var scaffold: Scaffold
    internal lateinit var body: LinearLayout
    internal lateinit var message: TextView
    internal var profiles = JsonArray()
    internal var revision = 0L
    internal var editingRevision = 0L
    internal var draft: JsonObject? = null
    /** The editor as first shown, to detect unsaved changes. */
    internal var pristine: JsonObject? = null
    internal var editing: String? = null
    internal var editorVisible = false
    internal var choices = JsonArray()
    internal var busy = false
    internal var blocked = false
    internal var probeId: String? = null
    internal val fields = linkedMapOf<String, TextInputEditText>()
    internal val selections = linkedMapOf<String, MaterialCheckBox>()
    internal val frozen = mutableListOf<Pair<View, Boolean>>()
    /** Pinned below the editor: Delete (existing servers) and Save, like the presets and memory editors. */
    internal lateinit var bar: LinearLayout
    internal lateinit var enabled: SettingRow
    internal lateinit var risk: ChoiceRow
    internal lateinit var token: TextInputEditText
    internal lateinit var clearToken: MaterialCheckBox
    internal var prompt: AlertDialog? = null

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
        bar = kit.actionBar().apply { visibility = View.GONE }
        scaffold.root.addView(bar, LinearLayout.LayoutParams(-1, -2))
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
    internal fun readDraft() = jsonObject("id" to fields.getValue("id").text.toString().json(), "name" to fields.getValue("name").text.toString().json(),
        "endpoint" to fields.getValue("endpoint").text.toString().json(), "enabled" to (enabled.switch?.isChecked == true).json(),
        "risk" to RISKS[risk.selectedIndex.coerceAtLeast(0)].json(), "selectedTools" to JsonArray().apply { selections.filterValues { it.isChecked }.keys.forEach(::add) },
        "hasBearerToken" to (draft?.flag("hasBearerToken") == true).json(), "clearToken" to clearToken.isChecked.json())
    internal fun save() {
        if (busy || blocked) return
        val profile = readDraft(); val clear = clearToken.isChecked; val secret = token.text.toString()
        profile.remove("hasBearerToken"); profile.remove("clearToken")
        mutate("save", jsonObject("revision" to editingRevision.json(), "profile" to profile, "create" to (editing == null).json(),
            "token" to secret.json(), "clearToken" to clear.json()))
    }
    internal fun mutate(operation: String, value: JsonObject) {
        if (busy || blocked) return
        busy = true; freeze()
        request(operation, value) { result ->
            busy = false; unfreeze()
            result.onSuccess { token.text?.clear(); editorVisible = false; editing = null; draft = null; choices = JsonArray(); refresh() }
                .onFailure { error(R.string.mcp_error) }
        }
    }
    internal fun freeze() {
        if (frozen.isNotEmpty()) return
        fun visit(view: View) {
            if (view.tag == "mcp-cancel-probe" && probeId != null) return
            frozen += view to view.isEnabled; view.isEnabled = false
            if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
        }
        for (index in 0 until body.childCount) visit(body.getChildAt(index))
        for (index in 0 until bar.childCount) visit(bar.getChildAt(index))
    }
    private fun unfreeze() { frozen.forEach { (view, enabled) -> view.isEnabled = enabled }; frozen.clear() }
    internal fun probe() {
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
    internal fun error(label: Int) { message.setText(label); scaffold.scroll?.smoothScrollTo(0, 0) }
    internal companion object {
        val RISKS = listOf("sensitive", "normal", "read_only")
        const val LOCAL_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val LOCAL_REQUEST = 71
    }
}
