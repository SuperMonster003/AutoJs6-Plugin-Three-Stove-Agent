package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.Locale

class ReleaseHistoryTest {
    @Test fun supportedLocalesAndChineseScriptsSelectTheirBundledDocuments() {
        for (tag in listOf("en", "ar", "es", "fr", "ja", "ko", "ru", "zh-Hans", "zh-Hant-HK", "zh-Hant-TW"))
            assertEquals("doc/CHANGELOG-$tag.md", ReleaseHistory.candidates(Locale.forLanguageTag(tag)).first())
        assertEquals("doc/CHANGELOG-zh-Hant-TW.md", ReleaseHistory.candidates(Locale.forLanguageTag("zh-Hant")).first())
        assertEquals(listOf("doc/CHANGELOG-en.md"), ReleaseHistory.candidates(Locale.GERMANY))
    }
    @Test fun missingOrBlankTranslationFallsBackToEnglishAndMissingEnglishReturnsNull() {
        val paths = mutableListOf<String>()
        assertEquals("English fixture", ReleaseHistory.load(Locale.JAPAN) { paths += it; if (it.endsWith("-en.md")) "English fixture" else throw IOException() })
        assertEquals(2, paths.size)
        assertEquals("Fallback", ReleaseHistory.load(Locale.FRANCE) { if (it.endsWith("-en.md")) "Fallback" else " " })
        assertNull(ReleaseHistory.load(Locale.CHINA) { throw IOException() })
    }
}
