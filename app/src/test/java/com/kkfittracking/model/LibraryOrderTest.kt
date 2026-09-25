package com.kkfittracking.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryOrderTest {
    @Test
    fun theUsersOrderComesFirstAndNewSectionsFollow() {
        val sections = listOf("chest", "back", "legs", "core", "mine")
        assertEquals(sections, sections.inUserOrder(emptyList()) { it })
        // Legs first, then core; the others keep their places after them.
        assertEquals(listOf("legs", "core", "chest", "back", "mine"), sections.inUserOrder(listOf("legs", "core")) { it })
        // A section that no longer exists is simply ignored.
        assertEquals(listOf("back", "chest", "legs", "core", "mine"), sections.inUserOrder(listOf("gone", "back")) { it })
    }

    @Test
    fun movingASectionUpOrDown() {
        val shown = listOf("chest", "back", "legs")
        assertEquals(listOf("chest", "legs", "back"), moveInOrder(shown, "legs", -1))
        assertEquals(listOf("back", "chest", "legs"), moveInOrder(shown, "chest", 1))
        assertEquals(shown, moveInOrder(shown, "chest", -1))
        assertEquals(TrainingStyle.MOBILITY, TrainingStyle.sectionOrder.inUserOrder(listOf("MOBILITY")) { it.name }.first())
    }
}
