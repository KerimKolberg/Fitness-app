package com.kkfittracking.data

/** Ready-made plans the user can add with one tap, built from the built-in exercises. */
object StarterPlans {
    /** [supersets] lists groups of the plan's exercises that are done back to back. */
    data class StarterPlan(
        val name: String,
        val exercises: List<String>,
        val supersets: List<List<String>> = emptyList(),
    )

    val plans: List<StarterPlan> = listOf(
        StarterPlan(
            "Push",
            listOf(
                "Flat Barbell Bench Press",
                "Incline Barbell Bench Press",
                "Overhead Press",
                "Lateral Dumbbell Raise",
                "Rope Push Down",
            ),
        ),
        StarterPlan("Pull", listOf("Pull Up", "Barbell Row", "Lat Pulldown", "Face Pull", "Barbell Curl")),
        StarterPlan(
            "Legs",
            listOf(
                "Barbell Squat",
                "Romanian Deadlift",
                "Leg Press",
                "Seated Leg Curl Machine",
                "Standing Calf Raise",
            ),
        ),
        StarterPlan(
            "Upper body",
            listOf(
                "Incline Dumbbell Bench Press",
                "Pull Up",
                "Seated Dumbbell Press",
                "One-Arm Dumbbell Row",
                "Hammer Curl",
                "Rope Push Down",
            ),
        ),
        StarterPlan(
            "Arms supersets",
            listOf(
                "Barbell Curl",
                "Rope Push Down",
                "Hammer Curl",
                "Overhead Dumbbell Triceps Extension",
                "Concentration Curl",
            ),
            supersets = listOf(
                listOf("Barbell Curl", "Rope Push Down"),
                listOf("Hammer Curl", "Overhead Dumbbell Triceps Extension"),
            ),
        ),
        StarterPlan(
            "Tendon health",
            listOf(
                "Nordic Hamstring Curl",
                "Eccentric Heel Drop",
                "Spanish Squat Hold",
                "Decline Board Squat",
                "Tyler Twist",
                "Copenhagen Plank",
            ),
        ),
        StarterPlan(
            "Mobility flow",
            listOf(
                "Cat-Cow",
                "World's Greatest Stretch",
                "90/90 Hip Switches",
                "Thoracic Rotation",
                "Ankle Dorsiflexion Rocks",
                "Deep Squat Hold",
            ),
        ),
    )

    fun exerciseId(name: String): String = BuiltInExercises.stableId("exercise", BuiltInExercises.keyOf(name))
}
