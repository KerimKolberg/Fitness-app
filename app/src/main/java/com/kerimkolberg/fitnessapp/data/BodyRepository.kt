package com.kerimkolberg.fitnessapp.data

import com.kerimkolberg.fitnessapp.data.db.BodyDao
import com.kerimkolberg.fitnessapp.data.db.BodyMeasurementEntity
import com.kerimkolberg.fitnessapp.model.BodyMeasurement
import com.kerimkolberg.fitnessapp.model.BodyMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class BodyRepository(
    private val dao: BodyDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    /** All measurements, newest first. */
    val measurements: Flow<List<BodyMeasurement>> = dao.observeMeasurements().map { list ->
        list.map { BodyMeasurement(id = it.id, date = it.date, metric = it.metric, value = it.value) }
    }

    /** Records [value] (in stored units) for [metric] on [date], replacing that day's earlier value. */
    suspend fun saveMeasurement(metric: BodyMetric, date: LocalDate, value: Double) {
        val time = now()
        val existing = dao.getMeasurement(metric, date)
        if (existing != null) {
            dao.updateMeasurement(existing.copy(value = value, updatedAt = time))
        } else {
            dao.insertMeasurement(
                BodyMeasurementEntity(
                    id = newId(),
                    date = date,
                    metric = metric,
                    value = value,
                    createdAt = time,
                    updatedAt = time,
                ),
            )
        }
    }

    suspend fun deleteMeasurement(id: String) {
        dao.softDeleteMeasurement(id, now())
    }
}
