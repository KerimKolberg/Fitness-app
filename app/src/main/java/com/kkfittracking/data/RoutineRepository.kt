package com.kkfittracking.data

import androidx.room.withTransaction
import com.kkfittracking.data.db.AppDatabase
import com.kkfittracking.data.db.RoutineEntity
import com.kkfittracking.data.db.RoutineExerciseEntity
import com.kkfittracking.model.ArrangedExercise
import com.kkfittracking.model.PlannedExercise
import com.kkfittracking.model.Routine
import com.kkfittracking.model.RoutineExercise
import com.kkfittracking.model.groupSupersets
import com.kkfittracking.model.moveBlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoutineRepository(
    private val database: AppDatabase,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val dao = database.routineDao()

    val routines: Flow<List<Routine>> =
        combine(dao.observeRoutines(), dao.observeRoutineExercises()) { routines, rows ->
            val byRoutine = rows.groupBy { it.routineId }
            routines.map { routine ->
                Routine(
                    id = routine.id,
                    name = routine.name,
                    notes = routine.notes,
                    exercises = byRoutine[routine.id].orEmpty().map {
                        RoutineExercise(
                            id = it.id,
                            exerciseId = it.exerciseId,
                            exerciseName = it.exerciseName,
                            categoryColor = it.categoryColor,
                            supersetId = it.supersetId,
                            transitionSeconds = it.transitionSeconds,
                            roundRestSeconds = it.roundRestSeconds,
                        )
                    },
                )
            }
        }

    fun observeRoutine(id: String): Flow<Routine?> = routines.map { list -> list.firstOrNull { it.id == id } }

    suspend fun createRoutine(name: String): String {
        val time = now()
        val routine = RoutineEntity(
            id = newId(),
            name = name.trim(),
            notes = "",
            createdAt = time,
            updatedAt = time,
        )
        dao.insertRoutine(routine)
        return routine.id
    }

    suspend fun renameRoutine(id: String, name: String) {
        val routine = dao.getRoutine(id) ?: return
        dao.updateRoutine(routine.copy(name = name.trim(), updatedAt = now()))
    }

    suspend fun deleteRoutine(id: String) {
        database.withTransaction {
            val routine = dao.getRoutine(id) ?: return@withTransaction
            val time = now()
            dao.softDeleteExercisesOf(id, time)
            dao.updateRoutine(routine.copy(deletedAt = time, updatedAt = time))
        }
    }

    /** Adds an exercise at the end of a routine, unless it is already in it. */
    suspend fun addExercise(routineId: String, exerciseId: String) {
        database.withTransaction {
            val existing = dao.getRoutineExercises(routineId)
            if (existing.any { it.exerciseId == exerciseId }) return@withTransaction
            val time = now()
            dao.insertRoutineExercise(
                RoutineExerciseEntity(
                    id = newId(),
                    routineId = routineId,
                    exerciseId = exerciseId,
                    sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                    createdAt = time,
                    updatedAt = time,
                ),
            )
        }
    }

    /** Removes an exercise from a routine, wherever it appears in it. */
    suspend fun removeExerciseFromRoutine(routineId: String, exerciseId: String) {
        dao.getRoutineExercises(routineId).filter { it.exerciseId == exerciseId }.forEach { removeExercise(it.id) }
    }

    /** Adds the [StarterPlans] that the user does not have yet (matched by name). Returns how many were added. */
    suspend fun addStarterPlans(): Int = database.withTransaction {
        val existing = dao.getRoutineNames().map { it.lowercase() }.toSet()
        var added = 0
        StarterPlans.plans.filter { it.name.lowercase() !in existing }.forEach { plan ->
            val id = createRoutine(plan.name)
            plan.exercises.forEach { addExercise(id, StarterPlans.exerciseId(it)) }
            val entries = dao.getRoutineExercises(id)
            plan.supersets.forEach { group ->
                val ids = group.map { StarterPlans.exerciseId(it) }
                dao.setSuperset(
                    ids = entries.filter { it.exerciseId in ids }.map { it.id },
                    supersetId = newId(),
                    transitionSeconds = null,
                    roundRestSeconds = null,
                    now = now(),
                )
            }
            added++
        }
        added
    }

    /** Removes an exercise from its routine. A superset left with one exercise is no longer a superset. */
    suspend fun removeExercise(routineExerciseId: String) {
        database.withTransaction {
            val removed = dao.getRoutineExercise(routineExerciseId) ?: return@withTransaction
            val time = now()
            dao.softDeleteRoutineExercise(routineExerciseId, time)
            val supersetId = removed.supersetId ?: return@withTransaction
            val left = dao.getRoutineExercises(removed.routineId).filter { it.supersetId == supersetId }
            if (left.size < 2) dao.setSuperset(left.map { it.id }, null, null, null, time)
        }
    }

    /**
     * Moves an exercise, or a whole superset, one place up (-1) or down (+1) in its routine.
     * [blockIndex] counts the routine's blocks, as [Routine.blocks] shows them.
     */
    suspend fun moveInPlan(routineId: String, blockIndex: Int, direction: Int) {
        database.withTransaction {
            val entries = dao.getRoutineExercises(routineId)
            val ordered = moveBlock(groupSupersets(entries) { it.supersetId }, blockIndex, direction)
            val time = now()
            val changed = ordered.mapIndexedNotNull { index, item ->
                if (item.sortOrder != index) item.copy(sortOrder = index, updatedAt = time) else null
            }
            dao.updateRoutineExercises(changed)
        }
    }

    /** Saves the order and supersets chosen on the "+Super-sets" screen. */
    suspend fun arrangePlan(exercises: List<ArrangedExercise>) {
        database.withTransaction {
            val time = now()
            exercises.forEach {
                dao.arrange(
                    id = it.id,
                    sortOrder = it.sortOrder,
                    supersetId = it.supersetId,
                    transitionSeconds = it.transitionSeconds,
                    roundRestSeconds = it.roundRestSeconds,
                    now = time,
                )
            }
        }
    }

    /** The exercise ids of a routine, in order. */
    suspend fun exerciseIds(routineId: String): List<String> =
        dao.getRoutineExercises(routineId).map { it.exerciseId }

    /** A routine's exercises with their supersets, in order, for adding to a day. */
    suspend fun plannedExercises(routineId: String): List<PlannedExercise> =
        dao.getRoutineExercises(routineId).map {
            PlannedExercise(it.exerciseId, it.supersetId, it.transitionSeconds, it.roundRestSeconds)
        }
}
