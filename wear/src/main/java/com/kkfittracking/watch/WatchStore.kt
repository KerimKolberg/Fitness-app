package com.kkfittracking.watch

import android.content.Context
import androidx.wear.tiles.TileService
import com.kkfittracking.wear.TrackKind
import com.kkfittracking.wear.WatchState
import com.kkfittracking.wear.WearJson

/**
 * What the watch remembers between launches: the last workout state from the phone (for the tile)
 * and the activity being recorded (to pick it up again after the app was closed).
 */
object WatchStore {
    private const val FILE = "watch"
    private const val KEY_STATE = "state"
    private const val KEY_TRACK_KIND = "trackKind"
    private const val KEY_TRACK_START = "trackStart"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Keeps the phone's latest state and refreshes the tile. */
    fun saveState(context: Context, bytes: ByteArray) {
        prefs(context).edit().putString(KEY_STATE, bytes.decodeToString()).apply()
        runCatching { TileService.getUpdater(context).requestUpdate(WorkoutTileService::class.java) }
    }

    fun loadState(context: Context): WatchState? =
        prefs(context).getString(KEY_STATE, null)?.let { WearJson.decodeState(it.encodeToByteArray()) }

    fun saveTracking(context: Context, kind: TrackKind, startedAtMillis: Long) {
        prefs(context).edit().putString(KEY_TRACK_KIND, kind.name).putLong(KEY_TRACK_START, startedAtMillis).apply()
    }

    fun loadTracking(context: Context): Pair<TrackKind, Long>? {
        val prefs = prefs(context)
        val kind = prefs.getString(KEY_TRACK_KIND, null)?.let { name -> TrackKind.entries.firstOrNull { it.name == name } } ?: return null
        return kind to prefs.getLong(KEY_TRACK_START, System.currentTimeMillis())
    }

    fun clearTracking(context: Context) {
        prefs(context).edit().remove(KEY_TRACK_KIND).remove(KEY_TRACK_START).apply()
    }
}
