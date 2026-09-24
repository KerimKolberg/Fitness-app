package com.kerimkolberg.fitnessapp.data.db

import android.content.Context
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
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    abstract fun workoutDao(): WorkoutDao

    companion object {
        // Once the app is released, every schema change needs a Migration: never use destructive migrations.
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "fitness.db").build()
    }
}
