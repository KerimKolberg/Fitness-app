package com.kkfittracking.watch

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kkfittracking.wear.WatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The workout as the watch knows it, for the ongoing activity while the app is in the background. */
object WatchSession {
    private val _state = MutableStateFlow<WatchState?>(null)
    val state: StateFlow<WatchState?> = _state.asStateFlow()

    /** Keeps the latest state; a workout that starts brings up the ongoing activity. */
    fun update(context: Context, new: WatchState) {
        val wasActive = _state.value?.active == true
        _state.value = new
        if (new.active && !wasActive) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, WatchWorkoutService::class.java)) }
        }
    }
}
