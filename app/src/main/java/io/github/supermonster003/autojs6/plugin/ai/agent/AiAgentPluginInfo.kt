package io.github.supermonster003.autojs6.plugin.ai.agent

import android.content.Context
import android.os.Build
import android.os.Bundle
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.ai.agent.api.AiAgentCapabilityKeys
import org.autojs.plugin.ai.agent.api.AiAgentContract

/** Collects the installed package version and the localized metadata of this plugin. */
internal fun Context.aiAgentPluginRuntimeInfo(): AiAgentPluginRuntimeInfo {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    return AiAgentPluginRuntimeInfo(
        name = getString(R.string.app_name),
        description = getString(R.string.plugin_description),
        instruction = resources.openRawResource(R.raw.plugin_instruction)
            .bufferedReader()
            .use { it.readText() },
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = versionCode,
        versionDate = getString(R.string.plugin_version_date),
    )
}

/** Maps the pure-data view onto the host contract parcelable. */
internal fun AiAgentPluginRuntimeInfo.toPluginInfo(): PluginInfo {
    val runtimeInfo = this
    return PluginInfo().apply {
        name = runtimeInfo.name
        description = runtimeInfo.description
        instruction = runtimeInfo.instruction
        author = runtimeInfo.author
        collaborators = null
        versionName = runtimeInfo.versionName
        versionCode = runtimeInfo.versionCode
        versionDate = runtimeInfo.versionDate
        id = runtimeInfo.id
        engine = runtimeInfo.engine
        variant = runtimeInfo.variant
        supportedAbis = runtimeInfo.supportedAbis
        capabilities = runtimeInfo.capabilitiesBundle()
    }
}

/** Advertise only the installed V1 loop; future native tools, vision and MCP are not capabilities. */
internal fun AiAgentPluginRuntimeInfo.capabilitiesBundle(): Bundle = Bundle().apply {
    putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, requiresHostVersion)
    putInt(AiAgentCapabilityKeys.CONTRACT_VERSION, AiAgentContract.CONTRACT_VERSION)
    putStringArray(AiAgentCapabilityKeys.TOOL_GROUPS, arrayOf("observe", "act", "script", "user", "gesture", "files", "shell"))
    putStringArray(AiAgentCapabilityKeys.FEATURES, arrayOf(AiAgentCapabilityKeys.FEATURE_STRUCTURED_JSON_LOOP))
}
