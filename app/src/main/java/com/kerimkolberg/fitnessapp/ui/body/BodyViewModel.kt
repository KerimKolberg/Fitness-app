package com.kerimkolberg.fitnessapp.ui.body

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kerimkolberg.fitnessapp.data.BodyRepository
import com.kerimkolberg.fitnessapp.data.SettingsRepository
import com.kerimkolberg.fitnessapp.model.BodyMeasurement
import com.kerimkolberg.fitnessapp.model.BodyMetric
import com.kerimkolberg.fitnessapp.model.UnitSystem
import com.kerimkolberg.fitnessapp.model.parseDecimal
import com.kerimkolberg.fitnessapp.ui.BodyMetricRoute
import com.kerimkolberg.fitnessapp.ui.appViewModelFactory
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
