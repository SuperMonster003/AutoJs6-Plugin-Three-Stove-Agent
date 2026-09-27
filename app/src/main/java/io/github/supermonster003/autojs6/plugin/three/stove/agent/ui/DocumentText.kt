package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.AgentPalette

/**
 * Minimal offline Markdown for bundled changelogs and notices: headings, date lines, bullets with
 * category badges and inline code. No links, images or HTML are interpreted.
 */
internal object DocumentText {
    fun render(text: String, palette: AgentPalette): CharSequence = SpannableStringBuilder().apply {
        var previousBlank = true
        text.lineSequence().forEach { raw ->
            val line = raw.trimEnd()
            if (line.matches(Regex("[ *-]{3,}"))) return@forEach
            if (line.isBlank()) { if (!previousBlank) append('\n'); previousBlank = true; return@forEach }
            previousBlank = false
            val level = line.takeWhile { it == '#' }.length
            val start = length
            when {
                level in 1..5 -> {
                    append(line.drop(level).trimStart()).append('\n')
                    setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(RelativeSizeSpan(if (level == 1) 1.3f else 1.12f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(palette.text), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                level == 6 -> {
                    append(line.drop(level).trimStart()).append('\n')
                    setSpan(ForegroundColorSpan(palette.muted), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(RelativeSizeSpan(0.9f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.trimStart().startsWith("* ") || line.trimStart().startsWith("- ") -> {
                    inline(line.trimStart().drop(2), palette)
                    append('\n')
                    setSpan(BulletSpan(24, palette.muted), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                else -> { inline(line, palette); append('\n') }
            }
        }
    }

    /** `code` segments become medium-weight accent labels (category tags such as `Feature`). */
    private fun SpannableStringBuilder.inline(value: String, palette: AgentPalette) {
        val parts = value.split('`')
        parts.forEachIndexed { index, part ->
            val start = length
            append(part)
            if (index % 2 == 1 && part.isNotEmpty()) {
                setSpan(ForegroundColorSpan(palette.accent), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(TypefaceSpan("sans-serif-medium"), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
