package com.kkfittracking.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayPlanTest {
    private fun sets(count: Int, drops: Int = 0) =
        List(count) { SetEntry("s$it", SetValues(100.0, 8)) } +
            List(drops) { SetEntry("d$it", SetValues(80.0, 8, isDropSet = true)) }

    private fun exercise(
        id: String,
        plan: ExercisePlan = ExercisePlan(),
        done: Int = 0,
        drops: Int = 0,
        type: ExerciseType = ExerciseType.WEIGHT_REPS,
        superset: String? = null,
        rounds: Int? = null,
        memberRounds: Int? = null,
    ) = DayExercise(
        workoutExerciseId = "we-$id", exerciseId = id, exerciseName = id, exerciseType = type, categoryColor = 0,
        sets = if (type.isSession) sets(done).map { SetEntry(it.id, SetValues(durationSeconds = 3600)) } else sets(done, drops),
        supersetId = superset, supersetRounds = rounds, memberRounds = memberRounds, plan = plan,
    )

    @Test
    fun theGuideGoesThroughTheDayInOrder() {
        val day = listOf(
            exercise("bench", ExercisePlan(sets = 3)),
            exercise("curl"),
            exercise("tennis", type = ExerciseType.SESSION),
        )
        assertEquals(GuideTarget("bench", "bench", "Set 1 of 3", 1, 3), guideTarget(day))
        val benchDone = day.mapIndexed { i, e -> if (i == 0) exercise("bench", ExercisePlan(sets = 3), done = 3) else e }
        // Without a set plan, the guide expects three sets.
        assertEquals("curl" to "Set 1 of 3", guideTarget(benchDone)!!.let { it.exerciseId to it.step })
        assertEquals("tennis" to "Session", guideTarget(benchDone, skipped = setOf("curl"))!!.let { it.exerciseId to it.step })
        val allDone = listOf(
            exercise("bench", ExercisePlan(sets = 3), done = 3),
            exercise("curl", done = 4),
            exercise("tennis", type = ExerciseType.SESSION, done = 1),
        )
        assertNull(guideTarget(allDone))
    }

    @Test
    fun aDueDropSetComesRightAfterItsSet() {
        val plan = ExercisePlan(sets = 2, dropSets = true, drops = 2)
        val target = guideTarget(listOf(exercise("bench", plan, done = 2), exercise("row")))!!
        assertEquals("bench", target.exerciseId)
        assertEquals("Drop 1 of 2", target.step)
        assertTrue(target.isDrop)
        assertEquals("row", guideTarget(listOf(exercise("bench", plan, done = 2, drops = 2), exercise("row")))?.exerciseId)
    }

    @Test
    fun aSupersetGoesRoundByRound() {
        // Bench and row for 3 rounds; curl joins the last round only.
        fun day(bench: Int, row: Int, curl: Int) = listOf(
            exercise("bench", done = bench, superset = "s", rounds = 3),
            exercise("row", done = row, superset = "s", rounds = 3),
            exercise("curl", done = curl, superset = "s", rounds = 3, memberRounds = 1),
            exercise("plank", ExercisePlan(sets = 1)),
        )
        fun next(bench: Int, row: Int, curl: Int) = guideTarget(day(bench, row, curl))?.let { "${it.exerciseId} ${it.step}" }
        assertEquals("bench Round 1 of 3", next(0, 0, 0))
        assertEquals("row Round 1 of 3", next(1, 0, 0))
        assertEquals("bench Round 2 of 3", next(1, 1, 0))
        assertEquals("row Round 3 of 3", next(3, 2, 0))
        assertEquals("curl Round 3 of 3", next(3, 3, 0))
        assertEquals("plank Set 1 of 1", next(3, 3, 1))
    }

    @Test
    fun howMuchOfTheDayIsDone() {
        val day = listOf(
            exercise("bench", ExercisePlan(sets = 3, dropSets = true, drops = 1), done = 3, drops = 1),
            exercise("row", ExercisePlan(sets = 4), done = 2),
            exercise("curl", ExercisePlan(sets = 3)),
            exercise("tennis", type = ExerciseType.SESSION),
        )
        val completion = dayCompletion(day)
        // Bench 4/4, row 2/4, curl 0/3, tennis 0/1: 6 of 12.
        assertEquals(50, completion.percent)
        assertEquals(listOf("bench"), completion.finished.map { it.exerciseId })
        assertEquals(listOf("row"), completion.partly.map { it.exerciseId })
        assertEquals(listOf("curl", "tennis"), completion.notStarted.map { it.exerciseId })
        assertEquals("3 of 3 sets · 1 of 1 drops", completion.finished.single().label)
        assertEquals("Session not done", completion.notStarted.last().label)
        // Extra sets do not count past the plan.
        assertEquals(100, dayCompletion(listOf(exercise("row", ExercisePlan(sets = 2), done = 5))).percent)
    }

    @Test
    fun supersetRoundsAreThePlan() {
        val day = listOf(
            exercise("bench", ExercisePlan(sets = 5), done = 3, superset = "s", rounds = 3),
            exercise("curl", done = 1, superset = "s", rounds = 3, memberRounds = 1),
        )
        val completion = dayCompletion(day)
        assertEquals(listOf(3, 1), completion.exercises.map { it.plannedSets })
        assertEquals(100, completion.percent)
    }

    @Test
    fun pausesDoNotCountAsTrainingTime() {
        var session = GuideSession(epochDay = 0, startedAtMillis = 0)
        session = session.pause(60_000)
        assertTrue(session.isPaused)
        assertEquals(60_000, session.activeMillis(600_000))
        // Pausing twice keeps the first pause.
        assertEquals(session, session.pause(90_000))
        session = session.resume(300_000)
        assertEquals(120_000, session.activeMillis(360_000))
        assertEquals(setOf("curl"), session.skip("curl").skipped)
    }

    @Test
    fun theGuideSuggestsWhatToLift() {
        val settings = Settings(dropSetPercent = 20)
        fun target(day: List<DayExercise>) = guideTarget(day, settings = settings)!!
        // Nothing today: the plan's reps over last session's weight.
        val fresh = listOf(exercise("bench", ExercisePlan(sets = 3, reps = 10)))
        val last = listOf(SetEntry("x", SetValues(90.0, 6)))
        assertEquals(SetValues(weightKg = 90.0, reps = 10), guideSuggestion(fresh, target(fresh), settings, last))
        // After a set today: the same again.
        val started = listOf(exercise("bench", ExercisePlan(sets = 3), done = 1))
        assertEquals(SetValues(100.0, 8), guideSuggestion(started, target(started), settings, last))
        // A due drop set: 20% lighter.
        val dropping = listOf(exercise("bench", ExercisePlan(sets = 2, dropSets = true), done = 2))
        assertEquals(SetValues(weightKg = 80.0, reps = 8, isDropSet = true), guideSuggestion(dropping, target(dropping), settings))
    }

    @Test
    fun theUnfinishedPartMovesToAnotherDay() {
        val day = listOf(
            exercise("bench", ExercisePlan(sets = 3), done = 3),
            exercise("row", ExercisePlan(sets = 4), done = 2),
            exercise("curl", ExercisePlan(sets = 3)),
            // A 3-round superset: 1 round done, 2 left.
            exercise("squat", done = 1, superset = "s", rounds = 3),
            exercise("lunge", done = 1, superset = "s", rounds = 3),
        )
        val move = unfinishedPart(day)
        assertEquals(listOf("row", "curl", "squat", "lunge"), move.toAdd.map { it.exerciseId })
        // Only the exercise not started leaves the day; the row keeps its two sets here.
        assertEquals(listOf("we-curl"), move.toRemove)
        assertEquals(listOf(2, 2), move.toAdd.filter { it.supersetId == "s" }.map { it.supersetRounds })
        assertTrue(unfinishedPart(listOf(exercise("bench", ExercisePlan(sets = 1), done = 1))).toAdd.isEmpty())
    }
}
