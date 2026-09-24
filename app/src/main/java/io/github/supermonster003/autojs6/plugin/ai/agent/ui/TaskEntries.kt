package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import java.security.MessageDigest

internal object TaskEntries {
    const val NEW_TASK = "io.github.supermonster003.autojs6.plugin.ai.agent.NEW_TASK"
    const val PRESET_TASK = "io.github.supermonster003.autojs6.plugin.ai.agent.PRESET_TASK"
    fun read(intent: Intent): TaskEntry? = runCatching {
        when (intent.action) {
            NEW_TASK -> TaskEntry("")
            PRESET_TASK -> TaskEntry(intent.getStringExtra("rerunGoal").orEmpty(), requireNotNull(intent.getStringExtra("rerunPreset")))
            Intent.ACTION_SEND -> {
                require(intent.type == "text/plain")
                val text = requireNotNull(intent.getCharSequenceExtra(Intent.EXTRA_TEXT))
                require(text.length <= 4096 && text.isNotBlank())
                TaskEntry(text.toString()) // Strip spans and never forward arbitrary extras or ClipData.
            }
            else -> if (intent.hasExtra("rerunGoal") || intent.hasExtra("rerunPreset"))
                TaskEntry(intent.getStringExtra("rerunGoal").orEmpty(), intent.getStringExtra("rerunPreset")) else null
        }
    }.getOrNull()
    fun intent(context: Context, entry: TaskEntry) = Intent(context, LauncherActivity::class.java)
        .setAction(if (entry.preset == null) Intent.ACTION_VIEW else PRESET_TASK)
        .putExtra("rerunGoal", entry.goal).apply { entry.preset?.let { putExtra("rerunPreset", it) } }
    fun canPin(context: Context) = Build.VERSION.SDK_INT >= 26 && context.getSystemService(ShortcutManager::class.java)?.isRequestPinShortcutSupported == true
    private fun id(entry: TaskEntry) = "preset-" + MessageDigest.getInstance("SHA-256")
        .digest((requireNotNull(entry.preset) + "\u0000" + entry.goal).toByteArray()).joinToString("") { "%02x".format(it) }
    fun opened(context: Context, entry: TaskEntry) {
        if (Build.VERSION.SDK_INT >= 25 && entry.preset != null) runCatching { context.getSystemService(ShortcutManager::class.java).reportShortcutUsed(id(entry)) }
    }
    fun pin(context: Context, entry: TaskEntry): Boolean {
        if (Build.VERSION.SDK_INT < 26 || !canPin(context)) return false
        val name = requireNotNull(entry.preset)
        val shortcut = ShortcutInfo.Builder(context, id(entry)).setShortLabel(name)
            .setLongLabel(context.getString(R.string.shortcut_preset, name)).setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent(context, entry)).build()
        val manager = context.getSystemService(ShortcutManager::class.java)
        // Also refresh the launcher long-press list. Respect the device's per-activity quota.
        val slots = (manager.maxShortcutCountPerActivity - manager.manifestShortcuts.size).coerceAtLeast(0)
        if (slots > 0) manager.dynamicShortcuts = (listOf(shortcut) + manager.dynamicShortcuts.filter { it.id != shortcut.id }).take(slots)
        return manager.requestPinShortcut(shortcut, null) // The launcher still requires user confirmation.
    }
}

/** The only additional public entry. A share always opens a reviewable draft, never a task. */
class ShareTargetActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entry = if (intent.action == Intent.ACTION_SEND) TaskEntries.read(intent) else null
        if (entry == null) Toast.makeText(this, R.string.entry_invalid, Toast.LENGTH_LONG).show()
        else startActivity(TaskEntries.intent(this, entry))
        finish()
    }
}
