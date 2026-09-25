package com.kerimkolberg.fitnessapp.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonTest {
    private val sample = BackupFile(
        formatVersion = BackupFile.CURRENT_FORMAT_VERSION,
        createdAt = 1_000,
        appVersion = "0.1.0",
        workouts = listOf(WorkoutDto("w1", "2026-09-25", "", 1, 1)),
        sets = listOf(
            SetDto("s1", "we1", 0, weightKg = 100.0, reps = 5, createdAt = 1, updatedAt = 1),
            SetDto("s2", "we1", 1, weightKg = 100.0, reps = 5, createdAt = 1, updatedAt = 2, deletedAt = 2),
        ),
    )

    @Test
    fun roundTrip() {
        assertEquals(sample, BackupJson.decode(BackupJson.encode(sample)))
    }

    @Test
    fun summaryCountsOnlyLiveRows() {
        assertEquals(1, sample.summary().sets)
        assertEquals(1, sample.summary().workouts)
    }

    @Test
    fun olderFilesWithoutNewFieldsStillLoad() {
        val old = """{"formatVersion":1,"createdAt":5,"sets":[{"id":"s","workoutExerciseId":"we","sortOrder":0,
            |"reps":8,"createdAt":1,"updatedAt":1}],"someFutureField":true}""".trimMargin()
        val file = BackupJson.decode(old)
        assertEquals(null, file.sets.single().rpe)
        assertEquals("", file.sets.single().comment)
    }

    @Test
    fun rejectsOtherFilesAndNewerVersions() {
        val notJson = assertThrows(BackupException::class.java) { BackupJson.decode("hello") }
        assertTrue(notJson.message!!.contains("not a Fitness App backup"))
        val newer = assertThrows(BackupException::class.java) {
            BackupJson.decode("""{"formatVersion":99,"createdAt":1}""")
        }
        assertTrue(newer.message!!.contains("newer version"))
    }
}
