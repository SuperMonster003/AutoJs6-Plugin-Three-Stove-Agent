package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.*
import android.text.InputFilter
import android.text.InputType
import android.view.*
import android.widget.*
import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.service.McpEndpoint
import java.util.UUID

/** Credentials stay write-only and out of screenshots, saved instance state and autofill. */
class McpServersActivity : HostAppearanceActivity() {
    private lateinit var connection: McpConnection
    private lateinit var body: LinearLayout
    private lateinit var message: TextView
    private var profiles = JsonArray()
    private var revision = 0L
    private var editingRevision = 0L
    private var draft: JsonObject? = null
    private var editing: String? = null
    private var editorVisible = false
    private var choices = JsonArray()
    private var busy = false
    private var blocked = false
    private var probeId: String? = null
    private val fields = linkedMapOf<String, EditText>()
    private val selections = linkedMapOf<String, CheckBox>()
    private val frozen = mutableListOf<Pair<View, Boolean>>()
    private lateinit var enabled: CheckBox
    private lateinit var risk: Spinner
    private lateinit var token: EditText
    private lateinit var clearToken: CheckBox
    internal var prompt: AlertDialog? = null; private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setTitle(R.string.mcp_servers)
        savedInstanceState?.let {
            editing = it.getString("editing"); editorVisible = it.getBoolean("editor")
            editingRevision = it.getLong("revision")
            draft = it.getString("draft")?.let { text -> runCatching { AgentJson.objectOf(text, 16 * 1024) }.getOrNull() }
            choices = it.getString("choices")?.let { text -> runCatching { AgentJson.parse(text, McpEndpoint.MAX_RESPONSE_BYTES).asJsonArray }.getOrNull() } ?: JsonArray()
        }
        body = HistoryViews.column(this).apply { layoutDirection = resources.configuration.layoutDirection }
        setContentView(AgentUi.screen(this, getString(R.string.mcp_servers), body, onBack = ::goBack))
        message = HistoryViews.label(body, getString(R.string.interaction_loading))
        connection = McpConnection(this, ::refresh)
    }
    override fun onStart() { super.onStart(); busy = false; connection.start() }
    override fun onStop() {
        if (busy && probeId == null) {
            // A write may commit after this screen stops. Reload its acknowledged metadata on return.
            draft = null; editing = null; editorVisible = false; choices = JsonArray()
        } else if (editorVisible && fields.isNotEmpty()) draft = readDraft()
        if (::token.isInitialized) token.text.clear()
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
    private fun request(operation: String, parameters: JsonObject = JsonObject(), complete: (Result<JsonObject>) -> Unit) {
        parameters.addProperty("operation", operation); connection.query(parameters, complete)
    }
    private fun refresh() = request("get") { result -> result.onSuccess {
        profiles = it.getAsJsonArray("profiles"); revision = requireNotNull(it.number("revision")); blocked = it.flag("busy") == true
        if (editorVisible && draft != null) editor(draft!!) else list()
    }.onFailure { error(R.string.mcp_error) } }
    private fun button(label: Int, tag: String, action: () -> Unit) = HistoryViews.button(body, label, tag, action).apply { isAllCaps = false }
    private fun goBack() {
        if (!busy) { if (editorVisible) { draft = null; editing = null; editorVisible = false; choices = JsonArray(); refresh() } else finish() }
    }
    override fun navigateBack() { goBack() }
    private fun header() {
        frozen.clear(); body.removeAllViews(); fields.clear(); selections.clear()

        message = HistoryViews.label(body, "").apply { tag = "mcp-message"; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE }
        if (blocked) message.setText(R.string.mcp_busy)
        if (Build.VERSION.SDK_INT >= 37 && checkSelfPermission(LOCAL_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            button(R.string.mcp_local_permission, "mcp-local-permission") { requestPermissions(arrayOf(LOCAL_PERMISSION), LOCAL_REQUEST) }
            HistoryViews.label(body, getString(R.string.mcp_local_permission_note))
        }
    }
    private fun list() {
        editorVisible = false; draft = null; header()
        HistoryViews.label(body, getString(R.string.mcp_setup_note))
        button(R.string.mcp_add, "mcp-add") {
            if (!blocked) {
                editing = null; editingRevision = revision; choices = JsonArray()
                editor(jsonObject("id" to "".json(), "name" to "".json(), "endpoint" to "http://127.0.0.1:9637/mcp".json(),
                    "enabled" to false.json(), "risk" to "sensitive".json(), "selectedTools" to JsonArray(), "hasBearerToken" to false.json()))
            }
        }.isEnabled = !blocked && profiles.size() < 8
        if (profiles.isEmpty) HistoryViews.label(body, getString(R.string.mcp_empty))
        profiles.forEach { value -> val profile = value.asJsonObject
            Button(this).apply {
                text = getString(R.string.mcp_server_label, profile.string("name"), profile.string("id")); isAllCaps = false
                tag = "mcp-server-${profile.string("id")}"; body.addView(this, LinearLayout.LayoutParams(-1, -2))
                setOnClickListener { editing = profile.string("id"); editingRevision = revision; choices = JsonArray(); editor(profile.deepCopy()) }
            }
        }
        tint(body)
    }
    private fun input(key: String, label: Int, value: String, maximum: Int, inputType: Int = InputType.TYPE_CLASS_TEXT): EditText {
        val caption = HistoryViews.label(body, getString(label))
        return EditText(this).apply {
            id = View.generateViewId(); tag = "mcp-$key"; caption.labelFor = id
            this.inputType = inputType; filters = arrayOf(InputFilter.LengthFilter(maximum)); setText(value)
            isSaveEnabled = false; setSaveFromParentEnabled(false)
            body.addView(this, LinearLayout.LayoutParams(-1, -2)); fields[key] = this
        }
    }
    private fun check(label: Int, tag: String, checked: Boolean) = CheckBox(this).apply {
        setText(label); this.tag = tag; isChecked = checked; body.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    private fun editor(value: JsonObject) {
        draft = value.deepCopy(); editorVisible = true; header()
        input("id", R.string.mcp_id, value.string("id").orEmpty(), 12).isEnabled = editing == null
        input("name", R.string.mcp_name, value.string("name").orEmpty(), 80)
        input("endpoint", R.string.mcp_endpoint, value.string("endpoint").orEmpty(), 2048, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        HistoryViews.label(body, getString(R.string.mcp_endpoint_note))
        enabled = check(R.string.mcp_enabled, "mcp-enabled", value.flag("enabled") == true)
        HistoryViews.label(body, getString(R.string.mcp_risk_note))
        risk = Spinner(this).apply {
            tag = "mcp-risk"; adapter = ArrayAdapter(this@McpServersActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf(getString(R.string.interaction_risk_sensitive), getString(R.string.interaction_risk_normal), getString(R.string.interaction_risk_read_only)))
            setSelection(RISKS.indexOf(value.string("risk")).coerceAtLeast(0)); body.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        HistoryViews.label(body, getString(if (value.flag("hasBearerToken") == true) R.string.mcp_token_saved else R.string.mcp_token_empty))
        token = input("token", R.string.mcp_token, "", 4096, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD).apply {
            if (Build.VERSION.SDK_INT >= 26) {
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                imeOptions = imeOptions or android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            }
            isLongClickable = false
        }
        clearToken = check(R.string.mcp_clear_token, "mcp-clear-token", value.flag("clearToken") == true)
        HistoryViews.label(body, getString(R.string.mcp_token_note))
        button(R.string.mcp_probe, "mcp-probe", ::probe).isEnabled = editing != null && !busy
        button(android.R.string.cancel, "mcp-cancel-probe") {
            probeId?.let(connection::cancel); probeId = null; busy = false; draft = readDraft(); editor(draft!!)
        }.visibility = if (probeId == null) View.GONE else View.VISIBLE
        HistoryViews.label(body, getString(R.string.mcp_selection_note))
        val selected = value.getAsJsonArray("selectedTools").map { it.asString }.toSet()
        val offered = choices.map { it.asJsonObject.string("name")!! }.toSet()
        val all = choices.toList() + selected.filter { it !in offered }.map { jsonObject("name" to it.json(), "supported" to true.json()) }
        if (all.isEmpty()) HistoryViews.label(body, getString(R.string.mcp_no_tools))
        all.forEach { item -> val tool = item.asJsonObject; val name = requireNotNull(tool.string("name")); val supported = tool.flag("supported") == true
            val reason = tool.string("reason")?.takeIf { it in McpEndpoint.UNSUPPORTED_REASONS }
            CheckBox(this).apply {
                text = if (supported) name else getString(R.string.mcp_unsupported, name) + (reason?.let { " ($it)" } ?: "")
                tag = "mcp-tool-$name"; isChecked = supported && name in selected; isEnabled = supported && !blocked
                body.addView(this, LinearLayout.LayoutParams(-1, -2)); selections[name] = this
            }
            tool.string("description")?.takeIf { it.isNotBlank() }?.let { HistoryViews.label(body, AgentJson.truncate(it, 256)) }
        }
        button(R.string.settings_save, "mcp-save", ::save).isEnabled = !blocked && !busy
        if (editing != null) button(R.string.mcp_delete, "mcp-delete") {
            prompt = AlertDialog.Builder(this).setMessage(R.string.mcp_delete_confirm).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ -> mutate("delete", jsonObject("revision" to editingRevision.json(), "id" to editing!!.json())) }.showStyled()
        }.isEnabled = !blocked && !busy
        tint(body)
        if (busy) freeze()
    }
    private fun readDraft() = jsonObject("id" to fields.getValue("id").text.toString().json(), "name" to fields.getValue("name").text.toString().json(),
        "endpoint" to fields.getValue("endpoint").text.toString().json(), "enabled" to enabled.isChecked.json(),
        "risk" to RISKS[risk.selectedItemPosition.coerceAtLeast(0)].json(), "selectedTools" to JsonArray().apply { selections.filterValues { it.isChecked }.keys.forEach(::add) },
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
            result.onSuccess { token.text.clear(); editorVisible = false; editing = null; draft = null; choices = JsonArray(); refresh() }
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
        if (token.text.isNotEmpty() || clearToken.isChecked || fields.getValue("endpoint").text.toString() != saved.string("endpoint")) {
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
    private fun error(label: Int) { message.setText(label); (body.parent as? ScrollView)?.smoothScrollTo(0, 0) }
    private companion object {
        val RISKS = listOf("sensitive", "normal", "read_only")
        const val LOCAL_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val LOCAL_REQUEST = 71
    }
}
