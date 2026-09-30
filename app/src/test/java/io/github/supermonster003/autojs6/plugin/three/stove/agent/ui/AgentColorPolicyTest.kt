package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.AgentColorPolicy as P
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.AgentPalette
import org.junit.Assert.*
import org.junit.Test

class AgentColorPolicyTest {
    private val light = intArrayOf(0xFFF6F7F9.toInt(), -1, 0xFFEDEFF3.toInt(), 0xFFD3D8E0.toInt(), 0xFFE5E8ED.toInt(),
        0xFF191C22.toInt(), 0xFF5C6472.toInt(), 0xFFB3261E.toInt(), 0xFFFCEEEE.toInt(), 0xFF1B7A3A.toInt(), 0xFFE7F4EB.toInt(),
        0xFF8A5300.toInt(), 0xFFFFF3DE.toInt())
    private val dark = intArrayOf(0xFF111318.toInt(), 0xFF1A1D23.toInt(), 0xFF252932.toInt(), 0xFF3B414D.toInt(), 0xFF2A2F38.toInt(),
        0xFFECEEF3.toInt(), 0xFFA6AEBC.toInt(), 0xFFF2B8B5.toInt(), 0xFF3A2020.toInt(), 0xFF83D99F.toInt(), 0xFF1C2E23.toInt(),
        0xFFF1C274.toInt(), 0xFF342A1A.toInt())

    @Test fun contrastMatchesWcagReferencePoints() {
        assertEquals(21.0, P.contrastRatio(0xFF000000.toInt(), -1), 0.01)
        assertEquals(1.0, P.contrastRatio(0xFF777777.toInt(), 0xFF777777.toInt()), 0.001)
        assertEquals(0xFF000000.toInt(), P.onFilledColor(0xFFFFDEAD.toInt()))
        assertEquals(-1, P.onFilledColor(0xFF4F46E5.toInt()))
    }

    @Test fun readableAccentKeepsReadableColorsAndFixesPaleOnes() {
        assertEquals(0xFF4F46E5.toInt(), P.readableAccent(0xFF4F46E5.toInt(), -1))
        for (seed in listOf(0xFFFFDEAD.toInt(), 0xFFFFFF00.toInt(), 0xFF101010.toInt(), 0xFF007C8A.toInt())) {
            for (background in listOf(-1, 0xFF111318.toInt(), 0xFF808080.toInt())) {
                assertTrue(P.contrastRatio(P.readableAccent(seed, background), background) >= 4.5)
            }
        }
    }

    @Test fun hsvRoundTripAndDynamicPrimaryAddsOnlyMinimalChroma() {
        for (color in listOf(0xFF4F46E5.toInt(), 0xFFFFDEAD.toInt(), 0xFF2E7D32.toInt(), 0xFF808080.toInt(), -1, 0xFF000000.toInt())) {
            assertEquals(color, P.fromHsv(P.toHsv(color)))
        }
        val pale = P.dynamicPrimary(0xFFF5EEE6.toInt())
        assertEquals(0.28, P.toHsv(pale)[1], 0.01)
        assertEquals(P.toHsv(0xFFF5EEE6.toInt())[2], P.toHsv(pale)[2], 0.01)
        // Already chromatic or neutral seeds keep their color.
        assertEquals(0xFFFFDEAD.toInt(), P.dynamicPrimary(0xFFFFDEAD.toInt()))
        assertEquals(0xFF808080.toInt(), P.dynamicPrimary(0xFF808080.toInt()))
    }

    @Test fun harmonizedSurfacesKeepTextContrast() {
        val surface = P.harmonizeSurface(-1, 0xFF00FF00.toInt(), 0xFF191C22.toInt(), 0.9)
        assertTrue(P.contrastRatio(surface, 0xFF191C22.toInt()) >= 4.5)
        assertEquals(0x1C, P.withAlpha(0xFF123456.toInt(), 0x1C) ushr 24)
    }

    @Test fun curatedPaletteKeepsNeutralsAndCustomColorsStayReadable() {
        for ((neutrals, isDark) in listOf(light to false, dark to true)) {
            val curated = AgentPalette.build(neutrals, AppearancePreferences.DEFAULT_COLOR, isDark)
            assertEquals(neutrals[0], curated.background); assertEquals(neutrals[1], curated.surface)
            assertEquals(neutrals[2], curated.surfaceVariant)
            // Tonal buttons and chips put accent text on the accent tone over a card.
            fun tonal(palette: AgentPalette) = P.blend(palette.surface, palette.accent, AgentPalette.TONE_ALPHA / 255.0)
            assertTrue(P.contrastRatio(curated.accent, tonal(curated)) >= 4.5)
            for (seed in listOf(0xFFFFDEAD.toInt(), 0xFFE91E63.toInt(), 0xFF00BCD4.toInt(), 0xFF334455.toInt(), 0xFFEEDDCC.toInt())) {
                val palette = AgentPalette.build(neutrals, seed, isDark)
                assertEquals(neutrals[0], palette.background)
                assertEquals(neutrals[1], palette.surface)
                assertEquals(neutrals[2], palette.surfaceVariant)
                assertTrue(P.contrastRatio(palette.accent, palette.background) >= 4.5)
                assertTrue(P.contrastRatio(palette.accent, palette.surface) >= 4.5)
                assertTrue(P.contrastRatio(palette.accent, tonal(palette)) >= 4.5)
                assertTrue(P.contrastRatio(palette.onPrimary, palette.primary) >= 4.5)
                assertTrue(P.contrastRatio(palette.text, palette.surface) >= 4.5)
            }
        }
    }
}
