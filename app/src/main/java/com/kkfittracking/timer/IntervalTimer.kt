package com.kkfittracking.timer

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

enum class IntervalPhase(val label: String) {
    IDLE("Interval timer"),
    GET_READY("Get ready"),
    HIGH("High intensity"),
    LOW("Low intensity"),
    DONE("Done"),
}

data class IntervalTimerState(
    val phase: IntervalPhase = IntervalPhase.IDLE,
    /** The round being done, from 1. */
    val round: Int = 0,
    val rounds: Int = 0,
    val highSeconds: Int = 0,
    val lowSeconds: Int = 0,
    /** Seconds left in the current phase. */
    val remainingSeconds: Int = 0,
    val isPaused: Boolean = false,
    /** Rounds whose high-intensity part was finished. */
    val completedRounds: Int = 0,
    /** Seconds of high and low intensity done so far; the get-ready countdown does not count. */
    val workoutSeconds: Int = 0,
    /** The exercise the timer was started for. */
    val exerciseId: String? = null,
    /** New for every start, so a finished workout is only filled in once. */
    val runId: Long = 0,
) {
    val isActive: Boolean
        get() = phase == IntervalPhase.GET_READY || phase == IntervalPhase.HIGH || phase == IntervalPhase.LOW
}

/** What the alerts react to. */
sealed interface IntervalEvent {
    data class PhaseStarted(val phase: IntervalPhase, val round: Int, val rounds: Int) : IntervalEvent

    /** The last seconds of a phase: 3, 2, 1. */
    data class Countdown(val secondsLeft: Int) : IntervalEvent

    data class Finished(val rounds: Int) : IntervalEvent
}

/**
 * Times HIIT: a short get-ready countdown, then rounds of high intensity followed by low
 * intensity. The last round ends after its high part. Lives as long as the app process, like the
 * rest timer, so it keeps going while the user moves between screens. [scope] should run on the
 * main thread.
 */
class IntervalTimer(
    private val scope: CoroutineScope,
    private val onEvent: (IntervalEvent) -> Unit,
    private val elapsedMillis: () -> Long = SystemClock::elapsedRealtime,
) {
    private val _state = MutableStateFlow(IntervalTimerState())
    val state: StateFlow<IntervalTimerState> = _state.asStateFlow()

    private var job: Job? = null
    private var phaseEndAtMillis = 0L
    private var pausedRemainingMillis = 0L

    /** Workout seconds of the phases already finished. */
    private var finishedPhaseSeconds = 0
    private var lastRunId = 0L

    fun start(
        highSeconds: Int,
        lowSeconds: Int,
        rounds: Int,
        exerciseId: String? = null,
        getReadySeconds: Int = GET_READY_SECONDS,
    ) {
        job?.cancel()
        finishedPhaseSeconds = 0
        _state.value = IntervalTimerState(
            rounds = rounds.coerceAtLeast(1),
            highSeconds = highSeconds.coerceAtLeast(1),
            lowSeconds = lowSeconds.coerceAtLeast(0),
            exerciseId = exerciseId,
            runId = ++lastRunId,
        )
        phaseEndAtMillis = elapsedMillis()
        if (getReadySeconds > 0) {
            enterPhase(IntervalPhase.GET_READY, round = 1, seconds = getReadySeconds)
        } else {
            enterPhase(IntervalPhase.HIGH, round = 1, seconds = _state.value.highSeconds)
        }
        job = scope.launch { run() }
    }

    fun pause() {
        val current = _state.value
        if (!current.isActive || current.isPaused) return
        job?.cancel()
        job = null
        pausedRemainingMillis = (phaseEndAtMillis - elapsedMillis()).coerceAtLeast(0)
        _state.update { it.copy(isPaused = true) }
    }

    fun resume() {
        val current = _state.value
        if (!current.isActive || !current.isPaused) return
        phaseEndAtMillis = elapsedMillis() + pausedRemainingMillis
        _state.update { it.copy(isPaused = false) }
        job = scope.launch { run() }
    }

    /** Ends the workout early. What was done so far can still be logged; if nothing was, the timer resets. */
    fun finish() {
        val current = _state.value
        if (!current.isActive) return
        job?.cancel()
        job = null
        val done = workoutSecondsNow(current)
        _state.value = if (done > 0) {
            current.copy(phase = IntervalPhase.DONE, remainingSeconds = 0, isPaused = false, workoutSeconds = done)
        } else {
            IntervalTimerState()
        }
    }

    /** Back to the start, forgetting a finished workout. */
    fun reset() {
        job?.cancel()
        job = null
        _state.value = IntervalTimerState()
    }

    private fun enterPhase(phase: IntervalPhase, round: Int, seconds: Int) {
        // Chain from the previous phase's end, so a late wake-up does not make the workout drift.
        phaseEndAtMillis += seconds * 1000L
        _state.update {
            it.copy(phase = phase, round = round, remainingSeconds = seconds, workoutSeconds = finishedPhaseSeconds)
        }
        onEvent(IntervalEvent.PhaseStarted(phase, round, _state.value.rounds))
    }

    private suspend fun run() {
        while (true) {
            val current = _state.value
            if (!current.isActive || current.isPaused) return
            val remainingMillis = phaseEndAtMillis - elapsedMillis()
            if (remainingMillis <= 0) {
                advance(current)
                continue
            }
            val remaining = ceil(remainingMillis / 1000.0).toInt()
            if (remaining != current.remainingSeconds) {
                _state.update { it.copy(remainingSeconds = remaining, workoutSeconds = workoutSecondsNow(it)) }
                if (remaining <= COUNTDOWN_SECONDS) onEvent(IntervalEvent.Countdown(remaining))
            }
            // Wake up right when the displayed second changes.
            val untilNextSecond = remainingMillis % 1000L
            delay(if (untilNextSecond > 0) untilNextSecond else 1000L)
        }
    }

    private fun advance(current: IntervalTimerState) {
        when (current.phase) {
            IntervalPhase.GET_READY -> enterPhase(IntervalPhase.HIGH, current.round, current.highSeconds)
            IntervalPhase.HIGH -> {
                finishedPhaseSeconds += current.highSeconds
                val completed = current.completedRounds + 1
                _state.update { it.copy(completedRounds = completed) }
                when {
                    completed >= current.rounds -> complete()
                    current.lowSeconds > 0 -> enterPhase(IntervalPhase.LOW, current.round, current.lowSeconds)
                    else -> enterPhase(IntervalPhase.HIGH, current.round + 1, current.highSeconds)
                }
            }
            IntervalPhase.LOW -> {
                finishedPhaseSeconds += current.lowSeconds
                enterPhase(IntervalPhase.HIGH, current.round + 1, current.highSeconds)
            }
            IntervalPhase.IDLE, IntervalPhase.DONE -> Unit
        }
    }

    private fun complete() {
        _state.update {
            it.copy(phase = IntervalPhase.DONE, remainingSeconds = 0, workoutSeconds = finishedPhaseSeconds)
        }
        job = null
        onEvent(IntervalEvent.Finished(_state.value.completedRounds))
    }

    /** Finished phases plus the part of the current high or low phase already done. */
    private fun workoutSecondsNow(state: IntervalTimerState): Int {
        val phaseLength = when (state.phase) {
            IntervalPhase.HIGH -> state.highSeconds
            IntervalPhase.LOW -> state.lowSeconds
            else -> return finishedPhaseSeconds
        }
        val remaining = if (state.isPaused) {
            ceil(pausedRemainingMillis / 1000.0).toInt()
        } else {
            ceil((phaseEndAtMillis - elapsedMillis()).coerceAtLeast(0) / 1000.0).toInt()
        }
        return finishedPhaseSeconds + (phaseLength - remaining).coerceIn(0, phaseLength)
    }

    companion object {
        const val GET_READY_SECONDS = 5
        const val COUNTDOWN_SECONDS = 3
    }
}
