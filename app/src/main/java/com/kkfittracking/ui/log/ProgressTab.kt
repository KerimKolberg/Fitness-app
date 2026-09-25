package com.kkfittracking.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kkfittracking.model.ExerciseType
import com.kkfittracking.model.HistorySession
import com.kkfittracking.model.ProgressMetric
import com.kkfittracking.model.UnitSystem
import com.kkfittracking.model.formatNumber
import com.kkfittracking.model.personalRecords
import com.kkfittracking.model.progressPoints
import com.kkfittracking.model.progressSummary
import com.kkfittracking.model.repMaxes
import com.kkfittracking.ui.components.LineChart
import com.kkfittracking.ui.components.formatShortDate

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
            progressSummary(points)?.takeIf { points.size > 1 }?.let { summary ->
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryTile("All-time high", metric.format(summary.high.value, units), formatShortDate(summary.high.date), MaterialTheme.colorScheme.tertiary)
                    SummaryTile("All-time low", metric.format(summary.low.value, units), formatShortDate(summary.low.date), MaterialTheme.colorScheme.error)
                    val change = summary.change
                    SummaryTile(
                        "Since the start",
                        (if (change >= 0) "+" else "−") + metric.format(kotlin.math.abs(change), units),
                        formatShortDate(summary.first.date),
                        MaterialTheme.colorScheme.primary,
                    )
                }
            }
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

/** One figure under the graph, such as the all-time high, with its date. */
@Composable
private fun RowScope.SummaryTile(label: String, value: String, date: String, color: Color) {
    Card(modifier = Modifier.weight(1f)) {
        Column(Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
