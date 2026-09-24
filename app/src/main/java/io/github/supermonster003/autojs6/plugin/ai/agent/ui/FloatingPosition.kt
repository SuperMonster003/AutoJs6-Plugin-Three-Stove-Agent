package io.github.supermonster003.autojs6.plugin.ai.agent.ui

/** Fractions survive rotation and display/font scaling. All inputs are usable window coordinates. */
internal object FloatingPosition {
    fun coordinate(fraction: Float, start: Int, end: Int, size: Int): Int {
        val safe = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0.5f
        return start + ((end - start - size).coerceAtLeast(0) * safe).toInt()
    }
    fun fraction(coordinate: Int, start: Int, end: Int, size: Int): Float =
        if (end - start <= size) 0f else ((coordinate - start).toFloat() / (end - start - size)).coerceIn(0f, 1f)
}
