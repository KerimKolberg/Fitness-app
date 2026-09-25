package com.kkfittracking.data

import com.kkfittracking.model.ExercisePlan

/** Ready-made plans the user can add with one tap, built from the built-in exercises. */
object StarterPlans {
    /**
     * [supersets] lists groups of the plan's exercises that are done back to back, for [rounds]
     * rounds when set. [setPlans] are sets, reps and weights for exercises that have no set plan yet.
     */
    data class StarterPlan(
        val name: String,
        val exercises: List<String>,
        val supersets: List<List<String>> = emptyList(),
        val rounds: Int? = null,
        val setPlans: Map<String, ExercisePlan> = emptyMap(),
    )

    private fun sets(reps: Int? = null, kg: Double? = null) = ExercisePlan(sets = 3, reps = reps, weightKg = kg)

    /** A plan in blocks: each block is a superset of [rounds] rounds (a block of one is a normal exercise). */
    private fun blocks(name: String, rounds: Int, vararg blocks: List<Pair<String, ExercisePlan>>) = StarterPlan(
        name = name,
        exercises = blocks.flatMap { block -> block.map { it.first } },
        supersets = blocks.filter { it.size >= 2 }.map { block -> block.map { it.first } },
        rounds = rounds,
        setPlans = blocks.flatMap { it.toList() }.toMap(),
    )

    val plans: List<StarterPlan> = listOf(
        // Kerim's own plans: a muscular exercise and its tendon work, block by block.
        blocks(
            "KK Upper body", 3,
            listOf("Pull Up" to sets(reps = 8), "Half-Crimp Hang" to sets(kg = 80.0)),
            listOf(
                "Incline Dumbbell Bench Press" to sets(reps = 10, kg = 25.0),
                "Slow Cable External Rotation" to sets(reps = 8, kg = 20.0),
                "Slow High Cable External Rotation" to sets(reps = 8, kg = 20.0),
            ),
            listOf(
                "Seated Dumbbell Press" to sets(reps = 10, kg = 12.0),
                "Band Internal Rotation Hold" to sets(),
                "Slow High Cable Internal Rotation" to sets(reps = 8, kg = 20.0),
            ),
            listOf(
                "Reverse Curl" to sets(reps = 15, kg = 50.0),
                "Overhead Cable Triceps Extension" to sets(reps = 15, kg = 50.0),
                "Eccentric Wrist Flexion" to sets(reps = 8, kg = 10.0),
                "Eccentric Wrist Extension" to sets(reps = 8, kg = 10.0),
            ),
            listOf(
                "Lateral Dumbbell Raise" to sets(reps = 15, kg = 8.0),
                "One-Arm Dumbbell Row" to sets(reps = 10, kg = 36.0),
                "Plate Pinch Hold" to sets(kg = 15.0),
            ),
        ),
        blocks(
            "KK Lower body", 3,
            listOf(
                "Walking Lunge" to sets(reps = 20, kg = 22.0),
                "Wall Sit" to sets(kg = 25.0),
                "Isometric Reverse Nordic Hold" to sets(),
            ),
            listOf(
                "Eccentric Romanian Deadlift" to sets(reps = 8, kg = 17.5),
                "45-Degree Hyperextension" to sets(kg = 10.0),
                "Nordic Hamstring Curl" to sets(),
            ),
            listOf(
                "Farmer's Carry" to sets(kg = 36.0),
                "Side-Lying Clamshell Hold" to sets(),
                "Slow Single-Leg Pelvic Drop" to sets(reps = 8, kg = 36.0),
            ),
            listOf("Cable Woodchopper" to sets(reps = 12, kg = 40.0), "Slow Deficit Calf Raise" to sets(reps = 8, kg = 16.0)),
            listOf("Sit-Up" to sets(kg = 15.0), "Hanging Leg Raise" to sets(reps = 15)),
        ),
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
            "Court sports prehab",
            listOf(
                "Single-Leg Balance Hold",
                "Banded Ankle Eversion",
                "Lateral Lunge",
                "Copenhagen Plank",
                "Side-Lying External Rotation",
                "90/90 External Rotation Hold",
                "Tyler Twist",
                "Rotational Medicine Ball Throw",
                "Slow Deficit Calf Raise",
            ),
        ),
        StarterPlan(
            "Climbing prehab",
            listOf(
                "Hangboard Hang",
                "No-Hang Lift",
                "Finger Extension with Band",
                "Reverse Tyler Twist",
                "Scapular Pull Up",
                "Lock-Off Hold",
                "Prone Y-T-W Raise",
                "Wall Slide",
            ),
        ),
        StarterPlan(
            "Sprint & jump",
            listOf(
                "A-Skip",
                "Psoas March",
                "Nordic Hamstring Curl",
                "Isometric Reverse Nordic Hold",
                "Slow Deficit Calf Raise",
                "Single-Leg Landing Stick",
                "Approach Jump",
                "Hill Sprints",
            ),
        ),
        StarterPlan(
            "Kickboxing conditioning",
            listOf(
                "Shadow Boxing",
                "Heavy Bag Rounds",
                "Rotational Medicine Ball Throw",
                "Cossack Squat",
                "Psoas March",
                "Slow Single-Leg Pelvic Drop",
                "Hollow Rock",
            ),
            supersets = listOf(listOf("Cossack Squat", "Psoas March")),
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
