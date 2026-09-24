package com.kerimkolberg.fitnessapp.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kerimkolberg.fitnessapp.data.SettingsRepository
import com.kerimkolberg.fitnessapp.model.formatDuration
import com.kerimkolberg.fitnessapp.timer.RestTimerState

private const val STEP_SECONDS = 15

@Composable
fun RestTimerDialog(
    timer: RestTimerState,
    defaultSeconds: Int,
    onStart: (Int) -> Unit,
    onStop: () -> Unit,
    onAddSeconds: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // The duration to start with when the timer is not running.
    var seconds by remember { mutableIntStateOf(defaultSeconds) }

    fun adjust(delta: Int) {
        if (timer.isRunning) {
            onAddSeconds(delta)
        } else {
            seconds = (seconds + delta).coerceIn(SettingsRepository.MIN_REST_SECONDS, SettingsRepository.MAX_REST_SECONDS)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rest timer") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = formatDuration(if (timer.isRunning) timer.remainingSeconds else seconds),
                    style = MaterialTheme.typography.displayMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { adjust(-STEP_SECONDS) }) { Text("−${STEP_SECONDS}s") }
                    OutlinedButton(onClick = { adjust(STEP_SECONDS) }) { Text("+${STEP_SECONDS}s") }
                }
            }
        },
        confirmButton = {
            if (timer.isRunning) {
                TextButton(onClick = onStop) { Text("Stop") }
            } else {
                TextButton(onClick = { onStart(seconds) }) { Text("Start") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
