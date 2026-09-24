package io.github.supermonster003.autojs6.plugin.ai.agent.service

import android.util.AtomicFile
import io.github.supermonster003.autojs6.plugin.ai.agent.model.MemoryContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Worker-only read side. P6's MemoryStore will write this versioned private snapshot after confirmation. */
internal class PrivateMemorySource(private val file: File) {
    fun snapshot(preset: String, enabled: Boolean, scope: String = "global_and_preset"): MemoryContext {
        if (!enabled || scope == "none" || !file.exists() && !File(file.path + ".bak").exists()) return MemoryContext.EMPTY
        return try {
            val bytes = AtomicFile(file).openRead().use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= MemoryContext.MAX_FILE_BYTES)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val json = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
            MemoryContext.decode(json, preset, scope in setOf("global_and_preset", "global"), scope in setOf("global_and_preset", "preset"))
        } catch (_: Exception) { MemoryContext.UNAVAILABLE }
    }
}
