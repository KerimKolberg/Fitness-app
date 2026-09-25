package com.kkfittracking.ui.components

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.kkfittracking.model.ExerciseLink
import com.kkfittracking.model.ExerciseLinks

/** Returns a function that opens a web address in the browser or the YouTube app. */
@Composable
fun rememberLinkOpener(): (String) -> Unit {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    return remember(uriHandler, context) {
        { url ->
            try {
                uriHandler.openUri(url)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "No app on this phone can open this link", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                Toast.makeText(context, "No app on this phone can open this link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/** A video or page link: tap to open it. */
@Composable
fun LinkRow(link: ExerciseLink, onRemove: (() -> Unit)?, modifier: Modifier = Modifier) {
    val open = rememberLinkOpener()
    ListItem(
        modifier = modifier.clickable { open(link.url) },
        leadingContent = {
            Icon(
                imageVector = if (link.isVideo) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = if (link.isVideo) "Video" else "Link",
            )
        },
        headlineContent = { Text(link.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (link.title.isNotBlank()) {
            { Text(link.url, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else {
            null
        },
        trailingContent = onRemove?.let { remove ->
            {
                IconButton(onClick = remove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove link ${link.label}")
                }
            }
        },
    )
}

/** Asks for a link (a YouTube address or any web page) and an optional title. */
@Composable
fun AddLinkDialog(onAdd: (ExerciseLink) -> Unit, onDismiss: () -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    val normalized = ExerciseLinks.normalizeUrl(url)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a video or link") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Link, e.g. from YouTube's Share button") },
                    singleLine = true,
                    isError = url.isNotBlank() && normalized == null,
                    supportingText = if (url.isNotBlank() && normalized == null) {
                        { Text("That is not a web address") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { normalized?.let { onAdd(ExerciseLink(it, title.trim())) } },
                enabled = normalized != null,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
