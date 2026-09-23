package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import com.google.gson.JsonElement
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.security.MessageDigest

/** Values parsed from the host's compact format. These are display identities, not live nodes. */
object CompactNodeText {
    data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        init { require(left <= right && top <= bottom) }
        fun permits(other: Bounds): Boolean {
            val dx = if (left == right) 48L else minOf(48L, (right.toLong() - left) / 2)
            val dy = if (top == bottom) 48L else minOf(48L, (bottom.toLong() - top) / 2)
            return kotlin.math.abs(other.left.toLong() - left) <= dx && kotlin.math.abs(other.right.toLong() - right) <= dx &&
                kotlin.math.abs(other.top.toLong() - top) <= dy && kotlin.math.abs(other.bottom.toLong() - bottom) <= dy
        }
    }
    data class Node(val ref: String, val depth: Int, val className: String, val id: String, val text: String,
                    val description: String, val flags: Set<String>, val bounds: Bounds) {
        val relocatable get() = text != "[password]" && !text.endsWith("...") && !description.endsWith("...")
        fun fingerprint(window: String): String = MessageDigest.getInstance("SHA-256")
            .digest(jsonArray(window.json(), className.json(), id.json(), text.json(), description.json(),
                flags.intersect(IDENTITY_FLAGS).sorted().joinToString(",").json()).toString().toByteArray(Charsets.UTF_8))
            .let { bytes -> buildString(64) { bytes.forEach { byte -> val n = byte.toInt() and 255; append("0123456789abcdef"[n ushr 4]); append("0123456789abcdef"[n and 15]) } } }
    }
    data class Snapshot(val id: String, val window: String, val nodes: List<Node>, val truncated: Boolean)
    private val row = Regex("^#n([1-9][0-9]{0,3}) ( *)([^ ]+)(.*)$")
    private val location = Regex(" (?:\\[(-?\\d+),(-?\\d+)]\\[(-?\\d+),(-?\\d+)]|c=\\((-?\\d+),(-?\\d+)\\))$")
    private val flags = setOf("clickable", "long_clickable", "checkable", "checked", "scrollable", "editable", "focused", "selected", "!enabled", "hidden")
    // Keep action capabilities in the identity, but not transient checked/focused/selected state.
    private val IDENTITY_FLAGS = setOf("clickable", "long_clickable", "checkable", "scrollable", "editable")

    fun parse(value: JsonElement): Snapshot {
        val data = AgentJson.parse(value.toString(), 300 * 1024).asJsonObject
        require(data.string("format") == "compact")
        val id = requireNotNull(data.string("snapshotId")).also { require(it.isNotBlank() && it.length <= 128) }
        val text = requireNotNull(data.string("text")).also { require(it.toByteArray(Charsets.UTF_8).size <= 256 * 1024) }
        val lines = text.lines()
        require(lines.first().startsWith("window: ") && lines.size <= 402)
        val nodes = mutableListOf<Node>()
        var footer = false
        for (line in lines.drop(1)) {
            if (line.startsWith('(') && line.endsWith(')') && !footer) { footer = true; continue }
            require(!footer)
            val match = requireNotNull(row.matchEntire(line))
            val number = match.groupValues[1].toInt()
            require(number == nodes.size + 1 && number <= 400)
            val depth = match.groupValues[2].length.also { require(it <= 32) }
            val kind = match.groupValues[3].also { require(it.length <= 256) }
            val rest = match.groupValues[4]
            val position = requireNotNull(location.find(rest))
            val coordinates = position.groupValues
            val bounds = if (coordinates[1].isNotEmpty()) Bounds(coordinates[1].toInt(), coordinates[2].toInt(), coordinates[3].toInt(), coordinates[4].toInt())
                else Bounds(coordinates[5].toInt(), coordinates[6].toInt(), coordinates[5].toInt(), coordinates[6].toInt())
            var label = ""; var desc = ""; var resource = ""
            var hasLabel = false; var hasDesc = false; var hasId = false
            val markers = linkedSetOf<String>()
            for (token in tokens(rest.substring(0, position.range.first))) {
                when {
                    token.startsWith('"') -> { require(!hasLabel); label = unquote(token); hasLabel = true }
                    token.startsWith("desc=\"") -> { require(!hasDesc); desc = unquote(token.removePrefix("desc=")); hasDesc = true }
                    token.startsWith("id=") -> { require(!hasId); resource = token.removePrefix("id="); require(resource.length <= 512); hasId = true }
                    else -> require(token in flags && markers.add(token))
                }
            }
            nodes += Node("#n$number", depth, kind, resource, label, desc, markers.toSet(), bounds)
        }
        require(data.number("nodeCount") == nodes.size.toLong())
        val pkg = data.string("packageName").orEmpty().also { require(it.length <= 256) }
        val activity = data.string("activityName").orEmpty().also { require(it.length <= 256) }
        val identity = data.string("windowIdentity").orEmpty().also { require(it.length <= 128) }
        return Snapshot(id, jsonArray(pkg.json(), activity.json(), identity.json()).toString(), nodes.toList(), requireNotNull(data.flag("truncated")))
    }

    private fun tokens(value: String): List<String> {
        val result = mutableListOf<String>()
        var start = 0; var quoted = false; var escaped = false
        for (i in value.indices) {
            val c = value[i]
            if (escaped) escaped = false
            else if (c == '\\' && quoted) escaped = true
            else if (c == '"') quoted = !quoted
            if (c == ' ' && !quoted) { if (i > start) result += value.substring(start, i); start = i + 1 }
        }
        require(!quoted)
        if (start < value.length) result += value.substring(start)
        return result
    }
    private fun unquote(value: String): String {
        require(value.length >= 2 && value.last() == '"')
        // The host clips escaped display text at 40 characters, possibly inside an escape.
        return buildString {
            var i = 1
            while (i < value.lastIndex) {
                val c = value[i++]
                if (c == '\\' && i < value.lastIndex) {
                    when (val next = value[i++]) { 'n' -> append('\n'); 'r' -> append('\r'); 't' -> append('\t'); '"', '\\' -> append(next); else -> { append('\\'); append(next) } }
                } else append(c)
            }
        }.also { require(it.length <= 128) }
    }
}
