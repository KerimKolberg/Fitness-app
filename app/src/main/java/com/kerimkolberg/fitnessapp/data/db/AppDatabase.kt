package com.kerimkolberg.fitnessapp.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CategoryEntity::class,
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        BodyMeasurementEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v2 adds routines and body measurements (new tables only).
        AutoMigration(from = 1, to = 2),
        // v3 adds exercise tempo and per-side flag, and set effort (RPE).
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    abstract fun workoutDao(): WorkoutDao

    abstract fun routineDao(): RoutineDao

    abstract fun bodyDao(): BodyDao

    companion object {
        // Once the app is released, every schema change needs a Migration: never use destructive migrations.
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "fitness.db").build()
    }
}
