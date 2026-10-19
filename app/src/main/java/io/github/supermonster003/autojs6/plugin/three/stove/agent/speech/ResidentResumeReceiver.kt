package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reboot and update end the resident listener (roadmap D67). Nothing restarts here: Android 14+ forbids opening the
 * microphone of 3-Macaw Speech from these broadcasts, so an enabled listener only posts "tap to resume". One whose
 * listening time ran out meanwhile (D82) is turned off without a reminder.
 */
class ResidentResumeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        when (ResidentResume.of(ResidentListener.enabled(context), ResidentSettingsStore(context).read(), System.currentTimeMillis())) {
            ResidentResume.TURN_OFF -> ResidentListener.turnOff(context)
            ResidentResume.RESUME -> ResidentNotifications.resume(context)
            ResidentResume.NOTHING -> Unit
        }
    }
}
