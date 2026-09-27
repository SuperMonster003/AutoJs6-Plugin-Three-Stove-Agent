package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import io.github.supermonster003.autojs6.plugin.ai.agent.model.utf8Size

/** Lexical admission only. The host still resolves symlinks and enforces its actual workspace root. */
internal object WorkspacePath {
    fun requireValid(value: String) {
        val relative = value.removePrefix("./").removeSuffix("/")
        val root = value == "." || value == "./"
        val drive = relative.length >= 2 && relative[0].isLetter() && relative[1] == ':'
        if (value.utf8Size() > 4096 || (!root &&
            (relative.startsWith('/') || drive || '\\' in relative || relative.any(Character::isISOControl) ||
                relative.split('/').any { it.isEmpty() || it == "." || it == ".." }))) {
            throw ToolFailure("TOOL_ARGUMENTS_INVALID", "Use a workspace-relative path with forward slashes, at most 4096 UTF-8 bytes, and no parent segments, drive prefix or control characters.")
        }
    }
}
