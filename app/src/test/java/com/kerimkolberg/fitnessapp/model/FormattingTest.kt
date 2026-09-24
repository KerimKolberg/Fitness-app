package com.kerimkolberg.fitnessapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattingTest {
    @Test
    fun formatNumber_dropsTrailingZeros() {
        assertEquals("100", formatNumber(100.0))
        assertEquals("102.5", formatNumber(102.5))
        assertEquals("2.25", formatNumber(2.25))
        assertEquals("0", formatNumber(0.0))
    }

    @Test
    fun parseDecimal_acceptsCommaAndDot() {
        assertEquals(12.5, parseDecimal("12,5")!!, 0.0)
        assertEquals(12.5, parseDecimal(" 12.5 ")!!, 0.0)
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal(""))
    }

    @Test
    fun formatDuration_usesMinutesAndHours() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("1:05", formatDuration(65))
        assertEquals("1:02:05", formatDuration(3725))
    }

    @Test
    fun formatSet_weightAndReps() {
        val values = SetValues(weightKg = 100.0, reps = 5)
        assertEquals("100 kg × 5", formatSet(values, ExerciseType.WEIGHT_REPS, UnitSystem.METRIC))
        assertEquals("220.46 lb × 5", formatSet(values, ExerciseType.WEIGHT_REPS, UnitSystem.IMPERIAL))
    }

    @Test
    fun formatSet_otherTypes() {
        assertEquals("12 reps", formatSet(SetValues(reps = 12), ExerciseType.REPS, UnitSystem.METRIC))
        assertEquals("1 rep", formatSet(SetValues(reps = 1), ExerciseType.REPS, UnitSystem.METRIC))
        assertEquals(
            "5 km · 25:00",
            formatSet(SetValues(distanceMeters = 5000.0, durationSeconds = 1500), ExerciseType.DISTANCE_TIME, UnitSystem.METRIC),
        )
        assertEquals("1:30", formatSet(SetValues(durationSeconds = 90), ExerciseType.TIME, UnitSystem.METRIC))
    }

    @Test
    fun unitSystem_weightRoundTrips() {
        val kg = UnitSystem.IMPERIAL.weightToKg(225.0)
        assertEquals(102.058, kg, 0.001)
        assertEquals("225", formatNumber(UnitSystem.IMPERIAL.weightFromKg(kg)))
    }
}
