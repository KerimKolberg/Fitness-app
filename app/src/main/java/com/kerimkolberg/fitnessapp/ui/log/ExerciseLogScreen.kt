@file:OptIn(ExperimentalMaterial3Api::class)

package com.kerimkolberg.fitnessapp.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kerimkolberg.fitnessapp.R
import com.kerimkolberg.fitnessapp.model.ExerciseType
import com.kerimkolberg.fitnessapp.model.HistorySession
import com.kerimkolberg.fitnessapp.model.UnitSystem
import com.kerimkolberg.fitnessapp.model.formatDuration
import com.kerimkolberg.fitnessapp.model.formatSet
import com.kerimkolberg.fitnessapp.timer.RestTimerState
import com.kerimkolberg.fitnessapp.ui.components.formatShortDate
import com.kerimkolberg.fitnessapp.ui.components.rememberNotificationPermissionRequester
import java.time.LocalDate

@Composable
fun ExerciseLogScreen(
    onBack: () -> Unit,
    onEditExercise: (String) -> Unit,
    viewModel: ExerciseLogViewModel = viewModel(factory = ExerciseLogViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val timer by viewModel.timerState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showTimer by remember { mutableStateOf(false) }
    val requestNotificationPermission = rememberNotificationPermissionRequester()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.exercise?.name.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatShortDate(viewModel.date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TimerAction(timer = timer, onClick = { showTimer = true })
                    IconButton(onClick = { onEditExercise(viewModel.exerciseId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit exercise")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Track") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("History") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Progress") })
            }
            val exercise = state.exercise
            when {
                exercise == null -> Unit
                selectedTab == 0 -> TrackTab(
                    viewModel = viewModel,
                    state = state,
                    type = exercise.type,
                    onSave = {
                        if (viewModel.selectedSetId == null && state.settings.autoStartRestTimer) {
                            requestNotificationPermission()
                        }
                        viewModel.save()
                    },
                )
                selectedTab == 1 -> HistoryTab(state = state, type = exercise.type, currentDate = viewModel.date)
                else -> ProgressTab(history = state.history, type = exercise.type, units = state.units)
            }
        }
    }

    if (showTimer) {
        RestTimerDialog(
            timer = timer,
            defaultSeconds = state.settings.restTimerSeconds,
            onStart = { seconds ->
                requestNotificationPermission()
                viewModel.startTimer(seconds)
            },
            onStop = viewModel::stopTimer,
            onAddSeconds = viewModel::addTimerSeconds,
            onDismiss = { showTimer = false },
        )
    }
}

@Composable
private fun TimerAction(timer: RestTimerState, onClick: () -> Unit) {
    if (timer.isRunning) {
        TextButton(onClick = onClick) {
            Icon(painterResource(R.drawable.ic_timer), contentDescription = "Rest timer")
            Spacer(Modifier.width(4.dp))
            Text(formatDuration(timer.remainingSeconds))
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(painterResource(R.drawable.ic_timer), contentDescription = "Rest timer")
        }
    }
}

@Composable
private fun TrackTab(
    viewModel: ExerciseLogViewModel,
    state: ExerciseLogUiState,
    type: ExerciseType,
    onSave: () -> Unit,
) {
    val input = viewModel.input
    val units = state.units
    val isEditing = viewModel.selectedSetId != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (type.usesWeight) {
                    StepperField(
                        label = "Weight (${units.weightUnit})",
                        value = input.weight,
                        onValueChange = { viewModel.updateInput(input.copy(weight = it)) },
                        onDecrement = { viewModel.adjustWeight(-1) },
                        onIncrement = { viewModel.adjustWeight(1) },
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                if (type.usesReps) {
                    StepperField(
                        label = "Reps",
                        value = input.reps,
                        onValueChange = { viewModel.updateInput(input.copy(reps = it)) },
                        onDecrement = { viewModel.adjustReps(-1) },
                        onIncrement = { viewModel.adjustReps(1) },
                        keyboardType = KeyboardType.Number,
                    )
                }
                if (type.usesDistance) {
                    OutlinedTextField(
                        value = input.distance,
                        onValueChange = { viewModel.updateInput(input.copy(distance = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Distance (${units.distanceUnit})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                if (type.usesTime) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = input.minutes,
                            onValueChange = { viewModel.updateInput(input.copy(minutes = it)) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Minutes") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(
                            value = input.seconds,
                            onValueChange = { viewModel.updateInput(input.copy(seconds = it)) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Seconds") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
                viewModel.errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Text(if (isEditing) "Update" else "Save")
                    }
                    if (isEditing) {
                        OutlinedButton(onClick = viewModel::deleteSelectedSet, modifier = Modifier.weight(1f)) {
                            Text("Delete")
                        }
                    } else {
                        OutlinedButton(onClick = viewModel::clearInput, modifier = Modifier.weight(1f)) {
                            Text("Clear")
                        }
                    }
                }
            }
        }

        state.previousSession?.let { previous ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "Last time · ${formatShortDate(previous.date)}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = previous.sets.joinToString(", ") { formatSet(it.values, type, units) },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        item {
            HorizontalDivider()
        }

        if (state.sets.isEmpty()) {
            item {
                Text(
                    text = "No sets logged yet. Enter your values and tap Save.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        itemsIndexed(state.sets, key = { _, set -> set.id }) { index, set ->
            val selected = set.id == viewModel.selectedSetId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { viewModel.selectSet(set) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}",
                    modifier = Modifier.width(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatSet(set.values, type, units),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (set.id in state.recordSetIds) RecordBadge()
            }
        }
    }
}

@Composable
private fun StepperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    keyboardType: KeyboardType,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onDecrement) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            label = { Text(label) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
        FilledTonalIconButton(onClick = onIncrement) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun HistoryTab(state: ExerciseLogUiState, type: ExerciseType, currentDate: LocalDate) {
    if (state.history.isEmpty()) {
        Text(
            text = "No history yet for this exercise.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        historySessions(state.history, type, state.units, currentDate, state.recordSetIds)
    }
}

private fun LazyListScope.historySessions(
    sessions: List<HistorySession>,
    type: ExerciseType,
    units: UnitSystem,
    currentDate: LocalDate,
    recordSetIds: Set<String>,
) {
    sessions.forEach { session ->
        item(key = session.date.toEpochDay()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = formatShortDate(session.date) + if (session.date == currentDate) " (this workout)" else "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    session.sets.forEachIndexed { index, set ->
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${index + 1}",
                                modifier = Modifier.width(28.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(formatSet(set.values, type, units))
                            if (set.id in recordSetIds) RecordBadge()
                        }
                    }
                }
            }
        }
    }
}

/** Marks a set that beat every earlier set of the exercise. */
@Composable
private fun RecordBadge() {
    Icon(
        imageVector = Icons.Default.Star,
        contentDescription = "Personal record",
        modifier = Modifier
            .padding(start = 8.dp)
            .size(18.dp),
        tint = MaterialTheme.colorScheme.secondary,
    )
}
