package com.kerimkolberg.fitnessapp.model

enum class UnitSystem(
    val label: String,
    val weightUnit: String,
    val distanceUnit: String,
    /** Step used by the +/- buttons on the weight field, in this unit system's weight unit. */
    val weightStep: Double,
) {
    METRIC("Metric (kg, km)", "kg", "km", 2.5),
    IMPERIAL("Imperial (lb, mi)", "lb", "mi", 5.0);

    fun weightFromKg(kg: Double): Double = if (this == METRIC) kg else kg / KG_PER_LB

    fun weightToKg(value: Double): Double = if (this == METRIC) value else value * KG_PER_LB

    fun distanceFromMeters(meters: Double): Double =
        if (this == METRIC) meters / METERS_PER_KM else meters / METERS_PER_MILE

    fun distanceToMeters(value: Double): Double =
        if (this == METRIC) value * METERS_PER_KM else value * METERS_PER_MILE

    val lengthUnit: String get() = if (this == METRIC) "cm" else "in"

    fun lengthFromCm(cm: Double): Double = if (this == METRIC) cm else cm / CM_PER_INCH

    fun lengthToCm(value: Double): Double = if (this == METRIC) value else value * CM_PER_INCH

    /** Jump height or distance, stored in meters, shown in cm or inches. */
    fun heightFromMeters(meters: Double): Double = lengthFromCm(meters * 100)

    fun heightToMeters(value: Double): Double = lengthToCm(value) / 100

    companion object {
        const val KG_PER_LB = 0.45359237
        const val METERS_PER_KM = 1000.0
        const val METERS_PER_MILE = 1609.344
        const val CM_PER_INCH = 2.54
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
}

data class Settings(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val restTimerSeconds: Int = 90,
    val autoStartRestTimer: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Workouts per week that keep a streak going. */
    val weeklyGoal: Int = 3,
    /** Show the drop set option when logging weighted sets. */
    val dropSetsEnabled: Boolean = true,
    /** How much lighter each drop set is, in percent. */
    val dropSetPercent: Int = 20,
    /** After saving a set in a superset, open the next exercise of the superset. */
    val supersetAutoAdvance: Boolean = true,
)
