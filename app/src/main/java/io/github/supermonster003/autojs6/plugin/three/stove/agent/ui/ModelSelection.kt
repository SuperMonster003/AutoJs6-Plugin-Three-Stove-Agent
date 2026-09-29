package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Context
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.*

/**
 * The shared model choice for new tasks from the workbench and the floating ball. It lives in
 * `filesDir/model-selection.json`, readable from both the UI and the :agent process, and is never
 * part of a preset. Nothing is read from anywhere else: the one-time move out of the old workbench
 * draft preferences shipped with 1.2.0 and was retired after that release (roadmap P13).
 */
internal object ModelSelection {
    fun read(context: Context): ModelSelectionState =
        file(context).read()?.let(ModelSelectionCodec::decode) ?: ModelSelectionState.AUTOMATIC

    fun update(context: Context, change: (ModelSelectionState) -> ModelSelectionState): ModelSelectionState {
        var result = ModelSelectionState.AUTOMATIC
        file(context).update { stored ->
            val current = if (stored == null) ModelSelectionState.AUTOMATIC else ModelSelectionCodec.decode(stored)
            result = runCatching { change(current) }.getOrDefault(current)
            ModelSelectionCodec.encode(result)
        }
        return result
    }

    fun choose(context: Context, model: ModelRef?) = update(context) { it.choose(model) }

    private fun file(context: Context) = LockedJsonFile(context.filesDir, "model-selection", ModelSelectionCodec.MAX_BYTES)
}
