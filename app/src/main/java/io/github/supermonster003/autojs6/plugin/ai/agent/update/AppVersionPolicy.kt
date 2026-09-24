package io.github.supermonster003.autojs6.plugin.ai.agent.update

/** Semantic versions without integer overflow; metadata never changes ordering. */
internal object AppVersionPolicy {
    private val pattern = Regex("[vV]?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:\\.(0|[1-9][0-9]*))?(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?")
    internal data class Version(val parts: List<String>, val pre: List<String>) : Comparable<Version> {
        override fun compareTo(other: Version): Int {
            parts.zip(other.parts).forEach { (a, b) -> numeric(a, b).takeIf { it != 0 }?.let { return it } }
            if (pre.isEmpty() || other.pre.isEmpty()) return pre.isEmpty().compareTo(other.pre.isEmpty())
            pre.zip(other.pre).forEach { (a, b) ->
                val an = a.all(Char::isDigit); val bn = b.all(Char::isDigit)
                val order = if (an && bn) numeric(a, b) else if (an != bn) if (an) -1 else 1 else a.compareTo(b)
                if (order != 0) return order
            }
            return pre.size.compareTo(other.pre.size)
        }
    }
    private fun numeric(a: String, b: String) = a.length.compareTo(b.length).takeIf { it != 0 } ?: a.compareTo(b)
    fun parse(text: String?): Version? {
        if (text == null || text.length > 128) return null
        val match = pattern.matchEntire(text.trim()) ?: return null
        val pre = match.groupValues[4].takeIf { it.isNotEmpty() }?.split('.').orEmpty()
        if (pre.any { it.length > 1 && it.all(Char::isDigit) && it.startsWith('0') }) return null
        return Version(listOf(match.groupValues[1], match.groupValues[2], match.groupValues[3].ifEmpty { "0" }), pre)
    }
    fun isNewer(remote: String?, installed: String?): Boolean = parse(remote)?.let { a -> parse(installed)?.let { a > it } } == true
    fun isIgnored(remote: String?, ignored: String?): Boolean = parse(remote)?.let { a -> parse(ignored)?.let { a.compareTo(it) == 0 } } == true
}
