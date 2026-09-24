package com.kerimkolberg.fitnessapp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kerimkolberg.fitnessapp.model.BodyMetric
import com.kerimkolberg.fitnessapp.model.ExerciseType
import java.time.LocalDate

// Sync-ready conventions (see ROADMAP.md): every table uses a UUID string primary key,
// createdAt/updatedAt epoch-millis timestamps, and soft deletes through a nullable deletedAt.
// Queries must always filter on `deletedAt IS NULL`.

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"]),
    ],
    indices = [Index("categoryId")],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String,
    val type: ExerciseType,
    val notes: String,
    val isCustom: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "workouts", indices = [Index("date")])
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val comment: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workoutId"]),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"]),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseId: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
        ),
    ],
    indices = [Index("workoutExerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val workoutExerciseId: String,
    val sortOrder: Int,
    val weightKg: Double?,
    val reps: Int?,
    val distanceMeters: Double?,
    val durationSeconds: Int?,
    val comment: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"]),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"]),
    ],
    indices = [Index("routineId"), Index("exerciseId")],
)
data class RoutineExerciseEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val exerciseId: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** One body measurement. The value is stored in base units: kg, percent, or cm (see [BodyMetric]). */
@Entity(tableName = "body_measurements", indices = [Index("metric", "date")])
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val metric: BodyMetric,
    val value: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
