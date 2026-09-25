package com.kkfittracking.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kkfittracking.model.ExercisePlan
import com.kkfittracking.model.ExerciseType
import com.kkfittracking.model.UnitSystem
import com.kkfittracking.model.dropWeights
import com.kkfittracking.model.formatDuration
import com.kkfittracking.model.formatWeightSteps
import com.kkfittracking.model.summary

/** The exercise's set plan at a glance, with where today's workout is in it. */
@Composable
fun SetPlanCard(
    plan: ExercisePlan,
    type: ExerciseType,
    progress: String?,
    inSuperset: Boolean,
    defaultPercent: Int,
    units: UnitSystem,
    onEdit: () -> Unit,
) {
    val summary = plan.summary(type, defaultPercent, units)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Set plan", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = summary ?: "Plan sets, reps, rest and drop sets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (summary == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
                TextButton(onClick = onEdit) { Text(if (summary == null) "Plan" else "Edit") }
            }
            progress?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            if (inSuperset && plan.restSeconds != null) {
                Text(
                    text = "In a superset today: the superset's timing replaces this exercise's own rest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

/**
 * Edits the set plan: sets, reps, weight and rest, and drop sets. With several sets the drops
 * follow the last set; with one set the whole exercise is a drop set.
 */
@Composable
fun SetPlanDialog(
    plan: ExercisePlan,
    type: ExerciseType,
    units: UnitSystem,
    defaultRestSeconds: Int,
    defaultPercent: Int,
    /** The weight entered on the exercise, for the preview of the drops, in kg. */
    enteredWeightKg: Double?,
    inSuperset: Boolean,
    onSave: (ExercisePlan) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    /** Only the drop set settings, opened from the "Drop set" button. */
    dropOnly: Boolean = false,
) {
    var form by remember { mutableStateOf(SetPlanForm.from(plan, units, defaultPercent)) }
    var error by remember { mutableStateOf<String?>(null) }
    fun update(value: SetPlanForm) {
        form = value
        error = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (dropOnly) "Drop sets" else "Set plan") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (dropOnly) {
                    DropSetFields(form, units, form.weightKg(units) ?: enteredWeightKg, ::update, alwaysShowSettings = true)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                } else {
                    Stepper("Sets", "${form.sets}", onMinus = { update(form.changeSets(-1)) }, onPlus = { update(form.changeSets(1)) })
                    if (type.usesReps) {
                        NumberField(form.reps, "Reps per set (optional)", KeyboardType.Number) { update(form.copy(reps = it)) }
                    }
                    if (type.usesWeight) {
                        NumberField(form.weight, "Weight (${units.weightUnit}, optional)", KeyboardType.Decimal) { update(form.copy(weight = it)) }
                    }
                    Stepper(
                        label = "Rest between sets",
                        value = formatDuration(form.restSeconds ?: defaultRestSeconds) + if (form.restSeconds == null) " (default)" else "",
                        onMinus = { update(form.changeRest(-REST_STEP, defaultRestSeconds)) },
                        onPlus = { update(form.changeRest(REST_STEP, defaultRestSeconds)) },
                    )
                    if (form.restSeconds != null) {
                        TextButton(onClick = { update(form.copy(restSeconds = null)) }) { Text("Use the default rest") }
                    }
                    if (inSuperset) {
                        Text(
                            text = "This exercise is in a superset today, so the superset's timing is used instead of this rest.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    if (type == ExerciseType.WEIGHT_REPS) {
                        HorizontalDivider()
                        DropSetFields(form, units, form.weightKg(units) ?: enteredWeightKg, ::update)
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                    if (plan.withoutSetPlan() != plan) {
                        TextButton(onClick = onClear) { Text("Clear the set plan") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (val result = form.toPlan(plan, units)) {
                        is SetPlanForm.Result.Valid -> onSave(
                            // The drop set settings alone leave the rest of the set plan as it was.
                            if (dropOnly) {
                                result.plan.copy(
                                    sets = if (result.plan.dropSets) result.plan.sets else plan.sets,
                                    reps = plan.reps,
                                    weightKg = plan.weightKg,
                                    restSeconds = plan.restSeconds,
                                )
                            } else {
                                result.plan
                            },
                        )
                        is SetPlanForm.Result.Invalid -> error = result.message
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DropSetFields(
    form: SetPlanForm,
    units: UnitSystem,
    startKg: Double?,
    update: (SetPlanForm) -> Unit,
    /** Show how much each drop takes off even when drop sets are not planned (for the "Drop set" button). */
    alwaysShowSettings: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (alwaysShowSettings) "Planned drop sets" else "Drop sets", style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (alwaysShowSettings) {
                    "Start them by themselves after the planned sets"
                } else {
                    "Lower the weight and keep going, without resting"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = form.dropSets, onCheckedChange = { update(form.copy(dropSets = it)) })
    }
    if (!form.dropSets && !alwaysShowSettings) return
    if (form.dropSets) DropSetWhere(form, update)
    DropSetAmounts(form, units, startKg, update)
}

@Composable
private fun DropSetWhere(form: SetPlanForm, update: (SetPlanForm) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = !form.isWholeExercise,
            onClick = { update(form.dropOnWholeExercise(false)) },
            label = { Text("Only the last set") },
        )
        FilterChip(
            selected = form.isWholeExercise,
            onClick = { update(form.dropOnWholeExercise(true)) },
            label = { Text("Whole exercise") },
        )
    }
    val drops = if (form.drops == 1) "once" else "${form.drops} times"
    Text(
        text = if (form.isWholeExercise) {
            "The whole exercise is one drop set: 1 set, then lower the weight $drops."
        } else {
            "After set ${form.sets}, the last one, lower the weight $drops."
        },
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun DropSetAmounts(form: SetPlanForm, units: UnitSystem, startKg: Double?, update: (SetPlanForm) -> Unit) {
    Stepper("Drops", "${form.drops}", onMinus = { update(form.changeDrops(-1)) }, onPlus = { update(form.changeDrops(1)) })
    NumberField(form.dropReps, "Reps per drop (empty: as many as the set before)", KeyboardType.Number) {
        update(form.copy(dropReps = it))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = form.dropByPercent, onClick = { update(form.copy(dropByPercent = true)) }, label = { Text("By percent") })
        FilterChip(selected = !form.dropByPercent, onClick = { update(form.copy(dropByPercent = false)) }, label = { Text("By weight") })
    }
    if (form.dropByPercent) {
        Stepper(
            label = "Each drop",
            value = "−${form.dropPercent}%",
            onMinus = { update(form.changeDropPercent(-5)) },
            onPlus = { update(form.changeDropPercent(5)) },
        )
    } else {
        NumberField(form.dropAmount, "Each drop takes off (${units.weightUnit})", KeyboardType.Decimal) {
            update(form.copy(dropAmount = it))
        }
    }
    Text(
        text = if (startKg != null) {
            "≈ " + formatWeightSteps(form.previewPlan(units).dropWeights(startKg, form.dropPercent, units), units) +
                ", rounded to real plates"
        } else {
            "Enter a weight to see what each drop will be."
        },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = onMinus) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(value, modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleMedium)
        FilledTonalIconButton(onClick = onPlus) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun NumberField(value: String, label: String, keyboardType: KeyboardType, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

private const val REST_STEP = 15
