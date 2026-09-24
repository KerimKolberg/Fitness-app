package com.kerimkolberg.fitnessapp.ui.log

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kerimkolberg.fitnessapp.data.ExerciseRepository
import com.kerimkolberg.fitnessapp.data.SettingsRepository
import com.kerimkolberg.fitnessapp.data.WorkoutRepository
import com.kerimkolberg.fitnessapp.model.Exercise
import com.kerimkolberg.fitnessapp.model.HistorySession
import com.kerimkolberg.fitnessapp.model.SetEntry
import com.kerimkolberg.fitnessapp.model.Settings
import com.kerimkolberg.fitnessapp.model.UnitSystem
import com.kerimkolberg.fitnessapp.model.personalRecordSetIds
import com.kerimkolberg.fitnessapp.timer.RestTimer
import com.kerimkolberg.fitnessapp.timer.RestTimerState
import com.kerimkolberg.fitnessapp.ui.ExerciseLogRoute
import com.kerimkolberg.fitnessapp.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExerciseLogUiState(
    val exercise: Exercise? = null,
    val units: UnitSystem = UnitSystem.METRIC,
    /** The sets logged on the screen's date. */
    val sets: List<SetEntry> = emptyList(),
    /** The most recent session before the screen's date. */
    val previousSession: HistorySession? = null,
    val history: List<HistorySession> = emptyList(),
    /** Sets that were a personal record when logged. */
    val recordSetIds: Set<String> = emptySet(),
    val settings: Settings = Settings(),
    val isLoading: Boolean = true,
)

class ExerciseLogViewModel(
    savedStateHandle: SavedStateHandle,
    exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
    private val restTimer: RestTimer,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<ExerciseLogRoute>()
    val date: LocalDate = LocalDate.ofEpochDay(route.epochDay)
    val exerciseId: String = route.exerciseId

    var input by mutableStateOf(SetInput())
        private set

    /** The set being edited, or null when the buttons add a new set. */
    var selectedSetId by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val timerState: StateFlow<RestTimerState> = restTimer.state

    val uiState: StateFlow<ExerciseLogUiState> = combine(
        exerciseRepository.observeExercise(exerciseId),
        workoutRepository.observeHistory(exerciseId),
        settingsRepository.settings,
    ) { exercise, history, settings ->
        ExerciseLogUiState(
            exercise = exercise,
            units = settings.unitSystem,
            sets = history.firstOrNull { it.date == date }?.sets.orEmpty(),
            previousSession = history.firstOrNull { it.date < date },
            history = history,
            recordSetIds = exercise?.let { personalRecordSetIds(history, it.type) }.orEmpty(),
            settings = settings,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLogUiState())

    init {
        // Start from the last values used, like a gym notebook: today's last set, else last session's.
        viewModelScope.launch {
            val state = uiState.first { !it.isLoading }
            val lastSet = state.sets.lastOrNull() ?: state.previousSession?.sets?.lastOrNull()
            if (lastSet != null && input == SetInput()) {
                input = SetInput.from(lastSet.values, state.units)
            }
        }
    }

    fun updateInput(value: SetInput) {
        input = value
        errorMessage = null
    }

    fun adjustWeight(direction: Int) = updateInput(input.adjustWeight(direction, uiState.value.units))

    fun adjustReps(direction: Int) = updateInput(input.adjustReps(direction))

    /** Adds a new set, or updates the selected one. */
    fun save() {
        val state = uiState.value
        val exercise = state.exercise ?: return
        when (val result = input.toSetValues(exercise.type, state.units)) {
            is SetInput.Result.Invalid -> errorMessage = result.message
            is SetInput.Result.Valid -> {
                val editingId = selectedSetId
                selectedSetId = null
                viewModelScope.launch {
                    if (editingId != null) {
                        workoutRepository.updateSet(editingId, result.values)
                    } else {
                        workoutRepository.addSet(date, exerciseId, result.values)
                        if (state.settings.autoStartRestTimer) {
                            restTimer.start(state.settings.restTimerSeconds)
                        }
                    }
                }
            }
        }
    }

    /** Tapping a set loads it for editing; tapping it again goes back to adding sets. */
    fun selectSet(set: SetEntry) {
        errorMessage = null
        if (selectedSetId == set.id) {
            selectedSetId = null
        } else {
            selectedSetId = set.id
            input = SetInput.from(set.values, uiState.value.units)
        }
    }

    fun deleteSelectedSet() {
        val id = selectedSetId ?: return
        selectedSetId = null
        viewModelScope.launch { workoutRepository.deleteSet(id) }
    }

    fun clearInput() = updateInput(SetInput())

    fun startTimer(seconds: Int) = restTimer.start(seconds)

    fun stopTimer() = restTimer.stop()

    fun addTimerSeconds(delta: Int) = restTimer.addSeconds(delta)

    companion object {
        val Factory = appViewModelFactory { container ->
            ExerciseLogViewModel(
                savedStateHandle = createSavedStateHandle(),
                exerciseRepository = container.exerciseRepository,
                workoutRepository = container.workoutRepository,
                settingsRepository = container.settingsRepository,
                restTimer = container.restTimer,
            )
        }
    }
}
