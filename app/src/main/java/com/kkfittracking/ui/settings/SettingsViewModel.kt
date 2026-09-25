package com.kkfittracking.ui.settings

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkfittracking.data.SettingsRepository
import com.kkfittracking.data.backup.BackupException
import com.kkfittracking.data.backup.BackupFile
import com.kkfittracking.data.backup.DataTransfer
import com.kkfittracking.model.Settings
import com.kkfittracking.model.ThemeMode
import com.kkfittracking.model.UnitSystem
import com.kkfittracking.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val dataTransfer: DataTransfer,
) : ViewModel() {
    val lastBackupAt: StateFlow<Long?> =
        repository.lastBackupAt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** A short result message, such as "Backup saved". */
    var message by mutableStateOf<String?>(null)
        private set

    /** A backup that was read and is waiting for the user to confirm the restore. */
    var pendingRestore by mutableStateOf<BackupFile?>(null)
        private set

    var isBusy by mutableStateOf(false)
        private set

    fun backUp(uri: Uri) = perform("Backup saved") { dataTransfer.writeBackup(uri) }

    fun exportWorkouts(uri: Uri) = perform("Workouts exported") { dataTransfer.exportWorkouts(uri) }

    fun exportBodyMeasurements(uri: Uri) = perform("Body measurements exported") { dataTransfer.exportBodyMeasurements(uri) }

    /** Reads the file and asks for confirmation before replacing anything. */
    fun openBackup(uri: Uri) = perform(null) { pendingRestore = dataTransfer.readBackup(uri) }

    fun confirmRestore() {
        val file = pendingRestore ?: return
        pendingRestore = null
        perform("Backup restored") { dataTransfer.restore(file) }
    }

    fun cancelRestore() {
        pendingRestore = null
    }

    fun consumeMessage() {
        message = null
    }

    private fun perform(successMessage: String?, action: suspend () -> Unit) {
        viewModelScope.launch {
            isBusy = true
            message = try {
                action()
                successMessage
            } catch (e: BackupException) {
                e.message
            } catch (e: IOException) {
                "Could not use that file: ${e.message}"
            } catch (e: SecurityException) {
                "The app is not allowed to use that file."
            } finally {
                isBusy = false
            }
        }
    }

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

    fun setDropSetsEnabled(value: Boolean) {
        viewModelScope.launch { repository.setDropSetsEnabled(value) }
    }

    fun changeDropSetPercent(delta: Int) {
        val current = settings.value ?: return
        viewModelScope.launch { repository.setDropSetPercent(current.dropSetPercent + delta) }
    }

    fun changeSupersetTransition(delta: Int) {
        val current = settings.value ?: return
        viewModelScope.launch { repository.setSupersetTransitionSeconds(current.supersetTransitionSeconds + delta) }
    }

    fun setSupersetAutoAdvance(value: Boolean) {
        viewModelScope.launch { repository.setSupersetAutoAdvance(value) }
    }

    fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(value) }
    }

    companion object {
        val Factory = appViewModelFactory { container ->
            SettingsViewModel(container.settingsRepository, container.dataTransfer)
        }
    }
}
