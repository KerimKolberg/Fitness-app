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
class RestTimerTest {
    private var finishedCount = 0
    private var finishedLabel: String? = null

    private fun TestScope.newTimer() = RestTimer(
        scope = backgroundScope,
        onFinished = { label ->
            finishedCount++
            finishedLabel = label
        },
        elapsedMillis = { testScheduler.currentTime },
    )

    private fun TestScope.advanceSeconds(seconds: Int) {
        advanceTimeBy(seconds * 1000L)
        runCurrent()
    }

    @Test
    fun countsDownAndFinishesOnce() = runTest {
        val timer = newTimer()
        timer.start(3)
        runCurrent()
        assertTrue(timer.state.value.isRunning)
        assertEquals(3, timer.state.value.remainingSeconds)

        advanceSeconds(1)
        assertEquals(2, timer.state.value.remainingSeconds)

        advanceSeconds(2)
        assertFalse(timer.state.value.isRunning)
        assertEquals(0, timer.state.value.remainingSeconds)
        assertEquals(1, finishedCount)

        advanceSeconds(10)
        assertEquals(1, finishedCount)
    }

    @Test
    fun addingAndRemovingTime() = runTest {
        val timer = newTimer()
        timer.start(60)
        advanceSeconds(10)
        assertEquals(50, timer.state.value.remainingSeconds)

        timer.addSeconds(15)
        assertEquals(65, timer.state.value.remainingSeconds)
        assertEquals(75, timer.state.value.totalSeconds)

        timer.addSeconds(-100)
        advanceSeconds(1)
        assertFalse(timer.state.value.isRunning)
        assertEquals(1, finishedCount)
    }

    @Test
    fun stoppingDoesNotFireTheAlarm() = runTest {
        val timer = newTimer()
        timer.start(5)
        advanceSeconds(2)
        timer.stop()
        advanceSeconds(10)
        assertFalse(timer.state.value.isRunning)
        assertEquals(0, finishedCount)
    }

    @Test
    fun restartingReplacesTheRunningTimer() = runTest {
        val timer = newTimer()
        timer.start(5)
        advanceSeconds(3)
        timer.start(5)
        advanceSeconds(3)
        assertTrue(timer.state.value.isRunning)
        assertEquals(2, timer.state.value.remainingSeconds)
        advanceSeconds(2)
        assertEquals(1, finishedCount)
    }

    @Test
    fun aLabeledCountdownPassesItsLabelOn() = runTest {
        val timer = newTimer()
        timer.start(15, label = "Go to Lat Pulldown")
        runCurrent()
        assertEquals("Go to Lat Pulldown", timer.state.value.label)
        advanceSeconds(15)
        assertEquals("Go to Lat Pulldown", finishedLabel)

        timer.start(90)
        assertEquals(null, timer.state.value.label)
    }
}
