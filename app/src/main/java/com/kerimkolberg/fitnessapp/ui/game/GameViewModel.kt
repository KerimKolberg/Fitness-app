package com.kerimkolberg.fitnessapp.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerimkolberg.fitnessapp.data.GameRepository
import com.kerimkolberg.fitnessapp.data.SettingsRepository
import com.kerimkolberg.fitnessapp.model.GameStats
import com.kerimkolberg.fitnessapp.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(
    gameRepository: GameRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    /** Null while loading. */
    val stats: StateFlow<GameStats?> =
        gameRepository.stats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun changeWeeklyGoal(delta: Int) {
        val current = stats.value?.weeklyGoal ?: return
        viewModelScope.launch { settingsRepository.setWeeklyGoal(current + delta) }
    }

    companion object {
        val Factory = appViewModelFactory { container ->
            GameViewModel(container.gameRepository, container.settingsRepository)
        }
    }
}
