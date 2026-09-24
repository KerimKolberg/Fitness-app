package com.kerimkolberg.fitnessapp.ui.log

import com.kerimkolberg.fitnessapp.model.ExerciseType
import com.kerimkolberg.fitnessapp.model.SetValues
import com.kerimkolberg.fitnessapp.model.UnitSystem
import com.kerimkolberg.fitnessapp.model.formatNumber
import com.kerimkolberg.fitnessapp.model.parseDecimal

/** The text the user typed into the set fields, in their display units. */
data class SetInput(
    val weight: String = "",
    val reps: String = "",
    val distance: String = "",
    val minutes: String = "",
    val seconds: String = "",
) {
    sealed interface Result {
        data class Valid(val values: SetValues) : Result
        data class Invalid(val message: String) : Result
    }

    /** Checks the fields needed by [type] and converts them to stored units. */
    fun toSetValues(type: ExerciseType, units: UnitSystem): Result {
        var weightKg: Double? = null
        var repCount: Int? = null
        var distanceMeters: Double? = null
        var durationSeconds: Int? = null

        if (type.usesWeight) {
            val value = parseDecimal(weight.ifBlank { "0" })
            if (value == null || value < 0) return Result.Invalid("Enter a valid weight")
            weightKg = units.weightToKg(value)
        }
        if (type.usesReps) {
            val value = reps.trim().toIntOrNull()
            if (value == null || value <= 0) return Result.Invalid("Enter the number of reps")
            repCount = value
        }
        if (type.usesDistance && distance.isNotBlank()) {
            val value = parseDecimal(distance)
            if (value == null || value < 0) return Result.Invalid("Enter a valid distance")
            distanceMeters = units.distanceToMeters(value)
        }
        if (type.usesTime && (minutes.isNotBlank() || seconds.isNotBlank())) {
            val min = minutes.trim().ifEmpty { "0" }.toIntOrNull()
            val sec = seconds.trim().ifEmpty { "0" }.toIntOrNull()
            if (min == null || sec == null || min < 0 || sec < 0) return Result.Invalid("Enter a valid time")
            durationSeconds = min * 60 + sec
        }
        if (type == ExerciseType.DISTANCE_TIME && (distanceMeters ?: 0.0) <= 0.0 && (durationSeconds ?: 0) <= 0) {
            return Result.Invalid("Enter a distance or a time")
        }
        if (type == ExerciseType.TIME && (durationSeconds ?: 0) <= 0) {
            return Result.Invalid("Enter a time")
        }
        return Result.Valid(SetValues(weightKg, repCount, distanceMeters, durationSeconds))
    }

    fun adjustWeight(direction: Int, units: UnitSystem): SetInput {
        val current = parseDecimal(weight) ?: 0.0
        return copy(weight = formatNumber((current + direction * units.weightStep).coerceAtLeast(0.0)))
    }

    fun adjustReps(direction: Int): SetInput {
        val current = reps.trim().toIntOrNull() ?: 0
        return copy(reps = (current + direction).coerceAtLeast(0).toString())
    }

    companion object {
        /** Fills the fields from a stored set, converted to [units]. */
        fun from(values: SetValues, units: UnitSystem) = SetInput(
            weight = values.weightKg?.let { formatNumber(units.weightFromKg(it)) }.orEmpty(),
            reps = values.reps?.toString().orEmpty(),
            distance = values.distanceMeters?.let { formatNumber(units.distanceFromMeters(it)) }.orEmpty(),
            minutes = values.durationSeconds?.let { (it / 60).toString() }.orEmpty(),
            seconds = values.durationSeconds?.let { (it % 60).toString() }.orEmpty(),
        )
    }
}
