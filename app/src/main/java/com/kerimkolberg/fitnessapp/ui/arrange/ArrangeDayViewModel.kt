package com.kerimkolberg.fitnessapp.ui.arrange

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kerimkolberg.fitnessapp.data.SettingsRepository
import com.kerimkolberg.fitnessapp.data.WorkoutRepository
import com.kerimkolberg.fitnessapp.model.ArrangeRow
import com.kerimkolberg.fitnessapp.model.DropSetMode
import com.kerimkolberg.fitnessapp.model.addSection
import com.kerimkolberg.fitnessapp.model.arrangedExercises
import com.kerimkolberg.fitnessapp.model.arrangementOf
import com.kerimkolberg.fitnessapp.model.moveRow
import com.kerimkolberg.fitnessapp.model.moveToSection
import com.kerimkolberg.fitnessapp.model.oversizedSections
import com.kerimkolberg.fitnessapp.model.removeSection
import com.kerimkolberg.fitnessapp.model.updateRow
import com.kerimkolberg.fitnessapp.ui.ArrangeDayRoute
import com.kerimkolberg.fitnessapp.ui.appViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Edits a day's supersets and drop set plans in memory; nothing is saved until [save]. */
class ArrangeDayViewModel(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val date: LocalDate = LocalDate.ofEpochDay(savedStateHandle.toRoute<ArrangeDayRoute>().epochDay)

    var rows by mutableStateOf<List<ArrangeRow>>(emptyList())
        private set
    var isLoaded by mutableStateOf(false)
        private set
    var isSaved by mutableStateOf(false)
        private set

    private var defaultTransition = 15

    init {
        viewModelScope.launch {
            defaultTransition = settingsRepository.settings.first().supersetTransitionSeconds
            var loaded = arrangementOf(workoutRepository.observeDay(date).first(), defaultTransition)
            // Start with an empty superset to drag exercises into.
            if (loaded.none { it is ArrangeRow.Section && it.supersetId != null }) {
                loaded = addSection(loaded, newId(), defaultTransition)
            }
            rows = loaded
            isLoaded = true
        }
    }

    /** Supersets with too many exercises; saving is blocked while there are any. */
    val oversized: List<String> get() = oversizedSections(rows)

    fun indexOf(key: String): Int = rows.indexOfFirst { it.key == key }

    fun move(from: Int, to: Int) {
        rows = moveRow(rows, from, to)
    }

    fun moveTo(itemKey: String, sectionKey: String) {
        rows = moveToSection(rows, itemKey, sectionKey)
    }

    /** Moves an exercise into a new superset of its own, ready for more to be added. */
    fun moveToNewSuperset(itemKey: String) {
        val id = newId()
        rows = moveToSection(addSection(rows, id, defaultTransition), itemKey, ArrangeRow.Section(id, 0).key)
    }

    fun addSuperset() {
        rows = addSection(rows, newId(), defaultTransition)
    }

    fun removeSuperset(sectionKey: String) {
        rows = removeSection(rows, sectionKey)
    }

    fun changeTransition(sectionKey: String, delta: Int) {
        rows = updateRow(rows, sectionKey) {
            val section = it as ArrangeRow.Section
            section.copy(transitionSeconds = (section.transitionSeconds + delta).coerceIn(0, SettingsRepository.MAX_TRANSITION_SECONDS))
        }
    }

    fun setDropSetMode(itemKey: String, mode: DropSetMode) {
        rows = updateRow(rows, itemKey) {
            val item = it as ArrangeRow.Item
            item.copy(
                dropSetMode = mode,
                plannedSets = if (mode == DropSetMode.LAST_SET) item.plannedSets ?: ArrangeRow.DEFAULT_PLANNED_SETS else item.plannedSets,
            )
        }
    }

    fun changePlannedSets(itemKey: String, delta: Int) {
        rows = updateRow(rows, itemKey) {
            val item = it as ArrangeRow.Item
            item.copy(plannedSets = ((item.plannedSets ?: ArrangeRow.DEFAULT_PLANNED_SETS) + delta).coerceIn(1, 10))
        }
    }

    fun save() {
        if (oversized.isNotEmpty()) return
        viewModelScope.launch {
            workoutRepository.arrangeDay(arrangedExercises(rows))
            isSaved = true
        }
    }

    private fun newId() = UUID.randomUUID().toString()

    companion object {
        val Factory = appViewModelFactory { container ->
            ArrangeDayViewModel(createSavedStateHandle(), container.workoutRepository, container.settingsRepository)
        }
    }
}
