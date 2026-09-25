package com.kkfittracking.ui.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kkfittracking.data.GameRepository
import com.kkfittracking.data.RoutineRepository
import com.kkfittracking.data.SettingsRepository
import com.kkfittracking.data.WorkoutRepository
import com.kkfittracking.guide.GuideState
import com.kkfittracking.guide.GuidedWorkout
import com.kkfittracking.model.DayExercise
import com.kkfittracking.model.GameStats
import com.kkfittracking.model.PlannedExercise
import com.kkfittracking.model.Routine
import com.kkfittracking.model.UnitSystem
import com.kkfittracking.ui.appViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WorkoutUiState(
    val date: LocalDate,
    val exercises: List<DayExercise> = emptyList(),
    val units: UnitSystem = UnitSystem.METRIC,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    settingsRepository: SettingsRepository,
    gameRepository: GameRepository,
    private val guidedWorkout: GuidedWorkout,
) : ViewModel() {
    /** The play button's guided workout, when one runs. */
    val guide: StateFlow<GuideState> = guidedWorkout.state

    /** The exercise the guide sends the user to right after starting. */
    var guideOpens by mutableStateOf<String?>(null)
        private set

    val gameStats: StateFlow<GameStats?> =
        gameRepository.stats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val routines: StateFlow<List<Routine>> =
        routineRepository.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // The calendar screen writes the picked day into this key of our SavedStateHandle.
    private val epochDay = savedStateHandle.getStateFlow(KEY_EPOCH_DAY, LocalDate.now().toEpochDay())

    val uiState: StateFlow<WorkoutUiState> = epochDay
        .flatMapLatest { day ->
            val date = LocalDate.ofEpochDay(day)
            combine(workoutRepository.observeDay(date), settingsRepository.settings) { exercises, settings ->
                WorkoutUiState(date = date, exercises = exercises, units = settings.unitSystem, isLoading = false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WorkoutUiState(date = LocalDate.ofEpochDay(epochDay.value)),
        )

    fun showPreviousDay() = moveDays(-1)

    fun showNextDay() = moveDays(1)

    fun showToday() {
        savedStateHandle[KEY_EPOCH_DAY] = LocalDate.now().toEpochDay()
    }

    fun ungroupSuperset(supersetId: String) {
        viewModelScope.launch { workoutRepository.ungroupSuperset(supersetId) }
    }

    /** Removes several exercises (and their sets) from the day at once. */
    fun deleteExercises(workoutExerciseIds: Collection<String>) {
        viewModelScope.launch { workoutRepository.deleteWorkoutExercises(workoutExerciseIds.toList()) }
    }

    fun deleteExercise(workoutExerciseId: String) {
        viewModelScope.launch { workoutRepository.deleteWorkoutExercise(workoutExerciseId) }
    }

    /** Adds a routine's exercises to the shown day, ready to be filled in, with or without its supersets. */
    fun applyRoutine(routineId: String, withSupersets: Boolean) {
        val date = uiState.value.date
        viewModelScope.launch {
            workoutRepository.addPlannedExercises(date, routineRepository.plannedExercises(routineId), withSupersets)
        }
    }

    /** Copies the shown day's exercises and supersets (not their sets) to today, then shows today. */
    fun copyExercisesToToday() {
        val exercises = uiState.value.exercises.map {
            PlannedExercise(
                it.exerciseId, it.supersetId, it.transitionSeconds, it.roundRestSeconds,
                it.supersetRounds, it.supersetDropLast, it.memberRounds, it.memberDropSet,
            )
        }
        viewModelScope.launch {
            workoutRepository.addPlannedExercises(LocalDate.now(), exercises, withSupersets = true)
            showToday()
        }
    }

    /** Starts guiding the shown day's plan and opens its first exercise. */
    fun startGuide() {
        val date = uiState.value.date
        viewModelScope.launch { guideOpens = guidedWorkout.start(date)?.exerciseId }
    }

    fun consumeGuideOpen() {
        guideOpens = null
    }

    fun pauseGuide() = guidedWorkout.pause()

    fun resumeGuide() = guidedWorkout.resume()

    fun skipInGuide() {
        guidedWorkout.state.value.target?.let { guidedWorkout.skip(it.exerciseId) }
    }

    fun stopGuide() = guidedWorkout.stop()

    private fun moveDays(days: Long) {
        savedStateHandle[KEY_EPOCH_DAY] = epochDay.value + days
    }

    companion object {
        const val KEY_EPOCH_DAY = "epochDay"

        val Factory = appViewModelFactory { container ->
            WorkoutViewModel(
                savedStateHandle = createSavedStateHandle(),
                workoutRepository = container.workoutRepository,
                routineRepository = container.routineRepository,
                settingsRepository = container.settingsRepository,
                gameRepository = container.gameRepository,
                guidedWorkout = container.guidedWorkout,
            )
        }
    }
}
