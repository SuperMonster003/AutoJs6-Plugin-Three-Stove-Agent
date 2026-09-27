package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Context
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*

/**
 * The shared model choice for new tasks from the workbench and the floating ball. It lives in
 * `filesDir/model-selection.json`, readable from both the UI and the :agent process, and is never
 * part of a preset.
 */
internal object ModelSelection {
    private const val LEGACY_PREFERENCES = "workbench"

    fun read(context: Context): ModelSelectionState {
        val value = file(context).read() ?: return migrate(context)
        return ModelSelectionCodec.decode(value)
    }

    fun update(context: Context, change: (ModelSelectionState) -> ModelSelectionState): ModelSelectionState {
        var result = ModelSelectionState.AUTOMATIC
        file(context).update { stored ->
            val current = if (stored == null) legacy(context) else ModelSelectionCodec.decode(stored)
            result = runCatching { change(current) }.getOrDefault(current)
            ModelSelectionCodec.encode(result)
        }
        forgetLegacy(context)
        return result
    }

    fun choose(context: Context, model: ModelRef?) = update(context) { it.choose(model) }

    private fun file(context: Context) = LockedJsonFile(context.filesDir, "model-selection", ModelSelectionCodec.MAX_BYTES)

    /** Earlier versions kept one model id in the workbench draft; move it once. */
    private fun migrate(context: Context): ModelSelectionState {
        val state = legacy(context)
        if (state != ModelSelectionState.AUTOMATIC) runCatching { update(context) { state } }
        return state
    }

    private fun legacy(context: Context): ModelSelectionState {
        val drafts = context.getSharedPreferences(LEGACY_PREFERENCES, Context.MODE_PRIVATE)
        val id = drafts.getString("target", null) ?: return ModelSelectionState.AUTOMATIC
        val name = drafts.getString("targetName", null)?.takeIf { it.isNotBlank() }?.take(ModelRef.MAX_NAME) ?: id
        return runCatching { ModelSelectionState.AUTOMATIC.choose(ModelRef(id, name)) }.getOrDefault(ModelSelectionState.AUTOMATIC)
    }

    private fun forgetLegacy(context: Context) {
        val drafts = context.getSharedPreferences(LEGACY_PREFERENCES, Context.MODE_PRIVATE)
        if (drafts.contains("target") || drafts.contains("targetName")) drafts.edit().remove("target").remove("targetName").apply()
    }
}
