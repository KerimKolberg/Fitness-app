@file:OptIn(ExperimentalMaterial3Api::class)

package com.kerimkolberg.fitnessapp.ui.arrange

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kerimkolberg.fitnessapp.model.ArrangeRow
import com.kerimkolberg.fitnessapp.model.DropSetMode
import com.kerimkolberg.fitnessapp.model.MAX_SUPERSET_SIZE
import com.kerimkolberg.fitnessapp.model.membersOf
import com.kerimkolberg.fitnessapp.ui.components.ColorDot
import com.kerimkolberg.fitnessapp.ui.components.formatShortDate

private val RowSpacing = 8.dp

/**
 * Arrange a day: drag exercises (by their ≡ handle) under a superset header to superset them,
 * set the time to walk between superset exercises, and plan drop sets per exercise.
 */
@Composable
fun ArrangeDayScreen(
    onDone: () -> Unit,
    viewModel: ArrangeDayViewModel = viewModel(factory = ArrangeDayViewModel.Factory),
) {
    var editingKey by remember { mutableStateOf<String?>(null) }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<String, Int>() }
    val spacingPx = with(LocalDensity.current) { RowSpacing.toPx() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arrange ${formatShortDate(viewModel.date)}") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back without saving")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = viewModel.isLoaded && viewModel.oversized.isEmpty()) {
                        Text("Save")
                    }
                },
            )
        },
    ) { padding ->
        if (!viewModel.isLoaded) return@Scaffold
        val rows = viewModel.rows
        var supersetNumber = 0
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(RowSpacing),
        ) {
            Text(
                text = "Drag ≡ to move an exercise under a superset header. Tap an exercise to plan drop sets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            rows.forEach { row ->
                key(row.key) {
                    val isDragging = row.key == dragKey
                    Box(
                        modifier = Modifier
                            .onSizeChanged { heights[row.key] = it.height }
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffset else 0f
                                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                            },
                    ) {
                        when (row) {
                            is ArrangeRow.Section -> if (row.supersetId == null) {
                                SeparateHeader()
                            } else {
                                supersetNumber++
                                SupersetHeader(
                                    number = supersetNumber,
                                    section = row,
                                    memberCount = membersOf(rows, row.key).size,
                                    onChangeTransition = { viewModel.changeTransition(row.key, it) },
                                    onRemove = { viewModel.removeSuperset(row.key) },
                                )
                            }
                            is ArrangeRow.Item -> ItemRow(
                                item = row,
                                onEdit = { editingKey = row.key },
                                dragHandle = Modifier.pointerInput(row.key) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragKey = row.key
                                            dragOffset = 0f
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragEnd = {
                                            dragKey = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            dragKey = null
                                            dragOffset = 0f
                                        },
                                    ) { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        // Swap with a neighbor once the row is dragged past half of it.
                                        val current = viewModel.rows
                                        val index = current.indexOfFirst { it.key == row.key }
                                        if (dragOffset > 0 && index in 0 until current.lastIndex) {
                                            val next = (heights[current[index + 1].key] ?: 0) + spacingPx
                                            if (dragOffset > next / 2) {
                                                viewModel.move(index, index + 1)
                                                dragOffset -= next
                                            }
                                        } else if (dragOffset < 0 && index > 1) {
                                            val previous = (heights[current[index - 1].key] ?: 0) + spacingPx
                                            if (-dragOffset > previous / 2) {
                                                viewModel.move(index, index - 1)
                                                dragOffset += previous
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
            OutlinedButton(onClick = viewModel::addSuperset, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add superset", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    val editing = viewModel.rows.firstOrNull { it.key == editingKey } as? ArrangeRow.Item
    if (editing != null) {
        ItemDialog(
            item = editing,
            rows = viewModel.rows,
            onDropSetMode = { viewModel.setDropSetMode(editing.key, it) },
            onChangePlannedSets = { viewModel.changePlannedSets(editing.key, it) },
            onMoveTo = { viewModel.moveTo(editing.key, it) },
            onMoveToNewSuperset = { viewModel.moveToNewSuperset(editing.key) },
            onDismiss = { editingKey = null },
        )
    }
}

@Composable
private fun SeparateHeader() {
    Column(Modifier.padding(top = 8.dp)) {
        Text("Exercises on their own", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SupersetHeader(
    number: Int,
    section: ArrangeRow.Section,
    memberCount: Int,
    onChangeTransition: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Superset $number · $memberCount exercises",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove superset $number")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Time to the next exercise", modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = { onChangeTransition(-5) }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "${section.transitionSeconds} s",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                FilledTonalIconButton(onClick = { onChangeTransition(5) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
            when {
                memberCount > MAX_SUPERSET_SIZE -> Text(
                    text = "A superset has at most $MAX_SUPERSET_SIZE exercises. Move some out to save.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                memberCount < 2 -> Text(
                    text = "Drag at least 2 exercises here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ItemRow(item: ArrangeRow.Item, onEdit: () -> Unit, dragHandle: Modifier) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(dragHandle.padding(12.dp)) {
                Icon(Icons.Default.Menu, contentDescription = "Drag to move ${item.exerciseName}")
            }
            ColorDot(item.categoryColor)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(item.exerciseName, style = MaterialTheme.typography.bodyLarge)
                val details = listOfNotNull(
                    "${item.setCount} sets logged".takeIf { item.setCount > 0 },
                    dropSetSummary(item),
                )
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options for ${item.exerciseName}")
            }
        }
    }
}

private fun dropSetSummary(item: ArrangeRow.Item): String? = when {
    !item.supportsDropSets -> null
    item.dropSetMode == DropSetMode.LAST_SET ->
        "Drop set after ${item.plannedSets ?: ArrangeRow.DEFAULT_PLANNED_SETS} sets"
    item.dropSetMode == DropSetMode.EVERY_SET -> "Every set a drop set"
    else -> null
}

@Composable
private fun ItemDialog(
    item: ArrangeRow.Item,
    rows: List<ArrangeRow>,
    onDropSetMode: (DropSetMode) -> Unit,
    onChangePlannedSets: (Int) -> Unit,
    onMoveTo: (String) -> Unit,
    onMoveToNewSuperset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sections = rows.filterIsInstance<ArrangeRow.Section>()
    val currentSection = rows.take(rows.indexOf(item)).lastOrNull { it is ArrangeRow.Section }?.key
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.exerciseName) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Drop sets", style = MaterialTheme.typography.titleSmall)
                if (item.supportsDropSets) {
                    DropSetMode.entries.forEach { mode ->
                        RadioLine(mode.label, selected = item.dropSetMode == mode) { onDropSetMode(mode) }
                    }
                    if (item.dropSetMode == DropSetMode.LAST_SET) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Normal sets first", modifier = Modifier.weight(1f))
                            FilledTonalIconButton(onClick = { onChangePlannedSets(-1) }) { Text("−") }
                            Text(
                                text = "${item.plannedSets ?: ArrangeRow.DEFAULT_PLANNED_SETS}",
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            FilledTonalIconButton(onClick = { onChangePlannedSets(1) }) { Text("+") }
                        }
                    }
                } else {
                    Text(
                        text = "Drop sets are for weight and reps exercises.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text("Move to", style = MaterialTheme.typography.titleSmall)
                var number = 0
                sections.forEach { section ->
                    val label = if (section.supersetId == null) "On its own" else "Superset ${++number}"
                    RadioLine(label, selected = section.key == currentSection) { onMoveTo(section.key) }
                }
                TextButton(onClick = onMoveToNewSuperset) { Text("+ New superset") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun RadioLine(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 12.dp))
    }
}
