@file:OptIn(ExperimentalMaterial3Api::class)

package com.kerimkolberg.fitnessapp.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kerimkolberg.fitnessapp.BuildConfig
import com.kerimkolberg.fitnessapp.model.ThemeMode
import com.kerimkolberg.fitnessapp.model.UnitSystem
import com.kerimkolberg.fitnessapp.model.formatDuration

private const val REST_STEP_SECONDS = 15

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val current = settings ?: return@Scaffold
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Units")
            UnitSystem.entries.forEach { unitSystem ->
                RadioRow(
                    label = unitSystem.label,
                    selected = current.unitSystem == unitSystem,
                    onClick = { viewModel.setUnitSystem(unitSystem) },
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionTitle("Rest timer")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Default duration", modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = { viewModel.changeRestTimer(-REST_STEP_SECONDS) }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = formatDuration(current.restTimerSeconds),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                FilledTonalIconButton(onClick = { viewModel.changeRestTimer(REST_STEP_SECONDS) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = current.autoStartRestTimer,
                        onValueChange = viewModel::setAutoStartRestTimer,
                        role = Role.Switch,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Start automatically")
                    Text(
                        text = "Start the timer each time you save a set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = current.autoStartRestTimer, onCheckedChange = null)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionTitle("Theme")
            ThemeMode.entries.forEach { mode ->
                RadioRow(
                    label = mode.label,
                    selected = current.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 16.dp))
    }
}
