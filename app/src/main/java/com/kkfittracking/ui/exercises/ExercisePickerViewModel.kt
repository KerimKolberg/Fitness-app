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
import com.kkfittracking.model.Muscle
import com.kkfittracking.model.Routine
import com.kkfittracking.model.Tendon
import com.kkfittracking.model.TrainingStyle
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
    /** The library as section, muscle and style headers with exercises under them. */
    val rows: List<LibraryRow> = emptyList(),
    val selectedCategoryId: String? = null,
    /** Muscles to filter by, offered once a section is chosen. */
    val muscles: List<Muscle> = emptyList(),
    val selectedMuscle: Muscle? = null,
    val styles: List<TrainingStyle> = emptyList(),
    val selectedStyle: TrainingStyle? = null,
    /** Tendons to filter by, offered in the isometric and eccentric sections. */
    val tendons: List<Tendon> = emptyList(),
    val selectedTendon: Tendon? = null,
    val plans: List<Routine> = emptyList(),
    val selectedPlanId: String? = null,
)

/** The section, muscle and style filters, chosen together. */
private data class LibrarySelection(
    val categoryId: String? = null,
    val muscle: Muscle? = null,
    val style: TrainingStyle? = null,
    val tendon: Tendon? = null,
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

    private val selection = MutableStateFlow(LibrarySelection())
    private val selectedPlanId = MutableStateFlow<String?>(null)
    private val planFilter = combine(routineRepository.routines, selectedPlanId) { plans, planId -> plans to planId }

    val uiState: StateFlow<ExercisePickerUiState> = combine(
        repository.categories,
        repository.exercises,
        snapshotFlow { query },
        selection,
        planFilter,
    ) { categories, exercises, query, selection, (plans, planId) ->
        val plan = plans.firstOrNull { it.id == planId }
        val filter = LibraryFilter(
            query = query,
            categoryId = selection.categoryId,
            muscle = selection.muscle,
            style = selection.style,
            tendon = selection.tendon,
            allowedIds = plan?.exercises?.map { it.exerciseId }?.toSet(),
        )
        ExercisePickerUiState(
            categories = categories,
            rows = libraryRows(categories, exercises, filter),
            selectedCategoryId = selection.categoryId,
            muscles = if (selection.categoryId != null || selection.style != null) {
                muscleChoices(categories, exercises, filter)
            } else {
                emptyList()
            },
            selectedMuscle = selection.muscle,
            styles = styleChoices(categories, exercises, filter),
            selectedStyle = selection.style,
            tendons = if (selection.style in TENDON_STYLES || selection.tendon != null) {
                tendonChoices(categories, exercises, filter)
            } else {
                emptyList()
            },
            selectedTendon = selection.tendon,
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

    /** Selects a section filter, or shows everything when [categoryId] is null. */
    fun selectCategory(categoryId: String?) {
        selection.value = LibrarySelection(categoryId = categoryId, style = selection.value.style.takeIf { categoryId != null })
        selectedPlanId.value = null
    }

    /** Shows one muscle of the chosen section or style (tap again to show all of them). */
    fun selectMuscle(muscle: Muscle) {
        selection.value = selection.value.let { it.copy(muscle = if (it.muscle == muscle) null else muscle) }
    }

    /** Opens a training style section, such as Stretching (tap again to close it). Its muscles become sub-sections. */
    fun selectStyle(style: TrainingStyle) {
        selection.value = selection.value.let { it.copy(style = if (it.style == style) null else style, muscle = null, tendon = null) }
    }

    /** Shows the exercises that load one tendon (tap again to show all). */
    fun selectTendon(tendon: Tendon) {
        selection.value = selection.value.let { it.copy(tendon = if (it.tendon == tendon) null else tendon) }
    }

    /** Shows only the exercises of a plan (tap again to show all). */
    fun selectPlan(planId: String) {
        selectedPlanId.value = if (selectedPlanId.value == planId) null else planId
        selection.value = LibrarySelection()
    }

    companion object {
        /** The sections where tendons are offered as a filter. */
        private val TENDON_STYLES = setOf(TrainingStyle.ISOMETRIC, TrainingStyle.ECCENTRIC)

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
