package com.kerimkolberg.fitnessapp.model

import java.time.LocalDate

/** What gets recorded for each set of an exercise. The name is stored in the database: never rename one. */
enum class ExerciseType(val label: String) {
    WEIGHT_REPS("Weight and reps"),
    REPS("Reps only"),
    DISTANCE_TIME("Distance and time"),
    TIME("Time only"),
    TIME_WEIGHT("Time and weight (loaded holds)"),
    REPS_HEIGHT("Reps and height or distance (jumps)"),
    SESSION("Session time and intensity (sports)");

    val usesWeight: Boolean get() = this == WEIGHT_REPS || this == TIME_WEIGHT
    val usesReps: Boolean get() = this == WEIGHT_REPS || this == REPS || this == REPS_HEIGHT
    val usesDistance: Boolean get() = this == DISTANCE_TIME
    /** Jump height or distance, stored in [SetValues.distanceMeters] and shown in cm or inches. */
    val usesHeight: Boolean get() = this == REPS_HEIGHT
    val usesTime: Boolean get() = this == DISTANCE_TIME || this == TIME || this == TIME_WEIGHT || this == SESSION
    /** Intensity (RPE 1-10) and a note per entry. */
    val usesIntensity: Boolean get() = this == SESSION
}

data class Category(
    val id: String,
    val name: String,
    val color: Int,
)

data class Exercise(
    val id: String,
    val name: String,
    val categoryId: String,
    val type: ExerciseType,
    val notes: String,
    val isCustom: Boolean,
    /** Optional lifting tempo such as "5-0-1-0": seconds down, pause, up, pause. */
    val tempo: String = "",
    /** Each side is trained separately, so a set is logged per side. */
    val perSide: Boolean = false,
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
    val dropSetMode: DropSetMode = DropSetMode.NONE,
    /** The number of normal sets planned, used to know when the "last set" drop set comes. */
    val plannedSets: Int? = null,
)

/** How drop sets are planned for an exercise on a day. [code] is stored in the database. */
enum class DropSetMode(val code: Int, val label: String) {
    NONE(0, "No drop sets"),
    LAST_SET(1, "Drop set on the last set"),
    EVERY_SET(2, "Every set after the first is a drop set");

    companion object {
        fun of(code: Int?): DropSetMode = entries.firstOrNull { it.code == code } ?: NONE
    }
}

/** Supersets can hold at most this many exercises. */
const val MAX_SUPERSET_SIZE = 6

/** All sets of one exercise on one date, used for history and "last time" hints. */
data class HistorySession(
    val date: LocalDate,
    val sets: List<SetEntry>,
)
