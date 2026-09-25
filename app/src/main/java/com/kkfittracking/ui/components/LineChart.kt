package com.kkfittracking.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kkfittracking.model.ProgressPoint
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val axisDateFormat = DateTimeFormatter.ofPattern("MMM d")

/**
 * A single-series line chart over time. Tapping or dragging picks a point, whose value and date
 * are shown above the chart; the latest point is picked at first.
 */
@Composable
fun LineChart(
    points: List<ProgressPoint>,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
) {
    if (points.isEmpty()) {
        Text(
            text = "Nothing logged yet.",
            modifier = modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    var selected by remember(points) { mutableIntStateOf(points.lastIndex) }
    val colors = MaterialTheme.colorScheme
    val point = points[selected.coerceIn(points.indices)]

    Column(modifier = modifier) {
        Text(
            text = formatValue(point.value),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = formatShortDate(point.date),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        if (points.size == 1) {
            Text(
                text = "Log one more session to see a graph.",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        } else {
            ChartCanvas(points, formatValue, height, selected) { selected = it }
        }
    }
}

@Composable
private fun ChartCanvas(
    points: List<ProgressPoint>,
    formatValue: (Double) -> String,
    height: Dp,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val textMeasurer = rememberTextMeasurer()
    val minValue = points.minOf { it.value }
    val maxValue = points.maxOf { it.value }
    val span = (maxValue - minValue).takeIf { it > 0 } ?: maxOf(abs(maxValue) * 0.1, 1.0)
    val yMin = minValue - span * 0.1
    val yMax = maxValue + span * 0.1
    val yLabels = listOf(maxValue, (minValue + maxValue) / 2, minValue).distinct()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(top = 12.dp)
            .semantics {
                contentDescription = "Graph from ${formatValue(points.first().value)} to " +
                    "${formatValue(points.last().value)} over ${points.size} sessions"
            }
            .pointerInput(points) {
                detectTapGestures { onSelect(nearestIndex(points, it.x, size.width.toFloat(), 56.dp.toPx(), 8.dp.toPx())) }
            }
            .pointerInput(points) {
                detectHorizontalDragGestures { change, _ ->
                    onSelect(nearestIndex(points, change.position.x, size.width.toFloat(), 56.dp.toPx(), 8.dp.toPx()))
                }
            },
    ) {
        val leftPad = 56.dp.toPx()
        val rightPad = 8.dp.toPx()
        val bottomPad = 20.dp.toPx()
        val topPad = 6.dp.toPx()
        val plotWidth = size.width - leftPad - rightPad
        val plotHeight = size.height - bottomPad - topPad

        fun x(index: Int) = pointX(points, index, leftPad, plotWidth)
        fun y(value: Double) = topPad + plotHeight * ((yMax - value) / (yMax - yMin)).toFloat()

        // Recessive grid with value labels on the left.
        yLabels.forEach { value ->
            val lineY = y(value)
            drawLine(colors.outlineVariant, Offset(leftPad, lineY), Offset(size.width - rightPad, lineY), 1.dp.toPx())
            val label = textMeasurer.measure(formatValue(value), labelStyle)
            drawText(
                label,
                topLeft = Offset(leftPad - label.size.width - 6.dp.toPx(), lineY - label.size.height / 2f),
            )
        }
        // First and last dates under the axis.
        val firstLabel = textMeasurer.measure(points.first().date.format(axisDateFormat), labelStyle)
        drawText(firstLabel, topLeft = Offset(leftPad, size.height - firstLabel.size.height))
        val lastLabel = textMeasurer.measure(points.last().date.format(axisDateFormat), labelStyle)
        drawText(lastLabel, topLeft = Offset(size.width - rightPad - lastLabel.size.width, size.height - lastLabel.size.height))

        // Crosshair on the picked point.
        val selectedIndex = selected.coerceIn(points.indices)
        drawLine(
            colors.outline,
            Offset(x(selectedIndex), topPad),
            Offset(x(selectedIndex), topPad + plotHeight),
            1.dp.toPx(),
        )

        val path = Path()
        points.forEachIndexed { index, p ->
            if (index == 0) path.moveTo(x(index), y(p.value)) else path.lineTo(x(index), y(p.value))
        }
        drawPath(
            path,
            colors.primary,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        points.forEachIndexed { index, p ->
            val center = Offset(x(index), y(p.value))
            val radius = if (index == selectedIndex) 6.dp.toPx() else 4.dp.toPx()
            // A ring in the surface color keeps overlapping dots apart.
            drawCircle(colors.surface, radius + 2.dp.toPx(), center)
            drawCircle(colors.primary, radius, center)
        }
    }
}

/** The horizontal position of a point: dates are spaced by time, not by index. */
private fun pointX(points: List<ProgressPoint>, index: Int, leftPad: Float, plotWidth: Float): Float {
    val firstDay = points.first().date.toEpochDay()
    val daySpan = (points.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
    return leftPad + plotWidth * (points[index].date.toEpochDay() - firstDay) / daySpan
}

private fun nearestIndex(points: List<ProgressPoint>, x: Float, width: Float, leftPad: Float, rightPad: Float): Int {
    val plotWidth = width - leftPad - rightPad
    return points.indices.minBy { abs(pointX(points, it, leftPad, plotWidth) - x) }
}
