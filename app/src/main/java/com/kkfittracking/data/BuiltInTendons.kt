package com.kkfittracking.data

import com.kkfittracking.model.Tendon

/** The tendons the built-in exercises load most, by exercise name. */
object BuiltInTendons {
    private val lists: Map<Tendon, List<String>> = mapOf(
        Tendon.PATELLAR to listOf(
            "Spanish Squat Hold", "Wall Sit", "Split Squat Hold", "Isometric Leg Extension Hold", "Horse Stance",
            "Decline Board Squat", "Heavy Slow Leg Press", "Heavy Slow Squat", "Eccentric Leg Extension",
            "Eccentric Step-Down", "Pistol Squat Negative", "Leg Extension Machine", "Barbell Squat", "Front Squat",
            "Bulgarian Split Squat", "Chair Pose", "Box Jump", "Depth Jump", "Jump Squat",
        ),
        Tendon.QUADRICEPS to listOf(
            "Reverse Nordic Curl", "Spanish Squat Hold", "Decline Board Squat", "Heavy Slow Squat",
            "Isometric Leg Extension Hold", "Eccentric Leg Extension",
        ),
        Tendon.ACHILLES to listOf(
            "Eccentric Heel Drop", "Single-Leg Calf Raise Hold", "Standing Calf Raise", "Seated Calf Raise",
            "Donkey Calf Raise", "Pogo Hops", "Single-Leg Hop", "Jump Rope", "Calf Stretch", "Ankle Dorsiflexion Rocks",
        ),
        Tendon.PLANTAR_FASCIA to listOf("Single-Leg Calf Raise Hold", "Standing Calf Raise", "Eccentric Heel Drop"),
        Tendon.TIBIALIS to listOf("Tibialis Raise"),
        Tendon.HAMSTRING to listOf(
            "Nordic Hamstring Curl", "Isometric Nordic Hold", "Hamstring Bridge Hold", "Slider Hamstring Curl",
            "Romanian Deadlift", "Single-Leg Romanian Deadlift", "Good Morning",
        ),
        Tendon.ADDUCTOR to listOf("Copenhagen Plank", "Adductor Squeeze", "Adductor Machine"),
        Tendon.GLUTEAL to listOf("Glute Bridge Hold", "Side Plank"),
        Tendon.ROTATOR_CUFF to listOf("Cable External Rotation", "Eccentric External Rotation", "Shoulder CARs", "Face Pull"),
        Tendon.BICEPS to listOf("Eccentric Barbell Curl", "Isometric Curl Hold", "Incline Dumbbell Curl"),
        Tendon.LATERAL_ELBOW to listOf("Tyler Twist", "Eccentric Wrist Extension", "Reverse Wrist Curl", "Reverse Curl"),
        Tendon.MEDIAL_ELBOW to listOf("Eccentric Wrist Flexion", "Wrist Curl", "Wrist Flexor Stretch"),
        Tendon.TRICEPS to listOf("Eccentric Dip", "Parallel Bar Triceps Dip", "EZ-Bar Skullcrusher"),
        Tendon.FINGER_FLEXORS to listOf("Dead Hang", "Towel Hang", "Plate Pinch Hold", "Flexed-Arm Hang", "Climbing"),
    )

    /** Every exercise name mentioned above, to check that each one is in the catalog. */
    val exerciseNames: Set<String> get() = lists.values.flatten().toSet()

    private val byId: Map<String, List<Tendon>> = lists
        .flatMap { (tendon, names) -> names.map { BuiltInExercises.stableId("exercise", BuiltInExercises.keyOf(it)) to tendon } }
        .groupBy({ it.first }, { it.second })

    /** The tendons a built-in exercise loads; empty for others. */
    fun of(exerciseId: String): List<Tendon> = byId[exerciseId].orEmpty()
}
