package com.kkfittracking.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val fullDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
private val shortDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d")

/** "Today", "Yesterday", "Tomorrow", or the weekday name. */
fun relativeDayName(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
}

fun formatFullDate(date: LocalDate): String = date.format(fullDateFormat)

/** "Today", "Yesterday", or a short date such as "Mon, Sep 22" (with the year when it is not this year). */
fun formatShortDate(date: LocalDate, today: LocalDate = LocalDate.now()): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.year == today.year -> date.format(shortDateFormat)
    else -> date.format(fullDateFormat)
}

@Composable
fun ColorDot(color: Int, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(color), CircleShape),
    )
}

/**
 * Returns a function that asks for the notification permission (Android 13+) the first time it is
 * called, so the rest timer can notify when the app is in the background.
 */
@Composable
fun rememberNotificationPermissionRequester(): () -> Unit {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return {}
    val context = LocalContext.current
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted && !asked) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
