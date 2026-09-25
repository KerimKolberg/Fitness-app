package com.kerimkolberg.fitnessapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetStylesTest {
    private fun exercise(id: String, superset: String? = null) =
        DayExercise("we-$id", id, id, ExerciseType.WEIGHT_REPS, 0, emptyList(), superset)

    @Test
    fun supersetsAreShownTogetherWhereTheyStart() {
        val day = listOf(exercise("a"), exercise("b", "s"), exercise("c"), exercise("d", "s"), exercise("e", "lonely"))
        val blocks = groupDay(day)
        assertEquals(4, blocks.size)
        assertEquals("a", (blocks[0] as DayBlock.Single).exercise.exerciseId)
        assertEquals(listOf("b", "d"), (blocks[1] as DayBlock.Superset).exercises.map { it.exerciseId })
        assertEquals("c", (blocks[2] as DayBlock.Single).exercise.exerciseId)
        // A superset left with one exercise is shown as a normal exercise.
        assertEquals("e", (blocks[3] as DayBlock.Single).exercise.exerciseId)
    }

    @Test
    fun supersetOrderWrapsAround() {
        val members = listOf("a", "b", "c")
        assertEquals("b", nextInSuperset(members, "a"))
        assertEquals("a", nextInSuperset(members, "c"))
        assertNull(nextInSuperset(listOf("a"), "a"))
        assertNull(nextInSuperset(members, "x"))
        assertFalse(isLastInSuperset(members, "a"))
        assertTrue(isLastInSuperset(members, "c"))
        assertTrue(isLastInSuperset(emptyList(), "a"))
    }

    @Test
    fun dropSetWeightsAreRoundedToRealPlates() {
        assertEquals(80.0, dropSetWeightKg(100.0, 20, UnitSystem.METRIC), 1e-9)
        assertEquals(50.0, dropSetWeightKg(62.5, 20, UnitSystem.METRIC), 1e-9)
        assertEquals(26.0, dropSetWeightKg(32.5, 20, UnitSystem.METRIC), 1e-9)
        // 225 lb - 20% = 180 lb.
        assertEquals(180.0, UnitSystem.IMPERIAL.weightFromKg(dropSetWeightKg(UnitSystem.IMPERIAL.weightToKg(225.0), 20, UnitSystem.IMPERIAL)), 1e-9)
    }

    @Test
    fun dropSetsContinueTheSetBefore() {
        fun set(drop: Boolean) = SetEntry("x", SetValues(weightKg = 100.0, reps = 8, isDropSet = drop))
        assertEquals(
            listOf("1", "2", "↘", "↘", "3"),
            setLabels(listOf(set(false), set(false), set(true), set(true), set(false))),
        )
        // A drop set with nothing before it still gets a number.
        assertEquals(listOf("1"), setLabels(listOf(set(true))))
    }
}
