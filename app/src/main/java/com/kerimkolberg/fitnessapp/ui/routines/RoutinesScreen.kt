@file:OptIn(ExperimentalMaterial3Api::class)

package com.kerimkolberg.fitnessapp.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kerimkolberg.fitnessapp.ui.components.TextInputDialog

@Composable
fun RoutinesScreen(
    onBack: () -> Unit,
    onOpenRoutine: (String) -> Unit,
    viewModel: RoutinesViewModel = viewModel(factory = RoutinesViewModel.Factory),
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routines") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New routine") },
            )
        },
    ) { padding ->
        val list = routines ?: return@Scaffold
        if (list.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No routines yet", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "A routine is a list of exercises you do together, like \"Push day\". " +
                        "Add one to a workout day to fill it in quickly.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(list, key = { it.id }) { routine ->
                    ListItem(
                        modifier = Modifier.clickable { onOpenRoutine(routine.id) },
                        headlineContent = { Text(routine.name) },
                        supportingContent = {
                            Text(
                                text = when (routine.exercises.size) {
                                    0 -> "No exercises yet"
                                    else -> routine.exercises.joinToString(", ") { it.exerciseName }
                                },
                                maxLines = 2,
                            )
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (creating) {
        TextInputDialog(
            title = "New routine",
            label = "Name, e.g. Push day",
            confirmText = "Create",
            onConfirm = { name ->
                creating = false
                viewModel.createRoutine(name, onOpenRoutine)
            },
            onDismiss = { creating = false },
        )
    }
}
