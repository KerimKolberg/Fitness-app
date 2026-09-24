package com.kerimkolberg.fitnessapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kerimkolberg.fitnessapp.ui.calendar.CalendarScreen
import com.kerimkolberg.fitnessapp.ui.exercises.EditExerciseScreen
import com.kerimkolberg.fitnessapp.ui.exercises.ExercisePickerScreen
import com.kerimkolberg.fitnessapp.ui.log.ExerciseLogScreen
import com.kerimkolberg.fitnessapp.ui.settings.SettingsScreen
import com.kerimkolberg.fitnessapp.ui.workout.WorkoutScreen
import com.kerimkolberg.fitnessapp.ui.workout.WorkoutViewModel
import kotlinx.serialization.Serializable

@Serializable
object WorkoutRoute

@Serializable
data class ExercisePickerRoute(val epochDay: Long)

@Serializable
data class EditExerciseRoute(val exerciseId: String? = null)

@Serializable
data class ExerciseLogRoute(val epochDay: Long, val exerciseId: String)

@Serializable
data class CalendarRoute(val epochDay: Long)

@Serializable
object SettingsRoute

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
            )
        }
        composable<ExercisePickerRoute> { entry ->
            val route = entry.toRoute<ExercisePickerRoute>()
            ExercisePickerScreen(
                onBack = { navController.popBackStack() },
                onPickExercise = { exerciseId ->
                    navController.navigate(ExerciseLogRoute(route.epochDay, exerciseId)) {
                        popUpTo<ExercisePickerRoute> { inclusive = true }
                    }
                },
                onCreateExercise = { navController.navigate(EditExerciseRoute()) },
                onEditExercise = { exerciseId -> navController.navigate(EditExerciseRoute(exerciseId)) },
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
        composable<ExerciseLogRoute> {
            ExerciseLogScreen(
                onBack = { navController.popBackStack() },
                onEditExercise = { exerciseId -> navController.navigate(EditExerciseRoute(exerciseId)) },
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
    }
}
