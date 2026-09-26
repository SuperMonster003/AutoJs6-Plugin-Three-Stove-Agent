package io.github.supermonster003.autojs6.plugin.ai.agent.update

import android.app.*
import android.content.*
import android.net.Uri
import android.os.*
import android.widget.Toast
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.showStyled
import java.util.concurrent.Executors

/** Visible-screen checks. Automatic checks are opt-in, silent on failure and respect ignored releases. */
internal class AppUpdateCoordinator(private val activity: Activity, private val installed: String) {
    private val preferences = activity.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var pending: UpdateCancellation? = null
    private var generation = 0
    internal var dialog: AlertDialog? = null; private set
    private val settings = AppUpdateSettings(activity)
    fun checkAutomatically() {
        val now = System.currentTimeMillis()
        val last = preferences.takeIf { it.contains("automaticAttempt") }?.getLong("automaticAttempt", 0)
        if (pending != null || !UpdateSchedulePolicy.automaticFetchDue(settings.automatic, last, now)) return
        preferences.edit().putLong("automaticAttempt", now).apply()
        check(automatic = true)
    }
    fun check(automatic: Boolean = false) {
        if (pending != null) return
        val cached = preferences.getString("release", null)
        val parsed = if (cached.isNullOrEmpty()) null else runCatching { ReleaseInfoCodec.decode(cached) }.getOrNull()
        val last = if (preferences.contains("checked") && (cached == "" || parsed != null)) preferences.getLong("checked", 0) else null
        if (!automatic && !UpdateSchedulePolicy.manualFetchDue(last, System.currentTimeMillis())) { present(parsed, false); return }
        val call = UpdateCancellation(); pending = call; val expected = ++generation
        if (!automatic) dialog = AlertDialog.Builder(activity).setMessage(R.string.update_checking)
            .setNegativeButton(android.R.string.cancel) { _, _ -> cancel() }.setOnCancelListener { cancel() }.showStyled()
        val timeout = Runnable {
            if (pending === call) { cancel(); if (!automatic) toast(R.string.update_failed) }
        }
        main.postDelayed(timeout, 25_000)
        worker.execute {
            val result = runCatching { (sourceOverride ?: AppUpdateRepository()).fetchLatest(call) }
                .getOrElse { UpdateResult.Failure(UpdateFailure.NETWORK) }
            main.post {
                if (generation != expected || call.cancelled || activity.isFinishing || activity.isDestroyed) return@post
                main.removeCallbacks(timeout); pending = null; dialog?.dismiss(); dialog = null
                when (result) {
                    is UpdateResult.Success -> {
                        val encoded = runCatching { result.release?.let(ReleaseInfoCodec::encode).orEmpty() }
                        if (encoded.isFailure) { if (!automatic) toast(R.string.update_failed) } else {
                            preferences.edit().putString("release", encoded.getOrThrow()).putLong("checked", System.currentTimeMillis()).apply()
                            present(result.release, automatic)
                        }
                    }
                    is UpdateResult.Failure -> if (!automatic) toast(R.string.update_failed)
                }
            }
        }
    }
    private fun present(release: ReleaseInfo?, automatic: Boolean) {
        if (release == null) { if (!automatic) toast(R.string.update_no_release); return }
        if (!AppVersionPolicy.isNewer(release.tag, installed)) { if (!automatic) toast(R.string.update_current); return }
        val ignored = settings.ignored.any { AppVersionPolicy.isIgnored(release.tag, it) }
        if (automatic && ignored) return
        dialog?.dismiss()
        dialog = AlertDialog.Builder(activity).setTitle(activity.getString(R.string.update_available, release.tag))
            .setMessage(activity.getString(R.string.update_installed, installed) + "\n\n" + release.notes)
            .setPositiveButton(R.string.update_open_release) { _, _ ->
                if (ReleaseInfoCodec.validUrl(release.url, release.tag)) openPage(activity, release.url)
            }.setNeutralButton(R.string.release_history_title) { _, _ -> activity.startActivity(Intent(activity, ReleaseHistoryActivity::class.java)) }
            .setNegativeButton(if (ignored) R.string.update_unignore else R.string.update_ignore) { _, _ ->
                if (ignored) settings.unignore(listOf(release.tag)) else settings.ignore(release.tag)
            }.showStyled()
    }
    fun cancel() { generation++; pending?.cancel(); pending = null; main.removeCallbacksAndMessages(null); dialog?.dismiss(); dialog = null }
    fun close() { cancel(); worker.shutdownNow() }
    private fun toast(resource: Int) = Toast.makeText(activity, resource, Toast.LENGTH_LONG).show()
    companion object {
        @Volatile internal var sourceOverride: UpdateSource? = null
        fun openPage(context: Context, url: String) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                .onFailure { Toast.makeText(context, R.string.settings_open_failed, Toast.LENGTH_LONG).show() }
        }
    }
}

/** Preserve the previous single ignored tag while allowing independent versions to be managed. */
internal class AppUpdateSettings(context: Context) {
    private val preferences = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    var automatic: Boolean
        get() = preferences.getBoolean("automatic", false)
        set(value) { preferences.edit().putBoolean("automatic", value).apply() }
    val ignored: Set<String> get() = (preferences.getStringSet("ignoredTags", emptySet()).orEmpty() +
        listOfNotNull(preferences.getString("ignored", null))).filter { AppVersionPolicy.parse(it) != null }.toSet()
    fun ignore(tag: String) { preferences.edit().putStringSet("ignoredTags", ignored + tag).putString("ignored", tag).apply() }
    fun unignore(tags: Collection<String>) {
        val next = ignored.filterNot { saved -> tags.any { AppVersionPolicy.isIgnored(saved, it) } }.toSet()
        preferences.edit().putStringSet("ignoredTags", next).putString("ignored", preferences.getString("ignored", null)?.takeIf { it in next }).apply()
    }
}
