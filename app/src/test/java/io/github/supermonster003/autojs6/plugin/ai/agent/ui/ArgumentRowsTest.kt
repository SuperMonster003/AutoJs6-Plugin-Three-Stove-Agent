package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class ArgumentRowsTest {
    @Test fun textStaysExactWhileLiteralsAreMarkedAndNestedNamesAreDotted() {
        val value = "<b>line\nquoted\"</b>" + "x".repeat(8000)
        val rows = ArgumentRows.rows(AgentJson.objectOf("""{"z":false,"a":${value.json()},"n":1.5,"none":null,"text":"false",
            "target":{"ref":"n12","point":[10,20]},"empty":{},"list":[]}"""))
        assertEquals(listOf("z", "a", "n", "none", "text", "target.ref", "target.point[0]", "target.point[1]", "empty", "list"), rows.map { it.name })
        assertEquals(ArgumentRow("a", value, false), rows[1])
        assertEquals(ArgumentRow("z", "false", true), rows[0])
        assertEquals("The string \"false\" is not the boolean false", ArgumentRow("text", "false", false), rows[4])
        assertEquals(ArgumentRow("none", "null", true), rows[3])
        assertEquals(ArgumentRow("empty", "{}", true), rows[8]); assertEquals(ArgumentRow("list", "[]", true), rows[9])
    }

    @Test fun scriptParametersSortDeterministicallyAndEmptyArgumentsHaveNoRows() {
        val rows = ArgumentRows.rows(AgentJson.objectOf("""{"z":false,"a":"x","n":1}"""), sorted = true)
        assertEquals(listOf("a", "n", "z"), rows.map { it.name })
        assertTrue(ArgumentRows.rows(AgentJson.objectOf("{}")).isEmpty())
        assertTrue(ArgumentRows.rows(null).isEmpty())
        assertEquals(listOf(ArgumentRow("value", "3", true)), ArgumentRows.rows(3.json()))
    }

    @Test fun deepValuesStayJsonInsteadOfBeingLost() {
        var nested = """"leaf""""
        repeat(ArgumentRows.MAX_DEPTH + 2) { nested = """{"k":$nested}""" }
        val row = ArgumentRows.rows(AgentJson.objectOf(nested)).single()
        assertEquals(List(ArgumentRows.MAX_DEPTH) { "k" }.joinToString("."), row.name)
        assertTrue(row.literal); assertTrue(row.value.contains("leaf"))
    }
}
