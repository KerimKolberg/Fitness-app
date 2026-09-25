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
import com.kkfittracking.model.Muscle
import com.kkfittracking.model.TrainingStyle
import com.kkfittracking.ui.components.AddLinkDialog
import com.kkfittracking.ui.components.ColorDot
import com.kkfittracking.ui.components.LinkRow

@Composable
fun EditExerciseScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: EditExerciseViewModel = viewModel(factory = EditExerciseViewModel.Factory),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var addingLink by remember { mutableStateOf(false) }
    var choosingMuscles by remember { mutableStateOf(false) }

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
            DropdownField(
                label = "Section",
                value = selectedCategory?.name.orEmpty(),
                options = categories,
                optionLabel = { it.name },
                onSelect = { viewModel.updateCategory(it.id) },
                leadingIcon = selectedCategory?.let { category -> { ColorDot(category.color) } },
                optionIcon = { ColorDot(it.color) },
            )
            DropdownField(
                label = "Main muscle",
                value = viewModel.muscle.label,
                options = Muscle.forRegion(viewModel.regionKey),
                optionLabel = { it.label },
                onSelect = viewModel::updateMuscle,
            )
            Box {
                OutlinedTextField(
                    value = viewModel.otherMuscles.joinToString(", ") { it.label }.ifEmpty { "Nothing else" },
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("Also trains") },
                    supportingText = { Text("It is listed under each of these muscles too") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { choosingMuscles = true },
                )
            }
            DropdownField(
                label = "Training style",
                value = viewModel.style.label,
                options = TrainingStyle.entries,
                optionLabel = { it.label },
                onSelect = viewModel::updateStyle,
            )

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
                label = { Text("How to do it (optional)") },
                minLines = 3,
            )

            Column {
                Text("Videos & links", style = MaterialTheme.typography.titleSmall)
                viewModel.links.forEach { link ->
                    LinkRow(link, onRemove = { viewModel.removeLink(link) })
                }
                TextButton(onClick = { addingLink = true }) { Text("+ Add a video or link") }
            }
        }
    }

    if (choosingMuscles) {
        AlertDialog(
            onDismissRequest = { choosingMuscles = false },
            title = { Text("Also trains") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    categories.filter { it.key != null }.forEach { section ->
                        val muscles = Muscle.forRegion(section.key).filter { it != Muscle.OTHER && it != viewModel.muscle }
                        if (muscles.isEmpty()) return@forEach
                        Text(section.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                        muscles.forEach { muscle ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = muscle in viewModel.otherMuscles,
                                        onValueChange = { viewModel.toggleOtherMuscle(muscle) },
                                        role = Role.Checkbox,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = muscle in viewModel.otherMuscles, onCheckedChange = null)
                                Text(muscle.label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingMuscles = false }) { Text("Done") } },
        )
    }

    if (addingLink) {
        AddLinkDialog(
            onAdd = {
                addingLink = false
                viewModel.addLink(it)
            },
            onDismiss = { addingLink = false },
        )
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

/** A read-only field that opens a menu of [options]. */
@Composable
private fun <T> DropdownField(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    optionIcon: (@Composable (T) -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            leadingIcon = leadingIcon,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
        )
        // A read-only text field swallows clicks, so an invisible layer on top opens the menu.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    leadingIcon = optionIcon?.let { icon -> { icon(option) } },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
    }
}
