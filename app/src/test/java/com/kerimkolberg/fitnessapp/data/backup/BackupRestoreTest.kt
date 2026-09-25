package com.kerimkolberg.fitnessapp.data.backup

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kerimkolberg.fitnessapp.data.BodyRepository
import com.kerimkolberg.fitnessapp.data.ExerciseRepository
import com.kerimkolberg.fitnessapp.data.RoutineRepository
import com.kerimkolberg.fitnessapp.data.SettingsRepository
import com.kerimkolberg.fitnessapp.data.StarterPlans
import com.kerimkolberg.fitnessapp.data.WorkoutRepository
import com.kerimkolberg.fitnessapp.data.db.AppDatabase
import com.kerimkolberg.fitnessapp.model.BodyMetric
import com.kerimkolberg.fitnessapp.model.ExerciseType
import com.kerimkolberg.fitnessapp.model.SetValues
import com.kerimkolberg.fitnessapp.model.UnitSystem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class BackupRestoreTest {
    private val day = LocalDate.of(2026, 9, 25)
    private val bench = StarterPlans.exerciseId("Flat Barbell Bench Press")
    private val squat = StarterPlans.exerciseId("Barbell Squat")
    private val databases = mutableListOf<AppDatabase>()

    /** One phone: a database and its settings. */
    private inner class Phone {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build().also { databases += it }
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(
                produceFile = { Files.createTempDirectory("settings").resolve("test.preferences_pb").toFile() },
            ),
        )
        val exercises = ExerciseRepository(database.exerciseDao())
        val workouts = WorkoutRepository(database)
        val plans = RoutineRepository(database)
        val body = BodyRepository(database.bodyDao())
        val backups = BackupRepository(database, settings, "test")
    }

    @After
    fun tearDown() {
        databases.forEach { it.close() }
    }

    @Test
    fun backupAndRestoreOnAnotherPhone() = runTest {
        val old = Phone()
        old.exercises.addMissingBuiltIns()
        old.workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 5))
        val deleted = old.workouts.addSet(day, bench, SetValues(weightKg = 100.0, reps = 3))
        old.workouts.deleteSet(deleted)
        old.plans.addStarterPlans()
        old.body.saveMeasurement(BodyMetric.BODYWEIGHT, day, 80.0)
        old.settings.setUnitSystem(UnitSystem.IMPERIAL)
        old.exercises.saveExercise(null, "My Custom Lift", old.exercises.categories.first().first().id,
            ExerciseType.WEIGHT_REPS, "", tempo = "4-0-1-0", perSide = true)

        val text = BackupJson.encode(old.backups.createBackup())

        val new = Phone()
        new.exercises.addMissingBuiltIns()
        new.workouts.addSet(day, squat, SetValues(weightKg = 140.0, reps = 3))
        new.backups.restore(BackupJson.decode(text))

        val restoredDay = new.workouts.observeDay(day).first()
        assertEquals(listOf(bench), restoredDay.map { it.exerciseId })
        assertEquals(listOf(5), restoredDay.single().sets.map { it.values.reps })
        assertEquals(StarterPlans.plans.size, new.plans.routines.first().size)
        assertEquals(80.0, new.body.measurements.first().single().value, 0.0)
        assertEquals(UnitSystem.IMPERIAL, new.settings.settings.first().unitSystem)
        val custom = new.exercises.exercises.first().single { it.name == "My Custom Lift" }
        assertEquals("4-0-1-0", custom.tempo)
        assertTrue(custom.perSide)
        // Deleted rows travel too, so a future sync still knows about the deletion.
        assertEquals(2, new.database.backupDao().sets().size)
    }

    @Test
    fun aDamagedBackupChangesNothing() = runTest {
        val phone = Phone()
        phone.exercises.addMissingBuiltIns()
        phone.workouts.addSet(day, squat, SetValues(weightKg = 140.0, reps = 3))
        val backup = phone.backups.createBackup()

        // Two categories with the same id cannot both be stored.
        val damaged = backup.copy(categories = backup.categories + backup.categories.first())
        assertThrows(BackupException::class.java) { runBlocking { phone.backups.restore(damaged) } }
        assertEquals(listOf(squat), phone.workouts.observeDay(day).first().map { it.exerciseId })

        val unknownType = backup.copy(exercises = backup.exercises.map { it.copy(type = "FLYING") })
        assertThrows(BackupException::class.java) { runBlocking { phone.backups.restore(unknownType) } }
        assertEquals(listOf(squat), phone.workouts.observeDay(day).first().map { it.exerciseId })
    }
}
