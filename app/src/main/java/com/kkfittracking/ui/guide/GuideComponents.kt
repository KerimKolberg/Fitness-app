package com.kkfittracking.ui.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kkfittracking.guide.GuideState
import com.kkfittracking.model.DayCompletion
import com.kkfittracking.model.ExerciseCompletion
import com.kkfittracking.model.GuideSummary
import com.kkfittracking.model.formatDuration
import kotlinx.coroutines.delay

/** The current time, updated every second while shown. */
@Composable
private fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/**
 * The running guided workout: where to go now, how much is done and the training time, with
 * Pause, Skip and Stop. [currentExerciseId] is the exercise on screen, if any.
 */
@Composable
fun GuideBar(
    state: GuideState,
    currentExerciseId: String?,
    onGo: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = state.session ?: return
    val target = state.target
    var confirmStop by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (session.isPaused) "⏸ Paused" else "▶ Guided workout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${state.completion.percent}% · ${formatDuration((session.activeMillis(rememberNow()) / 1000).toInt())}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            LinearProgressIndicator(progress = { state.completion.percent / 100f }, modifier = Modifier.fillMaxWidth())
            Text(
                text = when {
                    target == null -> "Everything planned is done. Log more, or stop to see the summary."
                    target.exerciseId == currentExerciseId -> "Now: ${target.step} of this exercise"
                    else -> "Next: ${target.name} · ${target.step}"
                },
                style = MaterialTheme.typography.titleSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (target != null && target.exerciseId != currentExerciseId) {
                    Button(onClick = { onGo(target.exerciseId) }) { Text("Go") }
                }
                if (session.isPaused) {
                    Button(onClick = onResume) { Text("Resume") }
                } else {
                    OutlinedButton(onClick = onPause) { Text("Pause") }
                }
                if (target != null && !target.isDrop) OutlinedButton(onClick = onSkip) { Text("Skip") }
                TextButton(onClick = { confirmStop = true }) { Text("Stop") }
            }
        }
    }
    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("End the workout?") },
            text = { Text("Everything you logged stays. You'll see what was done and what's left.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmStop = false
                        onStop()
                    },
                ) { Text("End workout") }
            },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Keep going") } },
        )
    }
}

/** How much of the day's plan is done, and what was done and what was not. Tap to see the lists. */
@Composable
fun DayCompletionCard(completion: DayCompletion, modifier: Modifier = Modifier) {
    if (completion.exercises.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(modifier = modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${completion.percent}% of the plan done",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${completion.finished.size}/${completion.exercises.size} exercises" + if (expanded) " ▲" else " ▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(progress = { completion.percent / 100f }, modifier = Modifier.fillMaxWidth())
            if (expanded) CompletionLists(completion)
        }
    }
}

@Composable
private fun CompletionLists(completion: DayCompletion) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CompletionGroup("Done ✓", completion.finished)
        CompletionGroup("Partly done", completion.partly)
        CompletionGroup("Not done", completion.notStarted)
        if (completion.exercises.any { it.setsGuessed }) {
            Text(
                text = "Exercises without a set plan count as 3 sets.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompletionGroup(title: String, exercises: List<ExerciseCompletion>) {
    if (exercises.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
    exercises.forEach {
        Row {
            Text(it.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(it.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Shown when a guided workout ends: the training time and what was done and what was not. */
@Composable
fun GuideSummaryDialog(summary: GuideSummary, onDismiss: () -> Unit) {
    val completion = summary.completion
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (summary.stoppedEarly) "Workout ended" else "Workout complete 🏁") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${completion.percent}% of the plan done in ${formatDuration((summary.activeMillis / 1000).toInt())}" +
                        " of training (pauses not counted).",
                )
                LinearProgressIndicator(progress = { completion.percent / 100f }, modifier = Modifier.fillMaxWidth())
                CompletionLists(completion)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}
