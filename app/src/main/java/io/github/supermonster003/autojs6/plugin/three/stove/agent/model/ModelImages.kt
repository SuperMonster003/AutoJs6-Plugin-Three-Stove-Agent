package io.github.supermonster003.autojs6.plugin.three.stove.agent.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.autojs.plugin.three.stove.agent.api.ThreeStoveAgentContract as C
import java.io.OutputStream

/** Task-local encoded observations. Never serialize the bytes into a journal or diagnostic. */
class ModelImage(bytes: ByteArray, val width: Int, val height: Int, val mimeType: String = "image/jpeg") {
    private val encoded = bytes.copyOf()
    val byteCount = encoded.size.toLong()
    private val digest = Digests.sha256Hex(encoded)
    val estimatedTokens: Long get() = 1024L + 4L * ((width + 31) / 32) * ((height + 31) / 32)
    init {
        require(width in 1..4096 && height in 1..4096 && byteCount in 1..C.MAX_MODEL_IMAGE_BYTES)
        require(mimeType in setOf("image/jpeg", "image/png"))
    }
    fun writeTo(output: OutputStream) { output.write(encoded) }
    fun reference(index: Int, messageIndex: Int? = null): JsonObject {
        require(index in 0 until C.MAX_MODEL_IMAGES && (messageIndex == null || messageIndex in 0..255))
        return jsonObject("descriptorIndex" to index.json(), "mimeType" to mimeType.json(), "byteLength" to byteCount.json(),
            "width" to width.json(), "height" to height.json(), "sha256" to digest.json()).apply {
            messageIndex?.let { addProperty("messageIndex", it) }
        }
    }
    override fun toString() = "ModelImage(width=$width, height=$height, bytes=$byteCount)"
}

/** Negotiated ceilings only. A matching target capability is required independently. */
data class VisionLimits(val images: Int = C.MAX_MODEL_IMAGES, val imageBytes: Long = C.MAX_MODEL_IMAGE_BYTES,
                        val batchBytes: Long = C.MAX_MODEL_TOTAL_IMAGE_BYTES, val edge: Int = 4096,
                        val pixels: Long = 16_777_216, val sessionImages: Int = 16, val sessionBytes: Long = 32L * 1024 * 1024) {
    init {
        require(images in 1..C.MAX_MODEL_IMAGES && imageBytes in 1..C.MAX_MODEL_IMAGE_BYTES && batchBytes in 1..C.MAX_MODEL_TOTAL_IMAGE_BYTES)
        require(edge in 1..4096 && pixels in 1..16_777_216 && sessionImages in 1..16 && sessionBytes in 1..32L * 1024 * 1024)
    }
    fun validate(values: List<ModelImage>, retainedCount: Int = 0, retainedBytes: Long = 0) {
        if (values.size > images || values.sumOf { it.byteCount } > batchBytes || values.any {
            it.byteCount > imageBytes || it.width > edge || it.height > edge || it.width.toLong() * it.height > pixels
        } || values.size > sessionImages - retainedCount || values.sumOf { it.byteCount } > sessionBytes - retainedBytes) throw ContextLimitExceeded()
    }
    companion object {
        fun fromBroker(info: JsonObject): VisionLimits? {
            if (info.number(C.MODEL_INFO_VISION_VERSION) != C.MODEL_VISION_VERSION.toLong()) return null
            fun limit(key: String, maximum: Long) = requireNotNull(info.number(key)).also { require(it > 0) }.coerceAtMost(maximum)
            return VisionLimits(limit("maximumImages", C.MAX_MODEL_IMAGES.toLong()).toInt(), limit("maximumImageBytes", C.MAX_MODEL_IMAGE_BYTES),
                limit("maximumTotalImageBytes", C.MAX_MODEL_TOTAL_IMAGE_BYTES), limit("maximumImageEdge", 4096).toInt(),
                limit("maximumImagePixels", 16_777_216), limit("maximumSessionImages", 16).toInt(), limit("maximumSessionImageBytes", 32L * 1024 * 1024))
        }
    }
}

fun imageReferences(images: List<ModelImage>, firstIndex: Int = 0, messageIndex: Int? = null) = JsonArray().apply {
    images.forEachIndexed { index, image -> add(image.reference(firstIndex + index, messageIndex)) }
}
