package com.kerimkolberg.fitnessapp.ui.exercises

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kerimkolberg.fitnessapp.data.ExerciseRepository
import com.kerimkolberg.fitnessapp.data.RoutineRepository
import com.kerimkolberg.fitnessapp.model.Category
import com.kerimkolberg.fitnessapp.ui.ExercisePickerRoute
import com.kerimkolberg.fitnessapp.ui.appViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExercisePickerUiState(
    val categories: List<Category> = emptyList(),
    val groups: List<ExerciseGroup> = emptyList(),
    val selectedCategoryId: String? = null,
)

class ExercisePickerViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
) : ViewModel() {
    /** Set when picking an exercise to add to this routine instead of logging it. */
    val routineId: String? = savedStateHandle.toRoute<ExercisePickerRoute>().routineId

    /** Held as Compose state (not a flow) so the search field never lags behind typing. */
    var query by mutableStateOf("")
        private set

    private val selectedCategoryId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExercisePickerUiState> = combine(
        repository.categories,
        repository.exercises,
        snapshotFlow { query },
        selectedCategoryId,
    ) { categories, exercises, query, selectedCategoryId ->
        ExercisePickerUiState(
            categories = categories,
            groups = groupExercises(categories, exercises, query, selectedCategoryId),
            selectedCategoryId = selectedCategoryId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisePickerUiState())

    fun updateQuery(value: String) {
        query = value
    }

    /** Logs the exercise, or adds it to the routine being edited and then calls [onAddedToRoutine]. */
    fun pick(exerciseId: String, onLog: (String) -> Unit, onAddedToRoutine: () -> Unit) {
        val routine = routineId
        if (routine == null) {
            onLog(exerciseId)
        } else {
            viewModelScope.launch {
                routineRepository.addExercise(routine, exerciseId)
                onAddedToRoutine()
            }
        }
    }

    /** Selects a category filter, or shows all categories when [categoryId] is null. */
    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }

    companion object {
        val Factory = appViewModelFactory { container ->
            ExercisePickerViewModel(createSavedStateHandle(), container.exerciseRepository, container.routineRepository)
        }
    }
}
