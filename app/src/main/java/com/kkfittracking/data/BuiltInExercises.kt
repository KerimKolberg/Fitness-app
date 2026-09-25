package com.kkfittracking.data

import com.kkfittracking.model.ExercisePlan
import com.kkfittracking.model.ExerciseType
import com.kkfittracking.model.ExerciseType.DISTANCE_TIME
import com.kkfittracking.model.ExerciseType.INTERVALS
import com.kkfittracking.model.ExerciseType.REPS
import com.kkfittracking.model.ExerciseType.REPS_HEIGHT
import com.kkfittracking.model.ExerciseType.SESSION
import com.kkfittracking.model.ExerciseType.TIME
import com.kkfittracking.model.ExerciseType.TIME_WEIGHT
import com.kkfittracking.model.ExerciseType.WEIGHT_REPS
import com.kkfittracking.model.Muscle
import com.kkfittracking.model.Muscle.ABS
import com.kkfittracking.model.Muscle.ADDUCTORS
import com.kkfittracking.model.Muscle.BICEPS
import com.kkfittracking.model.Muscle.CALVES
import com.kkfittracking.model.Muscle.CHEST
import com.kkfittracking.model.Muscle.FOREARMS
import com.kkfittracking.model.Muscle.FRONT_DELTS
import com.kkfittracking.model.Muscle.FULL_BODY
import com.kkfittracking.model.Muscle.GLUTES
import com.kkfittracking.model.Muscle.HAMSTRINGS
import com.kkfittracking.model.Muscle.HIPS
import com.kkfittracking.model.Muscle.LATS
import com.kkfittracking.model.Muscle.LOWER_BACK
import com.kkfittracking.model.Muscle.OBLIQUES
import com.kkfittracking.model.Muscle.QUADS
import com.kkfittracking.model.Muscle.REAR_DELTS
import com.kkfittracking.model.Muscle.ROTATOR_CUFF
import com.kkfittracking.model.Muscle.SHINS
import com.kkfittracking.model.Muscle.SIDE_DELTS
import com.kkfittracking.model.Muscle.TRICEPS
import com.kkfittracking.model.Muscle.UPPER_BACK
import com.kkfittracking.model.Muscle.WHOLE_LEGS
import com.kkfittracking.model.Regions
import com.kkfittracking.model.TrainingStyle
import com.kkfittracking.model.TrainingStyle.CARDIO
import com.kkfittracking.model.TrainingStyle.ECCENTRIC
import com.kkfittracking.model.TrainingStyle.HIIT
import com.kkfittracking.model.TrainingStyle.ISOMETRIC
import com.kkfittracking.model.TrainingStyle.MOBILITY
import com.kkfittracking.model.TrainingStyle.PLYOMETRIC
import com.kkfittracking.model.TrainingStyle.SPORT
import com.kkfittracking.model.TrainingStyle.STRENGTH
import com.kkfittracking.model.TrainingStyle.STRETCHING
import java.util.UUID

/**
 * The body sections and exercises shipped with the app. The library has three levels: section
 * (Legs), muscle (Hamstrings) and training style (Eccentric), e.g. Legs → Hamstrings → Eccentric →
 * Nordic Hamstring Curl.
 *
 * Ids are derived from the names, so the same built-in exercise has the same id on every device.
 * That keeps a future cloud sync from creating duplicates. A key must never change: if a built-in
 * name is ever reworded, pass its old key explicitly so the id stays the same.
 */
object BuiltInExercises {
    data class Region(val key: String, val name: String, val color: Int) {
        val id: String get() = stableId("category", key)
    }

    data class BuiltInExercise(
        val key: String,
        val name: String,
        val muscle: Muscle,
        val style: TrainingStyle,
        val type: ExerciseType = WEIGHT_REPS,
        val tempo: String = "",
        val perSide: Boolean = false,
        /** The plan a new install starts with, e.g. the interval timings of Tabata. */
        val plan: ExercisePlan = ExercisePlan(),
    ) {
        val id: String get() = stableId("exercise", key)
        val regionKey: String get() = muscle.regionKey
    }

    /**
     * A category of earlier versions, now split over the sections. Built-in exercises move to their
     * place in the catalog; the user's own exercises move to this fallback.
     */
    data class RetiredCategory(val key: String, val muscle: Muscle, val style: TrainingStyle) {
        val id: String get() = stableId("category", key)
    }

    fun stableId(kind: String, key: String): String =
        UUID.nameUUIDFromBytes("builtin:$kind:$key".toByteArray()).toString()

    /** The key of a built-in exercise, derived from its name. */
    fun keyOf(name: String): String = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    val regions: List<Region> = listOf(
        Region(Regions.CHEST, "Chest", 0xFFE53935.toInt()),
        Region(Regions.BACK, "Back", 0xFF43A047.toInt()),
        Region(Regions.SHOULDERS, "Shoulders", 0xFF8E24AA.toInt()),
        Region(Regions.ARMS, "Arms", 0xFF3949AB.toInt()),
        Region(Regions.LEGS, "Legs", 0xFFFB8C00.toInt()),
        Region(Regions.CORE, "Core", 0xFF00897B.toInt()),
        Region(Regions.FULL_BODY, "Full body", 0xFF00ACC1.toInt()),
        Region(Regions.CARDIO, "Cardio", 0xFF6D4C41.toInt()),
        Region(Regions.SPORTS, "Sports", 0xFF546E7A.toInt()),
    )

    val retiredCategories: List<RetiredCategory> = listOf(
        RetiredCategory("triceps", TRICEPS, STRENGTH),
        RetiredCategory("biceps", BICEPS, STRENGTH),
        RetiredCategory("mobility", FULL_BODY, MOBILITY),
        RetiredCategory("stretching", FULL_BODY, STRETCHING),
        RetiredCategory("isometrics", FULL_BODY, ISOMETRIC),
        RetiredCategory("tendons", FULL_BODY, ECCENTRIC),
        RetiredCategory("plyometrics", FULL_BODY, PLYOMETRIC),
    )

    private class Entry(
        val name: String,
        val type: ExerciseType,
        val tempo: String,
        val perSide: Boolean,
        val plan: ExercisePlan,
    )

    private fun e(
        name: String,
        type: ExerciseType = WEIGHT_REPS,
        tempo: String = "",
        perSide: Boolean = false,
        plan: ExercisePlan = ExercisePlan(),
    ) = Entry(name, type, tempo, perSide, plan)

    private fun intervals(high: Int, low: Int, rounds: Int) = ExercisePlan(highSeconds = high, lowSeconds = low, rounds = rounds)

    private fun MutableList<BuiltInExercise>.group(muscle: Muscle, style: TrainingStyle, vararg entries: Entry) {
        entries.forEach {
            add(BuiltInExercise(keyOf(it.name), it.name, muscle, style, it.type, it.tempo, it.perSide, it.plan))
        }
    }

    val exercises: List<BuiltInExercise> = buildList {
        // Chest
        group(
            CHEST, STRENGTH,
            e("Flat Barbell Bench Press"), e("Flat Dumbbell Bench Press"), e("Incline Barbell Bench Press"),
            e("Incline Dumbbell Bench Press"), e("Decline Barbell Bench Press"), e("Machine Chest Press"),
            e("Flat Dumbbell Fly"), e("Cable Crossover"), e("Push Up", REPS),
        )
        group(CHEST, ISOMETRIC, e("Push-Up Hold", TIME))
        group(CHEST, ECCENTRIC, e("Eccentric Push Up", REPS, tempo = "4-0-1-0"))
        group(CHEST, PLYOMETRIC, e("Clap Push Up", REPS))
        group(CHEST, STRETCHING, e("Doorway Chest Stretch", TIME))

        // Back
        group(LATS, STRENGTH, e("Lat Pulldown"), e("Pull Up", REPS), e("Chin Up", REPS), e("One-Arm Dumbbell Row"))
        group(LATS, ISOMETRIC, e("Flexed-Arm Hang", TIME))
        group(LATS, ECCENTRIC, e("Eccentric Pull Up", REPS, tempo = "5-0-1-0"))
        group(LATS, STRETCHING, e("Dead Hang", TIME))
        group(UPPER_BACK, STRENGTH, e("Barbell Row"), e("T-Bar Row"), e("Seated Cable Row"))
        group(UPPER_BACK, MOBILITY, e("Thoracic Rotation", REPS, perSide = true))
        group(LOWER_BACK, STRENGTH, e("Deadlift"), e("Rack Pull"), e("Back Extension", REPS))
        group(LOWER_BACK, ISOMETRIC, e("Superman Hold", TIME))
        group(LOWER_BACK, MOBILITY, e("Cat-Cow", REPS))
        group(LOWER_BACK, STRETCHING, e("Child's Pose", TIME))

        // Shoulders
        group(
            FRONT_DELTS, STRENGTH,
            e("Overhead Press"), e("Seated Dumbbell Press"), e("Arnold Dumbbell Press"), e("Push Press"),
            e("Front Dumbbell Raise"),
        )
        group(SIDE_DELTS, STRENGTH, e("Lateral Dumbbell Raise"), e("Upright Barbell Row"))
        group(SIDE_DELTS, ISOMETRIC, e("Lateral Raise Hold", TIME_WEIGHT))
        group(REAR_DELTS, STRENGTH, e("Rear Delt Dumbbell Raise"), e("Face Pull"))
        group(REAR_DELTS, STRETCHING, e("Cross-Body Shoulder Stretch", TIME, perSide = true))
        group(ROTATOR_CUFF, STRENGTH, e("Cable External Rotation", perSide = true))
        group(ROTATOR_CUFF, MOBILITY, e("Shoulder CARs", REPS, perSide = true))

        // Arms
        group(
            BICEPS, STRENGTH,
            e("Barbell Curl"), e("Dumbbell Curl"), e("Hammer Curl"), e("Incline Dumbbell Curl"),
            e("EZ-Bar Preacher Curl"), e("Cable Curl"), e("Concentration Curl"),
        )
        group(BICEPS, ISOMETRIC, e("Isometric Curl Hold", TIME_WEIGHT))
        group(BICEPS, ECCENTRIC, e("Eccentric Barbell Curl", tempo = "4-0-1-0"))
        group(
            TRICEPS, STRENGTH,
            e("Close Grip Barbell Bench Press"), e("Rope Push Down"), e("V-Bar Push Down"),
            e("Parallel Bar Triceps Dip"), e("Overhead Dumbbell Triceps Extension"), e("EZ-Bar Skullcrusher"),
        )
        group(
            FOREARMS, ECCENTRIC,
            e("Tyler Twist", REPS, tempo = "3-0-1-0", perSide = true),
            e("Eccentric Wrist Extension", tempo = "3-0-1-0", perSide = true),
        )

        // Legs
        group(
            QUADS, STRENGTH,
            e("Barbell Squat"), e("Front Squat"), e("Leg Press"), e("Leg Extension Machine"),
            e("Bulgarian Split Squat"), e("Walking Lunge"),
        )
        group(
            QUADS, ISOMETRIC,
            e("Wall Sit", TIME_WEIGHT), e("Spanish Squat Hold", TIME_WEIGHT),
            e("Split Squat Hold", TIME_WEIGHT, perSide = true), e("Isometric Leg Extension Hold", TIME_WEIGHT),
        )
        group(
            QUADS, ECCENTRIC,
            e("Decline Board Squat", tempo = "3-0-1-0"), e("Heavy Slow Leg Press", tempo = "3-0-3-0"),
            e("Reverse Nordic Curl", REPS, tempo = "3-0-1-0"), e("Eccentric Leg Extension", tempo = "4-0-1-0"),
        )
        group(QUADS, PLYOMETRIC, e("Jump Squat", REPS))
        group(QUADS, STRETCHING, e("Couch Stretch", TIME, perSide = true))
        group(HAMSTRINGS, STRENGTH, e("Romanian Deadlift"), e("Seated Leg Curl Machine"), e("Lying Leg Curl Machine"))
        group(HAMSTRINGS, ISOMETRIC, e("Hamstring Bridge Hold", TIME))
        group(HAMSTRINGS, ECCENTRIC, e("Nordic Hamstring Curl", REPS, tempo = "5-0-1-0"))
        group(HAMSTRINGS, STRETCHING, e("Hamstring Stretch", TIME, perSide = true))
        group(GLUTES, STRENGTH, e("Barbell Hip Thrust"))
        group(GLUTES, ISOMETRIC, e("Glute Bridge Hold", TIME_WEIGHT))
        group(GLUTES, STRETCHING, e("Pigeon Stretch", TIME, perSide = true))
        group(HIPS, MOBILITY, e("Hip CARs", REPS, perSide = true), e("90/90 Hip Switches", REPS), e("Deep Squat Hold", TIME))
        group(HIPS, STRETCHING, e("Hip Flexor Stretch", TIME, perSide = true))
        group(ADDUCTORS, STRENGTH, e("Adductor Machine"))
        group(ADDUCTORS, ISOMETRIC, e("Copenhagen Plank", TIME, perSide = true))
        group(CALVES, STRENGTH, e("Standing Calf Raise"), e("Seated Calf Raise"))
        group(CALVES, ISOMETRIC, e("Single-Leg Calf Raise Hold", TIME_WEIGHT, perSide = true))
        group(CALVES, ECCENTRIC, e("Eccentric Heel Drop", tempo = "3-0-1-0", perSide = true))
        group(CALVES, PLYOMETRIC, e("Pogo Hops", REPS))
        group(CALVES, MOBILITY, e("Ankle Dorsiflexion Rocks", REPS, perSide = true))
        group(CALVES, STRETCHING, e("Calf Stretch", TIME, perSide = true))
        group(SHINS, STRENGTH, e("Tibialis Raise"))
        group(
            WHOLE_LEGS, PLYOMETRIC,
            e("Box Jump", REPS_HEIGHT), e("Broad Jump", REPS_HEIGHT), e("Depth Jump", REPS_HEIGHT),
            e("Lateral Bound", REPS_HEIGHT, perSide = true), e("Tuck Jump", REPS), e("Skater Jump", REPS),
        )

        // Core
        group(
            ABS, STRENGTH,
            e("Crunch", REPS), e("Hanging Leg Raise", REPS), e("Cable Crunch"), e("Ab Wheel Rollout", REPS),
            e("Dead Bug", REPS),
        )
        group(ABS, ISOMETRIC, e("Plank", TIME), e("Hollow Body Hold", TIME))
        group(ABS, STRETCHING, e("Cobra Stretch", TIME))
        group(OBLIQUES, ISOMETRIC, e("Side Plank", TIME), e("Pallof Press Hold", TIME, perSide = true))

        // Full body
        group(FULL_BODY, STRENGTH, e("Kettlebell Swing"), e("Farmer's Carry", TIME_WEIGHT))
        group(FULL_BODY, ISOMETRIC, e("Isometric Mid-Thigh Pull", TIME_WEIGHT))
        group(FULL_BODY, PLYOMETRIC, e("Medicine Ball Slam"))
        group(FULL_BODY, MOBILITY, e("World's Greatest Stretch", REPS, perSide = true))

        // Cardio
        group(
            Muscle.CARDIO, CARDIO,
            e("Running", DISTANCE_TIME), e("Walking", DISTANCE_TIME), e("Cycling", DISTANCE_TIME),
            e("Rowing Machine", DISTANCE_TIME), e("Elliptical Trainer", DISTANCE_TIME), e("Swimming", DISTANCE_TIME),
            e("Stair Climber", TIME), e("Jump Rope", TIME),
        )

        // Sports
        group(
            Muscle.SPORT, SPORT,
            e("Tennis", SESSION), e("Table Tennis", SESSION), e("Volleyball", SESSION), e("Padel", SESSION),
            e("Badminton", SESSION), e("Squash", SESSION), e("Football", SESSION), e("Basketball", SESSION),
            e("Climbing", SESSION), e("Martial Arts", SESSION),
        )
        group(
            Muscle.SPORT, HIIT,
            e("HIIT Intervals", INTERVALS, plan = intervals(high = 30, low = 30, rounds = 10)),
            e("Tabata", INTERVALS, plan = intervals(high = 20, low = 10, rounds = 8)),
            e("Sprint Intervals", INTERVALS, plan = intervals(high = 15, low = 45, rounds = 10)),
        )
    }

    private val regionKeysById: Map<String, String> = regions.associate { it.id to it.key }
    private val retiredById: Map<String, RetiredCategory> = retiredCategories.associateBy { it.id }
    private val exercisesById: Map<String, BuiltInExercise> = exercises.associateBy { it.id }

    /** The section key of a category id, or null for a category that is not a built-in section. */
    fun regionKeyOf(categoryId: String): String? = regionKeysById[categoryId]

    fun retired(categoryId: String): RetiredCategory? = retiredById[categoryId]

    fun exercise(id: String): BuiltInExercise? = exercisesById[id]
}
