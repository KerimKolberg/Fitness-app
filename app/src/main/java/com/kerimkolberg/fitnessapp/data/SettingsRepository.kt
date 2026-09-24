package com.kerimkolberg.fitnessapp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kerimkolberg.fitnessapp.model.Settings
import com.kerimkolberg.fitnessapp.model.ThemeMode
import com.kerimkolberg.fitnessapp.model.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        val defaults = Settings()
        Settings(
            unitSystem = prefs[UNIT_SYSTEM].toEnumOr(defaults.unitSystem),
            restTimerSeconds = prefs[REST_TIMER_SECONDS] ?: defaults.restTimerSeconds,
            autoStartRestTimer = prefs[AUTO_START_REST_TIMER] ?: defaults.autoStartRestTimer,
            themeMode = prefs[THEME_MODE].toEnumOr(defaults.themeMode),
            weeklyGoal = prefs[WEEKLY_GOAL] ?: defaults.weeklyGoal,
        )
    }

    suspend fun setUnitSystem(value: UnitSystem) = dataStore.edit { it[UNIT_SYSTEM] = value.name }

    suspend fun setRestTimerSeconds(value: Int) =
        dataStore.edit { it[REST_TIMER_SECONDS] = value.coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS) }

    suspend fun setAutoStartRestTimer(value: Boolean) = dataStore.edit { it[AUTO_START_REST_TIMER] = value }

    suspend fun setThemeMode(value: ThemeMode) = dataStore.edit { it[THEME_MODE] = value.name }

    suspend fun setWeeklyGoal(value: Int) = dataStore.edit { it[WEEKLY_GOAL] = value.coerceIn(1, 7) }

    companion object {
        const val MIN_REST_SECONDS = 15
        const val MAX_REST_SECONDS = 15 * 60

        private val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        private val REST_TIMER_SECONDS = intPreferencesKey("rest_timer_seconds")
        private val AUTO_START_REST_TIMER = booleanPreferencesKey("auto_start_rest_timer")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
