package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader
import java.math.BigDecimal

/** Bounded tree decoding, without reflection, coercion, duplicate keys or lenient JSON. */
object AgentJson {
    const val MAX_MODEL_BYTES = 64 * 1024
    fun parse(text: String, maxBytes: Int = MAX_MODEL_BYTES): JsonElement {
        require(text.length <= maxBytes && text.toByteArray(Charsets.UTF_8).size <= maxBytes) { "JSON exceeds byte limit" }
        var nodes = 0
        JsonReader(StringReader(text)).use { reader ->
            reader.strictness = Strictness.STRICT
            fun read(depth: Int): JsonElement {
                require(depth <= 32 && ++nodes <= 16_384) { "JSON exceeds structural limit" }
                return when (reader.peek()) {
                    JsonToken.BEGIN_OBJECT -> JsonObject().apply {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val key = reader.nextName().also(::checkUnicode)
                            require(!has(key)) { "Duplicate JSON key" }
                            add(key, read(depth + 1))
                        }
                        reader.endObject()
                    }
                    JsonToken.BEGIN_ARRAY -> JsonArray().apply {
                        reader.beginArray()
                        while (reader.hasNext()) add(read(depth + 1))
                        reader.endArray()
                    }
                    JsonToken.STRING -> JsonPrimitive(reader.nextString().also(::checkUnicode))
                    JsonToken.NUMBER -> {
                        val raw = reader.nextString()
                        require(raw.length <= 128) { "Number exceeds limit" }
                        val number = BigDecimal(raw)
                        require(kotlin.math.abs(number.scale().toLong()) <= 1024) { "Number exponent exceeds limit" }
                        JsonPrimitive(number)
                    }
                    JsonToken.BOOLEAN -> JsonPrimitive(reader.nextBoolean())
                    JsonToken.NULL -> { reader.nextNull(); JsonNull.INSTANCE }
                    else -> throw IllegalArgumentException("Expected JSON value")
                }
            }
            val result = read(0)
            require(reader.peek() == JsonToken.END_DOCUMENT) { "Trailing JSON content" }
            return result
        }
    }

    fun objectOf(text: String, maxBytes: Int = MAX_MODEL_BYTES): JsonObject =
        parse(text, maxBytes).also { require(it.isJsonObject) { "Expected JSON object" } }.asJsonObject

    fun checkUnicode(text: String) {
        var index = 0
        while (index < text.length) {
            val char = text[index++]
            if (char.isHighSurrogate()) require(index < text.length && text[index++].isLowSurrogate()) { "Unpaired surrogate" }
            else require(!char.isLowSurrogate()) { "Unpaired surrogate" }
        }
    }

    fun truncate(text: String, maxBytes: Int): String {
        require(maxBytes >= 0)
        if (text.toByteArray(Charsets.UTF_8).size <= maxBytes) return text
        var end = 0
        var bytes = 0
        while (end < text.length) {
            val point = text.codePointAt(end)
            val size = when { point <= 0x7f -> 1; point <= 0x7ff -> 2; point <= 0xffff -> 3; else -> 4 }
            if (bytes + size > maxBytes) break
            bytes += size
            end += Character.charCount(point)
        }
        return text.substring(0, end)
    }
}

fun jsonObject(vararg pairs: Pair<String, JsonElement>) = JsonObject().apply { pairs.forEach { (key, value) -> add(key, value) } }
fun jsonArray(vararg items: JsonElement) = JsonArray().apply { items.forEach(::add) }
fun String.json() = JsonPrimitive(this)
fun Number.json() = JsonPrimitive(this)
fun Boolean.json() = JsonPrimitive(this)

fun JsonObject.string(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
fun JsonObject.number(key: String): Long? = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asBigDecimal?.longValueExact()
fun JsonObject.flag(key: String): Boolean? = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean
