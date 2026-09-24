package io.github.supermonster003.autojs6.plugin.ai.agent.model

/** Fixed diagnostics only; rejected model text, tool names and arguments are never retained. */
enum class DecisionRejection {
    DECISION_UNPARSABLE, TOOL_UNKNOWN, TOOL_DISABLED, TOOL_ARGUMENTS_INVALID, LIMIT_EXCEEDED;

    companion object {
        fun fromCode(code: String) = entries.firstOrNull { it.name == code } ?: DECISION_UNPARSABLE
    }
}
