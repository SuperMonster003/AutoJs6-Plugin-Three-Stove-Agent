package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

class SpeechInputTest {
    @Test fun microphonePinsMacawOnlyWhilePreferredAndResolvable() {
        assertEquals("io.github.supermonster003.autojs6.plugin.three.macaw.speech", SpeechInput.MACAW_PACKAGE)
        assertEquals(SpeechInput.MACAW_PACKAGE, SpeechInput.target(preferMacaw = true, macawAvailable = true))
        assertNull("Switched off: the system chooses", SpeechInput.target(preferMacaw = false, macawAvailable = true))
        assertNull("Not installed or its system voice input is off", SpeechInput.target(preferMacaw = true, macawAvailable = false))
        assertNull(SpeechInput.target(preferMacaw = false, macawAvailable = false))
    }

    @Test fun everyLocaleNamesMacawInTheVoiceTexts() {
        val res = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.isDirectory(it.resolve("app/src/main")) }
            .resolve("app/src/main/res")
        val locales = listOf("values", "values-en", "values-ar", "values-es", "values-fr", "values-ja", "values-ko", "values-ru",
            "values-zh", "values-zh-rHK", "values-zh-rTW")
        for (locale in locales) {
            val strings = strings(res.resolve("$locale/strings.xml"))
            // Arabic isolates the digit-led name so that it is not reordered into "Macaw Speech-3".
            val name = if (locale == "values-ar") "\u20683-Macaw Speech\u2069" else "3-Macaw Speech"
            for (key in listOf("settings_voice_macaw", "settings_voice_macaw_note", "settings_voice_unavailable")) {
                assertTrue("$locale $key", strings.getValue(key).contains(name))
            }
            assertFalse("$locale: the recognizer note stays generic", strings.getValue("settings_voice_note").contains("Macaw"))
        }
    }

    private fun strings(path: Path): Map<String, String> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile()).documentElement.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) }.associate { it.attributes.getNamedItem("name").nodeValue to it.textContent }
    }
}
