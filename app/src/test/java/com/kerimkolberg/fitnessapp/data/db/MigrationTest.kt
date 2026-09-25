package com.kerimkolberg.fitnessapp.data.db

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kerimkolberg.fitnessapp.data.BodyRepository
import com.kerimkolberg.fitnessapp.data.ExerciseRepository
import com.kerimkolberg.fitnessapp.data.RoutineRepository
import com.kerimkolberg.fitnessapp.data.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/**
 * Builds a real database the way an older app version left it (from the exported schema files),
 * then opens it with the current app. Room runs the migrations and fails if the result does not
 * match the current schema exactly; the checks below make sure no data was lost.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test.db"
    private val day = LocalDate.of(2026, 9, 1)
    private var database: AppDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(name)
    }

    private fun schemaFile(version: Int): File {
        val path = "schemas/com.kerimkolberg.fitnessapp.data.db.AppDatabase/$version.json"
        return listOf(File(path), File("app/$path")).first { it.exists() }
    }

    /** Creates the database exactly as schema [version] defines it, then lets [seed] add rows. */
    private fun createOldDatabase(version: Int, seed: SQLiteDatabase.() -> Unit) {
        context.deleteDatabase(name)
        val schema = Json.parseToJsonElement(schemaFile(version).readText()).jsonObject.getValue("database").jsonObject
        val db = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        schema.getValue("entities").jsonArray.forEach { entity ->
            val table = entity.jsonObject.getValue("tableName").jsonPrimitive.content
            fun sql(template: String) = template.replace("\${TABLE_NAME}", table)
            db.execSQL(sql(entity.jsonObject.getValue("createSql").jsonPrimitive.content))
            entity.jsonObject["indices"]?.jsonArray?.forEach {
                db.execSQL(sql(it.jsonObject.getValue("createSql").jsonPrimitive.content))
            }
        }
        schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
        db.seed()
        db.version = version
        db.close()
    }

    private fun SQLiteDatabase.seedVersion1Rows() {
        execSQL("INSERT INTO categories VALUES ('c', 'Chest', -1, 0, 1, 1, NULL)")
        execSQL("INSERT INTO exercises VALUES ('e', 'Bench', 'c', 'WEIGHT_REPS', '', 1, 1, 1, NULL)")
        execSQL("INSERT INTO workouts VALUES ('w', ${day.toEpochDay()}, '', 1, 1, NULL)")
        execSQL("INSERT INTO workout_exercises VALUES ('we', 'w', 'e', 0, 1, 1, NULL)")
        execSQL("INSERT INTO workout_sets VALUES ('s', 'we', 0, 100.0, 5, NULL, NULL, '', 1, 1, NULL)")
    }

    private fun openCurrent(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
            .also { database = it }

    @Test
    fun fromVersion1() = runTest {
        createOldDatabase(1) { seedVersion1Rows() }
        val db = openCurrent()

        val logged = WorkoutRepository(db).observeDay(day).first().single()
        assertEquals("Bench", logged.exerciseName)
        val set = logged.sets.single().values
        assertEquals(100.0, set.weightKg!!, 0.0)
        assertEquals(5, set.reps)
        assertNull(set.rpe)

        val exercise = ExerciseRepository(db.exerciseDao()).getExercise("e")!!
        assertEquals("", exercise.tempo)
        assertFalse(exercise.perSide)
        assertEquals(0, RoutineRepository(db).routines.first().size)
    }

    @Test
    fun fromVersion2() = runTest {
        createOldDatabase(2) {
            seedVersion1Rows()
            execSQL("INSERT INTO routines VALUES ('r', 'Push', '', 1, 1, NULL)")
            execSQL("INSERT INTO routine_exercises VALUES ('re', 'r', 'e', 0, 1, 1, NULL)")
            execSQL("INSERT INTO body_measurements VALUES ('b', ${day.toEpochDay()}, 'BODYWEIGHT', 80.5, 1, 1, NULL)")
        }
        val db = openCurrent()

        assertEquals(listOf("e"), RoutineRepository(db).exerciseIds("r"))
        assertEquals(80.5, BodyRepository(db.bodyDao()).measurements.first().single().value, 0.0)
        assertEquals(5, WorkoutRepository(db).observeDay(day).first().single().sets.single().values.reps)
    }
}
