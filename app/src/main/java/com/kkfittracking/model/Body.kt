package com.kkfittracking.model

import java.time.LocalDate

enum class BodyMetricKind { MASS, PERCENT, LENGTH }

/** What can be tracked in the body tracker. Never rename an entry: the name is stored in the database. */
enum class BodyMetric(val label: String, val kind: BodyMetricKind) {
    BODYWEIGHT("Bodyweight", BodyMetricKind.MASS),
    BODY_FAT("Body fat", BodyMetricKind.PERCENT),
    NECK("Neck", BodyMetricKind.LENGTH),
    SHOULDERS("Shoulders", BodyMetricKind.LENGTH),
    CHEST("Chest", BodyMetricKind.LENGTH),
    WAIST("Waist", BodyMetricKind.LENGTH),
    HIPS("Hips", BodyMetricKind.LENGTH),
    UPPER_ARM("Upper arm", BodyMetricKind.LENGTH),
    FOREARM("Forearm", BodyMetricKind.LENGTH),
    THIGH("Thigh", BodyMetricKind.LENGTH),
    CALF("Calf", BodyMetricKind.LENGTH);

    fun unit(units: UnitSystem): String = when (kind) {
        BodyMetricKind.MASS -> units.weightUnit
        BodyMetricKind.PERCENT -> "%"
        BodyMetricKind.LENGTH -> units.lengthUnit
    }

    /** Converts a stored value (kg, %, cm) to the user's units. */
    fun toDisplay(value: Double, units: UnitSystem): Double = when (kind) {
        BodyMetricKind.MASS -> units.weightFromKg(value)
        BodyMetricKind.PERCENT -> value
        BodyMetricKind.LENGTH -> units.lengthFromCm(value)
    }

    /** Converts a value typed in the user's units to the stored unit. */
    fun fromDisplay(value: Double, units: UnitSystem): Double = when (kind) {
        BodyMetricKind.MASS -> units.weightToKg(value)
        BodyMetricKind.PERCENT -> value
        BodyMetricKind.LENGTH -> units.lengthToCm(value)
    }

    fun format(value: Double, units: UnitSystem): String =
        "${formatNumber(toDisplay(value, units))} ${unit(units)}"
}

data class BodyMeasurement(
    val id: String,
    val date: LocalDate,
    val metric: BodyMetric,
    val value: Double,
)

data class Routine(
    val id: String,
    val name: String,
    val notes: String,
    val exercises: List<RoutineExercise>,
)

data class RoutineExercise(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val categoryColor: Int,
)
