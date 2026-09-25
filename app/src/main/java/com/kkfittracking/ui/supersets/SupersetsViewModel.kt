package com.kkfittracking.ui.supersets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kkfittracking.data.ExerciseRepository
import com.kkfittracking.data.RoutineRepository
import com.kkfittracking.data.SettingsRepository
import com.kkfittracking.data.WorkoutRepository
import com.kkfittracking.model.ArrangeEntry
import com.kkfittracking.model.ArrangeRow
import com.kkfittracking.model.SupersetTiming
import com.kkfittracking.model.addSuperset
import com.kkfittracking.model.arrangedExercises
import com.kkfittracking.model.arrangementOf
import com.kkfittracking.model.moveRow
import com.kkfittracking.model.moveToNewSuperset
import com.kkfittracking.model.moveToSuperset
import com.kkfittracking.model.oversizedSupersets
import com.kkfittracking.model.removeSuperset
import com.kkfittracking.model.updateItem
import com.kkfittracking.model.updateSuperset
import com.kkfittracking.ui.SupersetsRoute
import com.kkfittracking.ui.appViewModelFactory
import com.kkfittracking.ui.components.formatShortDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/**
 * Groups a day's or a plan's exercises into supersets and sets their timing. Changes stay in
 * memory until [save].
 */
class SupersetsViewModel(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<SupersetsRoute>()

    /** Set when arranging a plan; otherwise the day of [route] is arranged. */
    private val planId: String? = route.planId

    val isPlan: Boolean get() = planId != null

    /** The plan's name or the day, for the title. */
    var subject by mutableStateOf("")
        private set
    var rows by mutableStateOf<List<ArrangeRow>>(emptyList())
        private set
    var isLoaded by mutableStateOf(false)
        private set
    var isSaved by mutableStateOf(false)
        private set

    private var defaults = SupersetTiming(transitionSeconds = 15, roundRestSeconds = 90)

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            defaults = SupersetTiming(settings.supersetTransitionSeconds, settings.restTimerSeconds)
            // An exercise's own rest is replaced by the superset's timing, so it is pointed out.
            val ownRest = exerciseRepository.exercises.first().associate { it.id to it.plan.restSeconds }
            val entries = if (planId != null) {
                val plan = routineRepository.observeRoutine(planId).first()
                subject = plan?.name.orEmpty()
                plan?.exercises.orEmpty().map {
                    ArrangeEntry(
                        id = it.id,
                        exerciseName = it.exerciseName,
                        categoryColor = it.categoryColor,
                        supersetId = it.supersetId,
                        transitionSeconds = it.transitionSeconds,
                        roundRestSeconds = it.roundRestSeconds,
                        supersetRounds = it.supersetRounds,
                        supersetDropLast = it.supersetDropLast,
                        memberRounds = it.memberRounds,
                        memberDropSet = it.memberDropSet,
                        ownRestSeconds = ownRest[it.exerciseId],
                    )
                }
            } else {
                val date = LocalDate.ofEpochDay(route.epochDay)
                subject = formatShortDate(date)
                workoutRepository.observeDay(date).first().map {
                    ArrangeEntry(
                        id = it.workoutExerciseId,
                        exerciseName = it.exerciseName,
                        categoryColor = it.categoryColor,
                        supersetId = it.supersetId,
                        transitionSeconds = it.transitionSeconds,
                        roundRestSeconds = it.roundRestSeconds,
                        supersetRounds = it.supersetRounds,
                        supersetDropLast = it.supersetDropLast,
                        memberRounds = it.memberRounds,
                        memberDropSet = it.memberDropSet,
                        detail = it.sets.size.takeIf { count -> count > 0 }?.let { count ->
                            if (count == 1) "1 set logged" else "$count sets logged"
                        },
                        ownRestSeconds = ownRest[it.exerciseId],
                    )
                }
            }
            var loaded = arrangementOf(entries, defaults)
            // Start with an empty superset to drag exercises into.
            if (loaded.none { it is ArrangeRow.Start }) loaded = addSuperset(loaded, newStart())
            rows = loaded
            isLoaded = true
        }
    }

    /** Supersets with too many exercises; saving is blocked while there are any. */
    val oversized: List<String> get() = oversizedSupersets(rows)

    fun move(from: Int, to: Int) {
        rows = moveRow(rows, from, to)
    }

    /** Moves an exercise to the end of a superset, or out of its superset when [supersetId] is null. */
    fun moveTo(itemKey: String, supersetId: String?) {
        rows = moveToSuperset(rows, itemKey, supersetId)
    }

    /** Starts a new superset with this exercise, where it is now. */
    fun startSupersetWith(itemKey: String) {
        rows = moveToNewSuperset(rows, itemKey, newStart())
    }

    fun addEmptySuperset() {
        rows = addSuperset(rows, newStart())
    }

    /** Ends a superset; its exercises stay where they are, on their own. */
    fun ungroup(supersetId: String) {
        rows = removeSuperset(rows, supersetId)
    }

    fun changeTransition(supersetId: String, delta: Int) {
        rows = updateSuperset(rows, supersetId) {
            it.copy(transitionSeconds = (it.transitionSeconds + delta).coerceIn(0, SettingsRepository.MAX_TRANSITION_SECONDS))
        }
    }

    fun changeRoundRest(supersetId: String, delta: Int) {
        rows = updateSuperset(rows, supersetId) {
            it.copy(
                roundRestSeconds = (it.roundRestSeconds + delta)
                    .coerceIn(SettingsRepository.MIN_REST_SECONDS, SettingsRepository.MAX_REST_SECONDS),
            )
        }
    }

    /** Rounds of the superset: off (keep going) or 1 to [MAX_ROUNDS]. */
    fun changeRounds(supersetId: String, delta: Int) {
        rows = updateSuperset(rows, supersetId) {
            val next = (it.rounds ?: 0) + delta
            it.copy(rounds = next.takeIf { rounds -> rounds > 0 }?.coerceAtMost(MAX_ROUNDS))
        }
    }

    fun setDropOnLastRound(supersetId: String, value: Boolean) {
        rows = updateSuperset(rows, supersetId) { it.copy(dropOnLastRound = value) }
    }

    /** The rounds one exercise joins (the last ones): all of them, or fewer. */
    fun changeMemberRounds(itemKey: String, delta: Int, supersetRounds: Int) {
        rows = updateItem(rows, itemKey) {
            val next = (it.memberRounds ?: supersetRounds) + delta
            it.copy(memberRounds = next.coerceIn(1, supersetRounds).takeIf { rounds -> rounds < supersetRounds })
        }
    }

    /** One exercise's drop set on its last round: yes, no, or as the superset says (null). */
    fun setMemberDropSet(itemKey: String, value: Boolean?) {
        rows = updateItem(rows, itemKey) { it.copy(memberDropSet = value) }
    }

    fun save() {
        if (oversized.isNotEmpty()) return
        viewModelScope.launch {
            val arranged = arrangedExercises(rows)
            if (planId != null) routineRepository.arrangePlan(arranged) else workoutRepository.arrangeDay(arranged)
            isSaved = true
        }
    }

    private fun newStart() = ArrangeRow.Start(UUID.randomUUID().toString(), defaults.transitionSeconds, defaults.roundRestSeconds)

    companion object {
        const val MAX_ROUNDS = 10

        val Factory = appViewModelFactory { container ->
            SupersetsViewModel(
                savedStateHandle = createSavedStateHandle(),
                workoutRepository = container.workoutRepository,
                routineRepository = container.routineRepository,
                exerciseRepository = container.exerciseRepository,
                settingsRepository = container.settingsRepository,
            )
        }
    }
}
