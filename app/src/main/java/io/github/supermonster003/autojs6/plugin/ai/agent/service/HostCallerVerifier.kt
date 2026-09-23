package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import io.github.supermonster003.autojs6.plugin.ai.agent.AiAgentPlugin

/** Package ownership, installed version and the current signer set are checked at every host entry. */
internal class HostCallerVerifier(context: Context) {
    private val pm = context.packageManager
    private val pluginPackage = context.packageName
    @Suppress("DEPRECATION")
    fun enforce(): Int {
        val uid = Binder.getCallingUid()
        val valid = runCatching {
            val host = pm.getPackageInfo(AiAgentPlugin.HOST_PACKAGE_NAME,
                if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
            val plugin = pm.getPackageInfo(pluginPackage,
                if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
            fun signers(info: android.content.pm.PackageInfo) =
                (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures).orEmpty().toSet()
            val version = if (Build.VERSION.SDK_INT >= 28) host.longVersionCode else host.versionCode.toLong()
            uid == host.applicationInfo?.uid && AiAgentPlugin.HOST_PACKAGE_NAME in pm.getPackagesForUid(uid).orEmpty() &&
                version >= AiAgentPlugin.REQUIRED_HOST_VERSION && signers(host).isNotEmpty() && signers(host) == signers(plugin)
        }.getOrDefault(false)
        if (!valid) throw SecurityException("Caller is not the installed same-signer AutoJs6 host")
        return uid
    }
    fun enforceOwner(uid: Int) { if (Binder.getCallingUid() != uid || enforce() != uid) throw SecurityException("Host link owner changed") }
}
