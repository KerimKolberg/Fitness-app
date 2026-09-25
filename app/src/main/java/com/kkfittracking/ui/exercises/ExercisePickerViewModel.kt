package com.kkfittracking.ui.exercises

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kkfittracking.data.ExerciseRepository
import com.kkfittracking.data.RoutineRepository
import com.kkfittracking.data.WorkoutRepository
import com.kkfittracking.model.Category
import com.kkfittracking.model.MAX_SUPERSET_SIZE
import com.kkfittracking.model.Routine
import com.kkfittracking.ui.ExercisePickerRoute
import com.kkfittracking.ui.appViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExercisePickerUiState(
    val categories: List<Category> = emptyList(),
    val groups: List<ExerciseGroup> = emptyList(),
    val selectedCategoryId: String? = null,
    val plans: List<Routine> = emptyList(),
    val selectedPlanId: String? = null,
)

class ExercisePickerViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<ExercisePickerRoute>()

    /** Set when picking an exercise to add to this routine instead of logging it. */
    val routineId: String? = route.routineId

    /** True when picking several exercises to group as a superset. */
    val supersetMode: Boolean = route.superset

    /** The exercises picked for the superset, in the order they will be done. */
    var supersetPicks by mutableStateOf<List<String>>(emptyList())
        private set

    /** Held as Compose state (not a flow) so the search field never lags behind typing. */
    var query by mutableStateOf("")
        private set

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val selectedPlanId = MutableStateFlow<String?>(null)
    private val planFilter = combine(routineRepository.routines, selectedPlanId) { plans, planId -> plans to planId }

    val uiState: StateFlow<ExercisePickerUiState> = combine(
        repository.categories,
        repository.exercises,
        snapshotFlow { query },
        selectedCategoryId,
        planFilter,
    ) { categories, exercises, query, selectedCategoryId, (plans, planId) ->
        val plan = plans.firstOrNull { it.id == planId }
        ExercisePickerUiState(
            categories = categories,
            groups = groupExercises(
                categories,
                exercises,
                query,
                selectedCategoryId,
                allowedIds = plan?.exercises?.map { it.exerciseId }?.toSet(),
            ),
            selectedCategoryId = selectedCategoryId,
            plans = plans,
            selectedPlanId = plan?.id,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisePickerUiState())

    fun updateQuery(value: String) {
        query = value
    }

    /** Logs the exercise, or adds it to the routine being edited and then calls [onAddedToRoutine]. */
    fun pick(exerciseId: String, onLog: (String) -> Unit, onAddedToRoutine: () -> Unit) {
        val routine = routineId
        if (supersetMode) {
            togglePick(exerciseId)
        } else if (routine == null) {
            onLog(exerciseId)
        } else {
            viewModelScope.launch {
                routineRepository.addExercise(routine, exerciseId)
                onAddedToRoutine()
            }
        }
    }

    private fun togglePick(exerciseId: String) {
        supersetPicks = when {
            exerciseId in supersetPicks -> supersetPicks - exerciseId
            supersetPicks.size < MAX_SUPERSET_SIZE -> supersetPicks + exerciseId
            else -> supersetPicks
        }
    }

    /** Groups the picked exercises as a superset on the day, then calls [onCreated] with the first one. */
    fun createSuperset(onCreated: (String) -> Unit) {
        val picks = supersetPicks
        if (picks.size < 2) return
        viewModelScope.launch {
            workoutRepository.createSuperset(LocalDate.ofEpochDay(route.epochDay), picks)
            onCreated(picks.first())
        }
    }

    /** Selects a category filter, or shows all categories when [categoryId] is null. */
    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
        selectedPlanId.value = null
    }

    /** Shows only the exercises of a plan (tap again to show all). */
    fun selectPlan(planId: String) {
        selectedPlanId.value = if (selectedPlanId.value == planId) null else planId
        selectedCategoryId.value = null
    }

    companion object {
        val Factory = appViewModelFactory { container ->
            ExercisePickerViewModel(
                createSavedStateHandle(),
                container.exerciseRepository,
                container.routineRepository,
                container.workoutRepository,
            )
        }
    }
}
