package com.kkfittracking.ui.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kkfittracking.data.WorkoutRepository
import com.kkfittracking.ui.CalendarRoute
import com.kkfittracking.ui.appViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth,
    val selectedDate: LocalDate,
    val workoutDates: Set<LocalDate> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    savedStateHandle: SavedStateHandle,
    workoutRepository: WorkoutRepository,
) : ViewModel() {
    private val selectedDate = LocalDate.ofEpochDay(savedStateHandle.toRoute<CalendarRoute>().epochDay)
    private val month = MutableStateFlow(YearMonth.from(selectedDate))

    val uiState: StateFlow<CalendarUiState> = month
        .flatMapLatest { month ->
            workoutRepository.observeWorkoutDates(month.atDay(1), month.atEndOfMonth())
                .map { dates -> CalendarUiState(month = month, selectedDate = selectedDate, workoutDates = dates) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState(month = month.value, selectedDate = selectedDate),
        )

    fun showPreviousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun showNextMonth() {
        month.value = month.value.plusMonths(1)
    }

    companion object {
        val Factory = appViewModelFactory { container ->
            CalendarViewModel(createSavedStateHandle(), container.workoutRepository)
        }
    }
}
