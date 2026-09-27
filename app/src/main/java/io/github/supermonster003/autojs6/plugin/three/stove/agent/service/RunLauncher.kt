package io.github.supermonster003.autojs6.plugin.three.stove.agent.service

import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.PresetSnapshot
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.AgentSettings
import org.autojs.plugin.ai.agent.api.AiAgentContract as C

/** The admission gate shared by host scripts and every private UI entry.
 * Admission queues preparation, which promotes the foreground service before any broker work. */
internal object RunLauncher {
    fun <T> start(state: String, config: LinkConfiguration, json: String, presets: PresetSnapshot = PresetSnapshot.INITIAL,
                  settings: AgentSettings? = null, pluginUi: Boolean = false, admit: (StartRequest) -> T): T {
        if (state != C.LINK_STATE_ATTACHED) throw WireFailure(
            if (state == C.LINK_STATE_DETACHED) C.ERROR_LINK_DETACHED else C.ERROR_HOST_UNAVAILABLE)
        return admit(StartRequest.parse(json, config, presets, settings, pluginUi))
    }

    fun uiRequest(goal: String, preset: String, locale: String, target: String? = null): String {
        require(goal.isNotBlank() && goal.utf8Size() <= 4096)
        target?.let(io.github.supermonster003.autojs6.plugin.three.stove.agent.store.PresetCodec::target)
        return jsonObject("goal" to goal.json(), "origin" to "ui".json(), "options" to jsonObject(
            "preset" to preset.json(), "locale" to locale.json(), "interaction" to "plugin".json()).apply {
                target?.let { addProperty("target", it) }
            }).toString()
    }
}
