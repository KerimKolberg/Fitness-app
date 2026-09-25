@file:OptIn(ExperimentalMaterial3Api::class)

package com.kkfittracking.ui.supersets

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kkfittracking.model.ArrangeRow
import com.kkfittracking.model.MAX_SUPERSET_SIZE
import com.kkfittracking.model.formatDuration
import com.kkfittracking.model.membersOf
import com.kkfittracking.model.supersetMembership
import com.kkfittracking.ui.components.ColorDot

private val RowSpacing = 8.dp

/**
 * "+Super-sets" for a day or a plan: drag exercises (by their ≡ handle) between a superset's
 * header and its end line to superset them, set the time to walk from one exercise to the next
 * and the rest after each round. Exercises outside a superset are done on their own.
 */
@Composable
fun SupersetsScreen(
    onDone: () -> Unit,
    viewModel: SupersetsViewModel = viewModel(factory = SupersetsViewModel.Factory),
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
                title = {
                    Column {
                        Text("+Super-sets")
                        Text(
                            text = viewModel.subject,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
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
        val membership = supersetMembership(rows)
        val numbers = rows.filterIsInstance<ArrangeRow.Start>().mapIndexed { index, start -> start.supersetId to index + 1 }.toMap()
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(RowSpacing),
        ) {
            Text(
                text = "Drag ≡ to move an exercise between a superset's header and its end line. " +
                    if (viewModel.isPlan) {
                        "When you add this plan to a day, you can take the supersets along or leave them out."
                    } else {
                        "Exercises outside a superset are done on their own."
                    },
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
                            is ArrangeRow.Start -> SupersetHeader(
                                number = numbers[row.supersetId] ?: 0,
                                start = row,
                                memberCount = membersOf(rows, row.supersetId).size,
                                onChangeTransition = { viewModel.changeTransition(row.supersetId, it) },
                                onChangeRoundRest = { viewModel.changeRoundRest(row.supersetId, it) },
                                onRemove = { viewModel.ungroup(row.supersetId) },
                            )
                            is ArrangeRow.End -> SupersetEnd(number = numbers[row.supersetId] ?: 0)
                            is ArrangeRow.Item -> ItemRow(
                                item = row,
                                inSuperset = row.key in membership,
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
                                        } else if (dragOffset < 0 && index > 0) {
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
            OutlinedButton(onClick = viewModel::addEmptySuperset, modifier = Modifier.fillMaxWidth()) {
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
            onMoveTo = { viewModel.moveTo(editing.key, it) },
            onNewSuperset = { viewModel.startSupersetWith(editing.key) },
            onDismiss = { editingKey = null },
        )
    }
}

@Composable
private fun SupersetHeader(
    number: Int,
    start: ArrangeRow.Start,
    memberCount: Int,
    onChangeTransition: (Int) -> Unit,
    onChangeRoundRest: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Superset $number · $memberCount ${if (memberCount == 1) "exercise" else "exercises"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove superset $number")
                }
            }
            TimeStepper(
                label = "Time to the next exercise",
                text = "${start.transitionSeconds} s",
                onMinus = { onChangeTransition(-5) },
                onPlus = { onChangeTransition(5) },
            )
            TimeStepper(
                label = "Rest after each round",
                text = formatDuration(start.roundRestSeconds),
                onMinus = { onChangeRoundRest(-15) },
                onPlus = { onChangeRoundRest(15) },
            )
            when {
                memberCount > MAX_SUPERSET_SIZE -> Text(
                    text = "A superset has at most $MAX_SUPERSET_SIZE exercises. Move some out to save.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                memberCount < 2 -> Text(
                    text = "Drag at least 2 exercises below this header, above the end line.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun TimeStepper(label: String, text: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = onMinus) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(text = text, modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleMedium)
        FilledTonalIconButton(onClick = onPlus) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** The line closing a superset: exercises dragged below it are done on their own again. */
@Composable
private fun SupersetEnd(number: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = "End of superset $number",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ItemRow(item: ArrangeRow.Item, inSuperset: Boolean, onEdit: () -> Unit, dragHandle: Modifier) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (inSuperset) 16.dp else 0.dp),
    ) {
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
                item.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val ownRest = item.ownRestSeconds
                if (inSuperset && ownRest != null) {
                    Text(
                        text = "Its own ${formatDuration(ownRest)} rest is replaced by the superset's timing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options for ${item.exerciseName}")
            }
        }
    }
}

@Composable
private fun ItemDialog(
    item: ArrangeRow.Item,
    rows: List<ArrangeRow>,
    onMoveTo: (supersetId: String?) -> Unit,
    onNewSuperset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val supersets = rows.filterIsInstance<ArrangeRow.Start>().map { it.supersetId }
    val current = supersetMembership(rows)[item.key]
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.exerciseName) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Move to", style = MaterialTheme.typography.titleSmall)
                RadioLine("On its own", selected = current == null) { onMoveTo(null) }
                supersets.forEachIndexed { index, id ->
                    RadioLine("Superset ${index + 1}", selected = current == id) { onMoveTo(id) }
                }
                TextButton(onClick = onNewSuperset) { Text("+ New superset") }
                val ownRest = item.ownRestSeconds
                if (ownRest != null) {
                    Text(
                        text = "This exercise has its own ${formatDuration(ownRest)} rest. In a superset, the " +
                            "superset's timing is used instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
