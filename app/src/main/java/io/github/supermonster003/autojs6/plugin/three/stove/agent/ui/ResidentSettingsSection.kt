package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.speech.ListeningDuration
import io.github.supermonster003.autojs6.plugin.three.stove.agent.speech.MacawSpeech
import io.github.supermonster003.autojs6.plugin.three.stove.agent.speech.ResidentAvailability
import io.github.supermonster003.autojs6.plugin.three.stove.agent.speech.ResidentKeywords
import io.github.supermonster003.autojs6.plugin.three.stove.agent.speech.ResidentListener
import io.github.supermonster003.autojs6.plugin.three.stove.agent.speech.ResidentListenerApp
import io.github.supermonster003.autojs6.plugin.three.stove.agent.speech.ResidentNotifications
import io.github.supermonster003.autojs6.plugin.three.stove.agent.speech.ResidentSettings
import io.github.supermonster003.autojs6.plugin.three.stove.agent.speech.ResidentSettingsStore
import io.github.supermonster003.autojs6.plugin.three.stove.agent.speech.WakeSensitivity
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*

/**
 * Settings rows of the resident listener (roadmap D61-D75, D81). The switch mirrors the component state (D73); whether
 * 3-Macaw Speech can serve it is probed on every resume (D71, D74). Turning it on checks for another resident listener,
 * asks for notifications and, once, for the overlay permission (D63) before enabling the component and starting its
 * listening time. The listening time and "only while charging" sit right below the switch and apply at once (D81, D82).
 */
internal class ResidentSettingsSection(private val activity: SettingsActivity) {
    private val kit get() = activity.kit
    private val store = ResidentSettingsStore(activity)
    private lateinit var toggle: SettingRow
    private lateinit var openMacaw: SettingRow
    private var openMacawDivider: View? = null
    /** The listening time row, exposed for instrumentation. */
    internal lateinit var duration: ChoiceRow; private set
    private lateinit var charging: SettingRow
    private lateinit var keywords: SettingRow
    private lateinit var sensitivity: ChoiceRow
    /** Null while probing. */
    internal var availability: ResidentAvailability? = null; private set
    private var cancelProbe: (() -> Unit)? = null
    /** The overlay settings screen is open on behalf of the switch; returning finishes turning it on. */
    internal var awaitingOverlay = false
    /** The last dialog of this section, exposed for instrumentation. */
    internal var dialog: AlertDialog? = null; private set

    fun build(page: LinearLayout) {
        toggle = kit.switchRow(activity.getString(R.string.settings_resident), activity.getString(R.string.settings_resident_checking),
            R.drawable.ic_mic, ResidentListener.enabled(activity), "resident") { on -> if (on) turnOn() else turnOff() }
        activity.add(page, "resident", toggle)
        openMacaw = kit.settingRow(activity.getString(R.string.settings_resident_open_macaw), activity.getString(R.string.settings_resident_open_macaw_note),
            R.drawable.ic_download, "resident-macaw") { openMacaw() }
        openMacawDivider = activity.add(page, "resident-macaw", openMacaw)
        val settings = store.read()
        duration = kit.choiceRow(activity.getString(R.string.settings_resident_duration), DURATION_LABELS.map(activity::getString),
            ListeningDuration.entries.indexOf(settings.duration), R.drawable.ic_timer, "resident-duration") { index ->
            val now = System.currentTimeMillis()
            save(restart = false) { store.update { it.withDuration(ListeningDuration.entries[index], ResidentListener.enabled(activity), now) } }
        }
        activity.add(page, "resident-duration", duration.row)
        charging = kit.switchRow(activity.getString(R.string.settings_resident_charging), activity.getString(R.string.settings_resident_charging_note),
            R.drawable.ic_battery_charging, settings.chargingOnly, "resident-charging") { on ->
            save(restart = false) { store.update { it.copy(chargingOnly = on) } }
        }
        activity.add(page, "resident-charging", charging)
        keywords = kit.settingRow(activity.getString(R.string.settings_resident_keywords), settings.keywords.joinToString(", "),
            R.drawable.ic_spark, "resident-keywords") { editKeywords() }
        activity.add(page, "resident-keywords", keywords)
        sensitivity = kit.choiceRow(activity.getString(R.string.settings_resident_sensitivity),
            listOf(R.string.resident_sensitivity_high, R.string.resident_sensitivity_medium, R.string.resident_sensitivity_low).map(activity::getString),
            WakeSensitivity.entries.indexOf(settings.sensitivity), R.drawable.ic_settings_tune, "resident-sensitivity") { index ->
            save { store.update { it.copy(sensitivity = WakeSensitivity.entries[index]) } }
        }
        activity.add(page, "resident-sensitivity", sensitivity.row)
        render()
    }

    /** Every resume: the component state may have changed elsewhere (notification Turn off) and 3-Macaw Speech may have too. */
    fun resume() {
        if (awaitingOverlay) { awaitingOverlay = false; enable() }
        render()
        cancelProbe?.invoke()
        cancelProbe = MacawSpeech.probe(activity) { probe ->
            cancelProbe = null
            availability = ResidentAvailability.of(probe)
            render()
        }
    }

    fun stop() { cancelProbe?.invoke(); cancelProbe = null; dialog?.dismiss(); dialog = null }

    private fun render() {
        val on = ResidentListener.enabled(activity)
        val others = if (on) emptyList() else ResidentListener.others(activity)
        val state = availability
        toggle.switch!!.isChecked = on
        toggle.setSummary(when {
            state == null -> activity.getString(R.string.settings_resident_checking)
            state != ResidentAvailability.READY -> reason(state)
            others.isNotEmpty() -> activity.getString(R.string.settings_resident_conflict, others.joinToString(", ") { it.label })
            else -> store.read().let { settings ->
                // An enabled listener with a listening time says when it ends (D81).
                val note = activity.getString(R.string.settings_resident_note, settings.keywords.first())
                val deadline = settings.deadline?.takeIf { on && !settings.expired(System.currentTimeMillis()) }
                if (deadline == null) note else note + "\n" + ResidentNotifications.until(activity, deadline)
            }
        })
        // An enabled listener can always be turned off, even when 3-Macaw Speech lost a model meanwhile.
        toggle.setEnabled(on || state == ResidentAvailability.READY)
        val macaw = state != null && state != ResidentAvailability.READY && state != ResidentAvailability.MACAW_MISSING &&
            MacawSpeech.launchIntent(activity) != null
        openMacaw.view.visibility = if (macaw) View.VISIBLE else View.GONE
        openMacawDivider?.visibility = openMacaw.view.visibility
    }

    private fun reason(state: ResidentAvailability) = activity.getString(when (state) {
        ResidentAvailability.MACAW_MISSING -> R.string.settings_resident_missing
        ResidentAvailability.MACAW_UNAVAILABLE -> R.string.settings_resident_unavailable
        ResidentAvailability.MACAW_INCOMPATIBLE -> R.string.settings_resident_incompatible
        ResidentAvailability.NO_WAKE_MODEL -> R.string.settings_resident_no_wake_model
        ResidentAvailability.NO_RECOGNITION_MODEL -> R.string.settings_resident_no_recognition_model
        ResidentAvailability.MICROPHONE_DENIED -> R.string.settings_resident_microphone
        ResidentAvailability.READY -> R.string.settings_resident_checking
    })

    private fun turnOn() {
        toggle.switch!!.isChecked = false
        if (availability != ResidentAvailability.READY) { render(); return }
        val others = ResidentListener.others(activity)
        if (others.isNotEmpty()) { conflict(others); render(); return }
        if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST)
        if (Settings.canDrawOverlays(activity)) { enable(); return }
        // Asked once per turning on (D63): without it a background service cannot open the draft, so a notification does.
        dialog = kit.materialDialog().setTitle(R.string.resident_overlay_title).setMessage(R.string.resident_overlay_message)
            .setPositiveButton(R.string.resident_overlay_grant) { _, _ ->
                awaitingOverlay = true
                runCatching { activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.packageName))) }
                    .onFailure { awaitingOverlay = false; enable() }
            }
            .setNegativeButton(R.string.resident_overlay_skip) { _, _ -> enable() }
            .setOnCancelListener { render() }
            .show().also { kit.tintDialogButtons(it) }
    }

    private fun enable() {
        if (ResidentListener.others(activity).isNotEmpty()) { render(); return }
        if (!ResidentListener.turnOn(activity)) kit.snackbar(activity.scaffold.root, activity.getString(R.string.settings_open_failed))
        render()
    }

    private fun turnOff() { ResidentListener.turnOff(activity); render() }

    /** D73: "only one resident listener", with a way to each rival's settings; Stove cannot turn them off itself. */
    private fun conflict(others: List<ResidentListenerApp>) {
        val names = others.joinToString(", ") { it.label }
        val message = activity.getString(R.string.resident_conflict_message, names)
        val builder = kit.materialDialog().setTitle(R.string.resident_conflict_title).setNegativeButton(android.R.string.cancel, null)
        dialog = if (others.size == 1) builder.setMessage(message)
            .setPositiveButton(activity.getString(R.string.resident_conflict_open, others[0].label)) { _, _ -> open(others[0]) }
            .show().also { kit.tintDialogButtons(it) }
        else builder.setTitle(message).setItems(others.map { activity.getString(R.string.resident_conflict_open, it.label) }.toTypedArray()) { _, index ->
            open(others[index])
        }.show().also { kit.tintDialogButtons(it) }
    }

    private fun open(app: ResidentListenerApp) {
        if (!ResidentListener.openSettings(activity, app)) kit.snackbar(activity.scaffold.root, activity.getString(R.string.settings_open_failed))
    }

    private fun openMacaw() {
        val intent = MacawSpeech.launchIntent(activity)
        if (intent == null || runCatching { activity.startActivity(intent) }.isFailure) kit.snackbar(activity.scaffold.root, activity.getString(R.string.settings_open_failed))
    }

    private fun editKeywords() {
        dialog = kit.inputDialog(activity.getString(R.string.settings_resident_keywords), store.read().keywords.joinToString("\n"),
            message = activity.getString(R.string.settings_resident_keywords_note, ResidentKeywords.MAX_KEYWORDS, ResidentKeywords.MAX_CHARACTERS),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE, singleLine = false,
            validate = { if (ResidentKeywords.parse(it) == null) activity.getString(R.string.settings_resident_keywords_invalid) else null }) { text ->
            save { store.update { it.copy(keywords = requireNotNull(ResidentKeywords.parse(text))) } }
        }
    }

    /**
     * Saves under the file lock and shows the result. New keywords or sensitivity reopen a running wake session; the
     * listening time and the power rule ([restart] false) only make the service apply them (D82).
     */
    private fun save(restart: Boolean = true, write: () -> ResidentSettings) {
        val value = runCatching(write).getOrElse { kit.snackbar(activity.scaffold.root, activity.getString(R.string.settings_error)); render(); return }
        keywords.setSummary(value.keywords.joinToString(", "))
        sensitivity.select(WakeSensitivity.entries.indexOf(value.sensitivity))
        duration.select(ListeningDuration.entries.indexOf(value.duration))
        charging.switch!!.isChecked = value.chargingOnly; charging.refreshDescription()
        if (restart) ResidentListener.restart(activity) else ResidentListener.refresh(activity)
        render()
    }

    private companion object {
        const val NOTIFICATION_REQUEST = 21
        /** In the order of [ListeningDuration]. */
        val DURATION_LABELS = listOf(R.string.resident_duration_10, R.string.resident_duration_30, R.string.resident_duration_60, R.string.resident_duration_always)
    }
}
