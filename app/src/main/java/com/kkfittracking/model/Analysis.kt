package com.kkfittracking.model

import java.time.LocalDate

/** How far back a graph goes. */
enum class TimeRange(val label: String, val months: Long?) {
    MONTH("1 month", 1),
    THREE_MONTHS("3 months", 3),
    YEAR("1 year", 12),
    ALL("All", null),
    ;

    /** The first day shown, or null for everything. */
    fun start(today: LocalDate): LocalDate? = months?.let { today.minusMonths(it) }
}

/** The sessions of [range], counted back from [today]. */
fun List<HistorySession>.within(range: TimeRange, today: LocalDate = LocalDate.now()): List<HistorySession> {
    val start = range.start(today) ?: return this
    return filter { !it.date.isBefore(start) }
}

/** An exercise's headline personal record, for the list of all records. */
data class ExerciseRecord(val exercise: Exercise, val record: RecordEntry, val sessions: Int)

/**
 * The best of every exercise that has been logged: its first record (an estimated 1RM for weights,
 * the longest time for holds, the most reps…), newest record first.
 */
fun allRecords(
    histories: Map<String, List<HistorySession>>,
    exercises: List<Exercise>,
    units: UnitSystem,
): List<ExerciseRecord> = exercises.mapNotNull { exercise ->
    val history = histories[exercise.id].orEmpty()
    personalRecords(history, exercise.type, exercise.unitsOr(units)).firstOrNull()?.let { ExerciseRecord(exercise, it, history.size) }
}.sortedWith(compareByDescending<ExerciseRecord> { it.record.date }.thenBy { it.exercise.name })

/** The history of every exercise at once, newest session first, from sets logged on a date. */
fun historiesOf(sets: List<Triple<LocalDate, String, SetEntry>>): Map<String, List<HistorySession>> =
    sets.groupBy { it.second }.mapValues { (_, rows) ->
        rows.groupBy { it.first }.map { (date, dayRows) -> HistorySession(date, dayRows.map { it.third }) }.sortedByDescending { it.date }
    }
