package com.kerimkolberg.fitnessapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BuiltInExercisesTest {
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
