package com.kkfittracking.ui.exercises

import com.kkfittracking.model.Category
import com.kkfittracking.model.Exercise
import com.kkfittracking.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupExercisesTest {
    private val chest = Category("chest", "Chest", 0)
    private val legs = Category("legs", "Legs", 0)
    private val abs = Category("abs", "Abs", 0)

    private fun exercise(name: String, category: Category) =
        Exercise(id = name, name = name, categoryId = category.id, type = ExerciseType.WEIGHT_REPS, notes = "", isCustom = false)

    private val exercises = listOf(
        exercise("Flat Barbell Bench Press", chest),
        exercise("Incline Dumbbell Bench Press", chest),
        exercise("Barbell Squat", legs),
    )

    @Test
    fun groupsInCategoryOrderAndSkipsEmptyCategories() {
        val groups = groupExercises(listOf(legs, abs, chest), exercises, query = "", selectedCategoryId = null)
        assertEquals(listOf("legs", "chest"), groups.map { it.category.id })
    }

    @Test
    fun searchMatchesAllWordsInAnyOrder() {
        val groups = groupExercises(listOf(chest, legs), exercises, query = "press barbell", selectedCategoryId = null)
        assertEquals(listOf("Flat Barbell Bench Press"), groups.flatMap { it.exercises }.map { it.name })
    }

    @Test
    fun categoryFilter() {
        val groups = groupExercises(listOf(chest, legs), exercises, query = "barbell", selectedCategoryId = "legs")
        assertEquals(listOf("Barbell Squat"), groups.flatMap { it.exercises }.map { it.name })
    }
}
