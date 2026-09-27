package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.util.AtomicFile
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.model.AgentJson
import java.io.File
import java.io.FileOutputStream

/**
 * A small JSON snapshot read and written by both the UI and the :agent process (floating window).
 * AtomicFile on older Android versions can restore a backup from openRead(), so every access is
 * serialized across processes with a lock file, and within a process with a monitor.
 */
internal class LockedJsonFile(directory: File, name: String, private val maxBytes: Int) {
    private val file = AtomicFile(File(directory, "$name.json"))
    private val lock = File(directory, "$name.lock")

    /** The stored value, or null when the file is missing or unreadable. */
    fun read(): JsonObject? = locked(::readUnlocked)

    fun write(value: JsonObject) = locked { writeUnlocked(value) }

    /** Read-modify-write under one lock; [transform] receives null for a missing or unreadable file. */
    fun update(transform: (JsonObject?) -> JsonObject): JsonObject = locked { transform(readUnlocked()).also(::writeUnlocked) }

    private fun readUnlocked(): JsonObject? = runCatching {
        file.openRead().use { stream ->
            require(stream.channel.size() <= maxBytes)
            AgentJson.objectOf(stream.readBytes().toString(Charsets.UTF_8), maxBytes)
        }
    }.getOrNull()

    private fun writeUnlocked(value: JsonObject) {
        val bytes = value.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= maxBytes)
        val stream = file.startWrite()
        try { stream.write(bytes); file.finishWrite(stream) }
        catch (failure: Exception) { file.failWrite(stream); throw failure }
    }

    private fun <T> locked(action: () -> T): T = synchronized(MONITOR) {
        FileOutputStream(lock, true).channel.use { channel -> channel.lock().use { action() } }
    }

    private companion object {
        val MONITOR = Any()
    }
}
