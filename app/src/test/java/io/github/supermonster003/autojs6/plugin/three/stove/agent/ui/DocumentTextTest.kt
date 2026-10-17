package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DocumentTextTest {
    @Test fun linksKeepOnlyTheirLabel() {
        assertEquals(
            "see ROADMAP.md. Native tools",
            DocumentText.linkLabels("see [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md). Native tools"),
        )
        assertEquals("a b and c", DocumentText.linkLabels("[a](x) [b](https://y/z) and c"))
    }

    @Test fun textThatIsNotALinkIsUnchanged() {
        for (value in listOf("[Feature] item", "(see notes)", "[label] (url)", "[](empty)", "f(x) = [1, 2]"))
            assertEquals(value, DocumentText.linkLabels(value))
    }

    @Test fun bundledChangelogsShowNoRawLinkTargets() {
        val dir = File("src/main/assets/doc")
        val docs = dir.listFiles { file -> file.name.startsWith("CHANGELOG-") }.orEmpty()
        assertTrue("no bundled changelogs under ${dir.absolutePath}", docs.isNotEmpty())
        for (doc in docs) for (line in doc.readLines()) {
            val outsideCode = line.split('`').filterIndexed { index, _ -> index % 2 == 0 }
            for (part in outsideCode) assertFalse("${doc.name}: $line", DocumentText.linkLabels(part).contains("]("))
        }
    }
}
