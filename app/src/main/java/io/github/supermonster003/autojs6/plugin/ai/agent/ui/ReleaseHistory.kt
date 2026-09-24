package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import java.io.IOException
import java.util.Locale

internal object ReleaseHistory {
    fun candidates(locale: Locale): List<String> {
        val code = when {
            locale.language == "zh" && locale.country == "HK" -> "zh-Hant-HK"
            locale.language == "zh" && (locale.country == "TW" || locale.script == "Hant") -> "zh-Hant-TW"
            locale.language == "zh" -> "zh-Hans"
            locale.language in setOf("en", "ar", "es", "fr", "ja", "ko", "ru") -> locale.language
            else -> "en"
        }
        return listOf("doc/CHANGELOG-$code.md", "doc/CHANGELOG-en.md").distinct()
    }
    fun load(locale: Locale, read: (String) -> String): String? {
        for (path in candidates(locale)) try { read(path).takeIf { it.isNotBlank() }?.let { return it } } catch (_: IOException) { }
        return null
    }
}
