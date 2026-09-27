package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.PresetCodec
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.utf8Size

/** External entries carry drafts only. They cannot carry grants, responses or run options. */
internal data class TaskEntry(val goal: String, val preset: String? = null) {
    init {
        io.github.supermonster003.autojs6.plugin.three.stove.agent.model.AgentJson.checkUnicode(goal)
        require(goal.utf8Size() <= 4096 && '\u0000' !in goal)
        preset?.let(PresetCodec::name)
    }
}
