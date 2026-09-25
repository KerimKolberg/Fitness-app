package com.kkfittracking.ui.exercises

import com.kkfittracking.model.Category
import com.kkfittracking.model.Exercise
import com.kkfittracking.model.ExerciseType
import com.kkfittracking.model.Muscle
import com.kkfittracking.model.Regions
import com.kkfittracking.model.Tendon
import com.kkfittracking.model.TrainingStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryRowsTest {
    private val chest = Category("chest", "Chest", 0, Regions.CHEST)
    private val back = Category("back", "Back", 0, Regions.BACK)
    private val legs = Category("legs", "Legs", 0, Regions.LEGS)
    private val fullBody = Category("full", "Full body", 0, Regions.FULL_BODY)
    private val sports = Category("sports", "Sports", 0, Regions.SPORTS)
    private val categories = listOf(chest, back, legs, fullBody, sports)

    private fun exercise(
        name: String,
        category: Category,
        vararg muscles: Muscle,
        style: TrainingStyle = TrainingStyle.STRENGTH,
        tendons: List<Tendon> = emptyList(),
    ) = Exercise(
        id = name, name = name, categoryId = category.id, type = ExerciseType.WEIGHT_REPS, notes = "", isCustom = false,
        muscles = muscles.toList(), styles = listOf(style), tendons = tendons,
    )

    private val exercises = listOf(
        exercise("Flat Barbell Bench Press", chest, Muscle.CHEST),
        exercise("Deadlift", back, Muscle.LOWER_BACK, Muscle.HAMSTRINGS),
        exercise("Barbell Squat", legs, Muscle.QUADS),
        exercise("Hamstring Stretch", legs, Muscle.HAMSTRINGS, style = TrainingStyle.STRETCHING),
        exercise("Nordic Hamstring Curl", legs, Muscle.HAMSTRINGS, style = TrainingStyle.ECCENTRIC, tendons = listOf(Tendon.HAMSTRING)),
        exercise("Romanian Deadlift", legs, Muscle.HAMSTRINGS),
        exercise("Wall Sit", legs, Muscle.QUADS, style = TrainingStyle.ISOMETRIC, tendons = listOf(Tendon.PATELLAR)),
        exercise("World's Greatest Stretch", fullBody, Muscle.FULL_BODY, Muscle.HAMSTRINGS, style = TrainingStyle.STRETCHING),
        exercise("Tennis", sports, Muscle.SPORT, style = TrainingStyle.SPORT),
    )

    private fun List<LibraryRow>.describe() = map {
        when (it) {
            is LibraryRow.SectionHeader -> "# ${it.category.name} (${it.count})"
            is LibraryRow.MuscleHeader -> "## ${it.muscle.label}"
            is LibraryRow.StyleHeader -> "### ${it.style.label}"
            is LibraryRow.Item -> it.exercise.name + if (it.secondary) " (also)" else ""
        }
    }

    @Test
    fun anExerciseIsListedUnderEveryMuscleItTrains() {
        assertEquals(
            listOf(
                "# Legs (5)",
                "## Hamstrings",
                "### Strength",
                "Romanian Deadlift",
                "Deadlift (also)",
                "### Eccentrics & tendons",
                "Nordic Hamstring Curl",
                "### Stretching",
                "Hamstring Stretch",
                "World's Greatest Stretch (also)",
            ),
            libraryRows(categories, exercises, LibraryFilter(categoryId = "legs", muscle = Muscle.HAMSTRINGS)).describe(),
        )
        // The deadlift is still in its own section too.
        assertEquals(
            listOf("# Back (1)", "## Lower back", "Deadlift"),
            libraryRows(categories, exercises, LibraryFilter(categoryId = "back")).describe(),
        )
    }

    @Test
    fun aTrainingStyleIsASectionWithMuscleGroupsUnderIt() {
        val stretching = LibraryFilter(style = TrainingStyle.STRETCHING)
        assertEquals(
            listOf(
                "# Legs (2)",
                "## Hamstrings",
                "Hamstring Stretch",
                "World's Greatest Stretch (also)",
                "# Full body (1)",
                "World's Greatest Stretch",
            ),
            libraryRows(categories, exercises, stretching).describe(),
        )
        assertEquals(listOf(Muscle.HAMSTRINGS, Muscle.FULL_BODY), muscleChoices(categories, exercises, stretching))
        // Picking a muscle group narrows the style section.
        assertEquals(
            listOf("Hamstring Stretch", "World's Greatest Stretch"),
            filterExercises(categories, exercises, stretching.copy(muscle = Muscle.HAMSTRINGS)).map { it.name },
        )
    }

    @Test
    fun sectionsWithOneMuscleAndTheUsualStyleNeedNoHeaders() {
        assertEquals(
            listOf("# Chest (1)", "Flat Barbell Bench Press"),
            libraryRows(categories, exercises, LibraryFilter(categoryId = "chest")).describe(),
        )
        assertEquals(
            listOf("# Sports (1)", "Tennis"),
            libraryRows(categories, exercises, LibraryFilter(categoryId = "sports")).describe(),
        )
    }

    @Test
    fun searchMatchesNamesMusclesStylesAndSections() {
        fun search(query: String) = filterExercises(categories, exercises, LibraryFilter(query = query)).map { it.name }
        assertEquals(listOf("Flat Barbell Bench Press"), search("press barbell"))
        assertEquals(listOf("Nordic Hamstring Curl"), search("hamstrings eccentric"))
        // The deadlift's other muscles count too.
        assertEquals(listOf("Deadlift", "Romanian Deadlift"), search("deadlift hamstrings"))
        assertEquals(listOf("Nordic Hamstring Curl"), search("eccentric legs"))
    }

    @Test
    fun styleSectionsComeInTheirOwnOrder() {
        assertEquals(
            listOf(TrainingStyle.STRETCHING, TrainingStyle.ISOMETRIC, TrainingStyle.ECCENTRIC, TrainingStyle.STRENGTH, TrainingStyle.SPORT),
            styleChoices(categories, exercises, LibraryFilter()),
        )
        assertEquals(
            listOf(TrainingStyle.ISOMETRIC, TrainingStyle.STRENGTH),
            styleChoices(categories, exercises, LibraryFilter(categoryId = "legs", muscle = Muscle.QUADS)),
        )
        assertEquals(listOf("Tennis"), filterExercises(categories, exercises, LibraryFilter(allowedIds = setOf("Tennis"))).map { it.name })
    }

    @Test
    fun tendonsCanBeFoundAndFiltered() {
        fun search(query: String) = filterExercises(categories, exercises, LibraryFilter(query = query)).map { it.name }
        assertEquals(listOf("Wall Sit"), search("patellar"))
        assertEquals(listOf(Tendon.PATELLAR, Tendon.HAMSTRING), tendonChoices(categories, exercises, LibraryFilter()))
        assertEquals(
            listOf("Nordic Hamstring Curl"),
            filterExercises(categories, exercises, LibraryFilter(style = TrainingStyle.ECCENTRIC, tendon = Tendon.HAMSTRING)).map { it.name },
        )
        assertEquals(listOf(Tendon.PATELLAR), tendonChoices(categories, exercises, LibraryFilter(style = TrainingStyle.ISOMETRIC)))
    }
}
