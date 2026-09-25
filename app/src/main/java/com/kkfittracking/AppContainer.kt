package com.kkfittracking

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.kkfittracking.data.BodyRepository
import com.kkfittracking.data.ExerciseRepository
import com.kkfittracking.data.GameRepository
import com.kkfittracking.data.RoutineRepository
import com.kkfittracking.data.SettingsRepository
import com.kkfittracking.data.WorkoutRepository
import com.kkfittracking.data.backup.BackupRepository
import com.kkfittracking.data.backup.DataTransfer
import com.kkfittracking.data.db.AppDatabase
import com.kkfittracking.data.health.HealthConnect
import com.kkfittracking.guide.GuidedWorkout
import com.kkfittracking.guide.WatchBridge
import com.kkfittracking.guide.WorkoutGuideService
import com.kkfittracking.timer.Alerts
import com.kkfittracking.timer.IntervalAlarm
import com.kkfittracking.timer.IntervalTimer
import com.kkfittracking.timer.RestTimer
import com.kkfittracking.timer.RestTimerAlarm
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

    /** Daily steps and distance from Health Connect. */
    val healthConnect = HealthConnect(appContext)

    val dataTransfer = DataTransfer(
        context = appContext,
        backups = BackupRepository(database, settingsRepository, BuildConfig.VERSION_NAME),
        workoutDao = database.workoutDao(),
        bodyRepository = bodyRepository,
        settingsRepository = settingsRepository,
        exerciseRepository = exerciseRepository,
    )

    private val alerts = Alerts(appContext)

    val restTimer = RestTimer(appScope, onFinished = RestTimerAlarm(alerts)::fire)

    val intervalTimer = IntervalTimer(appScope, onEvent = IntervalAlarm(alerts)::onEvent)

    /** The play button's guided workout, with its ongoing notification. */
    val guidedWorkout = GuidedWorkout(
        scope = appScope,
        workoutRepository = workoutRepository,
        settingsRepository = settingsRepository,
        // Started from the watch while the phone app is in the background, Android may refuse the
        // notification; the workout still runs.
        onStarted = { runCatching { WorkoutGuideService.start(appContext) } },
    )

    /** The watch app's link to the guided workout. */
    val watchBridge = WatchBridge(
        context = appContext,
        scope = appScope,
        guide = guidedWorkout,
        workouts = workoutRepository,
        settingsRepository = settingsRepository,
        restTimer = restTimer,
    )
}
