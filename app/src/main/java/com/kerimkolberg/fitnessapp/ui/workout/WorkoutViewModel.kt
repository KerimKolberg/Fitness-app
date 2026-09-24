package com.kerimkolberg.fitnessapp.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kerimkolberg.fitnessapp.data.SettingsRepository
import com.kerimkolberg.fitnessapp.data.WorkoutRepository
import com.kerimkolberg.fitnessapp.model.DayExercise
import com.kerimkolberg.fitnessapp.model.UnitSystem
import com.kerimkolberg.fitnessapp.ui.appViewModelFactory
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
    settingsRepository: SettingsRepository,
) : ViewModel() {

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

    fun deleteExercise(workoutExerciseId: String) {
        viewModelScope.launch { workoutRepository.deleteWorkoutExercise(workoutExerciseId) }
    }

    private fun moveDays(days: Long) {
        savedStateHandle[KEY_EPOCH_DAY] = epochDay.value + days
    }

    companion object {
        const val KEY_EPOCH_DAY = "epochDay"

        val Factory = appViewModelFactory { container ->
            WorkoutViewModel(
                savedStateHandle = createSavedStateHandle(),
                workoutRepository = container.workoutRepository,
                settingsRepository = container.settingsRepository,
            )
        }
    }
}
