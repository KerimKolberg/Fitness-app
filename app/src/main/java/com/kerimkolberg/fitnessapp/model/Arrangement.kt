package com.kerimkolberg.fitnessapp.model

/**
 * The arrange screen as one flat list: a "separate exercises" section first, then one section per
 * superset. An exercise belongs to the section whose header is the nearest one above it, so
 * dragging it under a superset header puts it in that superset.
 */
sealed interface ArrangeRow {
    val key: String

    data class Section(
        /** Null for the first section, which holds the exercises that are not in a superset. */
        val supersetId: String?,
        val transitionSeconds: Int,
    ) : ArrangeRow {
        override val key: String get() = supersetId?.let { "superset-$it" } ?: SEPARATE_KEY
    }

    data class Item(
        val workoutExerciseId: String,
        val exerciseName: String,
        val categoryColor: Int,
        val exerciseType: ExerciseType,
        val setCount: Int,
        val dropSetMode: DropSetMode,
        val plannedSets: Int?,
    ) : ArrangeRow {
        override val key: String get() = workoutExerciseId

        /** Drop sets only make sense for weight and reps. */
        val supportsDropSets: Boolean get() = exerciseType == ExerciseType.WEIGHT_REPS
    }

    companion object {
        const val SEPARATE_KEY = "separate"
        const val DEFAULT_PLANNED_SETS = 3
    }
}

/** What is saved for each exercise of the day after arranging. */
data class ArrangedExercise(
    val workoutExerciseId: String,
    val sortOrder: Int,
    val supersetId: String?,
    val transitionSeconds: Int?,
    val dropSetMode: DropSetMode,
    val plannedSets: Int?,
)

/** Builds the arrange list from a day: separate exercises first, then each superset in day order. */
fun arrangementOf(day: List<DayExercise>, defaultTransitionSeconds: Int): List<ArrangeRow> {
    fun item(exercise: DayExercise) = ArrangeRow.Item(
        workoutExerciseId = exercise.workoutExerciseId,
        exerciseName = exercise.exerciseName,
        categoryColor = exercise.categoryColor,
        exerciseType = exercise.exerciseType,
        setCount = exercise.sets.size,
        dropSetMode = exercise.dropSetMode,
        plannedSets = exercise.plannedSets,
    )
    val blocks = groupDay(day)
    val rows = mutableListOf<ArrangeRow>(ArrangeRow.Section(null, defaultTransitionSeconds))
    blocks.filterIsInstance<DayBlock.Single>().forEach { rows += item(it.exercise) }
    blocks.filterIsInstance<DayBlock.Superset>().forEach { superset ->
        val seconds = superset.exercises.firstNotNullOfOrNull { it.transitionSeconds } ?: defaultTransitionSeconds
        rows += ArrangeRow.Section(superset.id, seconds)
        superset.exercises.forEach { rows += item(it) }
    }
    return rows
}

/**
 * Moves the row at [from] to [to]. Only exercises move; the first section header always stays on
 * top, so nothing can be placed above it.
 */
fun moveRow(rows: List<ArrangeRow>, from: Int, to: Int): List<ArrangeRow> {
    if (from !in rows.indices || rows[from] !is ArrangeRow.Item) return rows
    val target = to.coerceIn(1, rows.lastIndex)
    if (target == from) return rows
    return rows.toMutableList().apply { add(target, removeAt(from)) }
}

/** Moves an exercise to the end of a section. */
fun moveToSection(rows: List<ArrangeRow>, itemKey: String, sectionKey: String): List<ArrangeRow> {
    val item = rows.firstOrNull { it.key == itemKey } as? ArrangeRow.Item ?: return rows
    val without = rows.filterNot { it.key == itemKey }.toMutableList()
    val sectionIndex = without.indexOfFirst { it.key == sectionKey }
    if (sectionIndex < 0) return rows
    val nextSection = (sectionIndex + 1 until without.size).firstOrNull { without[it] is ArrangeRow.Section } ?: without.size
    without.add(nextSection, item)
    return without
}

/** Adds an empty superset at the end. */
fun addSection(rows: List<ArrangeRow>, supersetId: String, transitionSeconds: Int): List<ArrangeRow> =
    rows + ArrangeRow.Section(supersetId, transitionSeconds)

/** Removes a superset; its exercises go back to the separate exercises. */
fun removeSection(rows: List<ArrangeRow>, sectionKey: String): List<ArrangeRow> {
    if (sectionKey == ArrangeRow.SEPARATE_KEY) return rows
    val members = membersOf(rows, sectionKey)
    var result = rows.filterNot { it.key == sectionKey || it in members }
    members.forEach { member -> result = moveToSection(result + member, member.key, ArrangeRow.SEPARATE_KEY) }
    return result
}

/** Replaces one row, found by key. */
fun updateRow(rows: List<ArrangeRow>, key: String, change: (ArrangeRow) -> ArrangeRow): List<ArrangeRow> =
    rows.map { if (it.key == key) change(it) else it }

/** The exercises in a section, in order. */
fun membersOf(rows: List<ArrangeRow>, sectionKey: String): List<ArrangeRow.Item> {
    val start = rows.indexOfFirst { it.key == sectionKey }
    if (start < 0) return emptyList()
    return rows.drop(start + 1).takeWhile { it is ArrangeRow.Item }.filterIsInstance<ArrangeRow.Item>()
}

/** Supersets that have more exercises than allowed. */
fun oversizedSections(rows: List<ArrangeRow>): List<String> =
    rows.filterIsInstance<ArrangeRow.Section>()
        .filter { it.supersetId != null && membersOf(rows, it.key).size > MAX_SUPERSET_SIZE }
        .map { it.key }

/**
 * What to save, in order. A superset with fewer than two exercises is not a superset, so its
 * exercise is saved as a separate one.
 */
fun arrangedExercises(rows: List<ArrangeRow>): List<ArrangedExercise> {
    var section: ArrangeRow.Section? = null
    var order = 0
    return rows.mapNotNull { row ->
        when (row) {
            is ArrangeRow.Section -> {
                section = row
                null
            }
            is ArrangeRow.Item -> {
                val current = section
                val isSuperset = current?.supersetId != null && membersOf(rows, current.key).size >= 2
                ArrangedExercise(
                    workoutExerciseId = row.workoutExerciseId,
                    sortOrder = order++,
                    supersetId = if (isSuperset) current?.supersetId else null,
                    transitionSeconds = if (isSuperset) current?.transitionSeconds else null,
                    dropSetMode = if (row.supportsDropSets) row.dropSetMode else DropSetMode.NONE,
                    plannedSets = row.plannedSets,
                )
            }
        }
    }
}

/**
 * Whether the next set of an exercise should be a drop set, given the plan and the sets logged
 * today: every set after the first, or once the planned normal sets are done.
 */
fun dropSetDue(mode: DropSetMode, plannedSets: Int?, setsToday: List<SetEntry>): Boolean {
    val normalSets = setsToday.count { !it.values.isDropSet }
    return when (mode) {
        DropSetMode.NONE -> false
        DropSetMode.EVERY_SET -> setsToday.isNotEmpty()
        DropSetMode.LAST_SET -> normalSets >= (plannedSets ?: ArrangeRow.DEFAULT_PLANNED_SETS)
    }
}
