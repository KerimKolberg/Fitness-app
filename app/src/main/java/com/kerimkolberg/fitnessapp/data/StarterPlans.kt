package com.kerimkolberg.fitnessapp.data

/** Ready-made plans the user can add with one tap, built from the built-in exercises. */
object StarterPlans {
    val plans: List<Pair<String, List<String>>> = listOf(
        "Push" to listOf(
            "Flat Barbell Bench Press",
            "Incline Barbell Bench Press",
            "Overhead Press",
            "Lateral Dumbbell Raise",
            "Rope Push Down",
        ),
        "Pull" to listOf("Pull Up", "Barbell Row", "Lat Pulldown", "Face Pull", "Barbell Curl"),
        "Legs" to listOf(
            "Barbell Squat",
            "Romanian Deadlift",
            "Leg Press",
            "Seated Leg Curl Machine",
            "Standing Calf Raise",
        ),
        "Upper body" to listOf(
            "Incline Dumbbell Bench Press",
            "Pull Up",
            "Seated Dumbbell Press",
            "One-Arm Dumbbell Row",
            "Hammer Curl",
            "Rope Push Down",
        ),
        "Tendon health" to listOf(
            "Nordic Hamstring Curl",
            "Eccentric Heel Drop",
            "Spanish Squat Hold",
            "Decline Board Squat",
            "Tyler Twist",
            "Copenhagen Plank",
        ),
        "Mobility flow" to listOf(
            "Cat-Cow",
            "World's Greatest Stretch",
            "90/90 Hip Switches",
            "Thoracic Rotation",
            "Ankle Dorsiflexion Rocks",
            "Deep Squat Hold",
        ),
    )

    fun exerciseId(name: String): String = BuiltInExercises.stableId("exercise", BuiltInExercises.keyOf(name))
}
