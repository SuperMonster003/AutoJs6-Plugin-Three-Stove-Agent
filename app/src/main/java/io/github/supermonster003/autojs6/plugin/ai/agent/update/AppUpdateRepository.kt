package io.github.supermonster003.autojs6.plugin.ai.agent.update

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import java.io.ByteArrayOutputStream
import java.net.*
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.atomic.*

internal data class ReleaseInfo(val tag: String, val url: String, val notes: String)
internal object ReleaseInfoCodec {
    const val REPOSITORY = "SuperMonster003/AutoJs6-Plugin-AI-Agent"
    const val SOURCE = "https://github.com/$REPOSITORY"
    fun validUrl(url: String, tag: String) = runCatching { URI(url).let {
        it.scheme == "https" && it.host == "github.com" && it.port == -1 && it.userInfo == null &&
            it.query == null && it.fragment == null && it.path == "/$REPOSITORY/releases/tag/$tag"
    } }.getOrDefault(false)
    fun decode(text: String): ReleaseInfo {
        val value = AgentJson.objectOf(text, 256 * 1024)
        require(value.flag("draft") == false && value.flag("prerelease") == false)
        val tag = requireNotNull(value.string("tag_name")).also { require(AppVersionPolicy.parse(it)?.pre?.isEmpty() == true) }
        val url = requireNotNull(value.string("html_url")).also { require(validUrl(it, tag)) }
        val notes = if (value["body"] == null || value["body"].isJsonNull) "" else requireNotNull(value.string("body"))
        return ReleaseInfo(tag, url, AgentJson.truncate(notes, 16 * 1024))
    }
    fun encode(release: ReleaseInfo) = jsonObject("draft" to false.json(), "prerelease" to false.json(),
        "tag_name" to release.tag.json(), "html_url" to release.url.json(), "body" to release.notes.json()).toString().also { decode(it) }
}
internal enum class UpdateFailure { NETWORK, HTTP, TOO_LARGE, MALFORMED }
internal sealed class UpdateResult {
    data class Success(val release: ReleaseInfo?) : UpdateResult()
    data class Failure(val reason: UpdateFailure) : UpdateResult()
}
internal class UpdateCancellation {
    private val stopped = AtomicBoolean()
    private val connection = AtomicReference<HttpURLConnection?>()
    val cancelled get() = stopped.get()
    fun attach(value: HttpURLConnection) { connection.set(value); if (cancelled) value.disconnect() }
    fun cancel() { stopped.set(true); connection.getAndSet(null)?.disconnect() }
}
internal fun interface UpdateSource { fun fetchLatest(cancellation: UpdateCancellation): UpdateResult }

/** HTTPS only, bounded response, cancellable IO and no APK transfer or private task data. */
internal class AppUpdateRepository(private val connect: () -> HttpURLConnection = { URL(ENDPOINT).openConnection() as HttpURLConnection }) : UpdateSource {
    override fun fetchLatest(cancellation: UpdateCancellation): UpdateResult {
        if (cancellation.cancelled) return UpdateResult.Failure(UpdateFailure.NETWORK)
        val connection = try { connect() } catch (_: Exception) { return UpdateResult.Failure(UpdateFailure.NETWORK) }
        cancellation.attach(connection)
        connection.connectTimeout = 10_000; connection.readTimeout = 10_000
        connection.instanceFollowRedirects = false; connection.useCaches = false
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
        connection.setRequestProperty("User-Agent", "AutoJs6-Plugin-AI-Agent")
        return try {
            when (connection.responseCode) {
                404 -> UpdateResult.Success(null)
                200 -> connection.inputStream.use { input ->
                    val out = ByteArrayOutputStream(); val buffer = ByteArray(8192)
                    while (!cancellation.cancelled) {
                        val count = input.read(buffer); if (count < 0) break
                        if (out.size() + count > 256 * 1024) return UpdateResult.Failure(UpdateFailure.TOO_LARGE)
                        out.write(buffer, 0, count)
                    }
                    runCatching { UpdateResult.Success(ReleaseInfoCodec.decode(Charsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(out.toByteArray())).toString())) }
                        .getOrElse { UpdateResult.Failure(UpdateFailure.MALFORMED) }
                }
                else -> UpdateResult.Failure(UpdateFailure.HTTP)
            }
        } catch (_: Exception) { UpdateResult.Failure(UpdateFailure.NETWORK) } finally { connection.disconnect() }
    }
    companion object { const val ENDPOINT = "https://api.github.com/repos/${ReleaseInfoCodec.REPOSITORY}/releases/latest" }
}
