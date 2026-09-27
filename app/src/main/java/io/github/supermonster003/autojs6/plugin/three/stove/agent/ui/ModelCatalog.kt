package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Context
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.store.ModelRef

/** One public catalog target, as reported by the connected host. No credentials or Provider details. */
internal data class ModelEntry(val targetId: String, val name: String, val providerId: String, val locality: ModelLocality,
                               val tools: Boolean, val vision: Boolean) {
    val ref get() = ModelRef(targetId, name)
}

/** The host model catalog, loaded through the private `targets` operation while the workbench is attached. */
internal class ModelCatalog(context: Context, private val changed: () -> Unit) {
    enum class Status { IDLE, LOADING, READY, FAILED }
    var status = Status.IDLE; private set
    var entries = emptyList<ModelEntry>(); private set
    private var attached = false
    private var connected = false
    private var generation = 0
    private val connection = PresetConnection(context) { connected = true; if (attached) refresh() }

    /** The model Automatic would pick right now, or null while the catalog is unknown or empty. */
    val automatic: ModelEntry? get() = AutomaticTarget.pick(entries) { it.locality }

    fun find(targetId: String) = entries.firstOrNull { it.targetId == targetId }

    /** A missing model is reported only once the catalog is known; admission still fails closed. */
    fun available(model: ModelRef?) = model == null || status != Status.READY || find(model.targetId) != null

    fun start() { connection.start() }
    fun stop() { generation++; connected = false; if (status == Status.LOADING) status = Status.IDLE; connection.stop() }
    fun close() { connection.close() }

    fun attached(value: Boolean) {
        val was = attached; attached = value
        if (!value && status != Status.IDLE) { status = Status.IDLE; changed() }
        if (value && !was && connected) refresh()
    }

    fun refresh() {
        if (!connected || !attached || status == Status.LOADING) return
        status = Status.LOADING; val expected = ++generation; changed()
        connection.query(jsonObject("operation" to "targets".json())) { result ->
            if (expected != generation) return@query
            val parsed = result.getOrNull()?.let(::parse)
            entries = parsed ?: entries
            status = if (parsed == null) Status.FAILED else Status.READY
            changed()
        }
    }

    companion object {
        /** Skips malformed rows instead of trusting them; the endpoint already bounds the list. */
        fun parse(value: JsonObject): List<ModelEntry> = value.getAsJsonArray("targets")?.mapNotNull { element ->
            runCatching {
                val row = element.asJsonObject
                ModelEntry(requireNotNull(row.string("targetId")), requireNotNull(row.string("displayName")).take(ModelRef.MAX_NAME),
                    row.string("providerId").orEmpty(), ModelLocality.valueOf(requireNotNull(row.string("locality"))),
                    row.flag("nativeTools") == true, row.flag("vision") == true).also { it.ref }
            }.getOrNull()
        }?.distinctBy { it.targetId }.orEmpty()
    }
}
