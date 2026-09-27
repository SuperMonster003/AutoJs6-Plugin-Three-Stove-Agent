package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

/**
 * What the launcher screen can say about the AutoJs6 host in this preview. The full six-state
 * guidance (activation, enablement, authorization, trust, attached link) is owned by the host
 * inspector of roadmap P1.3; the plugin can only observe the package itself.
 */
enum class HostPresence {
    /** The host package is not installed. */
    MISSING,

    /** The host package is installed but disabled in the system settings. */
    DISABLED,

    /** The host package is installed and enabled but older than the required build. */
    INCOMPATIBLE,

    /** The host package is installed, enabled and recent enough. */
    READY,
}

/** Package facts the launcher reads from the package manager; `null` means not installed. */
data class HostPackageSnapshot(
    val enabled: Boolean,
    val versionCode: Long,
    val versionName: String,
)

/** Pure classification, kept free of Android types so the JVM test covers every branch. */
fun classifyHostPresence(snapshot: HostPackageSnapshot?, requiredVersionCode: Long): HostPresence = when {
    snapshot == null -> HostPresence.MISSING
    !snapshot.enabled -> HostPresence.DISABLED
    snapshot.versionCode < requiredVersionCode -> HostPresence.INCOMPATIBLE
    else -> HostPresence.READY
}
