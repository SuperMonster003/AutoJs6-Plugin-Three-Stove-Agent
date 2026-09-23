package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptOutputRedactor

/** Normalizes only successful observation replies, before they enter journals or model context. */
class ObservationTools {
    val nodes = NodeRefRegistry()
    private var lastAction: CompactNodeText.Snapshot? = null
    fun actionBaseline(snapshot: CompactNodeText.Snapshot?) { lastAction = snapshot }
    val hasActionBaseline get() = lastAction != null
    fun sinceAction(snapshot: CompactNodeText.Snapshot) = lastAction?.let { baseline ->
        NodeRefRegistry().use { registry -> registry.record(baseline); registry.record(snapshot) }
    }
    fun transform(invocation: ToolInvocation, value: JsonElement): JsonElement = when (invocation.name) {
        "ui_dump" -> {
            val snapshot = CompactNodeText.parse(value)
            val result = value.asJsonObject.deepCopy()
            result.add("changes", nodes.record(snapshot))
            sinceAction(snapshot)?.let { result.add("sinceLastAction", it) }
            ObservationCompactor.compact(result, MAX_BYTES, false)
        }
        "ui_find" -> {
            require(value.isJsonArray)
            val limit = invocation.arguments.number("limit")?.toInt() ?: 10
            val items = JsonArray()
            var bytes = 128
            for (entry in value.asJsonArray.take(limit)) {
                val item = node(entry)
                bytes += StepJournal.bytes(item) + 1
                if (bytes > MAX_BYTES) break
                items.add(item)
            }
            jsonObject("nodes" to items, "total" to value.asJsonArray.size().json(), "truncated" to (items.size() < value.asJsonArray.size()).json())
        }
        "ui_wait_for" -> value.asJsonObject.deepCopy().apply {
            require(flag("matched") == true && string("state") in setOf("appear", "disappear"))
            val entry = get("node") ?: JsonNull.INSTANCE
            if (!entry.isJsonNull && entry != false.json()) add("node", node(entry))
        }
        "console_tail" -> console(value, invocation.arguments.number("lines")?.toInt() ?: 40)
        "ocr_screen" -> OcrScreenObservation.normalize(value)
        "app_current", "device_info" -> ObservationCompactor.compact(ScriptOutputRedactor.redact(value), MAX_BYTES, false)
        "screen_state" -> { require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean); jsonObject("screenOn" to value) }
        else -> value
    }
    private fun node(value: JsonElement): JsonObject = JsonObject().apply {
        val source = value.asJsonObject
        for (key in listOf("text", "desc", "id", "className")) {
            val original = requireNotNull(source.string(key))
            val bounded = AgentJson.truncate(original, 512)
            addProperty(key, bounded)
            if (original != bounded) addProperty("truncated", true)
        }
        for (key in listOf("clickable", "enabled")) addProperty(key, requireNotNull(source.flag(key)))
        bounds(source.getAsJsonObject("bounds"))
        add("bounds", source.getAsJsonObject("bounds").deepCopy())
    }
    private fun console(value: JsonElement, count: Int): JsonElement {
        // The host console window is process-wide; never imply engine-exclusive ownership.
        val objectValue = value.asJsonObject
        val entries = objectValue.getAsJsonArray("entries")
        require(entries.size() <= 500 && count in 1..500)
        val lines = ArrayDeque<String>()
        var dropped = false
        for (entry in entries) {
            val message = requireNotNull(entry.asJsonObject.string("text"))
            ScriptOutputRedactor.text(message).lineSequence().forEach { lines.add(it); if (lines.size > count) { lines.removeFirst(); dropped = true } }
        }
        val output = JsonArray()
        var bytes = 2
        for (line in lines.toList().asReversed()) {
            val text = AgentJson.truncate(line, 512).json()
            val cost = StepJournal.bytes(text) + 1
            if (bytes + cost > 8192) break
            output.add(text); bytes += cost
        }
        return jsonObject("lines" to JsonArray().apply { output.reversed().forEach(::add) },
            "consoleCaptureMode" to "global-window".json(), "truncated" to (dropped || output.size() < lines.size || lines.any { it.toByteArray().size > 512 } || objectValue.flag("truncated") == true).json())
    }
    companion object {
        const val MAX_BYTES = 20 * 1024
        internal fun bounds(value: JsonObject): CompactNodeText.Bounds {
            fun coordinate(key: String) = requireNotNull(value.number(key)).also { require(it in Int.MIN_VALUE..Int.MAX_VALUE) }.toInt()
            return CompactNodeText.Bounds(coordinate("left"), coordinate("top"), coordinate("right"), coordinate("bottom"))
        }
    }
}
