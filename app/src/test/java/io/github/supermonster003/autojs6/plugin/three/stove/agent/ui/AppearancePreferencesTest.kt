package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import org.junit.Assert.*
import org.junit.Test

class AppearancePreferencesTest {
    private val host = HostAppearance("ar", true, 0xff445566.toInt(), 0xff778899.toInt())
    @Test fun defaultsFollowHostAndRecoverToSystemWhenItIsMissing() {
        assertEquals(host, AppearancePreferences().resolve(host, "en", false))
        val fallback = AppearancePreferences().resolve(null, "ja", false)
        assertEquals("ja", fallback.language); assertFalse(fallback.dark)
        assertEquals(AppearancePreferences.DEFAULT_COLOR, fallback.primary)
    }
    @Test fun independentOverridesDoNotLeakBackIntoHostFollowingFields() {
        val mixed = AppearancePreferences(language = "en", darkMode = "system").resolve(host, "fr", false)
        assertEquals("en", mixed.language); assertFalse(mixed.dark); assertEquals(host.primary, mixed.primary)
        val custom = AppearancePreferences(language = "system", darkMode = "dark", color = 0x112233).resolve(host, "ko", false)
        assertEquals("ko", custom.language); assertTrue(custom.dark)
        assertEquals(0xff112233.toInt(), custom.primary); assertEquals(custom.primary, custom.accent)
        assertEquals("ar", host.language)
    }
    @Test fun customColorValidationIsStrictAndAlwaysOpaque() {
        assertEquals(0xff12abcd.toInt(), AppearancePreferences.parseColor(" #12AbCd "))
        assertEquals("#12ABCD", AppearancePreferences.colorHex(0xff12abcd.toInt()))
        for (bad in listOf("#fff", "#80123456", "12345g", "", "-12345", "#1234567")) assertNull(AppearancePreferences.parseColor(bad))
    }
}
