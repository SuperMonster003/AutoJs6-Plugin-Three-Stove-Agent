package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HostPresenceTest {

    @Test
    fun `a missing package is reported before anything else`() {
        assertEquals(HostPresence.MISSING, classifyHostPresence(null, REQUIRED))
    }

    @Test
    fun `a disabled package wins over an outdated version`() {
        val disabledAndOld = HostPackageSnapshot(enabled = false, versionCode = REQUIRED - 100, versionName = "6.7.0")
        assertEquals(HostPresence.DISABLED, classifyHostPresence(disabledAndOld, REQUIRED))
        val disabledAndNew = HostPackageSnapshot(enabled = false, versionCode = REQUIRED + 1, versionName = "6.8.1")
        assertEquals(HostPresence.DISABLED, classifyHostPresence(disabledAndNew, REQUIRED))
    }

    @Test
    fun `an enabled package older than the required build is incompatible`() {
        val old = HostPackageSnapshot(enabled = true, versionCode = REQUIRED - 1, versionName = "6.8.0")
        assertEquals(HostPresence.INCOMPATIBLE, classifyHostPresence(old, REQUIRED))
    }

    @Test
    fun `the required build itself and anything newer are ready`() {
        val exact = HostPackageSnapshot(enabled = true, versionCode = REQUIRED, versionName = "6.8.0")
        assertEquals(HostPresence.READY, classifyHostPresence(exact, REQUIRED))
        val newer = HostPackageSnapshot(enabled = true, versionCode = REQUIRED + 500, versionName = "6.9.0")
        assertEquals(HostPresence.READY, classifyHostPresence(newer, REQUIRED))
    }

    private companion object {
        const val REQUIRED = 5287L
    }
}
