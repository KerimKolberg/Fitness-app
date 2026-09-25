package com.kkfittracking.ui.log

import com.kkfittracking.model.ExercisePlan
import com.kkfittracking.model.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetPlanFormTest {
    private val metric = UnitSystem.METRIC

    private fun SetPlanForm.plan(base: ExercisePlan = ExercisePlan(), units: UnitSystem = metric): ExercisePlan =
        (toPlan(base, units) as SetPlanForm.Result.Valid).plan

    @Test
    fun anEmptyPlanStartsFromTheDefaults() {
        val form = SetPlanForm.from(ExercisePlan(), metric, defaultPercent = 25)
        assertEquals(3, form.sets)
        assertEquals("", form.reps)
        assertNull(form.restSeconds)
        assertEquals(25, form.dropPercent)
        assertEquals("5", form.dropAmount)
        assertEquals("10", SetPlanForm.from(ExercisePlan(), UnitSystem.IMPERIAL, 20).dropAmount)
    }

    @Test
    fun savesWhatWasTyped() {
        val base = ExercisePlan(highSeconds = 20, rounds = 8)
        val plan = SetPlanForm(sets = 4, reps = "8", weight = "82,5", restSeconds = 120).plan(base)
        assertEquals(ExercisePlan(sets = 4, reps = 8, weightKg = 82.5, restSeconds = 120, dropPercent = 20, highSeconds = 20, rounds = 8), plan)
        // The form round-trips.
        assertEquals(plan, SetPlanForm.from(plan, metric, 20).copy(dropAmount = "").plan(base))
    }

    @Test
    fun rejectsBadNumbers() {
        assertTrue(SetPlanForm(reps = "eight").toPlan(ExercisePlan(), metric) is SetPlanForm.Result.Invalid)
        assertTrue(SetPlanForm(reps = "0").toPlan(ExercisePlan(), metric) is SetPlanForm.Result.Invalid)
        assertTrue(SetPlanForm(weight = "-5").toPlan(ExercisePlan(), metric) is SetPlanForm.Result.Invalid)
        assertTrue(SetPlanForm(dropReps = "1.5").toPlan(ExercisePlan(), metric) is SetPlanForm.Result.Invalid)
        assertTrue(SetPlanForm(dropAmount = "0").toPlan(ExercisePlan(), metric) is SetPlanForm.Result.Invalid)
    }

    @Test
    fun dropSetsOnTheLastSetOrTheWholeExercise() {
        val form = SetPlanForm(sets = 4, dropSets = true)
        assertEquals(1, form.dropOnWholeExercise(true).sets)
        assertTrue(form.dropOnWholeExercise(true).isWholeExercise)
        assertEquals(4, form.dropOnWholeExercise(false).sets)
        assertEquals(3, form.dropOnWholeExercise(true).dropOnWholeExercise(false).sets)

        val plan = form.copy(drops = 3, dropReps = "12", dropByPercent = false, dropAmount = "10").plan()
        assertEquals(3, plan.drops)
        assertEquals(12, plan.dropReps)
        assertEquals(10.0, plan.dropAmountKg!!, 1e-9)
    }

    @Test
    fun steppersStayInRange() {
        assertEquals(105, SetPlanForm().changeRest(15, defaultSeconds = 90).restSeconds)
        assertEquals(15, SetPlanForm(restSeconds = 20).changeRest(-15, 90).restSeconds)
        assertEquals(1, SetPlanForm(sets = 1).changeSets(-1).sets)
        assertEquals(ExercisePlan.MAX_DROPS, SetPlanForm(drops = ExercisePlan.MAX_DROPS).changeDrops(1).drops)
        assertEquals(50, SetPlanForm(dropPercent = 50).changeDropPercent(5).dropPercent)
    }

    @Test
    fun poundsAreStoredAsKilograms() {
        val plan = SetPlanForm(weight = "225", dropByPercent = false, dropAmount = "10").plan(units = UnitSystem.IMPERIAL)
        assertEquals(225.0, UnitSystem.IMPERIAL.weightFromKg(plan.weightKg!!), 1e-9)
        assertEquals(10.0, UnitSystem.IMPERIAL.weightFromKg(plan.dropAmountKg!!), 1e-9)
    }
}
