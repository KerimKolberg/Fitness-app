package com.kkfittracking.ui.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kkfittracking.model.Routine
import com.kkfittracking.ui.components.TextInputDialog

/** Tick the plans an exercise belongs to. One exercise can be in any number of plans. */
@Composable
fun PlansDialog(
    exerciseName: String,
    exerciseId: String,
    plans: List<Routine>,
    onToggle: (planId: String, inPlan: Boolean) -> Unit,
    onCreatePlan: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    if (creating) {
        TextInputDialog(
            title = "New plan with $exerciseName",
            label = "Plan name, e.g. Push",
            confirmText = "Create",
            onConfirm = { name ->
                creating = false
                onCreatePlan(name)
            },
            onDismiss = { creating = false },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plans with $exerciseName") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (plans.isEmpty()) {
                    Text(
                        text = "No plans yet. Create one to group exercises, like Push or Tendon health.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                plans.forEach { plan ->
                    val inPlan = plan.exercises.any { it.exerciseId == exerciseId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = inPlan, onValueChange = { onToggle(plan.id, it) }, role = Role.Checkbox)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = inPlan, onCheckedChange = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(plan.name)
                            Text(
                                text = "${plan.exercises.size} exercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = { creating = true }) { Text("New plan") } },
    )
}
