package com.kerimkolberg.fitnessapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kerimkolberg.fitnessapp.ui.arrange.ArrangeDayScreen
import com.kerimkolberg.fitnessapp.ui.body.BodyMetricScreen
import com.kerimkolberg.fitnessapp.ui.body.BodyScreen
import com.kerimkolberg.fitnessapp.ui.calendar.CalendarScreen
import com.kerimkolberg.fitnessapp.ui.exercises.EditExerciseScreen
import com.kerimkolberg.fitnessapp.ui.exercises.ExercisePickerScreen
import com.kerimkolberg.fitnessapp.ui.game.AchievementsScreen
import com.kerimkolberg.fitnessapp.ui.log.ExerciseLogScreen
import com.kerimkolberg.fitnessapp.ui.routines.RoutineScreen
import com.kerimkolberg.fitnessapp.ui.routines.RoutinesScreen
import com.kerimkolberg.fitnessapp.ui.settings.SettingsScreen
import com.kerimkolberg.fitnessapp.ui.workout.WorkoutScreen
import com.kerimkolberg.fitnessapp.ui.workout.WorkoutViewModel
import kotlinx.serialization.Serializable

@Serializable
object WorkoutRoute

/**
 * Picks an exercise to log on a day, or to add to a routine when [routineId] is set, or several
 * exercises to group as a superset on the day when [superset] is true.
 */
@Serializable
data class ExercisePickerRoute(val epochDay: Long = 0, val routineId: String? = null, val superset: Boolean = false)

@Serializable
data class EditExerciseRoute(val exerciseId: String? = null)

@Serializable
data class ExerciseLogRoute(val epochDay: Long, val exerciseId: String)

@Serializable
data class CalendarRoute(val epochDay: Long)

@Serializable
object SettingsRoute

@Serializable
object RoutinesRoute

@Serializable
data class RoutineRoute(val routineId: String)

@Serializable
data class ArrangeDayRoute(val epochDay: Long)

@Serializable
object AchievementsRoute

@Serializable
object BodyRoute

@Serializable
data class BodyMetricRoute(val metric: String)

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = WorkoutRoute) {
        composable<WorkoutRoute> {
            WorkoutScreen(
                onAddExercise = { date -> navController.navigate(ExercisePickerRoute(date.toEpochDay())) },
                onOpenExercise = { date, exerciseId ->
                    navController.navigate(ExerciseLogRoute(date.toEpochDay(), exerciseId))
                },
                onOpenCalendar = { date -> navController.navigate(CalendarRoute(date.toEpochDay())) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenRoutines = { navController.navigate(RoutinesRoute) },
                onOpenBody = { navController.navigate(BodyRoute) },
                onOpenAchievements = { navController.navigate(AchievementsRoute) },
                onNewSuperset = { date -> navController.navigate(ExercisePickerRoute(date.toEpochDay(), superset = true)) },
                onArrangeDay = { date -> navController.navigate(ArrangeDayRoute(date.toEpochDay())) },
            )
        }
        composable<ExercisePickerRoute> { entry ->
            val route = entry.toRoute<ExercisePickerRoute>()
            ExercisePickerScreen(
                onBack = { navController.popBackStack() },
                onLogExercise = { exerciseId ->
                    navController.navigate(ExerciseLogRoute(route.epochDay, exerciseId)) {
                        popUpTo<ExercisePickerRoute> { inclusive = true }
                    }
                },
                onCreateExercise = { navController.navigate(EditExerciseRoute()) },
                onEditExercise = { exerciseId -> navController.navigate(EditExerciseRoute(exerciseId)) },
                onSupersetCreated = { firstExerciseId ->
                    navController.navigate(ExerciseLogRoute(route.epochDay, firstExerciseId)) {
                        popUpTo<ExercisePickerRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<EditExerciseRoute> {
            EditExerciseScreen(
                onBack = { navController.popBackStack() },
                onDeleted = {
                    // Leave the deleted exercise's log screen too, when editing started there.
                    val previous = navController.previousBackStackEntry?.destination
                    val cameFromLog = previous?.hasRoute<ExerciseLogRoute>() == true
                    if (cameFromLog) {
                        navController.popBackStack<ExerciseLogRoute>(inclusive = true)
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable<ExerciseLogRoute> { entry ->
            val route = entry.toRoute<ExerciseLogRoute>()
            ExerciseLogScreen(
                onBack = { navController.popBackStack() },
                onEditExercise = { exerciseId -> navController.navigate(EditExerciseRoute(exerciseId)) },
                onSwitchExercise = { exerciseId ->
                    // Swap to the other superset exercise, so Back still returns to the day.
                    navController.navigate(ExerciseLogRoute(route.epochDay, exerciseId)) {
                        popUpTo<ExerciseLogRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<CalendarRoute> {
            CalendarScreen(
                onBack = { navController.popBackStack() },
                onDateSelected = { date ->
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(WorkoutViewModel.KEY_EPOCH_DAY, date.toEpochDay())
                    navController.popBackStack()
                },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<RoutinesRoute> {
            RoutinesScreen(
                onBack = { navController.popBackStack() },
                onOpenRoutine = { routineId -> navController.navigate(RoutineRoute(routineId)) },
            )
        }
        composable<RoutineRoute> {
            RoutineScreen(
                onBack = { navController.popBackStack() },
                onAddExercise = { routineId -> navController.navigate(ExercisePickerRoute(routineId = routineId)) },
            )
        }
        composable<ArrangeDayRoute> {
            ArrangeDayScreen(onDone = { navController.popBackStack() })
        }
        composable<AchievementsRoute> {
            AchievementsScreen(onBack = { navController.popBackStack() })
        }
        composable<BodyRoute> {
            BodyScreen(
                onBack = { navController.popBackStack() },
                onOpenMetric = { metric -> navController.navigate(BodyMetricRoute(metric.name)) },
            )
        }
        composable<BodyMetricRoute> {
            BodyMetricScreen(onBack = { navController.popBackStack() })
        }
    }
}
