package com.kerimkolberg.fitnessapp.ui.exercises

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kerimkolberg.fitnessapp.data.ExerciseRepository
import com.kerimkolberg.fitnessapp.model.Category
import com.kerimkolberg.fitnessapp.model.ExerciseType
import com.kerimkolberg.fitnessapp.ui.EditExerciseRoute
import com.kerimkolberg.fitnessapp.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EditExerciseResult { SAVED, DELETED }

class EditExerciseViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository,
) : ViewModel() {
    private val exerciseId: String? = savedStateHandle.toRoute<EditExerciseRoute>().exerciseId

    val isNew: Boolean = exerciseId == null

    val categories: StateFlow<List<Category>> =
        repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var name by mutableStateOf("")
        private set
    var categoryId by mutableStateOf<String?>(null)
        private set
    var type by mutableStateOf(ExerciseType.WEIGHT_REPS)
        private set
    var notes by mutableStateOf("")
        private set
    var nameError by mutableStateOf<String?>(null)
        private set

    /** Set once the exercise was saved or deleted, so the screen can close. */
    var result by mutableStateOf<EditExerciseResult?>(null)
        private set

    init {
        viewModelScope.launch {
            val exercise = exerciseId?.let { repository.getExercise(it) }
            if (exercise != null) {
                name = exercise.name
                categoryId = exercise.categoryId
                type = exercise.type
                notes = exercise.notes
            } else if (categoryId == null) {
                categoryId = repository.categories.first().firstOrNull()?.id
            }
        }
    }

    fun updateName(value: String) {
        name = value
        nameError = null
    }

    fun updateCategory(value: String) {
        categoryId = value
    }

    fun updateType(value: ExerciseType) {
        type = value
    }

    fun updateNotes(value: String) {
        notes = value
    }

    fun save() {
        val category = categoryId
        if (name.isBlank()) {
            nameError = "Enter a name"
            return
        }
        if (category == null) return
        viewModelScope.launch {
            repository.saveExercise(exerciseId, name, category, type, notes)
            result = EditExerciseResult.SAVED
        }
    }

    fun delete() {
        val id = exerciseId ?: return
        viewModelScope.launch {
            repository.deleteExercise(id)
            result = EditExerciseResult.DELETED
        }
    }

    companion object {
        val Factory = appViewModelFactory { container ->
            EditExerciseViewModel(createSavedStateHandle(), container.exerciseRepository)
        }
    }
}
