package com.kkfittracking.model

/**
 * How an exercise trains: the third level of the library, under body section and muscle
 * (e.g. Legs → Hamstrings → Eccentric → Nordic curl). The name is stored in the database.
 */
enum class TrainingStyle(val label: String) {
    STRENGTH("Strength"),
    ISOMETRIC("Isometric"),
    ECCENTRIC("Eccentric & tendon"),
    PLYOMETRIC("Plyometric"),
    MOBILITY("Mobility"),
    STRETCHING("Stretching"),
    CARDIO("Cardio"),
    HIIT("HIIT & intervals"),
    SPORT("Sport");

    companion object {
        /** The style an exercise has when none was chosen, from its section and type. */
        fun defaultFor(regionKey: String?, type: ExerciseType): TrainingStyle = when {
            type == ExerciseType.INTERVALS -> HIIT
            regionKey == Regions.CARDIO -> CARDIO
            regionKey == Regions.SPORTS -> SPORT
            else -> STRENGTH
        }

        /** The stored style, or the default when it is empty (never chosen) or unknown. */
        fun resolve(stored: String, regionKey: String?, type: ExerciseType): TrainingStyle =
            entries.firstOrNull { it.name == stored } ?: defaultFor(regionKey, type)
    }
}

/**
 * The muscle (or area) an exercise mainly trains: the second level of the library. [regionKey] is
 * the key of the body section it belongs to. The name is stored in the database.
 */
enum class Muscle(val label: String, val regionKey: String) {
    CHEST("Chest", Regions.CHEST),
    LATS("Lats", Regions.BACK),
    UPPER_BACK("Upper back", Regions.BACK),
    LOWER_BACK("Lower back", Regions.BACK),
    FRONT_DELTS("Front delts", Regions.SHOULDERS),
    SIDE_DELTS("Side delts", Regions.SHOULDERS),
    REAR_DELTS("Rear delts", Regions.SHOULDERS),
    ROTATOR_CUFF("Rotator cuff", Regions.SHOULDERS),
    BICEPS("Biceps", Regions.ARMS),
    TRICEPS("Triceps", Regions.ARMS),
    FOREARMS("Forearms & grip", Regions.ARMS),
    QUADS("Quads", Regions.LEGS),
    HAMSTRINGS("Hamstrings", Regions.LEGS),
    GLUTES("Glutes", Regions.LEGS),
    HIPS("Hips & hip flexors", Regions.LEGS),
    ADDUCTORS("Adductors", Regions.LEGS),
    CALVES("Calves & ankles", Regions.LEGS),
    SHINS("Shins", Regions.LEGS),
    WHOLE_LEGS("Whole legs", Regions.LEGS),
    ABS("Abs", Regions.CORE),
    OBLIQUES("Obliques", Regions.CORE),
    FULL_BODY("Full body", Regions.FULL_BODY),
    CARDIO("Cardio", Regions.CARDIO),
    SPORT("Sports", Regions.SPORTS),

    /** Not assigned to a muscle; fits any section. */
    OTHER("Other", "");

    companion object {
        /** The muscles to choose from in a section, "Other" last. */
        fun forRegion(regionKey: String?): List<Muscle> = entries.filter { it != OTHER && it.regionKey == regionKey } + OTHER

        /** A section's only muscle (such as Chest), or [OTHER] when it has several to pick from. */
        fun defaultFor(regionKey: String?): Muscle = forRegion(regionKey).dropLast(1).singleOrNull() ?: OTHER

        /** The stored muscle, or the default for the section when it is empty (never chosen) or unknown. */
        fun resolve(stored: String, regionKey: String?): Muscle =
            entries.firstOrNull { it.name == stored } ?: defaultFor(regionKey)
    }
}

/** Keys of the body section categories (the first level of the library). */
object Regions {
    const val CHEST = "chest"
    const val BACK = "back"
    const val SHOULDERS = "shoulders"
    const val ARMS = "arms"
    const val LEGS = "legs"

    /** Kept as "abs" from the first version so the id stays the same; now named "Core". */
    const val CORE = "abs"
    const val FULL_BODY = "full-body"
    const val CARDIO = "cardio"
    const val SPORTS = "sports"
}
