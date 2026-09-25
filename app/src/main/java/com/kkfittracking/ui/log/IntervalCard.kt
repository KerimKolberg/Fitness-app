package com.kkfittracking.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkfittracking.model.ExercisePlan
import com.kkfittracking.model.formatDuration
import com.kkfittracking.timer.IntervalPhase
import com.kkfittracking.timer.IntervalTimer
import com.kkfittracking.timer.IntervalTimerState

/**
 * The HIIT interval timer: set the high and low intensity times and the rounds, then start. It
 * beeps and vibrates at every change, so the phone can stay on the floor or in a pocket.
 */
@Composable
fun IntervalCard(
    timer: IntervalTimerState,
    plan: ExercisePlan,
    exerciseId: String,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    onChange: (highDelta: Int, lowDelta: Int, roundsDelta: Int) -> Unit,
) {
    val ours = timer.exerciseId == exerciseId
    // Keep the screen on while the workout runs.
    val view = LocalView.current
    val keepOn = ours && timer.isActive
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    when {
        timer.isActive && !ours -> OtherTimer(onStop = onReset)
        timer.isActive -> RunningTimer(timer, onPause, onResume, onFinish)
        timer.phase == IntervalPhase.DONE && ours -> Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Done! 💪", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "${timer.completedRounds} rounds · ${formatDuration(timer.workoutSeconds)}. " +
                        "Add how hard it was, then tap Save.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onReset) { Text("Start over") }
            }
        }
        else -> TimerSetup(plan, onStart, onChange)
    }
}

@Composable
private fun TimerSetup(plan: ExercisePlan, onStart: () -> Unit, onChange: (Int, Int, Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
            Text("Interval timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Setting("High intensity", formatDuration(plan.intervalHighSeconds), { onChange(-5, 0, 0) }, { onChange(5, 0, 0) })
            Setting("Low intensity", formatDuration(plan.intervalLowSeconds), { onChange(0, -5, 0) }, { onChange(0, 5, 0) })
            Setting("Rounds", "${plan.intervalRounds}", { onChange(0, 0, -1) }, { onChange(0, 0, 1) })
            // The last round ends after its high part.
            val total = plan.intervalRounds * plan.intervalHighSeconds + (plan.intervalRounds - 1) * plan.intervalLowSeconds
            Row(Modifier.padding(top = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Total ${formatDuration(total)}, after ${IntervalTimer.GET_READY_SECONDS} s to get ready",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onStart) { Text("Start") }
            }
        }
    }
}

@Composable
private fun Setting(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = onMinus) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(value, modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleMedium)
        FilledTonalIconButton(onClick = onPlus) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun RunningTimer(timer: IntervalTimerState, onPause: () -> Unit, onResume: () -> Unit, onFinish: () -> Unit) {
    val (container, content) = when (timer.phase) {
        IntervalPhase.HIGH -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        IntervalPhase.LOW -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(timer.phase.label.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Round ${timer.round} of ${timer.rounds}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = formatDuration(timer.remainingSeconds),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (timer.isPaused) content.copy(alpha = 0.5f) else Color.Unspecified,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (timer.isPaused) {
                    Button(onClick = onResume) { Text("Resume") }
                } else {
                    OutlinedButton(onClick = onPause) { Text("Pause") }
                }
                OutlinedButton(onClick = onFinish) { Text("Finish") }
            }
        }
    }
}

@Composable
private fun OtherTimer(onStop: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "The interval timer is running for another exercise.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onStop) { Text("Stop it") }
        }
    }
}
