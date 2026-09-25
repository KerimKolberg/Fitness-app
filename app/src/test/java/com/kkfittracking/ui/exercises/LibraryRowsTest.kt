package com.kkfittracking.ui.exercises

import com.kkfittracking.model.Category
import com.kkfittracking.model.Exercise
import com.kkfittracking.model.ExerciseType
import com.kkfittracking.model.Muscle
import com.kkfittracking.model.Regions
import com.kkfittracking.model.TrainingStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryRowsTest {
    private val chest = Category("chest", "Chest", 0, Regions.CHEST)
    private val legs = Category("legs", "Legs", 0, Regions.LEGS)
    private val sports = Category("sports", "Sports", 0, Regions.SPORTS)
    private val categories = listOf(chest, legs, sports)

    private fun exercise(name: String, category: Category, muscle: Muscle, style: TrainingStyle = TrainingStyle.STRENGTH) =
        Exercise(
            id = name, name = name, categoryId = category.id, type = ExerciseType.WEIGHT_REPS, notes = "", isCustom = false,
            muscle = muscle, style = style,
        )

    private val exercises = listOf(
        exercise("Flat Barbell Bench Press", chest, Muscle.CHEST),
        exercise("Incline Dumbbell Bench Press", chest, Muscle.CHEST),
        exercise("Barbell Squat", legs, Muscle.QUADS),
        exercise("Nordic Hamstring Curl", legs, Muscle.HAMSTRINGS, TrainingStyle.ECCENTRIC),
        exercise("Romanian Deadlift", legs, Muscle.HAMSTRINGS),
        exercise("Wall Sit", legs, Muscle.QUADS, TrainingStyle.ISOMETRIC),
        exercise("Tennis", sports, Muscle.SPORT, TrainingStyle.SPORT),
        exercise("Tabata", sports, Muscle.SPORT, TrainingStyle.HIIT),
    )

    private fun List<LibraryRow>.describe() = map {
        when (it) {
            is LibraryRow.SectionHeader -> "# ${it.category.name} (${it.count})"
            is LibraryRow.MuscleHeader -> "## ${it.muscle.label}"
            is LibraryRow.StyleHeader -> "### ${it.style.label}"
            is LibraryRow.Item -> it.exercise.name
        }
    }

    @Test
    fun sectionsThenMusclesThenStyles() {
        assertEquals(
            listOf(
                "# Chest (2)",
                "Flat Barbell Bench Press",
                "Incline Dumbbell Bench Press",
                "# Legs (4)",
                "## Quads",
                "### Strength",
                "Barbell Squat",
                "### Isometric",
                "Wall Sit",
                "## Hamstrings",
                "### Strength",
                "Romanian Deadlift",
                "### Eccentric & tendon",
                "Nordic Hamstring Curl",
                "# Sports (2)",
                "### HIIT & intervals",
                "Tabata",
                "### Sport",
                "Tennis",
            ),
            libraryRows(categories, exercises, LibraryFilter()).describe(),
        )
    }

    @Test
    fun searchMatchesNamesMusclesAndStyles() {
        fun search(query: String) = filterExercises(categories, exercises, LibraryFilter(query = query)).map { it.name }
        assertEquals(listOf("Flat Barbell Bench Press"), search("press barbell"))
        assertEquals(listOf("Nordic Hamstring Curl", "Romanian Deadlift"), search("hamstrings"))
        assertEquals(listOf("Nordic Hamstring Curl"), search("eccentric legs"))
    }

    @Test
    fun filtersByPlanSectionMuscleAndStyle() {
        fun names(filter: LibraryFilter) = filterExercises(categories, exercises, filter).map { it.name }
        assertEquals(listOf("Barbell Squat"), names(LibraryFilter(query = "barbell", categoryId = "legs")))
        assertEquals(listOf("Barbell Squat", "Wall Sit"), names(LibraryFilter(muscle = Muscle.QUADS)))
        assertEquals(listOf("Wall Sit"), names(LibraryFilter(style = TrainingStyle.ISOMETRIC)))
        assertEquals(listOf("Tennis"), names(LibraryFilter(allowedIds = setOf("Tennis"))))
    }

    @Test
    fun filterChoicesComeFromTheOtherFilters() {
        val inLegs = LibraryFilter(categoryId = "legs", muscle = Muscle.QUADS)
        assertEquals(listOf(Muscle.QUADS, Muscle.HAMSTRINGS), muscleChoices(categories, exercises, inLegs))
        assertEquals(listOf(TrainingStyle.STRENGTH, TrainingStyle.ISOMETRIC), styleChoices(categories, exercises, inLegs))
    }
}
