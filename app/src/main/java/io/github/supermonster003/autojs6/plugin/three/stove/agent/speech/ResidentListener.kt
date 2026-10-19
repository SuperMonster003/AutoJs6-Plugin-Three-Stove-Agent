package io.github.supermonster003.autojs6.plugin.three.stove.agent.speech

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ThreeStoveAgentPlugin
import io.github.supermonster003.autojs6.plugin.three.stove.agent.service.AgentRuntime
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.LockedJsonFile
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.TaskEntries
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.TaskEntry

/**
 * Switch of the resident listener (roadmap D73, the 3-Stone AI D31 convention): [ResidentListenerService] is enabled
 * exactly while it is on, with no separate preference, so the other apps see whether Stove listens.
 */
internal object ResidentListener {
    fun component(context: Context) = ComponentName(context, ResidentListenerService::class.java)

    fun enabled(context: Context) =
        context.packageManager.getComponentEnabledSetting(component(context)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    /** Off restores the Manifest default (disabled). Never kills the app: the service stops itself or is brought down. */
    fun setEnabled(context: Context, enabled: Boolean) = context.packageManager.setComponentEnabledSetting(component(context),
        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP)

    /** Other apps whose resident listener is on: only enabled components answer the query, this package never counts. */
    fun others(context: Context): List<ResidentListenerApp> = runCatching {
        val manager = context.packageManager
        val found = manager.queryIntentServices(Intent(ThreeStoveAgentPlugin.RESIDENT_LISTENER_ACTION), PackageManager.GET_META_DATA).map { info ->
            val service = info.serviceInfo
            ResidentListenerApp(service.packageName, service.applicationInfo.loadLabel(manager).toString(),
                service.metaData?.getString(ThreeStoveAgentPlugin.RESIDENT_LISTENER_SETTINGS))
        }
        ResidentListenerApps.others(found, context.packageName)
    }.getOrDefault(emptyList())

    /** "Open xxx settings": the activity the other app names, else its launcher entry. */
    fun openSettings(context: Context, app: ResidentListenerApp): Boolean = listOfNotNull(
        app.settingsClass?.let { Intent().setComponent(ComponentName(app.packageName, it)) },
        context.packageManager.getLaunchIntentForPackage(app.packageName),
    ).any { runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess }

    /** Starts (or keeps) the service; only from a visible screen, because 3-Macaw Speech needs that to open the microphone (D74). */
    fun start(context: Context, action: String? = null): Boolean = runCatching {
        val intent = Intent(context, ResidentListenerService::class.java).setAction(action)
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
    }.isSuccess

    /** Turning the switch on starts its listening time (D81, D82) and the service; only from a visible screen (D74). */
    fun turnOn(context: Context): Boolean {
        ResidentSettingsStore(context).update { it.turnedOn(System.currentTimeMillis()) }
        setEnabled(context, true)
        return start(context)
    }

    /**
     * The workbench or the settings became visible (D67): an enabled listener without a rival resumes; one whose listening
     * time ran out while nothing ran is turned off instead (D82).
     */
    fun resume(context: Context) {
        when (ResidentResume.of(enabled(context), ResidentSettingsStore(context).read(), System.currentTimeMillis())) {
            ResidentResume.TURN_OFF -> turnOff(context)
            ResidentResume.RESUME -> if (others(context).isEmpty()) start(context)
            ResidentResume.NOTHING -> Unit
        }
    }

    /** Keywords or sensitivity changed: a running listener reopens its wake session. */
    fun restart(context: Context) { if (enabled(context)) start(context, ResidentListenerService.ACTION_RESTART) }

    /** The listening time or "only while charging" changed: a running listener follows without reopening its wake session. */
    fun refresh(context: Context) { if (enabled(context)) start(context, ResidentListenerService.ACTION_REFRESH) }

    fun turnOff(context: Context) {
        runCatching { context.stopService(Intent(context, ResidentListenerService::class.java)) }
        setEnabled(context, false)
        ResidentNotifications.cancelReminders(context)
    }
}

/** Preferences file shared by the settings screen and the :agent service, read fresh on every session (D75). */
internal class ResidentSettingsStore(context: Context) {
    private val file = LockedJsonFile(context.filesDir, "resident-listener", 4096)
    fun read(): ResidentSettings = ResidentSettings.decode(file.read())
    fun write(value: ResidentSettings) = file.write(ResidentSettings.encode(value))
    /** Read-modify-write under the file lock, since both processes write the listening time. */
    fun update(transform: (ResidentSettings) -> ResidentSettings): ResidentSettings =
        ResidentSettings.decode(file.update { ResidentSettings.encode(transform(ResidentSettings.decode(it))) })
}

/** "Only while charging" (D81, D82): plugged into any power source, charging or not (battery care may hold the charge). */
internal object ResidentPower {
    fun plugged(context: Context): Boolean = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }.getOrDefault(true)
}

/** Hands a dictated sentence to the user as a draft (D62-D64, D77); main thread of the :agent process. */
internal object VoiceDrafts {
    fun deliver(context: Context, text: String): VoiceDraftRoute {
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val card = AgentRuntime.peek()?.voiceDraft(text) == true
        // Android 10+ lets a background service open an activity only with the overlay permission.
        val backgroundStarts = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context)
        var route = VoiceDraftRoute.of(card, power.isInteractive && !keyguard.isKeyguardLocked, backgroundStarts)
        if (route == VoiceDraftRoute.WORKBENCH && runCatching {
                context.startActivity(TaskEntries.intent(context, TaskEntry(text)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isFailure) route = VoiceDraftRoute.NOTIFICATION
        if (route == VoiceDraftRoute.NOTIFICATION) ResidentNotifications.draft(context, text)
        return route
    }
}
