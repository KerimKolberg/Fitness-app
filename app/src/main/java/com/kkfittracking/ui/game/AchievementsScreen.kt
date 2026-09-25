@file:OptIn(ExperimentalMaterial3Api::class)

package com.kkfittracking.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkfittracking.model.AchievementStatus
import com.kkfittracking.model.GameStats
import com.kkfittracking.model.XpRules
import com.kkfittracking.ui.components.formatShortDate

@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress & achievements") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val current = stats ?: return@Scaffold
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { LevelCard(current) }
            item { WeeklyGoalCard(current, onChangeGoal = viewModel::changeWeeklyGoal) }
            item {
                Text(
                    text = "Achievements · ${current.achievements.count { it.isUnlocked }} of ${current.achievements.size}",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(current.achievements.sortedByDescending { it.isUnlocked }, key = { it.achievement.name }) {
                AchievementRow(it)
            }
            item {
                Text(
                    text = "How to earn XP: ${XpRules.PER_SET} per set, ${XpRules.PER_WORKOUT} per workout, " +
                        "${XpRules.PER_RECORD} per personal record, ${XpRules.PER_NEW_EXERCISE} for each new exercise " +
                        "you try, and ${XpRules.PER_WEEKLY_GOAL} for reaching your weekly goal.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LevelCard(stats: GameStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Level ${stats.level}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { stats.xpIntoLevel.toFloat() / stats.xpForLevel },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${stats.xpIntoLevel} / ${stats.xpForLevel} XP to level ${stats.level + 1} · ${stats.xp} XP in total",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat("${stats.totalWorkouts}", "workouts")
                Stat("${stats.totalRecords}", "records")
                Stat("${stats.weekStreak}", "week streak")
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WeeklyGoalCard(stats: GameStats, onChangeGoal: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Weekly goal", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${stats.weeklyGoal} workout days a week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalIconButton(onClick = { onChangeGoal(-1) }, enabled = stats.weeklyGoal > 1) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                FilledTonalIconButton(onClick = { onChangeGoal(1) }, enabled = stats.weeklyGoal < 7) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
            LinearProgressIndicator(
                progress = { (stats.workoutsThisWeek.toFloat() / stats.weeklyGoal).coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (stats.workoutsThisWeek >= stats.weeklyGoal) {
                    "Goal reached this week 🎉 (${stats.workoutsThisWeek} workouts)"
                } else {
                    "This week: ${stats.workoutsThisWeek} of ${stats.weeklyGoal}"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AchievementRow(status: AchievementStatus) {
    val achievement = status.achievement
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .alpha(if (status.isUnlocked) 1f else 0.6f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(achievement.emoji, fontSize = 32.sp, modifier = Modifier.width(52.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(achievement.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(achievement.description, style = MaterialTheme.typography.bodySmall)
                val unlockedOn = status.unlockedOn
                if (unlockedOn != null) {
                    Text(
                        text = "Unlocked ${formatShortDate(unlockedOn)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { status.progress.toFloat() / achievement.target },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${status.progress} / ${achievement.target}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
