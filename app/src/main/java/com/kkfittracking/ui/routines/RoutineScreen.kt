@file:OptIn(ExperimentalMaterial3Api::class)

package com.kkfittracking.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkfittracking.model.Block
import com.kkfittracking.model.RoutineExercise
import com.kkfittracking.model.formatDuration
import com.kkfittracking.model.summary
import com.kkfittracking.ui.components.ColorDot
import com.kkfittracking.ui.components.TextInputDialog
import com.kkfittracking.ui.log.SetPlanDialog

@Composable
fun RoutineScreen(
    onBack: () -> Unit,
    onAddExercise: (routineId: String) -> Unit,
    onSupersets: (routineId: String) -> Unit,
    viewModel: RoutineViewModel = viewModel(factory = RoutineViewModel.Factory),
) {
    val routine by viewModel.routine.collectAsStateWithLifecycle()
    val library by viewModel.exercises.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    // The exercise whose sets, reps and weight are being edited.
    var editingPlan by rememberSaveable { mutableStateOf<String?>(null) }
    val planText = { member: RoutineExercise ->
        library[member.exerciseId]?.let { exercise ->
            exercise.plan.summary(exercise.type, settings.dropSetPercent, settings.unitSystem)
        } ?: "Tap to set sets, reps and weight"
    }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routine?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { renaming = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename plan")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete plan")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddExercise(viewModel.routineId) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add exercise") },
            )
        },
    ) { padding ->
        val current = routine
        val exercises = current?.exercises.orEmpty()
        if (current == null || exercises.isEmpty()) {
            Column(Modifier.padding(padding).padding(24.dp)) {
                Text(
                    text = "Add the exercises of this plan in the order you do them.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val blocks = current.blocks
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                if (exercises.size >= 2) {
                    item(key = "supersets") {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            OutlinedButton(onClick = { onSupersets(viewModel.routineId) }, modifier = Modifier.fillMaxWidth()) {
                                Text("+Super-sets")
                            }
                            if (current.supersetCount > 0) {
                                Text(
                                    text = "When you add this plan to a day, you can do it with its supersets or one by one.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
                itemsIndexed(blocks, key = { _, block ->
                    when (block) {
                        is Block.Single -> block.item.id
                        is Block.Superset -> "superset-${block.id}"
                    }
                }) { index, block ->
                    val moveUp = { viewModel.moveBlock(index, -1) }.takeIf { index > 0 }
                    val moveDown = { viewModel.moveBlock(index, 1) }.takeIf { index < blocks.lastIndex }
                    when (block) {
                        is Block.Single -> {
                            ListItem(
                                modifier = Modifier.clickable { editingPlan = block.item.exerciseId },
                                headlineContent = { Text(block.item.exerciseName) },
                                supportingContent = { Text(planText(block.item)) },
                                leadingContent = { ColorDot(block.item.categoryColor) },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        MoveButtons(block.item.exerciseName, moveUp, moveDown)
                                        IconButton(onClick = { viewModel.removeExercise(block.item.id) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove ${block.item.exerciseName}")
                                        }
                                    }
                                },
                            )
                            HorizontalDivider()
                        }
                        is Block.Superset -> SupersetCard(
                            members = block.items,
                            onMoveUp = moveUp,
                            onMoveDown = moveDown,
                            onRemove = { viewModel.removeExercise(it.id) },
                            onEdit = { editingPlan = it.exerciseId },
                            planText = planText,
                        )
                    }
                }
            }
        }
    }

    editingPlan?.let { exerciseId ->
        val exercise = library[exerciseId]
        if (exercise != null) {
            SetPlanDialog(
                plan = exercise.plan,
                type = exercise.type,
                units = settings.unitSystem,
                defaultRestSeconds = settings.restTimerSeconds,
                defaultPercent = settings.dropSetPercent,
                enteredWeightKg = exercise.plan.weightKg,
                inSuperset = routine?.exercises?.any { it.exerciseId == exerciseId && it.supersetId != null } == true,
                onSave = {
                    viewModel.savePlan(exerciseId, it)
                    editingPlan = null
                },
                onClear = {
                    viewModel.savePlan(exerciseId, exercise.plan.withoutSetPlan())
                    editingPlan = null
                },
                onDismiss = { editingPlan = null },
            )
        }
    }

    if (renaming) {
        TextInputDialog(
            title = "Rename plan",
            label = "Name",
            initialValue = routine?.name.orEmpty(),
            onConfirm = { name ->
                renaming = false
                viewModel.rename(name)
            },
            onDismiss = { renaming = false },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete plan?") },
            text = { Text("Workouts you already logged are not affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete(onBack)
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Up and down arrows; a null action disables its arrow. */
@Composable
private fun MoveButtons(name: String, onMoveUp: (() -> Unit)?, onMoveDown: (() -> Unit)?) {
    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        IconButton(onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move $name up")
        }
        IconButton(onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move $name down")
        }
    }
}

/** A superset of the plan: its exercises in order, moved up or down together. */
@Composable
private fun SupersetCard(
    members: List<RoutineExercise>,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onRemove: (RoutineExercise) -> Unit,
    onEdit: (RoutineExercise) -> Unit,
    planText: (RoutineExercise) -> String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Superset · ${members.size} exercises",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                val transition = members.firstNotNullOfOrNull { it.transitionSeconds }
                val rest = members.firstNotNullOfOrNull { it.roundRestSeconds }
                val timing = listOfNotNull(
                    transition?.let { "$it s to the next exercise" },
                    rest?.let { "${formatDuration(it)} rest after each round" },
                )
                if (timing.isNotEmpty()) {
                    Text(timing.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
            }
            MoveButtons("superset", onMoveUp, onMoveDown)
        }
        members.forEachIndexed { index, member ->
            ListItem(
                modifier = Modifier.clickable { onEdit(member) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text("${index + 1}. ${member.exerciseName}") },
                supportingContent = { Text(planText(member)) },
                leadingContent = { ColorDot(member.categoryColor) },
                trailingContent = {
                    IconButton(onClick = { onRemove(member) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove ${member.exerciseName}")
                    }
                },
            )
        }
    }
}
