@file:OptIn(ExperimentalMaterial3Api::class)

package com.kerimkolberg.fitnessapp.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kerimkolberg.fitnessapp.model.ExerciseType
import com.kerimkolberg.fitnessapp.model.MAX_SUPERSET_SIZE
import com.kerimkolberg.fitnessapp.ui.components.ColorDot

@Composable
fun ExercisePickerScreen(
    onBack: () -> Unit,
    onLogExercise: (String) -> Unit,
    onCreateExercise: () -> Unit,
    onEditExercise: (String) -> Unit,
    onSupersetCreated: (firstExerciseId: String) -> Unit = {},
    viewModel: ExercisePickerViewModel = viewModel(factory = ExercisePickerViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            viewModel.supersetMode -> "New superset (${viewModel.supersetPicks.size}/$MAX_SUPERSET_SIZE)"
                            viewModel.routineId != null -> "Add to plan"
                            else -> "Choose exercise"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateExercise) {
                        Icon(Icons.Default.Add, contentDescription = "New exercise")
                    }
                },
            )
        },
        floatingActionButton = {
            if (viewModel.supersetMode && viewModel.supersetPicks.size >= 2) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.createSuperset(onSupersetCreated) },
                    text = { Text("Create superset (${viewModel.supersetPicks.size})") },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (viewModel.supersetMode) {
                Text(
                    text = "Tick 2 to $MAX_SUPERSET_SIZE exercises in the order you will do them. " +
                        "Exercises already on this day are moved into the superset.",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (viewModel.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategoryId == null && state.selectedPlanId == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All") },
                    )
                }
                // Plans come first when not adding to a plan: they are the quickest way to find exercises.
                if (viewModel.routineId == null) {
                    items(state.plans, key = { "plan-${it.id}" }) { plan ->
                        FilterChip(
                            selected = state.selectedPlanId == plan.id,
                            onClick = { viewModel.selectPlan(plan.id) },
                            label = { Text(plan.name) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        )
                    }
                }
                items(state.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = state.selectedCategoryId == category.id,
                        onClick = { viewModel.selectCategory(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = { ColorDot(category.color) },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                if (state.groups.isEmpty() && state.categories.isNotEmpty()) {
                    item {
                        Text(
                            text = "No exercises found. Tap + to create one.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                state.groups.forEach { group ->
                    item(key = "header-${group.category.id}") {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = group.category.name.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(group.category.color),
                            )
                        }
                    }
                    items(group.exercises, key = { it.id }) { exercise ->
                        ListItem(
                            modifier = Modifier.clickable { viewModel.pick(exercise.id, onLogExercise, onBack) },
                            headlineContent = { Text(exercise.name) },
                            supportingContent = if (exercise.type != ExerciseType.WEIGHT_REPS) {
                                { Text(exercise.type.label) }
                            } else {
                                null
                            },
                            leadingContent = { ColorDot(group.category.color) },
                            trailingContent = {
                                if (viewModel.supersetMode) {
                                    val position = viewModel.supersetPicks.indexOf(exercise.id)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (position >= 0) {
                                            Text("${position + 1}", color = MaterialTheme.colorScheme.primary)
                                        }
                                        Checkbox(
                                            checked = position >= 0,
                                            onCheckedChange = { viewModel.pick(exercise.id, onLogExercise, onBack) },
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { onEditExercise(exercise.id) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit ${exercise.name}")
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
