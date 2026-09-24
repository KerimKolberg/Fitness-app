package com.kerimkolberg.fitnessapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInExercisesTest {
    @Test
    fun starterPlansOnlyUseBuiltInExercises() {
        val ids = BuiltInExercises.categories.flatMap { it.exercises }.map { it.id }.toSet()
        StarterPlans.plans.forEach { (plan, names) ->
            names.forEach { name ->
                assertTrue("$name in $plan is not a built-in exercise", StarterPlans.exerciseId(name) in ids)
            }
        }
    }

    @Test
    fun categoryKeysUsedByAchievementsExist() {
        val keys = BuiltInExercises.categories.map { it.key }
        listOf("mobility", "stretching", "tendons", "isometrics", "plyometrics", "sports").forEach {
            assertTrue(it in keys)
        }
    }

    @Test
    fun idsAreUniqueAndStable() {
        val categoryIds = BuiltInExercises.categories.map { it.id }
        val exerciseIds = BuiltInExercises.categories.flatMap { it.exercises }.map { it.id }
        assertEquals(categoryIds.size, categoryIds.toSet().size)
        assertEquals(exerciseIds.size, exerciseIds.toSet().size)

        // These ids are stored on users' devices, so they must never change between versions.
        assertEquals(
            "7211e2e6-ffde-3a55-96dc-bb98b77057c6",
            BuiltInExercises.categories.single { it.key == "chest" }.exercises.single { it.name == "Flat Barbell Bench Press" }.id,
        )
    }
}
