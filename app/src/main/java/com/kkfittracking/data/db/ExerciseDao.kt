package com.kkfittracking.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
}
