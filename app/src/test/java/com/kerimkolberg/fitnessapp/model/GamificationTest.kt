package com.kerimkolberg.fitnessapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GamificationTest {
    // Monday 7 September 2026.
    private val monday = LocalDate.of(2026, 9, 7)
    private var nextId = 0

    private fun set(
        date: LocalDate,
        exercise: String = "bench",
        kg: Double = 100.0,
        reps: Int = 5,
        categoryKey: String? = "chest",
        type: ExerciseType = ExerciseType.WEIGHT_REPS,
    ) = LoggedSet(
        id = "s${nextId++}",
        date = date,
        exerciseId = exercise,
        categoryKey = categoryKey,
        categoryId = categoryKey ?: "custom",
        type = type,
        values = SetValues(weightKg = kg, reps = reps),
    )

    private fun GameStats.status(achievement: Achievement) = achievements.single { it.achievement == achievement }

    @Test
    fun levelsGetLongerEachTime() {
        assertEquals(Triple(1, 0, 100), levelFor(0))
        assertEquals(Triple(1, 99, 100), levelFor(99))
        assertEquals(Triple(2, 0, 150), levelFor(100))
        assertEquals(Triple(3, 10, 200), levelFor(260))
    }

    @Test
    fun emptyLogStartsAtLevelOne() {
        val stats = computeGameStats(emptyList(), weeklyGoal = 3, today = monday)
        assertEquals(1, stats.level)
        assertEquals(0, stats.xp)
        assertTrue(stats.achievements.none { it.isUnlocked })
    }

    @Test
    fun xpForAFirstWorkout() {
        // One workout, two sets, one new exercise; the first set is a record, the second is not.
        val stats = computeGameStats(
            listOf(set(monday), set(monday, kg = 90.0)),
            weeklyGoal = 3,
            today = monday,
        )
        assertEquals(25 + 2 * 10 + 50 + 15, stats.xp)
        assertEquals(1, stats.totalRecords)
        assertEquals(monday, stats.status(Achievement.FIRST_WORKOUT).unlockedOn)
        assertEquals(monday, stats.status(Achievement.FIRST_RECORD).unlockedOn)
        assertEquals(1, stats.status(Achievement.WORKOUTS_10).progress)
        assertNull(stats.status(Achievement.WORKOUTS_10).unlockedOn)
    }

    @Test
    fun weeklyStreaks() {
        // Goal of 2 workouts per week, reached in three weeks in a row and started in the fourth.
        val sets = (0L..2L).flatMap { week ->
            listOf(set(monday.plusWeeks(week)), set(monday.plusWeeks(week).plusDays(2)))
        } + set(monday.plusWeeks(3))
        val midFourthWeek = monday.plusWeeks(3).plusDays(3)
        val stats = computeGameStats(sets, weeklyGoal = 2, today = midFourthWeek)
        assertEquals(3, stats.weekStreak)
        assertEquals(1, stats.workoutsThisWeek)
        assertEquals(3, stats.status(Achievement.STREAK_4).progress)

        // A week without reaching the goal breaks it.
        val later = computeGameStats(sets, weeklyGoal = 2, today = monday.plusWeeks(5))
        assertEquals(0, later.weekStreak)
    }

    @Test
    fun varietyAchievements() {
        val sets = (0L until 10L).map { day ->
            set(monday.plusDays(day), exercise = "hip-cars", categoryKey = "mobility", type = ExerciseType.REPS)
        }
        val stats = computeGameStats(sets, weeklyGoal = 3, today = monday.plusDays(10))
        assertEquals(monday.plusDays(9), stats.status(Achievement.MOBILITY_10).unlockedOn)
        assertEquals(0, stats.status(Achievement.SPORTS_10).progress)
    }

    @Test
    fun heavyDay() {
        val sets = List(20) { set(monday, kg = 100.0, reps = 5) }
        val stats = computeGameStats(sets, weeklyGoal = 3, today = monday)
        assertEquals(monday, stats.status(Achievement.TON_DAY).unlockedOn)
    }

    @Test
    fun celebrations() {
        val before = computeGameStats(emptyList(), 3, monday)
        val after = computeGameStats(listOf(set(monday)), 3, monday)
        val messages = celebrationsBetween(before, after)
        assertTrue(messages.any { "First step" in it })
        assertTrue(messages.any { "Personal best" in it })
        assertTrue(celebrationsBetween(after, after).isEmpty())
    }

    @Test
    fun newTypesFormatAndScore() {
        val hold = SetValues(weightKg = 20.0, durationSeconds = 45)
        assertEquals("20 kg · 0:45", formatSet(hold, ExerciseType.TIME_WEIGHT, UnitSystem.METRIC))
        assertEquals("0:45", formatSet(SetValues(durationSeconds = 45), ExerciseType.TIME_WEIGHT, UnitSystem.METRIC))
        assertEquals("8 reps · 60 cm", formatSet(SetValues(reps = 8, distanceMeters = 0.6), ExerciseType.REPS_HEIGHT, UnitSystem.METRIC))
        assertEquals(
            "1:00:00 · RPE 7 · doubles",
            formatSet(SetValues(durationSeconds = 3600, rpe = 7, note = "doubles"), ExerciseType.SESSION, UnitSystem.METRIC),
        )
        assertNull(recordScore(SetValues(durationSeconds = 3600), ExerciseType.SESSION))
        assertEquals(45.0, recordScore(hold, ExerciseType.TIME_WEIGHT)!!, 0.0)
    }
}
