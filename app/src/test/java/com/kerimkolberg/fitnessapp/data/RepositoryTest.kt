package com.kerimkolberg.fitnessapp.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kerimkolberg.fitnessapp.data.db.AppDatabase
import com.kerimkolberg.fitnessapp.model.ExerciseType
import com.kerimkolberg.fitnessapp.model.SetValues
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Runs the repositories against a real (in-memory) Room database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var exercises: ExerciseRepository
    private lateinit var workouts: WorkoutRepository
    private var clock = 1_000L

    private val bench = BuiltInExercises.stableId("exercise", "flat-barbell-bench-press")
    private val squat = BuiltInExercises.stableId("exercise", "barbell-squat")
    private val day = LocalDate.of(2026, 9, 24)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        exercises = ExerciseRepository(database.exerciseDao(), now = { clock++ })
        workouts = WorkoutRepository(database, now = { clock++ })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun builtInsAreAddedOnceAndKeepUserChanges() = runTest {
        exercises.addMissingBuiltIns()
        val total = BuiltInExercises.categories.sumOf { it.exercises.size }
        assertEquals(total, exercises.exercises.first().size)
        assertEquals(BuiltInExercises.categories.size, exercises.categories.first().size)

        exercises.saveExercise(bench, "Bench Press", exercises.getExercise(bench)!!.categoryId, ExerciseType.WEIGHT_REPS, "")
        exercises.deleteExercise(squat)
        exercises.addMissingBuiltIns()

        val list = exercises.exercises.first()
        assertEquals(total - 1, list.size)
        assertEquals("Bench Press", list.single { it.id == bench }.name)
    }

    @Test
    fun customExercisesCanBeCreated() = runTest {
        exercises.addMissingBuiltIns()
        val categoryId = exercises.categories.first().first().id
        val id = exercises.saveExercise(null, "  Landmine Press ", categoryId, ExerciseType.WEIGHT_REPS, "")
        val saved = exercises.getExercise(id)
        assertNotNull(saved)
        assertEquals("Landmine Press", saved!!.name)
        assertTrue(saved.isCustom)
    }

    @Test
    fun setsOfOneExerciseShareOneDayEntry() = runTest {
        exercises.addMissingBuiltIns()
        workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 5))
        workouts.addSet(day, squat, SetValues(weightKg = 140.0, reps = 3))
        workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 4))

        val logged = workouts.observeDay(day).first()
        assertEquals(listOf(bench, squat), logged.map { it.exerciseId })
        assertEquals(listOf(5, 4), logged[0].sets.map { it.values.reps })
        assertEquals("Flat Barbell Bench Press", logged[0].exerciseName)
    }

    @Test
    fun updatingASet() = runTest {
        exercises.addMissingBuiltIns()
        val setId = workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 5))
        workouts.updateSet(setId, SetValues(weightKg = 102.5, reps = 5))
        assertEquals(102.5, workouts.observeDay(day).first()[0].sets[0].values.weightKg!!, 0.0)
    }

    @Test
    fun deletingTheLastSetRemovesTheExerciseFromTheDay() = runTest {
        exercises.addMissingBuiltIns()
        val first = workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 5))
        val second = workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 5))

        workouts.deleteSet(first)
        assertEquals(1, workouts.observeDay(day).first()[0].sets.size)

        workouts.deleteSet(second)
        assertTrue(workouts.observeDay(day).first().isEmpty())
        assertTrue(workouts.observeWorkoutDates(day, day).first().isEmpty())

        // Logging again after that starts a fresh entry.
        workouts.addSet(day, bench, SetValues(weightKg = 90.0, reps = 8))
        assertEquals(listOf(8), workouts.observeDay(day).first().single().sets.map { it.values.reps })
    }

    @Test
    fun removingAnExerciseFromTheDay() = runTest {
        exercises.addMissingBuiltIns()
        workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 5))
        workouts.addSet(day, squat, SetValues(weightKg = 140.0, reps = 3))
        val benchEntry = workouts.observeDay(day).first().first { it.exerciseId == bench }

        workouts.deleteWorkoutExercise(benchEntry.workoutExerciseId)

        assertEquals(listOf(squat), workouts.observeDay(day).first().map { it.exerciseId })
        assertTrue(workouts.observeHistory(bench).first().isEmpty())
    }

    @Test
    fun historyIsGroupedByDateNewestFirst() = runTest {
        exercises.addMissingBuiltIns()
        val earlier = day.minusDays(3)
        workouts.addSet(earlier, bench, SetValues(weightKg = 95.0, reps = 5))
        workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 5))
        workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 4))

        val history = workouts.observeHistory(bench).first()
        assertEquals(listOf(day, earlier), history.map { it.date })
        assertEquals(listOf(5, 4), history[0].sets.map { it.values.reps })

        val dates = workouts.observeWorkoutDates(day.minusDays(30), day).first()
        assertEquals(setOf(day, earlier), dates)
    }

    @Test
    fun deletedExercisesStillShowInPastWorkouts() = runTest {
        exercises.addMissingBuiltIns()
        workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 5))
        exercises.deleteExercise(bench)

        assertEquals(listOf(bench), workouts.observeDay(day).first().map { it.exerciseId })
        assertTrue(exercises.exercises.first().none { it.id == bench })
    }
}
