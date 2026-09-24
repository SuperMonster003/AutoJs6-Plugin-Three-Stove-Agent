package io.github.supermonster003.autojs6.plugin.ai.agent.service

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.PresetSnapshot
import io.github.supermonster003.autojs6.plugin.ai.agent.store.AgentSettings
import org.autojs.plugin.ai.agent.api.AiAgentContract as C

/** The admission gate shared by host scripts and every private UI entry.
 * Admission queues preparation, which promotes the foreground service before any broker work. */
internal object RunLauncher {
    fun <T> start(state: String, config: LinkConfiguration, json: String, presets: PresetSnapshot = PresetSnapshot.INITIAL,
                  settings: AgentSettings? = null, admit: (StartRequest) -> T): T {
        if (state != C.LINK_STATE_ATTACHED) throw WireFailure(
            if (state == C.LINK_STATE_DETACHED) C.ERROR_LINK_DETACHED else C.ERROR_HOST_UNAVAILABLE)
        return admit(StartRequest.parse(json, config, presets, settings))
    }

    fun uiRequest(goal: String, preset: String, locale: String): String {
        require(goal.isNotBlank() && goal.toByteArray(Charsets.UTF_8).size <= 4096)
        return jsonObject("goal" to goal.json(), "origin" to "ui".json(), "options" to jsonObject(
            "preset" to preset.json(), "locale" to locale.json(), "interaction" to "plugin".json())).toString()
    }
}
