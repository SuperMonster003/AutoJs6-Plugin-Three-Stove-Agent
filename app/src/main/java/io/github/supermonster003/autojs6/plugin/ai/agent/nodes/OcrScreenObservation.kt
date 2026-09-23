package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.StepJournal
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptOutputRedactor
import kotlin.math.abs

/** Text-only OCR packing. Coordinates are already in screen space, including host crop offsets. */
object OcrScreenObservation {
    private data class Line(val text: String, val bounds: CompactNodeText.Bounds?, val confidence: Double?, val block: Boolean = false)
    fun normalize(value: JsonElement): JsonObject {
        val source = AgentJson.parse(value.toString(), 64 * 1024).asJsonArray
        require(source.size() <= 400)
        val fragments = source.flatMap { entry ->
            val row = entry.asJsonObject
            val text = requireNotNull(row.string("text"))
            val bounds = row["bounds"]?.takeUnless { it.isJsonNull }?.let { ObservationTools.bounds(it.asJsonObject) }
            val confidence = row["confidence"]?.takeUnless { it.isJsonNull }?.let { require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber); it.asDouble.also { n -> require(n.isFinite()) } }
            val lines = text.lineSequence().filter { it.isNotBlank() }.take(401).toList()
            require(lines.size <= 400)
            lines.map { Line(ScriptOutputRedactor.text(it), bounds, confidence, lines.size > 1) }
        }
        require(fragments.size <= 4000)
        val merged = mutableListOf<Line>()
        for (part in fragments.sortedWith(compareBy<Line> { it.bounds?.top ?: Int.MAX_VALUE }.thenBy { it.bounds?.left ?: Int.MAX_VALUE })) {
            val index = if (part.block || part.bounds == null) -1 else merged.indexOfLast { previous ->
                val a = previous.bounds; val b = part.bounds
                if (previous.block || a == null) false else {
                    val height = minOf(a.bottom.toLong() - a.top, b.bottom.toLong() - b.top)
                    height > 0 && abs((a.top.toLong() + a.bottom) - (b.top.toLong() + b.bottom)) <= height &&
                        b.left >= a.right && b.left.toLong() - a.right <= height * 2 &&
                        previous.text.toByteArray().size + part.text.toByteArray().size < 2048
                }
            }
            if (index < 0) merged += part else {
                val previous = merged[index]; val a = previous.bounds!!; val b = part.bounds!!
                merged[index] = Line(previous.text + " " + part.text,
                    CompactNodeText.Bounds(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)),
                    if (previous.confidence != null && part.confidence != null) minOf(previous.confidence, part.confidence) else null)
            }
        }
        val output = JsonArray()
        var truncated = source.size() == 400
        val result = jsonObject("lines" to output, "sourceItems" to source.size().json(), "truncated" to true.json())
        for (line in merged.take(400)) {
            val text = AgentJson.truncate(line.text, 512)
            if (text != line.text) truncated = true
            output.add(jsonObject("text" to text.json(), "bounds" to (line.bounds?.let {
                jsonObject("left" to it.left.json(), "top" to it.top.json(), "right" to it.right.json(), "bottom" to it.bottom.json())
            } ?: JsonNull.INSTANCE), "confidence" to (line.confidence?.let(::JsonPrimitive) ?: JsonNull.INSTANCE)).apply {
                if (line.block) addProperty("boundsScope", "source-block")
            })
            if (StepJournal.bytes(result) > ObservationTools.MAX_BYTES) { output.remove(output.size() - 1); break }
        }
        result.addProperty("truncated", truncated || output.size() < merged.size)
        return result
    }
}
