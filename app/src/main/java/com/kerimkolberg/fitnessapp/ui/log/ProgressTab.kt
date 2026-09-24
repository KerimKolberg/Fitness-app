package com.kerimkolberg.fitnessapp.ui.log

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
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kerimkolberg.fitnessapp.model.ExerciseType
import com.kerimkolberg.fitnessapp.model.HistorySession
import com.kerimkolberg.fitnessapp.model.ProgressMetric
import com.kerimkolberg.fitnessapp.model.UnitSystem
import com.kerimkolberg.fitnessapp.model.formatNumber
import com.kerimkolberg.fitnessapp.model.personalRecords
import com.kerimkolberg.fitnessapp.model.progressPoints
import com.kerimkolberg.fitnessapp.model.repMaxes
import com.kerimkolberg.fitnessapp.ui.components.LineChart
import com.kerimkolberg.fitnessapp.ui.components.formatShortDate

/** Personal records, a progress graph, and (for weighted exercises) rep maxes. */
@Composable
fun ProgressTab(history: List<HistorySession>, type: ExerciseType, units: UnitSystem) {
    if (history.isEmpty()) {
        Text(
            text = "Log some sets to see your records and progress.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val metrics = ProgressMetric.forType(type)
    var metric by rememberSaveable(type) { mutableStateOf(metrics.first()) }
    val points = remember(history, metric) { progressPoints(history, metric) }
    val records = remember(history, type, units) { personalRecords(history, type, units) }
    val maxes = remember(history, type) { if (type == ExerciseType.WEIGHT_REPS) repMaxes(history) else emptyMap() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionTitle("Personal records")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    records.forEach { record ->
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text(record.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = formatShortDate(record.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(record.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("Progress")
            if (metrics.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(metrics) { option ->
                        FilterChip(
                            selected = option == metric,
                            onClick = { metric = option },
                            label = { Text(option.label) },
                        )
                    }
                }
            }
            Text(
                text = metric.label,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LineChart(points = points, formatValue = { metric.format(it, units) })
        }
        if (maxes.isNotEmpty()) {
            item {
                SectionTitle("Rep maxes")
                Text(
                    text = "Heaviest weight lifted for at least this many reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        maxes.forEach { (reps, kg) ->
                            Row {
                                Text(if (reps == 1) "1 rep" else "$reps reps", modifier = Modifier.weight(1f))
                                Text("${formatNumber(units.weightFromKg(kg))} ${units.weightUnit}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}
