package com.kkfittracking.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A routine's exercise, joined with the exercise's name and category color. */
data class RoutineExerciseRow(
    val id: String,
    val routineId: String,
    val exerciseId: String,
    val exerciseName: String,
    val categoryColor: Int,
    val supersetId: String?,
    val transitionSeconds: Int?,
    val roundRestSeconds: Int?,
)

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query(
        """
        SELECT re.id AS id, re.routineId AS routineId, re.exerciseId AS exerciseId,
               e.name AS exerciseName, c.color AS categoryColor, re.supersetId AS supersetId,
               re.transitionSeconds AS transitionSeconds, re.roundRestSeconds AS roundRestSeconds
        FROM routine_exercises re
        JOIN exercises e ON e.id = re.exerciseId
        JOIN categories c ON c.id = e.categoryId
        WHERE re.deletedAt IS NULL
        ORDER BY re.sortOrder
        """,
    )
    fun observeRoutineExercises(): Flow<List<RoutineExerciseRow>>

    @Query("SELECT name FROM routines WHERE deletedAt IS NULL")
    suspend fun getRoutineNames(): List<String>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutine(id: String): RoutineEntity?

    @Insert
    suspend fun insertRoutine(routine: RoutineEntity)

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId AND deletedAt IS NULL ORDER BY sortOrder")
    suspend fun getRoutineExercises(routineId: String): List<RoutineExerciseEntity>

    @Insert
    suspend fun insertRoutineExercise(routineExercise: RoutineExerciseEntity)

    @Update
    suspend fun updateRoutineExercises(routineExercises: List<RoutineExerciseEntity>)

    @Query("UPDATE routine_exercises SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteRoutineExercise(id: String, now: Long)

    @Query("SELECT * FROM routine_exercises WHERE id = :id")
    suspend fun getRoutineExercise(id: String): RoutineExerciseEntity?

    @Query(
        """
        UPDATE routine_exercises
        SET sortOrder = :sortOrder, supersetId = :supersetId, transitionSeconds = :transitionSeconds,
            roundRestSeconds = :roundRestSeconds, updatedAt = :now
        WHERE id = :id
        """,
    )
    suspend fun arrange(
        id: String,
        sortOrder: Int,
        supersetId: String?,
        transitionSeconds: Int?,
        roundRestSeconds: Int?,
        now: Long,
    )

    @Query(
        """
        UPDATE routine_exercises
        SET supersetId = :supersetId, transitionSeconds = :transitionSeconds, roundRestSeconds = :roundRestSeconds,
            updatedAt = :now
        WHERE id IN (:ids)
        """,
    )
    suspend fun setSuperset(ids: List<String>, supersetId: String?, transitionSeconds: Int?, roundRestSeconds: Int?, now: Long)

    @Query(
        """
        UPDATE routine_exercises SET deletedAt = :now, updatedAt = :now
        WHERE routineId = :routineId AND deletedAt IS NULL
        """,
    )
    suspend fun softDeleteExercisesOf(routineId: String, now: Long)
}
