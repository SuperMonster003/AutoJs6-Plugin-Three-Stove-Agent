package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.StepJournal

/** Selects whole compact-tree rows before byte truncation; quoted node text is never rewritten. */
object ObservationCompactor {
    private val node = Regex("^\\s*#n[1-9][0-9]*\\s")
    private val quoted = Regex("\"(?:[^\"\\\\]|\\\\.)*\"")
    private val bounds = Regex("\\s+(?:bounds=)?\\[-?\\d+,-?\\d+](?:\\[-?\\d+,-?\\d+])?|\\s+c=\\(-?\\d+,-?\\d+\\)")
    private val actionable = Regex("(?:^|[ ,])(?:clickable|long_clickable|editable|scrollable)(?:$|[ ,])")
    fun compact(value: JsonElement, maxBytes: Int, local: Boolean): JsonElement {
        require(maxBytes >= 128)
        val copy = value.deepCopy()
        fun visit(element: JsonElement) {
            when {
                element.isJsonObject -> element.asJsonObject.entrySet().toList().forEach { (key, child) ->
                    if (key == "text" && child.isJsonPrimitive && child.asJsonPrimitive.isString && isTree(child.asString)) {
                        val packed = tree(child.asString, maxBytes, local)
                        if (packed != child.asString) { element.asJsonObject.addProperty(key, packed); element.asJsonObject.addProperty("truncated", true) }
                    } else visit(child)
                }
                element.isJsonArray -> element.asJsonArray.forEach(::visit)
            }
        }
        visit(copy)
        if (StepJournal.bytes(copy) <= maxBytes) return copy
        val treeParent = when {
            copy.isJsonObject && copy.asJsonObject.string("text")?.let(::isTree) == true -> copy.asJsonObject
            copy.isJsonObject && copy.asJsonObject["result"]?.isJsonObject == true -> copy.asJsonObject.getAsJsonObject("result")
            else -> null
        }
        if (treeParent?.string("text")?.let(::isTree) == true) {
            // Account for envelope and JSON string escaping by trying progressively smaller whole-row selections.
            var allowance = maxBytes / 2
            while (allowance >= 64) {
                treeParent.addProperty("text", tree(treeParent.string("text")!!, allowance, local))
                treeParent.addProperty("truncated", true)
                if (StepJournal.bytes(copy) <= maxBytes) return copy
                allowance /= 2
            }
        }
        val text = if (copy.isJsonPrimitive && copy.asJsonPrimitive.isString) copy.asString else copy.toString()
        val result = jsonObject("truncated" to true.json(), "text" to "".json())
        var low = 0; var high = maxBytes
        while (low < high) {
            val middle = (low + high + 1) / 2
            result.addProperty("text", AgentJson.truncate(text, middle))
            if (StepJournal.bytes(result) <= maxBytes) low = middle else high = middle - 1
        }
        result.addProperty("text", AgentJson.truncate(text, low))
        return result
    }
    private fun isTree(text: String) = text.lineSequence().firstOrNull()?.startsWith("window:") == true && text.lineSequence().any { node.containsMatchIn(it) }
    private fun tree(text: String, maxBytes: Int, local: Boolean): String {
        data class Row(val index: Int, val text: String, val priority: Int)
        fun outsideQuotes(line: String, transform: (String) -> String): String {
            var position = 0
            return buildString {
                quoted.findAll(line).forEach { match -> append(transform(line.substring(position, match.range.first))); append(match.value); position = match.range.last + 1 }
                append(transform(line.substring(position)))
            }
        }
        val lines = text.lines()
        val header = if (local) outsideQuotes(lines.first()) { bounds.replace(it, "") } else lines.first()
        val rows = lines.drop(1).mapIndexedNotNull { index, line ->
            if (!node.containsMatchIn(line)) return@mapIndexedNotNull null
            val metadata = quoted.replace(line, "\"\"")
            val important = actionable.containsMatchIn(metadata)
            val hasText = quoted.findAll(line).any { it.value.length > 2 }
            if (local && !important && !hasText) null else Row(index,
                if (local) outsideQuotes(line) { bounds.replace(it, "") }.trimStart() else line,
                if (important) 2 else if (hasText) 1 else 0)
        }
        val selected = mutableListOf<Row>()
        var bytes = header.toByteArray(Charsets.UTF_8).size + 40
        for (row in rows.sortedWith(compareByDescending<Row> { it.priority }.thenBy { it.index })) {
            val size = row.text.toByteArray(Charsets.UTF_8).size + 1
            if (selected.size < (if (local) 70 else 400) && bytes + size <= maxBytes) { selected += row; bytes += size }
        }
        val changed = local || selected.size != lines.drop(1).count { node.containsMatchIn(it) }
        if (!changed && text.toByteArray(Charsets.UTF_8).size <= maxBytes) return text
        return (listOf(header) + selected.sortedBy { it.index }.map { it.text } + "[context truncated]").joinToString("\n")
    }
}
