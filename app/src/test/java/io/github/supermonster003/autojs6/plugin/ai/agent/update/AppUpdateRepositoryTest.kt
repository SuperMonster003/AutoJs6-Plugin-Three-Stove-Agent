package io.github.supermonster003.autojs6.plugin.ai.agent.update

import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateRepositoryTest {
    private class Connection(private val code: Int, private val bytes: ByteArray) : HttpURLConnection(URL(AppUpdateRepository.ENDPOINT)) {
        var closed = false
        override fun connect() = Unit
        override fun usingProxy() = false
        override fun disconnect() { closed = true }
        override fun getResponseCode() = code
        override fun getInputStream() = bytes.inputStream()
    }
    @Test fun fetchIsBoundedTimedOutAndDoesNotFollowRedirectsOrReadErrorBodies() {
        for ((code, expected) in listOf(301 to UpdateFailure.HTTP, 403 to UpdateFailure.HTTP, 500 to UpdateFailure.HTTP,
            200 to UpdateFailure.TOO_LARGE)) {
            val connection = Connection(code, ByteArray(256 * 1024 + 1) { 32 })
            val result = AppUpdateRepository { connection }.fetchLatest(UpdateCancellation())
            assertEquals(UpdateResult.Failure(expected), result)
            assertEquals(10000, connection.connectTimeout); assertEquals(10000, connection.readTimeout)
            assertFalse(connection.instanceFollowRedirects); assertTrue(connection.closed)
        }
    }
    @Test fun noReleaseMalformedUtf8AndPreCancelledRequestAreDistinct() {
        assertEquals(UpdateResult.Success(null), AppUpdateRepository { Connection(404, byteArrayOf()) }.fetchLatest(UpdateCancellation()))
        assertEquals(UpdateResult.Failure(UpdateFailure.MALFORMED),
            AppUpdateRepository { Connection(200, byteArrayOf(0xc3.toByte(), 0x28)) }.fetchLatest(UpdateCancellation()))
        val token = UpdateCancellation(); token.cancel()
        assertEquals(UpdateResult.Failure(UpdateFailure.NETWORK), AppUpdateRepository { error("Must not connect") }.fetchLatest(token))
        val connection = Connection(200, byteArrayOf()); val active = UpdateCancellation(); active.attach(connection); active.cancel()
        assertTrue(connection.closed)
    }
}
