package com.kkfittracking

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kkfittracking.model.Settings
import com.kkfittracking.model.ThemeMode
import com.kkfittracking.ui.AppNavHost
import com.kkfittracking.ui.ExerciseLogRoute
import com.kkfittracking.ui.components.formatFullDate
import com.kkfittracking.ui.guide.GuideSummaryDialog
import com.kkfittracking.ui.theme.FitnessTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    /** An exercise to open, from the guided workout's notification. */
    private val openRequest = MutableStateFlow<ExerciseLogRoute?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openRequest.value = exerciseToOpen(intent)
        enableEdgeToEdge()
        val container = (application as FitnessApplication).container
        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            FitnessTheme(darkTheme = darkTheme) {
                val open by openRequest.collectAsStateWithLifecycle()
                AppNavHost(openRequest = open, onOpened = { openRequest.value = null })
                val guideSummary by container.guidedWorkout.summary.collectAsStateWithLifecycle()
                guideSummary?.let { summary ->
                    GuideSummaryDialog(
                        summary = summary,
                        onDismiss = container.guidedWorkout::consumeSummary,
                        onMoveRest = { to ->
                            container.guidedWorkout.consumeSummary()
                            container.appScope.launch {
                                val moved = container.workoutRepository.moveUnfinished(LocalDate.ofEpochDay(summary.epochDay), to)
                                showMoved(moved, to)
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        exerciseToOpen(intent)?.let { openRequest.value = it }
    }

    private fun showMoved(count: Int, to: LocalDate) {
        val what = if (count == 1) "1 exercise" else "$count exercises"
        Toast.makeText(this, "Moved $what to ${formatFullDate(to)}", Toast.LENGTH_LONG).show()
    }

    private fun exerciseToOpen(intent: Intent?): ExerciseLogRoute? {
        val exerciseId = intent?.getStringExtra(EXTRA_EXERCISE_ID) ?: return null
        return ExerciseLogRoute(intent.getLongExtra(EXTRA_EPOCH_DAY, 0), exerciseId)
    }

    companion object {
        const val EXTRA_EPOCH_DAY = "epochDay"
        const val EXTRA_EXERCISE_ID = "exerciseId"
    }
}
