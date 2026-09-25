package com.kkfittracking.data.backup

import android.content.Context
import android.net.Uri
import com.kkfittracking.data.BodyRepository
import com.kkfittracking.data.ExerciseRepository
import com.kkfittracking.data.SettingsRepository
import com.kkfittracking.data.db.WorkoutDao
import com.kkfittracking.model.CsvExport
import com.kkfittracking.model.ExportSet
import com.kkfittracking.model.SetValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException

/** Reads and writes backup and export files that the user picked with the system file picker. */
class DataTransfer(
    private val context: Context,
    private val backups: BackupRepository,
    private val workoutDao: WorkoutDao,
    private val bodyRepository: BodyRepository,
    private val settingsRepository: SettingsRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    suspend fun writeBackup(uri: Uri) {
        val text = BackupJson.encode(backups.createBackup())
        write(uri, text)
        settingsRepository.setLastBackupAt(System.currentTimeMillis())
    }

    suspend fun readBackup(uri: Uri): BackupFile = BackupJson.decode(read(uri))

    /** Restores the backup, then files a backup from an earlier version under the current library. */
    suspend fun restore(file: BackupFile) {
        backups.restore(file)
        exerciseRepository.syncBuiltIns()
    }

    suspend fun exportWorkouts(uri: Uri) {
        val units = settingsRepository.settings.first().unitSystem
        val sets = workoutDao.exportRows().map { row ->
            ExportSet(
                date = row.date,
                exercise = row.exerciseName,
                category = row.categoryName,
                type = row.exerciseType,
                values = SetValues(
                    weightKg = row.set.weightKg,
                    reps = row.set.reps,
                    distanceMeters = row.set.distanceMeters,
                    durationSeconds = row.set.durationSeconds,
                    rpe = row.set.rpe,
                    note = row.set.comment,
                    isDropSet = row.set.isDropSet,
                ),
            )
        }
        write(uri, CsvExport.workouts(sets, units))
    }

    suspend fun exportBodyMeasurements(uri: Uri) {
        val units = settingsRepository.settings.first().unitSystem
        write(uri, CsvExport.bodyMeasurements(bodyRepository.measurements.first(), units))
    }

    private suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        // "wt" truncates, so overwriting a longer file leaves no old bytes behind.
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Could not open the file for writing.")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open the file.")
        stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
