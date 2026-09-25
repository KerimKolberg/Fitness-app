package com.kkfittracking.model

/**
 * How an exercise trains. The library can be browsed by body section (Legs → Hamstrings →
 * Eccentrics → Nordic curl) or by training style (Stretching → Legs → Hamstrings). The name is
 * stored in the database.
 */
enum class TrainingStyle(val label: String, val color: Int) {
    STRENGTH("Strength", 0xFF607D8B.toInt()),
    ISOMETRIC("Isometrics", 0xFF5E35B1.toInt()),
    ECCENTRIC("Eccentrics & tendons", 0xFFD81B60.toInt()),
    PLYOMETRIC("Plyometrics", 0xFFF4511E.toInt()),
    MOBILITY("Mobility", 0xFF00ACC1.toInt()),
    STRETCHING("Stretching", 0xFF7CB342.toInt()),
    CARDIO("Cardio", 0xFF6D4C41.toInt()),
    HIIT("HIIT & intervals", 0xFFFFB300.toInt()),
    SPORT("Sports", 0xFF546E7A.toInt());

    companion object {
        /** The order of the training style sections: the ones beyond the usual gym work first. */
        val sectionOrder: List<TrainingStyle> =
            listOf(MOBILITY, STRETCHING, ISOMETRIC, ECCENTRIC, PLYOMETRIC, HIIT, STRENGTH, CARDIO, SPORT)

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
 * A muscle (or area) an exercise trains: the level under the body sections. [regionKey] is the key
 * of the body section it belongs to. An exercise can train several; the first is its main one. The
 * names are stored in the database.
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

        /**
         * The stored muscles (comma separated, the main one first), or the default for the section
         * when there are none (never chosen) or none is known.
         */
        fun resolveAll(stored: String, regionKey: String?): List<Muscle> =
            stored.split(',').mapNotNull { name -> entries.firstOrNull { it.name == name.trim() } }.distinct()
                .ifEmpty { listOf(defaultFor(regionKey)) }

        /** How [muscles] are stored. */
        fun format(muscles: List<Muscle>): String = muscles.distinct().joinToString(",") { it.name }
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
