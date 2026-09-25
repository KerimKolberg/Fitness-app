package com.kkfittracking.ui.log

import com.kkfittracking.model.ExercisePlan
import com.kkfittracking.model.UnitSystem
import com.kkfittracking.model.defaultDropAmountKg
import com.kkfittracking.model.formatNumber
import com.kkfittracking.model.parseDecimal

/** The set plan dialog's fields, with weights in the user's units. */
data class SetPlanForm(
    val sets: Int = DEFAULT_SETS,
    val reps: String = "",
    val weight: String = "",
    /** Null uses the default rest from the settings. */
    val restSeconds: Int? = null,
    val dropSets: Boolean = false,
    val drops: Int = ExercisePlan.DEFAULT_DROPS,
    val dropReps: String = "",
    val dropByPercent: Boolean = true,
    val dropPercent: Int = 20,
    val dropAmount: String = "",
) {
    sealed interface Result {
        data class Valid(val plan: ExercisePlan) : Result
        data class Invalid(val message: String) : Result
    }

    /** Drop sets on a single set make the whole exercise one drop set. */
    val isWholeExercise: Boolean get() = sets == 1

    fun changeSets(delta: Int) = copy(sets = (sets + delta).coerceIn(1, ExercisePlan.MAX_SETS))

    fun changeDrops(delta: Int) = copy(drops = (drops + delta).coerceIn(1, ExercisePlan.MAX_DROPS))

    fun changeDropPercent(delta: Int) = copy(dropPercent = (dropPercent + delta).coerceIn(MIN_DROP_PERCENT, MAX_DROP_PERCENT))

    fun changeRest(delta: Int, defaultSeconds: Int) =
        copy(restSeconds = ((restSeconds ?: defaultSeconds) + delta).coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS))

    /** "Only the last set" (keeps several sets) or "Whole exercise" (one set). */
    fun dropOnWholeExercise(whole: Boolean) = when {
        whole -> copy(sets = 1)
        sets == 1 -> copy(sets = DEFAULT_SETS)
        else -> this
    }

    /** Checks the fields and builds the plan, keeping [base]'s interval timings. */
    fun toPlan(base: ExercisePlan, units: UnitSystem): Result {
        val repCount = wholeNumber(reps) ?: return Result.Invalid("Reps must be a whole number")
        val targetKg = if (weight.isBlank()) {
            null
        } else {
            val value = parseDecimal(weight)
            if (value == null || value < 0) return Result.Invalid("Enter a valid weight")
            units.weightToKg(value)
        }
        val dropRepCount = wholeNumber(dropReps) ?: return Result.Invalid("Drop reps must be a whole number")
        val amountKg = if (dropAmount.isBlank()) {
            null
        } else {
            val value = parseDecimal(dropAmount)
            if (value == null || value <= 0) return Result.Invalid("Enter how much weight each drop takes off")
            units.weightToKg(value)
        }
        return Result.Valid(
            base.copy(
                sets = sets,
                reps = repCount.value,
                weightKg = targetKg,
                restSeconds = restSeconds,
                dropSets = dropSets,
                drops = drops,
                dropReps = dropRepCount.value,
                dropByPercent = dropByPercent,
                dropPercent = dropPercent,
                dropAmountKg = amountKg,
            ),
        )
    }

    /** The target weight typed in the form, in kg, if it is valid. */
    fun weightKg(units: UnitSystem): Double? = parseDecimal(weight)?.takeIf { it > 0 }?.let(units::weightToKg)

    /** The form as a plan for the preview, even while a field is being typed. */
    fun previewPlan(units: UnitSystem): ExercisePlan = ExercisePlan(
        sets = sets,
        dropSets = dropSets,
        drops = drops,
        dropByPercent = dropByPercent,
        dropPercent = dropPercent,
        dropAmountKg = parseDecimal(dropAmount)?.takeIf { it > 0 }?.let(units::weightToKg),
    )

    /** Blank is null; anything else must be a positive whole number. */
    private class Count(val value: Int?)

    private fun wholeNumber(text: String): Count? {
        if (text.isBlank()) return Count(null)
        val value = text.trim().toIntOrNull() ?: return null
        return if (value > 0) Count(value) else null
    }

    companion object {
        const val DEFAULT_SETS = 3
        const val MIN_DROP_PERCENT = 5
        const val MAX_DROP_PERCENT = 50
        const val MIN_REST_SECONDS = 15
        const val MAX_REST_SECONDS = 15 * 60

        /** Fills the form from a stored plan; drop settings not chosen yet start from the defaults. */
        fun from(plan: ExercisePlan, units: UnitSystem, defaultPercent: Int) = SetPlanForm(
            sets = plan.sets ?: DEFAULT_SETS,
            reps = plan.reps?.toString().orEmpty(),
            weight = plan.weightKg?.let { formatNumber(units.weightFromKg(it)) }.orEmpty(),
            restSeconds = plan.restSeconds,
            dropSets = plan.dropSets,
            drops = plan.drops,
            dropReps = plan.dropReps?.toString().orEmpty(),
            dropByPercent = plan.dropByPercent,
            dropPercent = plan.dropPercent ?: defaultPercent,
            dropAmount = formatNumber(units.weightFromKg(plan.dropAmountKg ?: defaultDropAmountKg(units))),
        )
    }
}
