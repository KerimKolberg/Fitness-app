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
import com.kerimkolberg.fitnessapp.data.GameRepository
import com.kerimkolberg.fitnessapp.data.RoutineRepository
import com.kerimkolberg.fitnessapp.data.SettingsRepository
import com.kerimkolberg.fitnessapp.data.WorkoutRepository
import com.kerimkolberg.fitnessapp.model.DayExercise
import com.kerimkolberg.fitnessapp.model.Exercise
import com.kerimkolberg.fitnessapp.model.ExerciseType
import com.kerimkolberg.fitnessapp.model.GameStats
import com.kerimkolberg.fitnessapp.model.HistorySession
import com.kerimkolberg.fitnessapp.model.Routine
import com.kerimkolberg.fitnessapp.model.SetEntry
import com.kerimkolberg.fitnessapp.model.SetValues
import com.kerimkolberg.fitnessapp.model.Settings
import com.kerimkolberg.fitnessapp.model.UnitSystem
import com.kerimkolberg.fitnessapp.model.XpRules
import com.kerimkolberg.fitnessapp.model.celebrationsBetween
import com.kerimkolberg.fitnessapp.model.dropSetDue
import com.kerimkolberg.fitnessapp.model.dropSetWeightKg
import com.kerimkolberg.fitnessapp.model.formatNumber
import com.kerimkolberg.fitnessapp.model.isLastInSuperset
import com.kerimkolberg.fitnessapp.model.nextInSuperset
import com.kerimkolberg.fitnessapp.model.parseDecimal
import com.kerimkolberg.fitnessapp.model.personalRecordSetIds
import com.kerimkolberg.fitnessapp.model.recordScore
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
    /** The exercises of this exercise's superset on the screen's date, in order; empty if none. */
    val superset: List<DayExercise> = emptyList(),
    /** This exercise's entry on the screen's date (with its drop set plan), once logged or planned. */
    val dayEntry: DayExercise? = null,
    val isLoading: Boolean = true,
)

class ExerciseLogViewModel(
    savedStateHandle: SavedStateHandle,
    exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
    private val restTimer: RestTimer,
    gameRepository: GameRepository,
    private val routineRepository: RoutineRepository,
) : ViewModel() {
    /** All plans, to add this exercise to or remove it from. */
    val plans: StateFlow<List<Routine>> =
        routineRepository.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** A message to celebrate with, such as a new record or an unlocked achievement. */
    var celebration by mutableStateOf<String?>(null)
        private set

    /** When on, new sets are saved as drop sets and the weight is lowered for the next one. */
    var dropMode by mutableStateOf(false)
        private set

    /** The superset exercise to open next, after a set was saved. */
    var switchTo by mutableStateOf<String?>(null)
        private set

    val timerState: StateFlow<RestTimerState> = restTimer.state

    val uiState: StateFlow<ExerciseLogUiState> = combine(
        exerciseRepository.observeExercise(exerciseId),
        workoutRepository.observeHistory(exerciseId),
        settingsRepository.settings,
        workoutRepository.observeDay(date),
    ) { exercise, history, settings, day ->
        val entry = day.firstOrNull { it.exerciseId == exerciseId }
        val superset = entry?.supersetId?.let { id -> day.filter { it.supersetId == id } }.orEmpty()
        ExerciseLogUiState(
            dayEntry = entry,
            superset = if (superset.size >= 2) superset else emptyList(),
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
        // Compare each new snapshot of the game stats with the previous one and celebrate what changed.
        viewModelScope.launch {
            var previous: GameStats? = null
            gameRepository.stats.collect { stats ->
                previous?.let { before -> celebrate(celebrationsBetween(before, stats)) }
                previous = stats
            }
        }
        // Start from the last values used, like a gym notebook: today's last set, else last session's.
        viewModelScope.launch {
            val state = uiState.first { !it.isLoading }
            val lastSet = state.sets.lastOrNull() ?: state.previousSession?.sets?.lastOrNull()
            if (lastSet != null && input == SetInput()) {
                input = SetInput.from(lastSet.values, state.units)
            }
            // The day's plan may say the next set is a drop set.
            val entry = state.dayEntry
            if (entry != null && canUseDropSets(state) && dropSetDue(entry.dropSetMode, entry.plannedSets, state.sets)) {
                dropMode = true
                lowerWeightForDrop(state)
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
                if (editingId != null) {
                    // Editing keeps whether the set was a drop set.
                    val wasDrop = state.sets.firstOrNull { it.id == editingId }?.values?.isDropSet == true
                    viewModelScope.launch { workoutRepository.updateSet(editingId, result.values.copy(isDropSet = wasDrop)) }
                    return
                }
                val isDrop = dropMode && canUseDropSets(state)
                val values = result.values.copy(isDropSet = isDrop)
                val memberIds = state.superset.map { it.exerciseId }
                viewModelScope.launch {
                    val best = state.history.flatMap { it.sets }.mapNotNull { recordScore(it.values, exercise.type) }.maxOrNull()
                    val score = recordScore(values, exercise.type)
                    workoutRepository.addSet(date, exerciseId, values)
                    if (best != null && score != null && score > best) {
                        celebrate(listOf("⭐ New personal record! +${XpRules.PER_RECORD} XP"))
                    }
                    afterNewSet(state, values, memberIds)
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

    /**
     * What happens after a new set: straight into a drop set when one is due (no rest), else to the
     * next superset exercise with a short countdown to get there, else normal rest.
     */
    private fun afterNewSet(state: ExerciseLogUiState, saved: SetValues, memberIds: List<String>) {
        val settings = state.settings
        if (saved.isDropSet) {
            // Ready for the next drop: lighter again. Rest restarts after each drop.
            lowerWeightForDrop(state)
            if (settings.autoStartRestTimer) restTimer.start(settings.restTimerSeconds)
            return
        }
        val entry = state.dayEntry
        val setsNow = state.sets + SetEntry("new", saved)
        if (entry != null && canUseDropSets(state) && dropSetDue(entry.dropSetMode, entry.plannedSets, setsNow)) {
            dropMode = true
            lowerWeightForDrop(state)
            celebrate(listOf("↘ Drop set next: ${input.weight} ${state.units.weightUnit}, no rest"))
            return
        }
        val next = nextInSuperset(memberIds, exerciseId)
        val nextName = state.superset.firstOrNull { it.exerciseId == next }?.exerciseName
        if (next != null && !isLastInSuperset(memberIds, exerciseId)) {
            // Mid-round: a short countdown to walk to the next machine.
            val seconds = entry?.transitionSeconds ?: settings.supersetTransitionSeconds
            if (settings.autoStartRestTimer && seconds > 0) restTimer.start(seconds, label = "Go to $nextName")
        } else if (settings.autoStartRestTimer) {
            restTimer.start(settings.restTimerSeconds)
        }
        if (settings.supersetAutoAdvance && next != null) switchTo = next
    }

    fun canUseDropSets(state: ExerciseLogUiState = uiState.value): Boolean =
        state.settings.dropSetsEnabled && state.exercise?.type == ExerciseType.WEIGHT_REPS

    /** Turns drop set mode on (lowering the weight right away) or off. */
    fun toggleDropMode() {
        dropMode = !dropMode
        if (dropMode) lowerWeightForDrop(uiState.value)
    }

    private fun lowerWeightForDrop(state: ExerciseLogUiState) {
        val current = parseDecimal(input.weight) ?: return
        val kg = dropSetWeightKg(state.units.weightToKg(current), state.settings.dropSetPercent, state.units)
        updateInput(input.copy(weight = formatNumber(state.units.weightFromKg(kg))))
    }

    fun consumeSwitch() {
        switchTo = null
    }

    fun setInPlan(planId: String, inPlan: Boolean) {
        viewModelScope.launch {
            if (inPlan) {
                routineRepository.addExercise(planId, exerciseId)
            } else {
                routineRepository.removeExerciseFromRoutine(planId, exerciseId)
            }
        }
    }

    fun createPlanWithExercise(name: String) {
        viewModelScope.launch { routineRepository.addExercise(routineRepository.createRoutine(name), exerciseId) }
    }

    fun consumeCelebration() {
        celebration = null
    }

    private fun celebrate(messages: List<String>) {
        if (messages.isEmpty()) return
        celebration = (listOfNotNull(celebration) + messages).joinToString("\n")
    }

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
                gameRepository = container.gameRepository,
                routineRepository = container.routineRepository,
            )
        }
    }
}
