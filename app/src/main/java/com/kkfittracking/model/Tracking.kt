package com.kkfittracking.model

import com.kkfittracking.wear.TrackKind

/**
 * How the watch can record an exercise with its sensors: cardio with distance, sessions and HIIT.
 * The kind picks what the watch measures (GPS distance for a run, steps for a walk…). Null for
 * sets of weights and reps, which are entered by hand.
 */
fun trackKind(name: String, type: ExerciseType): TrackKind? {
    if (!type.usesDistance && !type.isSession) return null
    val words = name.lowercase()
    fun has(vararg parts: String) = parts.any { it in words }
    return when {
        type == ExerciseType.INTERVALS || has("hiit", "tabata", "interval") -> TrackKind.HIIT
        has("run", "jog", "sprint") -> TrackKind.RUNNING
        has("hik") -> TrackKind.HIKING
        has("walk") -> TrackKind.WALKING
        has("cycl", "bike", "spin") -> TrackKind.BIKING
        has("rowing") -> TrackKind.ROWING
        has("swim") -> TrackKind.SWIMMING
        has("ellip") -> TrackKind.ELLIPTICAL
        has("stair") -> TrackKind.STAIRS
        type == ExerciseType.SESSION && has("tennis", "ball", "padel", "squash", "badminton", "football", "soccer", "martial", "climb") ->
            TrackKind.SPORT
        else -> TrackKind.WORKOUT
    }
}
