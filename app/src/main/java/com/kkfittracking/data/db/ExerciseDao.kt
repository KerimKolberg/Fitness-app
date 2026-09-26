package com.kkfittracking.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY sortOrder")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM exercises WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeExercise(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExercise(id: String): ExerciseEntity?

    /** Inserts rows whose id does not exist yet; existing rows (including user edits and soft deletes) are kept. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategoriesIfMissing(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercisesIfMissing(exercises: List<ExerciseEntity>)

    @Upsert
    suspend fun upsertExercise(exercise: ExerciseEntity)

    /** Every category, deleted ones included, for keeping the built-in catalog up to date. */
    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    /** Every exercise, deleted ones included, for keeping the built-in catalog up to date. */
    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Update
    suspend fun updateCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateExercises(exercises: List<ExerciseEntity>)

    @Query("UPDATE exercises SET plan = :plan, updatedAt = :now WHERE id = :id")
    suspend fun updatePlan(id: String, plan: String, now: Long)

    @Query("UPDATE exercises SET notes = :notes, updatedAt = :now WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String, now: Long)

    @Query("UPDATE exercises SET links = :links, updatedAt = :now WHERE id = :id")
    suspend fun updateLinks(id: String, links: String, now: Long)

    @Query("UPDATE exercises SET weightUnit = :unit, updatedAt = :now WHERE id = :id")
    suspend fun updateWeightUnit(id: String, unit: String, now: Long)
}
