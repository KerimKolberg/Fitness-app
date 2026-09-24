package com.kerimkolberg.fitnessapp.data

import com.kerimkolberg.fitnessapp.model.ExerciseType
import com.kerimkolberg.fitnessapp.model.ExerciseType.DISTANCE_TIME
import com.kerimkolberg.fitnessapp.model.ExerciseType.REPS
import com.kerimkolberg.fitnessapp.model.ExerciseType.TIME
import com.kerimkolberg.fitnessapp.model.ExerciseType.WEIGHT_REPS
import java.util.UUID

/**
 * The categories and exercises shipped with the app.
 *
 * Their ids are derived from the names, so the same built-in exercise has the same id on every
 * device. That keeps a future cloud sync from creating duplicates. A key must never change: if a
 * built-in name is ever reworded, pass its old key explicitly so the id stays the same.
 */
object BuiltInExercises {
    data class BuiltInCategory(
        val key: String,
        val name: String,
        val color: Int,
        val exercises: List<BuiltInExercise>,
    ) {
        val id: String get() = stableId("category", key)
    }

    data class BuiltInExercise(
        val key: String,
        val name: String,
        val type: ExerciseType = WEIGHT_REPS,
    ) {
        val id: String get() = stableId("exercise", key)
    }

    fun stableId(kind: String, key: String): String =
        UUID.nameUUIDFromBytes("builtin:$kind:$key".toByteArray()).toString()

    private fun exercise(
        name: String,
        type: ExerciseType = WEIGHT_REPS,
        key: String = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
    ) = BuiltInExercise(key = key, name = name, type = type)

    val categories: List<BuiltInCategory> = listOf(
        BuiltInCategory(
            key = "shoulders", name = "Shoulders", color = 0xFF8E24AA.toInt(),
            exercises = listOf(
                exercise("Overhead Press"),
                exercise("Seated Dumbbell Press"),
                exercise("Arnold Dumbbell Press"),
                exercise("Push Press"),
                exercise("Lateral Dumbbell Raise"),
                exercise("Front Dumbbell Raise"),
                exercise("Rear Delt Dumbbell Raise"),
                exercise("Face Pull"),
                exercise("Upright Barbell Row"),
            ),
        ),
        BuiltInCategory(
            key = "triceps", name = "Triceps", color = 0xFF3949AB.toInt(),
            exercises = listOf(
                exercise("Close Grip Barbell Bench Press"),
                exercise("Rope Push Down"),
                exercise("V-Bar Push Down"),
                exercise("Parallel Bar Triceps Dip"),
                exercise("Overhead Dumbbell Triceps Extension"),
                exercise("EZ-Bar Skullcrusher"),
            ),
        ),
        BuiltInCategory(
            key = "biceps", name = "Biceps", color = 0xFF039BE5.toInt(),
            exercises = listOf(
                exercise("Barbell Curl"),
                exercise("Dumbbell Curl"),
                exercise("Hammer Curl"),
                exercise("Incline Dumbbell Curl"),
                exercise("EZ-Bar Preacher Curl"),
                exercise("Cable Curl"),
                exercise("Concentration Curl"),
            ),
        ),
        BuiltInCategory(
            key = "chest", name = "Chest", color = 0xFFE53935.toInt(),
            exercises = listOf(
                exercise("Flat Barbell Bench Press"),
                exercise("Flat Dumbbell Bench Press"),
                exercise("Incline Barbell Bench Press"),
                exercise("Incline Dumbbell Bench Press"),
                exercise("Decline Barbell Bench Press"),
                exercise("Machine Chest Press"),
                exercise("Flat Dumbbell Fly"),
                exercise("Cable Crossover"),
                exercise("Push Up", REPS),
            ),
        ),
        BuiltInCategory(
            key = "back", name = "Back", color = 0xFF43A047.toInt(),
            exercises = listOf(
                exercise("Deadlift"),
                exercise("Barbell Row"),
                exercise("One-Arm Dumbbell Row"),
                exercise("T-Bar Row"),
                exercise("Seated Cable Row"),
                exercise("Lat Pulldown"),
                exercise("Pull Up", REPS),
                exercise("Chin Up", REPS),
                exercise("Rack Pull"),
                exercise("Back Extension", REPS),
            ),
        ),
        BuiltInCategory(
            key = "legs", name = "Legs", color = 0xFFFB8C00.toInt(),
            exercises = listOf(
                exercise("Barbell Squat"),
                exercise("Front Squat"),
                exercise("Romanian Deadlift"),
                exercise("Leg Press"),
                exercise("Leg Extension Machine"),
                exercise("Seated Leg Curl Machine"),
                exercise("Lying Leg Curl Machine"),
                exercise("Bulgarian Split Squat"),
                exercise("Walking Lunge"),
                exercise("Barbell Hip Thrust"),
                exercise("Standing Calf Raise"),
                exercise("Seated Calf Raise"),
            ),
        ),
        BuiltInCategory(
            key = "abs", name = "Abs", color = 0xFF00897B.toInt(),
            exercises = listOf(
                exercise("Crunch", REPS),
                exercise("Hanging Leg Raise", REPS),
                exercise("Cable Crunch"),
                exercise("Ab Wheel Rollout", REPS),
                exercise("Plank", TIME),
                exercise("Side Plank", TIME),
            ),
        ),
        BuiltInCategory(
            key = "cardio", name = "Cardio", color = 0xFF6D4C41.toInt(),
            exercises = listOf(
                exercise("Running", DISTANCE_TIME),
                exercise("Walking", DISTANCE_TIME),
                exercise("Cycling", DISTANCE_TIME),
                exercise("Rowing Machine", DISTANCE_TIME),
                exercise("Elliptical Trainer", DISTANCE_TIME),
                exercise("Swimming", DISTANCE_TIME),
                exercise("Stair Climber", TIME),
                exercise("Jump Rope", TIME),
            ),
        ),
    )
}
