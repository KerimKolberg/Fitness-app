package com.kerimkolberg.fitnessapp.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CsvExportTest {
    private val day = LocalDate.of(2026, 9, 25)

    @Test
    fun workoutsOneRowPerSetInTheUsersUnits() {
        val csv = CsvExport.workouts(
            listOf(
                ExportSet(day, "Flat Barbell Bench Press", "Chest", ExerciseType.WEIGHT_REPS, SetValues(100.0, 5)),
                ExportSet(day, "Flat Barbell Bench Press", "Chest", ExerciseType.WEIGHT_REPS, SetValues(100.0, 4)),
                ExportSet(day, "Box Jump", "Plyometrics", ExerciseType.REPS_HEIGHT, SetValues(reps = 5, distanceMeters = 0.6)),
                ExportSet(
                    day, "Tennis", "Sports", ExerciseType.SESSION,
                    SetValues(durationSeconds = 3600, rpe = 7, note = "doubles, won 6-4"),
                ),
            ),
            UnitSystem.METRIC,
        )
        val lines = csv.trimEnd().split("\r\n")
        assertEquals("Date,Exercise,Category,Set,Weight (kg),Reps,Distance (km),Height (cm),Time (s),RPE,Note,Drop set", lines[0])
        assertEquals("2026-09-25,Flat Barbell Bench Press,Chest,1,100,5,,,,,,", lines[1])
        assertEquals("2026-09-25,Flat Barbell Bench Press,Chest,2,100,4,,,,,,", lines[2])
        assertEquals("2026-09-25,Box Jump,Plyometrics,1,,5,,60,,,,", lines[3])
        assertEquals("2026-09-25,Tennis,Sports,1,,,,,3600,7,\"doubles, won 6-4\",", lines[4])
    }

    @Test
    fun poundsWhenImperial() {
        val csv = CsvExport.workouts(
            listOf(ExportSet(day, "Squat", "Legs", ExerciseType.WEIGHT_REPS, SetValues(100.0, 5))),
            UnitSystem.IMPERIAL,
        )
        assertEquals("2026-09-25,Squat,Legs,1,220.46,5,,,,,,", csv.trimEnd().split("\r\n")[1])
    }

    @Test
    fun quotesAreEscaped() {
        assertEquals("\"say \"\"hi\"\"\",plain\r\n", CsvExport.toCsv(listOf(listOf("say \"hi\"", "plain"))))
    }

    @Test
    fun bodyMeasurements() {
        val csv = CsvExport.bodyMeasurements(
            listOf(
                BodyMeasurement("2", day, BodyMetric.WAIST, 84.0),
                BodyMeasurement("1", day.minusDays(7), BodyMetric.BODYWEIGHT, 80.0),
            ),
            UnitSystem.METRIC,
        )
        assertEquals("Date,Measurement,Value,Unit\r\n2026-09-18,Bodyweight,80,kg\r\n2026-09-25,Waist,84,cm\r\n", csv)
    }

    @Test
    fun dropSetsShareTheSetNumber() {
        val csv = CsvExport.workouts(
            listOf(
                ExportSet(day, "Curl", "Biceps", ExerciseType.WEIGHT_REPS, SetValues(20.0, 8)),
                ExportSet(day, "Curl", "Biceps", ExerciseType.WEIGHT_REPS, SetValues(16.0, 6, isDropSet = true)),
                ExportSet(day, "Curl", "Biceps", ExerciseType.WEIGHT_REPS, SetValues(20.0, 8)),
            ),
            UnitSystem.METRIC,
        )
        val lines = csv.trimEnd().split("\r\n")
        assertEquals("2026-09-25,Curl,Biceps,1,16,6,,,,,,yes", lines[2])
        assertEquals("2026-09-25,Curl,Biceps,2,20,8,,,,,,", lines[3])
    }
}
