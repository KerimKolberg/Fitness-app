package com.kkfittracking.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrangementTest {
    private fun exercise(id: String, superset: String? = null, type: ExerciseType = ExerciseType.WEIGHT_REPS) =
        DayExercise("we-$id", id, id, type, 0, emptyList(), superset, transitionSeconds = superset?.let { 20 })

    private fun List<ArrangeRow>.keys() = map { it.key }

    // A plan was added: bench, row, curl, plank are separate; squat and lunge are a superset.
    private val day = listOf(
        exercise("bench"),
        exercise("squat", "s1"),
        exercise("row"),
        exercise("lunge", "s1"),
        exercise("curl"),
        exercise("plank", type = ExerciseType.TIME),
    )

    @Test
    fun sectionsStartWithSeparateExercises() {
        val rows = arrangementOf(day, defaultTransitionSeconds = 15)
        assertEquals(
            listOf("separate", "we-bench", "we-row", "we-curl", "we-plank", "superset-s1", "we-squat", "we-lunge"),
            rows.keys(),
        )
        assertEquals(20, (rows[5] as ArrangeRow.Section).transitionSeconds)
    }

    @Test
    fun draggingUnderASupersetHeaderAddsToIt() {
        var rows = arrangementOf(day, 15)
        rows = addSection(rows, "s2", 15)
        // Drag bench (index 1) to the very end, under the new superset header.
        rows = moveRow(rows, 1, rows.lastIndex)
        // Drag row (now index 1) there too.
        rows = moveRow(rows, 1, rows.lastIndex)

        val saved = arrangedExercises(rows).associateBy { it.workoutExerciseId }
        assertEquals("s2", saved.getValue("we-bench").supersetId)
        assertEquals("s2", saved.getValue("we-row").supersetId)
        assertEquals(15, saved.getValue("we-row").transitionSeconds)
        assertEquals("s1", saved.getValue("we-squat").supersetId)
        assertNull(saved.getValue("we-curl").supersetId)
        assertEquals((0 until 6).toList(), arrangedExercises(rows).map { it.sortOrder })
    }

    @Test
    fun nothingGoesAboveTheFirstHeader() {
        val rows = arrangementOf(day, 15)
        assertEquals(rows, moveRow(rows, 0, 3))
        assertEquals("separate", moveRow(rows, 2, 0).first().key)
    }

    @Test
    fun aSupersetOfOneIsSavedAsSeparate() {
        var rows = arrangementOf(day, 15)
        rows = moveToSection(rows, "we-lunge", ArrangeRow.SEPARATE_KEY)
        val saved = arrangedExercises(rows).associateBy { it.workoutExerciseId }
        assertNull(saved.getValue("we-squat").supersetId)
        assertNull(saved.getValue("we-squat").transitionSeconds)
    }

    @Test
    fun removingASupersetKeepsItsExercises() {
        val rows = removeSection(arrangementOf(day, 15), "superset-s1")
        assertFalse(rows.any { it.key == "superset-s1" })
        assertTrue(arrangedExercises(rows).all { it.supersetId == null })
        assertEquals(6, arrangedExercises(rows).size)
    }

    @Test
    fun supersetsAreLimitedToSix() {
        var rows = listOf<ArrangeRow>(ArrangeRow.Section(null, 15), ArrangeRow.Section("big", 15))
        (1..7).forEach { rows = rows + ArrangeRow.Item("we-$it", "$it", 0, ExerciseType.WEIGHT_REPS, 0, DropSetMode.NONE, null) }
        assertEquals(listOf("superset-big"), oversizedSections(rows))
    }

    @Test
    fun dropSetPlansOnlyForWeightAndReps() {
        var rows = arrangementOf(day, 15)
        rows = updateRow(rows, "we-plank") { (it as ArrangeRow.Item).copy(dropSetMode = DropSetMode.EVERY_SET) }
        rows = updateRow(rows, "we-curl") { (it as ArrangeRow.Item).copy(dropSetMode = DropSetMode.LAST_SET, plannedSets = 4) }
        val saved = arrangedExercises(rows).associateBy { it.workoutExerciseId }
        assertEquals(DropSetMode.NONE, saved.getValue("we-plank").dropSetMode)
        assertEquals(DropSetMode.LAST_SET, saved.getValue("we-curl").dropSetMode)
        assertEquals(4, saved.getValue("we-curl").plannedSets)
    }

    @Test
    fun whenADropSetIsDue() {
        fun sets(vararg drops: Boolean) = drops.map { SetEntry("x", SetValues(100.0, 8, isDropSet = it)) }
        assertFalse(dropSetDue(DropSetMode.NONE, null, sets(false, false, false)))
        assertFalse(dropSetDue(DropSetMode.EVERY_SET, null, sets()))
        assertTrue(dropSetDue(DropSetMode.EVERY_SET, null, sets(false)))
        assertFalse(dropSetDue(DropSetMode.LAST_SET, 3, sets(false, false)))
        assertTrue(dropSetDue(DropSetMode.LAST_SET, 3, sets(false, false, false)))
        assertTrue(dropSetDue(DropSetMode.LAST_SET, 3, sets(false, false, false, true)))
        assertTrue(dropSetDue(DropSetMode.LAST_SET, null, sets(false, false, false)))
    }
}
