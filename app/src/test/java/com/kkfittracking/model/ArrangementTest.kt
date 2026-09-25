package com.kkfittracking.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrangementTest {
    private val defaults = SupersetTiming(transitionSeconds = 15, roundRestSeconds = 90)

    private fun entry(id: String, superset: String? = null, ownRest: Int? = null) =
        ArrangeEntry(id, id, 0, superset, transitionSeconds = superset?.let { 20 }, roundRestSeconds = superset?.let { 120 }, ownRestSeconds = ownRest)

    private fun List<ArrangeRow>.keys() = map { it.key }

    // A plan was added: bench, row, curl, plank are on their own; squat and lunge are a superset.
    private val day = listOf(
        entry("bench"),
        entry("squat", "s1"),
        entry("row"),
        entry("lunge", "s1"),
        entry("curl"),
        entry("plank"),
    )

    @Test
    fun keepsTheOrderWithSupersetsWhereTheyStart() {
        val rows = arrangementOf(day, defaults)
        assertEquals(
            listOf("bench", "start-s1", "squat", "lunge", "end-s1", "row", "curl", "plank"),
            rows.keys(),
        )
        val start = rows[1] as ArrangeRow.Start
        assertEquals(20, start.transitionSeconds)
        assertEquals(120, start.roundRestSeconds)
    }

    @Test
    fun supersetsWithoutTimingGetTheDefaults() {
        val rows = arrangementOf(listOf(ArrangeEntry("a", "a", 0, "s", null, null), ArrangeEntry("b", "b", 0, "s", null, null)), defaults)
        assertEquals(ArrangeRow.Start("s", 15, 90), rows.first())
    }

    @Test
    fun draggingBetweenStartAndEndAddsToTheSuperset() {
        var rows = addSuperset(arrangementOf(day, defaults), ArrangeRow.Start("s2", 15, 90))
        // Drag bench (index 0) to just before the new superset's end, then row there too.
        rows = moveRow(rows, 0, rows.lastIndex - 1)
        rows = moveRow(rows, rows.indexOfFirst { it.key == "row" }, rows.lastIndex - 1)

        val saved = arrangedExercises(rows).associateBy { it.id }
        assertEquals("s2", saved.getValue("bench").supersetId)
        assertEquals("s2", saved.getValue("row").supersetId)
        assertEquals(90, saved.getValue("row").roundRestSeconds)
        assertEquals("s1", saved.getValue("squat").supersetId)
        assertNull(saved.getValue("curl").supersetId)
        assertEquals((0 until 6).toList(), arrangedExercises(rows).map { it.sortOrder })
        assertEquals(listOf("squat", "lunge", "curl", "plank", "bench", "row"), arrangedExercises(rows).map { it.id })
    }

    @Test
    fun exercisesCanGoFirstAndBetweenSupersets() {
        var rows = arrangementOf(day, defaults)
        // Plank to the very top, row right after the superset: both stay on their own.
        rows = moveRow(rows, rows.indexOfFirst { it.key == "plank" }, 0)
        val saved = arrangedExercises(rows)
        assertEquals(listOf("plank", "bench", "squat", "lunge", "row", "curl"), saved.map { it.id })
        assertNull(saved.first().supersetId)
        // Start and end rows never move on their own.
        assertEquals(rows, moveRow(rows, rows.indexOfFirst { it.key == "start-s1" }, 0))
    }

    @Test
    fun moveToSupersetAndBackOut() {
        var rows = arrangementOf(day, defaults)
        rows = moveToSuperset(rows, "curl", "s1")
        assertEquals(listOf("squat", "lunge", "curl"), membersOf(rows, "s1").map { it.id })
        // Out again: it lands right after the superset.
        rows = moveToSuperset(rows, "squat", null)
        assertEquals(listOf("bench", "start-s1", "lunge", "curl", "end-s1", "squat", "row", "plank"), rows.keys())
        assertEquals(mapOf("lunge" to "s1", "curl" to "s1"), supersetMembership(rows))
    }

    @Test
    fun aNewSupersetStartsWhereTheExerciseIs() {
        val rows = moveToNewSuperset(arrangementOf(day, defaults), "row", ArrangeRow.Start("s2", 15, 90))
        assertEquals(
            listOf("bench", "start-s1", "squat", "lunge", "end-s1", "start-s2", "row", "end-s2", "curl", "plank"),
            rows.keys(),
        )
    }

    @Test
    fun aSupersetOfOneIsSavedAsOnItsOwn() {
        val rows = moveToSuperset(arrangementOf(day, defaults), "lunge", null)
        val saved = arrangedExercises(rows).associateBy { it.id }
        assertNull(saved.getValue("squat").supersetId)
        assertNull(saved.getValue("squat").transitionSeconds)
        assertNull(saved.getValue("squat").roundRestSeconds)
    }

    @Test
    fun removingASupersetKeepsItsExercisesInPlace() {
        val rows = removeSuperset(arrangementOf(day, defaults), "s1")
        assertEquals(listOf("bench", "squat", "lunge", "row", "curl", "plank"), rows.keys())
        assertTrue(arrangedExercises(rows).all { it.supersetId == null })
    }

    @Test
    fun timingCanBeChanged() {
        val rows = updateSuperset(arrangementOf(day, defaults), "s1") { it.copy(transitionSeconds = 30, roundRestSeconds = 60) }
        val saved = arrangedExercises(rows).first { it.id == "lunge" }
        assertEquals(30, saved.transitionSeconds)
        assertEquals(60, saved.roundRestSeconds)
    }

    @Test
    fun supersetsAreLimitedToSix() {
        var rows: List<ArrangeRow> = listOf(ArrangeRow.Start("big", 15, 90))
        (1..7).forEach { rows = rows + ArrangeRow.Item("$it", "$it", 0) }
        rows = rows + ArrangeRow.End("big")
        assertEquals(listOf("big"), oversizedSupersets(rows))
        assertEquals(emptyList<String>(), oversizedSupersets(moveToSuperset(rows, "7", null)))
    }
}
