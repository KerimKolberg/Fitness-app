package com.kkfittracking.ui.log

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kkfittracking.model.formatDuration
import kotlinx.coroutines.delay

/**
 * A timer for holds and timed sets: counts down [targetSeconds] (or up, without a target), then
 * beeps and buzzes until Done while it keeps counting, shown as −0:10, so a longer hold is timed
 * too. Done hands the whole time held to [onDone]. The screen stays on while it runs.
 */
@Composable
fun HoldTimerCard(targetSeconds: Int, onDone: (heldSeconds: Int) -> Unit) {
    var startedAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var target by rememberSaveable { mutableStateOf(0) }
    val started = startedAt
    if (started == null) {
        OutlinedButton(
            onClick = {
                target = targetSeconds
                startedAt = System.currentTimeMillis()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (targetSeconds > 0) "⏱ Start ${formatDuration(targetSeconds)} timer" else "⏱ Start timer")
        }
        return
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(started) {
        while (true) {
            now = System.currentTimeMillis()
            delay(200)
        }
    }
    val elapsed = ((now - started) / 1000).toInt()
    val left = target - elapsed
    val over = target > 0 && left <= 0
    val context = LocalContext.current
    // Time is up: a beep and a buzz every 1.5 s until Done.
    LaunchedEffect(over) {
        if (!over) return@LaunchedEffect
        val tones = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME) }.getOrNull()
        try {
            while (true) {
                tones?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                vibrate(context, 500)
                delay(1_500)
            }
        } finally {
            tones?.release()
        }
    }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (over) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when {
                    target <= 0 -> formatDuration(elapsed)
                    over -> "−" + formatDuration(-left)
                    else -> formatDuration(left)
                },
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            if (over) Text("Held ${formatDuration(elapsed)} · keep going or tap Done")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    onClick = {
                        startedAt = null
                        onDone(elapsed)
                    },
                ) { Text("✓ Done") }
                TextButton(onClick = { startedAt = null }) { Text("Cancel") }
            }
        }
    }
}

private fun vibrate(context: android.content.Context, millis: Long) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }
    vibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
}
