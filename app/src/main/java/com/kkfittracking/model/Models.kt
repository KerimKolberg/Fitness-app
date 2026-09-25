package com.kkfittracking.model

import java.time.LocalDate

/** What gets recorded for each set of an exercise. The name is stored in the database: never rename one. */
enum class ExerciseType(val label: String) {
    WEIGHT_REPS("Weight and reps"),
    REPS("Reps only"),
    DISTANCE_TIME("Distance and time"),
    TIME("Time only"),
    TIME_WEIGHT("Time and weight (loaded holds)"),
    REPS_HEIGHT("Reps and height or distance (jumps)"),
    SESSION("Session time and intensity (sports)"),
    INTERVALS("Intervals (HIIT): rounds, time and intensity");

    val usesWeight: Boolean get() = this == WEIGHT_REPS || this == TIME_WEIGHT
    /** Reps, or rounds for [INTERVALS], stored in [SetValues.reps]. */
    val usesReps: Boolean get() = this == WEIGHT_REPS || this == REPS || this == REPS_HEIGHT || this == INTERVALS
    val usesDistance: Boolean get() = this == DISTANCE_TIME
    /** Jump height or distance, stored in [SetValues.distanceMeters] and shown in cm or inches. */
    val usesHeight: Boolean get() = this == REPS_HEIGHT
    val usesTime: Boolean get() =
        this == DISTANCE_TIME || this == TIME || this == TIME_WEIGHT || this == SESSION || this == INTERVALS
    /** Intensity (RPE 1-10) and a note per entry. */
    val usesIntensity: Boolean get() = this == SESSION || this == INTERVALS

    /** A whole session is logged at once, so there is no rest between sets and no set plan. */
    val isSession: Boolean get() = this == SESSION || this == INTERVALS

    /** What the reps field counts. */
    val repsLabel: String get() = if (this == INTERVALS) "Rounds" else "Reps"
}

data class Category(
    val id: String,
    val name: String,
    val color: Int,
    /** The body section key (see [Regions]) for built-in categories, null for others. */
    val key: String? = null,
)

data class Exercise(
    val id: String,
    val name: String,
    val categoryId: String,
    val type: ExerciseType,
    /** How to do it, shown before the video links. */
    val notes: String,
    val isCustom: Boolean,
    /** Optional lifting tempo such as "5-0-1-0": seconds down, pause, up, pause. */
    val tempo: String = "",
    /** Each side is trained separately, so a set is logged per side. */
    val perSide: Boolean = false,
    val muscle: Muscle = Muscle.OTHER,
    val style: TrainingStyle = TrainingStyle.STRENGTH,
    /** Planned sets, reps, rest, drop sets and interval timings. */
    val plan: ExercisePlan = ExercisePlan(),
    /** Videos and pages showing how the exercise is done. */
    val links: List<ExerciseLink> = emptyList(),
)

/**
 * The measured values of one set. Weight is always stored in kg and distance (or jump height) in
 * meters. [rpe] is the effort from 1 (very easy) to 10 (maximal).
 */
data class SetValues(
    val weightKg: Double? = null,
    val reps: Int? = null,
    val distanceMeters: Double? = null,
    val durationSeconds: Int? = null,
    val rpe: Int? = null,
    val note: String = "",
    /** Done right after the previous set with less weight, without rest. */
    val isDropSet: Boolean = false,
)

data class SetEntry(
    val id: String,
    val values: SetValues,
)

/** One exercise logged on a given day, with its sets in order. */
data class DayExercise(
    val workoutExerciseId: String,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseType: ExerciseType,
    val categoryColor: Int,
    val sets: List<SetEntry>,
    /** Exercises of the same day with the same id are done as a superset. */
    val supersetId: String? = null,
    /** Seconds to get to the next exercise of the superset. */
    val transitionSeconds: Int? = null,
    /** Seconds of rest after each round of the superset. */
    val roundRestSeconds: Int? = null,
)

/** An exercise to put on a day, from a plan or an earlier day, with its superset if it is in one. */
data class PlannedExercise(
    val exerciseId: String,
    val supersetId: String? = null,
    val transitionSeconds: Int? = null,
    val roundRestSeconds: Int? = null,
)

/** Supersets can hold at most this many exercises. */
const val MAX_SUPERSET_SIZE = 6

/** All sets of one exercise on one date, used for history and "last time" hints. */
data class HistorySession(
    val date: LocalDate,
    val sets: List<SetEntry>,
)
