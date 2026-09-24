package com.kerimkolberg.fitnessapp.ui.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kerimkolberg.fitnessapp.data.RoutineRepository
import com.kerimkolberg.fitnessapp.model.Routine
import com.kerimkolberg.fitnessapp.ui.RoutineRoute
import com.kerimkolberg.fitnessapp.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
) : ViewModel() {
    val routineId: String = savedStateHandle.toRoute<RoutineRoute>().routineId

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

    fun moveExercise(routineExerciseId: String, direction: Int) {
        viewModelScope.launch { repository.moveExercise(routineId, routineExerciseId, direction) }
    }

    companion object {
        val Factory = appViewModelFactory { container ->
            RoutineViewModel(createSavedStateHandle(), container.routineRepository)
        }
    }
}
