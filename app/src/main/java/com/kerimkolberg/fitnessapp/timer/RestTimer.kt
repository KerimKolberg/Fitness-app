package com.kerimkolberg.fitnessapp.timer

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class RestTimerState(
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    /** What the countdown is for, e.g. "Go to Lat Pulldown"; null for normal rest. */
    val label: String? = null,
)

/**
 * Counts down the rest between sets. Lives as long as the app process, so it keeps running while
 * the user moves between screens. [scope] should run on the main thread.
 */
class RestTimer(
    private val scope: CoroutineScope,
    /** Called with the timer's label when the countdown reaches zero. */
    private val onFinished: (label: String?) -> Unit,
    private val elapsedMillis: () -> Long = SystemClock::elapsedRealtime,
) {
    private val _state = MutableStateFlow(RestTimerState())
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    private var job: Job? = null
    private var endAtMillis = 0L

    fun start(seconds: Int, label: String? = null) {
        job?.cancel()
        endAtMillis = elapsedMillis() + seconds * 1000L
        _state.value = RestTimerState(totalSeconds = seconds, remainingSeconds = seconds, isRunning = true, label = label)
        job = scope.launch { countDown() }
    }

    /** Adds (or with a negative value, removes) time from a running timer. */
    fun addSeconds(delta: Int) {
        if (!_state.value.isRunning) return
        endAtMillis = maxOf(elapsedMillis(), endAtMillis + delta * 1000L)
        _state.update {
            it.copy(
                totalSeconds = (it.totalSeconds + delta).coerceAtLeast(0),
                remainingSeconds = remainingSeconds(),
            )
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.update { it.copy(remainingSeconds = 0, isRunning = false) }
    }

    private suspend fun countDown() {
        while (true) {
            val remaining = remainingSeconds()
            _state.update { it.copy(remainingSeconds = remaining) }
            if (remaining <= 0) break
            // Wake up right when the displayed second changes.
            val untilNextSecond = (endAtMillis - elapsedMillis()) % 1000L
            delay(if (untilNextSecond > 0) untilNextSecond else 1000L)
        }
        _state.update { it.copy(isRunning = false) }
        job = null
        onFinished(_state.value.label)
    }

    private fun remainingSeconds(): Int =
        ceil((endAtMillis - elapsedMillis()) / 1000.0).toInt().coerceAtLeast(0)
}
