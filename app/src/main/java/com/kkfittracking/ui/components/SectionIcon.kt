package com.kkfittracking.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kkfittracking.model.Regions

/**
 * An icon for a body section: a small figure with that part of the body in the section's color,
 * or a heart (cardio), a ball (sports) or a lotus (mind). Drawn here, so there is no artwork to
 * license. Sections without an icon get a colored dot.
 */
@Composable
fun SectionIcon(key: String?, color: Int, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val highlight = Color(color)
    val body = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val cutout = MaterialTheme.colorScheme.surface
    when (key) {
        Regions.CARDIO -> Canvas(modifier.size(size)) { heart(highlight) }
        Regions.SPORTS -> Canvas(modifier.size(size)) { ball(highlight, cutout) }
        Regions.MIND -> Canvas(modifier.size(size)) { lotus(highlight) }
        Regions.CHEST, Regions.BACK, Regions.SHOULDERS, Regions.ARMS, Regions.LEGS, Regions.CORE, Regions.FULL_BODY ->
            Canvas(modifier.size(size)) { figure(key, highlight, body, cutout) }
        else -> ColorDot(color, modifier, size = size / 2)
    }
}

// Each icon is drawn in a 24 × 24 grid, scaled to the canvas by u.

private fun DrawScope.figure(key: String?, highlight: Color, body: Color, cutout: Color) {
    val u = size.minDimension / 24f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun paint(part: String) = if (key == Regions.FULL_BODY || key == part) highlight else body

    // Head
    drawCircle(if (key == Regions.FULL_BODY) highlight else body, 2.4f * u, p(12f, 3.6f))
    // Torso: chest on top, core below; the back is the whole torso, with the spine drawn over it.
    val torsoBack = key == Regions.BACK
    drawRoundRect(
        if (torsoBack) highlight else paint(Regions.CHEST), p(8.5f, 6.8f), Size(7f * u, 4f * u), CornerRadius(1.5f * u),
    )
    drawRoundRect(if (torsoBack) highlight else paint(Regions.CORE), p(9f, 10.6f), Size(6f * u, 4.6f * u), CornerRadius(1f * u))
    if (torsoBack) drawLine(cutout, p(12f, 7.5f), p(12f, 14.6f), 0.9f * u, StrokeCap.Round)
    // Shoulders
    drawCircle(paint(Regions.SHOULDERS), 1.7f * u, p(7.6f, 7.9f))
    drawCircle(paint(Regions.SHOULDERS), 1.7f * u, p(16.4f, 7.9f))
    // Arms
    drawLine(paint(Regions.ARMS), p(6.9f, 9.6f), p(5.6f, 15.4f), 2.1f * u, StrokeCap.Round)
    drawLine(paint(Regions.ARMS), p(17.1f, 9.6f), p(18.4f, 15.4f), 2.1f * u, StrokeCap.Round)
    // Legs
    drawLine(paint(Regions.LEGS), p(10.4f, 16f), p(9.7f, 22.4f), 2.6f * u, StrokeCap.Round)
    drawLine(paint(Regions.LEGS), p(13.6f, 16f), p(14.3f, 22.4f), 2.6f * u, StrokeCap.Round)
}

private fun DrawScope.heart(color: Color) {
    val u = size.minDimension / 24f
    val path = Path().apply {
        moveTo(12f * u, 20.5f * u)
        cubicTo(4f * u, 14.5f * u, 2f * u, 10f * u, 3.6f * u, 6.8f * u)
        cubicTo(5.4f * u, 3.4f * u, 10.2f * u, 3.4f * u, 12f * u, 7.4f * u)
        cubicTo(13.8f * u, 3.4f * u, 18.6f * u, 3.4f * u, 20.4f * u, 6.8f * u)
        cubicTo(22f * u, 10f * u, 20f * u, 14.5f * u, 12f * u, 20.5f * u)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.ball(color: Color, cutout: Color) {
    val u = size.minDimension / 24f
    val center = Offset(12f * u, 12f * u)
    drawCircle(color, 9f * u, center)
    val seam = Stroke(width = 1.1f * u)
    drawArc(cutout, -60f, 120f, false, Offset(-1f * u, 3f * u), Size(18f * u, 18f * u), style = seam)
    drawArc(cutout, 120f, 120f, false, Offset(7f * u, 3f * u), Size(18f * u, 18f * u), style = seam)
}

private fun DrawScope.lotus(color: Color) {
    val u = size.minDimension / 24f
    val petal = Size(5f * u, 11f * u)
    listOf(-50f, -25f, 0f, 25f, 50f).forEach { angle ->
        rotate(angle, pivot = Offset(12f * u, 19f * u)) {
            drawOval(color.copy(alpha = if (angle == 0f) 1f else 0.75f), Offset(9.5f * u, 8f * u), petal)
        }
    }
    drawLine(color, Offset(5f * u, 20.5f * u), Offset(19f * u, 20.5f * u), 1.4f * u, StrokeCap.Round)
}
