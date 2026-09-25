@file:OptIn(ExperimentalMaterial3Api::class)

package com.kkfittracking.ui.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkfittracking.model.BodyMetric
import com.kkfittracking.model.Exercise
import com.kkfittracking.model.ProgressPoint
import com.kkfittracking.model.TimeRange
import com.kkfittracking.model.within
import com.kkfittracking.ui.components.ColorDot
import com.kkfittracking.ui.components.LineChart
import com.kkfittracking.ui.components.formatShortDate
import com.kkfittracking.ui.log.GraphSummary
import com.kkfittracking.ui.log.ProgressTab

/**
 * Graphs and records in one place: an exercise's progress over time (weight, 1RM, volume, reps,
 * time… by date), the body tracker's graphs, and every exercise's personal record.
 */
@Composable
fun AnalysisScreen(onBack: () -> Unit, viewModel: AnalysisViewModel = viewModel(factory = AnalysisViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var exerciseId by rememberSaveable { mutableStateOf(viewModel.startExerciseId) }
    var range by rememberSaveable { mutableStateOf(TimeRange.ALL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Graphs & records") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Exercises") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Body") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Records") })
            }
            if (tab != 2 && !state.isLoading) RangeChips(range) { range = it }
            when {
                state.isLoading -> Unit
                tab == 0 -> {
                    val exercise = state.logged.firstOrNull { it.id == exerciseId } ?: state.logged.firstOrNull()
                    if (exercise == null) {
                        Empty("Log some sets to see graphs of your exercises.")
                    } else {
                        ExerciseChooser(exercise, state.logged) { exerciseId = it.id }
                        Column(Modifier.weight(1f)) {
                            ProgressTab(state.histories[exercise.id].orEmpty().within(range), exercise.type, state.units)
                        }
                    }
                }
                tab == 1 -> BodyGraphs(state, range)
                else -> Records(state) { exercise ->
                    exerciseId = exercise.id
                    tab = 0
                }
            }
        }
    }
}

@Composable
private fun RangeChips(range: TimeRange, onSelect: (TimeRange) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(TimeRange.entries) { option ->
            FilterChip(selected = option == range, onClick = { onSelect(option) }, label = { Text(option.label) })
        }
    }
}

/** The exercise shown; tapping it lists every logged exercise, with a search. */
@Composable
private fun ExerciseChooser(selected: Exercise, logged: List<Exercise>, onSelect: (Exercise) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { choosing = true },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Text(selected.name, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose an exercise")
    }
    if (choosing) {
        var query by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text("Exercise") },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val words = query.trim().lowercase().split(" ").filter { it.isNotEmpty() }
                    LazyColumn(Modifier.heightIn(max = 400.dp)) {
                        items(logged.filter { e -> words.all { it in e.name.lowercase() } }, key = { it.id }) { exercise ->
                            ListItem(
                                modifier = Modifier.clickable {
                                    onSelect(exercise)
                                    choosing = false
                                },
                                headlineContent = { Text(exercise.name) },
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosing = false }) { Text("Close") } },
        )
    }
}

/** A body tracker graph: bodyweight, body fat or a measurement, by date. */
@Composable
private fun BodyGraphs(state: AnalysisUiState, range: TimeRange) {
    val tracked = BodyMetric.entries.filter { metric -> state.measurements.any { it.metric == metric } }
    if (tracked.isEmpty()) {
        Empty("Add your bodyweight or measurements in the body tracker to see them here.")
        return
    }
    var chosen by rememberSaveable { mutableStateOf(tracked.first()) }
    val metric = if (chosen in tracked) chosen else tracked.first()
    val start = range.start(java.time.LocalDate.now())
    val points = state.measurements
        .filter { it.metric == metric && (start == null || !it.date.isBefore(start)) }
        .sortedBy { it.date }
        .map { ProgressPoint(it.date, it.value) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tracked) { option ->
                FilterChip(selected = option == metric, onClick = { chosen = option }, label = { Text(option.label) })
            }
        }
        Text(
            text = "${metric.label} (${metric.unit(state.units)})",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LineChart(points = points, formatValue = { metric.format(it, state.units) })
        GraphSummary(points) { metric.format(it, state.units) }
    }
}

/** Every logged exercise's personal record, newest first; tapping one shows its graph. */
@Composable
private fun Records(state: AnalysisUiState, onOpen: (Exercise) -> Unit) {
    if (state.records.isEmpty()) {
        Empty("Your personal records will show up here once you log some sets.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(state.records, key = { it.exercise.id }) { entry ->
            ListItem(
                modifier = Modifier.clickable { onOpen(entry.exercise) },
                leadingContent = { ColorDot(0xFFFFC107.toInt()) },
                headlineContent = { Text(entry.exercise.name) },
                supportingContent = {
                    Text("${entry.record.label} · ${formatShortDate(entry.record.date)} · ${entry.sessions} sessions")
                },
                trailingContent = { Text(entry.record.value, fontWeight = FontWeight.SemiBold) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun Empty(text: String) {
    Text(text, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}
