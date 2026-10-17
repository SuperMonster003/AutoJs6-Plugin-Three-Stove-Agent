package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reboot and update end the resident listener (roadmap D67). Nothing restarts here: Android 14+ forbids opening the
 * microphone of 3-Macaw Speech from these broadcasts, so an enabled listener only posts "tap to resume".
 */
class ResidentResumeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (ResidentListener.enabled(context)) ResidentNotifications.resume(context)
    }
}
