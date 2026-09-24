package com.kerimkolberg.fitnessapp.ui.log

import com.kerimkolberg.fitnessapp.model.ExerciseType
import com.kerimkolberg.fitnessapp.model.SetValues
import com.kerimkolberg.fitnessapp.model.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetInputTest {
    private fun SetInput.valid(type: ExerciseType, units: UnitSystem = UnitSystem.METRIC): SetValues {
        val result = toSetValues(type, units)
        assertTrue("Expected valid input but got $result", result is SetInput.Result.Valid)
        return (result as SetInput.Result.Valid).values
    }

    private fun SetInput.invalid(type: ExerciseType) {
        assertTrue(toSetValues(type, UnitSystem.METRIC) is SetInput.Result.Invalid)
    }

    @Test
    fun weightAndReps() {
        assertEquals(SetValues(weightKg = 100.0, reps = 5), SetInput(weight = "100", reps = "5").valid(ExerciseType.WEIGHT_REPS))
    }

    @Test
    fun blankWeightMeansBodyweight() {
        assertEquals(SetValues(weightKg = 0.0, reps = 8), SetInput(reps = "8").valid(ExerciseType.WEIGHT_REPS))
    }

    @Test
    fun repsAreRequired() {
        SetInput(weight = "100").invalid(ExerciseType.WEIGHT_REPS)
        SetInput(weight = "100", reps = "0").invalid(ExerciseType.WEIGHT_REPS)
        SetInput(reps = "abc").invalid(ExerciseType.REPS)
    }

    @Test
    fun poundsAreStoredAsKilograms() {
        val values = SetInput(weight = "225", reps = "5").valid(ExerciseType.WEIGHT_REPS, UnitSystem.IMPERIAL)
        assertEquals(102.058, values.weightKg!!, 0.001)
        assertEquals("225", SetInput.from(values, UnitSystem.IMPERIAL).weight)
    }

    @Test
    fun distanceAndTime() {
        val values = SetInput(distance = "5", minutes = "25", seconds = "30").valid(ExerciseType.DISTANCE_TIME)
        assertEquals(SetValues(distanceMeters = 5000.0, durationSeconds = 1530), values)
        assertEquals(SetValues(distanceMeters = 2500.0), SetInput(distance = "2,5").valid(ExerciseType.DISTANCE_TIME))
        SetInput().invalid(ExerciseType.DISTANCE_TIME)
    }

    @Test
    fun timeOnly() {
        assertEquals(SetValues(durationSeconds = 60), SetInput(minutes = "1").valid(ExerciseType.TIME))
        SetInput(minutes = "0", seconds = "0").invalid(ExerciseType.TIME)
    }

    @Test
    fun fromFillsFieldsInDisplayUnits() {
        val input = SetInput.from(SetValues(distanceMeters = 1609.344, durationSeconds = 605), UnitSystem.IMPERIAL)
        assertEquals("1", input.distance)
        assertEquals("10", input.minutes)
        assertEquals("5", input.seconds)
    }

    @Test
    fun steppersNeverGoBelowZero() {
        assertEquals("2.5", SetInput().adjustWeight(1, UnitSystem.METRIC).weight)
        assertEquals("105", SetInput(weight = "100").adjustWeight(1, UnitSystem.IMPERIAL).weight)
        assertEquals("0", SetInput(weight = "1").adjustWeight(-1, UnitSystem.METRIC).weight)
        assertEquals("6", SetInput(reps = "5").adjustReps(1).reps)
        assertEquals("0", SetInput().adjustReps(-1).reps)
    }

    @Test
    fun loadedHoldsNeedATimeButNotAWeight() {
        assertEquals(SetValues(durationSeconds = 45), SetInput(seconds = "45").valid(ExerciseType.TIME_WEIGHT))
        assertEquals(
            SetValues(weightKg = 20.0, durationSeconds = 45),
            SetInput(weight = "20", seconds = "45").valid(ExerciseType.TIME_WEIGHT),
        )
        SetInput(weight = "20").invalid(ExerciseType.TIME_WEIGHT)
    }

    @Test
    fun jumpsWithOptionalHeight() {
        assertEquals(SetValues(reps = 5, distanceMeters = 0.6), SetInput(reps = "5", height = "60").valid(ExerciseType.REPS_HEIGHT))
        assertEquals(SetValues(reps = 5), SetInput(reps = "5").valid(ExerciseType.REPS_HEIGHT))
        assertEquals("60", SetInput.from(SetValues(reps = 5, distanceMeters = 0.6), UnitSystem.METRIC).height)
    }

    @Test
    fun sportsSessions() {
        assertEquals(
            SetValues(durationSeconds = 5400, rpe = 7, note = "doubles"),
            SetInput(minutes = "90", rpe = "7", note = " doubles ").valid(ExerciseType.SESSION),
        )
        SetInput(minutes = "90", rpe = "11").invalid(ExerciseType.SESSION)
        SetInput(rpe = "5").invalid(ExerciseType.SESSION)
    }
}
