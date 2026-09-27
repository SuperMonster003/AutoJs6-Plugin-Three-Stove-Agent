package io.github.supermonster003.autojs6.plugin.three.stove.agent

import org.autojs.plugin.common.api.PluginActions

/**
 * Identity constants shared by the manifest, the Binder services, the documentation, and the
 * tests. They must stay identical to the host-side registration (see `ROADMAP.md`, decision D1
 * and phase P1.5); the JVM manifest contract test fails when the manifest drifts from them.
 */
object ThreeStoveAgentPlugin {

    const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
    const val HOST_PACKAGE_NAME = "org.autojs.autojs6"

    const val ID = "ai-agent"
    const val ENGINE = "ai-agent"
    const val VARIANT = "default"
    const val AUTHOR = "SuperMonster003"

    /** Discovery contract of [ThreeStoveAgentPluginService]. */
    const val SERVICE_ACTION = "org.autojs.plugin.AI_AGENT"
    const val SERVICE_CATEGORY = "ai-agent"

    /**
     * Process suffix of [ThreeStoveAgentPluginService]: the agent loop, the run queue and the task
     * foreground service of roadmap D15 live there, away from the launcher UI process.
     */
    const val SERVICE_PROCESS = ":agent"

    /** Discovery contract of [ThreeStoveAgentPluginInfoService]. */
    const val INFO_ACTION = PluginActions.INFO

    /**
     * Binder descriptor of the `IAiAgentPlugin` AIDL from the host `ai-agent-api` module, staged
     * as a locked AAR in `libs/`. [ThreeStoveAgentPluginService] implements the full interface; the
     * contract test asserts that the bound Binder carries exactly this descriptor.
     */
    const val SERVICE_DESCRIPTOR = "org.autojs.plugin.ai.agent.api.IAiAgentPlugin"

    /**
     * Minimum AutoJs6 `versionCode` shipping P4.2 inspected node bindings
     * and host-side append with authoritative identity validation.
     */
    const val REQUIRED_HOST_VERSION = 5289L
}
