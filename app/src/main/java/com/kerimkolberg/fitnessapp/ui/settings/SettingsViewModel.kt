package com.kerimkolberg.fitnessapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerimkolberg.fitnessapp.data.SettingsRepository
import com.kerimkolberg.fitnessapp.model.Settings
import com.kerimkolberg.fitnessapp.model.ThemeMode
import com.kerimkolberg.fitnessapp.model.UnitSystem
import com.kerimkolberg.fitnessapp.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    /** Null until the stored settings are loaded, so the screen never flashes the defaults. */
    val settings: StateFlow<Settings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setUnitSystem(value: UnitSystem) {
        viewModelScope.launch { repository.setUnitSystem(value) }
    }

    fun changeRestTimer(deltaSeconds: Int) {
        val current = settings.value ?: return
        viewModelScope.launch { repository.setRestTimerSeconds(current.restTimerSeconds + deltaSeconds) }
    }

    fun setAutoStartRestTimer(value: Boolean) {
        viewModelScope.launch { repository.setAutoStartRestTimer(value) }
    }

    fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(value) }
    }

    companion object {
        val Factory = appViewModelFactory { container -> SettingsViewModel(container.settingsRepository) }
    }
}
