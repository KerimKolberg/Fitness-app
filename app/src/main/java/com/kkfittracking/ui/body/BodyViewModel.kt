package com.kkfittracking.ui.body

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kkfittracking.data.BodyRepository
import com.kkfittracking.data.SettingsRepository
import com.kkfittracking.model.BodyMeasurement
import com.kkfittracking.model.BodyMetric
import com.kkfittracking.model.UnitSystem
import com.kkfittracking.model.parseDecimal
import com.kkfittracking.ui.BodyMetricRoute
import com.kkfittracking.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BodyUiState(
    /** Every measurement, newest first. */
    val measurements: List<BodyMeasurement> = emptyList(),
    val units: UnitSystem = UnitSystem.METRIC,
) {
    fun latest(metric: BodyMetric): BodyMeasurement? = measurements.firstOrNull { it.metric == metric }

    fun entries(metric: BodyMetric): List<BodyMeasurement> = measurements.filter { it.metric == metric }
}

private fun bodyState(bodyRepository: BodyRepository, settingsRepository: SettingsRepository) =
    combine(bodyRepository.measurements, settingsRepository.settings) { measurements, settings ->
        BodyUiState(measurements, settings.unitSystem)
    }

class BodyViewModel(bodyRepository: BodyRepository, settingsRepository: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<BodyUiState> = bodyState(bodyRepository, settingsRepository)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyUiState())

    companion object {
        val Factory = appViewModelFactory { container ->
            BodyViewModel(container.bodyRepository, container.settingsRepository)
        }
    }
}

class BodyMetricViewModel(
    savedStateHandle: SavedStateHandle,
    private val bodyRepository: BodyRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val metric: BodyMetric = BodyMetric.valueOf(savedStateHandle.toRoute<BodyMetricRoute>().metric)

    val uiState: StateFlow<BodyUiState> = bodyState(bodyRepository, settingsRepository)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyUiState())

    var date by mutableStateOf(LocalDate.now())
        private set
    var valueText by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun updateDate(value: LocalDate) {
        date = value
    }

    fun updateValue(value: String) {
        valueText = value
        error = null
    }

    fun save() {
        val value = parseDecimal(valueText)
        if (value == null || value <= 0) {
            error = "Enter a value"
            return
        }
        val stored = metric.fromDisplay(value, uiState.value.units)
        val day = date
        valueText = ""
        viewModelScope.launch { bodyRepository.saveMeasurement(metric, day, stored) }
    }

    fun delete(id: String) {
        viewModelScope.launch { bodyRepository.deleteMeasurement(id) }
    }

    companion object {
        val Factory = appViewModelFactory { container ->
            BodyMetricViewModel(createSavedStateHandle(), container.bodyRepository, container.settingsRepository)
        }
    }
}
