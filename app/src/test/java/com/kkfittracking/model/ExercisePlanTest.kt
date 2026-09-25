package com.kkfittracking.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExercisePlanTest {
    private val settings = Settings(restTimerSeconds = 90, dropSetPercent = 20, supersetTransitionSeconds = 15)
    private val metric = UnitSystem.METRIC

    private fun sets(vararg drops: Boolean, kg: Double = 100.0, reps: Int = 8) =
        drops.mapIndexed { index, drop -> SetEntry("s$index", SetValues(weightKg = kg, reps = reps, isDropSet = drop)) }

    @Test
    fun jsonRoundTripAndEmptyPlans() {
        val plan = ExercisePlan(sets = 3, reps = 8, weightKg = 80.0, restSeconds = 120, dropSets = true, dropPercent = 25)
        assertEquals(plan, ExercisePlan.fromJson(plan.toJson()))
        assertEquals("", ExercisePlan().toJson())
        assertEquals(ExercisePlan(), ExercisePlan.fromJson(""))
        // Only what differs from the defaults is stored.
        assertEquals("""{"sets":3}""", ExercisePlan(sets = 3).toJson())
    }

    @Test
    fun damagedOrNewerJsonStillLoads() {
        assertEquals(ExercisePlan(), ExercisePlan.fromJson("{not json"))
        assertEquals(ExercisePlan(sets = 4), ExercisePlan.fromJson("""{"sets":4,"somethingNew":true}"""))
    }

    @Test
    fun dropsByPercentOrByWeight() {
        val byPercent = ExercisePlan(dropSets = true, drops = 3, dropPercent = 20)
        assertEquals(listOf(100.0, 80.0, 64.0, 51.0), byPercent.dropWeights(100.0, 20, metric))
        assertEquals("100 → 80 → 64 → 51 kg", formatWeightSteps(byPercent.dropWeights(100.0, 20, metric), metric))
        // No percentage chosen: the default from the settings.
        assertEquals(75.0, ExercisePlan(dropSets = true).nextDropKg(100.0, 25, metric), 1e-9)

        val byWeight = ExercisePlan(dropSets = true, dropByPercent = false, dropAmountKg = 10.0)
        assertEquals(listOf(100.0, 90.0, 80.0), byWeight.dropWeights(100.0, 20, metric))
        assertEquals("−10 kg", byWeight.dropStepLabel(20, metric))
        // Never below zero.
        assertEquals(0.0, byWeight.nextDropKg(5.0, 20, metric), 1e-9)
    }

    @Test
    fun dropsInPounds() {
        val imperial = UnitSystem.IMPERIAL
        val byWeight = ExercisePlan(dropSets = true, dropByPercent = false)
        // The default fixed drop is 10 lb.
        val next = byWeight.nextDropKg(imperial.weightToKg(225.0), 20, imperial)
        assertEquals(215.0, imperial.weightFromKg(next), 1e-9)
        assertEquals("−10 lb", byWeight.dropStepLabel(20, imperial))
    }

    @Test
    fun dropSetOnTheLastSet() {
        val plan = ExercisePlan(sets = 3, dropSets = true, drops = 2, dropReps = 10)
        fun due(vararg drops: Boolean) = pendingDrop(plan, ExerciseType.WEIGHT_REPS, sets(*drops), 20, metric)

        assertNull(due(false, false))
        assertEquals(NextStep.DropSet(1, 2, 80.0, 10), due(false, false, false))
        assertEquals(2, due(false, false, false, true)?.number)
        assertNull(due(false, false, false, true, true))
        // Another normal set after the drops does not start them again.
        assertNull(due(false, false, false, true, true, false))
        // Only weight and reps exercises have drop sets.
        assertNull(pendingDrop(plan, ExerciseType.REPS, sets(false, false, false), 20, metric))
    }

    @Test
    fun theWholeExerciseAsADropSet() {
        // One set, then drops: reps default to the set before.
        val plan = ExercisePlan(sets = 1, dropSets = true, drops = 3)
        assertTrue(plan.isWholeExerciseDropSet)
        val first = pendingDrop(plan, ExerciseType.WEIGHT_REPS, sets(false, reps = 12), 20, metric)
        assertEquals(NextStep.DropSet(1, 3, 80.0, 12), first)
        // The next drop is 20% lighter than the drop before, not than the first set.
        val second = pendingDrop(
            plan,
            ExerciseType.WEIGHT_REPS,
            listOf(SetEntry("a", SetValues(100.0, 12)), SetEntry("b", SetValues(80.0, 12, isDropSet = true))),
            20,
            metric,
        )
        assertEquals(64.0, second!!.weightKg, 1e-9)
    }

    @Test
    fun restComesFromThePlanOrTheSettings() {
        val plan = ExercisePlan(sets = 3, restSeconds = 150)
        assertEquals(NextStep.Rest(150, null), nextStep("bench", ExerciseType.WEIGHT_REPS, plan, sets(false), null, settings))
        assertEquals(NextStep.Rest(90, null), nextStep("bench", ExerciseType.WEIGHT_REPS, ExercisePlan(), sets(false), null, settings))
        // Sports and HIIT sessions are whole workouts.
        assertEquals(NextStep.Done, nextStep("tabata", ExerciseType.INTERVALS, plan, sets(false), null, settings))
    }

    @Test
    fun aSupersetsTimingReplacesTheExercisesOwnRest() {
        val plan = ExercisePlan(sets = 3, restSeconds = 150)
        val superset = SupersetContext(listOf("bench", "row", "curl"), transitionSeconds = 20, roundRestSeconds = 120)
        assertEquals(NextStep.Transition("row", 20), nextStep("bench", ExerciseType.WEIGHT_REPS, plan, sets(false), superset, settings))
        assertEquals(NextStep.Rest(120, "bench"), nextStep("curl", ExerciseType.WEIGHT_REPS, plan, sets(false), superset, settings))

        // Without timing of its own, the superset uses the settings.
        val untimed = superset.copy(transitionSeconds = null, roundRestSeconds = null)
        assertEquals(NextStep.Transition("curl", 15), nextStep("row", ExerciseType.WEIGHT_REPS, plan, sets(false), untimed, settings))
        assertEquals(NextStep.Rest(90, "bench"), nextStep("curl", ExerciseType.WEIGHT_REPS, plan, sets(false), untimed, settings))
    }

    @Test
    fun dropSetsComeBeforeMovingOnInASuperset() {
        val plan = ExercisePlan(sets = 2, dropSets = true, drops = 1)
        val superset = SupersetContext(listOf("bench", "row"), 20, 120)
        assertTrue(nextStep("bench", ExerciseType.WEIGHT_REPS, plan, sets(false, false), superset, settings) is NextStep.DropSet)
        assertEquals(
            NextStep.Transition("row", 20),
            nextStep("bench", ExerciseType.WEIGHT_REPS, plan, sets(false, false, true), superset, settings),
        )
    }

    @Test
    fun progressThroughThePlan() {
        val plan = ExercisePlan(sets = 3, dropSets = true, drops = 2)
        assertNull(planProgress(ExercisePlan(), sets(false), null))
        assertEquals("Set 1 of 3", planProgress(plan, emptyList(), null))
        assertEquals("Set 3 of 3", planProgress(plan, sets(false, false), null))
        val drop = pendingDrop(plan, ExerciseType.WEIGHT_REPS, sets(false, false, false), 20, metric)
        assertEquals("Drop 1 of 2, no rest", planProgress(plan, sets(false, false, false), drop))
        assertEquals("3 of 3 sets done ✓", planProgress(plan, sets(false, false, false, true, true), null))
    }

    @Test
    fun summaries() {
        val plan = ExercisePlan(sets = 3, reps = 8, weightKg = 80.0, restSeconds = 120, dropSets = true, drops = 2)
        assertEquals(
            "3 × 8 · 80 kg · rest 2:00 · drop set on the last set: 2 drops, −20% each",
            plan.summary(ExerciseType.WEIGHT_REPS, 20, metric),
        )
        assertEquals(
            "1 set · the whole exercise is a drop set: 1 drop, −5 kg each",
            ExercisePlan(sets = 1, dropSets = true, drops = 1, dropByPercent = false).summary(ExerciseType.WEIGHT_REPS, 20, metric),
        )
        assertEquals("3 sets · rest 1:00", ExercisePlan(sets = 3, restSeconds = 60).summary(ExerciseType.TIME, 20, metric))
        assertNull(ExercisePlan(highSeconds = 20).summary(ExerciseType.INTERVALS, 20, metric))
    }

    @Test
    fun intervalDefaults() {
        assertEquals(30, ExercisePlan().intervalHighSeconds)
        assertEquals(10, ExercisePlan(lowSeconds = 10).intervalLowSeconds)
        assertEquals(8, ExercisePlan(rounds = 8).intervalRounds)
        assertEquals(ExercisePlan(highSeconds = 20), ExercisePlan(sets = 3, highSeconds = 20, dropSets = true).withoutSetPlan())
    }
}
