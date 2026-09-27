package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Context
import android.content.res.Configuration
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
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
                val value = requireNotNull(file(context).read())
                AppearancePreferences(value.string("language").takeIf { it in languages } ?: "host",
                    value.string("darkMode").takeIf { it in modes } ?: "host", value.number("color")?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt())
            }.getOrDefault(AppearancePreferences())
        }
        fun resolve(context: Context, host: HostAppearance? = HostAppearance.cached): HostAppearance {
            val system = context.resources.configuration
            return read(context).resolve(host, system.locales[0].toLanguageTag(),
                system.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        }
        // Both the UI and the :agent floating window read this small snapshot; no process-local cache.
        private fun file(context: Context) = LockedJsonFile(context.filesDir, "app-appearance", 4096)
    }
    fun save(context: Context) = file(context).write(
        jsonObject("language" to language.json(), "darkMode" to darkMode.json()).apply { color?.let { addProperty("color", it) } })
}
