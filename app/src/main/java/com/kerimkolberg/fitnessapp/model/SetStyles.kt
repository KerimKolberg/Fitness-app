package com.kerimkolberg.fitnessapp.model

import kotlin.math.roundToInt

/** How a day's exercises are shown: on their own, or together as a superset. */
sealed interface DayBlock {
    data class Single(val exercise: DayExercise) : DayBlock
    data class Superset(val id: String, val exercises: List<DayExercise>) : DayBlock
}

/**
 * Groups a day's exercises (in their logged order) into blocks. A superset appears where its
 * first exercise is. A "superset" with a single exercise left (the others were removed) is shown
 * as a normal exercise.
 */
fun groupDay(exercises: List<DayExercise>): List<DayBlock> {
    val groups = exercises.filter { it.supersetId != null }.groupBy { it.supersetId!! }
    val shown = mutableSetOf<String>()
    return exercises.mapNotNull { exercise ->
        val id = exercise.supersetId
        val members = id?.let { groups[it] }.orEmpty()
        when {
            id == null || members.size < 2 -> DayBlock.Single(exercise)
            shown.add(id) -> DayBlock.Superset(id, members)
            else -> null
        }
    }
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
fun dropSetWeightKg(previousKg: Double, percent: Int, units: UnitSystem): Double {
    val step = if (units == UnitSystem.METRIC) 0.5 else 1.0
    val display = units.weightFromKg(previousKg) * (1 - percent / 100.0)
    val rounded = ((display / step).roundToInt() * step).coerceAtLeast(0.0)
    return units.weightToKg(rounded)
}

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
