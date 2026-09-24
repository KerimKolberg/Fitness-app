package com.kerimkolberg.fitnessapp.data

import com.kerimkolberg.fitnessapp.data.db.WorkoutDao
import com.kerimkolberg.fitnessapp.model.GameStats
import com.kerimkolberg.fitnessapp.model.LoggedSet
import com.kerimkolberg.fitnessapp.model.SetValues
import com.kerimkolberg.fitnessapp.model.computeGameStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

/** XP, levels, streaks and achievements, worked out from the workout log whenever it changes. */
class GameRepository(workoutDao: WorkoutDao, settingsRepository: SettingsRepository) {
    private val categoryKeys: Map<String, String> =
        BuiltInExercises.categories.associate { it.id to it.key }

    val stats: Flow<GameStats> =
        combine(workoutDao.observeAllSets(), settingsRepository.settings) { rows, settings ->
            val sets = rows.map { row ->
                LoggedSet(
                    id = row.set.id,
                    date = row.date,
                    exerciseId = row.exerciseId,
                    categoryKey = categoryKeys[row.categoryId],
                    categoryId = row.categoryId,
                    type = row.exerciseType,
                    values = SetValues(
                        weightKg = row.set.weightKg,
                        reps = row.set.reps,
                        distanceMeters = row.set.distanceMeters,
                        durationSeconds = row.set.durationSeconds,
                        rpe = row.set.rpe,
                    ),
                )
            }
            computeGameStats(sets, settings.weeklyGoal, LocalDate.now())
        }.flowOn(Dispatchers.Default)
}
