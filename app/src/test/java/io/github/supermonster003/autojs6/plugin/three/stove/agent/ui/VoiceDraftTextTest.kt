package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.utf8Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Roadmap D78: voice text is untrusted, cleaned to plain text and cut, never rejected for its length. */
class VoiceDraftTextTest {
    private fun chars(vararg codes: Int) = codes.joinToString("") { String(Character.toChars(it)) }

    @Test
    fun missingOrBlankTextGivesNoDraft() {
        assertNull(VoiceDraftText.clean(null))
        assertNull(VoiceDraftText.clean(""))
        assertNull(VoiceDraftText.clean(" \n\t" + chars(0, 0x2028)))
    }

    @Test
    fun ordinaryDictationIsKept() {
        assertEquals("打开设置, 然后关闭 Wi-Fi", VoiceDraftText.clean("  打开设置, 然后关闭 Wi-Fi  "))
        assertEquals("Hey Stove? Open the calculator!", VoiceDraftText.clean("Hey Stove? Open the calculator!"))
    }

    @Test
    fun controlCharactersBecomeSpacesAndLineBreaksStay() {
        assertEquals("a b c d", VoiceDraftText.clean("a" + chars(0) + "b" + chars(7) + "c\td"))
        assertEquals("one\ntwo\nthree", VoiceDraftText.clean("one\r\ntwo\rthree"))
        assertEquals("x y z", VoiceDraftText.clean("x" + chars(0x2028) + "y" + chars(0x2029) + "z"))
        assertEquals("a b", VoiceDraftText.clean("a" + chars(0x85) + "b"))
    }

    @Test
    fun loneSurrogatesAreReplacedAndPairsKept() {
        val smile = chars(0x1F600)
        assertEquals("x y$smile", VoiceDraftText.clean("x" + 0xD800.toChar() + "y" + smile))
        assertEquals("x y", VoiceDraftText.clean("x" + 0xDC00.toChar() + "y"))
        // The result always builds a draft entry, which rejects unpaired surrogates and NUL.
        TaskEntry(VoiceDraftText.clean("a" + 0xD83D.toChar() + chars(0) + "b")!!)
    }

    @Test
    fun longTextIsCutAtTheDraftLimitOnACodePointBoundary() {
        val han = VoiceDraftText.clean("中".repeat(1400))!!
        assertEquals(1365, han.length)
        assertEquals(4095, han.utf8Size())
        TaskEntry(han)
        val emoji = VoiceDraftText.clean(chars(0x1F600).repeat(1100))!!
        assertEquals(VoiceDraftText.MAX_BYTES, emoji.utf8Size())
        assertEquals(1024, emoji.codePointCount(0, emoji.length))
        TaskEntry(emoji)
        // Spaces that the cut leaves at the end are trimmed as well.
        assertEquals("a".repeat(4094), VoiceDraftText.clean("a".repeat(4094) + "  b"))
    }
}
