@file:OptIn(ExperimentalMaterial3Api::class)

package com.kerimkolberg.fitnessapp.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kerimkolberg.fitnessapp.model.DayBlock
import com.kerimkolberg.fitnessapp.model.DayExercise
import com.kerimkolberg.fitnessapp.model.GameStats
import com.kerimkolberg.fitnessapp.model.Routine
import com.kerimkolberg.fitnessapp.model.UnitSystem
import com.kerimkolberg.fitnessapp.model.formatSet
import com.kerimkolberg.fitnessapp.model.groupDay
import com.kerimkolberg.fitnessapp.model.setLabels
import com.kerimkolberg.fitnessapp.ui.components.formatFullDate
import com.kerimkolberg.fitnessapp.ui.components.relativeDayName
import java.time.LocalDate

@Composable
fun WorkoutScreen(
    onAddExercise: (LocalDate) -> Unit,
    onOpenExercise: (LocalDate, String) -> Unit,
    onOpenCalendar: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenAchievements: () -> Unit,
    onNewSuperset: (LocalDate) -> Unit,
    viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val gameStats by viewModel.gameStats.collectAsStateWithLifecycle()
    var exerciseToDelete by remember { mutableStateOf<DayExercise?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var choosingRoutine by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout log") },
                actions = {
                    IconButton(onClick = { onOpenCalendar(state.date) }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Calendar")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            val close = { menuOpen = false }
                            MenuItem("Add a plan to this day", close) { choosingRoutine = true }
                            MenuItem("New superset (2–6 exercises)", close) { onNewSuperset(state.date) }
                            if (state.exercises.isNotEmpty() && state.date != LocalDate.now()) {
                                MenuItem("Copy exercises to today", close, viewModel::copyExercisesToToday)
                            }
                            HorizontalDivider()
                            MenuItem("Plans", close, onOpenRoutines)
                            MenuItem("Body tracker", close, onOpenBody)
                            MenuItem("Progress & achievements", close, onOpenAchievements)
                            MenuItem("Settings", close, onOpenSettings)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.exercises.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onAddExercise(state.date) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add exercise") },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            DateSwitcher(
                date = state.date,
                onPrevious = viewModel::showPreviousDay,
                onNext = viewModel::showNextDay,
                onToday = viewModel::showToday,
            )
            gameStats?.let { GameSummaryBar(it, onClick = onOpenAchievements) }
            HorizontalDivider()
            when {
                state.isLoading -> Unit
                state.exercises.isEmpty() -> EmptyDay(
                    onAddExercise = { onAddExercise(state.date) },
                    onUseRoutine = { choosingRoutine = true },
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val blocks = groupDay(state.exercises)
                    items(blocks, key = { block ->
                        when (block) {
                            is DayBlock.Single -> block.exercise.workoutExerciseId
                            is DayBlock.Superset -> "superset-${block.id}"
                        }
                    }) { block ->
                        when (block) {
                            is DayBlock.Single -> DayExerciseCard(
                                exercise = block.exercise,
                                units = state.units,
                                onClick = { onOpenExercise(state.date, block.exercise.exerciseId) },
                                onDelete = { exerciseToDelete = block.exercise },
                            )
                            is DayBlock.Superset -> SupersetCard(
                                superset = block,
                                units = state.units,
                                onOpen = { onOpenExercise(state.date, it.exerciseId) },
                                onDelete = { exerciseToDelete = it },
                                onUngroup = { viewModel.ungroupSuperset(block.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (choosingRoutine) {
        RoutineChooserDialog(
            routines = routines,
            onChoose = { routine ->
                choosingRoutine = false
                viewModel.applyRoutine(routine.id)
            },
            onManageRoutines = {
                choosingRoutine = false
                onOpenRoutines()
            },
            onDismiss = { choosingRoutine = false },
        )
    }

    exerciseToDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Remove exercise?") },
            text = { Text("${exercise.exerciseName} and its ${exercise.sets.size} set(s) will be removed from this day.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExercise(exercise.workoutExerciseId)
                        exerciseToDelete = null
                    },
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DateSwitcher(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onToday)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = relativeDayName(date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatFullDate(date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun EmptyDay(onAddExercise: () -> Unit, onUseRoutine: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Workout log empty", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Nothing logged on this day yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddExercise) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start new workout")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onUseRoutine) {
            Text("Use a plan")
        }
    }
}

@Composable
private fun DayExerciseCard(
    exercise: DayExercise,
    units: UnitSystem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ExerciseBody(exercise, units, onDelete)
    }
}

/** Several exercises done back to back, shown together in one card. */
@Composable
private fun SupersetCard(
    superset: DayBlock.Superset,
    units: UnitSystem,
    onOpen: (DayExercise) -> Unit,
    onDelete: (DayExercise) -> Unit,
    onUngroup: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Superset · ${superset.exercises.size} exercises",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Superset options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Ungroup superset") },
                        onClick = {
                            menuOpen = false
                            onUngroup()
                        },
                    )
                }
            }
        }
        superset.exercises.forEachIndexed { index, exercise ->
            if (index > 0) HorizontalDivider(Modifier.padding(start = 22.dp))
            Box(Modifier.clickable { onOpen(exercise) }) {
                ExerciseBody(exercise, units, onDelete = { onDelete(exercise) }, position = index + 1)
            }
        }
    }
}

/** An exercise's name, menu and sets, with its category color on the left. */
@Composable
private fun ExerciseBody(
    exercise: DayExercise,
    units: UnitSystem,
    onDelete: () -> Unit,
    position: Int? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(Color(exercise.categoryColor)),
        )
        Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = position?.let { "$it. ${exercise.exerciseName}" } ?: exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Remove from day") },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            if (exercise.sets.isEmpty()) {
                Text(
                    text = "Not started yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val labels = setLabels(exercise.sets)
            exercise.sets.forEachIndexed { index, set ->
                Row(modifier = Modifier.padding(end = 16.dp, top = 2.dp)) {
                    Text(
                        text = labels[index],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(28.dp),
                    )
                    Text(
                        text = formatSet(set.values, exercise.exerciseType, units),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineChooserDialog(
    routines: List<Routine>,
    onChoose: (Routine) -> Unit,
    onManageRoutines: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a plan to this day") },
        text = {
            if (routines.isEmpty()) {
                Text("You have no plans yet. Create one, or add the starter plans.")
            } else {
                LazyColumn {
                    items(routines, key = { it.id }) { routine ->
                        ListItem(
                            modifier = Modifier.clickable { onChoose(routine) },
                            headlineContent = { Text(routine.name) },
                            supportingContent = { Text("${routine.exercises.size} exercises") },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onManageRoutines) { Text("Manage plans") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MenuItem(text: String, closeMenu: () -> Unit, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = {
            closeMenu()
            onClick()
        },
    )
}

/** Level, XP and this week's goal at a glance. Tapping it opens the achievements. */
@Composable
private fun GameSummaryBar(stats: GameStats, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Level ${stats.level}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "  ${stats.xpIntoLevel}/${stats.xpForLevel} XP",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = buildString {
                    if (stats.weekStreak > 0) append("🔥 ${stats.weekStreak} wk · ")
                    append("${stats.workoutsThisWeek}/${stats.weeklyGoal} this week")
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
        LinearProgressIndicator(
            progress = { stats.xpIntoLevel.toFloat() / stats.xpForLevel },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
