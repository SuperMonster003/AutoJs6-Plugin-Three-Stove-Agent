package io.github.supermonster003.autojs6.plugin.three.stove.agent

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginInfo

/** Answers `org.autojs.plugin.INFO` (category `three-stove-agent`) for the AutoJs6 plugin center. */
class ThreeStoveAgentPluginInfoService : Service() {

    private val binder = object : IPluginInfoProvider.Stub() {
        override fun getInfo(): PluginInfo {
            return threeStoveAgentPluginRuntimeInfo().toPluginInfo().apply {
                // Explicit and auditable: no ABI restriction (see AGENTS.md, PluginInfo rules).
                supportedAbis = emptyArray()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
