package com.kkfittracking.ui.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kkfittracking.data.ExerciseRepository
import com.kkfittracking.data.RoutineRepository
import com.kkfittracking.data.SettingsRepository
import com.kkfittracking.model.Exercise
import com.kkfittracking.model.ExercisePlan
import com.kkfittracking.model.Routine
import com.kkfittracking.model.Settings
import com.kkfittracking.ui.RoutineRoute
import com.kkfittracking.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutinesViewModel(private val repository: RoutineRepository) : ViewModel() {
    /** Null while loading. */
    val routines: StateFlow<List<Routine>?> =
        repository.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addStarterPlans() {
        viewModelScope.launch { repository.addStarterPlans() }
    }

    fun createRoutine(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(repository.createRoutine(name)) }
    }

    companion object {
        val Factory = appViewModelFactory { container -> RoutinesViewModel(container.routineRepository) }
    }
}

class RoutineViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val routineId: String = savedStateHandle.toRoute<RoutineRoute>().routineId

    /** Every exercise by id, for the set plans of the plan's exercises. */
    val exercises: StateFlow<Map<String, Exercise>> = exerciseRepository.exercises
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val settings: StateFlow<Settings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    /** Saves an exercise's sets, reps, weight and rest (they go with the exercise into every plan and day). */
    fun savePlan(exerciseId: String, plan: ExercisePlan) {
        viewModelScope.launch { exerciseRepository.savePlan(exerciseId, plan) }
    }

    val routine: StateFlow<Routine?> =
        repository.observeRoutine(routineId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun rename(name: String) {
        viewModelScope.launch { repository.renameRoutine(routineId, name) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
            onDeleted()
        }
    }

    fun removeExercise(routineExerciseId: String) {
        viewModelScope.launch { repository.removeExercise(routineExerciseId) }
    }

    /** Moves an exercise, or a whole superset, up (-1) or down (+1); [blockIndex] counts [Routine.blocks]. */
    fun moveBlock(blockIndex: Int, direction: Int) {
        viewModelScope.launch { repository.moveInPlan(routineId, blockIndex, direction) }
    }

    companion object {
        val Factory = appViewModelFactory { container ->
            RoutineViewModel(
                createSavedStateHandle(),
                container.routineRepository,
                container.exerciseRepository,
                container.settingsRepository,
            )
        }
    }
}
