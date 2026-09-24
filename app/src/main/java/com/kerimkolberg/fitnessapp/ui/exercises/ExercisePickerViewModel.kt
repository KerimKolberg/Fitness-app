package com.kerimkolberg.fitnessapp.ui.exercises

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerimkolberg.fitnessapp.data.ExerciseRepository
import com.kerimkolberg.fitnessapp.model.Category
import com.kerimkolberg.fitnessapp.ui.appViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ExercisePickerUiState(
    val categories: List<Category> = emptyList(),
    val groups: List<ExerciseGroup> = emptyList(),
    val selectedCategoryId: String? = null,
)

class ExercisePickerViewModel(repository: ExerciseRepository) : ViewModel() {
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

    /** Selects a category filter, or shows all categories when [categoryId] is null. */
    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }

    companion object {
        val Factory = appViewModelFactory { container -> ExercisePickerViewModel(container.exerciseRepository) }
    }
}
