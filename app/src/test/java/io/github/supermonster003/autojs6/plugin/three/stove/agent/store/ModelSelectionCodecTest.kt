package io.github.supermonster003.autojs6.plugin.three.stove.agent.store

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class ModelSelectionCodecTest {
    private fun ref(index: Int, name: String = "Model $index") = ModelRef("profile:m$index", name)

    @Test fun choosingMovesTheModelToTheFrontOfABoundedRecentListAndAutomaticKeepsIt() {
        var state = ModelSelectionState.AUTOMATIC
        for (index in 1..10) state = state.choose(ref(index))
        assertEquals(ref(10), state.current)
        assertEquals((10 downTo 3).map { ref(it) }, state.recents)
        state = state.choose(ref(5))
        assertEquals(listOf(5, 10, 9, 8, 7, 6, 4, 3).map { ref(it) }, state.recents)
        val automatic = state.choose(null)
        assertNull(automatic.current); assertEquals(state.recents, automatic.recents)
    }

    @Test fun pinsToggleAndAFullListIsLeftUnchanged() {
        var state = ModelSelectionState.AUTOMATIC
        for (index in 1..ModelSelectionState.MAX_PINNED) state = state.togglePin(ref(index))
        assertEquals(ModelSelectionState.MAX_PINNED, state.pinned.size)
        assertSame(state, state.togglePin(ref(99)))
        state = state.togglePin(ref(1))
        assertFalse(state.isPinned("profile:m1")); assertTrue(state.isPinned("profile:m2"))
    }

    @Test fun roundTripAndCatalogRenamesKeepTheChoice() {
        val state = ModelSelectionState.AUTOMATIC.choose(ref(1)).togglePin(ref(2)).choose(ref(3))
        assertEquals(state, ModelSelectionCodec.decode(ModelSelectionCodec.encode(state)))
        val renamed = state.renamed(mapOf("profile:m3" to "Renamed", "profile:m2" to " "))
        assertEquals("Renamed", renamed.current!!.name); assertEquals("Model 2", renamed.pinned.single().name)
        assertEquals("profile:m3", renamed.current!!.targetId)
    }

    @Test fun anythingUnreadableFailsClosedToAutomatic() {
        val valid = ModelSelectionCodec.encode(ModelSelectionState.AUTOMATIC.choose(ref(1)))
        fun decode(change: JsonObject.() -> Unit) = ModelSelectionCodec.decode(valid.deepCopy().apply(change))
        assertEquals(ModelSelectionState.AUTOMATIC, ModelSelectionCodec.decode(null))
        assertEquals(ModelSelectionState.AUTOMATIC, decode { addProperty("version", 2) })
        assertEquals(ModelSelectionState.AUTOMATIC, decode { addProperty("unknown", true) })
        assertEquals(ModelSelectionState.AUTOMATIC, decode { add("current", jsonObject("targetId" to "No spaces allowed".json(), "name" to "x".json())) })
        assertEquals(ModelSelectionState.AUTOMATIC, decode { add("current", jsonObject("targetId" to "profile:m1".json(), "name" to " ".json())) })
        assertEquals(ModelSelectionState.AUTOMATIC, decode { getAsJsonArray("recents").add(getAsJsonArray("recents")[0]) })
        assertEquals(ModelSelectionState.AUTOMATIC, decode { addProperty("pinned", "profile:m1") })
        assertEquals(ref(1), decode { }.current)
    }
}
