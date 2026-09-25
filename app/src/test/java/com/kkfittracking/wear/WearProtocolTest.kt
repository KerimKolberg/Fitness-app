package com.kkfittracking.wear

import com.kkfittracking.model.ExerciseType
import com.kkfittracking.model.trackKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WearProtocolTest {
    @Test
    fun stateGoesToTheWatchAndBack() {
        val state = WatchState(
            active = true, epochDay = 20_000, exerciseId = "bench", exerciseName = "Bench Press", step = "Set 2 of 3",
            position = 1, of = 4, fields = WatchFields(weight = true, reps = true), weight = 82.5, reps = 8,
            percent = 25, restEndsAtMillis = 1_000, restLabel = "Next: Bench Press · Set 2 of 3", sentAtMillis = 5,
        )
        assertEquals(state, WearJson.decodeState(WearJson.encode(state)))
        assertNull(WearJson.decodeState("not json".encodeToByteArray()))
    }

    @Test
    fun commandsGoToThePhoneAndBack() {
        val commands = listOf(
            WatchCommand.Sync, WatchCommand.Start, WatchCommand.Pause, WatchCommand.Resume, WatchCommand.Stop,
            WatchCommand.Skip("curl"),
            WatchCommand.Log(epochDay = 20_000, exerciseId = "bench", weight = 80.0, reps = 8, isDrop = true),
        )
        commands.forEach { assertEquals(it, WearJson.decodeCommand(WearJson.encode(it))) }
    }

    @Test
    fun aNewerAppsExtraFieldsAreIgnored() {
        val json = """{"active":true,"exerciseName":"Squat","somethingNew":42}"""
        assertEquals(WatchState(active = true, exerciseName = "Squat"), WearJson.decodeState(json.encodeToByteArray()))
    }

    @Test
    fun cardioAndSessionsCanBeTrackedByTheWatch() {
        assertEquals(TrackKind.RUNNING, trackKind("Running", ExerciseType.DISTANCE_TIME))
        assertEquals(TrackKind.ROWING, trackKind("Rowing Machine", ExerciseType.DISTANCE_TIME))
        assertEquals(TrackKind.HIIT, trackKind("Tabata", ExerciseType.INTERVALS))
        assertEquals(TrackKind.SPORT, trackKind("Tennis", ExerciseType.SESSION))
        assertEquals(TrackKind.WORKOUT, trackKind("Salsa", ExerciseType.SESSION))
        // Weights are entered by hand, even with "row" in the name.
        assertNull(trackKind("Barbell Row", ExerciseType.WEIGHT_REPS))
    }
}
