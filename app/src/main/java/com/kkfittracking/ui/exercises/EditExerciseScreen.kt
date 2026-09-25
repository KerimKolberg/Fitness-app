@file:OptIn(ExperimentalMaterial3Api::class)

package com.kkfittracking.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkfittracking.model.ExerciseType
import com.kkfittracking.ui.components.ColorDot

@Composable
fun EditExerciseScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: EditExerciseViewModel = viewModel(factory = EditExerciseViewModel.Factory),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.result) {
        when (viewModel.result) {
            EditExerciseResult.SAVED -> onBack()
            EditExerciseResult.DELETED -> onDeleted()
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "New exercise" else "Edit exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete exercise")
                        }
                    }
                    IconButton(onClick = viewModel::save) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = viewModel::updateName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                isError = viewModel.nameError != null,
                supportingText = viewModel.nameError?.let { error -> { Text(error) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )

            val selectedCategory = categories.firstOrNull { it.id == viewModel.categoryId }
            var categoryMenuOpen by remember { mutableStateOf(false) }
            Box {
                OutlinedTextField(
                    value = selectedCategory?.name.orEmpty(),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = selectedCategory?.let { category -> { ColorDot(category.color) } },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                )
                // A read-only text field swallows clicks, so an invisible layer on top opens the menu.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { categoryMenuOpen = true },
                )
                DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            leadingIcon = { ColorDot(category.color) },
                            onClick = {
                                viewModel.updateCategory(category.id)
                                categoryMenuOpen = false
                            },
                        )
                    }
                }
            }

            Column {
                Text("Type", style = MaterialTheme.typography.titleSmall)
                ExerciseType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = viewModel.type == type,
                                onClick = { viewModel.updateType(type) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = viewModel.type == type, onClick = null)
                        Text(type.label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }

            OutlinedTextField(
                value = viewModel.tempo,
                onValueChange = viewModel::updateTempo,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tempo (optional)") },
                supportingText = { Text("Seconds down-pause-up-pause, e.g. 5-0-1-0 for slow eccentrics") },
                singleLine = true,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = viewModel.perSide,
                        onValueChange = viewModel::updatePerSide,
                        role = Role.Checkbox,
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = viewModel.perSide, onCheckedChange = null)
                Text("Each side separately (left and right)", modifier = Modifier.padding(start = 12.dp))
            }

            OutlinedTextField(
                value = viewModel.notes,
                onValueChange = viewModel::updateNotes,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes (optional)") },
                minLines = 3,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete exercise?") },
            text = { Text("It will be removed from the exercise list. Workouts you already logged keep it.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}
