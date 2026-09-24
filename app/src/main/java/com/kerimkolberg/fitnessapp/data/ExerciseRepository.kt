package com.kerimkolberg.fitnessapp.data

import com.kerimkolberg.fitnessapp.data.db.CategoryEntity
import com.kerimkolberg.fitnessapp.data.db.ExerciseDao
import com.kerimkolberg.fitnessapp.data.db.ExerciseEntity
import com.kerimkolberg.fitnessapp.model.Category
import com.kerimkolberg.fitnessapp.model.Exercise
import com.kerimkolberg.fitnessapp.model.ExerciseType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ExerciseRepository(
    private val dao: ExerciseDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    val categories: Flow<List<Category>> =
        dao.observeCategories().map { list -> list.map { it.toModel() } }

    val exercises: Flow<List<Exercise>> =
        dao.observeExercises().map { list -> list.map { it.toModel() } }

    fun observeExercise(id: String): Flow<Exercise?> = dao.observeExercise(id).map { it?.toModel() }

    suspend fun getExercise(id: String): Exercise? = dao.getExercise(id)?.toModel()

    /**
     * Adds any built-in categories and exercises that are not in the database yet. Safe to call on
     * every launch: rows the user edited or deleted are left alone.
     */
    suspend fun addMissingBuiltIns() {
        val time = now()
        val categories = BuiltInExercises.categories
        dao.insertCategoriesIfMissing(
            categories.mapIndexed { index, category ->
                CategoryEntity(
                    id = category.id,
                    name = category.name,
                    color = category.color,
                    sortOrder = index,
                    createdAt = time,
                    updatedAt = time,
                )
            },
        )
        dao.insertExercisesIfMissing(
            categories.flatMap { category ->
                category.exercises.map { exercise ->
                    ExerciseEntity(
                        id = exercise.id,
                        name = exercise.name,
                        categoryId = category.id,
                        type = exercise.type,
                        notes = "",
                        isCustom = false,
                        createdAt = time,
                        updatedAt = time,
                    )
                }
            },
        )
    }

    /** Creates an exercise when [id] is null, otherwise updates it. Returns the exercise id. */
    suspend fun saveExercise(
        id: String?,
        name: String,
        categoryId: String,
        type: ExerciseType,
        notes: String,
    ): String {
        val time = now()
        val existing = id?.let { dao.getExercise(it) }
        val entity = existing?.copy(
            name = name.trim(),
            categoryId = categoryId,
            type = type,
            notes = notes.trim(),
            updatedAt = time,
        ) ?: ExerciseEntity(
            id = id ?: newId(),
            name = name.trim(),
            categoryId = categoryId,
            type = type,
            notes = notes.trim(),
            isCustom = true,
            createdAt = time,
            updatedAt = time,
        )
        dao.upsertExercise(entity)
        return entity.id
    }

    /** Hides the exercise from the library. Its logged sets stay in the workout history. */
    suspend fun deleteExercise(id: String) {
        val existing = dao.getExercise(id) ?: return
        val time = now()
        dao.upsertExercise(existing.copy(deletedAt = time, updatedAt = time))
    }
}

private fun CategoryEntity.toModel() = Category(id = id, name = name, color = color)

private fun ExerciseEntity.toModel() = Exercise(
    id = id,
    name = name,
    categoryId = categoryId,
    type = type,
    notes = notes,
    isCustom = isCustom,
)
