package com.kkfittracking.data

import com.kkfittracking.data.db.CategoryEntity
import com.kkfittracking.data.db.ExerciseDao
import com.kkfittracking.data.db.ExerciseEntity
import com.kkfittracking.model.Category
import com.kkfittracking.model.Exercise
import com.kkfittracking.model.ExerciseLink
import com.kkfittracking.model.ExerciseLinks
import com.kkfittracking.model.ExercisePlan
import com.kkfittracking.model.ExerciseType
import com.kkfittracking.model.Muscle
import com.kkfittracking.model.TrainingStyle
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
     * Brings the built-in catalog up to date. Safe to call on every launch: it only writes what
     * changed, and keeps the user's own edits and deletions.
     *
     * It adds new sections and exercises, updates the built-in sections' names, colors and order
     * ("Abs" became "Core"), files exercises that were never classified under their muscle and
     * training style, and moves exercises out of the categories of earlier versions (such as
     * Mobility or Tendons), which are then removed.
     */
    suspend fun syncBuiltIns() {
        val time = now()
        val categories = dao.getAllCategories().associateBy { it.id }

        val changedCategories = mutableListOf<CategoryEntity>()
        val missingCategories = mutableListOf<CategoryEntity>()
        BuiltInExercises.regions.forEachIndexed { index, region ->
            val existing = categories[region.id]
            when {
                existing == null -> missingCategories += CategoryEntity(
                    id = region.id,
                    name = region.name,
                    color = region.color,
                    sortOrder = index,
                    createdAt = time,
                    updatedAt = time,
                )
                existing.name != region.name || existing.color != region.color || existing.sortOrder != index ->
                    changedCategories += existing.copy(name = region.name, color = region.color, sortOrder = index, updatedAt = time)
            }
        }
        // Sections first: exercises may be moved into them.
        dao.insertCategoriesIfMissing(missingCategories)

        val exercises = dao.getAllExercises()
        val existingIds = exercises.map { it.id }.toSet()
        dao.insertExercisesIfMissing(
            BuiltInExercises.exercises.filter { it.id !in existingIds }.map { exercise ->
                ExerciseEntity(
                    id = exercise.id,
                    name = exercise.name,
                    categoryId = BuiltInExercises.stableId("category", exercise.regionKey),
                    type = exercise.type,
                    notes = "",
                    isCustom = false,
                    createdAt = time,
                    updatedAt = time,
                    tempo = exercise.tempo,
                    perSide = exercise.perSide,
                    muscles = Muscle.format(exercise.muscles),
                    style = TrainingStyle.format(exercise.styles),
                    plan = exercise.plan.toJson(),
                )
            },
        )
        dao.updateExercises(exercises.mapNotNull { filed(it, time) })

        // The old categories are empty now.
        changedCategories += BuiltInExercises.retiredCategories.mapNotNull { retired ->
            categories[retired.id]?.takeIf { it.deletedAt == null }?.copy(deletedAt = time, updatedAt = time)
        }
        dao.updateCategories(changedCategories)
    }

    /** The exercise filed under the new library levels, or null when nothing needs to change. */
    private fun filed(row: ExerciseEntity, time: Long): ExerciseEntity? {
        val retired = BuiltInExercises.retired(row.categoryId)
        val builtIn = BuiltInExercises.exercise(row.id)
        val section = BuiltInExercises.regionKeyOf(row.categoryId)
        val movedFrom = builtIn?.movedFrom?.let { BuiltInExercises.stableId("category", it) }
        val result = when {
            // Never filed: in the catalog's place, unless the user moved it to another section.
            builtIn != null && row.muscles.isEmpty() -> row.copy(
                categoryId = if (retired != null || section == null) {
                    BuiltInExercises.stableId("category", builtIn.regionKey)
                } else {
                    row.categoryId
                },
                muscles = Muscle.format(builtIn.muscles),
                style = row.style.ifEmpty { TrainingStyle.format(builtIn.styles) },
            )
            // Moved by a later version (HIIT went from Sports to Cardio): follow, unless the user moved it.
            builtIn != null && movedFrom != null && row.categoryId == movedFrom ->
                row.copy(
                    categoryId = BuiltInExercises.stableId("category", builtIn.regionKey),
                    muscles = Muscle.format(builtIn.muscles),
                )
            // A later version gave it more training styles (Child's Pose is yoga too); the user never changed it.
            builtIn != null && builtIn.styles.size > 1 && row.style == builtIn.style.name ->
                row.copy(style = TrainingStyle.format(builtIn.styles))
            // The user's own exercises in an old category go to its fallback.
            retired != null -> row.copy(
                categoryId = BuiltInExercises.stableId("category", retired.muscle.regionKey),
                muscles = row.muscles.ifEmpty { retired.muscle.name },
                style = row.style.ifEmpty { retired.style.name },
            )
            else -> return null
        }
        return result.copy(updatedAt = time)
    }

    /** Creates an exercise when [id] is null, otherwise updates it. Null values keep what is stored. Returns the id. */
    suspend fun saveExercise(
        id: String?,
        name: String,
        categoryId: String,
        type: ExerciseType,
        notes: String,
        tempo: String = "",
        perSide: Boolean = false,
        /** Every muscle it trains, the main one first. */
        muscles: List<Muscle>? = null,
        /** Every way it trains, the main one first. */
        styles: List<TrainingStyle>? = null,
        links: List<ExerciseLink>? = null,
    ): String {
        val time = now()
        val existing = id?.let { dao.getExercise(it) }
        val entity = existing?.copy(
            name = name.trim(),
            categoryId = categoryId,
            type = type,
            notes = notes.trim(),
            tempo = tempo.trim(),
            perSide = perSide,
            muscles = muscles?.let(Muscle::format) ?: existing.muscles,
            style = styles?.let(TrainingStyle::format) ?: existing.style,
            links = links?.let { ExerciseLinks.format(it) } ?: existing.links,
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
            tempo = tempo.trim(),
            perSide = perSide,
            muscles = muscles?.let(Muscle::format).orEmpty(),
            style = styles?.let(TrainingStyle::format).orEmpty(),
            links = links?.let { ExerciseLinks.format(it) }.orEmpty(),
        )
        dao.upsertExercise(entity)
        return entity.id
    }

    suspend fun savePlan(id: String, plan: ExercisePlan) = dao.updatePlan(id, plan.toJson(), now())

    /** The description: how the exercise is done. */
    suspend fun saveNotes(id: String, notes: String) = dao.updateNotes(id, notes.trim(), now())

    suspend fun saveLinks(id: String, links: List<ExerciseLink>) = dao.updateLinks(id, ExerciseLinks.format(links), now())

    /** Hides the exercise from the library. Its logged sets stay in the workout history. */
    suspend fun deleteExercise(id: String) {
        val existing = dao.getExercise(id) ?: return
        val time = now()
        dao.upsertExercise(existing.copy(deletedAt = time, updatedAt = time))
    }
}

private fun CategoryEntity.toModel() = Category(id = id, name = name, color = color, key = BuiltInExercises.regionKeyOf(id))

private fun ExerciseEntity.toModel(): Exercise {
    val section = BuiltInExercises.regionKeyOf(categoryId)
    return Exercise(
        id = id,
        name = name,
        categoryId = categoryId,
        type = type,
        notes = notes,
        isCustom = isCustom,
        tempo = tempo,
        perSide = perSide,
        muscles = Muscle.resolveAll(muscles, section),
        styles = TrainingStyle.resolveAll(style, section, type),
        plan = ExercisePlan.fromJson(plan),
        links = ExerciseLinks.parse(links),
        tendons = BuiltInTendons.of(id),
    )
}
