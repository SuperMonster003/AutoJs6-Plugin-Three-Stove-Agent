package io.github.supermonster003.autojs6.plugin.ai.agent.store

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

/** A model named by its public catalog id. The name is a display hint until the catalog loads. */
internal data class ModelRef(val targetId: String, val name: String) {
    init {
        PresetCodec.target(targetId)
        require(name.isNotBlank() && name.length <= MAX_NAME)
    }
    companion object {
        const val MAX_NAME = 256
    }
}

/**
 * The model chosen for new tasks started from the plugin UI, shared by the workbench and the floating
 * ball. [current] null means Automatic. Presets never carry this choice.
 */
internal data class ModelSelectionState(
    val current: ModelRef? = null,
    val recents: List<ModelRef> = emptyList(),
    val pinned: List<ModelRef> = emptyList(),
) {
    init {
        require(recents.size <= MAX_RECENTS && pinned.size <= MAX_PINNED)
        require(recents.distinctBy { it.targetId }.size == recents.size && pinned.distinctBy { it.targetId }.size == pinned.size)
    }

    /** Selects a model (or Automatic) and moves an explicit model to the front of the recent list. */
    fun choose(model: ModelRef?): ModelSelectionState = if (model == null) copy(current = null) else copy(
        current = model, recents = (listOf(model) + recents.filter { it.targetId != model.targetId }).take(MAX_RECENTS))

    fun isPinned(targetId: String) = pinned.any { it.targetId == targetId }

    /** Adds or removes a pin. A full pin list leaves the state unchanged. */
    fun togglePin(model: ModelRef): ModelSelectionState = when {
        isPinned(model.targetId) -> copy(pinned = pinned.filter { it.targetId != model.targetId })
        pinned.size >= MAX_PINNED -> this
        else -> copy(pinned = pinned + model)
    }

    /** Refreshes display names from a loaded catalog without changing which models are chosen. */
    fun renamed(names: Map<String, String>): ModelSelectionState {
        fun ModelRef.fresh() = names[targetId]?.takeIf { it != name && it.isNotBlank() && it.length <= ModelRef.MAX_NAME }
            ?.let { copy(name = it) } ?: this
        return ModelSelectionState(current?.fresh(), recents.map { it.fresh() }, pinned.map { it.fresh() })
    }

    companion object {
        const val MAX_RECENTS = 8
        const val MAX_PINNED = 16
        val AUTOMATIC = ModelSelectionState()
    }
}

/** Closed private format. Anything unreadable fails closed to Automatic. */
internal object ModelSelectionCodec {
    const val VERSION = 1L
    const val MAX_BYTES = 16 * 1024
    private val fields = setOf("version", "current", "recents", "pinned")

    fun decode(value: JsonObject?): ModelSelectionState = runCatching {
        requireNotNull(value)
        require(fields.containsAll(value.keySet()) && value.number("version") == VERSION)
        fun ref(element: JsonElement): ModelRef {
            require(element.isJsonObject)
            val entry = element.asJsonObject
            require(entry.keySet() == setOf("targetId", "name"))
            return ModelRef(requireNotNull(entry.string("targetId")), requireNotNull(entry.string("name")))
        }
        fun list(key: String): List<ModelRef> = if (!value.has(key)) emptyList()
            else requireNotNull(value[key]?.takeIf { it.isJsonArray }).asJsonArray.map(::ref)
        ModelSelectionState(value["current"]?.takeUnless { it.isJsonNull }?.let(::ref), list("recents"), list("pinned"))
    }.getOrDefault(ModelSelectionState.AUTOMATIC)

    fun encode(state: ModelSelectionState): JsonObject {
        fun ref(model: ModelRef) = jsonObject("targetId" to model.targetId.json(), "name" to model.name.json())
        return jsonObject("version" to VERSION.json(),
            "recents" to JsonArray().apply { state.recents.forEach { add(ref(it)) } },
            "pinned" to JsonArray().apply { state.pinned.forEach { add(ref(it)) } })
            .apply { state.current?.let { add("current", ref(it)) } }
            .also { require(it.toString().toByteArray(Charsets.UTF_8).size <= MAX_BYTES) }
    }
}
