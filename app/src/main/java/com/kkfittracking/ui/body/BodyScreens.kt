@file:OptIn(ExperimentalMaterial3Api::class)

package com.kkfittracking.ui.body

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkfittracking.model.BodyMeasurement
import com.kkfittracking.model.BodyMetric
import com.kkfittracking.model.ProgressPoint
import com.kkfittracking.model.formatNumber
import com.kkfittracking.ui.components.LineChart
import com.kkfittracking.ui.components.formatShortDate
import java.time.Instant
import java.time.ZoneOffset

@Composable
fun BodyScreen(
    onBack: () -> Unit,
    onOpenMetric: (BodyMetric) -> Unit,
    viewModel: BodyViewModel = viewModel(factory = BodyViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(BodyMetric.entries) { metric ->
                val latest = state.latest(metric)
                ListItem(
                    modifier = Modifier.clickable { onOpenMetric(metric) },
                    headlineContent = { Text(metric.label) },
                    supportingContent = {
                        Text(latest?.let { formatShortDate(it.date) } ?: "No entries yet")
                    },
                    trailingContent = {
                        latest?.let {
                            Text(metric.format(it.value, state.units), style = MaterialTheme.typography.titleMedium)
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun BodyMetricScreen(
    onBack: () -> Unit,
    viewModel: BodyMetricViewModel = viewModel(factory = BodyMetricViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val metric = viewModel.metric
    val entries = state.entries(metric)
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<BodyMeasurement?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(metric.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { pickingDate = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Text(formatShortDate(viewModel.date), modifier = Modifier.padding(start = 8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = viewModel.valueText,
                                onValueChange = viewModel::updateValue,
                                modifier = Modifier.weight(1f),
                                label = { Text("${metric.label} (${metric.unit(state.units)})") },
                                singleLine = true,
                                isError = viewModel.error != null,
                                supportingText = viewModel.error?.let { error -> { Text(error) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                            Button(onClick = viewModel::save, modifier = Modifier.padding(start = 12.dp)) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
            item {
                val points = entries.reversed().map { ProgressPoint(it.date, it.value) }
                LineChart(points = points, formatValue = { metric.format(it, state.units) })
            }
            if (entries.isNotEmpty()) {
                item { Text("History", style = MaterialTheme.typography.titleMedium) }
            }
            items(entries, key = { it.id }) { entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatShortDate(entry.date), modifier = Modifier.weight(1f))
                    Text(
                        text = "${formatNumber(metric.toDisplay(entry.value, state.units))} ${metric.unit(state.units)}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    IconButton(onClick = { toDelete = entry }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                    }
                }
            }
        }
    }

    if (pickingDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            viewModel.updateDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        pickingDate = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
    toDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("${formatShortDate(entry.date)}: ${metric.format(entry.value, state.units)}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(entry.id)
                        toDelete = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

