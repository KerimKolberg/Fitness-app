package com.kkfittracking.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressTest {
    private val day1 = LocalDate.of(2026, 9, 1)
    private val day2 = LocalDate.of(2026, 9, 3)
    private val day3 = LocalDate.of(2026, 9, 5)

    private fun set(id: String, kg: Double? = null, reps: Int? = null, meters: Double? = null, seconds: Int? = null) =
        SetEntry(id, SetValues(weightKg = kg, reps = reps, distanceMeters = meters, durationSeconds = seconds))

    // Newest first, like the repository returns it.
    private val bench = listOf(
        HistorySession(day3, listOf(set("c1", 100.0, 5), set("c2", 105.0, 3))),
        HistorySession(day2, listOf(set("b1", 100.0, 5))),
        HistorySession(day1, listOf(set("a1", 90.0, 5), set("a2", 95.0, 5))),
    )

    @Test
    fun epley() {
        assertEquals(100.0, estimatedOneRepMax(100.0, 1), 1e-9)
        assertEquals(116.667, estimatedOneRepMax(100.0, 5), 0.001)
        assertEquals(0.0, estimatedOneRepMax(0.0, 10), 1e-9)
        assertEquals(0.0, estimatedOneRepMax(100.0, 0), 1e-9)
    }

    @Test
    fun recordSetsBeatEveryEarlierSet() {
        // a1, a2 raise the best; b1 (100x5) beats a2; c1 only ties b1; c2 (105x3 = 115.5) is below 116.7.
        assertEquals(setOf("a1", "a2", "b1"), personalRecordSetIds(bench, ExerciseType.WEIGHT_REPS))
    }

    @Test
    fun recordsForOtherTypes() {
        val plank = listOf(
            HistorySession(day1, listOf(set("a", seconds = 60), set("b", seconds = 45))),
            HistorySession(day2, listOf(set("c", seconds = 75))),
        )
        assertEquals(setOf("a", "c"), personalRecordSetIds(plank, ExerciseType.TIME))
    }

    @Test
    fun personalRecordSummary() {
        val records = personalRecords(bench, ExerciseType.WEIGHT_REPS, UnitSystem.METRIC).associateBy { it.label }
        assertEquals("116.67 kg", records.getValue("Estimated 1RM").value)
        assertEquals(day2, records.getValue("Estimated 1RM").date)
        assertEquals("105 kg × 3", records.getValue("Heaviest weight").value)
        assertEquals("925 kg", records.getValue("Best session volume").value)
        assertEquals(day1, records.getValue("Best session volume").date)
    }

    @Test
    fun repMaxesCountHeavierSetsForFewerReps() {
        val maxes = repMaxes(bench)
        assertEquals(105.0, maxes.getValue(1), 0.0)
        assertEquals(105.0, maxes.getValue(3), 0.0)
        assertEquals(100.0, maxes.getValue(4), 0.0)
        assertEquals(100.0, maxes.getValue(5), 0.0)
        assertTrue(6 !in maxes)
    }

    @Test
    fun progressPointsAreOldestFirst() {
        val points = progressPoints(bench, ProgressMetric.MAX_WEIGHT)
        assertEquals(listOf(day1, day2, day3), points.map { it.date })
        assertEquals(listOf(95.0, 100.0, 105.0), points.map { it.value })
        assertEquals(listOf(925.0, 500.0, 815.0), progressPoints(bench, ProgressMetric.VOLUME).map { it.value })
    }

    @Test
    fun metricFormatting() {
        assertEquals("220.46 lb", ProgressMetric.ESTIMATED_1RM.format(100.0, UnitSystem.IMPERIAL))
        assertEquals("1:30", ProgressMetric.DURATION.format(90.0, UnitSystem.METRIC))
    }

    @Test
    fun bodyMetricUnits() {
        assertEquals("80 kg", BodyMetric.BODYWEIGHT.format(80.0, UnitSystem.METRIC))
        assertEquals("33.07 in", BodyMetric.WAIST.format(84.0, UnitSystem.IMPERIAL))
        assertEquals("15 %", BodyMetric.BODY_FAT.format(15.0, UnitSystem.IMPERIAL))
        assertEquals(84.0, BodyMetric.WAIST.fromDisplay(BodyMetric.WAIST.toDisplay(84.0, UnitSystem.IMPERIAL), UnitSystem.IMPERIAL), 1e-9)
    }
}
