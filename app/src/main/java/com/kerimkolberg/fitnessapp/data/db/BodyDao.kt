package com.kerimkolberg.fitnessapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kerimkolberg.fitnessapp.model.BodyMetric
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface BodyDao {
    @Query("SELECT * FROM body_measurements WHERE deletedAt IS NULL ORDER BY date DESC")
    fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Query(
        """
        SELECT * FROM body_measurements
        WHERE metric = :metric AND date = :date AND deletedAt IS NULL
        LIMIT 1
        """,
    )
    suspend fun getMeasurement(metric: BodyMetric, date: LocalDate): BodyMeasurementEntity?

    @Insert
    suspend fun insertMeasurement(measurement: BodyMeasurementEntity)

    @Update
    suspend fun updateMeasurement(measurement: BodyMeasurementEntity)

    @Query("UPDATE body_measurements SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteMeasurement(id: String, now: Long)
}
