package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.nodes.ActionTools
import java.security.MessageDigest

/** Per-run loop evidence. Read-only turns cannot erase an action repetition streak. */
class LoopRules {
    private var previousAction: String? = null
    private var repetitions = 0
    private var unchangedActions = 0
    private var observeRequired = false

    /** The third equivalent proposal is blocked before confirmation or side effects. */
    fun admit(spec: ToolSpec, prepared: PreparedTool): Boolean {
        if (spec.readOnlyHint || prepared.metadata.context.registeredScriptRisk == RiskLevel.READ_ONLY) return true
        val args = (prepared.metadata.script?.arguments() ?: prepared.invocation.arguments).deepCopy()
        args.remove("snapshotId") // Transport identity changes on every dump, even on an unchanged screen.
        prepared.metadata.actionIdentity?.let { args.remove("nodeRef"); args.addProperty("observedNode", it) }
        val context = prepared.metadata.context
        val key = digest(jsonArray(spec.name.json(), canonical(args), context.packageName.json(), context.nodeText.json(), context.nodeDescription.json()))
        repetitions = if (key == previousAction) repetitions + 1 else 1
        previousAction = key
        return repetitions < REPEAT_LIMIT
    }

    fun started(spec: ToolSpec) {
        if (spec.name in SCREEN_ACTIONS) observeRequired = true
    }

    fun succeeded(spec: ToolSpec, value: JsonElement) {
        val result = value.takeIf { it.isJsonObject }?.asJsonObject
        val changes = result?.get("changes")?.takeIf { it.isJsonObject }?.asJsonObject
        if (spec.name in SCREEN_ACTIONS) {
            val stability = result?.get("stability")?.takeIf { it.isJsonObject }?.asJsonObject
            observeRequired = stability?.flag("observed") != true
            // A window staying open does not imply its contents stayed unchanged.
            val unchanged = result?.flag("windowChanged") == false && changes?.flag("changed") == false &&
                changes.flag("partial") == false && changes.flag("baseline") == false
            unchangedActions = if (unchanged) unchangedActions + 1 else 0
        } else if (spec.name in SCREEN_OBSERVATIONS) {
            observeRequired = false
            if (changes?.flag("changed") == true) unchangedActions = 0
        }
    }

    fun failed(spec: ToolSpec) { if (spec.name in SCREEN_ACTIONS) unchangedActions = 0 }

    /** Only fixed keys and counters enter the mandatory system rules; identities stay private. */
    fun guidance(): JsonObject = jsonObject("observeRequired" to observeRequired.json(),
        "changeStrategy" to (unchangedActions >= UNCHANGED_LIMIT).json(), "unchangedActions" to unchangedActions.json(),
        "repeatedActionCount" to repetitions.json())

    companion object {
        const val REPEAT_LIMIT = 3
        const val UNCHANGED_LIMIT = 3
        private val SCREEN_ACTIONS = ActionTools.NAMES - "clipboard_get"
        private val SCREEN_OBSERVATIONS = setOf("ui_dump", "ui_find", "ui_wait_for", "ocr_screen", "app_current", "screen_state", "clipboard_get")
        private fun canonical(value: JsonElement): JsonElement = when {
            value.isJsonObject -> JsonObject().apply { value.asJsonObject.keySet().sorted().forEach { add(it, canonical(value.asJsonObject[it])) } }
            value.isJsonArray -> JsonArray().apply { value.asJsonArray.forEach { add(canonical(it)) } }
            value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> JsonPrimitive(value.asBigDecimal.stripTrailingZeros())
            else -> value.deepCopy()
        }
        private fun digest(value: JsonElement) = MessageDigest.getInstance("SHA-256").digest(value.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
