package com.kkfittracking.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AnalysisTest {
    private val today = LocalDate.of(2026, 9, 25)

    private fun exercise(id: String, type: ExerciseType = ExerciseType.WEIGHT_REPS) =
        Exercise(id = id, name = id, categoryId = "c", type = type, notes = "", isCustom = false)

    private fun set(id: String, kg: Double?, reps: Int? = null, seconds: Int? = null) =
        SetEntry(id, SetValues(weightKg = kg, reps = reps, durationSeconds = seconds))

    @Test
    fun graphsCanShowARecentPeriod() {
        val history = listOf(
            HistorySession(today.minusDays(3), emptyList()),
            HistorySession(today.minusMonths(2), emptyList()),
            HistorySession(today.minusYears(2), emptyList()),
        )
        assertEquals(1, history.within(TimeRange.MONTH, today).size)
        assertEquals(2, history.within(TimeRange.THREE_MONTHS, today).size)
        assertEquals(3, history.within(TimeRange.ALL, today).size)
    }

    @Test
    fun everyExercisesRecordNewestFirst() {
        val sets = listOf(
            Triple(today.minusDays(10), "bench", set("a", 100.0, 5)),
            Triple(today.minusDays(2), "bench", set("b", 90.0, 5)),
            Triple(today.minusDays(1), "plank", set("c", null, seconds = 90)),
        )
        val histories = historiesOf(sets)
        assertEquals(listOf(today.minusDays(2), today.minusDays(10)), histories.getValue("bench").map { it.date })

        val records = allRecords(
            histories,
            listOf(exercise("bench"), exercise("plank", ExerciseType.TIME), exercise("curl")),
            UnitSystem.METRIC,
        )
        // The plank's record is the newest; the curl was never logged.
        assertEquals(listOf("plank", "bench"), records.map { it.exercise.id })
        assertEquals("Estimated 1RM", records.last().record.label)
        assertEquals(today.minusDays(10), records.last().record.date)
        assertEquals(2, records.last().sessions)
    }
}
