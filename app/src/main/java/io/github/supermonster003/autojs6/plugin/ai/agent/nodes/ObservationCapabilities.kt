package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

object ObservationCapabilities {
    fun ocrAvailable(optionalMethods: Set<String>, grantedMethods: Set<String>, grantedPermissions: Set<String>): Boolean =
        "accessibility.readScreenText" in optionalMethods && "accessibility.readScreenText" in grantedMethods &&
            grantedPermissions.containsAll(setOf("accessibility", "screen_capture", "ocr"))
}
