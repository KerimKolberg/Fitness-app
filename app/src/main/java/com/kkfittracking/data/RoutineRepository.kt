package com.kkfittracking.data

import androidx.room.withTransaction
import com.kkfittracking.data.db.AppDatabase
import com.kkfittracking.data.db.RoutineEntity
import com.kkfittracking.data.db.RoutineExerciseEntity
import com.kkfittracking.model.Routine
import com.kkfittracking.model.RoutineExercise
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
                        RoutineExercise(it.id, it.exerciseId, it.exerciseName, it.categoryColor)
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
        val time = now()
        dao.getRoutineExercises(routineId).filter { it.exerciseId == exerciseId }.forEach {
            dao.softDeleteRoutineExercise(it.id, time)
        }
    }

    /** Adds the [StarterPlans] that the user does not have yet (matched by name). Returns how many were added. */
    suspend fun addStarterPlans(): Int = database.withTransaction {
        val existing = dao.getRoutineNames().map { it.lowercase() }.toSet()
        var added = 0
        StarterPlans.plans.filter { (name, _) -> name.lowercase() !in existing }.forEach { (name, exercises) ->
            val id = createRoutine(name)
            exercises.forEach { addExercise(id, StarterPlans.exerciseId(it)) }
            added++
        }
        added
    }

    suspend fun removeExercise(routineExerciseId: String) {
        dao.softDeleteRoutineExercise(routineExerciseId, now())
    }

    /** Moves an exercise up (-1) or down (+1) in its routine. */
    suspend fun moveExercise(routineId: String, routineExerciseId: String, direction: Int) {
        database.withTransaction {
            val list = dao.getRoutineExercises(routineId).toMutableList()
            val from = list.indexOfFirst { it.id == routineExerciseId }
            val to = from + direction
            if (from < 0 || to !in list.indices) return@withTransaction
            list.add(to, list.removeAt(from))
            val time = now()
            val changed = list.mapIndexedNotNull { index, item ->
                if (item.sortOrder != index) item.copy(sortOrder = index, updatedAt = time) else null
            }
            dao.updateRoutineExercises(changed)
        }
    }

    /** The exercise ids of a routine, in order. */
    suspend fun exerciseIds(routineId: String): List<String> =
        dao.getRoutineExercises(routineId).map { it.exerciseId }
}
