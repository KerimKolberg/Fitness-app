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
        /** Every muscle it trains, the main one first. */
        val muscles: List<Muscle>,
        val style: TrainingStyle,
        val type: ExerciseType = WEIGHT_REPS,
        val tempo: String = "",
        val perSide: Boolean = false,
        /** The plan a new install starts with, e.g. the interval timings of Tabata. */
        val plan: ExercisePlan = ExercisePlan(),
    ) {
        val id: String get() = stableId("exercise", key)
        val muscle: Muscle get() = muscles.first()

        /** The body section it is filed in: that of its main muscle. */
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
        Region(Regions.FULL_BODY, "Full body", 0xFF1E88E5.toInt()),
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
        val also: List<Muscle>,
    )

    /** [also] lists the other muscles it trains, besides the main one of its group. */
    private fun e(
        name: String,
        type: ExerciseType = WEIGHT_REPS,
        tempo: String = "",
        perSide: Boolean = false,
        plan: ExercisePlan = ExercisePlan(),
        also: List<Muscle> = emptyList(),
    ) = Entry(name, type, tempo, perSide, plan, also)

    private fun intervals(high: Int, low: Int, rounds: Int) =
        ExercisePlan(highSeconds = high, lowSeconds = low, rounds = rounds)

    /** Exercises whose main muscle is [muscle], trained the [style] way. */
    private fun MutableList<BuiltInExercise>.group(muscle: Muscle, style: TrainingStyle, vararg entries: Entry) {
        entries.forEach {
            val muscles = (listOf(muscle) + it.also).distinct()
            add(BuiltInExercise(keyOf(it.name), it.name, muscles, style, it.type, it.tempo, it.perSide, it.plan))
        }
    }

    val exercises: List<BuiltInExercise> = buildList {
        // Chest
        group(
            CHEST, STRENGTH,
            e("Flat Barbell Bench Press", also = listOf(TRICEPS, FRONT_DELTS)),
            e("Flat Dumbbell Bench Press", also = listOf(TRICEPS, FRONT_DELTS)),
            e("Incline Barbell Bench Press", also = listOf(FRONT_DELTS, TRICEPS)),
            e("Incline Dumbbell Bench Press", also = listOf(FRONT_DELTS, TRICEPS)),
            e("Decline Barbell Bench Press", also = listOf(TRICEPS)),
            e("Machine Chest Press", also = listOf(TRICEPS, FRONT_DELTS)), e("Flat Dumbbell Fly"), e("Cable Crossover"),
            e("Push Up", REPS, also = listOf(TRICEPS, FRONT_DELTS)),
        )
        group(CHEST, ISOMETRIC, e("Push-Up Hold", TIME, also = listOf(TRICEPS, ABS)))
        group(CHEST, ECCENTRIC, e("Eccentric Push Up", REPS, tempo = "4-0-1-0", also = listOf(TRICEPS, FRONT_DELTS)))
        group(CHEST, PLYOMETRIC, e("Clap Push Up", REPS, also = listOf(TRICEPS, FRONT_DELTS)))
        group(CHEST, STRETCHING, e("Doorway Chest Stretch", TIME, also = listOf(FRONT_DELTS)))

        // Back
        group(
            LATS, STRENGTH,
            e("Lat Pulldown", also = listOf(BICEPS)), e("Pull Up", REPS, also = listOf(BICEPS, UPPER_BACK)),
            e("Chin Up", REPS, also = listOf(BICEPS)), e("One-Arm Dumbbell Row", also = listOf(UPPER_BACK, BICEPS)),
        )
        group(LATS, ISOMETRIC, e("Flexed-Arm Hang", TIME, also = listOf(BICEPS, FOREARMS)))
        group(LATS, ECCENTRIC, e("Eccentric Pull Up", REPS, tempo = "5-0-1-0", also = listOf(BICEPS)))
        group(LATS, STRETCHING, e("Dead Hang", TIME, also = listOf(FOREARMS)))
        group(
            UPPER_BACK, STRENGTH,
            e("Barbell Row", also = listOf(LATS, BICEPS)), e("T-Bar Row", also = listOf(LATS)),
            e("Seated Cable Row", also = listOf(LATS, BICEPS)),
        )
        group(UPPER_BACK, MOBILITY, e("Thoracic Rotation", REPS, perSide = true))
        group(
            LOWER_BACK, STRENGTH,
            e("Deadlift", also = listOf(HAMSTRINGS, GLUTES, UPPER_BACK, FOREARMS)),
            e("Rack Pull", also = listOf(UPPER_BACK, GLUTES, FOREARMS)),
            e("Back Extension", REPS, also = listOf(GLUTES, HAMSTRINGS)),
        )
        group(LOWER_BACK, ISOMETRIC, e("Superman Hold", TIME, also = listOf(GLUTES)))
        group(LOWER_BACK, MOBILITY, e("Cat-Cow", REPS, also = listOf(UPPER_BACK)))
        group(LOWER_BACK, STRETCHING, e("Child's Pose", TIME, also = listOf(LATS)))

        // Shoulders
        group(
            FRONT_DELTS, STRENGTH,
            e("Overhead Press", also = listOf(TRICEPS, SIDE_DELTS)),
            e("Seated Dumbbell Press", also = listOf(TRICEPS, SIDE_DELTS)),
            e("Arnold Dumbbell Press", also = listOf(SIDE_DELTS, TRICEPS)),
            e("Push Press", also = listOf(TRICEPS, WHOLE_LEGS)), e("Front Dumbbell Raise"),
        )
        group(SIDE_DELTS, STRENGTH, e("Lateral Dumbbell Raise"), e("Upright Barbell Row", also = listOf(UPPER_BACK)))
        group(SIDE_DELTS, ISOMETRIC, e("Lateral Raise Hold", TIME_WEIGHT))
        group(
            REAR_DELTS, STRENGTH,
            e("Rear Delt Dumbbell Raise", also = listOf(UPPER_BACK)),
            e("Face Pull", also = listOf(UPPER_BACK, ROTATOR_CUFF)),
        )
        group(REAR_DELTS, STRETCHING, e("Cross-Body Shoulder Stretch", TIME, perSide = true, also = listOf(UPPER_BACK)))
        group(ROTATOR_CUFF, STRENGTH, e("Cable External Rotation", perSide = true))
        group(ROTATOR_CUFF, MOBILITY, e("Shoulder CARs", REPS, perSide = true))

        // Arms
        group(
            BICEPS, STRENGTH,
            e("Barbell Curl"), e("Dumbbell Curl"), e("Hammer Curl", also = listOf(FOREARMS)),
            e("Incline Dumbbell Curl"), e("EZ-Bar Preacher Curl"), e("Cable Curl"), e("Concentration Curl"),
        )
        group(BICEPS, ISOMETRIC, e("Isometric Curl Hold", TIME_WEIGHT))
        group(BICEPS, ECCENTRIC, e("Eccentric Barbell Curl", tempo = "4-0-1-0"))
        group(
            TRICEPS, STRENGTH,
            e("Close Grip Barbell Bench Press", also = listOf(CHEST, FRONT_DELTS)), e("Rope Push Down"),
            e("V-Bar Push Down"), e("Parallel Bar Triceps Dip", also = listOf(CHEST, FRONT_DELTS)),
            e("Overhead Dumbbell Triceps Extension"), e("EZ-Bar Skullcrusher"),
        )
        group(
            FOREARMS, ECCENTRIC,
            e("Tyler Twist", REPS, tempo = "3-0-1-0", perSide = true),
            e("Eccentric Wrist Extension", tempo = "3-0-1-0", perSide = true),
        )

        // Legs
        group(
            QUADS, STRENGTH,
            e("Barbell Squat", also = listOf(GLUTES, LOWER_BACK)), e("Front Squat", also = listOf(GLUTES)),
            e("Leg Press", also = listOf(GLUTES)), e("Leg Extension Machine"),
            e("Bulgarian Split Squat", also = listOf(GLUTES)), e("Walking Lunge", also = listOf(GLUTES)),
        )
        group(
            QUADS, ISOMETRIC,
            e("Wall Sit", TIME_WEIGHT, also = listOf(GLUTES)), e("Spanish Squat Hold", TIME_WEIGHT),
            e("Split Squat Hold", TIME_WEIGHT, perSide = true, also = listOf(GLUTES)),
            e("Isometric Leg Extension Hold", TIME_WEIGHT),
        )
        group(
            QUADS, ECCENTRIC,
            e("Decline Board Squat", tempo = "3-0-1-0"),
            e("Heavy Slow Leg Press", tempo = "3-0-3-0", also = listOf(GLUTES)),
            e("Reverse Nordic Curl", REPS, tempo = "3-0-1-0", also = listOf(HIPS)),
            e("Eccentric Leg Extension", tempo = "4-0-1-0"),
        )
        group(QUADS, PLYOMETRIC, e("Jump Squat", REPS, also = listOf(GLUTES, CALVES)))
        group(QUADS, STRETCHING, e("Couch Stretch", TIME, perSide = true, also = listOf(HIPS)))
        group(
            HAMSTRINGS, STRENGTH,
            e("Romanian Deadlift", also = listOf(GLUTES, LOWER_BACK)), e("Seated Leg Curl Machine"),
            e("Lying Leg Curl Machine"),
        )
        group(HAMSTRINGS, ISOMETRIC, e("Hamstring Bridge Hold", TIME, also = listOf(GLUTES)))
        group(HAMSTRINGS, ECCENTRIC, e("Nordic Hamstring Curl", REPS, tempo = "5-0-1-0"))
        group(HAMSTRINGS, STRETCHING, e("Hamstring Stretch", TIME, perSide = true))
        group(GLUTES, STRENGTH, e("Barbell Hip Thrust", also = listOf(HAMSTRINGS)))
        group(GLUTES, ISOMETRIC, e("Glute Bridge Hold", TIME_WEIGHT, also = listOf(HAMSTRINGS)))
        group(GLUTES, STRETCHING, e("Pigeon Stretch", TIME, perSide = true, also = listOf(HIPS)))
        group(
            HIPS, MOBILITY,
            e("Hip CARs", REPS, perSide = true), e("90/90 Hip Switches", REPS, also = listOf(GLUTES)),
            e("Deep Squat Hold", TIME, also = listOf(CALVES, ADDUCTORS)),
        )
        group(HIPS, STRETCHING, e("Hip Flexor Stretch", TIME, perSide = true, also = listOf(QUADS)))
        group(ADDUCTORS, STRENGTH, e("Adductor Machine"))
        group(ADDUCTORS, ISOMETRIC, e("Copenhagen Plank", TIME, perSide = true, also = listOf(OBLIQUES)))
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
            e("Lateral Bound", REPS_HEIGHT, perSide = true, also = listOf(GLUTES)), e("Tuck Jump", REPS),
            e("Skater Jump", REPS, also = listOf(GLUTES)),
        )

        // Core
        group(
            ABS, STRENGTH,
            e("Crunch", REPS), e("Hanging Leg Raise", REPS, also = listOf(HIPS)), e("Cable Crunch"),
            e("Ab Wheel Rollout", REPS, also = listOf(LATS)), e("Dead Bug", REPS),
        )
        group(ABS, ISOMETRIC, e("Plank", TIME), e("Hollow Body Hold", TIME))
        group(ABS, STRETCHING, e("Cobra Stretch", TIME, also = listOf(LOWER_BACK)))
        group(
            OBLIQUES, ISOMETRIC,
            e("Side Plank", TIME), e("Pallof Press Hold", TIME, perSide = true, also = listOf(ABS)),
        )

        // Full body
        group(
            FULL_BODY, STRENGTH,
            e("Kettlebell Swing", also = listOf(GLUTES, HAMSTRINGS, LOWER_BACK)),
            e("Farmer's Carry", TIME_WEIGHT, also = listOf(FOREARMS, UPPER_BACK)),
        )
        group(FULL_BODY, ISOMETRIC, e("Isometric Mid-Thigh Pull", TIME_WEIGHT, also = listOf(UPPER_BACK, WHOLE_LEGS)))
        group(FULL_BODY, PLYOMETRIC, e("Medicine Ball Slam", also = listOf(ABS, LATS)))
        group(
            FULL_BODY, MOBILITY,
            e("World's Greatest Stretch", REPS, perSide = true, also = listOf(HIPS, HAMSTRINGS, UPPER_BACK)),
        )

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
