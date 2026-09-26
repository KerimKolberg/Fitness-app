package com.kkfittracking.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UnitsTest {
    @Test
    fun machineLevelsAreKeptAsTheyAre() {
        val levels = UnitSystem.LEVELS
        assertEquals(7.0, levels.weightFromKg(7.0), 1e-9)
        assertEquals(7.0, levels.weightToKg(7.0), 1e-9)
        assertEquals("7 lvl", formatSet(SetValues(weightKg = 7.0, reps = 10), ExerciseType.WEIGHT_REPS, levels).substringBefore(" ×"))
        // Levels are for single machines, not the whole app; distances stay metric.
        assertFalse(levels.isAppWide)
        assertEquals("km", levels.distanceUnit)
        // A drop set goes down whole levels.
        assertEquals(8.0, ExercisePlan(dropSets = true).nextDropKg(10.0, 20, levels), 1e-9)
    }

    @Test
    fun anExerciseCanUseItsOwnUnit() {
        val machine = Exercise(
            id = "m", name = "Chest Press Machine", categoryId = "c", type = ExerciseType.WEIGHT_REPS, notes = "",
            isCustom = false, weightUnits = UnitSystem.IMPERIAL,
        )
        assertEquals(UnitSystem.IMPERIAL, machine.unitsOr(UnitSystem.METRIC))
        assertEquals(UnitSystem.METRIC, machine.copy(weightUnits = null).unitsOr(UnitSystem.METRIC))
    }
}
