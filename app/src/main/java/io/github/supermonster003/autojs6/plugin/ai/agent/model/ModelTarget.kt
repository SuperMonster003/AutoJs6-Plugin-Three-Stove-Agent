package io.github.supermonster003.autojs6.plugin.ai.agent.model

import com.google.gson.JsonObject

enum class ModelLocality { ON_DEVICE, REMOTE, HYBRID }

/** Public catalog metadata only; never contains credentials, Provider components or profile configuration. */
class ModelTarget(
    val providerId: String, val targetId: String, val locality: ModelLocality,
    val protocol: ModelProtocol, val structuredJson: Boolean,
    val maximumContextBytes: Int, val maximumOutputBytes: Int = AgentJson.MAX_MODEL_BYTES,
) {
    init {
        require(providerId.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{0,255}")))
        require(targetId.matches(Regex("(?:local|profile):[a-zA-Z0-9][a-zA-Z0-9._:-]{0,247}")))
        require(maximumContextBytes > 0 && maximumOutputBytes in 1..AgentJson.MAX_MODEL_BYTES)
        require((locality == ModelLocality.ON_DEVICE) == (protocol == ModelProtocol.LOCAL) || protocol == ModelProtocol.UNKNOWN)
    }
    val schemaTarget get() = SchemaTarget(providerId, targetId, protocol, structuredJson)
    override fun toString() = "ModelTarget(locality=$locality, protocol=$protocol, maximumContextBytes=$maximumContextBytes)"

    companion object {
        /** Current V1 catalogs do not expose an online wire protocol. Never guess one from an ID or display name. */
        fun fromCatalog(providerId: String, value: JsonObject, maximumOutputBytes: Int = AgentJson.MAX_MODEL_BYTES): ModelTarget {
            require(value.flag("configured") == true && value.flag("available") == true) { "TARGET_UNAVAILABLE" }
            val locality = when (value.number("locality")) {
                1L -> ModelLocality.ON_DEVICE; 2L -> ModelLocality.REMOTE; 3L -> ModelLocality.HYBRID
                else -> throw IllegalArgumentException("Invalid target locality")
            }
            val bytes = requireNotNull(value.number("maximumContextBytes"))
            require(bytes in 1..Int.MAX_VALUE)
            val capabilities = requireNotNull(value.getAsJsonArray("capabilityIds"))
            require(capabilities.size() <= 64 && capabilities.all { it.isJsonPrimitive && it.asJsonPrimitive.isString })
            return ModelTarget(providerId, requireNotNull(value.string("targetId")), locality,
                if (locality == ModelLocality.ON_DEVICE) ModelProtocol.LOCAL else ModelProtocol.UNKNOWN,
                capabilities.any { it.asString == "structured-json" }, bytes.toInt(), maximumOutputBytes)
        }
    }
}
