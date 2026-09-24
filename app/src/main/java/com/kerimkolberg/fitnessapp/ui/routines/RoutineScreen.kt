@file:OptIn(ExperimentalMaterial3Api::class)

package com.kerimkolberg.fitnessapp.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kerimkolberg.fitnessapp.ui.components.ColorDot
import com.kerimkolberg.fitnessapp.ui.components.TextInputDialog

@Composable
fun RoutineScreen(
    onBack: () -> Unit,
    onAddExercise: (routineId: String) -> Unit,
    viewModel: RoutineViewModel = viewModel(factory = RoutineViewModel.Factory),
) {
    val routine by viewModel.routine.collectAsStateWithLifecycle()
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
        val exercises = routine?.exercises.orEmpty()
        if (exercises.isEmpty()) {
            Column(Modifier.padding(padding).padding(24.dp)) {
                Text(
                    text = "Add the exercises of this plan in the order you do them.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                itemsIndexed(exercises, key = { _, item -> item.id }) { index, item ->
                    ListItem(
                        headlineContent = { Text(item.exerciseName) },
                        leadingContent = { ColorDot(item.categoryColor) },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                            ) {
                                IconButton(onClick = { viewModel.moveExercise(item.id, -1) }, enabled = index > 0) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                                }
                                IconButton(
                                    onClick = { viewModel.moveExercise(item.id, 1) },
                                    enabled = index < exercises.lastIndex,
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                                }
                                IconButton(onClick = { viewModel.removeExercise(item.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove ${item.exerciseName}")
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
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
