package com.kkfittracking.ui.exercises

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kkfittracking.data.BuiltInExercises
import com.kkfittracking.data.ExerciseRepository
import com.kkfittracking.model.Category
import com.kkfittracking.model.ExerciseLink
import com.kkfittracking.model.ExerciseType
import com.kkfittracking.model.Muscle
import com.kkfittracking.model.TrainingStyle
import com.kkfittracking.ui.EditExerciseRoute
import com.kkfittracking.ui.appViewModelFactory
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
    var tempo by mutableStateOf("")
        private set
    var perSide by mutableStateOf(false)
        private set
    var muscle by mutableStateOf(Muscle.OTHER)
        private set
    /** Other muscles it trains besides [muscle], in any section. */
    var otherMuscles by mutableStateOf<List<Muscle>>(emptyList())
        private set
    var style by mutableStateOf(TrainingStyle.STRENGTH)
        private set

    /** Other training styles it counts as, such as stretching for a yoga pose. */
    var otherStyles by mutableStateOf<List<TrainingStyle>>(emptyList())
        private set
    var links by mutableStateOf<List<ExerciseLink>>(emptyList())
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
                tempo = exercise.tempo
                perSide = exercise.perSide
                muscle = exercise.muscle
                otherMuscles = exercise.muscles.drop(1)
                style = exercise.style
                otherStyles = exercise.styles.drop(1)
                links = exercise.links
            } else if (categoryId == null) {
                repository.categories.first().firstOrNull()?.let { updateCategory(it.id) }
            }
        }
    }

    fun updateName(value: String) {
        name = value
        nameError = null
    }

    /** The body section key of the chosen category. */
    val regionKey: String? get() = categoryId?.let(BuiltInExercises::regionKeyOf)

    /** Moving to another section picks a muscle of that section. */
    fun updateCategory(value: String) {
        categoryId = value
        val key = BuiltInExercises.regionKeyOf(value)
        if (muscle !in Muscle.forRegion(key)) muscle = Muscle.defaultFor(key)
        if (isNew && style == TrainingStyle.STRENGTH) style = TrainingStyle.defaultFor(key, type)
    }

    fun updateMuscle(value: Muscle) {
        muscle = value
        otherMuscles = otherMuscles - value
    }

    /** Adds or removes a muscle it also trains. */
    fun toggleOtherMuscle(value: Muscle) {
        if (value == muscle) return
        otherMuscles = if (value in otherMuscles) otherMuscles - value else otherMuscles + value
    }

    fun updateStyle(value: TrainingStyle) {
        style = value
        otherStyles = otherStyles - value
    }

    fun toggleOtherStyle(value: TrainingStyle) {
        if (value == style) return
        otherStyles = if (value in otherStyles) otherStyles - value else otherStyles + value
    }

    fun updateType(value: ExerciseType) {
        type = value
        if (value == ExerciseType.INTERVALS) style = TrainingStyle.HIIT
    }

    fun addLink(link: ExerciseLink) {
        if (links.none { it.url == link.url }) links = links + link
    }

    fun removeLink(link: ExerciseLink) {
        links = links - link
    }

    fun updateNotes(value: String) {
        notes = value
    }

    fun updateTempo(value: String) {
        tempo = value
    }

    fun updatePerSide(value: Boolean) {
        perSide = value
    }

    fun save() {
        val category = categoryId
        if (name.isBlank()) {
            nameError = "Enter a name"
            return
        }
        if (category == null) return
        viewModelScope.launch {
            repository.saveExercise(exerciseId, name, category, type, notes, tempo, perSide, listOf(muscle) + otherMuscles, listOf(style) + otherStyles, links)
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
