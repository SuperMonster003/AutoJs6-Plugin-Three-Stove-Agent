package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Context
import android.content.res.Configuration
import android.util.AtomicFile
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.io.File
import java.util.Locale

/** App preferences never write to the host. Missing host settings fall back to Android. */
internal data class AppearancePreferences(val language: String = "host", val darkMode: String = "host", val color: Int? = null) {
    fun resolve(host: HostAppearance?, systemLanguage: String, systemDark: Boolean): HostAppearance {
        val dark = when (darkMode) { "light" -> false; "dark" -> true; "system" -> systemDark; else -> host?.dark ?: systemDark }
        val language = when (language) { "host" -> host?.language ?: systemLanguage; "system" -> systemLanguage; else -> language }
        val primary = color ?: host?.primary ?: DEFAULT_COLOR
        return HostAppearance(language, dark, primary or -0x1000000, (color ?: host?.accent ?: DEFAULT_COLOR) or -0x1000000)
    }
    companion object {
        const val DEFAULT_COLOR = 0xff4f46e5.toInt()
        /** Offered theme colors (blue, teal, green, purple, amber); they keep the static neutral surfaces. */
        val CURATED_COLORS = listOf(DEFAULT_COLOR, 0xff007c8a.toInt(), 0xff2e7d32.toInt(), 0xff7e57c2.toInt(), 0xffc86b0a.toInt())
        val languages = listOf("host", "system", "zh-Hans", "zh-Hant-HK", "zh-Hant-TW", "en", "fr", "es", "ja", "ko", "ru", "ar")
        val modes = listOf("host", "system", "light", "dark")
        fun parseColor(value: String): Int? = value.trim().removePrefix("#").takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }
            ?.toLong(16)?.toInt()?.or(-0x1000000)
        fun colorHex(value: Int) = String.format(Locale.ROOT, "#%06X", value and 0xffffff)
        fun read(context: Context): AppearancePreferences {
            return runCatching {
                val value = locked(context) { file(context).openRead().use {
                    require(it.channel.size() <= 4096)
                    AgentJson.objectOf(it.readBytes().toString(Charsets.UTF_8), 4096)
                } }
                AppearancePreferences(value.string("language").takeIf { it in languages } ?: "host",
                    value.string("darkMode").takeIf { it in modes } ?: "host", value.number("color")?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt())
            }.getOrDefault(AppearancePreferences())
        }
        fun resolve(context: Context, host: HostAppearance? = HostAppearance.cached): HostAppearance {
            val system = context.resources.configuration
            return read(context).resolve(host, system.locales[0].toLanguageTag(),
                system.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        }
        private fun file(context: Context) = AtomicFile(File(context.filesDir, "app-appearance.json"))
        private fun <T> locked(context: Context, action: () -> T): T = synchronized(AppearancePreferences::class.java) {
            // AtomicFile on older Android versions can restore a backup from openRead().
            // Serialize both processes so a floating-window read cannot undo an in-flight save.
            java.io.FileOutputStream(File(context.filesDir, "app-appearance.lock"), true).channel.use { channel ->
                channel.lock().use { action() }
            }
        }
    }
    fun save(context: Context) = locked(context) {
        // Both the UI and :agent floating window read this small atomic snapshot; no process-local preference cache.
        val value = jsonObject("language" to language.json(), "darkMode" to darkMode.json()).apply { color?.let { addProperty("color", it) } }
        val file = file(context)
        val stream = file.startWrite()
        try { stream.write(value.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (failure: Exception) { file.failWrite(stream); throw failure }
    }
}
