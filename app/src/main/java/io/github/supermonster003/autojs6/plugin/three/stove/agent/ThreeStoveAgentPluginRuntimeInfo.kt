package io.github.supermonster003.autojs6.plugin.three.stove.agent

/**
 * Pure-data view of the metadata reported through `IPluginInfoProvider.getInfo()` (and, from
 * roadmap P2.5 on, `IAiAgentPlugin.getInfo()` / `getCapabilities()`).
 *
 * Android-specific lookups (package version, localized strings, raw resources) happen in
 * [threeStoveAgentPluginRuntimeInfo]; this class keeps the mapping itself testable on the JVM.
 */
data class ThreeStoveAgentPluginRuntimeInfo(
    val name: String,
    val description: String,
    val instruction: String?,
    val versionName: String,
    val versionCode: Long,
    val versionDate: String,
) {
    val author: String get() = ThreeStoveAgentPlugin.AUTHOR
    val id: String get() = ThreeStoveAgentPlugin.ID
    val engine: String get() = ThreeStoveAgentPlugin.ENGINE
    val variant: String get() = ThreeStoveAgentPlugin.VARIANT

    /** Empty on purpose: the plugin ships no native code and runs on any ABI (roadmap D27). */
    val supportedAbis: Array<String> get() = emptyArray()

    val requiresHostVersion: Long get() = ThreeStoveAgentPlugin.REQUIRED_HOST_VERSION
}
