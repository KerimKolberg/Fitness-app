package com.kerimkolberg.fitnessapp.model

import java.time.LocalDate

/** What gets recorded for each set of an exercise. */
enum class ExerciseType(val label: String) {
    WEIGHT_REPS("Weight and reps"),
    REPS("Reps only"),
    DISTANCE_TIME("Distance and time"),
    TIME("Time only");

    val usesWeight: Boolean get() = this == WEIGHT_REPS
    val usesReps: Boolean get() = this == WEIGHT_REPS || this == REPS
    val usesDistance: Boolean get() = this == DISTANCE_TIME
    val usesTime: Boolean get() = this == DISTANCE_TIME || this == TIME
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
)

/** The measured values of one set. Weight is always stored in kg and distance in meters. */
data class SetValues(
    val weightKg: Double? = null,
    val reps: Int? = null,
    val distanceMeters: Double? = null,
    val durationSeconds: Int? = null,
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
)

/** All sets of one exercise on one date, used for history and "last time" hints. */
data class HistorySession(
    val date: LocalDate,
    val sets: List<SetEntry>,
)
