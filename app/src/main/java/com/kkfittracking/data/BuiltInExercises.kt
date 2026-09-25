package com.kkfittracking.data

import com.kkfittracking.model.ExerciseType
import com.kkfittracking.model.ExerciseType.DISTANCE_TIME
import com.kkfittracking.model.ExerciseType.REPS
import com.kkfittracking.model.ExerciseType.REPS_HEIGHT
import com.kkfittracking.model.ExerciseType.SESSION
import com.kkfittracking.model.ExerciseType.TIME
import com.kkfittracking.model.ExerciseType.TIME_WEIGHT
import com.kkfittracking.model.ExerciseType.WEIGHT_REPS
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
        val tempo: String = "",
        val perSide: Boolean = false,
    ) {
        val id: String get() = stableId("exercise", key)
    }

    fun stableId(kind: String, key: String): String =
        UUID.nameUUIDFromBytes("builtin:$kind:$key".toByteArray()).toString()

    /** The key of a built-in exercise, derived from its name. */
    fun keyOf(name: String): String = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private fun exercise(
        name: String,
        type: ExerciseType = WEIGHT_REPS,
        tempo: String = "",
        perSide: Boolean = false,
        key: String = keyOf(name),
    ) = BuiltInExercise(key = key, name = name, type = type, tempo = tempo, perSide = perSide)

    // Category keys that other features (achievements, starter plans) refer to.
    const val MOBILITY = "mobility"
    const val STRETCHING = "stretching"
    const val ISOMETRICS = "isometrics"
    const val TENDONS = "tendons"
    const val PLYOMETRICS = "plyometrics"
    const val SPORTS = "sports"

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
        BuiltInCategory(
            key = MOBILITY, name = "Mobility", color = 0xFF00ACC1.toInt(),
            exercises = listOf(
                exercise("Hip CARs", REPS, perSide = true),
                exercise("Shoulder CARs", REPS, perSide = true),
                exercise("Thoracic Rotation", REPS, perSide = true),
                exercise("Ankle Dorsiflexion Rocks", REPS, perSide = true),
                exercise("90/90 Hip Switches", REPS),
                exercise("Cat-Cow", REPS),
                exercise("World's Greatest Stretch", REPS, perSide = true),
                exercise("Deep Squat Hold", TIME),
            ),
        ),
        BuiltInCategory(
            key = STRETCHING, name = "Stretching", color = 0xFF7CB342.toInt(),
            exercises = listOf(
                exercise("Hamstring Stretch", TIME, perSide = true),
                exercise("Hip Flexor Stretch", TIME, perSide = true),
                exercise("Couch Stretch", TIME, perSide = true),
                exercise("Pigeon Stretch", TIME, perSide = true),
                exercise("Calf Stretch", TIME, perSide = true),
                exercise("Doorway Chest Stretch", TIME),
                exercise("Child's Pose", TIME),
                exercise("Dead Hang", TIME),
            ),
        ),
        BuiltInCategory(
            key = ISOMETRICS, name = "Isometrics", color = 0xFF5E35B1.toInt(),
            exercises = listOf(
                exercise("Wall Sit", TIME_WEIGHT),
                exercise("Spanish Squat Hold", TIME_WEIGHT),
                exercise("Split Squat Hold", TIME_WEIGHT, perSide = true),
                exercise("Copenhagen Plank", TIME, perSide = true),
                exercise("Isometric Mid-Thigh Pull", TIME_WEIGHT),
                exercise("Glute Bridge Hold", TIME_WEIGHT),
                exercise("Single-Leg Calf Raise Hold", TIME_WEIGHT, perSide = true),
                exercise("Hollow Body Hold", TIME),
            ),
        ),
        BuiltInCategory(
            key = TENDONS, name = "Tendons & eccentrics", color = 0xFFD81B60.toInt(),
            exercises = listOf(
                exercise("Nordic Hamstring Curl", REPS, tempo = "5-0-1-0"),
                exercise("Eccentric Heel Drop", WEIGHT_REPS, tempo = "3-0-1-0", perSide = true),
                exercise("Decline Board Squat", WEIGHT_REPS, tempo = "3-0-1-0"),
                exercise("Heavy Slow Leg Press", WEIGHT_REPS, tempo = "3-0-3-0"),
                exercise("Tyler Twist", REPS, tempo = "3-0-1-0", perSide = true),
                exercise("Eccentric Wrist Extension", WEIGHT_REPS, tempo = "3-0-1-0", perSide = true),
                exercise("Reverse Nordic Curl", REPS, tempo = "3-0-1-0"),
                exercise("Tibialis Raise", WEIGHT_REPS),
            ),
        ),
        BuiltInCategory(
            key = PLYOMETRICS, name = "Plyometrics", color = 0xFFF4511E.toInt(),
            exercises = listOf(
                exercise("Box Jump", REPS_HEIGHT),
                exercise("Broad Jump", REPS_HEIGHT),
                exercise("Depth Jump", REPS_HEIGHT),
                exercise("Lateral Bound", REPS_HEIGHT, perSide = true),
                exercise("Pogo Hops", REPS),
                exercise("Tuck Jump", REPS),
                exercise("Skater Jump", REPS),
                exercise("Medicine Ball Slam", WEIGHT_REPS),
            ),
        ),
        BuiltInCategory(
            key = SPORTS, name = "Sports", color = 0xFF546E7A.toInt(),
            exercises = listOf(
                exercise("Tennis", SESSION),
                exercise("Table Tennis", SESSION),
                exercise("Volleyball", SESSION),
                exercise("Padel", SESSION),
                exercise("Badminton", SESSION),
                exercise("Squash", SESSION),
                exercise("Football", SESSION),
                exercise("Basketball", SESSION),
                exercise("Climbing", SESSION),
                exercise("Martial Arts", SESSION),
            ),
        ),
    )
}
