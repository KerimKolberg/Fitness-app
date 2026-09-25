package com.kkfittracking.ui.exercises

import com.kkfittracking.model.Category
import com.kkfittracking.model.Exercise
import com.kkfittracking.model.ExerciseType
import com.kkfittracking.model.Muscle
import com.kkfittracking.model.TrainingStyle

/** One row of the exercise library: section, muscle and training style headers, then exercises. */
sealed interface LibraryRow {
    val key: String

    data class SectionHeader(val category: Category, val count: Int) : LibraryRow {
        override val key: String get() = "section-${category.id}"
    }

    data class MuscleHeader(val categoryId: String, val muscle: Muscle) : LibraryRow {
        override val key: String get() = "muscle-$categoryId-${muscle.name}"
    }

    data class StyleHeader(val categoryId: String, val muscle: Muscle, val style: TrainingStyle) : LibraryRow {
        override val key: String get() = "style-$categoryId-${muscle.name}-${style.name}"
    }

    data class Item(val exercise: Exercise, val category: Category) : LibraryRow {
        override val key: String get() = exercise.id
    }
}

data class LibraryFilter(
    /** Every word must appear in the exercise's name, muscle, style or section. */
    val query: String = "",
    val categoryId: String? = null,
    val muscle: Muscle? = null,
    val style: TrainingStyle? = null,
    /** When set, only these exercises are shown (the exercises of a plan). */
    val allowedIds: Set<String>? = null,
)

/** The exercises passing every filter, in their given order. */
fun filterExercises(categories: List<Category>, exercises: List<Exercise>, filter: LibraryFilter): List<Exercise> {
    val words = filter.query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val sectionNames = categories.associate { it.id to it.name }
    return exercises.filter { exercise ->
        (filter.allowedIds == null || exercise.id in filter.allowedIds) &&
            (filter.categoryId == null || exercise.categoryId == filter.categoryId) &&
            (filter.muscle == null || exercise.muscle == filter.muscle) &&
            (filter.style == null || exercise.style == filter.style) &&
            (words.isEmpty() || searchText(exercise, sectionNames[exercise.categoryId]).let { text -> words.all { it in text } })
    }
}

private fun searchText(exercise: Exercise, section: String?): String =
    listOfNotNull(exercise.name, exercise.muscle.label, exercise.style.label, section).joinToString(" ").lowercase()

/**
 * The library as rows, in section order, then by muscle and training style. Headers that would say
 * nothing new are left out: the muscle when a section has just one (Chest), and the style when all
 * exercises there are trained the usual way (strength in the gym, cardio, sport).
 */
fun libraryRows(categories: List<Category>, exercises: List<Exercise>, filter: LibraryFilter): List<LibraryRow> {
    val byCategory = filterExercises(categories, exercises, filter).groupBy { it.categoryId }
    return categories.flatMap { category ->
        val inSection = byCategory[category.id] ?: return@flatMap emptyList()
        val rows = mutableListOf<LibraryRow>(LibraryRow.SectionHeader(category, inSection.size))
        val byMuscle = inSection.groupBy { it.muscle }.toSortedMap()
        val showMuscles = Muscle.forRegion(category.key).size > 2 || byMuscle.size > 1
        val usualStyle = TrainingStyle.defaultFor(category.key, ExerciseType.WEIGHT_REPS)
        byMuscle.forEach { (muscle, muscleExercises) ->
            if (showMuscles) rows += LibraryRow.MuscleHeader(category.id, muscle)
            val byStyle = muscleExercises.groupBy { it.style }.toSortedMap()
            val showStyles = byStyle.size > 1 || byStyle.firstKey() != usualStyle
            byStyle.forEach { (style, styleExercises) ->
                if (showStyles) rows += LibraryRow.StyleHeader(category.id, muscle, style)
                styleExercises.forEach { rows += LibraryRow.Item(it, category) }
            }
        }
        rows
    }
}

/** The muscles to offer as filters: those of the exercises that pass the other filters. */
fun muscleChoices(categories: List<Category>, exercises: List<Exercise>, filter: LibraryFilter): List<Muscle> =
    filterExercises(categories, exercises, filter.copy(muscle = null)).map { it.muscle }.distinct().sorted()

/** The training styles to offer as filters: those of the exercises that pass the other filters. */
fun styleChoices(categories: List<Category>, exercises: List<Exercise>, filter: LibraryFilter): List<TrainingStyle> =
    filterExercises(categories, exercises, filter.copy(style = null)).map { it.style }.distinct().sorted()
