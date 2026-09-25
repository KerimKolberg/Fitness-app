package com.kerimkolberg.fitnessapp

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.kerimkolberg.fitnessapp.data.BodyRepository
import com.kerimkolberg.fitnessapp.data.ExerciseRepository
import com.kerimkolberg.fitnessapp.data.GameRepository
import com.kerimkolberg.fitnessapp.data.RoutineRepository
import com.kerimkolberg.fitnessapp.data.SettingsRepository
import com.kerimkolberg.fitnessapp.data.WorkoutRepository
import com.kerimkolberg.fitnessapp.data.backup.BackupRepository
import com.kerimkolberg.fitnessapp.data.backup.DataTransfer
import com.kerimkolberg.fitnessapp.data.db.AppDatabase
import com.kerimkolberg.fitnessapp.timer.RestTimer
import com.kerimkolberg.fitnessapp.timer.RestTimerAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Creates and holds the app-wide objects. Screens get what they need from here through their ViewModels. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** For work that must outlive a single screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: AppDatabase = AppDatabase.build(appContext)

    val exerciseRepository = ExerciseRepository(database.exerciseDao())

    val workoutRepository = WorkoutRepository(database)

    val routineRepository = RoutineRepository(database)

    val bodyRepository = BodyRepository(database.bodyDao())

    val settingsRepository = SettingsRepository(appContext.settingsDataStore)

    val gameRepository = GameRepository(database.workoutDao(), settingsRepository)

    val dataTransfer = DataTransfer(
        context = appContext,
        backups = BackupRepository(database, settingsRepository, BuildConfig.VERSION_NAME),
        workoutDao = database.workoutDao(),
        bodyRepository = bodyRepository,
        settingsRepository = settingsRepository,
    )

    val restTimer = RestTimer(appScope, onFinished = RestTimerAlarm(appContext)::fire)
}
