package com.kerimkolberg.fitnessapp.data

import androidx.room.withTransaction
import com.kerimkolberg.fitnessapp.data.db.AppDatabase
import com.kerimkolberg.fitnessapp.data.db.DayRow
import com.kerimkolberg.fitnessapp.data.db.WorkoutEntity
import com.kerimkolberg.fitnessapp.data.db.WorkoutExerciseEntity
import com.kerimkolberg.fitnessapp.data.db.WorkoutSetEntity
import com.kerimkolberg.fitnessapp.model.DayExercise
import com.kerimkolberg.fitnessapp.model.HistorySession
import com.kerimkolberg.fitnessapp.model.MAX_SUPERSET_SIZE
import com.kerimkolberg.fitnessapp.model.SetEntry
import com.kerimkolberg.fitnessapp.model.SetValues
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class WorkoutRepository(
    private val database: AppDatabase,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val dao = database.workoutDao()

    /** The exercises logged on [date], in the order they were first logged. */
    fun observeDay(date: LocalDate): Flow<List<DayExercise>> =
        dao.observeDay(date).map { groupDayRows(it) }

    /** Every session of an exercise, newest first. */
    fun observeHistory(exerciseId: String): Flow<List<HistorySession>> =
        dao.observeHistory(exerciseId).map { rows ->
            rows.groupBy { it.date }.map { (date, dateRows) ->
                HistorySession(date = date, sets = dateRows.map { it.set.toSetEntry() })
            }
        }

    /** The dates between [from] and [to] (inclusive) that have at least one logged set. */
    fun observeWorkoutDates(from: LocalDate, to: LocalDate): Flow<Set<LocalDate>> =
        dao.observeWorkoutDates(from, to).map { it.toSet() }

    /** Logs a new set, creating the day's workout and the exercise entry when needed. */
    suspend fun addSet(date: LocalDate, exerciseId: String, values: SetValues): String =
        database.withTransaction {
            val time = now()
            val workoutExercise = getOrCreateWorkoutExercise(getOrCreateWorkout(date, time), exerciseId, time)
            val set = WorkoutSetEntity(
                id = newId(),
                workoutExerciseId = workoutExercise.id,
                sortOrder = (dao.maxSetSortOrder(workoutExercise.id) ?: -1) + 1,
                weightKg = values.weightKg,
                reps = values.reps,
                distanceMeters = values.distanceMeters,
                durationSeconds = values.durationSeconds,
                comment = values.note,
                rpe = values.rpe,
                isDropSet = values.isDropSet,
                createdAt = time,
                updatedAt = time,
            )
            dao.insertSet(set)
            set.id
        }

    suspend fun updateSet(setId: String, values: SetValues) {
        val existing = dao.getSet(setId) ?: return
        dao.updateSet(
            existing.copy(
                weightKg = values.weightKg,
                reps = values.reps,
                distanceMeters = values.distanceMeters,
                durationSeconds = values.durationSeconds,
                rpe = values.rpe,
                comment = values.note,
                isDropSet = values.isDropSet,
                updatedAt = now(),
            ),
        )
    }

    /** Deletes a set. When it was the exercise's last set that day, the exercise leaves the day too. */
    suspend fun deleteSet(setId: String) {
        database.withTransaction {
            val set = dao.getSet(setId) ?: return@withTransaction
            val time = now()
            dao.softDeleteSet(setId, time)
            if (dao.countSets(set.workoutExerciseId) == 0) {
                dao.softDeleteWorkoutExercise(set.workoutExerciseId, time)
            }
        }
    }

    /**
     * Adds exercises to a day without sets, as a plan to fill in (from a routine or an earlier
     * workout). Exercises already on that day are skipped.
     */
    suspend fun addExercisesToDay(date: LocalDate, exerciseIds: List<String>) {
        if (exerciseIds.isEmpty()) return
        database.withTransaction {
            val time = now()
            val workout = getOrCreateWorkout(date, time)
            exerciseIds.distinct().forEach { getOrCreateWorkoutExercise(workout, it, time) }
        }
    }

    private suspend fun getOrCreateWorkout(date: LocalDate, time: Long): WorkoutEntity =
        dao.getWorkoutForDate(date) ?: WorkoutEntity(
            id = newId(),
            date = date,
            comment = "",
            createdAt = time,
            updatedAt = time,
        ).also { dao.insertWorkout(it) }

    private suspend fun getOrCreateWorkoutExercise(
        workout: WorkoutEntity,
        exerciseId: String,
        time: Long,
    ): WorkoutExerciseEntity =
        dao.getWorkoutExercise(workout.id, exerciseId) ?: WorkoutExerciseEntity(
            id = newId(),
            workoutId = workout.id,
            exerciseId = exerciseId,
            sortOrder = (dao.maxExerciseSortOrder(workout.id) ?: -1) + 1,
            createdAt = time,
            updatedAt = time,
        ).also { dao.insertWorkoutExercise(it) }

    /**
     * Groups exercises into a superset on [date], adding any that are not on that day yet. Exercises
     * already in another superset leave it. Returns the new superset id.
     */
    suspend fun createSuperset(date: LocalDate, exerciseIds: List<String>): String {
        val ids = exerciseIds.distinct()
        require(ids.size in 2..MAX_SUPERSET_SIZE) { "A superset has 2 to $MAX_SUPERSET_SIZE exercises" }
        return database.withTransaction {
            val time = now()
            val workout = getOrCreateWorkout(date, time)
            val entries = ids.map { getOrCreateWorkoutExercise(workout, it, time) }
            val supersetId = newId()
            dao.setSuperset(entries.map { it.id }, supersetId, time)
            supersetId
        }
    }

    /** Turns a superset back into separate exercises. */
    suspend fun ungroupSuperset(supersetId: String) {
        dao.clearSuperset(supersetId, now())
    }

    /** Removes an exercise and all its sets from a day. */
    suspend fun deleteWorkoutExercise(workoutExerciseId: String) {
        database.withTransaction {
            val time = now()
            dao.softDeleteSetsOf(workoutExerciseId, time)
            dao.softDeleteWorkoutExercise(workoutExerciseId, time)
        }
    }
}

private fun groupDayRows(rows: List<DayRow>): List<DayExercise> =
    rows.groupBy { it.workoutExerciseId }.map { (workoutExerciseId, exerciseRows) ->
        val first = exerciseRows.first()
        DayExercise(
            workoutExerciseId = workoutExerciseId,
            exerciseId = first.exerciseId,
            exerciseName = first.exerciseName,
            exerciseType = first.exerciseType,
            categoryColor = first.categoryColor,
            supersetId = first.supersetId,
            sets = exerciseRows.mapNotNull { row ->
                row.setId?.let { id ->
                    SetEntry(
                        id = id,
                        values = SetValues(
                            weightKg = row.weightKg,
                            reps = row.reps,
                            distanceMeters = row.distanceMeters,
                            durationSeconds = row.durationSeconds,
                            rpe = row.rpe,
                            note = row.comment.orEmpty(),
                            isDropSet = row.isDropSet == true,
                        ),
                    )
                }
            },
        )
    }

private fun WorkoutSetEntity.toSetEntry() = SetEntry(
    id = id,
    values = SetValues(
        weightKg = weightKg,
        reps = reps,
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        rpe = rpe,
        note = comment,
        isDropSet = isDropSet,
    ),
)
