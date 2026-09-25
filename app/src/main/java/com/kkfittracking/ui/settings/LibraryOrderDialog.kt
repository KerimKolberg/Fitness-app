package com.kkfittracking.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kkfittracking.model.Category
import com.kkfittracking.model.TrainingStyle
import com.kkfittracking.ui.components.ColorDot
import com.kkfittracking.ui.components.SectionIcon

/** Arranges the library: the body sections and the training styles, first to last. */
@Composable
fun LibraryOrderDialog(
    sections: List<Category>,
    styles: List<TrainingStyle>,
    onMoveSection: (id: String, direction: Int) -> Unit,
    onMoveStyle: (TrainingStyle, direction: Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Library order") },
        text = {
            LazyColumn {
                item { Heading("Body sections") }
                itemsIndexed(sections, key = { _, it -> "section-${it.id}" }) { index, section ->
                    OrderRow(
                        label = section.name,
                        icon = { SectionIcon(section.key, section.color, size = 22.dp) },
                        canMoveUp = index > 0,
                        canMoveDown = index < sections.lastIndex,
                        onMove = { onMoveSection(section.id, it) },
                    )
                }
                item { Heading("Training styles") }
                itemsIndexed(styles, key = { _, it -> "style-${it.name}" }) { index, style ->
                    OrderRow(
                        label = style.label,
                        icon = { ColorDot(style.color) },
                        canMoveUp = index > 0,
                        canMoveDown = index < styles.lastIndex,
                        onMove = { onMoveStyle(style, it) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = onReset) { Text("Reset") } },
    )
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun OrderRow(label: String, icon: @Composable () -> Unit, canMoveUp: Boolean, canMoveDown: Boolean, onMove: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Text(label, modifier = Modifier.padding(start = 12.dp).weight(1f))
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move $label up")
        }
        IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move $label down")
        }
    }
}
