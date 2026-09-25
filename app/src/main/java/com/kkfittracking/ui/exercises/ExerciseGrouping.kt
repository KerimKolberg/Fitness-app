package com.kkfittracking.ui.exercises

import com.kkfittracking.model.Category
import com.kkfittracking.model.Exercise
import com.kkfittracking.model.ExerciseType
import com.kkfittracking.model.Muscle
import com.kkfittracking.model.Tendon
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

    /** An exercise listed under [muscle]; [secondary] when that is not the muscle it mainly trains. */
    data class Item(val exercise: Exercise, val category: Category, val muscle: Muscle, val secondary: Boolean) : LibraryRow {
        override val key: String get() = "${category.id}/${muscle.name}/${exercise.id}"
    }
}

data class LibraryFilter(
    /** Every word must appear in the exercise's name, muscles, style or sections. */
    val query: String = "",
    /** A body section. */
    val categoryId: String? = null,
    val muscle: Muscle? = null,
    /** A training style section, such as Stretching. */
    val style: TrainingStyle? = null,
    /** A tendon the exercises load, such as the patellar tendon. */
    val tendon: Tendon? = null,
    /** When set, only these exercises are shown (the exercises of a plan). */
    val allowedIds: Set<String>? = null,
)

/** A place an exercise is listed: a muscle it trains, in that muscle's section. */
data class Placement(val category: Category, val muscle: Muscle, val secondary: Boolean)

/**
 * Where an exercise is listed: under every muscle it trains, in that muscle's body section (a
 * deadlift under Back → Lower back and under Legs → Hamstrings and Glutes), and always in its own
 * section.
 */
fun placements(exercise: Exercise, categories: List<Category>): List<Placement> {
    val result = mutableListOf<Placement>()
    exercise.muscles.forEachIndexed { index, muscle ->
        val section = categories.firstOrNull { it.key != null && it.key == muscle.regionKey } ?: return@forEachIndexed
        if (result.none { it.category.id == section.id && it.muscle == muscle }) {
            result += Placement(section, muscle, secondary = index > 0)
        }
    }
    val own = categories.firstOrNull { it.id == exercise.categoryId }
    if (own != null && result.none { it.category.id == own.id }) {
        result.add(0, Placement(own, Muscle.defaultFor(own.key), secondary = false))
    }
    return result
}

private fun LibraryFilter.accepts(placement: Placement) =
    (categoryId == null || placement.category.id == categoryId) && (muscle == null || placement.muscle == muscle)

/** The exercises passing every filter, in their given order. */
fun filterExercises(categories: List<Category>, exercises: List<Exercise>, filter: LibraryFilter): List<Exercise> {
    val words = filter.query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return exercises.filter { exercise ->
        val places = placements(exercise, categories)
        (filter.allowedIds == null || exercise.id in filter.allowedIds) &&
            (filter.style == null || filter.style in exercise.styles) &&
            (filter.tendon == null || filter.tendon in exercise.tendons) &&
            places.any { filter.accepts(it) } &&
            (words.isEmpty() || searchText(exercise, places).let { text -> words.all { it in text } })
    }
}

private fun searchText(exercise: Exercise, places: List<Placement>): String =
    (
        listOf(exercise.name) + exercise.styles.map { it.label } + exercise.muscles.map { it.label } +
            exercise.tendons.map { it.label } + places.map { it.category.name }
        )
        .joinToString(" ").lowercase()

/**
 * The library as rows: body sections, then muscles, then training styles (left out when a style
 * section is chosen). Headers that would say nothing new are left out: the muscle when a section
 * has just one (Chest), and the style when all exercises there are trained the usual way. Within a
 * group, exercises that mainly train that muscle come first.
 */
fun libraryRows(categories: List<Category>, exercises: List<Exercise>, filter: LibraryFilter): List<LibraryRow> {
    val placed = filterExercises(categories, exercises, filter).flatMap { exercise ->
        placements(exercise, categories).filter { filter.accepts(it) }.map { it to exercise }
    }
    val byCategory = placed.groupBy { it.first.category.id }
    return categories.flatMap { category ->
        val inSection = byCategory[category.id] ?: return@flatMap emptyList()
        val rows = mutableListOf<LibraryRow>(LibraryRow.SectionHeader(category, inSection.map { it.second.id }.distinct().size))
        val byMuscle = inSection.groupBy { it.first.muscle }.toSortedMap()
        val showMuscles = Muscle.forRegion(category.key).size > 2 || byMuscle.size > 1
        val usualStyle = TrainingStyle.defaultFor(category.key, ExerciseType.WEIGHT_REPS)
        byMuscle.forEach { (muscle, inMuscle) ->
            if (showMuscles) rows += LibraryRow.MuscleHeader(category.id, muscle)
            fun items(list: List<Pair<Placement, Exercise>>) = list.sortedBy { it.first.secondary }.forEach { (place, exercise) ->
                rows += LibraryRow.Item(exercise, category, muscle, place.secondary)
            }
            if (filter.style != null) {
                items(inMuscle)
            } else {
                val byStyle = inMuscle.groupBy { it.second.style }.toSortedMap()
                val showStyles = byStyle.size > 1 || byStyle.firstKey() != usualStyle
                byStyle.forEach { (style, inStyle) ->
                    if (showStyles) rows += LibraryRow.StyleHeader(category.id, muscle, style)
                    items(inStyle)
                }
            }
        }
        rows
    }
}

/** The muscles to offer as sub-sections: those of the exercises that pass the other filters. */
fun muscleChoices(categories: List<Category>, exercises: List<Exercise>, filter: LibraryFilter): List<Muscle> {
    val others = filter.copy(muscle = null)
    return filterExercises(categories, exercises, others)
        .flatMap { exercise -> placements(exercise, categories).filter { others.accepts(it) }.map { it.muscle } }
        .distinct().sorted()
}

/** The tendons to offer: those loaded by the exercises that pass the other filters. */
fun tendonChoices(categories: List<Category>, exercises: List<Exercise>, filter: LibraryFilter): List<Tendon> {
    val present = filterExercises(categories, exercises, filter.copy(tendon = null)).flatMap { it.tendons }.toSet()
    return Tendon.entries.filter { it in present }
}

/** The training style sections to offer: those of the exercises that pass the other filters. */
fun styleChoices(categories: List<Category>, exercises: List<Exercise>, filter: LibraryFilter): List<TrainingStyle> {
    val present = filterExercises(categories, exercises, filter.copy(style = null)).flatMap { it.styles }.toSet()
    return TrainingStyle.sectionOrder.filter { it in present }
}
