package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.view.View
import android.widget.LinearLayout
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*

/** What the workbench knows about the host link, reduced to one banner state. */
internal enum class LinkStage { ATTACHED, CONNECTING, TIMED_OUT, ROOTS_REJECTED, WAITING }

/**
 * One banner for every non-attached host state: missing, disabled, incompatible, connecting, timed
 * out, script roots rejected, or installed but not yet authorized. Hidden once attached.
 */
internal class ConnectionBanner(private val kit: Kit, onConnect: () -> Unit, onOpenHost: () -> Unit) {
    private val banner = Banner(kit)
    val view: LinearLayout get() = banner.view
    val message = banner.message.apply { id = R.id.launcher_host_status; textAlignment = View.TEXT_ALIGNMENT_VIEW_START }
    val openHost = kit.textButton(kit.string(R.string.launcher_open_host), "host") { onOpenHost() }.apply { id = R.id.launcher_open_host }
    val connect = kit.tonalButton(kit.string(R.string.launcher_connect), "connect") { onConnect() }.apply { id = R.id.launcher_connect }
    init {
        banner.actions.addView(openHost)
        banner.actions.addView(connect, LinearLayout.LayoutParams(-2, -2).apply { marginStart = kit.dp(Ui.SPACE_SM) })
    }

    fun render(presence: HostPresence, host: HostPackageSnapshot?, stage: LinkStage, requiredVersion: Long) {
        if (presence == HostPresence.READY && stage == LinkStage.ATTACHED) { banner.hide(); return }
        val (text, tone) = when (presence) {
            HostPresence.MISSING -> kit.string(R.string.launcher_host_missing, requiredVersion) to Tone.WARNING
            HostPresence.DISABLED -> kit.string(R.string.launcher_host_disabled) to Tone.WARNING
            HostPresence.INCOMPATIBLE -> kit.string(R.string.launcher_host_incompatible, host!!.versionCode, requiredVersion) to Tone.DANGER
            HostPresence.READY -> when (stage) {
                LinkStage.CONNECTING -> kit.string(R.string.launcher_link_connecting) to Tone.NEUTRAL
                LinkStage.ROOTS_REJECTED -> kit.string(R.string.script_roots_rejected) to Tone.WARNING
                LinkStage.TIMED_OUT -> kit.string(R.string.launcher_link_timeout) to Tone.WARNING
                else -> kit.string(R.string.launcher_host_ready, host!!.versionName, host.versionCode) to Tone.NEUTRAL
            }
        }
        banner.show(text, tone, if (tone == Tone.NEUTRAL) R.drawable.ic_hub else R.drawable.ic_warning)
        val ready = presence == HostPresence.READY
        connect.isEnabled = ready && stage != LinkStage.CONNECTING; openHost.isEnabled = ready
        connect.visibility = if (ready) View.VISIBLE else View.GONE
        openHost.visibility = if (ready) View.VISIBLE else View.GONE
    }
}
