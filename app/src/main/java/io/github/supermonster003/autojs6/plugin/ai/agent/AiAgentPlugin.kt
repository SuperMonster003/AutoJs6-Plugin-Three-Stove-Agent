package io.github.supermonster003.autojs6.plugin.ai.agent

import org.autojs.plugin.common.api.PluginActions

/**
 * Identity constants shared by the manifest, the Binder services, the documentation, and the
 * tests. They must stay identical to the host-side registration (see `ROADMAP.md`, decision D1
 * and phase P1.5); the JVM manifest contract test fails when the manifest drifts from them.
 */
object AiAgentPlugin {

    const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.ai.agent"
    const val HOST_PACKAGE_NAME = "org.autojs.autojs6"

    const val ID = "ai-agent"
    const val ENGINE = "ai-agent"
    const val VARIANT = "default"
    const val AUTHOR = "SuperMonster003"

    /** Discovery contract of [AiAgentPluginService]. */
    const val SERVICE_ACTION = "org.autojs.plugin.AI_AGENT"
    const val SERVICE_CATEGORY = "ai-agent"

    /**
     * Process suffix of [AiAgentPluginService]: the agent loop, the run queue and the task
     * foreground service of roadmap D15 live there, away from the launcher UI process.
     */
    const val SERVICE_PROCESS = ":agent"

    /** Discovery contract of [AiAgentPluginInfoService]. */
    const val INFO_ACTION = PluginActions.INFO

    /**
     * Binder descriptor of the `IAiAgentPlugin` AIDL that the host defines in its
     * `ai-agent-api` module (roadmap P1.1). Until that contract is staged in `libs/`, the
     * service exposes a placeholder Binder carrying only this descriptor.
     */
    const val SERVICE_DESCRIPTOR = "org.autojs.plugin.ai.agent.api.IAiAgentPlugin"

    /**
     * Minimum AutoJs6 `versionCode` shipping P3.3 result reporting, manifest checks
     * and invocation-scoped stopping, including cancellation before engine startup.
     */
    const val REQUIRED_HOST_VERSION = 5287L
}
