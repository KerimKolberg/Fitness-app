package com.kkfittracking.data

import com.kkfittracking.data.db.WorkoutDao
import com.kkfittracking.model.GameStats
import com.kkfittracking.model.LoggedSet
import com.kkfittracking.model.SetValues
import com.kkfittracking.model.TrainingStyle
import com.kkfittracking.model.computeGameStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

/** XP, levels, streaks and achievements, worked out from the workout log whenever it changes. */
class GameRepository(workoutDao: WorkoutDao, settingsRepository: SettingsRepository) {
    val stats: Flow<GameStats> =
        combine(workoutDao.observeAllSets(), settingsRepository.settings) { rows, settings ->
            val sets = rows.map { row ->
                LoggedSet(
                    id = row.set.id,
                    date = row.date,
                    exerciseId = row.exerciseId,
                    categoryId = row.categoryId,
                    type = row.exerciseType,
                    styles = TrainingStyle.resolveAll(row.exerciseStyle, BuiltInExercises.regionKeyOf(row.categoryId), row.exerciseType),
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
