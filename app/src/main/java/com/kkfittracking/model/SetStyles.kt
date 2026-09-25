package com.kkfittracking.model

/** Exercises shown together: one on its own, or the exercises of a superset. */
sealed interface Block<T> {
    val items: List<T>

    data class Single<T>(val item: T) : Block<T> {
        override val items: List<T> get() = listOf(item)
    }

    data class Superset<T>(val id: String, override val items: List<T>) : Block<T>
}

/**
 * Groups exercises (in their order) into blocks. A superset appears where its first exercise is.
 * A "superset" with a single exercise left (the others were removed) is shown as a normal exercise.
 */
fun <T> groupSupersets(items: List<T>, supersetIdOf: (T) -> String?): List<Block<T>> {
    val groups = items.filter { supersetIdOf(it) != null }.groupBy { supersetIdOf(it)!! }
    val shown = mutableSetOf<String>()
    return items.mapNotNull { item ->
        val id = supersetIdOf(item)
        val members = id?.let { groups[it] }.orEmpty()
        when {
            id == null || members.size < 2 -> Block.Single(item)
            shown.add(id) -> Block.Superset(id, members)
            else -> null
        }
    }
}

/** A day's exercises in blocks. */
fun groupDay(exercises: List<DayExercise>): List<Block<DayExercise>> = groupSupersets(exercises) { it.supersetId }

/**
 * Moves the block at [index] one place up (-1) or down (+1), and returns every exercise in its new
 * order. A superset moves as a whole.
 */
fun <T> moveBlock(blocks: List<Block<T>>, index: Int, direction: Int): List<T> {
    val target = index + direction
    if (index !in blocks.indices || target !in blocks.indices) return blocks.flatMap { it.items }
    val list = blocks.toMutableList()
    list.add(target, list.removeAt(index))
    return list.flatMap { it.items }
}

/** The exercise after [current] in a superset, going back to the first after the last. */
fun nextInSuperset(memberIds: List<String>, current: String): String? {
    val index = memberIds.indexOf(current)
    if (index < 0 || memberIds.size < 2) return null
    return memberIds[(index + 1) % memberIds.size]
}

/** True when [current] is the last exercise of a superset round, so the rest timer should start. */
fun isLastInSuperset(memberIds: List<String>, current: String): Boolean =
    memberIds.size < 2 || memberIds.lastOrNull() == current

/**
 * The weight for a drop set: [percent] less than the previous set, rounded to 0.5 kg or 1 lb so it
 * matches real plates and dumbbells. Takes and returns kg.
 */
fun dropSetWeightKg(previousKg: Double, percent: Int, units: UnitSystem): Double =
    roundToPlates(previousKg * (1 - percent / 100.0), units)

/** The label in front of each set: "1", "2", ... with "↘" for drop sets, which continue the set before. */
fun setLabels(sets: List<SetEntry>): List<String> {
    var number = 0
    return sets.map { set ->
        if (set.values.isDropSet && number > 0) {
            "↘"
        } else {
            number++
            number.toString()
        }
    }
}
