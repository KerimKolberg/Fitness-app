package com.kkfittracking.watch

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.concurrent.futures.await
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import com.kkfittracking.wear.TrackKind
import kotlin.math.roundToInt

/** An activity the watch is recording, with what its sensors measured so far. */
data class TrackedActivity(
    val kind: TrackKind,
    val startedAtMillis: Long,
    val distanceMeters: Double? = null,
    val steps: Long? = null,
    val heartRate: Int? = null,
    val averageHeartRate: Int? = null,
    val calories: Double? = null,
) {
    fun seconds(now: Long): Int = ((now - startedAtMillis) / 1000).toInt().coerceAtLeast(0)

    /** "8,432 steps · ♥ avg 141 bpm · 320 kcal", for the set's note. */
    val summary: String
        get() = listOfNotNull(
            steps?.let { "%,d steps".format(it) },
            averageHeartRate?.let { "♥ avg $it bpm" },
            calories?.takeIf { it > 0 }?.let { "${it.roundToInt()} kcal" },
        ).joinToString(" · ")
}

/**
 * The watch's sensors through Wear OS Health Services: the heart rate while a workout runs, and
 * recording cardio and sessions (time, GPS distance, steps, heart rate, calories). Health Services
 * keeps recording when the app is in the background.
 */
class HealthTracker(private val context: Context) {
    private val client = HealthServices.getClient(context)

    /** The latest heart rate, while measuring. */
    var heartRate by mutableStateOf<Int?>(null)
        private set

    /** The activity being recorded, or null. */
    var tracking by mutableStateOf<TrackedActivity?>(null)
        private set

    private var measuring = false

    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) = Unit

        override fun onDataReceived(data: DataPointContainer) {
            data.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value?.takeIf { it > 0 }?.let { heartRate = it.roundToInt() }
        }
    }

    private val exerciseCallback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val current = tracking ?: return
            val metrics = update.latestMetrics
            val bpm = metrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value?.roundToInt()
            tracking = current.copy(
                distanceMeters = metrics.getData(DataType.DISTANCE_TOTAL)?.total ?: current.distanceMeters,
                steps = metrics.getData(DataType.STEPS_TOTAL)?.total ?: current.steps,
                heartRate = bpm ?: current.heartRate,
                averageHeartRate = metrics.getData(DataType.HEART_RATE_BPM_STATS)?.average?.roundToInt() ?: current.averageHeartRate,
                calories = metrics.getData(DataType.CALORIES_TOTAL)?.total ?: current.calories,
            )
            bpm?.let { heartRate = it }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onRegistered() = Unit

        override fun onRegistrationFailed(throwable: Throwable) = Unit

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) = Unit
    }

    /** Measures the heart rate (needs the heart rate permission). */
    fun measureHeartRate(on: Boolean) {
        if (on == measuring) return
        measuring = on
        runCatching {
            if (on) {
                client.measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, measureCallback)
            } else {
                client.measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, measureCallback)
                heartRate = null
            }
        }.onFailure { measuring = false }
    }

    /** Picks up an activity this app was recording before it was closed. */
    suspend fun reattach() {
        if (tracking != null) return
        runCatching {
            val info = client.exerciseClient.getCurrentExerciseInfoAsync().await()
            if (info.exerciseTrackedStatus == ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS) {
                val saved = WatchStore.loadTracking(context)
                tracking = TrackedActivity(saved?.first ?: TrackKind.WORKOUT, saved?.second ?: System.currentTimeMillis())
                client.exerciseClient.setUpdateCallback(exerciseCallback)
            }
        }
    }

    /** Starts recording; returns why it could not, or null. */
    suspend fun start(kind: TrackKind, gpsAllowed: Boolean): String? = try {
        val capabilities = client.exerciseClient.getCapabilitiesAsync().await()
        val type = kind.exerciseType().takeIf { it in capabilities.supportedExerciseTypes } ?: ExerciseType.WORKOUT
        if (type !in capabilities.supportedExerciseTypes) {
            "This watch cannot record workouts"
        } else {
            val supported = capabilities.getExerciseTypeCapabilities(type).supportedDataTypes
            val wanted = setOf<DataType<*, *>>(
                DataType.HEART_RATE_BPM, DataType.HEART_RATE_BPM_STATS, DataType.DISTANCE_TOTAL,
                DataType.STEPS_TOTAL, DataType.CALORIES_TOTAL,
            ).filter { it in supported }.toSet()
            val config = ExerciseConfig(
                exerciseType = type,
                dataTypes = wanted,
                isAutoPauseAndResumeEnabled = false,
                isGpsEnabled = gpsAllowed && kind in OUTDOOR,
            )
            val now = System.currentTimeMillis()
            tracking = TrackedActivity(kind, now)
            WatchStore.saveTracking(context, kind, now)
            client.exerciseClient.setUpdateCallback(exerciseCallback)
            client.exerciseClient.startExerciseAsync(config).await()
            null
        }
    } catch (e: Exception) {
        tracking = null
        WatchStore.clearTracking(context)
        e.message ?: "Could not start recording"
    }

    /** Stops recording and returns what was measured. */
    suspend fun finish(): TrackedActivity? {
        val done = tracking ?: return null
        runCatching { client.exerciseClient.endExerciseAsync().await() }
        runCatching { client.exerciseClient.clearUpdateCallbackAsync(exerciseCallback) }
        tracking = null
        WatchStore.clearTracking(context)
        return done
    }

    private companion object {
        /** Outside, where GPS gives the distance. */
        val OUTDOOR = setOf(TrackKind.RUNNING, TrackKind.WALKING, TrackKind.HIKING, TrackKind.BIKING)

        fun TrackKind.exerciseType(): ExerciseType = when (this) {
            TrackKind.RUNNING -> ExerciseType.RUNNING
            TrackKind.WALKING -> ExerciseType.WALKING
            TrackKind.HIKING -> ExerciseType.HIKING
            TrackKind.BIKING -> ExerciseType.BIKING
            TrackKind.ROWING -> ExerciseType.ROWING_MACHINE
            TrackKind.SWIMMING -> ExerciseType.SWIMMING_POOL
            TrackKind.ELLIPTICAL -> ExerciseType.ELLIPTICAL
            TrackKind.STAIRS -> ExerciseType.STAIR_CLIMBING
            TrackKind.HIIT -> ExerciseType.HIGH_INTENSITY_INTERVAL_TRAINING
            TrackKind.SPORT, TrackKind.WORKOUT -> ExerciseType.WORKOUT
        }
    }
}
