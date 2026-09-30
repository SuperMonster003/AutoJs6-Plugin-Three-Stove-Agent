package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit

import android.content.Context
import android.content.res.Configuration
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.HostAppearance
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.ThemeAccentRoles
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Pure color math (WCAG 2 contrast), adapted from the 3-Stone AI palette policy. No Android types. */
internal object AgentColorPolicy {
    const val MINIMUM_TEXT_CONTRAST = 4.5
    private const val MINIMUM_DYNAMIC_SATURATION = 0.28
    private const val OPAQUE_BLACK = -0x1000000
    private const val OPAQUE_WHITE = -0x1

    fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val value = (color shr shift and 0xFF) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun contrastRatio(first: Int, second: Int): Double {
        val lighter = max(luminance(first), luminance(second))
        val darker = min(luminance(first), luminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** The higher-contrast opaque foreground for a filled control or badge. */
    fun onFilledColor(background: Int): Int =
        if (contrastRatio(OPAQUE_BLACK, background) >= contrastRatio(OPAQUE_WHITE, background)) OPAQUE_BLACK else OPAQUE_WHITE

    /** Keeps the hue and moves toward black or white only as far as needed for 4.5:1 text. */
    fun readableAccent(color: Int, background: Int): Int {
        val opaque = color or OPAQUE_BLACK
        if (contrastRatio(opaque, background) >= MINIMUM_TEXT_CONTRAST) return opaque
        fun toward(target: Int): Int? {
            if (contrastRatio(target, background) < MINIMUM_TEXT_CONTRAST) return null
            var low = 0.0
            var high = 1.0
            repeat(18) {
                val middle = (low + high) / 2.0
                if (contrastRatio(blend(opaque, target, middle), background) >= MINIMUM_TEXT_CONTRAST) high = middle else low = middle
            }
            return blend(opaque, target, high)
        }
        val source = luminance(opaque)
        return listOfNotNull(toward(OPAQUE_BLACK), toward(OPAQUE_WHITE)).minByOrNull { abs(luminance(it) - source) }
            ?: onFilledColor(background)
    }

    /** High-emphasis fill from an arbitrary seed: pale seeds gain a little chroma but keep their brightness. */
    fun dynamicPrimary(color: Int): Int {
        val hsv = toHsv(color or OPAQUE_BLACK)
        if (hsv[1] >= 0.06 && hsv[1] < MINIMUM_DYNAMIC_SATURATION) hsv[1] = MINIMUM_DYNAMIC_SATURATION
        return fromHsv(hsv)
    }

    /** Tints a neutral surface toward the accent while retaining text contrast. */
    fun harmonizeSurface(reference: Int, accent: Int, foreground: Int, ratio: Double): Int {
        val requested = ratio.coerceIn(0.0, 1.0)
        val candidate = blend(reference, accent, requested)
        if (contrastRatio(candidate, foreground) >= MINIMUM_TEXT_CONTRAST) return candidate
        var low = 0.0
        var high = requested
        repeat(18) {
            val middle = (low + high) / 2.0
            if (contrastRatio(blend(reference, accent, middle), foreground) >= MINIMUM_TEXT_CONTRAST) low = middle else high = middle
        }
        return blend(reference, accent, low)
    }

    fun withAlpha(color: Int, alpha: Int): Int = color and 0xFFFFFF or (alpha.coerceIn(0, 255) shl 24)

    /** Opaque linear blend; [ratio] 0 keeps [first], 1 yields [second]. */
    fun blend(first: Int, second: Int, ratio: Double): Int {
        fun channel(shift: Int): Int {
            val start = first shr shift and 0xFF
            val end = second shr shift and 0xFF
            return (start + (end - start) * ratio).toInt().coerceIn(0, 255)
        }
        return OPAQUE_BLACK or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    internal fun toHsv(color: Int): DoubleArray {
        val r = (color shr 16 and 0xFF) / 255.0
        val g = (color shr 8 and 0xFF) / 255.0
        val b = (color and 0xFF) / 255.0
        val maximum = max(r, max(g, b))
        val delta = maximum - min(r, min(g, b))
        val hue = when {
            delta == 0.0 -> 0.0
            maximum == r -> 60.0 * (((g - b) / delta).mod(6.0))
            maximum == g -> 60.0 * ((b - r) / delta + 2)
            else -> 60.0 * ((r - g) / delta + 4)
        }
        return doubleArrayOf(hue, if (maximum == 0.0) 0.0 else delta / maximum, maximum)
    }

    internal fun fromHsv(hsv: DoubleArray): Int {
        val (hue, saturation, value) = Triple(hsv[0], hsv[1], hsv[2])
        val chroma = value * saturation
        val x = chroma * (1 - abs((hue / 60.0).mod(2.0) - 1))
        val m = value - chroma
        val (r, g, b) = when ((hue / 60.0).toInt().coerceIn(0, 5)) {
            0 -> Triple(chroma, x, 0.0)
            1 -> Triple(x, chroma, 0.0)
            2 -> Triple(0.0, chroma, x)
            3 -> Triple(0.0, x, chroma)
            4 -> Triple(x, 0.0, chroma)
            else -> Triple(chroma, 0.0, x)
        }
        fun channel(value: Double) = ((value + m) * 255.0 + 0.5).toInt().coerceIn(0, 255)
        return OPAQUE_BLACK or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }
}

/** Runtime colors for every standalone surface, derived from the chosen or host theme color. */
internal data class AgentPalette(
    val primary: Int,
    val onPrimary: Int,
    val accent: Int,
    val onAccent: Int,
    val background: Int,
    val surface: Int,
    val surfaceVariant: Int,
    val outline: Int,
    val divider: Int,
    val text: Int,
    val muted: Int,
    val danger: Int,
    val dangerSurface: Int,
    val success: Int,
    val successSurface: Int,
    val warning: Int,
    val warningSurface: Int,
    val isDark: Boolean,
) {
    /** Translucent accent fills for tonal controls and ripples. */
    val accentTone: Int get() = AgentColorPolicy.withAlpha(accent, TONE_ALPHA)
    val accentRipple: Int get() = AgentColorPolicy.withAlpha(accent, 0x2E)

    internal class Neutrals(val values: IntArray)

    companion object {
        /** Alpha of [accentTone]; accent text must stay readable on this fill. */
        const val TONE_ALPHA = 0x1C
        private val neutrals = ConcurrentHashMap<Boolean, Neutrals>()
        private data class Key(val seed: Int, val accentSeed: Int, val dark: Boolean)
        private val cache = ConcurrentHashMap<Key, AgentPalette>()

        fun isDark(context: Context, appearance: HostAppearance?): Boolean = appearance?.dark
            ?: (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

        fun resolve(context: Context, appearance: HostAppearance?): AgentPalette {
            val dark = isDark(context, appearance)
            val seed = (appearance?.primary ?: AppearancePreferences.DEFAULT_COLOR) or -0x1000000
            val accentSeed = (appearance?.accent ?: seed) or -0x1000000
            val key = Key(seed, accentSeed, dark)
            return cache.getOrPut(key) { build(neutralColors(context, dark).values, seed, dark, accentSeed) }
        }

        /** Static neutrals come from colors.xml in the requested night mode, cached per mode. */
        private fun neutralColors(context: Context, dark: Boolean): Neutrals = neutrals.getOrPut(dark) {
            val current = context.resources.configuration
            val matches = (current.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) == dark
            val resources = if (matches) context else context.createConfigurationContext(Configuration(current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            })
            Neutrals(intArrayOf(R.color.window_background, R.color.surface, R.color.surface_variant, R.color.outline, R.color.divider,
                R.color.text_color_primary, R.color.text_color_secondary, R.color.status_danger, R.color.status_danger_surface,
                R.color.status_success, R.color.status_success_surface, R.color.status_warning, R.color.status_warning_surface)
                .map(resources::getColor).toIntArray())
        }

        internal fun build(n: IntArray, seed: Int, dark: Boolean, accentSeed: Int = seed): AgentPalette {
            val roles = ThemeAccentRoles.fromSeed(seed, dark)
            val accentRoles = ThemeAccentRoles.fromSeed(accentSeed, dark)
            val primary = roles.primary
            val background = n[0]
            val surface = n[1]
            var accent = AgentColorPolicy.readableAccent(accentRoles.primary, background)
            repeat(8) {
                for (reference in listOf(background, surface, n[2],
                    AgentColorPolicy.blend(background, accent, TONE_ALPHA / 255.0),
                    AgentColorPolicy.blend(surface, accent, TONE_ALPHA / 255.0))) {
                    accent = AgentColorPolicy.readableAccent(accent, reference)
                }
            }
            return AgentPalette(
                primary = primary,
                onPrimary = roles.onPrimary,
                accent = accent,
                onAccent = accentRoles.onPrimary,
                background = background,
                surface = surface,
                surfaceVariant = n[2],
                outline = n[3],
                divider = n[4],
                text = n[5],
                muted = n[6],
                danger = n[7], dangerSurface = n[8],
                success = n[9], successSurface = n[10],
                warning = n[11], warningSurface = n[12],
                isDark = dark,
            )
        }
    }
}
