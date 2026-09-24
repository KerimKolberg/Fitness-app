package com.kerimkolberg.fitnessapp.model

import java.time.LocalDate

/**
 * Estimated one-rep max with the Epley formula. A single rep is the weight itself. Estimates get
 * less reliable above about 10 reps.
 */
fun estimatedOneRepMax(weightKg: Double, reps: Int): Double = when {
    reps <= 0 || weightKg <= 0.0 -> 0.0
    reps == 1 -> weightKg
    else -> weightKg * (1 + reps / 30.0)
}

/**
 * The number a personal record is judged by: estimated 1RM for weighted sets, reps, time held, or
 * distance covered (time when no distance was logged). Null when the set has nothing to compare.
 */
fun recordScore(values: SetValues, type: ExerciseType): Double? {
    val score = when (type) {
        ExerciseType.WEIGHT_REPS -> estimatedOneRepMax(values.weightKg ?: 0.0, values.reps ?: 0)
        ExerciseType.REPS -> (values.reps ?: 0).toDouble()
        ExerciseType.TIME -> (values.durationSeconds ?: 0).toDouble()
        ExerciseType.DISTANCE_TIME ->
            values.distanceMeters?.takeIf { it > 0 } ?: (values.durationSeconds ?: 0).toDouble()
        // A longer hold is the record; the weight used is shown alongside it.
        ExerciseType.TIME_WEIGHT -> (values.durationSeconds ?: 0).toDouble()
        ExerciseType.REPS_HEIGHT -> values.distanceMeters?.takeIf { it > 0 } ?: (values.reps ?: 0).toDouble()
        // Sports sessions are not a contest with yourself.
        ExerciseType.SESSION -> 0.0
    }
    return score.takeIf { it > 0.0 }
}

/**
 * The sets that set a new personal record when they were logged: each beat every earlier set of the
 * exercise. [history] may be in any order; sets within a session keep their logged order.
 */
fun personalRecordSetIds(history: List<HistorySession>, type: ExerciseType): Set<String> {
    val records = mutableSetOf<String>()
    var best = 0.0
    history.sortedBy { it.date }.forEach { session ->
        session.sets.forEach { set ->
            val score = recordScore(set.values, type)
            if (score != null && score > best) {
                best = score
                records += set.id
            }
        }
    }
    return records
}

/** A best value and the date it was achieved. */
data class RecordEntry(val label: String, val value: String, val date: LocalDate)

/** The personal bests to show for an exercise, formatted in the user's units. */
fun personalRecords(history: List<HistorySession>, type: ExerciseType, units: UnitSystem): List<RecordEntry> {
    val sets = history.flatMap { session -> session.sets.map { session.date to it.values } }
    if (sets.isEmpty()) return emptyList()

    fun best(label: String, score: (SetValues) -> Double?, format: (SetValues, Double) -> String): RecordEntry? =
        sets.mapNotNull { (date, values) -> score(values)?.takeIf { it > 0 }?.let { Triple(date, values, it) } }
            // Earliest date wins a tie: that is when the record was set.
            .maxWithOrNull(compareBy<Triple<LocalDate, SetValues, Double>> { it.third }.thenByDescending { it.first })
            ?.let { (date, values, value) -> RecordEntry(label, format(values, value), date) }

    fun weight(kg: Double) = "${formatNumber(units.weightFromKg(kg))} ${units.weightUnit}"

    val entries = when (type) {
        ExerciseType.WEIGHT_REPS -> listOfNotNull(
            best("Estimated 1RM", { estimatedOneRepMax(it.weightKg ?: 0.0, it.reps ?: 0) }) { _, e1rm -> weight(e1rm) },
            best("Heaviest weight", { it.weightKg }) { values, _ -> formatSet(values, type, units) },
            best("Most reps", { it.reps?.toDouble() }) { values, _ -> formatSet(values, type, units) },
            bestSession(history) { session ->
                session.sets.sumOf { (it.values.weightKg ?: 0.0) * (it.values.reps ?: 0) }
            }?.let { (date, volume) -> RecordEntry("Best session volume", weight(volume), date) },
        )
        ExerciseType.REPS -> listOfNotNull(
            best("Most reps in a set", { it.reps?.toDouble() }) { _, reps -> "${reps.toInt()} reps" },
            bestSession(history) { session -> session.sets.sumOf { it.values.reps ?: 0 }.toDouble() }
                ?.let { (date, reps) -> RecordEntry("Most reps in a session", "${reps.toInt()} reps", date) },
        )
        ExerciseType.TIME -> listOfNotNull(
            best("Longest time", { it.durationSeconds?.toDouble() }) { _, seconds -> formatDuration(seconds.toInt()) },
        )
        ExerciseType.DISTANCE_TIME -> listOfNotNull(
            best("Longest distance", { it.distanceMeters }) { _, meters ->
                "${formatNumber(units.distanceFromMeters(meters))} ${units.distanceUnit}"
            },
            best("Longest time", { it.durationSeconds?.toDouble() }) { _, seconds -> formatDuration(seconds.toInt()) },
        )
        ExerciseType.TIME_WEIGHT -> listOfNotNull(
            best("Longest hold", { it.durationSeconds?.toDouble() }) { values, _ -> formatSet(values, type, units) },
            best("Heaviest weight", { it.weightKg }) { values, _ -> formatSet(values, type, units) },
        )
        ExerciseType.REPS_HEIGHT -> listOfNotNull(
            best("Highest or longest jump", { it.distanceMeters }) { _, meters ->
                "${formatNumber(units.heightFromMeters(meters))} ${units.lengthUnit}"
            },
            best("Most reps in a set", { it.reps?.toDouble() }) { _, reps -> "${reps.toInt()} reps" },
        )
        ExerciseType.SESSION -> listOfNotNull(
            best("Longest session", { it.durationSeconds?.toDouble() }) { _, seconds -> formatDuration(seconds.toInt()) },
            bestSession(history) { session -> session.sets.sumOf { it.values.durationSeconds ?: 0 }.toDouble() }
                ?.let { (date, seconds) -> RecordEntry("Most time in a day", formatDuration(seconds.toInt()), date) },
        )
    }
    return entries
}

private fun bestSession(
    history: List<HistorySession>,
    score: (HistorySession) -> Double,
): Pair<LocalDate, Double>? =
    history.map { it.date to score(it) }
        .filter { it.second > 0 }
        .maxWithOrNull(compareBy<Pair<LocalDate, Double>> { it.second }.thenByDescending { it.first })

/**
 * Heaviest weight lifted for at least N reps, for N = 1..[maxReps]. A set of 100 kg × 5 counts for
 * 1 to 5 reps. Weights are in kg; reps with no qualifying set are left out.
 */
fun repMaxes(history: List<HistorySession>, maxReps: Int = 12): Map<Int, Double> {
    val result = sortedMapOf<Int, Double>()
    history.flatMap { it.sets }.forEach { set ->
        val weight = set.values.weightKg ?: return@forEach
        val reps = set.values.reps ?: return@forEach
        if (weight <= 0) return@forEach
        for (n in 1..minOf(reps, maxReps)) {
            if (weight > (result[n] ?: 0.0)) result[n] = weight
        }
    }
    return result
}

/** What a progress graph shows, per session. */
enum class ProgressMetric(val label: String, val kind: MetricKind) {
    ESTIMATED_1RM("Estimated 1RM", MetricKind.WEIGHT),
    MAX_WEIGHT("Heaviest weight", MetricKind.WEIGHT),
    VOLUME("Volume (weight × reps)", MetricKind.WEIGHT),
    MAX_REPS("Most reps in a set", MetricKind.COUNT),
    TOTAL_REPS("Total reps", MetricKind.COUNT),
    DISTANCE("Distance", MetricKind.DISTANCE),
    MAX_HEIGHT("Best jump", MetricKind.HEIGHT),
    AVERAGE_RPE("Average intensity (RPE)", MetricKind.COUNT),
    LONGEST_TIME("Longest set", MetricKind.DURATION),
    DURATION("Total time", MetricKind.DURATION),
    ;

    enum class MetricKind { WEIGHT, COUNT, DISTANCE, HEIGHT, DURATION }

    /** Formats a value of this metric (stored units) for display. */
    fun format(value: Double, units: UnitSystem): String = when (kind) {
        MetricKind.WEIGHT -> "${formatNumber(units.weightFromKg(value))} ${units.weightUnit}"
        MetricKind.COUNT -> formatNumber(value)
        MetricKind.DISTANCE -> "${formatNumber(units.distanceFromMeters(value))} ${units.distanceUnit}"
        MetricKind.HEIGHT -> "${formatNumber(units.heightFromMeters(value))} ${units.lengthUnit}"
        MetricKind.DURATION -> formatDuration(value.toInt())
    }

    companion object {
        fun forType(type: ExerciseType): List<ProgressMetric> = when (type) {
            ExerciseType.WEIGHT_REPS -> listOf(ESTIMATED_1RM, MAX_WEIGHT, VOLUME, MAX_REPS)
            ExerciseType.REPS -> listOf(MAX_REPS, TOTAL_REPS)
            ExerciseType.DISTANCE_TIME -> listOf(DISTANCE, DURATION)
            ExerciseType.TIME -> listOf(LONGEST_TIME, DURATION)
            ExerciseType.TIME_WEIGHT -> listOf(LONGEST_TIME, MAX_WEIGHT)
            ExerciseType.REPS_HEIGHT -> listOf(MAX_HEIGHT, TOTAL_REPS)
            ExerciseType.SESSION -> listOf(DURATION, AVERAGE_RPE)
        }
    }
}

data class ProgressPoint(val date: LocalDate, val value: Double)

/** One point per session, oldest first; sessions where the metric is zero are skipped. */
fun progressPoints(history: List<HistorySession>, metric: ProgressMetric): List<ProgressPoint> =
    history.sortedBy { it.date }.mapNotNull { session ->
        val values = session.sets.map { it.values }
        val value = when (metric) {
            ProgressMetric.ESTIMATED_1RM ->
                values.maxOfOrNull { estimatedOneRepMax(it.weightKg ?: 0.0, it.reps ?: 0) } ?: 0.0
            ProgressMetric.MAX_WEIGHT -> values.maxOfOrNull { it.weightKg ?: 0.0 } ?: 0.0
            ProgressMetric.VOLUME -> values.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            ProgressMetric.MAX_REPS -> (values.maxOfOrNull { it.reps ?: 0 } ?: 0).toDouble()
            ProgressMetric.TOTAL_REPS -> values.sumOf { it.reps ?: 0 }.toDouble()
            ProgressMetric.DISTANCE -> values.sumOf { it.distanceMeters ?: 0.0 }
            ProgressMetric.MAX_HEIGHT -> values.maxOfOrNull { it.distanceMeters ?: 0.0 } ?: 0.0
            ProgressMetric.AVERAGE_RPE -> values.mapNotNull { it.rpe }.average().takeIf { !it.isNaN() } ?: 0.0
            ProgressMetric.LONGEST_TIME -> (values.maxOfOrNull { it.durationSeconds ?: 0 } ?: 0).toDouble()
            ProgressMetric.DURATION -> values.sumOf { it.durationSeconds ?: 0 }.toDouble()
        }
        if (value > 0) ProgressPoint(session.date, value) else null
    }
