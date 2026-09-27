package io.github.supermonster003.autojs6.plugin.three.stove.agent.model

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** Shared secret replacement and JSON string mapping used by the journal, script output and MCP responses. */
object Redaction {
    private fun escaped(secret: String) = secret.json().toString().drop(1).dropLast(1)

    /** Replaces [secret] both as raw text and in its JSON-escaped spelling; an empty secret matches nothing. */
    fun replaceSecret(text: String, secret: String, replacement: String): String {
        if (secret.isEmpty()) return text
        return text.replace(escaped(secret), replacement).replace(secret, replacement)
    }

    fun containsSecret(text: String, secret: String): Boolean = secret.isNotEmpty() && (text.contains(secret) || text.contains(escaped(secret)))

    /** Rebuilds [value] applying [transform] to every string value, and to object keys when [keys] is set. */
    fun mapStrings(value: JsonElement, keys: Boolean = false, transform: (String) -> String): JsonElement = when {
        value.isJsonObject -> JsonObject().apply {
            value.asJsonObject.entrySet().forEach { (key, child) -> add(if (keys) transform(key) else key, mapStrings(child, keys, transform)) }
        }
        value.isJsonArray -> JsonArray().apply { value.asJsonArray.forEach { add(mapStrings(it, keys, transform)) } }
        value.isJsonPrimitive && value.asJsonPrimitive.isString -> transform(value.asString).json()
        else -> value.deepCopy()
    }
}
