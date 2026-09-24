package io.github.supermonster003.autojs6.plugin.ai.agent.store

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Worker-confined atomic store. A successful write publishes a new immutable run-admission snapshot. */
internal class PresetStore(private val file: File) {
    private var current: PresetSnapshot? = null
    fun open(): PresetSnapshot {
        check(current == null)
        val backup = File(file.path + ".bak")
        val source = if (backup.exists()) backup else file
        val loaded = if (!source.exists()) PresetSnapshot.INITIAL else {
            require(source.length() <= PresetCodec.MAX_FILE_BYTES)
            val bytes = source.inputStream().use { it.readBytes() }.also { require(it.size <= PresetCodec.MAX_FILE_BYTES) }
            PresetCodec.decode(Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString())
        }
        if (backup.exists()) { check(!file.exists() || file.delete()); check(backup.renameTo(file)) }
        current = loaded
        return loaded
    }
    fun save(preset: Preset, create: Boolean, allowedGroups: Set<String>, allowedRoots: Set<String>): PresetSnapshot {
        val before = checkNotNull(current)
        PresetCodec.decodePreset(PresetCodec.encodePreset(preset))
        require((before.presets.none { it.name == preset.name }) == create)
        require(preset.toolGroups == null || allowedGroups.containsAll(preset.toolGroups))
        require(preset.scriptRoots == null || allowedRoots.containsAll(preset.scriptRoots))
        val next = if (create) before.presets + preset else before.presets.map { if (it.name == preset.name) preset else it }
        return write(PresetSnapshot(before.defaultName, next))
    }
    fun delete(name: String): PresetSnapshot {
        val before = checkNotNull(current); require(name != "default"); before.resolve(name)
        return write(PresetSnapshot(if (before.defaultName == name) "default" else before.defaultName, before.presets.filter { it.name != name }))
    }
    fun setDefault(name: String): PresetSnapshot {
        val before = checkNotNull(current); before.resolve(name)
        return write(PresetSnapshot(name, before.presets))
    }
    private fun write(next: PresetSnapshot): PresetSnapshot {
        val text = PresetCodec.encode(next)
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        val temp = File(file.path + ".new"); val backup = File(file.path + ".bak")
        try {
            FileOutputStream(temp).use { it.write(text.toByteArray(Charsets.UTF_8)); it.fd.sync() }
            check(!backup.exists())
            if (file.exists()) check(file.renameTo(backup))
            if (!temp.renameTo(file)) {
                if (backup.exists()) check(backup.renameTo(file))
                error("Preset write failed")
            }
            if (backup.exists() && !backup.delete()) {
                check(file.delete()); check(backup.renameTo(file)); error("Preset write failed")
            }
            current = next
            return next
        } finally { temp.delete() }
    }
}
