package io.github.supermonster003.autojs6.plugin.ai.agent.update

import org.junit.Assert.*
import org.junit.Test

class AppVersionPolicyTest {
    @Test fun semanticOrderingIncludesLargeNumbersAndPrereleaseIdentifiers() {
        val ordered = listOf("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2", "1.0.0-beta.11",
            "1.0.0-rc.1", "1.0.0", "1.0.1", "1.1.0", "10.0.0", "999999999999999999999.0.0")
        ordered.zipWithNext().forEach { (a, b) -> assertTrue("$b > $a", AppVersionPolicy.isNewer(b, a)); assertFalse(AppVersionPolicy.isNewer(a, b)) }
        assertTrue(AppVersionPolicy.isNewer("1.0.0-999999999999999999999", "1.0.0-99999999999999999999"))
    }
    @Test fun metadataAndPrefixDoNotChangeIgnoredVersionAndInvalidTagsNeverWin() {
        assertTrue(AppVersionPolicy.isIgnored("v1.2+new", "1.2.0+old"))
        assertFalse(AppVersionPolicy.isNewer("1.2.0+new", "1.2.0+old"))
        for (invalid in listOf("latest", "1", "1.0.0.0", "01.2.3", "1.0.0-01", "1.0.0-alpha..1", "1.0.0+", "x".repeat(200))) {
            assertNull(invalid, AppVersionPolicy.parse(invalid)); assertFalse(AppVersionPolicy.isNewer(invalid, "0.0.0"))
        }
    }
    @Test fun releaseCodecRejectsDraftsForeignPagesAndInvalidVersions() {
        val value = """{"tag_name":"v1.2.3","html_url":"${ReleaseInfoCodec.SOURCE}/releases/tag/v1.2.3","body":"Fixture notes","draft":false,"prerelease":false}"""
        val release = ReleaseInfoCodec.decode(value); assertEquals(release, ReleaseInfoCodec.decode(ReleaseInfoCodec.encode(release)))
        for (bad in listOf(value.replace("\"draft\":false", "\"draft\":true"), value.replace("github.com/", "github.com.example/"),
            value.replace("https://", "http://"), value.replace("v1.2.3", "latest"), value.replace("\"prerelease\":false", "\"prerelease\":true")))
            assertThrows(IllegalArgumentException::class.java) { ReleaseInfoCodec.decode(bad) }
    }
}
