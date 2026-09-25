package com.kkfittracking.timer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntervalTimerTest {
    private val events = mutableListOf<IntervalEvent>()

    private fun TestScope.newTimer() = IntervalTimer(
        scope = backgroundScope,
        onEvent = { events += it },
        elapsedMillis = { testScheduler.currentTime },
    )

    private fun TestScope.advanceSeconds(seconds: Int) {
        advanceTimeBy(seconds * 1000L)
        runCurrent()
    }

    @Test
    fun getReadyThenHighAndLowUntilTheLastRound() = runTest {
        val timer = newTimer()
        timer.start(highSeconds = 3, lowSeconds = 2, rounds = 2, exerciseId = "tabata", getReadySeconds = 2)
        runCurrent()
        assertEquals(IntervalPhase.GET_READY, timer.state.value.phase)
        assertEquals(2, timer.state.value.remainingSeconds)

        advanceSeconds(2)
        assertEquals(IntervalPhase.HIGH, timer.state.value.phase)
        assertEquals(1, timer.state.value.round)
        assertEquals(3, timer.state.value.remainingSeconds)

        advanceSeconds(3)
        assertEquals(IntervalPhase.LOW, timer.state.value.phase)
        assertEquals(1, timer.state.value.completedRounds)
        assertEquals(3, timer.state.value.workoutSeconds)

        advanceSeconds(2)
        assertEquals(IntervalPhase.HIGH, timer.state.value.phase)
        assertEquals(2, timer.state.value.round)

        // The last round ends after its high part: no low part at the end.
        advanceSeconds(3)
        val done = timer.state.value
        assertEquals(IntervalPhase.DONE, done.phase)
        assertEquals(2, done.completedRounds)
        assertEquals(8, done.workoutSeconds)
        assertEquals("tabata", done.exerciseId)

        assertEquals(
            listOf(
                IntervalEvent.PhaseStarted(IntervalPhase.GET_READY, 1, 2),
                IntervalEvent.Countdown(1),
                IntervalEvent.PhaseStarted(IntervalPhase.HIGH, 1, 2),
                IntervalEvent.Countdown(2),
                IntervalEvent.Countdown(1),
                IntervalEvent.PhaseStarted(IntervalPhase.LOW, 1, 2),
                IntervalEvent.Countdown(1),
                IntervalEvent.PhaseStarted(IntervalPhase.HIGH, 2, 2),
                IntervalEvent.Countdown(2),
                IntervalEvent.Countdown(1),
                IntervalEvent.Finished(2),
            ),
            events,
        )
        advanceSeconds(10)
        assertEquals(1, events.count { it is IntervalEvent.Finished })
    }

    @Test
    fun pausingStopsTheClock() = runTest {
        val timer = newTimer()
        timer.start(highSeconds = 10, lowSeconds = 5, rounds = 3, getReadySeconds = 0)
        advanceSeconds(4)
        assertEquals(6, timer.state.value.remainingSeconds)

        timer.pause()
        advanceSeconds(20)
        assertTrue(timer.state.value.isPaused)
        assertEquals(IntervalPhase.HIGH, timer.state.value.phase)
        assertEquals(6, timer.state.value.remainingSeconds)

        timer.resume()
        advanceSeconds(5)
        assertEquals(1, timer.state.value.remainingSeconds)
        advanceSeconds(1)
        assertEquals(IntervalPhase.LOW, timer.state.value.phase)
    }

    @Test
    fun finishingEarlyKeepsWhatWasDone() = runTest {
        val timer = newTimer()
        timer.start(highSeconds = 10, lowSeconds = 5, rounds = 3, getReadySeconds = 0)
        advanceSeconds(12)
        assertEquals(IntervalPhase.LOW, timer.state.value.phase)

        timer.finish()
        val done = timer.state.value
        assertEquals(IntervalPhase.DONE, done.phase)
        assertEquals(1, done.completedRounds)
        assertEquals(12, done.workoutSeconds)
        // Stopping by hand does not ring the "done" alarm.
        assertTrue(events.none { it is IntervalEvent.Finished })

        timer.reset()
        assertEquals(IntervalTimerState(), timer.state.value)
    }

    @Test
    fun finishingDuringGetReadyResets() = runTest {
        val timer = newTimer()
        timer.start(highSeconds = 20, lowSeconds = 10, rounds = 8)
        advanceSeconds(2)
        timer.finish()
        assertEquals(IntervalPhase.IDLE, timer.state.value.phase)
        assertFalse(timer.state.value.isActive)
    }

    @Test
    fun withoutLowIntensityTheRoundsFollowEachOther() = runTest {
        val timer = newTimer()
        timer.start(highSeconds = 2, lowSeconds = 0, rounds = 3, getReadySeconds = 0)
        val firstRun = timer.state.value.runId
        advanceSeconds(2)
        assertEquals(IntervalPhase.HIGH, timer.state.value.phase)
        assertEquals(2, timer.state.value.round)
        advanceSeconds(4)
        assertEquals(IntervalPhase.DONE, timer.state.value.phase)
        assertEquals(6, timer.state.value.workoutSeconds)

        timer.start(highSeconds = 2, lowSeconds = 0, rounds = 1)
        assertTrue(timer.state.value.runId > firstRun)
    }
}
