package io.github.supermonster003.autojs6.plugin.three.stove.agent

import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentActions
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentIds

/**
 * Identity constants shared by the manifest, the Binder services, the documentation, and the
 * tests. They must stay identical to the host-side registration (see `ROADMAP.md`, decision D1
 * and phase P1.5); the JVM manifest contract test fails when the manifest drifts from them.
 */
object ThreeStoveAgentPlugin {

    const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
    const val HOST_PACKAGE_NAME = "org.autojs.autojs6"

    const val ID = ThreeStoveAgentIds.PLUGIN_ID
    const val ENGINE = ThreeStoveAgentIds.ENGINE
    const val VARIANT = ThreeStoveAgentIds.VARIANT_DEFAULT
    const val AUTHOR = "SuperMonster003"

    /** Discovery contract of [ThreeStoveAgentPluginService], frozen by the host contract module. */
    const val SERVICE_ACTION = ThreeStoveAgentActions.SERVICE_ACTION
    const val SERVICE_CATEGORY = ThreeStoveAgentActions.SERVICE_CATEGORY

    /**
     * Process suffix of [ThreeStoveAgentPluginService]: the agent loop, the run queue and the task
     * foreground service of roadmap D15 live there, away from the launcher UI process.
     */
    const val SERVICE_PROCESS = ":agent"

    /** Discovery contract of [ThreeStoveAgentPluginInfoService]. */
    const val INFO_ACTION = PluginActions.INFO

    /**
     * Binder descriptor of the `IThreeStoveAgentPlugin` AIDL from the host `three-stove-agent-api` module, staged
     * as a locked AAR in `libs/`. [ThreeStoveAgentPluginService] implements the full interface; the
     * contract test asserts that the bound Binder carries exactly this descriptor.
     */
    const val SERVICE_DESCRIPTOR = "org.autojs.plugin.three.stove.agent.api.IThreeStoveAgentPlugin"

    /**
     * Minimum AutoJs6 `versionCode` shipping the 3-Stove Agent contract line (version 2). It
     * already includes the P4.2 inspected node bindings and the host-side append with
     * authoritative identity validation that build 5289 introduced.
     */
    const val REQUIRED_HOST_VERSION = ThreeStoveAgentIds.REQUIRED_HOST_VERSION_CODE
}
