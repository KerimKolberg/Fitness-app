package com.kerimkolberg.fitnessapp.ui.exercises

import com.kerimkolberg.fitnessapp.model.Category
import com.kerimkolberg.fitnessapp.model.Exercise

data class ExerciseGroup(val category: Category, val exercises: List<Exercise>)

/** Groups exercises by category, in category order, keeping only those matching the filters. */
fun groupExercises(
    categories: List<Category>,
    exercises: List<Exercise>,
    query: String,
    selectedCategoryId: String?,
): List<ExerciseGroup> {
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val byCategory = exercises
        .filter { exercise -> words.all { exercise.name.lowercase().contains(it) } }
        .groupBy { it.categoryId }
    return categories
        .filter { selectedCategoryId == null || it.id == selectedCategoryId }
        .mapNotNull { category ->
            byCategory[category.id]?.let { ExerciseGroup(category, it) }
        }
}
