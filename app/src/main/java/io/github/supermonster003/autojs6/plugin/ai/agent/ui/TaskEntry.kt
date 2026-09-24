package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import io.github.supermonster003.autojs6.plugin.ai.agent.store.PresetCodec

/** External entries carry drafts only. They cannot carry grants, responses or run options. */
internal data class TaskEntry(val goal: String, val preset: String? = null) {
    init {
        io.github.supermonster003.autojs6.plugin.ai.agent.model.AgentJson.checkUnicode(goal)
        require(goal.toByteArray(Charsets.UTF_8).size <= 4096 && '\u0000' !in goal)
        preset?.let(PresetCodec::name)
    }
}
