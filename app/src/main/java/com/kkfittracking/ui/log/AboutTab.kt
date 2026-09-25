package com.kkfittracking.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kkfittracking.model.Exercise
import com.kkfittracking.model.ExerciseLink
import com.kkfittracking.model.ExerciseLinks
import com.kkfittracking.model.Muscle
import com.kkfittracking.ui.components.AddLinkDialog
import com.kkfittracking.ui.components.LinkRow
import com.kkfittracking.ui.components.TendonIcon
import com.kkfittracking.ui.components.TextInputDialog
import com.kkfittracking.ui.components.rememberLinkOpener

/** How the exercise is done: the description first, then videos and links, and where it is in the library. */
@Composable
fun AboutTab(
    exercise: Exercise,
    sectionName: String?,
    onSaveDescription: (String) -> Unit,
    onAddLink: (ExerciseLink) -> Unit,
    onRemoveLink: (ExerciseLink) -> Unit,
    onEditExercise: () -> Unit,
) {
    var editingDescription by rememberSaveable { mutableStateOf(false) }
    var addingLink by rememberSaveable { mutableStateOf(false) }
    val open = rememberLinkOpener()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column {
                Text("How to do it", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = exercise.notes.ifBlank { "No description yet. Add the setup, the movement and the cues that help you." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (exercise.notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(onClick = { editingDescription = true }) {
                    Text(if (exercise.notes.isBlank()) "Add a description" else "Edit the description")
                }
                HorizontalDivider()
            }
        }
        if (exercise.tendons.isNotEmpty()) {
            item {
                Text(
                    text = if (exercise.tendons.size == 1) "The tendon it trains" else "The tendons it trains",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(exercise.tendons, key = { "tendon-${it.name}" }) { tendon ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TendonIcon(tendon, size = 56.dp)
                    Column(Modifier.weight(1f)) {
                        Text(tendon.label, style = MaterialTheme.typography.titleSmall)
                        Text("Connects ${tendon.connects}.", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = tendon.about,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Text(
                    text = "Tendons adapt slower than muscles: build up the load over weeks. Pain that lasts is worth a physio's look.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(Modifier.padding(top = 8.dp))
            }
        }
        item {
            Column {
                Text("Videos & links", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                if (exercise.links.isEmpty()) {
                    Text(
                        text = "No links yet. Add a YouTube video or a page that shows how it is done.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(exercise.links, key = { it.url }) { link ->
            LinkRow(link, onRemove = { onRemoveLink(link) })
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { addingLink = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add a link", modifier = Modifier.padding(start = 4.dp))
                    }
                    OutlinedButton(onClick = { open(ExerciseLinks.youTubeSearchUrl(exercise.name)) }) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Text("Find on YouTube", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Text(
                    text = "Tip: in YouTube, tap Share, then Copy link, and paste it here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(Modifier.padding(top = 4.dp))
            }
        }
        item {
            Column(Modifier.padding(top = 8.dp)) {
                Text("In the library", style = MaterialTheme.typography.titleMedium)
                val path = listOfNotNull(
                    sectionName,
                    exercise.muscle.takeIf { it != Muscle.OTHER }?.label,
                    exercise.style.label,
                )
                Text(path.joinToString(" › "), style = MaterialTheme.typography.bodyLarge)
                val others = exercise.muscles.drop(1)
                if (others.isNotEmpty()) {
                    Text("Also trains: " + others.joinToString(", ") { it.label }, style = MaterialTheme.typography.bodyMedium)
                }
                val details = listOfNotNull(
                    exercise.type.label,
                    exercise.tempo.takeIf { it.isNotBlank() }?.let { "tempo $it" },
                    "each side separately".takeIf { exercise.perSide },
                )
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onEditExercise) { Text("Edit exercise") }
            }
        }
    }

    if (editingDescription) {
        TextInputDialog(
            title = "How to do it",
            label = "Description",
            initialValue = exercise.notes,
            multiLine = true,
            onConfirm = {
                editingDescription = false
                onSaveDescription(it)
            },
            onDismiss = { editingDescription = false },
        )
    }
    if (addingLink) {
        AddLinkDialog(
            onAdd = {
                addingLink = false
                onAddLink(it)
            },
            onDismiss = { addingLink = false },
        )
    }
}
