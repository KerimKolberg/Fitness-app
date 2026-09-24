package com.kerimkolberg.fitnessapp.data

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kerimkolberg.fitnessapp.data.db.AppDatabase
import com.kerimkolberg.fitnessapp.model.BodyMetric
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
import java.nio.file.Files
import java.time.LocalDate

/** Runs the repositories against a real (in-memory) Room database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var exercises: ExerciseRepository
    private lateinit var workouts: WorkoutRepository
    private lateinit var routines: RoutineRepository
    private lateinit var body: BodyRepository
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
        routines = RoutineRepository(database, now = { clock++ })
        body = BodyRepository(database.bodyDao(), now = { clock++ })
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

    @Test
    fun routinesKeepTheirOrder() = runTest {
        exercises.addMissingBuiltIns()
        val row = BuiltInExercises.stableId("exercise", "barbell-row")
        val id = routines.createRoutine("Full body")
        routines.addExercise(id, squat)
        routines.addExercise(id, bench)
        routines.addExercise(id, row)

        val routine = routines.observeRoutine(id).first()!!
        assertEquals(listOf(squat, bench, row), routine.exercises.map { it.exerciseId })

        routines.moveExercise(id, routine.exercises[2].id, -1)
        assertEquals(listOf(squat, row, bench), routines.exerciseIds(id))

        routines.removeExercise(routine.exercises[0].id)
        assertEquals(listOf(row, bench), routines.exerciseIds(id))

        routines.deleteRoutine(id)
        assertTrue(routines.routines.first().isEmpty())
    }

    @Test
    fun plannedExercisesAppearWithoutSetsAndAreReusedWhenLogging() = runTest {
        exercises.addMissingBuiltIns()
        workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 5))
        workouts.addExercisesToDay(day, listOf(bench, squat, squat))

        val planned = workouts.observeDay(day).first()
        assertEquals(listOf(bench, squat), planned.map { it.exerciseId })
        assertTrue(planned[1].sets.isEmpty())
        // A day with only planned exercises is not a workout day on the calendar.
        val tomorrow = day.plusDays(1)
        workouts.addExercisesToDay(tomorrow, listOf(bench))
        assertEquals(1, workouts.observeDay(tomorrow).first().size)
        assertTrue(workouts.observeWorkoutDates(tomorrow, tomorrow).first().isEmpty())

        workouts.addSet(day, squat, SetValues(weightKg = 140.0, reps = 3))
        val logged = workouts.observeDay(day).first()
        assertEquals(2, logged.size)
        assertEquals(1, logged[1].sets.size)
    }

    @Test
    fun oneBodyMeasurementPerMetricAndDay() = runTest {
        body.saveMeasurement(BodyMetric.BODYWEIGHT, day.minusDays(7), 81.0)
        body.saveMeasurement(BodyMetric.BODYWEIGHT, day, 80.5)
        body.saveMeasurement(BodyMetric.BODYWEIGHT, day, 80.0)
        body.saveMeasurement(BodyMetric.WAIST, day, 84.0)

        val all = body.measurements.first()
        val weights = all.filter { it.metric == BodyMetric.BODYWEIGHT }
        assertEquals(listOf(day, day.minusDays(7)), weights.map { it.date })
        assertEquals(80.0, weights.first().value, 0.0)

        body.deleteMeasurement(weights.first().id)
        assertEquals(listOf(81.0), body.measurements.first().filter { it.metric == BodyMetric.BODYWEIGHT }.map { it.value })
    }

    @Test
    fun starterPlansAreAddedOnceAndShareExercises() = runTest {
        exercises.addMissingBuiltIns()
        assertEquals(StarterPlans.plans.size, routines.addStarterPlans())
        assertEquals(0, routines.addStarterPlans())

        val plans = routines.routines.first().associateBy { it.name }
        val incline = StarterPlans.exerciseId("Incline Barbell Bench Press")
        assertTrue(plans.getValue("Push").exercises.any { it.exerciseId == incline })

        // The same exercise can be in several plans, but only once per plan.
        val upper = plans.getValue("Upper body")
        routines.addExercise(upper.id, incline)
        routines.addExercise(upper.id, incline)
        assertEquals(1, routines.exerciseIds(upper.id).count { it == incline })
        routines.removeExerciseFromRoutine(upper.id, incline)
        assertTrue(incline !in routines.exerciseIds(upper.id))
        assertTrue(incline in routines.exerciseIds(plans.getValue("Push").id))
    }

    @Test
    fun gameStatsFollowTheLog() = runTest {
        exercises.addMissingBuiltIns()
        val settingsFile = Files.createTempDirectory("settings").resolve("test.preferences_pb").toFile()
        val settings = SettingsRepository(PreferenceDataStoreFactory.create(produceFile = { settingsFile }))
        val game = GameRepository(database.workoutDao(), settings)
        assertEquals(0, game.stats.first().xp)

        workouts.addSet(LocalDate.now(), StarterPlans.exerciseId("Box Jump"), SetValues(reps = 5, distanceMeters = 0.6))
        val stats = game.stats.first()
        assertEquals(1, stats.totalWorkouts)
        assertTrue(stats.xp > 0)
    }
}
